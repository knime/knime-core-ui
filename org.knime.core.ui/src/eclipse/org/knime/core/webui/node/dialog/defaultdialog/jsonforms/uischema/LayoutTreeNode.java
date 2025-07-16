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
 *   Apr 17, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Inside;

/**
 * A representation of a layout part class which is used in {@link LayoutTree} to determine the structure between all
 * layout parts.
 *
 * @author Paul Bärnreuther
 * @param <T> The type of a control that is associated with a leaf of this tree
 */
final class LayoutTreeNode<T> {
    private final Class<?> m_value;

    private final Collection<LayoutTreeNode<T>> m_isAfter = new HashSet<>();

    private final Collection<LayoutTreeNode<T>> m_isBefore = new HashSet<>();

    private PointsToUpdater<T> m_pointsToUpdater = new PointsToUpdater<>(new HashSet<>());

    /**
     * A wrapper class for a collection of layout tree nodes which the node points to. When any node pointed to by this
     * node points to a new node itself, this new node is also added here.
     *
     * @author Paul Bärnreuther
     */
    private static class PointsToUpdater<T> {

        private Collection<LayoutTreeNode<T>> m_pointers;

        private Collection<PointsToUpdater<T>> m_synced;

        public PointsToUpdater(final Collection<LayoutTreeNode<T>> value) {
            m_pointers = value;
            m_synced = new HashSet<>();
        }

        /**
         * Instead of pointing to the target, this method just makes sure that the two updaters always yield the same
         * values.
         *
         * @param target
         */
        void syncWith(final LayoutTreeNode<T> target) {
            var targetPointTo = target.getPointsToUpdater();

            // sync for the moment
            addNewPointers(targetPointTo.get());
            targetPointTo.addNewPointers(get());

            // make sure that they stay in sync forever
            this.addListener(targetPointTo);
            targetPointTo.addListener(this);
        }

        private void addListener(final PointsToUpdater<T> targetUpdater) {
            m_synced.add(targetUpdater);
        }

        /**
         * Points to a new target with a different parent for this updater and all that are in sync with it.
         *
         * @param target
         */
        void pointTo(final LayoutTreeNode<T> target) {
            addNewPointers(List.of(target));
        }

        private void addNewPointers(final Collection<LayoutTreeNode<T>> newValues) {
            var valuesSize = m_pointers.size();
            m_pointers.addAll(newValues);
            if (valuesSize < m_pointers.size()) {
                m_synced.forEach(listener -> listener.addNewPointers(newValues));
            }
        }

        Collection<LayoutTreeNode<T>> get() {
            return m_pointers;
        }

    }

    private final List<T> m_controls = new ArrayList<>(0);

    private Collection<LayoutTreeNode<T>> m_children;

    private LayoutTreeNode<T> m_parent;

    private Collection<LayoutTreeNode<T>> m_leaves;

    LayoutTreeNode(final Class<?> value) {
        m_value = value;
        m_children = new HashSet<>();
    }

    /**
     * @return the child layout nodes
     */
    public Collection<LayoutTreeNode<T>> getChildren() {
        return m_children;
    }

    /**
     * @return the layout part class that is represented by this tree
     */
    public Class<?> getValue() {
        return m_value;
    }

    /**
     * @return the controls that are associated with this layout part
     */
    public List<T> getControls() {
        return m_controls;
    }

    void addControls(final List<T> controls) {
        m_controls.addAll(controls);
    }

    /**
     * Add a simple arrow from this node to the given node. The arrow indicates that this node is before the
     * <code>inBack</code> node.
     *
     * @param inBack the node that comes after this node
     */
    void addArrowTo(final LayoutTreeNode<T> inBack) {
        inBack.m_isAfter.add(this);
        this.m_isBefore.add(inBack);
    }

    LayoutTreeNode<T> getParent() {
        return m_parent;
    }

    /**
     * @return whether the node has a parent.
     */
    boolean isRoot() {
        return getParent() == null;
    }

    /**
     * @return whether the node or any of its descendants contains controls
     */
    boolean hasContent() {
        return !getControls().isEmpty() || getChildren().stream().anyMatch(LayoutTreeNode::hasContent);
    }

    private PointsToUpdater<T> getPointsToUpdater() {
        return m_pointsToUpdater;
    }

    /**
     * A node points to another node if it targets it with an {@link After}, {@link Before} or {@link Inside}
     * annotation. At a later point the parents of these targets replace the parent of this node.
     *
     * @param target
     */
    void pointToAsSibling(final LayoutTreeNode<T> target) {
        var areNestSiblings = m_parent != null && target.getParent() == m_parent;
        if (areNestSiblings) {
            getPointsToUpdater().syncWith(target);
        } else {
            if (m_value.isNestmateOf(target.getValue())) {

                throw new UiSchemaGenerationException(String.format(
                    "Layout part %s targets layout part %s although it is a nest mate. "
                        + "Place the parts next to each other instead.",
                    getValue().getSimpleName(), target.getValue().getSimpleName()), this, target);

            }
            getPointsToUpdater().pointTo(target);
        }
    }

    void pointToAsChild(final LayoutTreeNode<T> target) {
        LayoutTreeNode<T> fakeSibling = new LayoutTreeNode<>(target.getValue());
        fakeSibling.setParent(target);
        pointToAsSibling(fakeSibling);
    }

