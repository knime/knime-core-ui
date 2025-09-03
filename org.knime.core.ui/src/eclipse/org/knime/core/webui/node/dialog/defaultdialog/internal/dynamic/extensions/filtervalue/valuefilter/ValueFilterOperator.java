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
 *   Sep 23, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.valuefilter;

import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;

/**
 * A filter operator for a specific {@link DataType} that will filter {@link DataValue}s of type {@code V}.
 *
 * @param <V> the type of data value the operator works on
 * @param <P> the type of parameters the operator takes
 * @author Paul Bärnreuther
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface ValueFilterOperator<V extends DataValue, P extends FilterValueParameters> extends FilterOperator<P> {

    @SuppressWarnings("unchecked")
    @Override
    default Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
        final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
        if (!runtimeColumnSpec.getType().isCompatible(getDataType().getPreferredValueClass())) {
            throw ValueFilterValidationUtil
                .createInvalidSettingsException(builder -> builder
                    .withSummary("Operator \"%s\" for column \"%s\" expects data of type \"%s\", but got \"%s\""
                        .formatted(getLabel(), runtimeColumnSpec.getName(), getDataType().getName(),
                            runtimeColumnSpec.getType().getName()))
                    .addResolutions(
                        "Please select a different operator that is compatible with the column's data type \"%s\"."
                            .formatted(runtimeColumnSpec.getType().getName())));
        }
        final var typedPredicate = createTypedPredicate(runtimeColumnSpec, filterParameters);
        return v -> typedPredicate.test((V)v);
    }

    /**
     * Creates the {@code V}-typed predicate for the given runtime column spec and filter parameters.
     *
     * @param runtimeColumnSpec column spec at runtime
     * @param filterParameters node parameters
     * @return the typed predicate
     * @throws InvalidSettingsException in case the parameters or column spec are invalid to produce the predicate
     */
    Predicate<V> createTypedPredicate(final DataColumnSpec runtimeColumnSpec, final P filterParameters)
        throws InvalidSettingsException;

    /**
     * The data type this operator works on.
     *
     * @return the data type
     */
    DataType getDataType();

}
