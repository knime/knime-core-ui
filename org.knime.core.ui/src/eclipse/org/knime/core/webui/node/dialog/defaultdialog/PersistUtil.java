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
 *   Oct 4, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigKeyUtil;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.PersistenceFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.OptionalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.DotSubstitutionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * For adding a representation of the settings persist structure to an ObjectNode
 *
 * @author Paul Bärnreuther
 */
public final class PersistUtil {

    private PersistUtil() {
        // Utility
    }

    /**
     * public and only used for tests
     *
     * @param parentNode
     * @param settings
     */
    public static void constructTreesAndAddPersist(final ObjectNode parentNode,
        final Map<SettingsType, NodeParameters> settings) {
        final var persistTreeFactory = new PersistTreeFactory();
        final var persistTrees =
            SettingsTypeMapUtil.map(settings, (type, s) -> persistTreeFactory.createTree(s.getClass(), type));
        addPersist(parentNode, persistTrees);
    }

    /**
     * Adds the information necessary for the frontend to adapt flow variable handling to custom persisting given by
     * {@link Persist @Persist} annotations and deviations of the {@link Persistable} structure from the
     * {@link WidgetGroup} structure.
     *
     * @param parentNode the parent object node to add to
     * @param persistTrees the persist trees to parse
     */
    static void addPersist(final ObjectNode parentNode, final Map<SettingsType, Tree<Persistable>> persistTrees) {
        final var persist = parentNode.putObject("persist");
        final var properties = addObjectProperties(persist);
        final var persistSchemaFactory = new PersistSchemaFactory();
        persistTrees.entrySet().forEach(entry -> properties.set(entry.getKey().getConfigKeyFrontend(),
            persistSchemaFactory.createPersistSchemaFromTree(entry.getValue())));
    }

    private static ObjectNode addObjectProperties(final ObjectNode objectNode) {
        objectNode.put("type", "object");
        return objectNode.putObject("properties");
    }

    @SuppressWarnings("rawtypes")
    static final class PersistSchemaFactory extends PersistenceFactory<ObjectNode> {

        static final ObjectMapper MAPPER = new ObjectMapper();

        ObjectNode createPersistSchemaFromTree(final Tree<Persistable> node) {
            return super.extractFromTree(node);
        }

        @Override
        protected ObjectNode getForLeaf(final LeafNode<Persistable> node) {
            final var objectNode = MAPPER.createObjectNode();
            final var configKey = ConfigKeyUtil.getConfigKey(node);
            final var defaultPersistor = DefaultFieldNodeSettingsPersistorFactory.createPersistor(node, configKey);
            final var subConfigPath = defaultPersistor.getSubConfigPath();
            return getNested(node, addConfigPathsIfPresent(objectNode, configKey, subConfigPath, node.isOptional()));
        }

        private static ObjectNode addConfigPathsIfPresent(final ObjectNode objectNode, final String configKey,
            final Optional<List<String>> subConfigPath, final boolean isOptional) {
            final var isPresentKey =
                Optional.of(OptionalPersistor.toIsPresentCfgKey(configKey)).filter(path -> isOptional);
            final var isPresentPath = isPresentKey.map(path -> new String[]{path});
            final var pathToSubConfig = subConfigPath.map(sub -> sub.isEmpty() ? new String[0]
                : Stream.concat(Stream.of(configKey), sub.stream()).toArray(String[]::new));
            if (isPresentPath.isPresent() || pathToSubConfig.isPresent()) {
                final var valueConfigPath = pathToSubConfig.or(() -> Optional.of(new String[]{configKey}));
                final var configPaths =
                    Stream.of(isPresentPath, valueConfigPath).flatMap(Optional::stream).toArray(String[][]::new);
                return addConfigPaths(objectNode, "configPaths", configPaths);
            }
            return objectNode;
        }

        @Override
        protected ObjectNode getFromCustomPersistor(final NodeParametersPersistor<?> nodeSettingsPersistor,
            final TreeNode<Persistable> node) {
            final var objectNode = MAPPER.createObjectNode();
            final var withConfigPaths =
                addConfigPaths(objectNode, "configPaths", nodeSettingsPersistor.getConfigPaths());
            return getNested(node, withConfigPaths);
        }

        @Override
        protected ObjectNode getFromCustomPersistorForType(final NodeParametersPersistor<?> nodeSettingsPersistor,
            final Tree<Persistable> tree) {
            final var objectNode = MAPPER.createObjectNode();
            return addConfigPaths(objectNode, "propertiesConfigPaths", nodeSettingsPersistor.getConfigPaths());
        }

        @Override
        protected ObjectNode getForTree(final Tree<Persistable> tree,
            final Function<TreeNode<Persistable>, ObjectNode> childNode) {
            final var objectNode = MAPPER.createObjectNode();
            final var properties = addObjectProperties(objectNode);
            tree.getChildren().stream().map(childNode::apply).forEach(properties::setAll);
            return objectNode;
        }

