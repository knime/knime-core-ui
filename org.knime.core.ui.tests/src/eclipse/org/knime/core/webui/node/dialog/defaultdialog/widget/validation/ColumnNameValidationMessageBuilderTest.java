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
 *   9 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder.ColumnNameSettingContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.InvalidColumnNameState;

/**
 *
 * @author Robin Gerling
 */
public class ColumnNameValidationMessageBuilderTest {

    @Test
    void testMessageOutsideArrayLayout() {
        final var invalidStateToMessage = new ColumnNameValidationMessageBuilder("output column name").build();
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.EMPTY),
            "The output column name must not be empty.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.BLANK),
            "The output column name must not be blank.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.NOT_TRIMMED),
            "The output column name must start and end with a non-whitespace character.");
    }

    @Test
    void testMessageInsideCompactArrayLayout() {
        final var invalidStateToMessage = new ColumnNameValidationMessageBuilder("new column name")
            .withSpecificSettingContext(ColumnNameSettingContext.INSIDE_COMPACT_ARRAY_LAYOUT)
            .withArrayItemIdentifier("<my-old-column-name>").build();

        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.EMPTY),
            "The new column name for \"<my-old-column-name>\" must not be empty.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.BLANK),
            "The new column name for \"<my-old-column-name>\" must not be blank.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.NOT_TRIMMED),
            "The new column name for \"<my-old-column-name>\" must start and end with a non-whitespace character.");
    }

    @Test
    void testMessageInsideNonCompactArrayLayout() {
        final var invalidStateToMessage = new ColumnNameValidationMessageBuilder("output column name")
            .withSpecificSettingContext(ColumnNameSettingContext.INSIDE_NON_COMPACT_ARRAY_LAYOUT)
            .withArrayItemIdentifier("Item 1").build();

        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.EMPTY),
            "The output column name of \"Item 1\" must not be empty.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.BLANK),
            "The output column name of \"Item 1\" must not be blank.");
        assertEquals(invalidStateToMessage.apply(InvalidColumnNameState.NOT_TRIMMED),
            "The output column name of \"Item 1\" must start and end with a non-whitespace character.");
    }

    static Stream<Arguments> getInvalidSettingIdentifiers() {
        return Stream.of( //
            Arguments.of(null, "The settings identifier must not be null."), //
            Arguments.of("", "The settings identifier must not be blank."), //
            Arguments.of(" ", "The settings identifier must not be blank.") //
        );
    }

    @ParameterizedTest
    @MethodSource("getInvalidSettingIdentifiers")
    void testBuilderThrowsOnInvalidSettingIdentifier(final String settingsIdentifier, final String msg) {
        assertThrows(IllegalArgumentException.class, () -> new ColumnNameValidationMessageBuilder(settingsIdentifier),
            msg);
    }

    @Test
    void testBuilderThrowsOnInvalidSettingsContext() {
        assertThrows(IllegalArgumentException.class,
            () -> new ColumnNameValidationMessageBuilder("setting1").withSpecificSettingContext(null),
            "The settings context must not be null.");
    }

    static Stream<Arguments> getInvalidArrayIdentifiers() {
        return Stream.of( //
            Arguments.of(ColumnNameSettingContext.INSIDE_COMPACT_ARRAY_LAYOUT,
                "The array item identifier cannot be null when the setting is inside a compact array layout."), //
            Arguments.of(ColumnNameSettingContext.INSIDE_NON_COMPACT_ARRAY_LAYOUT,
                "The array item identifier cannot be null when the setting is inside a non-compact array layout."));
    }

    @ParameterizedTest
    @MethodSource("getInvalidArrayIdentifiers")
    void testBuilderThrowsOnInvalidArrayItemIdentifier(final ColumnNameSettingContext cnsc, final String msg) {
        assertThrows(IllegalArgumentException.class, () -> new ColumnNameValidationMessageBuilder("setting1")
            .withSpecificSettingContext(cnsc).withArrayItemIdentifier(null).build(), msg);
    }

}
