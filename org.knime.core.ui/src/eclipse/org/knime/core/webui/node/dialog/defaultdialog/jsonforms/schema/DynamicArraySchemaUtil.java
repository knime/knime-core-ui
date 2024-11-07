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
 *   Nov 12, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_DESCRIPTION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ITEMS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PROPERTIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TYPE_ARRAY;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TYPE_OBJECT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TYPE_STRING;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil.buildSchema;
import static org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil.getColumnKey;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class DynamicArraySchemaUtil {

    private DynamicArraySchemaUtil() {
        // Utility class
    }

    private static final String KEY_VALUES = "values";

    private static final String KEY_COLUMNS = "columns";

    static ObjectNode buildDynamicArraySchema(final ArrayParentNode<WidgetGroup> dynamicArray,
        final DefaultNodeSettingsContext context, final ObjectMapper mapper) {

        final var objectNode = mapper.createObjectNode();

        final var elementTree = dynamicArray.getElementTree();
        objectNode.put(TAG_TYPE, TYPE_OBJECT);
        final var properties = objectNode.putObject(TAG_PROPERTIES);
        if (DynamicArrayUtil.getAssociatedSpec(dynamicArray, context).isEmpty()) {
            putStaticValuesArray(elementTree, properties, context, mapper);
        } else {
            putColumns(elementTree, properties);
        }
        return objectNode;
    }

    private static void putColumns(final Tree<WidgetGroup> elementTree, final ObjectNode properties) {
        final var columns = properties.putObject(KEY_COLUMNS);
        columns.put(TAG_TYPE, TYPE_OBJECT);
        final var columnsProperties = columns.putObject(TAG_PROPERTIES);
        elementTree.getWidgetNodes().forEach(widget -> addDynamicArrayColumnProperty(columnsProperties, widget));
    }

    private static void putStaticValuesArray(final Tree<WidgetGroup> elementTree, final ObjectNode properties,
        final DefaultNodeSettingsContext context, final ObjectMapper mapper) {
        final var values = properties.putObject(KEY_VALUES);
        values.put(TAG_TYPE, TYPE_ARRAY);
        values.set(TAG_ITEMS, buildSchema(elementTree.getType(), elementTree, context, mapper));
    }

    private static void addDynamicArrayColumnProperty(final ObjectNode columnsProperties,
        final TreeNode<WidgetGroup> widget) {
        final var key = getColumnKey(widget);
        final var value = columnsProperties.putObject(key);
        value.put(TAG_TYPE, TYPE_STRING);
        widget.getAnnotation(Widget.class).ifPresent(w -> {
            if (!w.title().isEmpty()) {
                value.put(TAG_TITLE, toDropdownTitle(w.title()));
            }
            if (!w.description().isEmpty()) {
                value.put(TAG_DESCRIPTION, toDropdownDescription(w.description()));
            }
        });
    }

    static String toDropdownTitle(final String originalTitle) {
        return String.format("Column controlling \"%s\"", originalTitle);
    }

    static String toDropdownDescription(final String originalDescription) {
        return String.format(
            "Select a column from the connected table. "
                + "A single value of this column conforms to the following description:<br/><br/>%s",
            originalDescription);
    }

}