    /**
     * Pointers to different nodes coming from {@link After}, {@link Before} or {@link Inside} are resolved to yield
     * exactly one target parent which replaces the current one. If more than one parent is found in that way, an error
     * is thrown.
     */
    void adaptParentFromPointers() {
        final var pointsToLeaves = getPointsToLeaves(new HashSet<>());
        var pointsToParents = pointsToLeaves.stream().map(LayoutTreeNode::getParent).collect(Collectors.toSet());
        if (pointsToParents.size() > 1) {
            throwOrderDependenciesException();
        }
        pointsToParents.stream().filter(Objects::nonNull).findAny().ifPresent(this::setParent);
        Optional.ofNullable(getParent()).ifPresent(
            parent -> pointsToLeaves.stream().filter(LayoutTreeNode::isRoot).forEach(leaf -> leaf.setParent(parent)));
    }

    private Collection<LayoutTreeNode<T>> getPointsToLeaves(final Collection<LayoutTreeNode<T>> alreadyVisitedNodes) {
        if (m_leaves != null) {
            return m_leaves;
        }
        if (alreadyVisitedNodes.contains(this)) {
            throwOrderDependenciesException();
        }
        alreadyVisitedNodes.add(this);
        var directPointsTo = getPointsToUpdater().get();
        Collection<LayoutTreeNode<T>> output;
        if (directPointsTo.isEmpty()) {
            output = Set.of(this);
        } else {
            output = new HashSet<>();
            /**
             * We can't parallelize this, since we would throw an error when visiting already visited nodes whose leaves
             * are not yet computed.
             */
            for (var node : directPointsTo) {
                output.addAll(node.getPointsToLeaves(alreadyVisitedNodes));
            }
        }
        m_leaves = output;
        return output;
    }

    private void throwOrderDependenciesException() {
        throw new UiSchemaGenerationException(
            String.format("Conflicting order annotations for layout part %s", getValue().getSimpleName()), this);
    }

    void setParent(final LayoutTreeNode<T> target) {
        if (m_parent != null) {
            m_parent.getChildren().remove(this);
        }
        m_parent = target;
        if (target != null) {
            target.getChildren().add(this);
        }
    }

    /**
     * We want to order the children of all nodes in the tree and then keep only those which contain content.
     */
    TraversableLayoutTreeNode<T> toTraversable() {
        final var orderedChildren = sortByTopologicalOrder(m_children);
        final var filteredOrderedChildren = orderedChildren.stream().filter(LayoutTreeNode::hasContent).toList();
        final var traversableChildren = filteredOrderedChildren.stream().map(LayoutTreeNode::toTraversable).toList();
        return new TraversableLayoutTreeNode<>(m_controls, traversableChildren, Optional.ofNullable(m_value));
    }

    /**
     * Kahn's algorithm for sorting the given nodes with respect their isBefore and isAfter specifications. Whenever two
     * ore more elements have equivalent such specifications, they are sorted alphabetically.
     *
     * @see <a href= "https://www.geeksforgeeks.org/topological-sorting-indegree-based-solution/">
     *      https://www.geeksforgeeks.org/topological-sorting-indegree-based-solution/ </a>
     * @param nodes
     * @return a new object of sorted nodes.
     */
    private static <T> List<LayoutTreeNode<T>> sortByTopologicalOrder(final Collection<LayoutTreeNode<T>> nodes) {
        return new DirectedGraph<>(nodes).sort();

    }

    private static class DirectedGraph<T> {

        private final Collection<LayoutTreeNode<T>> m_nodes;

        /**
         * @param nodes
         */
        public DirectedGraph(final Collection<LayoutTreeNode<T>> nodes) {
            m_nodes = new HashSet<>(nodes);
        }

        List<LayoutTreeNode<T>> sort() {
            var sorted = new ArrayList<LayoutTreeNode<T>>();
            while (!m_nodes.isEmpty()) {
                var nextChild = getNextChild();
                removeChild(nextChild);
                sorted.add(nextChild);
            }
            return sorted;
        }

        /**
         * @return the next node without any incoming edges or the alphabetically first of such if there are multiple
         */
        private LayoutTreeNode<T> getNextChild() {
            return m_nodes.stream() //
                .filter(node -> node.m_isAfter.isEmpty()) //
                .sorted(Comparator.comparing(node -> node.getValue().getSimpleName())) //
                .findFirst().orElseThrow(() -> {
                    throw new UiSchemaGenerationException("Circular ordering of layout parts",
                        m_nodes.stream().toArray(LayoutTreeNode<?>[]::new));
                });
        }

        private void removeChild(final LayoutTreeNode<T> nextChild) {
            nextChild.m_isBefore.forEach(o -> o.m_isAfter.remove(nextChild));
            m_nodes.remove(nextChild);
        }
    }

    @Override
    public String toString() {
        return toString("");
    }

    private String toString(final String indent) {
        final var lines = new ArrayList<String>();
        if (this.getValue() != null) {
            lines.add(
                indent + String.format("%s ({@link %s})", this.getValue().getSimpleName(), this.getValue().getName()));
        }
        final var childIndent = indent + "|    ";
        Map.of("after", m_isAfter, "before", m_isBefore).entrySet().forEach(e -> {
            if (!e.getValue().isEmpty()) {
                lines.add(String.format("%s  is %s ", indent, e.getKey()) + String.join(", ",
                    e.getValue().stream().map(LayoutTreeNode::getValue).map(Class::getSimpleName).toList()));
            }
        });
        getControls().stream().map(control -> String.format("%s| -> %s", indent, control)).forEach(lines::add); // NOSONAR
        getChildren().stream().map(child -> child.toString(childIndent)).forEach(lines::add); // NOSONAR

        return String.join("\n", lines);
    }

}