        @Override
        protected ObjectNode getForArray(final ArrayParentNode<Persistable> arrayNode, final ObjectNode elementNode) {
            final var objectNode = MAPPER.createObjectNode();
            objectNode.put("type", "array");
            objectNode.set("items", elementNode);
            return objectNode;
        }

        @Override
        protected ObjectNode getNested(final TreeNode<Persistable> node, final ObjectNode property) {
            resolvePersistAnnotation(property, node);
            final var objectNode = MAPPER.createObjectNode();
            objectNode.set(node.getName().orElseThrow(IllegalStateException::new), property);
            return objectNode;
        }

        @Override
        protected ObjectNode combineWithConfigsDeprecations(final ObjectNode existing,
            final List<ConfigMigration> configsDeprecations, final Supplier<String[][]> configPaths,
            final TreeNode<Persistable> node) {
            addDeprecatedConfigKeys(getCurrentField(existing, node), "deprecatedConfigKeys", configsDeprecations);
            return existing;
        }

        @Override
        protected ObjectNode combineWithConfigsDeprecationsForType(final ObjectNode withoutLoader,
            final List<ConfigMigration> configsDeprecations, final Supplier<String[][]> configPaths,
            final Tree<Persistable> node) {
            return addDeprecatedConfigKeys(withoutLoader, "propertiesDeprecatedConfigKeys", configsDeprecations);

        }

        private static ObjectNode addConfigPaths(final ObjectNode node, final String configPathsTag,
            final String[][] configPaths) {
            if (configPaths == null) {
                return node;
            }
            final var filteredValidatedConfigPaths =
                Arrays.stream(configPaths).map(path -> Arrays.stream(path).filter(PersistSchemaFactory::isNonInternal)
                    .map(DotSubstitutionUtil::substituteDots).toList()).filter(path -> !path.isEmpty()).toList();
            add2DStingArray(node, configPathsTag, filteredValidatedConfigPaths);
            return node;
        }

        private static boolean isNonInternal(final String key) {
            return !key.endsWith(SettingsModel.CFGKEY_INTERNAL);
        }

        private static void resolvePersistAnnotation(final ObjectNode objectNode,
            final TreeNode<Persistable> treeNode) {
            final var persistAnnotation = treeNode.getAnnotation(Persist.class);
            final var configRename = persistAnnotation.map(Persist::configKey).filter(key -> !key.isEmpty());
            final boolean isHidden = persistAnnotation.map(Persist::hidden).orElse(false);
            if (isHidden) {
                objectNode.putArray("configPaths");
            } else if (configRename.isPresent()) {
                objectNode.put("configKey", DotSubstitutionUtil.substituteDots(configRename.get()));
            }
        }

        private static ObjectNode addDeprecatedConfigKeys(final ObjectNode node, final String deprecatedConfigKeysTag,
            final List<ConfigMigration> configsDeprecations) {
            final var deprecatedConfigsNode = node.putArray(deprecatedConfigKeysTag);
            configsDeprecations.stream()
                .forEach(configsDeprecation -> putDeprecatedConfig(deprecatedConfigsNode, configsDeprecation));
            return node;
        }

        private static void putDeprecatedConfig(final ArrayNode deprecatedConfigsNode,
            final ConfigMigration<?> newAndDeprecatedConfigPaths) {
            final var nextDeprecatedConfigs = deprecatedConfigsNode.addObject();

            final var deprecatedConfigPaths =
                newAndDeprecatedConfigPaths.getDeprecatedConfigPaths().stream().map(ConfigPath::path)
                    .map(path -> path.stream().map(DotSubstitutionUtil::substituteDots).toList()).toList();
            add2DStingArray(nextDeprecatedConfigs, "deprecated", deprecatedConfigPaths);
        }

        private static void add2DStingArray(final ObjectNode node, final String key,
            final List<List<String>> twoDimensionalArray) {
            final var parentArrayNode = node.putArray(key);
            twoDimensionalArray.forEach(oneDimensionalArray -> {
                final var childArray = parentArrayNode.addArray();
                oneDimensionalArray.forEach(childArray::add);
            });
        }

        @Override
        protected ObjectNode reroute(final String[] relativePath, final ObjectNode existing,
            final TreeNode<Persistable> node) {
            final var routeArray = getCurrentField(existing, node).putArray("route");
            addRouteToArray(relativePath, routeArray);
            return existing;
        }

        @Override
        protected ObjectNode rerouteForType(final String[] relativePath, final ObjectNode property,
            final Tree<Persistable> node) {
            final var routeArray = property.putArray("propertiesRoute");
            addRouteToArray(relativePath, routeArray);
            return property;
        }


        private static ObjectNode getCurrentField(final ObjectNode existing, final TreeNode<Persistable> node) {
            return (ObjectNode)existing.get(node.getName().orElseThrow(IllegalStateException::new));
        }

        private static void addRouteToArray(final String[] relativePath, final ArrayNode routeArray) {
            for (final var pathElement : relativePath) {
                routeArray.add("..".equals(pathElement) ? ".." : DotSubstitutionUtil.substituteDots(pathElement));
            }
        }
    }

}
