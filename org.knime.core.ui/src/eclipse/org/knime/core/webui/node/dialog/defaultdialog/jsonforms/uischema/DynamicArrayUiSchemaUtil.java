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
 *   Nov 10, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ELEMENTS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_POSSIBLE_VALUES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_SCOPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_CONTROL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_VERTICAL_LAYOUT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format.DROP_DOWN;
import static org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil.getColumnDataValue;
import static org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil.getColumnKey;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class DynamicArrayUiSchemaUtil {

    private DynamicArrayUiSchemaUtil() {
        // Utility class
    }

    static ObjectNode addDynamicArrayControlElement(final ArrayNode parent, final ArrayParentNode<WidgetGroup> apn,
        final DataTableSpec associatedSpec) {
        final var node = parent.addObject();
        node.put(TAG_TYPE, TYPE_VERTICAL_LAYOUT);
        final var rootPath = getRootPath(apn);
        final var settingsType = apn.getSettingsType();
        final var elements = node.putArray(TAG_ELEMENTS);
        apn.getElementTree().getWidgetNodes()
            .forEach(w -> addDropdownForWidget(elements, rootPath, settingsType, w, associatedSpec));
        return node;

    }

    private static List<String> getRootPath(final ArrayParentNode<WidgetGroup> apn) {
        final var pathToStaticArray = apn.getPath();
        final var pathToArrayField = pathToStaticArray.subList(0, pathToStaticArray.size() - 1);
        return plus(pathToArrayField, "columns");
    }

    private static void addDropdownForWidget(final ArrayNode elements, final List<String> rootPath,
        final SettingsType settingsType, final TreeNode<WidgetGroup> widgetNode, final DataTableSpec associatedSpec) {

        final var key = getColumnKey(widgetNode);
        final var element = elements.addObject();
        final var scope = JsonFormsScopeUtil.toScope(plus(rootPath, key), settingsType);
        element.put(TAG_SCOPE, scope);
        element.put(TAG_TYPE, TYPE_CONTROL);

        final var options = element.putObject(TAG_OPTIONS);
        options.put(TAG_FORMAT, DROP_DOWN);
        var possibleValues = getPossibleValues(widgetNode, associatedSpec);
        options.set(TAG_POSSIBLE_VALUES, JsonFormsUiSchemaUtil.getMapper().valueToTree(possibleValues));
    }

    private static List<IdAndText> getPossibleValues(final TreeNode<WidgetGroup> widgetNode,
        final DataTableSpec associatedSpec) {
        var dataValueClass = getColumnDataValue(widgetNode);
        var columnNames = getColumnNames(dataValueClass, associatedSpec);
        return columnNames.stream().map(IdAndText::fromId).toList();
    }

    private static List<String> getColumnNames(final Class<? extends DataValue> dataValueClass,
        final DataTableSpec spec) {
        return spec.stream().filter(col -> col.getType().isCompatible(dataValueClass)).map(DataColumnSpec::getName)
            .toList();
    }

    private static List<String> plus(final List<String> existingSegments, final String lastSegment) {
        return Stream.concat(existingSegments.stream(), Stream.of(lastSegment)).toList();
    }

}
