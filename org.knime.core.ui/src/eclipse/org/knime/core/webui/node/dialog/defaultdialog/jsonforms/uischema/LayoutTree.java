/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 13, 2023 (benjamin): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.util.CheckUtils;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;

/**
 * A tree of layout parts. The tree has the structure that is needed to build a JSON Forms UI Schema from it. Its nodes
 * are {@link LayoutTreeNode}s and correspond to the layout parts classes.
 *
 * @param <T> The type of a control that is associated with a leaf of this tree
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Paul Bärnreuther
 */
final class LayoutTree<T> {

    private final Map<Class<?>, LayoutTreeNode<T>> m_nodes = new HashMap<>();

    private final Deque<LayoutTreeNode<T>> m_nodesStack = new ArrayDeque<>();

    private final List<LayoutTreeNode<T>> m_roots;

    private List<T> m_nodesWithoutLayout;

    /**
     * From the given map, we find an ordered hierarchical structure with controls as leaves. The order of siblings can
     * be controlled by {@link After} and {@link Before} annotations, while the vertical structure is derived from the
     * class nesting structure.
     *
     * @param layoutClassesToControls mapping the values of {@link Layout} annotations to their widget nodes
     * @param nodesWithoutLayout i.e. nodes without a designated {@link Layout} annotation
     */
    LayoutTree(final Map<Class<?>, List<T>> layoutClassesToControls, final List<T> nodesWithoutLayout) {

        m_nodesWithoutLayout = nodesWithoutLayout;

        buildTreeFromContentMap(layoutClassesToControls);

        addHorizontalArrows();

        m_nodes.values().forEach(LayoutTreeNode::adaptParentFromPointers);

        m_roots = shakeTreeAndFindRoots();

    }

    /**
     * We want to find a unique root for the present tree. From the previous construction it is possible that we find
     * multiple roots even in a valid case, since we initially create all parents of all entry points but possibly
     * re-wired some parents at a later point, but all but one of them must not contain any branch leading to a leave
     * with content.
     *
     * @return The unique root with content or an empty tree node if the tree is empty.
     */
    private List<LayoutTreeNode<T>> shakeTreeAndFindRoots() {
        return m_nodes.values().stream().filter(n -> n.isRoot()).filter(LayoutTreeNode::hasContent).toList();
    }

    private void buildTreeFromContentMap(final Map<Class<?>, List<T>> layoutClassesToControls) {
        layoutClassesToControls.entrySet().stream().forEach(e -> constructNodeFromEntry(e.getKey(), e.getValue()));

    }

    /**
     * Constructs a node with content given by the value of the entry.
     *
     * @param entry
     * @return
     */
    private LayoutTreeNode<T> constructNodeFromEntry(final Class<?> clazz, final List<T> widgetTreeNodes) {
        var node = getOrConstructNode(clazz);
        node.addControls(widgetTreeNodes);
        return node;
    }

    /**
     * Retrieves the node associated to the given class. If it does not yet exist, it and its ancestors are constructed.
     *
     * @param clazz a class corresponding to a layout part
     * @return A node with value clazz
     */
    private LayoutTreeNode<T> getOrConstructNode(final Class<?> clazz) {
        var node = m_nodes.get(clazz);
        if (node == null) {
            node = new LayoutTreeNode<>(clazz);
            m_nodes.put(clazz, node);
            m_nodesStack.push(node);
            constructParent(node);
        }
        return node;
    }

    /**
     * Constructs a new LayoutTreeNode or finds the existing one and wires the given node as a child of it
     *
     * @param node
     */
    private void constructParent(final LayoutTreeNode<T> node) {
        var parentClass = node.getValue().getEnclosingClass();
        if (parentClass == null) {
            return;
        }
        var parent = getOrConstructNode(parentClass);
        node.setParent(parent);
    }

    private void addHorizontalArrows() {
        while (!m_nodesStack.isEmpty()) {
            addArrowsAndPointers(m_nodesStack.pop());
        }
    }

    private void addArrowsAndPointers(final LayoutTreeNode<T> node) {
        getAnnotations(node, Before.class).forEach(before -> {
            var target = getOrConstructNode(before.value());
            node.addArrowTo(target); // node before target
            node.pointToAsSibling(target);
        });
        getAnnotations(node, After.class).forEach(after -> {
            var target = getOrConstructNode(after.value());
            target.addArrowTo(node); // node after target
            node.pointToAsSibling(target);
        });
        getAnnotations(node, Inside.class).stream().findFirst().map(Inside::value).ifPresent(targetClass -> {
            var target = getOrConstructNode(targetClass);
            node.pointToAsChild(target);
        });
    }

    private static <T, A extends Annotation> List<A> getAnnotations(final LayoutTreeNode<T> node,
        final Class<A> annotationsClass) {
        return Arrays.asList(node.getValue().getAnnotationsByType(annotationsClass));
    }

    boolean hasMultipleRoots() {
        return m_roots.size() > 1;
    }

    void assertSingleRoot() throws UiSchemaGenerationException {
        if (hasMultipleRoots()) {
            throw new UiSchemaGenerationException("Multiple root layout nodes detected", m_roots);
        }
    }

    /**
     * Only call this method if {@link #hasMultipleRoots()} returns false or {@link #assertSingleRoot()} has been
     * called.
     *
     * @return the unique {@link LayoutTreeNode} which is the root of the layout.
     */
    TraversableLayoutTreeNode<T> getRootNode() {
        CheckUtils.checkState(!hasMultipleRoots(),
            "The tree has multiple roots. Calling this method is not allowed in this case");
        final var rootNode = m_roots.stream().findFirst().orElseGet(() -> new LayoutTreeNode<>(null));
        rootNode.addControls(m_nodesWithoutLayout);
        return rootNode.toTraversable();

    }

}
