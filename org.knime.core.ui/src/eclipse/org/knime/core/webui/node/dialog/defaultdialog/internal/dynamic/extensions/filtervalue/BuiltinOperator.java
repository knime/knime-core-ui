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

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;

/**
 * implement this interface to define any of the standard filter operators for data cells:
 * <ul>
 * <li>Use a {@link EqualsOperator} to define the "equals" operator and other operators derived from it (like "not
 * equals")</li>
 * <li>Use a {@link LessThanOperator} to define the "less than" operator and with that also "greater than or equals"
 * (note that this can be different to the union of "greater than" and "equals").</li>
 * <li>Use a {@link GreaterThanOperator} to define the "greater than" operator and with that also "less than or equals"
 * (note that this can be different to the union of "less than" and "equals").</li>
 *
 * @author Paul Bärnreuther
 */
public sealed interface BuiltinOperator<T extends FilterValueParameters> extends FilterOperator2<T> {

    @Override
    default String getId() {
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }

    @Override
    default String getDisplayName() {
        throw new UnsupportedOperationException("Unimplemented method 'getDisplayName'");
    }

    /**
     * Uses {@link SingleCellValueParameters#createCell} to create predicates from cell operations. To implement
     * comparison operators, extend {@link ComparableCellOperatorFamily}.
     */
    public sealed class SingleCellOperatorFamily<T extends SingleCellValueParameters<?>> implements EqualsOperator<T> {

        private final Class<T> m_singleCellValueParametersClass;

        SingleCellOperatorFamily(final Class<T> singleCellValueParametersClass) {
            m_singleCellValueParametersClass = singleCellValueParametersClass;
        }

        /**
         * Not to be called directly. Use {@link SingleCellValueParameters#createCell} instead.
         */
        @Override
        public DataValuePredicate createPredicate(final T filterParameters) {
            throw new UnsupportedOperationException("Unimplemented method 'createPredicate'");
        }

        @Override
        public Class<T> getNodeParametersClass() {
            return m_singleCellValueParametersClass;
        }

        @Override
        public void validateOtherType(final DataType otherDataType) throws InvalidSettingsException {
            throw new InvalidSettingsException("");
        }

        /**
         * Use this class to implement comparison operators (less than, greater than, etc.) for data cells that can be
         * compared to each other (using {@link DataType#getComparator()}.
         *
         * @param <T> the type of filter value parameters
         */
        public static final class ComparableCellOperatorFamily<T extends SingleCellValueParameters<?>>
            extends SingleCellOperatorFamily<T> implements LessThanOperator<T>, GreaterThanOperator<T> {

            ComparableCellOperatorFamily(final Class<T> singleCellValueParametersClass) {
                super(singleCellValueParametersClass);
            }

        }

    }

    /**
     * Usually, us the {@link SingleCellOperatorFamily} instead of implementing this interface directly. But if e.g.
     * secondary parameters are needed, this interface can be implemented directly.
     *
     * Providing an implementation of this interface automatically registers the operator for the "equals" operator and
     * other built-in operators derived from it (like "not equals").
     *
     * @param <T> the type of filter value parameters
     */
    public non-sealed interface EqualsOperator<T extends FilterValueParameters> extends BuiltinOperator<T> {
    }

    /**
     * Usually, us the {@link SingleCellOperatorFamily.ComparableCellOperatorFamily} instead of implementing this
     * interface directly. But if e.g. secondary parameters are needed, this interface can be implemented directly.
     *
     * Providing an implementation of this interface automatically registers the operator for the "less than" operator
     * and with that also "greater or equals" (note that this can be different to the sum of "greater than" and
     *
     * @param <T> the type of filter value parameters
     */
    public non-sealed interface LessThanOperator<T extends FilterValueParameters> extends BuiltinOperator<T> {
    }

    /**
     * Usually, us the {@link SingleCellOperatorFamily.ComparableCellOperatorFamily} instead of implementing this
     * interface directly. But if e.g. secondary parameters are needed, this interface can be implemented directly.
     *
     * Providing an implementation of this interface automatically registers the operator for the "greater than"
     * operator and with that also "less or equals" (note that this can be different to the sum of "less than" and
     * "equals").
     *
     * @param <T> the type of filter value parameters
     */
    public non-sealed interface GreaterThanOperator<T extends FilterValueParameters> extends BuiltinOperator<T> {
    }

}
