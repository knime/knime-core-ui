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
 *   Sep 3, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;

/**
 * Filter operator that defines a predicate based on arbitrary {@link FilterValueParameters parameters}.
 *
 * @param <V> the type of data value to filter
 * @param <P> the type of parameters to create the predicate with
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public interface ValueFilterOperator<V extends DataValue, P extends FilterValueParameters> {

    /**
     * Gets the ID, which must be unique among the set of all operators applicable on {@code V}, for which this operator
     * is defined.
     *
     * @return ID
     */
    String getId();

    /**
     * Gets a label for the operator, which is shown in the UI and should not be used in cases where a stable ID would
     * be appropriate.
     *
     * @return label
     */
    String getLabel();

    /**
     * Creates the predicate for filtering data values of type {@code V} based on the given filter parameters.
     *
     * @param filterParameters the parameters to create the predicate with
     * @return the predicate for filtering data values of type {@code V}
     */
    Predicate<V> createPredicate(final DataColumnSpec runtimeColumnSpec, P filterParameters)
        throws InvalidSettingsException;

    boolean handlesMissingCells();

    /**
     * Data type this operator is registered for.
     *
     * @return
     */
    DataType getDataType();

    /**
     * Creates the parameters for creating a data cell of the given type.
     *
     * @param dataCellClass the class of the data cell to create
     * @return the parameters for creating a data cell of the given type
     */
    Class<P> getNodeParametersClass();

}