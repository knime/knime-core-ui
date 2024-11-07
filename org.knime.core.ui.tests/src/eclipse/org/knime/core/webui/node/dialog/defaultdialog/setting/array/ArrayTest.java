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
 *   Nov 10, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.array;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.array.TestElement.TestEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.testing.util.TableTestUtil;

class ArrayTest {

    @Test
    void testGetValues() throws InvalidSettingsException {
        final var stringColumnName = "myStringColumn";
        final var stringColumn =
            new TableTestUtil.ObjectColumn(stringColumnName, StringCell.TYPE, new String[]{"FIRST", "SECOND", "THIRD"});
        final var booleanColumnName = "myBooleanColumn";
        final var booleanColumn =
            new TableTestUtil.ObjectColumn(booleanColumnName, BooleanCell.TYPE, new Boolean[]{true, false, true});
        final var intColumnName = "myIntColumn";
        final var intColumn = new TableTestUtil.ObjectColumn(intColumnName, IntCell.TYPE, new Integer[]{1, 2, 3});

        Array<TestElement> array = new Array<>(List.of(), Map.of(//
            "string", stringColumnName, //
            "int", intColumnName, //
            "double", intColumnName, //
            "bool", booleanColumnName, //
            "long", intColumnName, //
            "enum", stringColumnName, //
            "columnSelection", stringColumnName, //
            "nestedSettings#innerSetting", stringColumnName//
        ));
        final var testTable = TableTestUtil.createTableFromColumns(stringColumn, booleanColumn, intColumn);

        final var elements = array.getValues(TestElement.class, testTable);

        assertThat(elements).hasSize(3);
        assertThat(elements.get(1).m_string).isEqualTo("SECOND");
        assertThat(elements.get(1).m_int).isEqualTo(2);
        assertThat(elements.get(1).m_double).isEqualTo(2.0);
        assertThat(elements.get(1).m_bool).isFalse();
        assertThat(elements.get(1).m_long).isEqualTo(2L);
        assertThat(elements.get(1).m_enum).isEqualTo(TestEnum.SECOND);
        assertThat(elements.get(1).m_columnSelection.getSelected()).isEqualTo("SECOND");
        assertThat(elements.get(1).m_nestedSettings.m_innerSetting).isEqualTo("SECOND");

    }

    static final class SimpleElement implements DefaultNodeSettings {

        @Widget(description = "", title = "")
        String m_string;

    }

    @Test
    void testGetValuesThrowsOnMissingValues() {

        final var columnName = "myStringColumn";
        final var stringColumn =
            new TableTestUtil.ObjectColumn(columnName, StringCell.TYPE, new String[]{"FIRST", null, "THIRD"});
        final var testTable = TableTestUtil.createTableFromColumns(stringColumn);
        Array<SimpleElement> array = new Array<>(List.of(), Map.of("string", columnName));

        assertThat(assertThrows(IllegalArgumentException.class, () -> array.getValues(SimpleElement.class, testTable)))
            .hasMessage("There exists an invalid cell in column \"myStringColumn\" of the dynamic array table.");
    }

    @Test
    void testValidateThrowsOnMissingKey() {
        final var columnName = "myStringColumn";
        final var stringColumn = new TableTestUtil.ObjectColumn(columnName, StringCell.TYPE, new String[]{});
        final var testTableSpec = TableTestUtil.createTableFromColumns(stringColumn).getSpec();
        final Array<SimpleElement> array = new Array<>();

        assertThat(
            assertThrows(InvalidSettingsException.class, () -> array.validate(SimpleElement.class, testTableSpec)))
                .hasMessage("There is no dynamic array column selected for key \"string\".");
    }

    @Test
    void testValidateThrowsOnMissingColumn() {
        final var columnName = "myStringColumn";
        final var stringColumn = new TableTestUtil.ObjectColumn(columnName, StringCell.TYPE, new String[]{});
        final var testTableSpec = TableTestUtil.createTableFromColumns(stringColumn).getSpec();
        final Array<SimpleElement> array = new Array<>(List.of(), Map.of("string", "missingColumn"));

        assertThat(
            assertThrows(InvalidSettingsException.class, () -> array.validate(SimpleElement.class, testTableSpec)))
                .hasMessage(
                    "The dynamic array column \"missingColumn\" is missing from the connected input table spec.");
    }

    @Test
    void testValidateThrowsOnInvalidColumn() {
        final var columnName = "myDoubleColumn";
        final var doubleColumn = new TableTestUtil.ObjectColumn(columnName, DoubleCell.TYPE, new String[]{});
        final var testTableSpec = TableTestUtil.createTableFromColumns(doubleColumn).getSpec();
        final Array<SimpleElement> array = new Array<>(List.of(), Map.of("string", columnName));

        assertThat(assertThrows(InvalidSettingsException.class,
            () -> array.validate(SimpleElement.class, testTableSpec))).hasMessage(
                "The selected dynamic array column \"myDoubleColumn\" is of type \"Number (double)\" but the setting requires a StringValue.");
    }

}
