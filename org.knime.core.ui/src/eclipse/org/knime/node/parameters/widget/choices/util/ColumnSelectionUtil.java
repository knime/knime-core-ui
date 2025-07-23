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
 *   Mar 24, 2025 (paulbaernreuther): created
 */
package org.knime.node.parameters.widget.choices.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.node.parameters.NodeParametersInput;

/**
 * A utility class around selecting columns in a dialog for either filtering columns or selecting a single one.
 *
 * @author Paul Bärnreuther
 */
public final class ColumnSelectionUtil {

    private ColumnSelectionUtil() {
        // Utility class
    }

    /**
     * Returns all columns of the specified port.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @return all columns of the specified port. Empty if not connected or no columns.
     */
    public static List<DataColumnSpec> getAllColumns(final NodeParametersInput context,
        final int portIndex) {
        return context.getInTableSpec(portIndex).stream().flatMap(DataTableSpec::stream).toList();
    }

    /**
     * Returns all columns of the first port.
     *
     * @param context the current context
     * @return all columns of the first port. Empty if not connected or no columns.
     */
    public static List<DataColumnSpec> getAllColumnsOfFirstPort(final NodeParametersInput context) {
        return getAllColumns(context, 0);
    }

    /**
     * Returns the first column of the specified port.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @return the first column of the specified port. Empty if not connected or no columns.
     */
    public static Optional<DataColumnSpec> getFirstColumn(final NodeParametersInput context,
        final int portIndex) {
        return context.getInTableSpec(portIndex).stream().flatMap(DataTableSpec::stream).findFirst();
    }

    /**
     * Returns the first column of the first port.
     *
     * @param context the current context
     * @return the first column of the first port. Empty if not connected or no columns.
     */
    public static Optional<DataColumnSpec> getFirstColumnOfFirstPort(final NodeParametersInput context) {
        return getFirstColumn(context, 0);
    }

    /**
     * Returns the first column of the specified port which satisfies a certain condition.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @param filter the condition to apply
     * @return the first matching column of the specified port. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getFilteredColumns(final NodeParametersInput context,
        final int portIndex, final Predicate<DataColumnSpec> filter) {
        return context.getInTableSpec(portIndex).stream().flatMap(DataTableSpec::stream).filter(filter).toList();
    }

    /**
     * Returns the first column of the first port which is compatible to certain columns.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @param valueClasses the classes to check compatibility against
     * @return the first compatible column of the specified port. Empty if not connected or no such columns.
     */
    @SafeVarargs
    public static List<DataColumnSpec> getCompatibleColumns(final NodeParametersInput context,
        final int portIndex, final Class<? extends DataValue>... valueClasses) {
        return getFilteredColumns(context, portIndex, toFilter(valueClasses));
    }

    /**
     * Returns the first column of the first port which is compatible to certain columns.
     *
     * @param context the current context
     * @param valueClasses the classes to check compatibility against
     * @return the first compatible column of the first. Empty if not connected or no such columns.
     */
    @SafeVarargs
    public static List<DataColumnSpec> getCompatibleColumnsOfFirstPort(final NodeParametersInput context,
        final Class<? extends DataValue>... valueClasses) {
        return getCompatibleColumns(context, 0, valueClasses);
    }

    /**
     * Returns the first column of the specified port which is a string column.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @return the first string column of the specified port. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getStringColumns(final NodeParametersInput context,
        final int portIndex) {
        return getCompatibleColumns(context, portIndex, StringValue.class);
    }

    /**
     * Returns the string columns within a spec.
     *
     * @param inSpec a data table spec
     * @return the string columns.
     */
    public static List<DataColumnSpec> getStringColumns(final DataTableSpec inSpec) {
        return getCompatibleColumns(inSpec, StringValue.class);
    }

    /**
     * Returns the first string column within a spec.
     *
     * @param inSpec a data table spec
     * @return the first string columns.
     */
    public static Optional<DataColumnSpec> getFirstStringColumn(final DataTableSpec inSpec) {
        return getFirstCompatibleColumn(inSpec, StringValue.class);
    }

