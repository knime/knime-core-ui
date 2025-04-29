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
 *   23 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;

/**
 * Utility class which provides helper methods for the validation of a date&time format setting annotated with the
 * {@link DateTimeFormatPickerWidget}.
 *
 * @author Robin Gerling
 */
public final class DateTimeFormatValidationUtil {

    private DateTimeFormatValidationUtil() {
    }

    /**
     * Validates whether the given format is valid w.r.t the temporal type and potentially throws if the format is
     * invalid for the given type.
     *
     * @param temporalFormat the temporal format to validate
     * @throws InvalidSettingsException if the format is invalid
     */
    public static void validateFormat(final TemporalFormat temporalFormat) throws InvalidSettingsException {
        validateFormatAndPossiblyThrow(temporalFormat.format(), temporalFormat.temporalType());

    }

    /**
     * Validates whether the given format is valid and potentially throws if the format is invalid.
     *
     * @param format the format to validate
     * @throws InvalidSettingsException if the format is invalid
     */
    public static void validateFormat(final String format) throws InvalidSettingsException {
        validateFormatAndPossiblyThrow(format, FormatTemporalType.ZONED_DATE_TIME);
    }

    private static void validateFormatAndPossiblyThrow(final String format, final FormatTemporalType formatTemporalType)
        throws InvalidSettingsException {
        final var ex = validateFormatGivenTemporalType(format, formatTemporalType);
        if (ex.isPresent()) {
            throw new InvalidSettingsException(
                "Invalid date&time format '%s'. Reason: %s".formatted(format, ex.get().getMessage()), ex.get());
        }
    }

    /**
     * Is a given type compatible with a given pattern?
     *
     * @param pattern the pattern like 'yyyy-MM-dd'.
     * @param type the type with which to check compatibility.
     * @return true if the type is compatible with the pattern, else false
     */
    public static boolean isTypeCompatibleWithPattern(final String pattern, final FormatTemporalType type) {
        return validateFormatGivenTemporalType(pattern, type).isEmpty();
    }

    private static Optional<String> validateFormatAndReturnMessage(final String format,
        final FormatTemporalType formatType) {
        final var ex = validateFormatGivenTemporalType(format, formatType);
        return ex.map(e -> "Invalid format: %s".formatted(e.getMessage()));
    }

    private static Optional<Exception> validateFormatGivenTemporalType(final String format,
        final FormatTemporalType formatType) {
        Temporal toFormat = switch (formatType) {
            case DATE -> LocalDate.now();
            case TIME -> LocalTime.now();
            case DATE_TIME -> LocalDateTime.now();
            case ZONED_DATE_TIME -> ZonedDateTime.now();
        };

        try {
            DateTimeFormatter.ofPattern(format).format(toFormat);
        } catch (Exception ex) {
            return Optional.of(ex);
        }
        return Optional.empty();
    }

    /**
     * The class used if the settings field uses the type {@link String}.
     */
    public static final class DateTimeStringFormatValidation extends ExternalValidation<String> {

        @Override
        Optional<String> validate(final String currentValue) {
            return validateFormatAndReturnMessage(currentValue, FormatTemporalType.ZONED_DATE_TIME);
        }
    }

    /**
     * The class used if the settings field uses the type {@link TemporalFormat}.
     */
    public static final class DateTimeTemporalFormatValidation extends ExternalValidation<TemporalFormat> {

        @Override
        Optional<String> validate(final TemporalFormat currentValue) {
            return validateFormatAndReturnMessage(currentValue.format(), currentValue.temporalType());
        }
    }

}
