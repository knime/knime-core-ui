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
 *   May 5, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.AllFlowVariablesProvider;

/**
 *
 * This tests the functionality of the {@link JsonFormsSchemaUtil} to set default options depending on types of fields
 * as well as options coming from widget annotations.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class ChoicesWidgetUiSchemaOptionsTest {

    enum MyEnum {
            A, B, C
    }

    static final class TestEnumChoicesProvider implements EnumChoicesProvider<MyEnum> {

        @Override
        public List<MyEnum> choices(final NodeParametersInput context) {
            return List.of(MyEnum.A, MyEnum.B);
        }
    }

    @Test
    void testFormatForChoicesWidget() {

        @SuppressWarnings("unused")
        class SeveralChoicesSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ColumnFilterWidget(choicesProvider = TestColumnChoicesProvider.class)
            ColumnFilter m_columnFilter;

            @Widget(title = "", description = "")
            @FlowVariableFilterWidget(choicesProvider = AllFlowVariablesProvider.class)
            FlowVariableFilter m_flowVariableFilter;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            String[] m_stringArray;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            @TwinlistWidget
            String[] m_twinList;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            String m_string;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestEnumChoicesProvider.class)
            MyEnum m_foo;
        }

        var response = buildTestUiSchema(SeveralChoicesSettings.class, null);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("columnFilter");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("typedStringFilter");
        assertThatJson(response).inPath("$.elements[0].options.unknownValuesText").isString()
            .isEqualTo("Any unknown column");
        assertThatJson(response).inPath("$.elements[0].options.emptyStateLabel").isString()
            .isEqualTo("No columns in this list.");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("flowVariableFilter");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("typedStringFilter");
        assertThatJson(response).inPath("$.elements[1].options.unknownValuesText").isString()
            .isEqualTo("Any unknown variable");
        assertThatJson(response).inPath("$.elements[1].options.emptyStateLabel").isString()
            .isEqualTo("No variables in this list.");

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("stringArray");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("comboBox");
        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("twinList");
        assertThatJson(response).inPath("$.elements[3].options.format").isString().isEqualTo("twinList");
        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[4].options.format").isString().isEqualTo("dropDown");
        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[5].options.format").isString().isEqualTo("dropDown");
    }

    static class TestColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return ((DataTableSpec)context.getInPortSpec(0).get()).stream().toList();
        }
    }

    static class TestChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return List.of("column1", "column2");
        }
    }

    static class TestChoicesProviderWithIdAndText implements StringChoicesProvider {

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            return List.of(new StringChoice("id1", "text1"), new StringChoice("id2", "text2"));
        }
    }

    private static DataColumnSpec[] columnSpecs =
        new DataColumnSpec[]{new DataColumnSpecCreator("column1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("column2", DoubleCell.TYPE).createSpec()};

    private static NodeParametersInput createDefaultNodeSettingsContext() {
        NodeParametersInput defaultNodeSettingsContext =
            NodeParametersInputImpl.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
                new PortObjectSpec[]{new DataTableSpec(columnSpecs)}, null, null);
        return defaultNodeSettingsContext;
    }

    @Test
    void testChoicesProviders() {

        class ChoicesSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ChoicesProvider(TestColumnChoicesProvider.class)
            String m_foo;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            String m_bar;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestEnumChoicesProvider.class)
            MyEnum m_enum;

        }

        NodeParametersInput defaultNodeSettingsContext = createDefaultNodeSettingsContext();

        var response = buildTestUiSchema(ChoicesSettings.class, defaultNodeSettingsContext);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("possibleValues");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("bar");
        assertThatJson(response).inPath("$.elements[1].providedOptions").isArray().containsExactly("possibleValues");

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("enum");
        assertThatJson(response).inPath("$.elements[2].providedOptions").isArray().containsExactly("possibleValues");

    }

    @Test
    void testTwinlistWidgetIncludedLabel() {
        class ChoicesWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ChoicesProvider(TestColumnChoicesProvider.class)
            @TwinlistWidget(includedLabel = "Label for included columns.")
            ColumnFilter m_hasIncludedLabel;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestColumnChoicesProvider.class)
            @TwinlistWidget(excludedLabel = "Label for excluded columns.")
            ColumnFilter m_hasExcludedLabel;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestColumnChoicesProvider.class)
            ColumnFilter m_bar;

        }
        var response = buildTestUiSchema(ChoicesWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("hasIncludedLabel");
        assertThatJson(response).inPath("$.elements[0].options.includedLabel").isString()
            .isEqualTo("Label for included columns.");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("hasExcludedLabel");
        assertThatJson(response).inPath("$.elements[1].options.excludedLabel").isString()
            .isEqualTo("Label for excluded columns.");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("bar");
        assertThatJson(response).inPath("$.elements[2].options.includedLabel").isAbsent();
    }

    @Test
    void testColumnFilterWithWrongChoicesProviderThrows() {
        class TestSettings implements NodeParameters {
            @ChoicesProvider(TestChoicesProvider.class)
            @Widget(title = "", description = "")
            ColumnFilter m_columnFilter;
        }

        assertThat(
            assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("Error when generating the options of #/properties/model/properties/columnFilter.: "
                    + "The field is a ColumnFilter and the provided choicesProvider "
                    + "'TestChoicesProvider' is not a ColumnChoicesProvider. "
                    + "To prevent this from happening in a type-safe way, "
                    + "use the @ColumnFilterWidget annotation instead");
    }

    @Test
    void testFlowVariableFilterWithWrongChoicesProviderThrows() {
        class TestSettings implements NodeParameters {
            @ChoicesProvider(TestChoicesProvider.class)
            @Widget(title = "", description = "")
            FlowVariableFilter m_flowVariableFilter;
        }

        assertThat(
            assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("Error when generating the options of #/properties/model/properties/flowVariableFilter.: "
                    + "The field is a FlowVariableFilter and the provided choicesProvider "
                    + "'TestChoicesProvider' is not a FlowVariableChoicesProvider. "
                    + "To prevent this from happening in a type-safe way, "
                    + "use the @FlowVariableFilterWidget annotation instead");
    }

    @Test
    void testEnumWithWrongChoicesProviderThrows() {
        class TestSettings implements NodeParameters {
            @ChoicesProvider(TestChoicesProvider.class)
            @Widget(title = "", description = "")
            MyEnum m_flowVariableFilter;
        }

        assertThat(
            assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("Error when generating the options of #/properties/model/properties/flowVariableFilter.:"
                    + " The field is an enum and the provided choicesProvider is not an EnumChoicesProvider.");
    }

    @Test
    void testEnumChoicesProviderWithWrongEnumThrows() {
        class TestSettings implements NodeParameters {

            enum OtherEnum {
                    A, X, W
            }

            @ChoicesProvider(TestEnumChoicesProvider.class)
            @Widget(title = "", description = "")
            OtherEnum m_enum;

        }

        assertThat(
            assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("Error when generating the options of #/properties/model/properties/enum.:"
                    + " The field is an enum of type OtherEnum but the choicesProvider "
                    + "TestEnumChoicesProvider is for type MyEnum.");
    }

}
