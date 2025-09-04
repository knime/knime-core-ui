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
 *   Sep 4, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ITEMS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PROPERTIES;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility method for preparing the KAI Configure Prompt for parameters including dynamic parameters.
 *
 * See {@link DynamicParameters#schemaForDefaultKaiNodeInterface()}.
 *
 * @author Paul Bärnreuther
 */
public class DynamicParametersSchemaReplacementUtil {

    private DynamicParametersSchemaReplacementUtil() {
        // utility class
    }

    static final NodeLogger LOGGER = NodeLogger.getLogger(DynamicParametersSchemaReplacementUtil.class);

    /**
     * Lists all locations where a custom schema should overwrite the default one.
     *
     * @param widgetTrees to extract schema replacements from
     * @return list of pairs of location and custom schema
     */
    public static List<Pair<Location, String>>
        getSchemaReplacements(final Map<SettingsType, Tree<WidgetGroup>> widgetTrees) {
        return widgetTrees.values().stream()//
            .flatMap(DynamicParametersSchemaReplacementUtil::getDynamicTrees)//
            .flatMap(tree -> {
                final var annotation = tree.getAnnotation(DynamicParameters.class).orElseThrow();
                final var customSchema = annotation.schemaForDefaultKaiNodeInterface();
                if (customSchema.isEmpty()) {
                    return Stream.of();
                }
                final var location = Location.fromTreeNode(tree);
                return Stream.of(new Pair<>(location, customSchema));
            }).toList();
    }

    private static Stream<Tree<WidgetGroup>> getDynamicTrees(final TreeNode<WidgetGroup> treeNode) {
        if (treeNode instanceof Tree<WidgetGroup> tree) {
            if (tree.isDynamic()) {
                return Stream.of(tree);
            }
            return tree.getChildren().stream().flatMap(DynamicParametersSchemaReplacementUtil::getDynamicTrees);
        } else if (treeNode instanceof ArrayParentNode<WidgetGroup> arrayParent) {
            return getDynamicTrees(arrayParent.getElementTree());
        } else {
            return Stream.empty();
        }

    }

    /**
     * Performs the schema replacements (in place). If the value of a replacement is no valid json or the path does not
     * exist in the given schema, the replacement is ignored and no error is thrown.
     *
     * @param schema to perform replacements on
     * @param replacements to perform
     */
    public static void performSchemaReplacements(final JsonNode schema,
        final Collection<Pair<Location, String>> replacements) {
        if (replacements.isEmpty()) {
            return;
        }
        final var mapper = new ObjectMapper();
        for (var replacement : replacements) {
            var location = replacement.getFirst();
            try {
                var newValue = mapper.readTree(replacement.getSecond());
                performSchemaReplacement(schema, location, newValue);
            } catch (JsonProcessingException | IllegalArgumentException e) {
                LOGGER.warn("Failed to perform schema replacement at location " + location, e);
            }
        }
    }

    private static void performSchemaReplacement(final JsonNode schema, final Location location,
        final JsonNode newValue) {
        final var settingsType = location.settingsType();
        final List<List<String>> paths = location.paths();
        final List<List<String>> pathsWithoutLastKeyInLastPath = Stream.concat(//
            paths.subList(0, paths.size() - 1).stream(), //
            Stream.of(paths.get(paths.size() - 1).subList(0, paths.get(paths.size() - 1).size() - 1))//
        ).toList();

        JsonNode parentNode = schema.get(TAG_PROPERTIES).get(settingsType.getConfigKeyFrontend());
        for (int i = 0; i < pathsWithoutLastKeyInLastPath.size(); i++) {
            if (i > 0) {
                parentNode = getItems(parentNode);
            }
            final var path = pathsWithoutLastKeyInLastPath.get(i);
            for (String key : path) {
                parentNode = getOrThrow(getProperties(parentNode), key);
            }
        }
        parentNode = getProperties(parentNode);
        final String lastKeyInLastPath = paths.get(paths.size() - 1).get(paths.get(paths.size() - 1).size() - 1);
        if (parentNode instanceof ObjectNode objectNode) {
            objectNode.replace(lastKeyInLastPath, newValue);
        } else {
            throw new IllegalArgumentException("Parent schema of replacement is not an object: " + parentNode);
        }

    }

    private static JsonNode getOrThrow(final JsonNode node, final String key) {
        var child = node.get(key);
        if (child == null) {
            throw new IllegalArgumentException("Key not found: " + key);
        }
        return child;
    }

    private static JsonNode getProperties(final JsonNode node) {
        return getOrThrow(node, TAG_PROPERTIES);
    }

    private static JsonNode getItems(final JsonNode node) {
        return getOrThrow(node, TAG_ITEMS);
    }

}
