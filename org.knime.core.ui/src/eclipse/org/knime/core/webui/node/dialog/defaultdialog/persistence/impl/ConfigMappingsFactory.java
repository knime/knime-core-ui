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
 *   Dec 19, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsSaver;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigMappingsFactory.GetConfigMappings;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;

/**
 *
 * Factory for creating {@link ConfigMappings} from {@link PersistableSettings} classes.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ConfigMappingsFactory extends PersistenceFactory<GetConfigMappings> {
    static final NodeLogger LOGGER = NodeLogger.getLogger(ConfigMappingsFactory.class);

    private static final ConfigMappingsFactory INSTANCE = new ConfigMappingsFactory();

    private ConfigMappingsFactory() {
        // use getInstance
    }

    static ConfigMappingsFactory getInstance() {
        return INSTANCE;
    }

    /**
     * This method extracts the config mappings (tree) from a settings class.
     *
     * @param settingsClass
     * @param settingsValue
     * @return the config mappings
     */
    public static ConfigMappings getConfigMappings(final Class<? extends PersistableSettings> settingsClass,
        final Object settingsValue) {
        return getInstance().extractFromSettings(settingsClass).getConfigMappings(settingsValue);
    }

    @Override
    protected GetConfigMappings getForLeaf(final LeafNode<PersistableSettings> node) {
        return obj -> new ConfigMappings(List.of());
    }

    @Override
    protected GetConfigMappings getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
        final TreeNode<PersistableSettings> node) {
        return obj -> new ConfigMappings(List.of());
    }

    @Override
    protected GetConfigMappings getForTree(final Tree<PersistableSettings> tree,
        final Function<TreeNode<PersistableSettings>, GetConfigMappings> childProperty) {
        return obj -> new ConfigMappings(
            tree.getChildren().stream().map(child -> fromChild(childProperty, obj, child)).toList());

    }

    private static ConfigMappings fromChild(
        final Function<TreeNode<PersistableSettings>, GetConfigMappings> childProperty, final Object obj,
        final TreeNode<PersistableSettings> child) {
        return childProperty.apply(child).getConfigMappings(child.getFromParentValue(obj));
    }

    @Override
    protected GetConfigMappings getForArray(final ArrayParentNode<PersistableSettings> arrayNode,
        final GetConfigMappings elementProperty) {
        return obj -> {
            final var array = (Object[])obj;
            return new ConfigMappings(IntStream.range(0, array.length)
                .mapToObj(
                    i -> new ConfigMappings(Integer.toString(i), List.of(elementProperty.getConfigMappings(array[i]))))
                .toList());
        };
    }

    @Override
    protected GetConfigMappings getNested(final TreeNode<PersistableSettings> node, final GetConfigMappings property) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        return obj -> new ConfigMappings(configKey, List.of(property.getConfigMappings(obj)));
    }

    @Override
    protected GetConfigMappings combineWithConfigsDeprecations(final GetConfigMappings withoutLoader,
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
        final TreeNode<PersistableSettings> node) {
        final var saver = SettingsSaverFactory.getInstance().extractFromTreeNode(node);
        final var newConfigMappings = configsDeprecationsToConfigMappings(configsDeprecations, configPaths, saver);
        return concatenateMappings(withoutLoader, newConfigMappings);
    }

    @Override
    protected GetConfigMappings combineWithConfigsDeprecationsForType(final GetConfigMappings withoutLoader,
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
        final Tree<PersistableSettings> node) {
        final var saver = SettingsSaverFactory.getInstance().extractFromTree(node);
        final var newConfigMappings = configsDeprecationsToConfigMappings(configsDeprecations, configPaths, saver);
        return concatenateMappings(withoutLoader, newConfigMappings);
    }

    @Override
    protected GetConfigMappings combineWithLoadDefault(final GetConfigMappings withoutLoader,
        final Supplier<String[][]> configPaths, final TreeNode<PersistableSettings> node) {
        final var saver = SettingsSaverFactory.getInstance().extractFromTreeNode(node);
        GetConfigMappings defaultValueConfigMapping = getDefaultConfigMapping(configPaths, node, saver);
        return concatenateMappings(withoutLoader, defaultValueConfigMapping);

    }

    private static GetConfigMappings getDefaultConfigMapping(final Supplier<String[][]> configPathsProvider,
        final TreeNode<PersistableSettings> node, final SettingsSaver saver) {
        final var configPaths = configPathsProvider.get();
        final UnaryOperator<NodeSettingsRO> setDefault = settings -> {
            final var newSettings = new NodeSettings("newSettings");
            final var defaultValue = CreateDefaultsUtil.createDefaultSettingsFieldValue(node);
            saver.save(defaultValue, newSettings);
            return newSettings;
        };
        return obj -> new ConfigMappings(List.of(), toConfigPaths(configPaths), setDefault);
    }

    private static <S> GetConfigMappings configsDeprecationsToConfigMappings(
        final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
        final SettingsSaver<S> saver) {
        return obj -> new ConfigMappings(configsDeprecations.stream().map(configsDeprecation -> new ConfigMappings( //
            configsDeprecation.getDeprecatedConfigPaths(), //
            toConfigPaths(configPaths.get()), //
            settings -> {
                final S fromPrevious = (S)loadOrDefault(settings, configsDeprecation.getLoader(), obj);
                final var newSettings = new NodeSettings("newSettings");
                saver.save(fromPrevious, newSettings);
                return newSettings;
            })//
        ).toList());
    }

    private static GetConfigMappings concatenateMappings(final GetConfigMappings first,
        final GetConfigMappings second) {
        return obj -> new ConfigMappings(List.of(first.getConfigMappings(obj), second.getConfigMappings(obj)));
    }

    private static <T> T loadOrDefault(final NodeSettingsRO settings, final SettingsLoader<T> deprecationLoader,
        final T obj) {
        try {
            return deprecationLoader.load(settings);
        } catch (InvalidSettingsException ex) {
            LOGGER
                .warn(String.format("Error when trying to load from previous settings when modifying settings on save. "
                    + "Using the saved settings instead. Exception: %s", ex));
            return obj;
        }
    }

    private static Collection<ConfigPath> toConfigPaths(final String[][] nonNullPaths) {
        return Arrays.stream(nonNullPaths).map(Arrays::asList).map(ConfigPath::new).toList();
    }

    @FunctionalInterface
    interface GetConfigMappings {
        ConfigMappings getConfigMappings(Object obj);
    }

}
