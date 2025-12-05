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
import org.knime.core.data.v2.IndexedRowReadPredicate;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persistor;

/**
 * Filter operator that defines a predicate based on {@link DataValue}s of multiple concrete types configured by
 * {@link FilterValueParameters parameters}.
 *
 * @param <P> the type of parameters to create the predicate with
 *
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public interface FilterOperator<P extends FilterValueParameters> extends FilterOperatorDefinition<P> {

    /**
     * Creates the predicate for filtering data values of type {@code V} based on the given filter parameters.
     *
     * For validation of the given runtime spec, you can use the methods in {@link FilterValidationUtil}.
     *
     * @param runtimeColumnSpec the column spec at runtime, whose values are to be filtered
     * @param configureColumnType the column type at configuration time
     * @param filterParameters the parameters to create the predicate with
     * @return the predicate for filtering data values of type {@code V}
     * @throws InvalidSettingsException in case the given column spec or filtered parameters are invalid for the
     *             operator
     */
    Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec, final DataType configureColumnType,
        P filterParameters) throws InvalidSettingsException;

    /**
     * Creates an {@link IndexedRowReadPredicate} based on the given operator and filter parameters.
     *
     * The returned predicate handles missing values according to the operator's {@link #mapMissingTo() mapping}.
     *
     * @param operator operator to create the predicate with
     * @param runtimeColumnSpec the column spec at runtime, whose values are to be filtered
     * @param columnIndex the index of the column to filter
     * @param configureColumnType the column type at configuration time
     * @param filterParameters the parameters to create the predicate with
     * @return the created predicate
     * @throws InvalidSettingsException in case the predicate could not be created due to invalid settings
     */
    static IndexedRowReadPredicate toPredicate(final FilterOperator<FilterValueParameters> operator,
        final DataColumnSpec runtimeColumnSpec, final int columnIndex, final DataType configureColumnType,
        final FilterValueParameters filterParameters) throws InvalidSettingsException {
        final var predicate = CheckUtils.checkSettingNotNull(
            operator.createPredicate(runtimeColumnSpec, configureColumnType, filterParameters),
            "Operator \"%s\" returned 'null' predicate and can thus not be evaluated", operator.getLabel());
        final var missingAsTrue = operator.mapMissingTo();
        return (index, read) -> {
            if (read.isMissing(columnIndex)) {
                return missingAsTrue;
            }
            final var dataValue = read.getValue(columnIndex);
            return predicate.test(dataValue);
        };
    }

    /**
     * Specifies whether a missing cell should match the predicate created by this operator or not.
     *
     * @implNote The default implementation returns {@code false}, i.e. comparison with missing cells are mapped to
     *           {@code false} -- criteria never match missing cells.
     *
     * @return {@code true} if missing cells should be considered to match the predicate, {@code false} otherwise
     */
    default boolean mapMissingTo() {
        return false;
    }

    /**
     * Indicates that the operator is deprecated. A deprecated operator may not be shown in the UI anymore. When a
     * dialog with a deprecated operator is loaded, the operator is either shown as missing operator or replaced by
     * another operator with the same id if it exists. However, existing configurations with deprecated operators are
     * still valid and can be executed.
     *
     * Deprecating an existing operator is the only way to add a new operator with the same id. If an operator should be
     * changed but not deprecated, the parameters need to be backwards compatible to the existing parameters (using
     * {@link Persistor} or {@link Migration} annotations)
     *
     * @return {@code true} if the operator is deprecated, {@code false} otherwise
     */
    default boolean isDeprecated() {
        return false;
    }
}
