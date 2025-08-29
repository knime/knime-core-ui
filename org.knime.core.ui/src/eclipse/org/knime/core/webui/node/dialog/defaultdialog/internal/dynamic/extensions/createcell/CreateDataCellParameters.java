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
 *   Aug 29, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;

/**
 * Parameters to create a new DataCell of a specific type. This interface extends {@link NodeParameters}!
 *
 * Use the {CUSTOM_VALUE_TITLE} and {CUSTOM_VALUE_DESCRIPTION} constants when the parameters contain only one
 * {@link Widget} to ensure consistent naming of dynamic parameters in the dialog.
 *
 * @author Paul Bärnreuther
 */
public interface CreateDataCellParameters extends DynamicNodeParameters {

    /**
     * Title to be used when the parameters contain only one {@link Widget}.
     */
    String CUSTOM_VALUE_TITLE = "Custom value";

    /**
     * Description to be used when the parameters contain only one {@link Widget}.
     */
    String CUSTOM_VALUE_DESCRIPTION = "The value to be used when filling the output column.";

    /**
     * Creates a new DataCell of the given type. Even if the present parameters are meant to create a data cell of a
     * specific type, this method can be called with another type when persisted settings are being loaded but the data
     * type has changed in the meantime.
     *
     * @param type the type of the to be constructed cell. Note that this might be a different type than the one this
     *            class is registered with within a {@link CreateDataCellParametersFactory} since cell creation might
     *            happen at a later point in time and the target type might have changed in the meantime. If the
     *            parameters should only be capable of constructing one type of cell, make the
     *            {@link #validate(DataType)} method check for that type.
     * @param ctx the execution context. This is null during validation time, if {@link #validate(DataType)} is not
     *            overwritten.
     * @return the created cell
     * @throws InvalidSettingsException if no cell could be created. Use the {@link #validate()} method to check before
     *             calling this method.
     */
    DataCell createDataCell(final DataType type, final ExecutionContext ctx) throws InvalidSettingsException;

    /**
     * Validates the current settings of the parameters.
     *
     * The default implementation tries to create a cell with a null execution context. This means for data types whose
     * factory requires an execution context, that this method might fail even if the settings are actually valid. So
     * override this method if the default implementation is not sufficient.
     *
     * @param type the type of the to be constructed cell
     * @throws InvalidSettingsException if the current settings should not be used to create a cell of the given type
     */
    default void validate(final DataType type) throws InvalidSettingsException {
        validate();
        createDataCell(type, null);
    }

    /**
     * Helper method to create a DataCell from a String using the FromString factory of the given type.
     *
     * @param value the string representation of the value
     * @param type the type of the to be constructed cell
     * @param ctx the execution context. This is null during validation time.
     * @return the created cell
     * @throws InvalidSettingsException if no cell could be created
     */
    default DataCell createDataCellFromString(final String value, final DataType type, final ExecutionContext ctx)
        throws InvalidSettingsException {

        var factory = type.getCellFactory(ctx).orElseThrow(
            () -> new InvalidSettingsException("No cell factory for " + type.toPrettyString() + " available"));

        if (!(factory instanceof FromString)) {
            throw new InvalidSettingsException(
                "The cell factory for " + type.toPrettyString() + " does not support creating cells from strings");
        }
        DataCell newCell;
        try {
            newCell = ((FromString)factory).createCell(value);
        } catch (RuntimeException ex) { // NOSONAR some factories break the contract of createCell
            // (i.e. they don't throw an IllegalArgumentException) which means that we need to catch
            // everything instead
            throw new InvalidSettingsException("Failed to create cell from value '" + value + "' for type "
                + type.toPrettyString() + ": " + ex.getMessage(), ex);
        }

        // some factories also return a MissingCell when the input is invalid, so handle that too
        if (newCell.isMissing()) {
            throw new InvalidSettingsException(
                "Created cell is missing for value '" + value + "' and type " + type.toPrettyString());
        }
        return newCell;
    }

    /**
     * Parameters to create a DataCell of a specific data type.
     */
    interface SpecificTypeParameters extends CreateDataCellParameters {

        /**
         * The specific type this parameter set creates cells for. If these parameters are provided by a
         * {@link CreateDataCellParametersFactory}, this should return the same value as the factory's
         * {@link CreateDataCellParametersFactory#getDataType()}.
         *
         * @return the specific data type
         */
        DataType getSpecificType();

        /**
         * Creates a new DataCell of the specific type.
         *
         * @return the created cell
         */
        DataCell createSpecificCell();

        /**
         * String representation used whenever the created specific cell is not of the requested type.
         *
         * @return string representation of the value
         */
        @Override
        String toString();

        @Override
        default DataCell createDataCell(final DataType type, final ExecutionContext ctx)
            throws InvalidSettingsException {
            if (getSpecificType().equals(type)) {
                return createSpecificCell();
            }
            return createDataCellFromString(toString(), type, ctx);
        }
    }

}
