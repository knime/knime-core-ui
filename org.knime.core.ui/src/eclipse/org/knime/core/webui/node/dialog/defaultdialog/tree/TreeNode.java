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
 *   Aug 5, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.tree;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persistable;

import com.fasterxml.jackson.databind.JavaType;

/**
 * These are the nodes within a {@link Tree}. Next to the branching {@link Tree} node, there are two kinds of leafs:
 * {@link LeafNode}s with no further child widgets and {@link ArrayParentNode}s with an attached separate {@link Tree}
 * for elements.
 *
 * @param <S> the type of the [S]ettings. Either {@link Persistable} or {@link WidgetGroup}
 *
 * @author Paul Bärnreuther
 */
public sealed class TreeNode<S> permits LeafNode, Tree, ArrayParentNode {

    private final Tree<S> m_parent;

    private List<String> m_path;

    private Optional<ArrayParentNode<S>> m_containingArrayNode;

    private final JavaType m_type;

    private final SettingsType m_settingsType;

    final Map<Class<? extends Annotation>, Annotation> m_annotations;

    private final Collection<Class<? extends Annotation>> m_possibleAnnotations;

    private final Field m_underlyingField;

    /**
     * @param parent the parent widget tree or {@code null} if it's the root
     * @param settingsType
     * @param type the type this widget tree node represents
     * @param annotations function to get get the 'annotation instance' for an annotation class; function returns
     *            {@code null} if there is none
     * @param possibleAnnotations all allowed annotations
     * @param underlyingField used to get or set parent values
     */
    protected TreeNode(final Tree<S> parent, final SettingsType settingsType, final JavaType type,
        final Function<Class<? extends Annotation>, Annotation> annotations,
        final Collection<Class<? extends Annotation>> possibleAnnotations, final Field underlyingField) {
        m_parent = parent;
        m_type = type;
        m_settingsType = settingsType;
        m_annotations = toMap(annotations, possibleAnnotations);
        m_possibleAnnotations = possibleAnnotations;
        m_underlyingField = underlyingField;
    }

