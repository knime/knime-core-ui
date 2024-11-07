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
package org.knime.core.webui.node.dialog.defaultdialog.util;

import java.util.Map;
import java.util.Optional;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.setting.array.Array;
import org.knime.core.webui.node.dialog.defaultdialog.setting.array.PortIndex;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;

/**
 * Helper methods for {@link Array}
 *
 * @author Paul Bärnreuther
 */
public final class DynamicArrayUtil {

    private DynamicArrayUtil() {
        // Utility class
    }

    /**
     * For a field of type {@link Array} this method will return the {@link DataTableSpec} associated with the input
     * port defined by {@link PortIndex}.
     *
     * @param node
     * @param context
     * @return the associated {@link DataTableSpec} or {@link Optional#empty()} if the port is not connected.
     */
    public static Optional<DataTableSpec> getAssociatedSpec(final ArrayParentNode<WidgetGroup> node,
        final DefaultNodeSettingsContext context) {
        final var portIndex = getPortIndex(node);
        try {
            return context.getDataTableSpec(portIndex);
        } catch (ArrayIndexOutOfBoundsException e) { // NOSONAR
            return Optional.empty();
        }
    }

    private static int getPortIndex(final ArrayParentNode<WidgetGroup> node) {
        return node.getAnnotation(PortIndex.class)
            .orElseThrow(() -> new IllegalStateException("No @PortIndex annotation found on Array field at path "
                + node.getPath().subList(0, node.getPath().size() - 1)))
            .value();
    }

    /**
     * @param widgetNode a sub-widget node of the arrays element tree
     * @return the key that is used within the map that is stored within an {@link Array} setting.
     */
    public static String getColumnKey(final TreeNode<?> widgetNode) {
        return String.join("#", widgetNode.getPath());
    }

    private static final Map<Class<?>, Class<? extends DataValue>> DATA_VALUE_MAP = Map.of(//
        String.class, StringValue.class, //
        Integer.class, IntValue.class, //
        int.class, IntValue.class, //
        Double.class, DoubleValue.class, //
        double.class, DoubleValue.class, //
        Long.class, LongValue.class, //
        long.class, LongValue.class, //
        Boolean.class, BooleanValue.class, //
        boolean.class, BooleanValue.class, //
        ColumnSelection.class, StringValue.class);

    /**
     * @param widgetNode a widget in the element tree
     * @return the {@link DataValue} class that the selected dynamic column should be compatible with.
     */
    public static Class<? extends DataValue> getColumnDataValue(final TreeNode<WidgetGroup> widgetNode) {
        final var type = widgetNode.getType();
        if (type.isEnum()) {
            return StringValue.class;
        }
        final var dataValueClass = DATA_VALUE_MAP.get(type);
        if (dataValueClass == null) {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return dataValueClass;

    }

    /**
     * @param dataCell
     * @param type
     * @return the field value of the given type
     * @throws IllegalArgumentException if the data cell is missing or the type is not supported
     * @throws ClassCastException if the data cell does not match the type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object toFieldValue(final DataCell dataCell, final Class<?> type) throws IllegalArgumentException {
        if (dataCell.isMissing()) {
            throw new IllegalArgumentException("Data cell is missing");
        }
        if (type.isEnum()) {
            final var stringValue = ((StringValue)dataCell).getStringValue();
            return Enum.valueOf((Class<? extends Enum>)type, stringValue);
        }
        if (type.equals(ColumnSelection.class)) {
            final var stringValue = ((StringValue)dataCell).getStringValue();
            return new ColumnSelection(stringValue, null);
        }
        if (type.equals(String.class)) {
            return ((StringValue)dataCell).getStringValue();
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return ((IntValue)dataCell).getIntValue();
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return ((DoubleValue)dataCell).getDoubleValue();
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return ((LongValue)dataCell).getLongValue();
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return ((BooleanValue)dataCell).getBooleanValue();
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
