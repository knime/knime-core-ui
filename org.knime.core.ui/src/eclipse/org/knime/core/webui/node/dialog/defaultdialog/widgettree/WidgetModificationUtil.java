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
 *   Sep 17, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widgettree;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification.ImperativeWidgetModification.AnnotationBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification.ImperativeWidgetModification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification.ImperativeWidgetModification.WidgetModifier;

/**
 * Resolves {@link WidgetModification} annotations for {@link WidgetTree}s and {@link ArrayWidgetNode}s
 *
 * @author Paul Bärnreuther
 */
final class WidgetModificationUtil {

    private WidgetModificationUtil() {
        // utility class
    }

    /**
     *
     * Resolves and executes the imperative instructions given by the widgetModification by altering the annotations of
     * children inside the given widgetTree.
     */
    static void resolveWidgetModification(final WidgetTree widgetTree, final WidgetModification widgetModification) {
        final var imperativeWidgetModification = widgetModification.value();
        final var widgetTreeModifier = new WidgetTreeModifier(widgetTree);
        InstantiationUtil.createInstance(imperativeWidgetModification).modify(widgetTreeModifier);
    }

    /**
     * This modifier finds widget by reference by traversing the widget tree and its element widget trees.
     */
    static final class WidgetTreeModifier implements WidgetGroupModifier {

        private final WidgetTree m_widgetTree;

        WidgetTreeModifier(final WidgetTree widgetTree) {
            m_widgetTree = widgetTree;
        }

        @Override
        public WidgetModifier find(final Class<? extends WidgetModification.Reference> reference) {
            return new WidgetTreeNodeModifier(findWidgetTreeNodeByReference(m_widgetTree, reference));
        }

        private static WidgetTreeNode findWidgetTreeNodeByReference(final WidgetTree widgetTree,
            final Class<? extends WidgetModification.Reference> reference) {
            final var candidates =
                widgetTree.getWidgetAndWidgetTreeNodes().flatMap(WidgetTreeModifier::traverseAlsoIntoElementWidgetTrees)
                    .filter(node -> hasReference(node, reference)).toList();
            return getSingleCandidate(reference, candidates);
        }

        private static WidgetTreeNode getSingleCandidate(final Class<? extends WidgetModification.Reference> reference,
            final List<WidgetTreeNode> candidates) {
            if (candidates.size() > 1) {
                throw new IllegalStateException("Multiple nodes with the same reference found: " + String.join(", ",
                    candidates.stream().map(WidgetTreeNode::getPath).map(l -> String.join(".", l)).toList()));
            } else if (candidates.isEmpty()) {
                throw new IllegalStateException("No node with the reference found: " + reference.getSimpleName());
            } else {
                return candidates.get(0);
            }
        }

        private static boolean hasReference(final WidgetTreeNode node,
            final Class<? extends WidgetModification.Reference> reference) {
            final var widgetReference = node.getAnnotation(WidgetModification.WidgetReference.class);
            return widgetReference.isPresent() && widgetReference.get().value().equals(reference);
        }

        private static Stream<WidgetTreeNode> traverseAlsoIntoElementWidgetTrees(final WidgetTreeNode node) {
            if (node instanceof ArrayWidgetNode arrayWidgetNode) {
                return Stream.concat(Stream.of(node),
                    arrayWidgetNode.getElementWidgetTree().getWidgetAndWidgetTreeNodes());
            }
            return Stream.of(node);
        }
    }

    /**
     * This modifier is used to modify the annotations of a single widget tree node. When modifying or adding an
     * annotation we create a proxy around the existing or default value of the annotation, respectively.
     */
    static final class WidgetTreeNodeModifier implements WidgetModifier {

        private final WidgetTreeNode m_widgetTreeNode;

        public WidgetTreeNodeModifier(final WidgetTreeNode widgetTreeNode) {
            m_widgetTreeNode = widgetTreeNode;
        }

        @Override
        public <T extends Annotation> AnnotationBuilder modifyAnnotation(final Class<T> annotationClass) {
            final var existingAnnotation =
                m_widgetTreeNode.getAnnotation(annotationClass).orElseThrow(() -> new IllegalStateException(
                    "Annotation cannot be modified because it is not present: " + annotationClass.getSimpleName()));
            return new WidgetTreeNodeAnnotationBuilder<>(annotationClass, existingAnnotation);
        }

