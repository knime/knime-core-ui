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

import static org.knime.core.webui.node.dialog.configmapping.NodeSettingsAtPathUtil.hasPath;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NewSettingsMissingMatcher;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;

/**
 * Factory responsible for loading settings
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
public final class SettingsLoaderFactory extends PersistenceFactory<SettingsLoader> {

    private static final SettingsLoaderFactory INSTANCE = new SettingsLoaderFactory();

    private SettingsLoaderFactory() {
        // use getInstance
    }

    static SettingsLoaderFactory getInstance() {
        return INSTANCE;
    }

    /**
     * @param settingsClass the class of the to be loaded settings
     * @param nodeSettings to load from
     * @return the loaded settings
     * @throws InvalidSettingsException
     * @param <S> the type of the settings
     */
    public static <S extends PersistableSettings> S loadSettings(final Class<S> settingsClass,
        final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        return getSettingsLoader(settingsClass).load(nodeSettings);
    }

    /**
     * @param settingsClass
     * @return the loader for the given settings class
     * @param <S> the type of the settings
     */
    @SuppressWarnings("unchecked")
    public static <S extends PersistableSettings> SettingsLoader<S> getSettingsLoader(final Class<S> settingsClass) {
        return getInstance().extractFromSettings(settingsClass);
    }

    @Override
    protected SettingsLoader getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
        final TreeNode<PersistableSettings> node) {
        return nodeSettingsPersistor;
    }

    @Override
    protected SettingsLoader getForTree(final Tree<PersistableSettings> tree,
        final Function<TreeNode<PersistableSettings>, SettingsLoader> getLoader) {
        return settings -> {
            final var loaded = CreateDefaultsUtil.createDefaultSettings(tree);
            for (var child : tree.getChildren()) {
                final var loadedChildValue = getLoader.apply(child).load(settings);
                child.setInParentValue(loaded, loadedChildValue);
            }
            return loaded;
        };
    }

    private static final Pattern IS_DIGIT = Pattern.compile("^\\d+$");

    @Override
    protected SettingsLoader getForArray(final ArrayParentNode<PersistableSettings> arrayNode,
        final SettingsLoader elementLoader) {
        return settings -> {
            int size = (int)settings.keySet().stream().filter(s -> IS_DIGIT.matcher(s).matches()).count();
            var values = (Object[])Array.newInstance(arrayNode.getElementTree().getType(), size);
            for (int i = 0; i < size; i++) {
                values[i] = elementLoader.load(settings.getNodeSettings(Integer.toString(i)));
            }
            return values;
        };
    }

    @Override
    protected SettingsLoader getForLeaf(final LeafNode<PersistableSettings> node) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        return DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey);
    }

    @Override
    protected SettingsLoader getNested(final TreeNode<PersistableSettings> node, final SettingsLoader loader) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        return settings -> loader.load(settings.getNodeSettings(configKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final SettingsLoader combineWithConfigsDeprecations(final SettingsLoader withoutLoader,
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
                        return loader.load(settings);
                    }

                } else if (matcher.test(settings)) {
                    return loader.load(settings);
                }
            }
            return withoutLoader.load(settings);

        };
    }

    @Override
    protected SettingsLoader combineWithLoadDefault(final SettingsLoader existingLoader,
        final Supplier<String[][]> configPathsSupplier, final TreeNode<PersistableSettings> node) {
        return settings -> {
            final var configPaths = configPathsSupplier.get();
            if (Stream.of(configPaths).anyMatch(path -> hasPath(settings, new ConfigPath(Arrays.asList(path))))) {
                return existingLoader.load(settings);
            } else {
                return CreateDefaultsUtil.createDefaultSettingsFieldValue(node);
            }
        };
    }

}
