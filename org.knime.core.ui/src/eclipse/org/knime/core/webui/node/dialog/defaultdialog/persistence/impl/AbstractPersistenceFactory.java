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

import static org.knime.core.webui.node.dialog.configmapping.NodeSettingsAtPathUtil.hasPath;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.BackwardsCompatibleLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DeprecatedSettingsLoadDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NewSettingsMissingMatcher;
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

    abstract T getDefault(TreeNode<PersistableSettings> node);

    abstract T getFromCustomPersistor(NodeSettingsPersistor<?> nodeSettingsPersistor);

    abstract T combineTreeChildren(Tree<PersistableSettings> tree,
        Function<TreeNode<PersistableSettings>, T> childProperty);

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

    private static String[][] getDefaultConfigPaths(final TreeNode<PersistableSettings> node) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        return CustomPersistorUtil
            .getNonNullConfigPaths(DefaultFieldNodeSettingsPersistorFactory.createDefaultPersistor(node, configKey));
    }

    T extractFromTree(final Tree<PersistableSettings> node) {
        final var customTypePersistor = node.getTypeAnnotation(Persistor.class)
            .map(persistorAnnotation -> createFromPersistorAnnotation(persistorAnnotation, node.getType()));
        final var withoutLoader = customTypePersistor.map(this::getFromCustomPersistor)
            .orElseGet(() -> combineTreeChildren(node, this::extractFromTreeNode));

        final var deprecatedConfigs = getConfigsDeprecations(node.getTypeAnnotation(BackwardsCompatibleLoader.class));
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        final Supplier<String[][]> defaultConfigPathsSupplier = () -> getDefaultConfigPaths(node);
        final Supplier<String[][]> configPathsSupplier =
            () -> customTypePersistor.map(NodeSettingsPersistor::getConfigPaths).orElseGet(defaultConfigPathsSupplier);
        return combineWithBackwardsCompatibleLoader(withoutLoader, deprecatedConfigs, configPathsSupplier);
    }

    @SuppressWarnings("unchecked")
    private static List<ConfigsDeprecation>
        getConfigsDeprecations(final Optional<BackwardsCompatibleLoader> annotation) {
        return annotation.map(BackwardsCompatibleLoader::value).map(InstantiationUtil::createInstance)
            .map(DeprecatedSettingsLoadDefinition::getConfigsDeprecations).orElse(List.of());
    }

    T extractFromTreeNode(final TreeNode<PersistableSettings> node) { //TODO: Deduplicate
        final var customPersistor = node.getAnnotation(Persistor.class)
            .map(persistorAnnotation -> createFromPersistorAnnotation(persistorAnnotation, node.getType(),
                node.getName().orElse("")));
        final var withoutLoader = customPersistor.map(this::getFromCustomPersistor).orElseGet(() -> getDefault(node));
        final var deprecatedConfigs = getConfigsDeprecations(node.getAnnotation(BackwardsCompatibleLoader.class));
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        final Supplier<String[][]> defaultConfigPathsSupplier = () -> getDefaultConfigPaths(node);
        final Supplier<String[][]> configPathsSupplier =
            () -> customPersistor.map(NodeSettingsPersistor::getConfigPaths).orElseGet(defaultConfigPathsSupplier);
        return combineWithBackwardsCompatibleLoader(withoutLoader, deprecatedConfigs, configPathsSupplier);
    }

    private static <S> NodeSettingsPersistor<S> createFromPersistorAnnotation(final Persistor persistorAnnotation,
        final Class<S> settingsClass) {
        var persistorClass = persistorAnnotation.value();
        return CreateNodeSettingsPersistorUtil.createInstance(persistorClass, settingsClass);
    }

    private static <S> NodeSettingsPersistor<S> createFromPersistorAnnotation(final Persistor persistorAnnotation,
        final Class<S> settingsClass, final String fieldName) {
        var persistorClass = persistorAnnotation.value();
        return CreateNodeSettingsPersistorUtil.createInstance(persistorClass, settingsClass, fieldName);
    }

    static final class NodeSettingsPersistorFactory extends AbstractPersistenceFactory<SettingsLoader> {

        @Override
        SettingsLoader getDefault(final TreeNode<PersistableSettings> node) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            return DefaultFieldNodeSettingsPersistorFactory.createDefaultPersistor(node, configKey)::load;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected final SettingsLoader combineWithBackwardsCompatibleLoader(final SettingsLoader withoutLoader,
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPathsSupplier) {

            return settings -> {
                for (final var configDeprecation : configsDeprecations) {
                    final var matcher = configDeprecation.getMatcher();
                    final var loader = configDeprecation.getLoader();

                    if (matcher instanceof NewSettingsMissingMatcher) {
                        final var configPaths = configPathsSupplier.get();
                        if (configPaths == null || configPaths.length == 0) {
                            throw new IllegalStateException(
                                "There exists a custom persistor without or with empty config paths but "
                                    + "a deprecation matcher was set via 'withNewSettingsMissingMatcher' is used."
                                    + "Either supply a custom matcher via 'withMatcher' or define config paths.");
                        }
                        if (Arrays.stream(configPaths).map(Arrays::asList).map(ConfigPath::new)
                            .allMatch(configPath -> !hasPath(settings, configPath))) {
                            return loader.apply(settings);
                        }

                    } else if (matcher.test(settings)) {
                        return loader.apply(settings);
                    }
                }
                return withoutLoader.apply(settings);

            };
        }

        @Override
        SettingsLoader getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor) {
            return nodeSettingsPersistor::load;
        }

        @Override
        SettingsLoader combineTreeChildren(final Tree<PersistableSettings> tree,
            final Function<TreeNode<PersistableSettings>, SettingsLoader> getLoader) {
            return settings -> {
                final var loaded = createDefaultSettings(tree);
                for (var child : tree.getChildren()) {
                    try {
                        final var loadedChildValue = getLoader.apply(child).apply(settings);
                        child.setInParentValue(loaded, loadedChildValue);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
                return loaded;
            };
        }

    }

    public static Object createDefaultSettings(final Tree<PersistableSettings> tree) {
        final var settingsClass = tree.getType();
        return ReflectionUtil.createInstance(settingsClass).orElseThrow(() -> new IllegalArgumentException(
            String.format("The provided PersistableSettings '%s' don't provide an empty constructor.", settingsClass)));
    }

}
