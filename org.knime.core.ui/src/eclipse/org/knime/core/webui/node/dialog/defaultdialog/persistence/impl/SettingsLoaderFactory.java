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
import static org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy.fromIdentifierConsistent;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.CLASS_ID_CFG_KEY;
import static org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil.createInstance;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NewSettingsMissingMatcher;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.ParametersLoader;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistable;

/**
 * Factory responsible for loading settings
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
public final class SettingsLoaderFactory extends PersistenceFactory<ParametersLoader> {

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
    public static <S extends Persistable> S loadSettings(final Class<S> settingsClass,
        final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        return createSettingsLoader(settingsClass).load(nodeSettings);
    }

    /**
     * @param settingsClass
     * @return the loader for the given settings class
     * @param <S> the type of the settings
     */
    @SuppressWarnings("unchecked")
    public static <S extends Persistable> ParametersLoader<S> createSettingsLoader(final Class<S> settingsClass) {
        return getInstance().extractFromSettings(settingsClass);
    }

    @Override
    protected ParametersLoader getFromCustomPersistor(final NodeParametersPersistor<?> nodeSettingsPersistor,
        final TreeNode<Persistable> node) {
        return nodeSettingsPersistor;
    }

    @Override
    protected ParametersLoader getForTree(final Tree<Persistable> tree,
        final Function<TreeNode<Persistable>, ParametersLoader> getLoader) {
        return settings -> {
            if (tree.isDynamic()) {
                final var classId = settings.getString(CLASS_ID_CFG_KEY, "");
                if (classId == null) {
                    return null;
                }
                final var dynamicParametersProvider = createInstance(
                    tree.getAnnotation(DynamicParameters.class).orElseThrow(IllegalStateException::new).value());
                final var parametersClass =
                    fromIdentifierConsistent(dynamicParametersProvider.getClassIdStrategy(), classId);
                if (parametersClass == null) {
                    throw new InvalidSettingsException("Could not find class '" + classId + "' for dynamic settings.");
                }
                final var loader = SettingsLoaderFactory.createSettingsLoader(parametersClass);
                return loader.load(settings);

            }
            final var loaded = CreateDefaultsUtil.createDefaultSettings(tree);
            for (var child : tree.getChildren()) {
                final var loadedChildValue = getLoader.apply(child).load(settings);
                child.setInParentValue(loaded, loadedChildValue);
            }
            return loaded;
        };
    }

    @Override
    protected ParametersLoader getForArray(final ArrayParentNode<Persistable> arrayNode,
        final ParametersLoader elementLoader) {
        return settings -> SettingsLoaderArrayParentUtil.instantiateFromSettings(arrayNode, settings,
            i -> elementLoader.load(settings.getNodeSettings(Integer.toString(i))));
    }

    @Override
    protected ParametersLoader getForLeaf(final LeafNode<Persistable> node) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        return DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey);
    }

    @Override
    protected ParametersLoader getNested(final TreeNode<Persistable> node, final ParametersLoader loader) {
        final var configKey = ConfigKeyUtil.getConfigKey(node);
        final var doNotNest = hasPersistWithinGoingUp(node);
        return settings -> loader.load(doNotNest ? settings : settings.getNodeSettings(configKey));
    }

    static boolean hasPersistWithinGoingUp(final TreeNode<Persistable> node) {
        if (node instanceof Tree<Persistable> tree) {
            return tree.getTypeAnnotation(PersistWithin.class).map(PersistWithin::value).map(Arrays::stream)
                .flatMap(Stream::findFirst).filter(firstKey -> "..".equals(firstKey)).isPresent();
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final ParametersLoader combineWithConfigsDeprecations(final ParametersLoader withoutLoader,
        final List<ConfigMigration> configsDeprecations, final Supplier<String[][]> configPathsSupplier) {

        return settings -> {
            for (final var configDeprecation : configsDeprecations) {
                final var matcher = configDeprecation.getMatcher();
                final var loader = configDeprecation.getLoader();

                if (matcher instanceof NewSettingsMissingMatcher) {
                    final var configPaths = configPathsSupplier.get();
                    if (configPaths == null || configPaths.length == 0) {
                        throw new IllegalStateException(
                            "There exists a custom persistor without or with empty config paths but "
                                + "a migration was defined without specifying deprecated config keys"
                                + " and without specifying a matcher."
                                + "Either supply a custom matcher via 'withMatcher', "
                                + "specify deprecated config paths using 'withDeprecatedConfigPaths' "
                                + "or define config paths within the custom persistor.");
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
    protected ParametersLoader combineWithLoadDefault(final ParametersLoader existingLoader,
        final Supplier<String[][]> configPathsSupplier, final TreeNode<Persistable> node) {
        return settings -> {
            final var configPaths = configPathsSupplier.get();
            if (Stream.of(configPaths).anyMatch(path -> hasPath(settings, new ConfigPath(Arrays.asList(path))))) {
                return existingLoader.load(settings);
            } else {
                return CreateDefaultsUtil.createDefaultSettingsFieldValue(node);
            }
        };
    }

    @Override
    protected ParametersLoader reroute(final String[] relativePath, final ParametersLoader property,
        final TreeNode<Persistable> node) {
        return settings -> {
            for (String key : relativePath) {
                if ("..".equals(key)) {
                    settings = (NodeSettings)settings.getParent();
                    if (settings == null) {
                        throw new InvalidSettingsException(
                            "Could not go to parent settings. Reached top of settings tree.");
                    }
                } else {
                    settings = settings.getNodeSettings(key);
                }
            }
            return property.load(settings);
        };
    }

    /**
     * Since the first ".." key is already handled in {@link #getNested}, we need to ignore it here. {@inheritDoc}
     */
    @Override
    protected ParametersLoader rerouteForType(final String[] relativePath, final ParametersLoader property,
        final Tree<Persistable> node) {
        var relativePathCorrected = correctRelativePath(relativePath);
        return reroute(relativePathCorrected, property, node);
    }

    static String[] correctRelativePath(final String[] relativePath) {
        if (relativePath.length > 0 && "..".equals(relativePath[0])) {
            return Arrays.copyOfRange(relativePath, 1, relativePath.length);
        }
        return relativePath;
    }

}
