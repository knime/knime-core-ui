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
 *   19 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin;

import java.util.List;
import java.util.function.Predicate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;

/**
 * A family of operators for equality comparisons between a data value and a reference data cell, obtained from the
 * single cell value parameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <C> type of data cell to compare with (from node parameters)
 * @param <P> type of node parameters to create the comparison cell
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class EqualsOperatorFamily<C extends DataCell, P extends SingleCellValueParameters<C>>
    implements FilterOperatorFamily<P> {

    private final DataType m_dataType;

    private final Class<P> m_paramClass;

    /**
     * Creates a new equality operator family associated with the given data type, using the given parameter class for
     * node parameters.
     *
     * The equality comparison is done via the reference cell's {@link DataCell#equals(Object)} method.
     *
     * @param dataType column data type
     * @param paramClass class of node parameters to create the comparison cell
     */
    public EqualsOperatorFamily(final DataType dataType, final Class<P> paramClass) {
        m_dataType = dataType;
        m_paramClass = paramClass;
    }

    /**
     * Gets the data type this family is defined for.
     * @return the data type
     */
    protected final DataType getDataType() {
        return m_dataType;
    }

    /**
     * Creates a predicate to evaluate equality between a data value and a reference cell created from the parameters
     * for all operators of this family. The specific operator is passed for context, e.g. to create specific error
     * messages for users.
     *
     * @param runtimeColumnSpec the column spec of the column to filter
     * @param operator the operator for which the equality is requested
     * @param filterParameters the parameters for the operator
     * @return the equality predicate
     * @throws InvalidSettingsException if the operator is not compatible with the column spec, i.e. will not produce a
     *             useful equality comparison
     */
    protected Predicate<DataValue> getEquality(final DataColumnSpec runtimeColumnSpec, final FilterOperator<P> operator,
        final P filterParameters) throws InvalidSettingsException {
        final var type = runtimeColumnSpec.getType();
        if (!type.isCompatible(m_dataType.getPreferredValueClass())) { // not isASuperType!
            throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
                .withSummary("Operator \"%s\" for column \"%s\" expects data of type \"%s\", but got \"%s\""
                    .formatted(operator.getLabel(), runtimeColumnSpec.getName(), m_dataType.getName(), type.getName()))
                .addResolutions(
                    "Please select a different operator that is compatible with the column's data type \"%s\"."
                        .formatted(type.getName())));
        }
        final var cell = filterParameters.createCell();
        return dv -> cell.equals(dv.materializeDataCell());
    }

    @Override
    public final List<FilterOperator<P>> getOperators() {
        return List.of(new Equal(), new NotEqual(), new NotEqualNorMissing());
    }

    private final class Equal implements FilterOperator<P>, EqualsOperator {

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
            return EqualsOperatorFamily.this.getEquality(runtimeColumnSpec, this, filterParameters)::test;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_paramClass;
        }
    }

    private abstract class NotEqualBase implements FilterOperator<P> { // NOSONAR only private class

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureDataType, final P filterParameters) throws InvalidSettingsException {
            final var eq = EqualsOperatorFamily.this.getEquality(runtimeColumnSpec, this, filterParameters);
            return dv -> !eq.test(dv);
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_paramClass;
        }
    }

    private final class NotEqual extends NotEqualBase implements NotEqualsOperator {

        private NotEqual() {
            super();
        }

        @Override
        public boolean mapMissingTo() {
            return true;
        }

    }

    private final class NotEqualNorMissing extends NotEqualBase implements NotEqualsNorMissingOperator {

        private NotEqualNorMissing() {
            super();
        }

    }
}
