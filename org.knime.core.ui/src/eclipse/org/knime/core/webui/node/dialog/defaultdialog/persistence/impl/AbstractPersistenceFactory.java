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

import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.CreateNodeSettingsPersistorUtil.createInstance;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.BackwardsCompatibleLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DeprecatedSettingsLoadDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;

/**
 * An abstract factory which splits the logic on how to combine persistence logic (e.g. how to save) across the tree
 * structure.
 *
 * @param <T> the type of what this factory produces
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractPersistenceFactory<T> {

    protected abstract T getDefaultForLeaf(LeafNode<PersistableSettings> node);

    protected abstract T getFromCustomPersistor(NodeSettingsPersistor<?> nodeSettingsPersistor,
        TreeNode<PersistableSettings> treeNode);

    protected T getFromCustomTypePersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
        final Tree<PersistableSettings> tree) {
        return getFromCustomPersistor(nodeSettingsPersistor, tree);
    }

    protected abstract T combineTreeChildren(Tree<PersistableSettings> tree,
        Function<TreeNode<PersistableSettings>, T> childProperty);

    protected abstract T processArray(ArrayParentNode<PersistableSettings> arrayNode, T elementProperty);

    protected abstract T getNested(final TreeNode<PersistableSettings> node, final T property);

    /**
     * Per default, do not use the node.
     *
     * @param withoutLoader
     * @param configsDeprecations
     * @param configPaths
     * @param node
     * @return the combined result
     */
    protected T combineWithBackwardsCompatibleLoader(final T withoutLoader,
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
        final TreeNode<PersistableSettings> node) {
        return combineWithBackwardsCompatibleLoader(withoutLoader, configsDeprecations, configPaths);
    }

    /**
     * Per default, do not use the node.
     *
     * @param withoutLoader
     * @param configsDeprecations
     * @param configPaths
     * @param node
     * @return the combined result
     */
    protected T combineWithBackwardsCompatibleTypeLoader(final T withoutLoader,
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
        final Tree<PersistableSettings> node) {
        return combineWithBackwardsCompatibleLoader(withoutLoader, configsDeprecations, configPaths);
    }

    /**
     * Per default, do nothing here.
     *
     * @param withoutLoader
     * @param configsDeprecations
     * @param configPaths
     * @return the combined result
     */
    protected T combineWithBackwardsCompatibleLoader(final T withoutLoader,
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths) {
        return withoutLoader;
    }

    protected final T extractFromSettings(final Class<? extends PersistableSettings> settingsClass) {
        final var persistTree = new PersistTreeFactory().createTree(settingsClass);
        return extractFromTree(persistTree);
    }

    protected final T extractFromTree(final Tree<PersistableSettings> node) {
        final var extractionMethods = new ExtractionMethods<T>() {

            @Override
            public <A extends Annotation> Optional<A> getAnnotation(final Class<A> annotationClass) {
                return node.getTypeAnnotation(annotationClass);
            }

            @SuppressWarnings("unchecked")
            @Override
            public NodeSettingsPersistor
                constructPersistor(final Class<? extends NodeSettingsPersistor> persistorClass) {
                return createInstance(persistorClass, node.getType());
            }

            @Override
            public T fromPersistor(final NodeSettingsPersistor<?> persistor) {
                return getFromCustomTypePersistor(persistor, node);
            }

            /**
             * Per default, we persist by persisting all fields.
             */
            @Override
            public T getDefault() {
                return combineTreeChildren(node, child -> extractFromTreeNode(child));
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
            public T combineWithLoader(final T withoutLoader, final List<ConfigsDeprecation> deprecatedConfigs,
                final Supplier<String[][]> configPathsSupplier) {
                return combineWithBackwardsCompatibleTypeLoader(withoutLoader, deprecatedConfigs, configPathsSupplier,
                    node);
            }

        };
        return performExtraction(extractionMethods);
    }

    protected final T extractFromTreeNode(final TreeNode<PersistableSettings> node) { //TODO: Deduplicate
        final var extractionMethods = new ExtractionMethods<T>() {

            @Override
            public <A extends Annotation> Optional<A> getAnnotation(final Class<A> annotationClass) {
                return node.getAnnotation(annotationClass);
            }

            @Override
            @SuppressWarnings("unchecked")
            public NodeSettingsPersistor
                constructPersistor(final Class<? extends NodeSettingsPersistor> persistorClass) {
                return createInstance(persistorClass, node.getType(), node.getName().orElse(""));
            }

            @Override
            public T fromPersistor(final NodeSettingsPersistor<?> persistor) {
                return getFromCustomPersistor(persistor, node);
            }

            @Override
            public T getDefault() {
                if (node instanceof Tree<PersistableSettings> tree) {
                    return getNested(node, extractFromTree(tree));
                } else if (node instanceof ArrayParentNode<PersistableSettings> arrayNode) {
                    return getNested(node, processArray(arrayNode, extractFromTree(arrayNode.getElementTree())));
                } else if (node instanceof LeafNode<PersistableSettings> leaf) {
                    return getDefaultForLeaf(leaf);
                }
                throw new NotImplementedException("Only tree, arrayParent and leaf exist when implementing this.");
            }

            @Override
            public String[][] getDefaultConfigPaths() {
                return new String[][]{{ConfigKeyUtil.getConfigKey(node)}};
            }

            @Override
            public T combineWithLoader(final T withoutLoader, final List<ConfigsDeprecation> deprecatedConfigs,
                final Supplier<String[][]> configPathsSupplier) {
                return combineWithBackwardsCompatibleLoader(withoutLoader, deprecatedConfigs, configPathsSupplier,
                    node);
            }

        };

        return performExtraction(extractionMethods);

    }

    interface ExtractionMethods<T> {
        <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass);

        NodeSettingsPersistor constructPersistor(Class<? extends NodeSettingsPersistor> persistorClass);

        T fromPersistor(NodeSettingsPersistor<?> persistor);

        T getDefault();

        String[][] getDefaultConfigPaths();

        T combineWithLoader(T withoutLoader, List<ConfigsDeprecation> deprecatedConfigs,
            Supplier<String[][]> configPathsSupplier);

    }

    private T performExtraction(final ExtractionMethods<T> ex) {
        final var customPersistor = ex.getAnnotation(Persistor.class).map(Persistor::value).map(ex::constructPersistor);
        final var withoutLoader = customPersistor.map(ex::fromPersistor).orElseGet(ex::getDefault);
        final var deprecatedConfigs = getConfigsDeprecations(ex.getAnnotation(BackwardsCompatibleLoader.class));
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        final Supplier<String[][]> configPathsSupplier =
            () -> customPersistor.map(NodeSettingsPersistor::getConfigPaths).orElseGet(ex::getDefaultConfigPaths);
        return ex.combineWithLoader(withoutLoader, deprecatedConfigs, configPathsSupplier);
    }

    @SuppressWarnings("unchecked")
    private static List<ConfigsDeprecation>
        getConfigsDeprecations(final Optional<BackwardsCompatibleLoader> annotation) {
        return annotation.map(BackwardsCompatibleLoader::value).map(InstantiationUtil::createInstance)
            .map(DeprecatedSettingsLoadDefinition::getConfigsDeprecations).orElse(List.of());
    }

    @FunctionalInterface
    public interface SettingsSaver {
        void save(Object obj, NodeSettingsWO nodeSettings);
    }

    @FunctionalInterface
    interface GetConfigMappings {
        ConfigMappings getConfigMappings(Object obj);
    }

}
