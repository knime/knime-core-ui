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
 *   29 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.validateFormat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;

/**
 *
 * @author Robin Gerling
 */
public class DateTimeFormatValidationUtilTest {

    /*
     * See DateTimeFormatter for restrictions on patterns:
     * https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html#patterns
     */

    static Stream<String> getValidStringFormats() {
        return Stream.of("yyyy-MM-dd", "HH:mm:ss", "yyyyyyyy-MM-dd", "yyyyyyyyyyyyyyy-MM-dd");
    }

    static Stream<TemporalFormat> getValidTemporalFormats() {
        return Stream.of( //
            new TemporalFormat("yyyy-MM-dd", FormatTemporalType.DATE), //
            new TemporalFormat("HH:mm:ss", FormatTemporalType.TIME), //
            new TemporalFormat("yyyy-MM-dd HH:mm:ss", FormatTemporalType.DATE_TIME), //
            new TemporalFormat("yyyyyyyyyy-MM-dd'T'HH:mm:ss.SSSVV", FormatTemporalType.ZONED_DATE_TIME) //
        );
    }

    @ParameterizedTest
    @MethodSource("getValidStringFormats")
    void testValidateFormatValidString(final String format) {
        assertDoesNotThrow(() -> validateFormat(format));
    }

    @ParameterizedTest
    @MethodSource("getValidTemporalFormats")
    void testValidateFormatValidTemporals(final TemporalFormat temporalFormat) {
        assertDoesNotThrow(() -> validateFormat(temporalFormat));
    }

    static Stream<String> getInvalidStringFormats() {
        return Stream.of(
            "b", // reserved but undefined
            "yyyy-MM-dddd" // 'd' can be specified only up to 2 times
            );
    }

    static Stream<TemporalFormat> getInvalidTemporalFormats() {
        return Stream.of( //
            new TemporalFormat("HH:mm:ss", FormatTemporalType.DATE), //
            new TemporalFormat("yyyy-MM-dd", FormatTemporalType.TIME), //
            new TemporalFormat("yyyy-MM-dd'T'HH:mm:ss.SSSVV", FormatTemporalType.DATE_TIME), //
            new TemporalFormat("b", FormatTemporalType.ZONED_DATE_TIME) //
        );
    }

    @ParameterizedTest
    @MethodSource("getInvalidStringFormats")
    void testValidateFormatInvalidStrings(final String format) {
        assertThrows(InvalidSettingsException.class, () -> validateFormat(format));
    }

    @ParameterizedTest
    @MethodSource("getInvalidTemporalFormats")
    void testValidateFormatInvalidTemporals(final TemporalFormat temporalFormat) {
        assertThrows(InvalidSettingsException.class, () -> validateFormat(temporalFormat));
    }
}
