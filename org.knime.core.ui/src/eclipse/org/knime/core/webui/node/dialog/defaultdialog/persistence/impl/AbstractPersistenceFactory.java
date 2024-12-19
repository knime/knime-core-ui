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
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.CreateNodeSettingsPersistorUtil.createInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation.DeprecationLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.BackwardsCompatibleLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DeprecatedSettingsLoadDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NewSettingsMissingMatcher;
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

    public final T extractFromTree(final Tree<PersistableSettings> node) {
        final var customTypePersistor = node.getTypeAnnotation(Persistor.class)
            .map(persistorAnnotation -> createFromPersistorAnnotation(persistorAnnotation, node.getType()));
        final var withoutLoader = customTypePersistor.map(persistor -> getFromCustomTypePersistor(persistor, node))
            .orElseGet(() -> combineTreeChildren(node, this::extractFromTreeNode));

        final var deprecatedConfigs = getConfigsDeprecations(node.getTypeAnnotation(BackwardsCompatibleLoader.class));
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        final Supplier<String[][]> configPathsSupplier =
            () -> customTypePersistor.map(NodeSettingsPersistor::getConfigPaths).orElse(new String[][]{new String[]{}});
        return combineWithBackwardsCompatibleTypeLoader(withoutLoader, deprecatedConfigs, configPathsSupplier, node);
    }

    @SuppressWarnings("unchecked")
    private static List<ConfigsDeprecation>
        getConfigsDeprecations(final Optional<BackwardsCompatibleLoader> annotation) {
        return annotation.map(BackwardsCompatibleLoader::value).map(InstantiationUtil::createInstance)
            .map(DeprecatedSettingsLoadDefinition::getConfigsDeprecations).orElse(List.of());
    }

    final T extractFromTreeNode(final TreeNode<PersistableSettings> node) { //TODO: Deduplicate
        final var customPersistor = node.getAnnotation(Persistor.class)
            .map(persistorAnnotation -> createFromPersistorAnnotation(persistorAnnotation, node.getType(),
                node.getName().orElse("")));
        final var withoutLoader =
            customPersistor.map(persistor -> getFromCustomPersistor(persistor, node)).orElseGet(() -> getDefault(node));
        final var deprecatedConfigs = getConfigsDeprecations(node.getAnnotation(BackwardsCompatibleLoader.class));
        if (deprecatedConfigs.isEmpty()) {
            return withoutLoader;
        }
        final Supplier<String[][]> defaultConfigPathsSupplier =
            () -> new String[][]{{ConfigKeyUtil.getConfigKey(node)}};
        final Supplier<String[][]> configPathsSupplier =
            () -> customPersistor.map(NodeSettingsPersistor::getConfigPaths).orElseGet(defaultConfigPathsSupplier);
        //return combineWithBackwardsCompatibleLoader(withoutLoader, deprecatedConfigs, configPathsSupplier, node);

        final var extractionParams = new ExtractionParams<T>() {

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
                } else {
                    return extractFromTreeNode(node);
                }
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

    }

    interface ExtractionParams<T> {
        <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass);

        NodeSettingsPersistor constructPersistor(Class<? extends NodeSettingsPersistor> persistorClass);

        T fromPersistor(NodeSettingsPersistor<?> persistor);

        T getDefault();

        String[][] getDefaultConfigPaths();

        T combineWithLoader(T withoutLoader, List<ConfigsDeprecation> deprecatedConfigs,
            Supplier<String[][]> configPathsSupplier);

    }

    private T performExtraction(final ExtractionParams<T> ex,
        final Function<Class<? extends NodeSettingsPersistor<?>>, NodeSettingsPersistor<?>> constructPersistor) {
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

    private static <S> NodeSettingsPersistor<S> createFromPersistorAnnotation(final Persistor persistorAnnotation,
        final Class<S> settingsClass) {
        var persistorClass = persistorAnnotation.value();
        return CreateNodeSettingsPersistorUtil.createInstance(persistorClass, settingsClass);
    }

    private static <S> NodeSettingsPersistor<S> createFromPersistorAnnotation(
        final Class<? extends NodeSettingsPersistor> persistorClass, final Class<S> settingsClass,
        final String fieldName) {
        return createInstance(persistorClass, settingsClass, fieldName);
    }

    static final class SettingsLoaderFactory extends AbstractPersistenceFactory<SettingsLoader> {

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
        protected SettingsLoader getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
            final TreeNode<PersistableSettings> node) {
            return nodeSettingsPersistor::load;
        }

        @Override
        protected SettingsLoader combineTreeChildren(final Tree<PersistableSettings> tree,
            final Function<TreeNode<PersistableSettings>, SettingsLoader> getLoader) {
            return settings -> {
                final var loaded = createDefaultSettings(tree);
                for (var child : tree.getChildren()) {
                    final var loadedChildValue = getLoader.apply(child).apply(settings);
                    child.setInParentValue(loaded, loadedChildValue);
                }
                return loaded;
            };
        }

        private static final Pattern IS_DIGIT = Pattern.compile("^\\d+$");

        @Override
        protected SettingsLoader processArray(final ArrayParentNode<PersistableSettings> arrayNode,
            final SettingsLoader elementLoader) {
            return settings -> {
                int size = (int)settings.keySet().stream().filter(s -> IS_DIGIT.matcher(s).matches()).count();
                var values = (Object[])Array.newInstance(arrayNode.getElementTree().getType(), size);
                for (int i = 0; i < size; i++) {
                    values[i] = elementLoader.apply(settings.getNodeSettings(Integer.toString(i)));
                }
                return values;
            };
        }

        @Override
        protected SettingsLoader getDefaultForLeaf(final LeafNode<PersistableSettings> node) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            return DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey)::load;
        }

        @Override
        protected SettingsLoader getNested(final TreeNode<PersistableSettings> node, final SettingsLoader loader) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            return settings -> loader.apply(settings.getNodeSettings(configKey));
        }

    }

    @FunctionalInterface
    interface SettingsSaver {
        void save(Object obj, NodeSettingsWO nodeSettings);
    }

    static final class SettingsSaverFactory extends AbstractPersistenceFactory<SettingsSaver> {

        @Override
        protected SettingsSaver getDefaultForLeaf(final LeafNode<PersistableSettings> node) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            final var defaultPersistor = DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey); // TODO: Maybe move this to the abstract factory??? Do we need this method at all?
            return (obj, settings) -> uncheckedSave(defaultPersistor, obj, settings);
        }

        @SuppressWarnings("unchecked")
        private static <T> void uncheckedSave(final NodeSettingsPersistor<T> persistor, final Object value,
            final NodeSettingsWO nodeSettings) {
            persistor.save((T)value, nodeSettings);
        }

        @Override
        protected SettingsSaver getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
            final TreeNode<PersistableSettings> node) {
            return (obj, settings) -> uncheckedSave(nodeSettingsPersistor, obj, settings);
        }

        @Override
        protected SettingsSaver combineTreeChildren(final Tree<PersistableSettings> tree,
            final Function<TreeNode<PersistableSettings>, SettingsSaver> childProperty) {
            return (obj, settings) -> {
                for (final var child : tree.getChildren()) {
                    final var childValue = child.getFromParentValue(obj);
                    childProperty.apply(child).save(childValue, settings);
                }
            };
        }

        @Override
        protected SettingsSaver processArray(final ArrayParentNode<PersistableSettings> arrayNode,
            final SettingsSaver elementProperty) {
            return (obj, settings) -> {
                final var array = (Object[])obj;
                for (int i = 0; i < array.length; i++) {
                    elementProperty.save(array[i], settings.addNodeSettings(Integer.toString(i)));
                }
            };
        }

        @Override
        protected SettingsSaver getNested(final TreeNode<PersistableSettings> node, final SettingsSaver property) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            return (obj, settings) -> property.save(obj, settings.addNodeSettings(configKey));
        }

    }

    @FunctionalInterface
    interface GetConfigMappings {
        ConfigMappings getConfigMappings(Object obj);
    }

    static final class ConifgMappingsFactory extends AbstractPersistenceFactory<GetConfigMappings> {
        final static NodeLogger LOGGER = NodeLogger.getLogger(ConifgMappingsFactory.class);

        @Override
        protected GetConfigMappings getDefaultForLeaf(final LeafNode<PersistableSettings> node) {
            return obj -> new ConfigMappings(List.of());
        }

        @Override
        protected GetConfigMappings getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
            final TreeNode<PersistableSettings> node) {
            return obj -> new ConfigMappings(List.of());
        }

        @Override
        protected GetConfigMappings combineTreeChildren(final Tree<PersistableSettings> tree,
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
        protected GetConfigMappings processArray(final ArrayParentNode<PersistableSettings> arrayNode,
            final GetConfigMappings elementProperty) {
            return obj -> {
                final var array = (Object[])obj;
                return new ConfigMappings(IntStream.range(0, array.length).mapToObj(
                    i -> new ConfigMappings(Integer.toString(i), List.of(elementProperty.getConfigMappings(array[i]))))
                    .toList());
            };
        }

        @Override
        protected GetConfigMappings getNested(final TreeNode<PersistableSettings> node,
            final GetConfigMappings property) {
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            return obj -> new ConfigMappings(configKey, List.of(property.getConfigMappings(obj)));
        }

        @Override
        protected GetConfigMappings combineWithBackwardsCompatibleLoader(final GetConfigMappings withoutLoader,
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
            final TreeNode<PersistableSettings> node) {
            final var saver = new SettingsSaverFactory().extractFromTreeNode(node);
            final var newConfigMappings = combineWithBackwardsCompatibleLoader(configsDeprecations, configPaths, saver);
            return concatinateMappings(withoutLoader, newConfigMappings);
        }

        @Override
        protected GetConfigMappings combineWithBackwardsCompatibleTypeLoader(final GetConfigMappings withoutLoader,
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
            final Tree<PersistableSettings> node) {
            final var saver = new SettingsSaverFactory().extractFromTree(node);
            final var newConfigMappings = combineWithBackwardsCompatibleLoader(configsDeprecations, configPaths, saver);
            return concatinateMappings(withoutLoader, newConfigMappings);
        }

        @SuppressWarnings("unchecked")
        private static GetConfigMappings combineWithBackwardsCompatibleLoader(
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
            final SettingsSaver saver) {
            return obj -> new ConfigMappings(configsDeprecations.stream().map(configsDeprecation -> new ConfigMappings( //
                configsDeprecation.getDeprecatedConfigPaths(), //
                toConfigPaths(configPaths.get()), //
                settings -> {
                    final var fromPrevious = loadOrDefault(settings, configsDeprecation.getLoader(), obj);
                    final var newSettings = new NodeSettings("newSettings");
                    saver.save(fromPrevious, newSettings);
                    return newSettings;
                })//
            ).toList());
        }

        private static GetConfigMappings concatinateMappings(final GetConfigMappings first,
            final GetConfigMappings second) {
            return obj -> new ConfigMappings(List.of(first.getConfigMappings(obj), second.getConfigMappings(obj)));
        }

        private static <T> T loadOrDefault(final NodeSettingsRO settings, final DeprecationLoader<T> deprecationLoader,
            final T obj) {
            try {
                return deprecationLoader.apply(settings);
            } catch (InvalidSettingsException ex) {
                LOGGER.warn(
                    String.format("Error when trying to load from previous settings when modifying settings on save. "
                        + "Using the saved settings instead. Exception: %s", ex));
                return obj;
            }
        }

        private static Collection<ConfigPath> toConfigPaths(final String[][] nonNullPaths) {
            return Arrays.stream(nonNullPaths).map(Arrays::asList).map(ConfigPath::new).toList();
        }

    }

    public static Object createDefaultSettings(final Tree<PersistableSettings> tree) {
        final var settingsClass = tree.getType();
        return ReflectionUtil.createInstance(settingsClass).orElseThrow(() -> new IllegalArgumentException(
            String.format("The provided PersistableSettings '%s' don't provide an empty constructor.", settingsClass)));
    }

}
