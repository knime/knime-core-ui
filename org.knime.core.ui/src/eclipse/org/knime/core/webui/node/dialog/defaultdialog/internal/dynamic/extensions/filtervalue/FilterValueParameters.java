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
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;

/**
 * Parameters to create a new DataCell of a specific type. This interface extends {@link NodeParameters}!
 *
 * @author Paul Bärnreuther
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public interface FilterValueParameters extends DynamicNodeParameters {

    /**
     * Used whenever switching to different parameters. The default implementation drops the current state, so overwrite
     * this method if new parameters should be initialized with the current state.
     *
     * @return the stashed values as strings.
     */
    default DataValue[] stash() {
        return new DataValue[0];
    }

    /**
     * Apply the stashed values, if any, to the parameters. The default implementation does nothing.
     *
     * @param stashedValues the stashed values as strings, possibly empty array
     */
    default void applyStash(final DataValue[] stashedValues) {
        // default does nothing
    }

    /**
     * Parameters able to create a data value of a specific data type.
     *
     * Use the {FILTER_VALUE_TITLE} and {FILTER_VALUE_DESCRIPTION} constants when the parameters contain only one
     * {@link Widget} to ensure consistent naming of dynamic parameters in the dialog.
     *
     *
     * @param <C> the type of the cell created by the parameters
     */
    interface SingleCellValueParameters<C extends DataCell> extends FilterValueParameters {

        /**
         * Title to be used when the parameters contain only one {@link Widget}
         */
        String FILTER_VALUE_TITLE = "Value";

        /**
         * Description to be used when the parameters contain only one {@link Widget}.
         */
        String FILTER_VALUE_DESCRIPTION = "The value to compare to.";

        C createCell();

        void loadFrom(C valueFromStash);

        DataType getSpecificType();

        @Override
        default DataValue[] stash() {
            return new DataValue[]{createCell()};
        }

        @SuppressWarnings("unchecked") // safe cast since V is a value implemented by targetType which is more general
        // than sourceType
        @Override
        default void applyStash(final DataValue[] stashedValues) {
            if (stashedValues.length == 0) {
                return;
            }
            final var firstValue = stashedValues[0];
            if (firstValue == null) {
                return;
            }
            // "best-effort" approach
            final var source = firstValue.materializeDataCell();
            final var sourceType = source.getType();
            final var targetType = getSpecificType();
            if (targetType.isASuperTypeOf(sourceType)
                && targetType.getCellClass().isAssignableFrom(source.getClass())) {
                loadFrom((C)source);
            }
        }
    }
}
