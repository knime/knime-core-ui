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
 *   Dec 18, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;

/**
 * An abstract factory which splits the logic on how to combine persistence logic (e.g. how to save) across the tree
 * structure.
 *
 * @param <T> the type of what this factory produces
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
public abstract class PersistenceFactory<T> {

    /**
     * Using this method, define how information encoded in a {@link NodeParametersPersistor} are translated to the type
     * T.
     *
     * @param nodeSettingsPersistor constructed from a {@link Persistor} annotation.
     * @param treeNode the node that is annotated with {@link Persistor}. In case
     *            {@link #getFromCustomPersistorForType(NodeParametersPersistor, Tree)} is not overwritten, this might
     *            be the tree associated to an annotated type. Otherwise, the annotation is always a field annotation.
     * @return the to be extracted property
     */
    protected abstract T getFromCustomPersistor(NodeParametersPersistor<?> nodeSettingsPersistor,
        TreeNode<Persistable> treeNode);

    /**
     * In addition to {@link #getFromCustomPersistor(NodeParametersPersistor, TreeNode)}, this method can be overwritten
     * to extract the property from a type-level {@link NodeParametersPersistor} in a different way.
     *
     * @param nodeSettingsPersistor constructed from a {@link Persistor} annotation on a type.
     * @param tree the associated tree of the annotated type.
     * @return the to be extracted property
     */
    protected T getFromCustomPersistorForType(final NodeParametersPersistor<?> nodeSettingsPersistor,
        final Tree<Persistable> tree) {
        return getFromCustomPersistor(nodeSettingsPersistor, tree);
    }

    /**
     * Construct the property for a leaf tree node. The result of this method is NOT nested additionally using
     * {@link #getNested}!
     *
     *
     * Note that this method is only called if the node does not provide a {@link Persistor}.
     *
     * @param node a leaf
     * @return the to be extracted property
     */
    protected abstract T getForLeaf(LeafNode<Persistable> node);

    /**
     * Construct the property for a tree node. Note that the in case of a tree which is also child to another tree, the
     * result of this method is nested additionally using {@link #getNested} but not in case this is an element tree of
     * an {@link ArrayParentNode} or the root node.
     *
     * Note that this method is only called if the tree does not provide a {@link Persistor}.
     *
     * @param tree
     * @param childProperty a method for extracting the property for each child of the tree
     * @return the combined property for the tree.
     */
    protected abstract T getForTree(Tree<Persistable> tree, Function<TreeNode<Persistable>, T> childProperty);

    /**
     * Construct the property for an array node. Note that the result of this method is nested additionally using
     * {@link #getNested} when used as child property for a tree.
     *
     * Note that this method is only called if the node does not provide a {@link Persistor}.
     *
     * @param arrayNode
     * @param elementProperty the already extracted property associated to
     * @return the extracted property of the array
     */
    protected abstract T getForArray(ArrayParentNode<Persistable> arrayNode, T elementProperty);

    /**
     * Construct the property for an array node using a custom {@link ArrayPersistor}.
     *
     * @param arrayNode the array node
     * @param customArrayPersistor an instance of the the custom array persistor attached to the array node
     * @param elementFieldPersistors instances of the element field persistors for the element of the array with
     *            suitable generic types
     * @return the extracted property of the array
     */
    protected abstract T getForCustomArrayPersistor(ArrayParentNode<Persistable> arrayNode,
        ArrayPersistor customArrayPersistor, Map<TreeNode<Persistable>, ElementFieldPersistor> elementFieldPersistors);

    /**
     * Called when a property was extracted for a {@link Tree} or a {@link ArrayParentNode} and this property is now to
     * be used in a {@link #getForTree} method of the parent of that node.
     *
     * @param node a child node
     * @param property the extracted property for the node that was constructed via
     *            {@link #getFromCustomPersistorForType}, {@link #getForTree} or {@link #getForArray} but NOT via
     *            {@link #getForLeaf} or {@link #getFromCustomPersistor}.
     * @return the property
     */
    protected abstract T getNested(final TreeNode<Persistable> node, final T property);

    /**
     * Called when an extracted property defines a relative route via {@link PersistWithin}.
     *
     * @param relativePath the relative path defined via {@link PersistWithin}.
     * @param property the already extracted property.
     * @param node the node with {@link PersistWithin} as field annotation.
     * @return the adjusted property.
     */
    protected abstract T reroute(final String[] relativePath, final T property, final TreeNode<Persistable> node);

    /**
     * Called when an extracted property defines a relative route via {@link PersistWithin}.
     *
     * @param relativePath the relative path defined via {@link PersistWithin}.
     * @param property the already extracted property.
     * @param node the tree with {@link PersistWithin} as type annotation.
     * @return the adjusted property.
     */
    protected T rerouteForType(final String[] relativePath, final T property, final Tree<Persistable> node) {
        return reroute(relativePath, property, node);
    }

    /**
     * Post process step for adjusting the property if a {@link Migration} is present.
     *
     * @param existing the already extracted property so far
     * @param configsDeprecations the non-empty configs deprecations (extracted from a {@link Migration} annotation).
     * @param configPaths the config paths used to persist this node.
     * @return the combined result
     */
    protected T combineWithConfigsDeprecations(final T existing, final List<ConfigMigration> configsDeprecations,
        final Supplier<String[][]> configPaths) {
        return existing;
    }

    /**
     * Overwrite this method together with {@link #combineWithConfigsDeprecationsForType} instead of
     * {@link #combineWithConfigsDeprecations(Object, List, Supplier)} if combining depends on the node and whether the
     * {@link Migration} annotation is a class- or field- annotation.
     *
     * This part is for field annotations
     *
     * @param existing the already extracted property so far
     * @param configsDeprecations the non-empty configs deprecations (extracted from a {@link Migration} annotation).
     * @param configPaths the config paths used to persist this node.
     * @param node the node with {@link Migration} as field annotation.
     * @return the adjusted property.
     */
    protected T combineWithConfigsDeprecations(final T existing, final List<ConfigMigration> configsDeprecations,
        final Supplier<String[][]> configPaths, final TreeNode<Persistable> node) {
        return combineWithConfigsDeprecations(existing, configsDeprecations, configPaths);
    }

    /**
     * Overwrite this method together with {@link #combineWithConfigsDeprecations(Object, List, Supplier, TreeNode)}
     * instead of {@link #combineWithConfigsDeprecations(Object, List, Supplier)} if combining depends on the node and
     * whether the {@link Migration} annotation is a class- or field- annotation.
     *
     * This part is for type annotations
     *
     * @param existing the already extracted property so far
     * @param configsDeprecations the non-empty configs deprecations (extracted from a {@link Migration} annotation).
     * @param configPaths the config paths used to persist this node.
     * @param node the tree with {@link Migration} as type annotation.
     * @return the adjusted property.
     */
    protected T combineWithConfigsDeprecationsForType(final T existing, final List<ConfigMigration> configsDeprecations,
        final Supplier<String[][]> configPaths, final Tree<Persistable> node) {
        return combineWithConfigsDeprecations(existing, configsDeprecations, configPaths);
    }

    /**
     * Post process step for adjusting the property if a {@link Migrate} annotation with true
     * {@link Migrate#loadDefaultIfAbsent()} is present for the given node.
     *
     * @param existing the already extracted property so far
     * @param configPathsProvider the config paths used to persist this field.
     * @param node with the {@link Migrate} annotation.
     * @return the adjusted property.
     */
    protected T combineWithLoadDefault(final T existing, final Supplier<String[][]> configPathsProvider,
        final TreeNode<Persistable> node) {
        return existing;
    }

    /**
     * Extracts the to be extracted property from a settings class.
     *
     * @param settingsClass
     * @return the extracted property.
     */
    protected final T extractFromSettings(final Class<? extends Persistable> settingsClass) {
        final var persistTree = new PersistTreeFactory().createTree(settingsClass);
        return extractFromTree(persistTree);
    }

    /**
     * Extracts the to be extracted property from the type-level within a tree by combining the results of all children
     * extracted with {@link #extractFromTreeNode(TreeNode)}.
     *
     * @param node
     * @return the extracted property.
     */
    protected final T extractFromTree(final Tree<Persistable> node) {
        return performExtraction(new TreeExtractionMethods(node));
    }

    /**
     * Extracts the to be extracted property from the node level within a tree.
     *
     * @param node
     * @return the extracted property.
     */
    protected final T extractFromTreeNode(final TreeNode<Persistable> node) {
        validateAnnotationCombinations(node);
        return performExtraction(new TreeNodeExtractionMethods(node));

    }

    /**
     * @throws IllegalStateException if the annotations are not valid.
     */
    private static void validateAnnotationCombinations(final TreeNode<Persistable> node) {
        if (node.getAnnotation(Persistor.class).isPresent() && node.getAnnotation(Persist.class).isPresent()) {
            throw new IllegalStateException(
                String.format(
                    "Cannot have both @Persistor and @Persist annotation on the same field %s."
                        + "Read the documentation of @Persist on how to realize the same via @Persistor.",
                    node.getPath()));
        }
        if (node.getAnnotation(Migration.class).isPresent() && node.getAnnotation(Migrate.class).isPresent()) {
            throw new IllegalStateException(
                String.format(
                    "Cannot have both @Migration and @Migrate annotation on the same field %s. "
                        + "Read the documentation of @Migrate on how to realize the same via @Migration.",
                    node.getPath()));
        }
        if (node instanceof ArrayParentNode<Persistable> && node.getAnnotation(PersistArray.class).isPresent()) {
            disallowOtherPersistAnnotations(node, PersistArray.class);
        }
        if (node.getPossibleAnnotations().contains(PersistArrayElement.class)
            && node.getAnnotation(PersistArrayElement.class).isPresent()) {
            disallowOtherPersistAnnotations(node, PersistArrayElement.class);
        }

    }

    private static void disallowOtherPersistAnnotations(final TreeNode<Persistable> node,
        final Class<? extends Annotation> annotationPreventingOthers) {
        final var persistAnnotation = node.getAnnotation(Persist.class);
        final var persistorAnnotation = node.getAnnotation(Persistor.class);
        final var migrateAnnotation = node.getAnnotation(Migrate.class);
        final var migrationAnnotation = node.getAnnotation(Migration.class);
        if (persistAnnotation.isPresent() || persistorAnnotation.isPresent() || migrateAnnotation.isPresent()
            || migrationAnnotation.isPresent()) {
            throw new IllegalStateException(String
                .format("The field %s cannot have @Persist, @Persistor, @Migrate or @Migration annotations since it"
                    + " has a @%s annotation.", node.getPath(), annotationPreventingOthers.getSimpleName()));
        }
    }

    private T performExtraction(final ExtractionMethods<T> ex) {
        final var customPersistor = ex.getAnnotation(Persistor.class).map(Persistor::value)
            .map(InitializeWithDefaultConstructorUtil::createPersistor);
        final var withoutLoader = customPersistor.map(ex::fromPersistor).orElseGet(ex::getDefault);
        final Supplier<String[][]> configPathsSupplier =
            () -> customPersistor.map(NodeParametersPersistor::getConfigPaths).orElseGet(ex::getDefaultConfigPaths);
        final var combinedWithLoaders = performCombineWithLoader(ex, withoutLoader, configPathsSupplier);
        return ex.postProcess(combinedWithLoaders, configPathsSupplier);
    }

    @SuppressWarnings("unchecked")
    private T performCombineWithLoader(final ExtractionMethods<T> ex, final T withoutLoader,
        final Supplier<String[][]> configPathsSupplier) {
        final List<ConfigMigration> deprecatedConfigs =
            ex.getAnnotation(Migration.class).map(InitializeWithDefaultConstructorUtil::createMigrator)
                .map(NodeParametersMigration::getConfigMigrations).orElse(List.of());
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        return ex.combineWithLoader(withoutLoader, deprecatedConfigs, configPathsSupplier);
    }

    interface ExtractionMethods<T> {
        <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass);

        T fromPersistor(NodeParametersPersistor<?> persistor);

        T getDefault();

        String[][] getDefaultConfigPaths();

        T combineWithLoader(T withoutLoader, List<ConfigMigration> deprecatedConfigs,
            Supplier<String[][]> configPathsSupplier);

        T postProcess(T result, Supplier<String[][]> configPathsProvider);

    }

    private final class TreeExtractionMethods implements ExtractionMethods<T> {
        private final Tree<Persistable> m_node;

        private TreeExtractionMethods(final Tree<Persistable> node) {
            m_node = node;
        }

        @Override
        public <A extends Annotation> Optional<A> getAnnotation(final Class<A> annotationClass) {
            return m_node.getTypeAnnotation(annotationClass);
        }

        @Override
        public T fromPersistor(final NodeParametersPersistor<?> persistor) {
            return getFromCustomPersistorForType(persistor, m_node);
        }

        /**
         * Per default, we persist by persisting all fields.
         */
        @Override
        public T getDefault() {
            return getForTree(m_node, child -> extractFromTreeNode(child));
        }

        /**
         * All sub paths. Note that returning an empty 2d array would result in the opposite, i.e. none of the sub
         * paths.
         */
        @Override
        public String[][] getDefaultConfigPaths() {
            return new String[][]{new String[]{}};
        }

        @Override
        public T combineWithLoader(final T withoutLoader, final List<ConfigMigration> deprecatedConfigs,
            final Supplier<String[][]> configPathsSupplier) {
            return combineWithConfigsDeprecationsForType(withoutLoader, deprecatedConfigs, configPathsSupplier, m_node);
        }

        @Override
        public T postProcess(final T result, final Supplier<String[][]> configPathsProvider) {
            final var relativePath = m_node.getTypeAnnotation(PersistWithin.class).map(PersistWithin::value);
            if (relativePath.isPresent()) {
                return rerouteForType(relativePath.get(), result, m_node);
            }
            return result;
        }
    }

    private final class TreeNodeExtractionMethods implements ExtractionMethods<T> {
        private final TreeNode<Persistable> m_node;

        private TreeNodeExtractionMethods(final TreeNode<Persistable> node) {
            m_node = node;
        }

        @Override
        public <A extends Annotation> Optional<A> getAnnotation(final Class<A> annotationClass) {
            return m_node.getAnnotation(annotationClass);
        }

        @Override
        public T fromPersistor(final NodeParametersPersistor<?> persistor) {
            return getFromCustomPersistor(persistor, m_node);
        }

        @Override
        public T getDefault() {
            if (m_node instanceof Tree<Persistable> tree) {
                return getNested(m_node, extractFromTree(tree));
            } else if (m_node instanceof ArrayParentNode<Persistable> arrayNode) {
                final var customArrayPersistorOpt =
                    arrayNode.getAnnotation(PersistArray.class).map(PersistArray::value);
                if (customArrayPersistorOpt.isPresent()) {
                    final var customArrayPersistor = customArrayPersistorOpt.get();
                    final var elementFieldPersistors = getElementFieldPersistors(arrayNode, customArrayPersistor);
                    final var customArrayPersistorInstance = InstantiationUtil.createInstance(customArrayPersistor);
                    return getForCustomArrayPersistor(arrayNode, customArrayPersistorInstance, elementFieldPersistors);
                }
                return getNested(m_node, getForArray(arrayNode, extractFromTree(arrayNode.getElementTree())));
            } else if (m_node instanceof LeafNode<Persistable> leaf) {
                return getForLeaf(leaf);
            }
            throw new NotImplementedException("Only tree, arrayParent and leaf exist when implementing this.");
        }

        private static Map<TreeNode<Persistable>, ElementFieldPersistor> getElementFieldPersistors(
            final ArrayParentNode<Persistable> arrayNode, final Class<? extends ArrayPersistor<?, ?>> arrayPersistor) {
            final var loadContextType =
                GenericTypeFinderUtil.getNthGenericType(arrayPersistor, ArrayPersistor.class, 0);
            final var saveDTOType = GenericTypeFinderUtil.getNthGenericType(arrayPersistor, ArrayPersistor.class, 1);
            final Map<TreeNode<Persistable>, ElementFieldPersistor> elementFieldPersistors = new HashMap<>();
            for (final var child : arrayNode.getElementTree().getChildren()) {
                final var elementFieldPersistor = getArrayElementPersistorType(child, loadContextType, saveDTOType);
                elementFieldPersistors.put(child, elementFieldPersistor);
            }
            return elementFieldPersistors;

        }

        private static ElementFieldPersistor getArrayElementPersistorType(final TreeNode<Persistable> arrayElementField,
            final Type loadContextType, final Type saveDTOType) {
            final var elementFieldPersistor =
                arrayElementField.getAnnotation(PersistArrayElement.class).map(PersistArrayElement::value)
                    .orElseThrow(() -> new IllegalStateException(String.format(
                        "When using a custom ArrayPersistor, the element tree's direct child %s must have a @%s"
                            + "annotation defining an %s.",
                        arrayElementField.getPath(), PersistArrayElement.class.getSimpleName(),
                        ElementFieldPersistor.class.getSimpleName())));
            validateElementFieldPersistorTypes(elementFieldPersistor, loadContextType, saveDTOType);
            return InstantiationUtil.createInstance(elementFieldPersistor);
        }

        private static void validateElementFieldPersistorTypes(
            final Class<? extends ElementFieldPersistor<?, ?, ?>> elementFieldPersistor, final Type loadContextType,
            final Type saveDTOType) {
            final var elementLoadContextType =
                GenericTypeFinderUtil.getNthGenericType(elementFieldPersistor, ElementFieldPersistor.class, 1);
            if (!elementLoadContextType.equals(loadContextType)) {
                throw new IllegalStateException(String.format(
                    "The ElementFieldPersistor %s's load context type %s does not match the"
                        + " ArrayPersistor's load context type %s.",
                    elementFieldPersistor.getName(), elementLoadContextType.getTypeName(),
                    loadContextType.getTypeName()));
            }
            final var elementSaveDTOType =
                GenericTypeFinderUtil.getNthGenericType(elementFieldPersistor, ElementFieldPersistor.class, 2);
            if (!elementSaveDTOType.equals(saveDTOType)) {
                throw new IllegalStateException(String.format(
                    "The ElementFieldPersistor %s's save DTO type %s does not match the"
                        + " ArrayPersistor's save DTO type %s.",
                    elementFieldPersistor.getName(), elementSaveDTOType.getTypeName(), saveDTOType.getTypeName()));
            }

        }

        @Override
        public String[][] getDefaultConfigPaths() {
            return new String[][]{{ConfigKeyUtil.getConfigKey(m_node)}};
        }

        @Override
        public T combineWithLoader(final T withoutLoader, final List<ConfigMigration> deprecatedConfigs,
            final Supplier<String[][]> configPathsSupplier) {
            return combineWithConfigsDeprecations(withoutLoader, deprecatedConfigs, configPathsSupplier, m_node);
        }

        @Override
        public T postProcess(final T result, final Supplier<String[][]> configPathsProvider) {
            return postProcessReroute(postProcessLoadDefault(result, configPathsProvider));
        }

        private T postProcessReroute(final T result) {
            final var relativePath = m_node.getAnnotation(PersistWithin.class).map(PersistWithin::value);
            if (relativePath.isPresent()) {
                return reroute(relativePath.get(), result, m_node);
            }
            return result;
        }

        private T postProcessLoadDefault(final T result, final Supplier<String[][]> configPathsProvider) {
            if (shouldLoadDefaultIfAbsent(configPathsProvider)) {
                return combineWithLoadDefault(result, configPathsProvider, m_node);
            }
            return result;
        }

        private boolean shouldLoadDefaultIfAbsent(final Supplier<String[][]> configPathsProvider) {
            if (m_node instanceof ArrayParentNode<Persistable> arrayParent
                && arrayParent.getAnnotation(PersistArray.class).isPresent()) {
                return false;
            }
            final var shouldFromFieldAnnotation =
                m_node.getAnnotation(Migrate.class).map(Migrate::loadDefaultIfAbsent).orElse(false).booleanValue();
            final var shouldFromClassAnnotation =
                m_node.getParentAnnotation(LoadDefaultsForAbsentFields.class).isPresent()
                    && m_node.getAnnotation(Migration.class).isEmpty();
            if (fieldWillBeIgnoredByRouting()) {
                if (shouldFromFieldAnnotation) {
                    throw new IllegalStateException(String.format(
                        "The field %s cannot have @%s with loadDefaultIfAbsent=true since it is routed out of the"
                            + " persistence tree via @%s.",
                        m_node.getPath(), Migrate.class.getSimpleName(), PersistWithin.class.getSimpleName()));
                }
                if (shouldFromClassAnnotation) {
                    return false;
                }
            }

            if (shouldFromFieldAnnotation) {
                final var fieldHasNoConfigPaths = configPathsProvider.get().length == 0;
                if (fieldHasNoConfigPaths) {
                    throw new IllegalStateException(String.format(
                        "The field %s cannot have @%s with loadDefaultIfAbsent=true since it has no config paths.",
                        m_node.getPath(), Migrate.class.getSimpleName()));
                }
                return true;
            }
            if (shouldFromClassAnnotation) {
                final var fieldHasNoConfigPaths = configPathsProvider.get().length == 0;
                return !fieldHasNoConfigPaths;
            }
            return false;
        }

        private boolean fieldWillBeIgnoredByRouting() {
            if (m_node instanceof Tree<Persistable> treeNode) {
                return treeNode.getTypeAnnotation(PersistWithin.class).map(PersistWithin::value)
                    .flatMap(TreeNodeExtractionMethods::getFirstParentPathSegment).isPresent();
            }
            return false;
        }

        private static Optional<String> getFirstParentPathSegment(final String[] relativePath) {
            return Arrays.stream(relativePath).findFirst().filter(s -> s.equals(".."));
        }
    }

}
