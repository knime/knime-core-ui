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
 *   Jan 2, 2026 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.tablecreator;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.tablecreator.TableCreatorNodeParameters.ColumnParameters.DataTypeRef;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Parameters for the Table Creator node.
 *
 * It is abstract since its data type choices need to be set to the node-specific data type choices provider. Use the
 * {@link DataTypeChoicesModification} for this.
 *
 * @author Paul Bärnreuther
 */
public abstract class TableCreatorNodeParameters implements NodeParameters {

    @Widget(title = "Columns", description = "The columns to create.")
    ColumnParameters[] m_columns = new ColumnParameters[]{defaultFirstColumn()};

    long m_numRows = 1L;

    static ColumnParameters defaultFirstColumn() {
        return new ColumnParameters("Column 1", StringCell.TYPE, new String[]{null});
    }

    /**
     * @return the columns
     */
    public ColumnParameters[] getColumns() {
        return m_columns;
    }

    /**
     * @return the numRows
     */
    public long getNumRows() {
        return m_numRows;
    }

    /**
     * @param numRows the numRows to set
     */
    public void setNumRows(final long numRows) {
        m_numRows = numRows;
    }

    /**
     * Parameters for a single created column
     */
    public static final class ColumnParameters implements NodeParameters {

        ColumnParameters() {
            // For JSON deserialization
        }

        private ColumnParameters(final String name, final DataType type, final String[] values) {
            m_name = name;
            m_type = type;
            m_values = values;
        }

        /**
         * Creates a new {@link DataColumnSpec} based on the current column parameters.
         *
         * @return the column spec
         */
        public DataColumnSpec createColumnSpec() {
            return new DataColumnSpecCreator(m_name, m_type).createSpec();
        }

        /**
         * The column type.
         *
         * @return the column type
         */
        public DataType getType() {
            return m_type;
        }

        /**
         * @return the name of the column
         */
        public String getName() {
            return m_name;
        }

        /**
         * @param rowIndex the row index for which to get the value
         * @return the value for the given row index. Note that this method also returns {@code null} for row indices
         *         larger than the number of rows in the table.
         */
        public String getValue(final long rowIndex) {
            return rowIndex < m_values.length ? m_values[(int)rowIndex] : null;
        }

        /**
         * The array of values. Note that this array can be shorter than the number of rows in the table, in which case
         * the missing values are considered to be {@code null} (i.e. missing).
         *
         * @return the array of values for this column
         */
        public String[] getValues() {
            return m_values;
        }

        @Widget(title = "Column name", description = "The name of the column.")
        @TextInputWidget(patternValidation = ColumnNameValidation.class)
        String m_name;

        @Widget(title = "Column type", description = "The data type of the column.")
        @Modification.WidgetReference(DataTypeRef.class)
        DataType m_type = StringCell.TYPE;

        interface DataTypeRef extends Modification.Reference {
        }

        // Not a @Widget since it is rendered custom in the dialog
        String[] m_values = new String[0];

    }

    /**
     * Abstract modification to set the data type choices to those supported by the node (and its implementation of the
     * {@link TableCreatorRpcService}).
     *
     * @author Paul Bärnreuther
     */
    public abstract static class DataTypeChoicesModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(DataTypeRef.class).addAnnotation(ChoicesProvider.class).withValue(providerClass()).modify();

        }

        /**
         * The class of the {@link DataTypeChoicesProvider} that provides the supported data types for this node.
         *
         * @return the provider class
         */
        protected abstract Class<? extends DataTypeChoicesProvider> providerClass();

    }

}
