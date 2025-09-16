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

import org.knime.core.data.DataCell;

/**
 * Implement this interface to define any of the standard filter operators for data cells:
 * <ul>
 * <li>Use a {@link EqualsOperator} to define the "equals" operator and other operators derived from it (like "not
 * equals")</li>
 * <li>Use a {@link LessThanOperator} to define the "less than" operator and with that also "greater than or equals"
 * (note that this can be different to the union of "greater than" and "equals").</li>
 * <li>Use a {@link GreaterThanOperator} to define the "greater than" operator and with that also "less than or equals"
 * (note that this can be different to the union of "less than" and "equals").</li>
 *
 * @param <V> the data value this operator works on
 * @param <P> the type of filter value parameters this operator works with
 *
 * @author Paul Bärnreuther
 */
public
//sealed
interface BuiltinOperator<V extends DataCell, P extends FilterValueParameters>
//    extends ValueFilterOperator<V, P>
    {
//
//    @Override
//    default String getId() {
//        throw new UnsupportedOperationException("Unimplemented method 'getId'");
//    }
//
//    @Override
//    default String getLabel() {
//        throw new UnsupportedOperationException("Unimplemented method 'getDisplayName'");
//    }
//
//    sealed interface OperatorFamily<V extends DataCell, P extends FilterValueParameters> extends BuiltinOperator<V, P>
//        permits EqualsOperatorFamily, ComparableOperatorFamily {
//
//        @Override
//        default Predicate<V> createPredicate(final P filterParameters) {
//            throw new UnsupportedOperationException(
//                "An operator family is not supposed to create a predicate directly.");
//        }
//
//    }
//
//    /**
//     * The family of operators for equality comparisons based on a {@link DataValueComparator}'s return value.
//     * <b>Note:</b> This can be different to the equality defined by a data cell's equal method.
//     *
//     * @param <V> the data value type this operator family works on
//     * @param <P> the type of filter value parameters this operator family works with
//     */
//    // TODO this could be a marker on the ComparableOperatorFamily?
//    non-sealed interface EqualsOperatorFamily<V extends DataCell, P extends SingleCellValueParameters<V>>
//        extends OperatorFamily<V, P>, EqualsOperatorMarker<V, P> {
//    }
//
//    /**
//     * Use this class to implement comparison operators (less than, greater than, etc.) for data cells that can be
//     * compared to each other (using {@link DataType#getComparator()}.
//     *
//     * @param <V> the data cell type this operator family works for
//     * @param <P> the type of filter value parameters
//     */
//    non-sealed interface ComparableOperatorFamily<V extends DataCell, P extends SingleCellValueParameters<V>>
//        extends OperatorFamily<V, P>, LessThanOperatorMarker<V, P>, GreaterThanOperatorMarker<V, P> {
//    }
//
//    /**
//     * Usually, us the {@link SingleCellOperatorFamily} instead of implementing this interface directly. But if e.g.
//     * secondary parameters are needed, this interface can be implemented directly.
//     *
//     * Providing an implementation of this interface automatically registers the operator for the "equals" operator and
//     * other built-in operators derived from it (like "not equals").
//     *
//     * @param <C> the data cell this operator works for
//     * @param <T> the type of filter value parameters
//     */
//    non-sealed interface EqualsOperatorMarker<V extends DataCell, P extends FilterValueParameters>
//        extends BuiltinOperator<V, P> {
//
//        // names from proposal of AP-24054
//        default String labelForEqual() {
//            return "Equal";
//        }
//
//        default String labelForNotEqual() {
//            return "Not equal";
//        }
//
//        default String labelForNotEqualNorMissing() {
//            return "Not equal nor missing";
//        }
//
//        V from(P params);
//    }
//
//    /**
//     * Usually, us the {@link SingleCellOperatorFamily.ComparableCellOperatorFamily} instead of implementing this
//     * interface directly. But if e.g. secondary parameters are needed, this interface can be implemented directly.
//     *
//     * Providing an implementation of this interface automatically registers the operator for the "less than" operator
//     * and with that also "greater or equals" (note that this can be different to the sum of "greater than" and
//     *
//     * @param <C> the data cell this operator works for
//     * @param <T> the type of filter value parameters
//     */
//    non-sealed interface LessThanOperatorMarker<V extends DataCell, P extends FilterValueParameters>
//        extends BuiltinOperator<V, P> {
//        V from(P params);
//    }
//
//    /**
//     * Usually, us the {@link SingleCellOperatorFamily.ComparableCellOperatorFamily} instead of implementing this
//     * interface directly. But if e.g. secondary parameters are needed, this interface can be implemented directly.
//     *
//     * Providing an implementation of this interface automatically registers the operator for the "greater than"
//     * operator and with that also "less or equals" (note that this can be different to the sum of "less than" and
//     * "equals").
//     *
//     * @param <C> the data cell this operator works for
//     * @param <T> the type of filter value parameters
//     */
//    non-sealed interface GreaterThanOperatorMarker<V extends DataCell, P extends FilterValueParameters>
//        extends BuiltinOperator<V, P> {
//        V from(P params);
//    }

}
