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

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;

/**
 * Operator family for data types that have a comparator.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <C> data cell type to compare with
 * @param <P> type of filter parameters
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ComparableOperatorFamily<C extends DataCell, P extends SingleCellValueParameters<C>>
    implements FilterOperatorFamily<P> {

    private final DataType m_dataType;

    private final Class<P> m_parametersClass;

    /**
     * Creates the operator family for the given data type and parameters class.
     * @param dataType the data type this family is for
     * @param parametersClass the class of parameters used for all operators of this family
     */
    public ComparableOperatorFamily(final DataType dataType, final Class<P> parametersClass) {
        m_dataType = dataType;
        m_parametersClass = parametersClass;
    }

    /**
     * Returns the data type this operator family is defined for.
     * @return the data type
     */
    protected final DataType getDataType() {
        return m_dataType;
    }

    /**
     * Creates a comparison function to compare between a data value and a reference cell created from the parameters
     * for all operators of this family. The specific operator is passed for context, e.g. to create specific error
     * messages for users.
     *
     * @param runtimeColumnSpec the column spec of the column to filter
     * @param operator the operator for which the comparator is requested
     * @return the comparison function (see {@link Comparator})
     * @throws InvalidSettingsException if the operator is not compatible with the column spec, i.e. will not produce a
     *             useful comparison
     */
    protected ToIntBiFunction<DataValue, C> getComparator(final DataColumnSpec runtimeColumnSpec,
        final FilterOperator<P> operator) throws InvalidSettingsException {
        final var type = runtimeColumnSpec.getType();
        if (!type.isCompatible(m_dataType.getPreferredValueClass())) { // not isASuperType!
            throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
                .withSummary("Operator \"%s\" for column \"%s\" expects data of type \"%s\", but got \"%s\""
                    .formatted(operator.getLabel(), runtimeColumnSpec.getName(), m_dataType.getName(), type.getName()))
                .addResolutions(
                    "Please select a different operator that is compatible with the column's data type \"%s\"."
                        .formatted(type.getName())));
        }
        final var comparator = new DataValueComparatorDelegator<>(m_dataType.getComparator());
        return comparator::compare;
    }

    @Override
    public List<FilterOperator<P>> getOperators() {
        return List.of(new LessThanOperatorImpl(), new LessThanOrEqualOperatorImpl(), new GreaterThanOperatorImpl(),
            new GreaterThanOrEqualOperatorImpl());
    }

    private final class LessThanOperatorImpl implements FilterOperator<P>, LessThanOperator {

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException { //
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) < 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class LessThanOrEqualOperatorImpl implements FilterOperator<P>, LessThanOrEqualOperator {

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException { //
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) <= 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class GreaterThanOperatorImpl implements FilterOperator<P>, GreaterThanOperator {

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException { //
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) > 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }

    private final class GreaterThanOrEqualOperatorImpl implements FilterOperator<P>, GreaterThanOrEqualOperator {

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException { //
            final var value = filterParameters.createCell();
            final var comparator = getComparator(runtimeColumnSpec, this);
            return dv -> comparator.applyAsInt(dv, value) >= 0;
        }

        @Override
        public Class<P> getNodeParametersClass() {
            return m_parametersClass;
        }
    }
}
