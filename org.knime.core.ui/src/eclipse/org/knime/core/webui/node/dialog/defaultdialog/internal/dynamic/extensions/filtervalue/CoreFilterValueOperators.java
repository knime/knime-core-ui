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
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.DoubleCellParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.IntCellParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.LongCellParameters;
import org.knime.node.parameters.widget.choices.Label;

/**
 *
 * @author Paul Bärnreuther
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class CoreFilterValueOperators {

    enum CoreID { // TODO copied from FilterOperator in base
        /** Operator checking equality between values. */
        @Label(value = "Equals", description = """
                Value in column must be <b>equal</b> to the specified reference value.
                Equality is define by the particular data type(s) involved and may be on the value's string
                representation.
                """)
        EQ,

        /**
         * Operator checking inequality between values. In particular, two missing cells are considered neither
         * equal nor non-equal to each other (following the SQL semantic of nullable comparison with
         * {@code !=}/{@code <>}). See also {@link #NEQ_MISS}.
         */
        @Label(value = "Is not equal (nor missing)", description = """
                Value in column must be <b>not equal</b> to specified reference and also not missing.
                """)
        NEQ,

        /**
         * Operator checking inequality between values, but allowing for missing values. See also {@link #NEQ}.
         */
        @Label(value = "Is not equal", description = """
                Value in column must be <b>not equal</b> to specified reference value but can be missing.
                """)
        NEQ_MISS,

        /** Operator checking that the left-hand-side value is strictly less than the right-hand-side value. */
        @Label(value = "Less than",
            description = """
                    Value in column must be <b>strictly smaller</b> than specified value.
                    <br />

                    This operator is applicable for all data types that offer a more meaningful ordering than just
                    lexicographic ordering. In particular, this includes by default numeric types and Date &amp; Time types.
                    String and Boolean types are not supported.
                    The same requirements apply to the other ordering-based operators: "Less than", "Less than or equal",
                    "Greather than", and "Greater than or equal".
                    """) //
        LT, //

        /** Operator checking that the lhs value is less than or equal to the rhs value. */
        @Label(value = "Less than or equal", //
            description = "Value in column must be <b>smaller than or equal</b> to specified value") //
        LTE, //

        /** Operator checking that the lhs value is strictly greater than the rhs value. */
        @Label(value = "Greater than", //
            description = "Value in column must be <b>strictly larger</b> than specified value") //
        GT, //

        /** Operator checking that the lhs value is strictly greater than or equal to the rhs value. */
        @Label(value = "Greater than or equal", //
            description = "Value in column must be <b>larger than or equal</b> than specified value") //
        GTE, //

        /** Operator matching the first {@code n} rows. */
        @Label(value = "First n rows", description = """
                Matches the specified number of rows counted from the start of the input.
                """)
        FIRST_N_ROWS, //

        /** Operator matching the last {@code n} rows. */
        @Label(value = "Last n rows", description = """
                Matches the specified number of rows counted from the end of the input.
                """)
        LAST_N_ROWS, //

        /** Operator matching the lhs value with the given regular expression. */
        @Label(value = "Matches regex",
            description = """
                    Value in column must match the specified regular expression.
                    <br />

                    This operator is applicable to all data types that are string-compatible,
                    i.e. offer a meaningful string representation of themselves, or integral numbers.
                    In particular, this includes Date &amp; Time types.
                    The same requirements apply to the "Matches wildcard" operator.
                    <br /><br />

                    <b>Regex matching behavior:</b> By default, the regex pattern must match the whole cell value,
                    not just parts of it, since
                    the regex pattern is configured with the <tt>DOTALL</tt> and <tt>MULTILINE</tt> flags
                    <i>enabled</i>. To disable the <tt>DOTALL</tt> flag, prefix the pattern with <tt>(?-s)</tt>, to disable
                    <tt>MULTILINE</tt> use prefix <tt>(?-m)</tt>. To disable both, use <tt>(?-sm)</tt>.
                    """)
        REGEX,
        /** Operator matching the lhs value with the given wildcard pattern. */
        @Label(value = "Matches wildcard", description = "Value in column must match the specified pattern, "
            + "which may contain wildcards <tt>*</tt> and <tt>?</tt>.")
        WILDCARD,

        /** Operator checking that the lhs boolean cell value is true. */
        @Label(value = "Is true", description = "Boolean value in column must be <tt>true</tt>")
        IS_TRUE,

        /** Operator checking that the lhs boolean cell value is false. */
        @Label(value = "Is false", description = "Boolean value in column must be <tt>false</tt>")
        IS_FALSE,

        /** Operator checking that the lhs cell is missing. */
        @Label(value = "Is missing", description = "Value in column must be <i>missing</i>")
        IS_MISSING,

        /** Operator checking that the lhs cell is not missing. */
        @Label(value = "Is not missing", description = "Value in column must <em>not</em> be <i>missing</i>")
        IS_NOT_MISSING;

        /**
         * Utility method to extract the label from a CoreID enum constant's @Label annotation.
         *
         * @param coreId the CoreID enum constant
         * @return the label value from the @Label annotation, or the enum name if no annotation is found
         */
        public static String getLabel(final CoreID coreId) {
            try {
                final var field = CoreID.class.getField(coreId.name());
                final var labelAnnotation = field.getAnnotation(Label.class);
                return labelAnnotation != null ? labelAnnotation.value() : coreId.name();
            } catch (final NoSuchFieldException e) {
                return coreId.name();
            }
        }
    }

    private CoreFilterValueOperators() {
    }

    static final class IntCellOperators implements FilterOperators {

        @Override
        public DataType getDataType() {
            return IntCell.TYPE;
        }

        @Override
        public List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies() {
            return List.of(new IntEqualityFamily(IntCell.TYPE, IntCellParameters.class),
                new IntComparisonFamily(IntCell.TYPE, IntCellParameters.class));
        }

        private static final class IntEqualityFamily extends EqualsOperatorFamily<IntCell, IntCellParameters> {

            private IntEqualityFamily(final DataType dataType, final Class<IntCellParameters> paramClass) {
                super(dataType, paramClass);
            }

            @Override
            public BiPredicate<DataValue, IntCell> getEquality(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<IntCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompLongVal = colType.isCompatible(LongValue.class);
                final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
                if (!(isCompIntVal || isCompLongVal || isCompDoubleVal)) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "), IntCell.TYPE,
                                DoubleCell.TYPE, LongCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> ((IntValue)v).getIntValue() == c.getIntValue();
                }
                if (isCompLongVal) {
                    return (v, c) -> ((LongValue)v).getLongValue() == c.getIntValue();
                }
                // isCompDoubleVal
                return (v, c) -> ((DoubleValue)v).getDoubleValue() == c.getIntValue(); // NOSONAR
            }
        }

        private static final class IntComparisonFamily extends ComparableOperatorFamily<IntCell, IntCellParameters> {

            private IntComparisonFamily(final DataType dataType, final Class<IntCellParameters> parametersClass) {
                super(dataType, parametersClass);
            }

            @Override
            protected ToIntBiFunction<DataValue, IntCell> getComparator(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<IntCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompLongVal = colType.isCompatible(LongValue.class);
                final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
                if (!(isCompIntVal || isCompLongVal || isCompDoubleVal)) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "), IntCell.TYPE,
                                DoubleCell.TYPE, LongCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> Integer.compare(((IntValue)v).getIntValue(), c.getIntValue());
                }
                if (isCompLongVal) {
                    return (v, c) -> Long.compare(((LongValue)v).getLongValue(), c.getIntValue());
                }
                // isCompDoubleVal
                return (v, c) -> Double.compare(((DoubleValue)v).getDoubleValue(), c.getIntValue());
            }

        }
    }

    static final class LongCellOperators implements FilterOperators {
        @Override
        public DataType getDataType() {
            return LongCell.TYPE;
        }

        @Override
        public List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies() {
            return List.of(new LongEqualityFamily(LongCell.TYPE, LongCellParameters.class),
                new LongComparisonFamily(LongCell.TYPE, LongCellParameters.class));
        }

        private static final class LongEqualityFamily extends EqualsOperatorFamily<LongCell, LongCellParameters> {

            private LongEqualityFamily(final DataType dataType, final Class<LongCellParameters> paramClass) {
                super(dataType, paramClass);
            }

            @Override
            public BiPredicate<DataValue, LongCell> getEquality(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<LongCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompLongVal = colType.isCompatible(LongValue.class);
                if (!isCompLongVal && !isCompIntVal) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "),
                                LongCell.TYPE, IntCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> ((IntValue)v).getIntValue() == c.getLongValue();
                }
                // isCompLongVal
                return (v, c) -> ((LongValue)v).getLongValue() == c.getLongValue();
            }
        }

        private static final class LongComparisonFamily extends ComparableOperatorFamily<LongCell, LongCellParameters> {

            private LongComparisonFamily(final DataType dataType, final Class<LongCellParameters> parametersClass) {
                super(dataType, parametersClass);
            }

            @Override
            protected ToIntBiFunction<DataValue, LongCell> getComparator(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<LongCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompLongVal = colType.isCompatible(LongValue.class);
                final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
                if (!(isCompIntVal || isCompLongVal || isCompDoubleVal)) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "), IntCell.TYPE,
                                DoubleCell.TYPE, LongCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> Long.compare(((IntValue)v).getIntValue(), c.getLongValue());
                }
                if (isCompLongVal) {
                    return (v, c) -> Long.compare(((LongValue)v).getLongValue(), c.getLongValue());
                }
                // isCompDoubleVal
                return (v, c) -> Double.compare(((DoubleValue)v).getDoubleValue(), c.getLongValue());
            }
        }
    }

    static final class DoubleCellOperators implements FilterOperators {
        @Override
        public DataType getDataType() {
            return DoubleCell.TYPE;
        }

        @Override
        public List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies() {
            return List.of(new DoubleEqualityFamily(DoubleCell.TYPE, DoubleCellParameters.class),
                new DoubleComparisonFamily(DoubleCell.TYPE, DoubleCellParameters.class));
        }

        private static final class DoubleEqualityFamily extends EqualsOperatorFamily<DoubleCell, DoubleCellParameters> {

            private DoubleEqualityFamily(final DataType dataType, final Class<DoubleCellParameters> paramClass) {
                super(dataType, paramClass);
            }

            @Override
            public BiPredicate<DataValue, DoubleCell> getEquality(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<DoubleCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
                if (!isCompDoubleVal && !isCompIntVal) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "),
                                DoubleCell.TYPE, IntCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> ((IntValue)v).getIntValue() == c.getDoubleValue(); // NOSONAR
                }
                // isCompDoubleVal
                return (v, c) -> ((DoubleValue)v).getDoubleValue() == c.getDoubleValue(); // NOSONAR
            }
        }

        private static final class DoubleComparisonFamily
            extends ComparableOperatorFamily<DoubleCell, DoubleCellParameters> {

            private DoubleComparisonFamily(final DataType dataType, final Class<DoubleCellParameters> parametersClass) {
                super(dataType, parametersClass);
            }

            @Override
            protected ToIntBiFunction<DataValue, DoubleCell> getComparator(final DataColumnSpec runtimeColumnSpec,
                final FilterOperator<DoubleCellParameters> operator) throws InvalidSettingsException {
                final var colType = runtimeColumnSpec.getType();
                final var isCompIntVal = colType.isCompatible(IntValue.class);
                final var isCompLongVal = colType.isCompatible(LongValue.class);
                final var isCompDoubleVal = colType.isCompatible(DoubleValue.class);
                if (!(isCompIntVal || isCompLongVal || isCompDoubleVal)) {
                    throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> builder
                        .withSummary("Operator \"%s\" for column \"%s\" is not supported for type \"%s\"".formatted(
                            operator.getLabel(), runtimeColumnSpec.getName(), colType.getName()))
                        .addResolutions(
                            // change input
                            ValueFilterValidationUtil.appendElements(
                                new StringBuilder("Convert the input column to a compatible type, e.g. "), IntCell.TYPE,
                                DoubleCell.TYPE, LongCell.TYPE).toString(),
                            // reconfigure
                            "Please select a different operator that is compatible with the column's data type \"%s\"." // NOSONAR
                                .formatted(colType.getName())));
                }
                if (isCompIntVal) {
                    return (v, c) -> Double.compare(((IntValue)v).getIntValue(), c.getDoubleValue());
                }
                if (isCompLongVal) {
                    return (v, c) -> Double.compare(((LongValue)v).getLongValue(), c.getDoubleValue());
                }
                // isCompDoubleVal
                return (v, c) -> Double.compare(((DoubleValue)v).getDoubleValue(), c.getDoubleValue());
            }
        }
    }

}
