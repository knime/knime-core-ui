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
 *   Sep 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell.CoreCreateDataCellParameters.FromStringCellParameters;

/**
 * Tests for the {@link CreateDataCellExtensionsUtil} class.
 *
 * @author Paul Bärnreuther
 */
class CreateDataCellParametersTest {

    static Stream<Arguments> getCoreDataTypeTestData() {
        return Stream.of(Arguments.of(IntCellFactory.TYPE, IntCell.TYPE, new IntCell(0), new StringCell("0")),
            Arguments.of(DoubleCellFactory.TYPE, DoubleCell.TYPE, new DoubleCell(0.0), new StringCell("0.0")),
            Arguments.of(LongCellFactory.TYPE, LongCell.TYPE, new LongCell(0L), new StringCell("0")),
            Arguments.of(BooleanCellFactory.TYPE, BooleanCell.TYPE, BooleanCell.FALSE, new StringCell("false")),
            Arguments.of(StringCellFactory.TYPE, StringCell.TYPE, new StringCell(""), new StringCell("")));
    }

    @Test
    void testGetCreateDataCellParametersExtensionsContainsAllCoreTypes() {
        final var extensions = CreateDataCellExtensionsUtil.getCreateDataCellParametersExtensions();

        assertThat(extensions).as("Should contain core data types").containsKeys(IntCellFactory.TYPE,
            DoubleCellFactory.TYPE, LongCellFactory.TYPE, BooleanCellFactory.TYPE, StringCellFactory.TYPE);
    }

    @ParameterizedTest
    @MethodSource("getCoreDataTypeTestData")
    void testCoreDataTypeParameterClasses(final DataType dataType, final DataType expectedCellType,
        final DataCell expectedDefaultCell, final StringCell expectedStringConversion) throws Exception {

        final var extensions = CreateDataCellExtensionsUtil.getCreateDataCellParametersExtensions();
        final var parameterClass = extensions.get(dataType);

        assertThat(parameterClass).as("Parameter class should exist for " + dataType.toPrettyString()).isNotNull();

        // Instantiate the parameter class using default constructor
        final var parameters = instantiateParameters(parameterClass);
        assertThat(parameters).as("Should be able to instantiate " + parameterClass.getSimpleName()).isNotNull();

        // Test creating a data cell with the correct type
        final var createdCell = parameters.createDataCell(dataType, null);
        assertThat(createdCell).as("Should create a cell for matching type").isNotNull();

        assertThat(createdCell.getType()).as("Created cell should have correct type").isEqualTo(expectedCellType);

        // For default constructor instances, the cell should have default values
        assertThat(createdCell).as("Default constructor should create cell with default value")
            .isEqualTo(expectedDefaultCell);

        // Test cross-type conversion to StringCell
        final var stringCell = parameters.createDataCell(StringCellFactory.TYPE, null);
        assertThat(stringCell).as("Should be able to convert to StringCell").isNotNull();

        assertThat(stringCell).as("Cross-type conversion to StringCell should yield expected string representation")
            .isEqualTo(expectedStringConversion);

        // Test toString() method (required for cross-type conversion)
        assertThatCode(parameters::toString).as("toString() should not throw exception").doesNotThrowAnyException();

        assertThat(parameters.toString()).as("toString() should match expected string representation")
            .isEqualTo(expectedStringConversion.getStringValue());
    }

    private static CreateDataCellParameters
        instantiateParameters(final Class<? extends CreateDataCellParameters> parameterClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return parameterClass.getDeclaredConstructor().newInstance();
    }

    @Test
    void testGetFromStringCreateDataCellParametersClass() throws Exception {
        final var parameterClass = CreateDataCellExtensionsUtil.getFromStringCreateDataCellParametersClass();

        assertThat(parameterClass).as("Should return non-null class").isNotNull();

        // Instantiate the parameter class and test its behavior
        final var parameters = instantiateParameters(parameterClass);
        assertThat(parameters).as("Should be able to instantiate FromString parameter class").isNotNull();

        final var stringCell = parameters.createDataCell(StringCellFactory.TYPE, null);
        assertThat(stringCell).as("Should be able to create StringCell").isNotNull();
        assertThat(stringCell).as("Should create empty StringCell").isEqualTo(new StringCell(""));
    }

    @Test
    void testValidateSettings() throws Exception {
        final var parameterClass = CreateDataCellExtensionsUtil.getFromStringCreateDataCellParametersClass();
        final var parameters = (FromStringCellParameters)instantiateParameters(parameterClass);
        final var notANumber = "test";
        parameters.m_value = notANumber;
        assertThat(
            assertThrows(InvalidSettingsException.class, () -> parameters.validate(IntCellFactory.TYPE)).getMessage())
                .as("Should throw for non-FromString type").contains(notANumber);
    }

}