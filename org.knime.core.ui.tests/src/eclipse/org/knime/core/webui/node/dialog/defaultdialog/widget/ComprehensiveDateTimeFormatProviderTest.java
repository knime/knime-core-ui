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
 *   Jan 28, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider.FormatWithoutExample;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatCategory;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatTemporalType;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class ComprehensiveDateTimeFormatProviderTest {

    @Test
    void checkThatAllFormatsAreValid() {
        var invalidFormatStrings = ComprehensiveDateTimeFormatProvider.ALL_FORMATS.stream() //
            .map(FormatWithoutExample::format) //
            .filter(ComprehensiveDateTimeFormatProviderTest::isFormatInvalid) //
            .toList();

        if (!invalidFormatStrings.isEmpty()) {
            fail("The following formats are invalid: "
                + invalidFormatStrings.stream().collect(Collectors.joining(", ")));
        }
    }

    @Test
    void checkThatThereAreNoDuplicateFormats() {
        // find duplicate formsts
        var duplicateFormats = ComprehensiveDateTimeFormatProvider.ALL_FORMATS.stream() //
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) //
            .entrySet().stream() //
            .filter(entry -> entry.getValue() > 1) //
            .toList();

        assertTrue(duplicateFormats.isEmpty(), "There are duplicate formats: " + duplicateFormats);
    }

    @Test
    void testAutoGuessFormat() {
        assertPresentAndEquals("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]",
            ComprehensiveDateTimeFormatProvider.bestFormatGuess(List.of( //
                "2025-01-28T12:00:00", //
                "2025-01-28T12:00:00.000", //
                "2025-02-01T12:00" //
            ), FormatTemporalType.DATE_TIME));

        assertTrue(
            ComprehensiveDateTimeFormatProvider
                .bestFormatGuess(List.of("blah", "hello", "world"), FormatTemporalType.TIME).isEmpty(),
            "Expected no format to be guessed because inputs are not date-times");

        assertTrue(
            ComprehensiveDateTimeFormatProvider
                .bestFormatGuess(List.of("2025-01-28", "2025-02-01"), FormatTemporalType.DATE_TIME).isEmpty(),
            "Expected no format to be guessed because query is for LocalDateTime but all inputs are LocalDate");

        assertTrue(
            ComprehensiveDateTimeFormatProvider.bestFormatGuess(List.of("2025-01-28 12:00:00"), FormatTemporalType.DATE)
                .isEmpty(),
            "Expected no format to be guessed because query is for LocalDate but all inputs are LocalDateTime");

        assertTrue(
            ComprehensiveDateTimeFormatProvider.bestFormatGuess(List.of("Q1/2024"), FormatTemporalType.DATE).isEmpty(),
            "Expected no format to be guessed because query is for LocalDate but all inputs less specific than that");

        // we had an issue where a string like '2025-01-28' will match a format like 'yyyy-MM-Q' even though
        // 28 is not a valid quarter. This should be fixed now when passing a FormatTemporalType since it will
        // use the attached query to determine the best format, so let's test that here
        var testFormat = new FormatWithoutExample("yyyy-Q", FormatTemporalType.DATE, FormatCategory.STANDARD);
        assertFalse(ComprehensiveDateTimeFormatProvider.matchesAllDateStrings(testFormat, List.of("2024-31"),
            FormatTemporalType.DATE), "Expected format to not match due to 31 being an invalid quarter");
    }

    private static <T> void assertPresentAndEquals(final T expected, final Optional<T> actual) {
        assertTrue(actual.isPresent(), "Expected value to be present");
        assertEquals(expected, actual.get(), "Expected value to be equal");
    }

    private static final boolean isFormatInvalid(final String fmt) {
        try {
            DateTimeFormatter.ofPattern(fmt);
            return false;
        } catch (IllegalArgumentException ex) { // NOSONAR
            return true;
        }
    }
}
