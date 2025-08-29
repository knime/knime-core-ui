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

import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.node.ExecutionContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;

/**
 * Parameters to create a new DataCell of a specific type. This interface extends {@link NodeParameters}!
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
     * @param type the type of the to be constructed cell
     * @param ctx the execution context. This is null during validation time.
     * @return the created cell or {@link Optional#empty()} if no cell could be created
     */
    Optional<DataCell> createDataCell(final DataType type, final ExecutionContext ctx);

    /**
     * Helper method to create a DataCell from a String using the FromString factory of the given type.
     *
     * @param value the string representation of the value
     * @param type the type of the to be constructed cell
     * @param ctx the execution context. This is null during validation time.
     * @return the created cell or {@link Optional#empty()} if no cell could be created
     */
    default Optional<DataCell> createDataCellFromString(final String value, final DataType type,
        final ExecutionContext ctx) {

        var factory = type.getCellFactory(ctx).orElseThrow(
            () -> new IllegalArgumentException("No cell factory for " + type.toPrettyString() + " available"));

        DataCell newCell;
        if (!(factory instanceof FromString)) {
            throw new IllegalArgumentException(
                "The cell factory for " + type.toPrettyString() + " does not support creating cells from strings");
        }
        try {
            newCell = ((FromString)factory).createCell(value);
        } catch (RuntimeException ex) { // NOSONAR some factories break the contract of createCell
            // (i.e. they don't throw an IllegalArgumentException) which means that we need to catch
            // everything instead
            return Optional.empty();
        }

        // some factories also return a MissingCell when the input is invalid, so handle that too
        return newCell.isMissing() ? Optional.empty() : Optional.of(newCell);
    }

    /**
     * Parameters to create a DataCell of a specific data type.
     */
    interface SpecificTypeParameters extends CreateDataCellParameters {

        DataType getSpecificType();

        DataCell createSpecificCell();

        /**
         * String representation used whenever the created specific cell is not of the requested type.
         *
         * @return string representation of the value
         */
        @Override
        String toString();

        @Override
        default public Optional<DataCell> createDataCell(final DataType type, final ExecutionContext ctx) {
            if (getSpecificType().equals(type)) {
                return Optional.of(createSpecificCell());
            }
            return createDataCellFromString(toString(), type, ctx);
        }
    }

}
