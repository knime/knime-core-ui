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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.AbstractPersistenceFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;

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
     * Adds the information necessary for the frontend to adapt flow variable handling to custom persisting given by
     * {@link Persist @Persist} annotations and deviations of the {@link PersistableSettings} structure from the
     * {@link WidgetGroup} structure.
     *
     * @param parentNode the parent object node to add to
     * @param persistTrees the persist trees to parse
     */
    static void addPersist(final ObjectNode parentNode,
        final Map<SettingsType, Tree<PersistableSettings>> persistTrees) {
        final var persist = parentNode.putObject("persist");
        final var properties = addObjectProperties(persist);
        final var persistSchemaFactory = new PersistSchemaFactory();
        persistTrees.entrySet().forEach(entry -> properties.set(entry.getKey().getConfigKey(),
            persistSchemaFactory.extractFromTree(entry.getValue())));
    }

    private static ObjectNode addObjectProperties(final ObjectNode objectNode) {
        objectNode.put("type", "object");
        return objectNode.putObject("properties");
    }

    @SuppressWarnings("rawtypes")
    static final class PersistSchemaFactory extends AbstractPersistenceFactory<ObjectNode> {

        static final ObjectMapper MAPPER = new ObjectMapper(); // TODO: Is it possible without Mapper i.e. in reverse?

        @Override
        protected ObjectNode getDefaultForLeaf(final LeafNode<PersistableSettings> node) {
            final var objectNode = MAPPER.createObjectNode();
            return getNested(node, objectNode);
        }

        @Override
        protected ObjectNode getFromCustomPersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
            final TreeNode<PersistableSettings> node) {
            final var objectNode = MAPPER.createObjectNode();
            final var withConfigPaths =
                addConfigPaths(objectNode, "configPaths", nodeSettingsPersistor.getConfigPaths());
            return getNested(node, withConfigPaths);
        }

        @Override
        protected ObjectNode getFromCustomTypePersistor(final NodeSettingsPersistor<?> nodeSettingsPersistor,
            final Tree<PersistableSettings> tree) {
            final var objectNode = MAPPER.createObjectNode();
            return addConfigPaths(objectNode, "propertiesConfigPaths", nodeSettingsPersistor.getConfigPaths());
        }

        @Override
        protected ObjectNode combineTreeChildren(final Tree<PersistableSettings> tree,
            final Function<TreeNode<PersistableSettings>, ObjectNode> childNode) {
            final var objectNode = MAPPER.createObjectNode();
            final var properties = addObjectProperties(objectNode);
            tree.getChildren().stream().map(childNode::apply).forEach(properties::setAll);
            return objectNode;
        }

        @Override
        protected ObjectNode processArray(final ArrayParentNode<PersistableSettings> arrayNode,
            final ObjectNode elementNode) {
            final var objectNode = MAPPER.createObjectNode();
            objectNode.put("type", "array");
            objectNode.set("items", elementNode);
            return objectNode;
        }

        @Override
        protected ObjectNode getNested(final TreeNode<PersistableSettings> node, final ObjectNode property) {
            resolvePersistAnnotation(property, node);
            final var objectNode = MAPPER.createObjectNode();
            objectNode.set(node.getName().orElseThrow()/** TODO */
                , property);
            return objectNode;
        }

        @Override
        protected ObjectNode combineWithBackwardsCompatibleLoader(final ObjectNode withoutLoader,
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
            final TreeNode<PersistableSettings> node) {
            return addDeprecatedConfigKeys(withoutLoader, "configsDeprecations", configsDeprecations);
        }

        @Override
        protected ObjectNode combineWithBackwardsCompatibleTypeLoader(final ObjectNode withoutLoader,
            final List<ConfigsDeprecation> configsDeprecations, final Supplier<String[][]> configPaths,
            final Tree<PersistableSettings> node) {
            return addDeprecatedConfigKeys(withoutLoader, "propertiesConfigsDeprecations", configsDeprecations);

        }

        private static ObjectNode addConfigPaths(final ObjectNode node, final String configPathsTag,
            final String[][] configPaths) {
            if (configPaths == null) {
                return node;
            }
            final var filteredValidatedConfigPaths =
                Arrays.stream(configPaths).map(path -> Arrays.stream(path).filter(PersistSchemaFactory::isNonInternal)
                    .map(PersistSchemaFactory::validateKey).toList()).filter(path -> !path.isEmpty()).toList();
            add2DStingArray(node, configPathsTag, filteredValidatedConfigPaths);
            return node;
        }

        private static boolean isNonInternal(final String key) {
            return !key.endsWith(SettingsModel.CFGKEY_INTERNAL);
        }

        private static String validateKey(final String key) {
            if (key.contains(".")) {
                throw new IllegalArgumentException(
                    "Config key must not contain dots. If nested config keys are required, use getConfigPaths instead "
                        + "of getConfigKeys. Config key: " + key);
            }
            return key;
        }

        private static void resolvePersistAnnotation(final ObjectNode objectNode,
            final TreeNode<PersistableSettings> treeNode) {
            final var persistAnnotation = treeNode.getAnnotation(Persist.class);
            final var configRename = persistAnnotation.map(Persist::configKey).filter(key -> !key.isEmpty());
            final boolean isHidden = persistAnnotation.map(Persist::hidden).orElse(false);
            if (isHidden) {
                objectNode.putArray("configPaths");
            } else if (configRename.isPresent()) {
                objectNode.put("configKey", configRename.get());
            }
        }

        private static ObjectNode addDeprecatedConfigKeys(final ObjectNode node, final String deprecatedConfigKeysTag,
            final List<ConfigsDeprecation> configsDeprecations) {
            final var deprecatedConfigsNode = node.putArray(deprecatedConfigKeysTag);
            configsDeprecations.stream()
                .forEach(configsDeprecation -> putDeprecatedConfig(deprecatedConfigsNode, configsDeprecation));
            return node;
        }

        private static void putDeprecatedConfig(final ArrayNode deprecatedConfigsNode,
            final ConfigsDeprecation<?> newAndDeprecatedConfigPaths) {
            final var nextDeprecatedConfigs = deprecatedConfigsNode.addObject();

            final var deprecatedConfigPaths = newAndDeprecatedConfigPaths.getDeprecatedConfigPaths();
            add2DStingArray(nextDeprecatedConfigs, "deprecated", to2DList(deprecatedConfigPaths));
        }

        private static List<List<String>> to2DList(final Collection<ConfigPath> newConfigPaths) {
            return newConfigPaths.stream().map(ConfigPath::path).toList();
        }

        private static void add2DStingArray(final ObjectNode node, final String key,
            final List<List<String>> twoDimensionalArray) {
            final var parentArrayNode = node.putArray(key);
            twoDimensionalArray.forEach(oneDimensionalArray -> {
                final var childArray = parentArrayNode.addArray();
                oneDimensionalArray.forEach(childArray::add);
            });
        }

    }

}