    static <K, V> Map<K, V> toMap(final Function<K, V> function, final Collection<K> keys) {
        return keys.stream().map(key -> new Pair<>(key, function.apply(key))).filter(pair -> pair.getSecond() != null)
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    /**
     * @return "view" or "model" or null in case of element trees of array widgets.
     */
    public SettingsType getSettingsType() {
        return m_settingsType;
    }

    /**
     * @return the path to the current node starting from the root tree (which can be an element widget tree of an
     *         {@link ArrayParentNode})
     */
    public List<String> getPath() {
        if (m_path == null) {
            m_path = getPathUsingParents();
        }
        return m_path;
    }

    /**
     * @return whether the current node has a parent. This is true for the global root as well as for the
     */
    public boolean isRoot() {
        return m_parent == null;
    }

    /**
     * Returns the root of the tree containing this node. If this is a root node, it returns itself.
     *
     * @return the parent of this node (or itself if it is a root node)
     */
    public TreeNode<S> getRoot() {
        return isRoot() //
            ? this //
            : getParent().getRoot();
    }

    /**
     * @return the name of this node. It is empty in case of a root tree node (which can be an element widget tree of an
     *         {@link ArrayParentNode})
     */
    public Optional<String> getName() {
        final var path = getPath();
        return path.isEmpty() ? Optional.empty() : Optional.of(path.get(path.size() - 1));
    }

    private List<String> getPathUsingParents() {
        final var parentTree = getParent();
        if (parentTree == null) {
            return List.of();
        }
        final var name = parentTree.getChildName(this);
        return Stream.concat(parentTree.getPath().stream(), Stream.of(name)).toList();

    }

    /**
     * This method can be used similar to {@link Field#set} to set the value of this node within a parents value. I.e.
     * this method can only be used when the node has a parent.
     *
     * @param parentValue a value of {@link #getRawClass()} of the parent node.
     * @param value the value to set
     *
     * @throws IllegalArgumentException if the parent value is null
     */
    public void setInParentValue(final Object parentValue, final Object value) {
        checkParentValue(parentValue);
        try {
            m_underlyingField.set(parentValue, value); // NOSONAR
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(String.format("Could not set access value of settings field %s although "
                + "reflection was used to make it accessible.", getPath()), ex);
        }
    }

    /**
     * This method can be used similar to {@link Field#get} to get the value of this node from the parents value. I.e.
     * this method can only be used when the node has a parent.
     *
     * @param parentValue a value of {@link #getRawClass()} of the parent node.
     *
     * @return the value of this node given the parent value
     * @throws IllegalArgumentException if the parent value is null
     */
    public Object getFromParentValue(final Object parentValue) {
        checkParentValue(parentValue);
        try {
            return m_underlyingField.get(parentValue);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(String.format(
                "Could not access value of settings field %s although reflection was used to make it accessible.",
                getPath()), ex);
        }
    }

    /**
     * @param defaultParentValue a constructor of the parent value to extract this nodes value from
     * @return the default value of this node given the parent value
     */
    public Object getDefaultValueFromParent(final Function<Tree<S>, Object> defaultParentValue) {
        return getFromParentValue(defaultParentValue.apply(getParent()));
    }

    private void checkParentValue(final Object parentValue) {
        CheckUtils.checkArgument(getParent() != null, "This method can only be used when the node has a parent.");
        CheckUtils.checkArgument(getParent().getRawClass().isAssignableFrom(parentValue.getClass()),
            "The parent value must be of the parent type.");
    }

    /**
     * @return the next parent {@link ArrayParentNode} if any
     */
    Optional<ArrayParentNode<S>> getContainingArrayWidgetNode() {
        if (m_containingArrayNode == null) { // NOSONAR
            m_containingArrayNode = getContainingArrayWidgetNodeUsingParents();
        }
        return m_containingArrayNode;
    }

    /**
     * @return a collection of all containing {@link ArrayParentNode}s starting with the most outer one.
     */
    public List<ArrayParentNode<S>> getContainingArrayWidgetNodes() {
        final List<ArrayParentNode<S>> containingArrayWidgetNodes = new ArrayList<>();
        getContainingArrayWidgetNode().ifPresent(containingNode -> {
            containingArrayWidgetNodes.addAll(containingNode.getContainingArrayWidgetNodes());
            containingArrayWidgetNodes.add(containingNode);
        });
        return containingArrayWidgetNodes;
    }

    /**
     * @return the first ancestor {@link ArrayParentNode} of this node if there are any.
     */
    protected Optional<ArrayParentNode<S>> getContainingArrayWidgetNodeUsingParents() {
        return getParent().getContainingArrayWidgetNodeUsingParents();
    }

    /**
     * @param key the annotation class
     * @param value of this annotation on the {@link #getParent() parent}
     */
    void addAnnotation(final Class<? extends Annotation> key, final Annotation value) {
        m_annotations.putIfAbsent(key, value);
    }

    /**
     * Used only for resolving {@link Modification}s.
     *
     * @param key
     * @param value
     */
    public void addOrReplaceAnnotation(final Class<? extends Annotation> key, final Annotation value) {
        m_annotations.put(key, value);
    }

    /**
     * @param annotationClass
     */
    public void removeAnnotation(final Class<? extends Annotation> annotationClass) {
        m_annotations.remove(annotationClass);
    }

    /**
     * @param annotationClass
     * @param <T> the type of the annotation
     * @return the annotation if present (or added via #addAnnotation(Class, Annotation))
     */
    @SuppressWarnings("unchecked") // The m_annotations map is constructed as required
    public <T extends Annotation> Optional<T> getAnnotation(final Class<T> annotationClass) {
        if (!m_possibleAnnotations.contains(annotationClass)) {
            throw new IllegalArgumentException(String.format("Annotation %s should not be used on a %s.",
                annotationClass.getSimpleName(), this.getClass().getSimpleName()));
        }
        return Optional.ofNullable((T)this.m_annotations.get(annotationClass));
    }

    /**
     * @return the collection of respected annotations for this node
     */
    public final Collection<Class<? extends Annotation>> getPossibleAnnotations() {
        return m_possibleAnnotations;
    }

    /**
     * @return the parent
     */
    Tree<S> getParent() {
        return m_parent;
    }

    /**
     *
     * Note: Ideally we would not use a jackson class here but it is either that or creating our own representation of a
     * type that contains information about generic types.
     *
     * @return the type
     */
    public JavaType getType() {
        if (isOptional()) {
            return m_type.containedType(0);
        }
        return m_type;
    }

    /**
     * @return true if this node is an optional field
     */
    public boolean isOptional() {
        return m_type.getRawClass() == Optional.class;
    }

    /**
     * In most regards {@link Optional} fields are handled like their contained class. So this method allows to get the
     * contained class in case the raw class is Optional.class.
     *
     * Use {@link #isOptional} to check if this is the case.
     *
     * @return the raw class or the contained class if the raw class is Optional.class
     */
    public Class<?> getRawClass() {
        return getType().getRawClass();
    }

    @Override
    public String toString() {
        return String.format("%s%s at %s", isOptional() ? "Optional " : "", getRawClass().getSimpleName(), getPath());
    }

}
