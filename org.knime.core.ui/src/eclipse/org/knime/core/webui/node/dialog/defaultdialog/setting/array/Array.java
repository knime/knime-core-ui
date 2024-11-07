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
 *   Nov 7, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;

/**
 * This class represents an array of elements. In contrast to using a java array, this class allows for setting the
 * content of the array dynamically/programatically (and in particular with varying length) by selecting a column from
 * an input table connected solely for that purpose. There is one column for each widget in the array. For example, a
 * check box widget in each element is then controlled by a boolean column in the input table. A drop down input widget
 * is controlled by a string column.
 *
 * Use the {@link PortIndex} annotation on a field with type {@link Array} to link it to an associated input port from
 * which the array can be constructed dynamically. If the port is connected, the user will not see the usual array
 * layout but instead a collection of column selections from this table.
 *
 * @param <E> The type of the elements
 * @author Paul Bärnreuther
 */
public final class Array<E extends DefaultNodeSettings> {

    Array() {
        // default constructor
    }

    /**
     * @param values
     */
    @SafeVarargs
    public Array(final E... values) {
        m_values = Arrays.asList(values);
    }

    Array(final List<E> values, final Map<String, String> columns) {
        m_values = values;
        m_columns = columns;
    }

    /**
     * Static values of the array. Used when there is no associated table connected.
     */
    List<E> m_values = List.of();

    /**
     * Mapping the path in the element settings to the column name in the table.
     */
    Map<String, String> m_columns = Map.of();

    private static final WidgetTreeFactory WIDGET_TREE_FACTORY = new WidgetTreeFactory();

    /**
     * To be called on configure. We validate that, if dynamic values are to be used, the selected columns are present
     * in the table and of the correct type.
     *
     * @param elementClass the class of the elements
     * @param spec of the associated table. In case this is null, we do not validate.
     * @throws InvalidSettingsException
     */
    public void validate(final Class<E> elementClass, final DataTableSpec spec) throws InvalidSettingsException {
        if (spec == null) {
            return;
        }
        for (var widgetNode : getElementTree(elementClass).getWidgetNodes().toList()) {
            String key = DynamicArrayUtil.getColumnKey(widgetNode);
            if (!m_columns.containsKey(key)) {
                throw new InvalidSettingsException(
                    String.format("There is no dynamic array column selected for key \"%s\".", key));
            }
            String column = m_columns.get(key);
            final var colSpec = spec.getColumnSpec(column);
            if (colSpec == null) {
                throw new InvalidSettingsException(String
                    .format("The dynamic array column \"%s\" is missing from the connected input table spec.", column));
            }
            final var dataType = colSpec.getType();
            final var requiredType = DynamicArrayUtil.getColumnDataValue(widgetNode);
            if (!dataType.isCompatible(requiredType)) {
                throw new InvalidSettingsException(String.format(
                    "The selected dynamic array column \"%s\" is of type \"%s\" but the setting requires a %s.", column,
                    dataType.getName(), requiredType.getSimpleName()));

            }

        }

    }

    private Tree<WidgetGroup> getElementTree(final Class<E> elementClass) {
        return WIDGET_TREE_FACTORY.createTree(elementClass);
    }

    /**
     * @param elementClass the class of the elements
     * @param table associated to this array. Call this method with a null value as this argument if the table is not
     *            connected.
     * @return the values within this array. If the table is non-null, these values are derived from its cells.
     */
    public List<E> getValues(final Class<E> elementClass, final BufferedDataTable table) {
        if (table == null) {
            return m_values;
        }
        final List<E> elements = new ArrayList<>();
        final var elementTree = getElementTree(elementClass);
        try (final var tableIter = table.iterator()) {
            while (tableIter.hasNext()) {
                elements.add(dataRowToElement(elementClass, elementTree, tableIter.next(), table.getSpec()));
            }
        }
        return toImmutableList(elements);
    }

    private List<E> toImmutableList(final List<E> elements) {
        return elements.stream().toList();
    }

    private E dataRowToElement(final Class<E> elementClass, final Tree<WidgetGroup> elementTree, final DataRow dataRow,
        final DataTableSpec spec) {
        final var element = InstantiationUtil.createInstance(elementClass);
        setValuesPerReflection(elementTree, element, dataRow, spec);
        return element;
    }

    private void setValuesPerReflection(final Tree<WidgetGroup> elementTree, final Object parentValue,
        final DataRow dataRow, final DataTableSpec spec) {
        elementTree.getChildren().forEach(child -> setFieldValuePerReflection(parentValue, dataRow, spec, child));
    }

    private void setFieldValuePerReflection(final Object parentValue, final DataRow dataRow, final DataTableSpec spec,
        final TreeNode<WidgetGroup> field) {
        try {
            if (field instanceof Tree<WidgetGroup> childTree) {
                // this value is non-null since widget group fields have a default.
                final var childValue = childTree.getFromParentValue(parentValue);
                Objects.requireNonNull(childValue, "Element field with nested fields must not be null.");
                setValuesPerReflection(childTree, childValue, dataRow, spec);
            } else {
                setLeafValuePerReflection(field, parentValue, dataRow, spec);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                String.format("Error while setting field \"%s\" within %s. This should not happen.",
                    field.getName().orElse(""), parentValue.getClass().getSimpleName()),
                ex);
        }
    }

    private void setLeafValuePerReflection(final TreeNode<WidgetGroup> child, final Object parentValue,
        final DataRow dataRow, final DataTableSpec spec) throws IllegalArgumentException, IllegalAccessException {
        final var key = DynamicArrayUtil.getColumnKey(child);
        final var column = m_columns.get(key);
        final var dataCell = dataRow.getCell(spec.findColumnIndex(column));
        try {
            final var fieldValue = DynamicArrayUtil.toFieldValue(dataCell, child.getType());
            child.setInParentValue(parentValue, fieldValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                String.format("There exists an invalid cell in column \"%s\" of the dynamic array table.", column), ex);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_values, m_columns);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Array<?> other) { // NOSONAR
            return Objects.equals(m_values, other.m_values) && Objects.equals(m_columns, other.m_columns);
        }
        return false;
    }

}
