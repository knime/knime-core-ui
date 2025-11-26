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
 *   23 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.function.UnaryOperator;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.node.util.CheckUtils;

/**
 * Utility methods for validation in filter operators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FilterValidationUtil {

    private FilterValidationUtil() {
        // hidden
    }

    /**
     * Returns the exception resulting from the configured message builder.
     *
     * @param builderFn function to configure the message builder
     * @return invalid settings exception containing mandatory summary, details, and potential resolutions from the
     *         builder
     */
    public static InvalidSettingsException
        createInvalidSettingsException(final UnaryOperator<MessageBuilder> builderFn) {
        return builderFn.apply(Message.builder()).build().orElseThrow().toInvalidSettingsException();
    }

    /**
     * Returns the exception resulting from the configured message builder, including a cause.
     *
     * @param builderFn function to configure the message builder
     * @param cause the cause for the exception
     * @return invalid settings exception containing mandatory summary, details, and potential resolutions from the
     *         builder
     */
    public static InvalidSettingsException createInvalidSettingsException(final UnaryOperator<MessageBuilder> builderFn,
        final Throwable cause) {
        return builderFn.apply(Message.builder()).build().orElseThrow().toInvalidSettingsException(cause);
    }

    /**
     * Checks the given runtime column type and the data type "at configure time", i.e. when the dialog settings were
     * saved, to be the same. The error message will contain a helpful message with potential resolutions.
     *
     * @param operator the operator for context in error messages
     * @param runtimeColumnType column type at runtime
     * @param configureDataType column type "at configure time"
     * @throws InvalidSettingsException if the types are not the same
     */
    public static void checkSameType(final FilterOperatorBase operator, final DataType runtimeColumnType,
        final DataType configureDataType) throws InvalidSettingsException { //
        if (runtimeColumnType.equals(configureDataType)) {
            return;
        }
        throw createInvalidSettingsException(builder -> builder
            .withSummary(
                String.format("\"%s\" cannot compare data of type \"%s\" with a reference value of type \"%s\"",
                    operator.getLabel(), runtimeColumnType.toPrettyString(), configureDataType.toPrettyString()))
            .addTextIssue(
                String.format("The column data type \"%s\" is not the same as the reference value type \"%s\"",
                    runtimeColumnType.toPrettyString(), configureDataType.toPrettyString()))
            .addResolutions("Convert the input column to \"%s\".".formatted(configureDataType.toPrettyString()),
                "Reconfigure the node to use a reference value of type \"%s\"."
                    .formatted(runtimeColumnType.toPrettyString())));
    }

    /**
     * Append the given non-empty elements to the given prefix, quoting them with double quotes.
     *
     * @param prefix prefix to append to
     * @param elements elements to append, must not be empty
     * @return the prefix with the elements appended
     */
    public static StringBuilder appendElements(final StringBuilder prefix, final DataType... elements) {
        CheckUtils.checkArgument(elements.length > 0, "Cannot append empty elements array");
        final var quote = "\"";
        if (elements.length == 1) {
            return prefix.append(quote).append(elements[0]).append(quote);
        }
        if (elements.length == 2) {
            return prefix.append(quote).append(elements[0]).append(quote) //
                .append(" or ").append(quote).append(elements[1]).append(quote);
        }
        for (var i = 0; i < elements.length - 1; i++) {
            prefix.append(quote).append(elements[i]).append(quote).append(", ");
            if (i == elements.length - 2) {
                prefix.append("or ");
            }
        }
        return prefix;
    }

    /**
     * Gets a summary message indicating that the given operator cannot be applied to the given column of the given data
     * type.
     *
     * @param dataType the data type the operator is defined for
     * @param operator the operator
     * @param runtimeColumnSpec the column spec at runtime
     * @return the summary message
     */
    public static String getUnsupportedOperatorSummary(final DataType dataType,
        final FilterOperator<? extends FilterValueParameters> operator, final DataColumnSpec runtimeColumnSpec) { // NOSONAR we don't care about the parameters
        return String.format("\"%s\" comparison with \"%s\" for column \"%s\" of type \"%s\" is not possible",
            dataType.getName(), operator.getLabel(), runtimeColumnSpec.getName(),
            runtimeColumnSpec.getType().toPrettyString());
    }

    /**
     * Gets a resolution message indicating to convert the input column to one of the given compatible types.
     *
     * @param compatibleTypes compatible types to convert to
     * @return the resolution message
     */
    public static String resolutionChangeInput(final DataType... compatibleTypes) {
        return FilterValidationUtil
            .appendElements(new StringBuilder("Convert the input column to a compatible type, e.g. "), compatibleTypes)
            .toString();
    }

    /**
     * Gets a resolution message indicating to select a different operator that is compatible with the given column.
     *
     * @param colType the column type
     * @return the resolution message
     */
    public static String resolutionSelectDifferentOperator(final DataType colType) {
        return "Select a different operator that is compatible with the column's data type \"%s\"."
            .formatted(colType.toPrettyString());
    }

}