    /**
     * Returns the first column of the first port which is a string column.
     *
     * @param context the current context
     * @return the first string column of the first port. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getStringColumnsOfFirstPort(final NodeParametersInput context) {
        return getStringColumns(context, 0);
    }

    /**
     * Returns the columns of the specified port which are double columns.
     *
     * @param context the current context
     * @param portIndex the port index which has to be a data table port.
     * @return the first double column of the specified port. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getDoubleColumns(final NodeParametersInput context,
        final int portIndex) {
        return getCompatibleColumns(context, portIndex, DoubleValue.class);
    }

    /**
     * Returns the numeric columns within a spec.
     *
     * @param inSpec a data table spec
     * @return the number columns.
     */
    public static List<DataColumnSpec> getDoubleColumns(final DataTableSpec inSpec) {
        return getCompatibleColumns(inSpec, DoubleValue.class);
    }

    /**
     * Returns the first numeric column within a spec.
     *
     * @param inSpec a data table spec
     * @return the first number columns.
     */
    public static Optional<DataColumnSpec> getFirstDoubleColumn(final DataTableSpec inSpec) {
        return getFirstCompatibleColumn(inSpec, DoubleValue.class);
    }

    /**
     * Returns the first column of the first port which is a double column.
     *
     * @param context the current context
     * @return the first double column of the first port. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getDoubleColumnsOfFirstPort(final NodeParametersInput context) {
        return getDoubleColumns(context, 0);
    }

    /**
     * Returns the columns within a spec that are compatible to at least one of the given value classes.
     *
     * @param spec a data table spec
     * @param valueClasses the classes to check compatibility against
     * @return the columns compatible to one of the valueClasses. Empty if not connected or no such columns.
     */
    @SafeVarargs
    public static List<DataColumnSpec> getCompatibleColumns(final DataTableSpec spec,
        final Class<? extends DataValue>... valueClasses) {
        return streamSpec(spec).filter(toFilter(valueClasses)).toList();
    }

    /**
     * Returns the columns within a spec that are compatible to at least one of the given value classes.
     *
     * @param spec a data table spec
     * @param valueClasses the classes to check compatibility against
     * @return the columns compatible to one of the valueClasses. Empty if not connected or no such columns.
     */
    public static List<DataColumnSpec> getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {
        return streamSpec(spec).filter(toFilter(valueClasses)).toList();
    }

    /**
     * Returns the first column within a spec that is compatible to at least one of the given value classes.
     *
     * @param spec a data table spec
     * @param valueClasses the classes to check compatibility against
     * @return the first column compatible to one of the valueClasses. Empty if not connected or no such columns.
     */
    @SafeVarargs
    public static Optional<DataColumnSpec> getFirstCompatibleColumn(final DataTableSpec spec,
        final Class<? extends DataValue>... valueClasses) {
        return streamSpec(spec).filter(toFilter(valueClasses)).findFirst();
    }

    /**
     * Returns the first column within a spec that is compatible to at least one of the given value classes.
     *
     * @param spec a data table spec
     * @param valueClasses the classes to check compatibility againsts
     * @return the first column compatible to one of the valueClasses. Empty if not connected or no such columns.
     */
    public static Optional<DataColumnSpec> getFirstCompatibleColumn(final DataTableSpec spec,
        final List<Class<? extends DataValue>> valueClasses) {
        return streamSpec(spec).filter(toFilter(valueClasses)).findFirst();
    }

    private static Stream<DataColumnSpec> streamSpec(final DataTableSpec spec) {
        if (spec == null) {
            return Stream.empty();
        }
        return spec.stream();
    }

    private static Predicate<DataColumnSpec> toFilter(final Class<? extends DataValue>[] valueClasses) {
        return toFilter(() -> Stream.of(valueClasses));
    }

    private static Predicate<DataColumnSpec> toFilter(final Collection<Class<? extends DataValue>> valueClasses) {
        return toFilter(valueClasses::stream);
    }

    private static Predicate<DataColumnSpec> toFilter(final Supplier<Stream<Class<? extends DataValue>>> valueClasses) {
        return col -> valueClasses.get().anyMatch(valueClass -> col.getType().isCompatible(valueClass));
    }

}