        @Override
        public <T extends Annotation> AnnotationBuilder addAnnotation(final Class<T> annotationClass) {
            if (m_widgetTreeNode.getAnnotation(annotationClass).isPresent()) {
                throw new IllegalStateException(
                    "Annotation cannot be added because it is already present: " + annotationClass.getSimpleName());
            }
            return new WidgetTreeNodeAnnotationBuilder<>(annotationClass, null);
        }

        @Override
        public <T extends Annotation> void removeAnnotation(final Class<T> annotationClass) {
            if (m_widgetTreeNode.getAnnotation(annotationClass).isEmpty()) {
                throw new IllegalStateException(
                    "Annotation cannot be removed because it is not present: " + annotationClass.getSimpleName());
            }
            m_widgetTreeNode.removeAnnotation(annotationClass);
        }

        private final class WidgetTreeNodeAnnotationBuilder<T extends Annotation> implements AnnotationBuilder {

            private final Map<String, Object> m_properties = new HashMap<>();

            private final Class<T> m_annotationClass;

            private final T m_existingAnnotation;

            public WidgetTreeNodeAnnotationBuilder(final Class<T> annotationClass, final T existingAnnotation) {
                m_annotationClass = annotationClass;
                m_existingAnnotation = existingAnnotation;
            }

            @Override
            public <S> AnnotationBuilder withProperty(final String key, final S value) {
                m_properties.put(key, value);
                return this;
            }

            @Override
            public void build() {
                final T newAnnotation = createProxy(m_annotationClass, m_properties, m_existingAnnotation);
                m_widgetTreeNode.addOrReplaceAnnotation(m_annotationClass, newAnnotation);
            }

            @SuppressWarnings("unchecked") // proxy instance is of the type of the annotation
            private static <T extends Annotation> T createProxy(final Class<T> annotation,
                final Map<String, Object> replacements, final T delegate) throws IllegalArgumentException {

                assertValidParameterReplacements(annotation, replacements);

                InvocationHandler handler = (proxy, method, args) -> {
                    return useReplacementsOrDelegateOrDefault(annotation, replacements, delegate, method, args);
                };
                return (T)newProxyInstance(WidgetModificationUtil.class.getClassLoader(), new Class<?>[]{annotation},
                    handler);
            }

            private static <T extends Annotation> Object useReplacementsOrDelegateOrDefault(final Class<T> annotation,
                final Map<String, Object> replacements, final T delegate, final Method method, final Object[] args)
                throws IllegalAccessException, InvocationTargetException {
                if (replacements.containsKey(method.getName())) {
                    return replacements.get(method.getName());
                }
                if (delegate != null) {
                    return method.invoke(delegate, args);
                }
                return getDefaultValue(method, annotation);
            }

            private static <T extends Annotation> Object getDefaultValue(final Method method,
                final Class<T> annotation) {
                final Object defaultValue;
                try {
                    defaultValue = method.getDefaultValue();
                } catch (TypeNotPresentException ex) {
                    throw new IllegalArgumentException(
                        "The property \"" + method.getName() + "\" is required for " + annotation.getSimpleName(), ex);
                }
                if (defaultValue == null) {
                    throw new IllegalArgumentException(
                        "The property \"" + method.getName() + "\" is required for " + annotation.getSimpleName());
                }
                return defaultValue;
            }

            /**
             * Check that all replacements have a key which is the name of a method of the annotation. Also check that
             * the respective value is of the return type if that method.
             */
            private static void assertValidParameterReplacements(final Class<? extends Annotation> annotation,
                final Map<String, Object> replacements) {
                for (var entry : replacements.entrySet()) {
                    var method = Stream.of(annotation.getDeclaredMethods())
                        .filter(m -> m.getName().equals(entry.getKey())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            "No method with name \"" + entry.getKey() + "\" found in " + annotation.getSimpleName()));
                    if (!method.getReturnType().isInstance(entry.getValue())) {
                        throw new IllegalArgumentException(
                            "The value for method \"" + method.getName() + "\" in " + annotation.getSimpleName()
                                + " must be of type " + method.getReturnType().getSimpleName());
                    }
                }

            }

        }

    }

}