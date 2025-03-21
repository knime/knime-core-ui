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
 *   Sep 16, 2024 (hornm): created
 */
package org.knime.core.webui.node;

import java.util.function.Supplier;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.DataValue;
import org.knime.core.node.workflow.NodeContainer;

import com.google.common.base.Objects;

/**
 * Wrapper referencing a {@link DataValue}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface DataValueWrapper extends NodeWrapper {

    /**
     * Convenience method to create a {@link DataValueWrapper}-instance.
     *
     * @param nc The node under consideration
     * @param portIdx The index of the output port (which has to be a table port)
     * @param rowIdx the row index within the table
     * @param colIdx the col index within the table
     * @return a new instance
     */
    static DataValueWrapper of(final NodeContainer nc, final int portIdx, final int rowIdx, final int colIdx) {
        return new DataValueWrapper() { // NOSONAR

            @Override
            public NodeContainer get() {
                return nc;
            }

            @Override
            public int getPortIdx() {
                return portIdx;
            }

            @Override
            public int getRowIdx() {
                return rowIdx;
            }

            @Override
            public int getColIdx() {
                return colIdx;
            }

            @Override
            public <T> T getWithContext(final Supplier<T> supplier) {
                return supplier.get();
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null) {
                    return false;
                }
                if (getClass() != o.getClass()) {
                    return false;
                }
                var w = (DataValueWrapper)o;
                return Objects.equal(nc, w.get()) //
                    && portIdx == w.getPortIdx() //
                    && colIdx == w.getColIdx() //
                    && rowIdx == w.getRowIdx();
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder()//
                    .append(nc)//
                    .append(portIdx)//
                    .append(colIdx)//
                    .append(rowIdx)//
                    .toHashCode();
            }
        };
    }

    /**
     * @return the output port index
     */
    int getPortIdx();

    /**
     * @return the row index
     */
    int getRowIdx();

    /**
     * @return the column index
     */
    int getColIdx();

}
