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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildUiSchema;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FolderSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.OverwriteDialogTitleInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.RichTextInputWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.TestButtonActionHandler.TestStates;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.dbtableselection.DBTableSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget.IntervalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DeclaringDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeStringFormatValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeTemporalFormatValidation;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.StringFilter;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.RichTextInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MinLengthValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * This tests the functionality of the {@link JsonFormsSchemaUtil} to set default options depending on types of fields
 * as well as options coming from widget annotations.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class UiSchemaOptionsTest {

    @Test
    void testDefaultFormats() {
        @SuppressWarnings("unused")
        @DBTableAdapterProvider(DummyDbAdapterWithoutCatalogues.class)
        class DefaultStylesSettings implements NodeParameters {
            @Widget(title = "", description = "")
            String m_string;

            @Widget(title = "", description = "")
            boolean m_boolean;

            enum MyEnum {
                    A, B, C
            }

            @Widget(title = "", description = "")
            MyEnum m_enum;

            @Widget(title = "", description = "")
            ColumnFilter m_columnFilter;

            @Widget(title = "", description = "")
            LocalDate m_localDate;

            @Widget(title = "", description = "")
            LocalTime m_localTime;

            @Widget(title = "", description = "")
            Credentials m_credentials;

            @Widget(title = "", description = "")
            LegacyCredentials m_legacyCredentials;

            @Widget(title = "", description = "")
            FileSelection m_fileSelection;

            @Widget(title = "", description = "")
            StringFilter m_nameFilter;

            @Widget(title = "", description = "")
            Interval m_interval;

            @Widget(title = "", description = "")
            DateInterval m_variableLengthInterval;

            @Widget(title = "", description = "")
            TimeInterval m_fixedLengthInterval;

            @Widget(title = "", description = "")
            ZonedDateTime m_zonedDateTime;

            @Widget(title = "", description = "")
            LocalDateTime m_localDateTime;

            @Widget(title = "", description = "")
            DataType m_dataType;

            @Widget(title = "", description = "")
            MultiFileSelection<MyVerySpecialTestFilterSettings> m_multiFileSelection =
                new MultiFileSelection<>(new MyVerySpecialTestFilterSettings());

            @Widget(title = "", description = "")
            DBTableSelection m_dbTableSelection;
        }

        var context = Mockito.mock(NodeParametersInput.class);
        var response = buildTestUiSchema(DefaultStylesSettings.class, context);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("options");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("boolean");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("checkbox");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("enum");
        assertThatJson(response).inPath("$.elements[2]").isObject().doesNotContainKey("options");
        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("columnFilter");
        assertThatJson(response).inPath("$.elements[3].options.format").isString().isEqualTo("typedStringFilter");
        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("localDate");
        assertThatJson(response).inPath("$.elements[4].options.format").isString().isEqualTo("localDate");
        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("localTime");
        assertThatJson(response).inPath("$.elements[5].options.format").isString().isEqualTo("localTime");
        assertThatJson(response).inPath("$.elements[6].scope").isString().contains("credentials");
        assertThatJson(response).inPath("$.elements[6].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[7].scope").isString().contains("legacyCredentials");
        assertThatJson(response).inPath("$.elements[7].options.format").isString().isEqualTo("legacyCredentials");
        assertThatJson(response).inPath("$.elements[8].scope").isString().contains("fileSelection");
        assertThatJson(response).inPath("$.elements[8].options.format").isString().isEqualTo("fileChooser");
        assertThatJson(response).inPath("$.elements[9].scope").isString().contains("nameFilter");
        assertThatJson(response).inPath("$.elements[9].options.format").isString().isEqualTo("nameFilter");
        assertThatJson(response).inPath("$.elements[10].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[10].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[10].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.DATE_OR_TIME.name());
        assertThatJson(response).inPath("$.elements[11].scope").isString().contains("variableLengthInterval");
        assertThatJson(response).inPath("$.elements[11].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[11].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.DATE.name());
        assertThatJson(response).inPath("$.elements[12].scope").isString().contains("fixedLengthInterval");
        assertThatJson(response).inPath("$.elements[12].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[12].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.TIME.name());
        assertThatJson(response).inPath("$.elements[13].scope").isString().contains("zonedDateTime");
        assertThatJson(response).inPath("$.elements[13].options.format").isString().isEqualTo("zonedDateTime");
        assertThatJson(response).inPath("$.elements[13].options.possibleValues").isArray();
        assertThatJson(response).inPath("$.elements[13].options.showMilliseconds").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[14].scope").isString().contains("localDateTime");
        assertThatJson(response).inPath("$.elements[14].options.format").isString().isEqualTo("dateTime");
        assertThatJson(response).inPath("$.elements[14].options.showMilliseconds").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[15].scope").isString().contains("dataType");
        assertThatJson(response).inPath("$.elements[15].options.format").isString().isEqualTo("dropDown");
        assertThatJson(response).inPath("$.elements[15].providedOptions").isArray().containsExactly("possibleValues");

        assertThatJson(response).inPath("$.elements[16].scope").isString().contains("multiFileSelection");
        assertThatJson(response).inPath("$.elements[16].options.format").isString().isEqualTo("multiFileChooser");
        assertThatJson(response).inPath("$.elements[16].options").isObject().doesNotContainKey("isWriter");
        assertThatJson(response).inPath("$.elements[16].options.filterSubUiSchema").isObject().containsKey("elements");
        // custom filter class has one field
        assertThatJson(response).inPath("$.elements[16].options.filterSubUiSchema.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[16].options.filterSubUiSchema.elements[0]") //
            .isObject() //
            .containsKey("scope");
        assertThatJson(response).inPath("$.elements[16].options.filterSubUiSchema.elements[0].scope") //
            .isString() //
            .contains("someVeryImportantField"); // name of the field inside the filter class we're using for this test

        assertThatJson(response).inPath("$.elements[17].scope").isString().contains("dbTableSelection");
        assertThatJson(response).inPath("$.elements[17].options.format").isString().isEqualTo("dbTableChooser");
    }

    /**
     * Used for testing the default settings for a field of type {@link MultiFileSelection}.
     */
    class MyVerySpecialTestFilterSettings implements FileChooserFilters {

        @Widget(title = "", description = "")
        String m_someVeryImportantField;

        @Override
        public boolean passesFilter(final Path root, final Path path) throws IllegalStateException {
            return false;
        }

        @Override
        public boolean followSymlinks() {
            return false;
        }
    }

    @Test
    void testComboBoxFormat() {
        class ComboBoxFormatSettings implements NodeParameters {

            @Widget(title = "", description = "")
            String[] m_comboBox;

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            String[] m_comboBoxWithChoices;

        }

        var response = buildTestUiSchema(ComboBoxFormatSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("comboBox");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo(Format.COMBO_BOX);

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("comboBoxWithChoices");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo(Format.COMBO_BOX);
    }

    @Test
    void testSortListFormat() {
        class ComboBoxFormatSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            @SortListWidget
            String[] m_sortList;

        }

        var response = buildTestUiSchema(ComboBoxFormatSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("sortList");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo(Format.SORT_LIST);

    }

    @Test
    void testAdvancedSettings() {
        class AdvancedSettings implements NodeParameters {

            @Widget(title = "", description = "", advanced = true)
            String m_foo;

            @Widget(title = "", description = "")
            String m_bar;

            @Widget(title = "", description = "")
            @Advanced
            String m_fieldAnnotation;

            @Advanced
            static final class AdvancedTestSettings implements Persistable, WidgetGroup {
                @Widget(title = "", description = "")
                String m_ats1;

                @Widget(title = "", description = "", advanced = false)
                String m_ats2;
            }

            @Widget(title = "", description = "")
            AdvancedTestSettings m_classAnnotation;

        }
        var response = buildTestUiSchema(AdvancedSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[0].options.isAdvanced").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("bar");
        assertThatJson(response).inPath("$.elements[1]").isObject().doesNotContainKey("isAdvanced");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("fieldAnnotation");
        assertThatJson(response).inPath("$.elements[2].options.isAdvanced").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[3].scope").isString().endsWith("classAnnotation/properties/ats1");
        assertThatJson(response).inPath("$.elements[3].options.isAdvanced").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[4].scope").isString().endsWith("classAnnotation/properties/ats2");
        assertThatJson(response).inPath("$.elements[4].options.isAdvanced").isBoolean().isTrue();
    }

    @Test
    void testRadioButtonWidget() {
        class RadioButtonsSettings implements NodeParameters {

            enum MyEnum {
                    A, //
                    @Label(value = "Option B", disabled = true)
                    B, //
                    @Label(value = "Option C")
                    C
            }

            @Widget(title = "", description = "")
            @RadioButtonsWidget()
            MyEnum m_foo;

            @Widget(title = "", description = "")
            @RadioButtonsWidget(horizontal = true)
            MyEnum m_bar;

            @Widget(title = "", description = "")
            @RadioButtonsWidget(horizontal = false)
            MyEnum m_baz;

        }
        var response = buildTestUiSchema(RadioButtonsSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("radio");
        assertThatJson(response).inPath("$.elements[0].options.radioLayout").isString().isEqualTo("vertical");
        assertThatJson(response).inPath("$.elements[0].options.disabledOptions").isArray().containsExactly("B");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("bar");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("radio");
        assertThatJson(response).inPath("$.elements[1].options.radioLayout").isString().isEqualTo("horizontal");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("baz");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("radio");
        assertThatJson(response).inPath("$.elements[2].options.radioLayout").isString().isEqualTo("vertical");
    }

    @Test
    void testIntervalWidget() {

        class SimpleDateStateProvider implements StateProvider<IntervalType> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public IntervalType computeState(final NodeParametersInput context) {
                return IntervalType.DATE;
            }
        }

        class SimpleTimeStateProvider implements StateProvider<IntervalType> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public IntervalType computeState(final NodeParametersInput context) {
                return IntervalType.TIME;
            }
        }

        class IntervalSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @IntervalWidget(typeProvider = SimpleDateStateProvider.class)
            Interval m_intervalDate;

            @Widget(title = "", description = "")
            @IntervalWidget(typeProvider = SimpleTimeStateProvider.class)
            Interval m_intervalTime;

        }

        var response = buildTestUiSchema(IntervalSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("intervalType");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[1].providedOptions").isArray().containsExactly("intervalType");

    }

    @Test
    void testValueSwitchWidget() {
        class ValueSwitchSettings implements NodeParameters {

            enum MyEnum {
                    A, //
                    @Label(value = "Option B", disabled = true)
                    B, //
                    @Label(value = "Option C")
                    C
            }

            @Widget(title = "", description = "")
            @ValueSwitchWidget
            MyEnum m_foo;
        }

        var response = buildTestUiSchema(ValueSwitchSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("valueSwitch");
        assertThatJson(response).inPath("$.elements[0].options.disabledOptions").isArray().containsExactly("B");
    }

    @Test
    void testThrowsIfIsNotApplicable() {
        class NonApplicableStyleSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @RadioButtonsWidget()
            String m_prop;
        }
        assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(NonApplicableStyleSettings.class));
    }

    @Test
    void testFolderSelectionWidgetSetsAppropriateSettings() {
        class SettingsWithFolderSelection implements NodeParameters {
            @Widget(title = "", description = "")
            @FolderSelectionWidget
            FileSelection m_myFolderSelection;
        }

        var fileSystemType = "myFileSystemType";
        var fileSystemSpecifier = "fileSystemSpecifier";
        var context = setupFileSystemMocks(fileSystemType, fileSystemSpecifier);

        var response = buildTestUiSchema(SettingsWithFolderSelection.class, context);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("myFolderSelection");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("fileChooser");
        assertThatJson(response).inPath("$.elements[0].options.selectionMode").isString().isEqualTo("FOLDER");
    }

    @Test
    void testShowSortButtonsTest() {
        @SuppressWarnings("unused")
        class ArrayElement implements WidgetGroup {
            String m_field1;

            int m_field2;
        }

        class ShowSortButtonsTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ArrayWidget
            ArrayElement[] m_arrayElementNoSortButtons;

            @Widget(title = "", description = "")
            @ArrayWidget(showSortButtons = true)
            ArrayElement[] m_arrayElementWithSortButtons;
        }

        var response = buildTestUiSchema(ShowSortButtonsTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("arrayElementNoSortButtons");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("showSortButtons");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("arrayElementWithSortButtons");
        assertThatJson(response).inPath("$.elements[1].options").isObject().containsKey("showSortButtons");
        assertThatJson(response).inPath("$.elements[1].options.showSortButtons").isBoolean().isTrue();
    }

    @Test
    void testHideControlHeader() {
        class HideTitleSettings implements NodeParameters {
            @Widget(title = "foo2", description = "")
            @WidgetInternal(hideControlHeader = true)
            String m_foo2;
        }

        var response = buildTestUiSchema(HideTitleSettings.class);
        assertThatJson(response).inPath("$.elements[0].options").isObject().containsKey("hideControlHeader");
    }

    @Test
    void overwriteTitle() {
        final String title = "Overwritten";

        class OverwriteTitleSettings implements NodeParameters {
            @Widget(title = "foo1", description = "")
            String m_foo1;

            @Widget(title = "foo2", description = "")
            @OverwriteDialogTitleInternal(title)
            String m_foo2;

            @Widget(title = "foo3", description = "")
            @OverwriteDialogTitleInternal("")
            String m_foo3;
        }

        var response = buildTestUiSchema(OverwriteTitleSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("label");

        assertThatJson(response).inPath("$.elements[1]").isObject().containsKey("label");
        assertThatJson(response).inPath("$.elements[1].label").isString().contains(title);

        assertThatJson(response).inPath("$.elements[2]").isObject().containsKey("label");
        assertThatJson(response).inPath("$.elements[2].label").isString().contains("");
    }

    @Test
    void testHasFixedSizeTest() {
        @SuppressWarnings("unused")
        class ArrayElement implements WidgetGroup {
            String m_field1;

            int m_field2;
        }

        class HasFixedSizeTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ArrayWidget
            ArrayElement[] m_arrayElementVariableSize;

            @Widget(title = "", description = "")
            @ArrayWidget(hasFixedSize = true)
            ArrayElement[] m_arrayElementFixedSize;
        }

        var response = buildTestUiSchema(HasFixedSizeTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("arrayElementVariableSize");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("hasFixedSize");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("arrayElementFixedSize");
        assertThatJson(response).inPath("$.elements[1].options").isObject().containsKey("hasFixedSize");
        assertThatJson(response).inPath("$.elements[1].options.hasFixedSize").isBoolean().isTrue();
    }

    @Test
    void testElementDefaultValueProvider() {
        @SuppressWarnings("unused")
        class ArrayElement implements WidgetGroup {
            String m_field1;

            int m_field2;
        }

        class ElementDefaultValueProvider implements StateProvider<ArrayElement> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                throw new IllegalStateException();

            }

            @Override
            public ArrayElement computeState(final NodeParametersInput context) {
                throw new IllegalStateException();
            }

        }

        class ElementDefaultValueProviderTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ArrayWidget
            ArrayElement[] m_arrayElementWithoutDefaultProvider;

            @Widget(title = "", description = "")
            @ArrayWidget(elementDefaultValueProvider = ElementDefaultValueProvider.class)
            ArrayElement[] m_arrayElementWithDefaultProvider;
        }

        var response = buildTestUiSchema(ElementDefaultValueProviderTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .contains("arrayElementWithoutDefaultProvider");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("hasFixedSize");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("arrayElementWithDefaultProvider");
        assertThatJson(response).inPath("$.elements[1].providedOptions").isArray()
            .containsExactly("elementDefaultValue");
    }

    @Nested
    class ButtonWidgetOptionsTest {

        static class EmptyButtonTestSettings {

        }

        static class ButtonActionHandlerWithoutDependencies extends TestButtonActionHandler<EmptyButtonTestSettings> {

        }

        @Test
        void testDefaultButtonWidgetOptions() {
            class ButtonWidgetDefaultTestSettings implements NodeParameters {

                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = ButtonActionHandlerWithoutDependencies.class)
                String m_foo;
            }

            var response = buildTestUiSchema(ButtonWidgetDefaultTestSettings.class);
            assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
            assertThatJson(response).inPath("$.elements[0].options.actionHandler").isString()
                .isEqualTo(ButtonActionHandlerWithoutDependencies.class.getName());
            assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("button");
            assertThatJson(response).inPath("$.elements[0].options.states").isArray().hasSize(3);
            assertThatJson(response).inPath("$.elements[0].options.displayErrorMessage").isBoolean().isTrue();
            assertThatJson(response).inPath("$.elements[0].options.showTitleAndDescription").isBoolean().isTrue();
            assertThatJson(response).inPath("$.elements[0].options.dependencies").isArray().hasSize(0);
        }

        @Test
        void testButtonWidgetOptions() {
            class ButtonWidgetOptionsTestSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = ButtonActionHandlerWithoutDependencies.class, displayErrorMessage = false,
                    showTitleAndDescription = false)
                String m_foo;
            }
            var response = buildTestUiSchema(ButtonWidgetOptionsTestSettings.class);
            assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
            assertThatJson(response).inPath("$.elements[0].options.actionHandler").isString()
                .isEqualTo(ButtonActionHandlerWithoutDependencies.class.getName());
            assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("button");
            assertThatJson(response).inPath("$.elements[0].options.states").isArray().hasSize(3);
            assertThatJson(response).inPath("$.elements[0].options.displayErrorMessage").isBoolean().isFalse();
            assertThatJson(response).inPath("$.elements[0].options.showTitleAndDescription").isBoolean().isFalse();
            assertThatJson(response).inPath("$.elements[0].options.dependencies").isArray().hasSize(0);
            assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("updateOptions");
        }

        @Test
        void testButtonStates() {
            class ButtonWidgetDefaultTestSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = ButtonActionHandlerWithoutDependencies.class)
                String m_foo;
            }

            var response = buildTestUiSchema(ButtonWidgetDefaultTestSettings.class);
            assertThatJson(response).inPath("$.elements[0].options.states").isArray().hasSize(3);

            assertThatJson(response).inPath("$.elements[0].options.states[0]").isObject().containsKey("id")
                .containsKey("disabled").containsKey("primary").containsKey("nextState").containsKey("text");
            assertThatJson(response).inPath("$.elements[0].options.states[0].id").isString()
                .isEqualTo(TestStates.READY.toString());
            assertThatJson(response).inPath("$.elements[0].options.states[0].disabled").isBoolean().isTrue();
            assertThatJson(response).inPath("$.elements[0].options.states[0].primary").isBoolean().isFalse();
            assertThatJson(response).inPath("$.elements[0].options.states[0].nextState").isString()
                .isEqualTo(TestStates.CANCEL.toString());
            // Test fallback to defaultText() in @ButtonState
            assertThatJson(response).inPath("$.elements[0].options.states[0].text").isString().isEqualTo("Ready");

            assertThatJson(response).inPath("$.elements[0].options.states[1]").isObject().containsKey("id")
                .containsKey("disabled").containsKey("primary").containsKey("text").doesNotContainKey("nextState");
            assertThatJson(response).inPath("$.elements[0].options.states[1].id").isString()
                .isEqualTo(TestStates.CANCEL.toString());
            assertThatJson(response).inPath("$.elements[0].options.states[1].disabled").isBoolean().isFalse();
            assertThatJson(response).inPath("$.elements[0].options.states[1].primary").isBoolean().isFalse();
            assertThatJson(response).inPath("$.elements[0].options.states[1].text").isString().isEqualTo("Cancel Text");

            assertThatJson(response).inPath("$.elements[0].options.states[2]").isObject().containsKey("id")
                .containsKey("disabled").containsKey("primary").containsKey("text").doesNotContainKey("nextState");
            assertThatJson(response).inPath("$.elements[0].options.states[2].id").isString()
                .isEqualTo(TestStates.DONE.toString());
            assertThatJson(response).inPath("$.elements[0].options.states[2].disabled").isBoolean().isTrue();
            assertThatJson(response).inPath("$.elements[0].options.states[2].primary").isBoolean().isTrue();
            assertThatJson(response).inPath("$.elements[0].options.states[2].text").isString().isEqualTo("Done Text");
        }

        static class GroupOfSettings implements WidgetGroup {
            @Widget(title = "", description = "")
            String m_sub1;

            @Widget(title = "", description = "")
            String m_sub2;
        }

        class ButtonWidgetWithDependenciesTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = ButtonActionHandlerWithDependencies.class,
                updateHandler = ButtonUpdateHandlerWithDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;

            @Widget(title = "", description = "")
            ColumnFilter m_otherSetting2;

            GroupOfSettings m_otherSetting3;

            @Widget(title = "", description = "")
            String m_otherSetting4;
        }

        static class OtherGroupOfSettings implements WidgetGroup {
            @Widget(title = "", description = "")
            String m_sub2;
        }

        static class OtherSettings {
            String m_foo;

            Boolean m_otherSetting1;

            ColumnFilter m_otherSetting2;

            OtherGroupOfSettings m_otherSetting3;

        }

        static class ButtonActionHandlerWithDependencies extends TestButtonActionHandler<OtherSettings> {

        }

        static class ButtonUpdateHandlerWithDependencies
            implements ButtonUpdateHandler<String, OtherSettings, TestButtonActionHandler.TestStates> {

            @Override
            public ButtonChange<String, TestStates> update(final OtherSettings settings,
                final NodeParametersInput context) throws WidgetHandlerException {
                return null;
            }

        }

        @Test
        void testButtonWidgetDependencies() {
            var response = buildTestUiSchema(ButtonWidgetWithDependenciesTestSettings.class);
            assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
            assertThatJson(response).inPath("$.elements[0].options.dependencies").isArray().hasSize(3);
            assertThatJson(response).inPath("$.elements[0].options.dependencies[0]").isString()
                .isEqualTo("#/properties/model/properties/otherSetting1");
            assertThatJson(response).inPath("$.elements[0].options.dependencies[1]").isString()
                .isEqualTo("#/properties/model/properties/otherSetting2");
            assertThatJson(response).inPath("$.elements[0].options.dependencies[2]").isString()
                .isEqualTo("#/properties/model/properties/otherSetting3/properties/sub2");
        }

        @Test
        void testButtonWidgetUpdateDependencies() {
            var response = buildTestUiSchema(ButtonWidgetWithDependenciesTestSettings.class);
            assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
            assertThatJson(response).inPath("$.elements[0].options.updateOptions.updateHandler").isString()
                .isEqualTo(ButtonUpdateHandlerWithDependencies.class.getName());
            assertThatJson(response).inPath("$.elements[0].options.updateOptions.dependencies").isArray().hasSize(3);

        }

        class ButtonWidgetWithMissingDependenciesTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = ButtonActionHandlerWithMissingDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithMissing {
            @Widget(title = "", description = "")
            Boolean m_otherSetting1;

            @Widget(title = "", description = "")
            ColumnFilter m_missingSetting;

        }

        static class ButtonActionHandlerWithMissingDependencies
            extends TestButtonActionHandler<OtherSettingsWithMissing> {

        }

        @Test
        void testThrowsForButtonWidgetWithMissingDependencies() {
            assertThrows(UiSchemaGenerationException.class,
                () -> buildTestUiSchema(ButtonWidgetWithMissingDependenciesTestSettings.class));
        }

        class ButtonWidgetWithAmbigousDependenciesTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = ButtonActionHandlerWithAmbiguousDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        class SecondSettings implements NodeParameters {

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithAmbigous implements NodeParameters {

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;

        }

        static class ButtonActionHandlerWithAmbiguousDependencies
            extends TestButtonActionHandler<OtherSettingsWithAmbigous> {

        }

        @Test
        void testThrowsForButtonWidgetWithAmbigousDependencies() {
            final Map<SettingsType, Class<? extends WidgetGroup>> settingsClasses = Map.of(SettingsType.MODEL,
                ButtonWidgetWithAmbigousDependenciesTestSettings.class, SettingsType.VIEW, SecondSettings.class);
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(settingsClasses));
        }

        class ButtonWidgetWithDisAmbigousDependenciesTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = TestButtonActionHandlerWithDisAmbiguousDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithSpecification implements NodeParameters {

            @Widget(title = "", description = "")
            @DeclaringDefaultNodeSettings(SecondSettings.class)
            Boolean m_otherSetting1;

        }

        static class TestButtonActionHandlerWithDisAmbiguousDependencies
            extends TestButtonActionHandler<OtherSettingsWithSpecification> {

        }

        @Test
        void testButtonWidgetWithAmbigousDependenciesUsingSpecifyingContainingClass() {
            final var settingsClasses = new LinkedHashMap<SettingsType, Class<? extends WidgetGroup>>();
            settingsClasses.put(SettingsType.MODEL, ButtonWidgetWithDisAmbigousDependenciesTestSettings.class);
            settingsClasses.put(SettingsType.VIEW, SecondSettings.class);
            var response = buildUiSchema(settingsClasses);
            assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
            assertThatJson(response).inPath("$.elements[0].options.dependencies").isArray().hasSize(1);
            assertThatJson(response).inPath("$.elements[0].options.dependencies[0]").isString()
                .isEqualTo("#/properties/view/properties/otherSetting1");
        }

        class ButtonWidgetWithWrongTypeDependenciesTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = TestButtonActionHandlerWithWrongType.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithWrongType implements NodeParameters {
            @Widget(title = "", description = "")
            String m_otherSetting1;
        }

        static class TestButtonActionHandlerWithWrongType extends TestButtonActionHandler<OtherSettingsWithWrongType> {

        }

        @Test
        void testThrowForButtonWidgetWithDependenciesWithConflictingTypes() {
            assertThrows(UiSchemaGenerationException.class,
                () -> buildTestUiSchema(ButtonWidgetWithWrongTypeDependenciesTestSettings.class));
        }
    }

    @Test
    void testSimpleButtonWidgetOptions() {
        class SimpleButtonWidgetTestSettings implements NodeParameters {

            class MyButtonTrigger implements ButtonReference {

            }

            @Widget(title = "", description = "")
            @SimpleButtonWidget(ref = MyButtonTrigger.class)
            Void m_button;

            @Widget(title = "", description = "")
            @SimpleButtonWidget(ref = MyButtonTrigger.class, icon = Icon.RELOAD)
            Void m_buttonWithIcon;

        }
        var response = buildTestUiSchema(SimpleButtonWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("simpleButton");
        assertThatJson(response).inPath("$.elements[0].options.triggerId").isString()
            .isEqualTo(SimpleButtonWidgetTestSettings.MyButtonTrigger.class.getName());
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("icon");
        assertThatJson(response).inPath("$.elements[1]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[1].options.icon").isString().isEqualTo("reload");

    }

    @Test
    void testTimeZoneWidgetDefaultOptions() {
        class TimeZoneDefaultTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            ZoneId m_zoneId;
        }

        var response = buildTestUiSchema(TimeZoneDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dropDown");
        assertThatJson(response).inPath("$.elements[0].options.possibleValues").isArray().isNotEmpty();
        var elementForUtcTimeZone = new StringChoice("UTC", "UTC");
        assertThatJson(response).inPath("$.elements[0].options.possibleValues").isArray()
            .contains(elementForUtcTimeZone);
    }

    static final class TimeZoneIdProvider implements StringChoicesProvider {
        @Override
        public List<String> choices(final NodeParametersInput context) {
            return List.of("UTC", "Europe/Berlin", "America/New_York");
        }
    }

    @Test
    void testTimeZoneWidgetCustomChoicesProviderOptions() {

        class TimeZoneDefaultTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ChoicesProvider(TimeZoneIdProvider.class)
            ZoneId m_zoneId;
        }

        var response = buildTestUiSchema(TimeZoneDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dropDown");
    }

    @Test
    void testRichTextInputWidget() {
        class RichTextInputWidgetSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @RichTextInputWidget
            String m_richTextContent;

            @Widget(title = "", description = "")
            @RichTextInputWidget
            @RichTextInputWidgetInternal(useFlowVarTemplates = true)
            String m_richTextContentWithTemplates;
        }

        var response = buildTestUiSchema(RichTextInputWidgetSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("richTextContent");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("richTextInput");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("useFlowVarTemplates");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("richTextContentWithTemplates");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("richTextInput");
        assertThatJson(response).inPath("$.elements[1].options.useFlowVarTemplates").isBoolean().isTrue();
    }

    static final class MyHasPasswordProvider implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
        }

        @Override
        public Boolean computeState(final NodeParametersInput context) {
            return true;
        }

    }

    static final class MyHasUsernameProvider implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
        }

        @Override
        public Boolean computeState(final NodeParametersInput context) {
            return true;
        }

    }

    @Test
    void testCredentials() {
        class CredentialsWidgetSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @CredentialsWidget(passwordLabel = "myPasswordLabel", usernameLabel = "myUsernameLabel")
            Credentials m_credentials;

            @Widget(title = "", description = "")
            @PasswordWidget(passwordLabel = "myPasswordLabel")
            Credentials m_password;

            @Widget(title = "", description = "")
            @UsernameWidget("myUsernameLabel")
            Credentials m_username;

            @Widget(title = "", description = "")
            @CredentialsWidget(hasSecondAuthenticationFactor = true, secondFactorLabel = "mySecondFactorLabel")
            Credentials m_credentialsWithSecondFactor;

            @Widget(title = "", description = "")
            @PasswordWidget(hasSecondAuthenticationFactor = true, secondFactorLabel = "mySecondFactorLabel")
            Credentials m_passwordWithSecondFactor;

            @Widget(title = "", description = "")
            @CredentialsWidget
            @CredentialsWidgetInternal(hasPasswordProvider = MyHasPasswordProvider.class,
                hasUsernameProvider = MyHasUsernameProvider.class)
            Credentials m_withStateProviders;
        }

        var response = buildTestUiSchema(CredentialsWidgetSettings.class);
        assertResponseCredentials(response);
        assertResponsePassword(response);
        assertResponseUsername(response);
        assertResponseCredentialsWithSecondFactor(response);
        assertResponsePasswordWithSecondFactor(response);
        assertResponseWithStateProviders(response);
    }

    private static void assertResponseCredentials(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("credentials");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[0].options.passwordLabel").isString().isEqualTo("myPasswordLabel");
        assertThatJson(response).inPath("$.elements[0].options.usernameLabel").isString().isEqualTo("myUsernameLabel");
        assertThatJson(response).inPath("$.elements[0].options.hasPassword").isAbsent();
        assertThatJson(response).inPath("$.elements[0].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[0].options.hasUsername").isAbsent();
    }

    private static void assertResponsePassword(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("password");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[1].options.passwordLabel").isString().isEqualTo("myPasswordLabel");
        assertThatJson(response).inPath("$.elements[1].options.hasPassword").isAbsent();
        assertThatJson(response).inPath("$.elements[1].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[1].options.hasUsername").isBoolean().isFalse();
    }

    private static void assertResponseUsername(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("username");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[2].options.usernameLabel").isString().isEqualTo("myUsernameLabel");
        assertThatJson(response).inPath("$.elements[2].options.hasPassword").isBoolean().isFalse();
        assertThatJson(response).inPath("$.elements[2].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[2].options.hasUsername").isAbsent();
    }

    private static void assertResponseCredentialsWithSecondFactor(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("credentialsWithSecondFactor");
        assertThatJson(response).inPath("$.elements[3].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[3].options.secondFactorLabel").isString()
            .isEqualTo("mySecondFactorLabel");
        assertThatJson(response).inPath("$.elements[3].options.hasPassword").isAbsent();
        assertThatJson(response).inPath("$.elements[3].options.showSecondFactor").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[3].options.hasUsername").isAbsent();
    }

    private static void assertResponsePasswordWithSecondFactor(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("passwordWithSecondFactor");
        assertThatJson(response).inPath("$.elements[4].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[4].options.secondFactorLabel").isString()
            .isEqualTo("mySecondFactorLabel");
        assertThatJson(response).inPath("$.elements[4].options.hasPassword").isAbsent();
        assertThatJson(response).inPath("$.elements[4].options.showSecondFactor").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[4].options.hasUsername").isBoolean().isFalse();
    }

    private static void assertResponseWithStateProviders(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("withStateProviders");
        assertThatJson(response).inPath("$.elements[5].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[5].options.secondFactorLabel").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.hideUsername").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[5].providedOptions").isArray().containsExactly("hasPassword",
            "hasUsername");

    }

    @Test
    void testThrowsIfUsernameWidget() {
        class CredentialsWidgetSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @PasswordWidget(passwordLabel = "myPasswordLabel")
            @UsernameWidget("myUsernameLabel")
            Credentials m_credentials;
        }

        assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(CredentialsWidgetSettings.class));

    }

    @Test
    void testLocalFileReaderWidget() {
        class LocalFileReaderWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @LocalFileReaderWidget
            String m_defaultOptions;

            @Widget(title = "", description = "")
            @LocalFileReaderWidget(placeholder = "myPlaceholder", fileExtensions = {"txt", "csv"})
            String m_specialOptions;

        }
        var response = buildTestUiSchema(LocalFileReaderWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("defaultOptions");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("localFileChooser");
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString().isEqualTo("");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("isWriter");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("specialOptions");
        assertThatJson(response).inPath("$.elements[1].options.placeholder").isString().isEqualTo("myPlaceholder");
        assertThatJson(response).inPath("$.elements[1].options.fileExtensions").isArray().containsExactly("txt", "csv");
    }

    static final class MyValueRef implements ParameterReference<String> {
    }

    static final class MyFileExtensionProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnValueChange(MyValueRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            throw new RuntimeException("Should not be called within this test");
        }

    }

    @Test
    void testLocalFileWriterWidget() {
        class LocalFileWriterWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @LocalFileWriterWidget
            String m_defaultOptions;

            @Widget(title = "", description = "")
            @LocalFileWriterWidget(placeholder = "myPlaceholder", fileExtension = "pdf")
            String m_specialOptions;

            @Widget(title = "", description = "")
            @LocalFileWriterWidget(fileExtensionProvider = MyFileExtensionProvider.class)
            String m_providedExtension;

            @Widget(title = "", description = "")
            @ValueReference(MyValueRef.class)
            String m_dependency;
        }
        var response = buildTestUiSchema(LocalFileWriterWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("defaultOptions");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("localFileChooser");
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString().isEqualTo("");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("extension");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("specialOptions");
        assertThatJson(response).inPath("$.elements[1].options.placeholder").isString().isEqualTo("myPlaceholder");
        assertThatJson(response).inPath("$.elements[1].options.fileExtension").isString().isEqualTo("pdf");
        assertThatJson(response).inPath("$.elements[1].options").isObject().doesNotContainKey("fileExtensionProvider");
        assertThatJson(response).inPath("$.elements[1].options.isWriter").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("providedExtension");
        assertThatJson(response).inPath("$.elements[2].options").isObject().doesNotContainKey("fileExtension");
        assertThatJson(response).inPath("$.elements[2].providedOptions").isArray().containsExactly("fileExtension");
        assertThatJson(response).inPath("$.elements[2].options.isWriter").isBoolean().isTrue();
    }

    @Test
    void testFileWriterWidget() {
        class FileWriterWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @FileWriterWidget
            FileSelection m_defaultOptions;

            @Widget(title = "", description = "")
            @FileWriterWidget(fileExtension = "pdf")
            FileSelection m_staticExtension;

            @Widget(title = "", description = "")
            @FileWriterWidget(fileExtensionProvider = MyFileExtensionProvider.class)
            FileSelection m_providedExtension;

            @Widget(title = "", description = "")
            @ValueReference(MyValueRef.class)
            String m_dependency;

        }
        var response = buildTestUiSchema(FileWriterWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("defaultOptions");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKeys("fileExtension",
            "fileExtensionProvider");
        assertThatJson(response).inPath("$.elements[0].options.isWriter").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("staticExtension");
        assertThatJson(response).inPath("$.elements[1].options.fileExtension").isString().isEqualTo("pdf");
        assertThatJson(response).inPath("$.elements[1].options").isObject().doesNotContainKey("fileExtensionProvider");
        assertThatJson(response).inPath("$.elements[1].options.isWriter").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("providedExtension");
        assertThatJson(response).inPath("$.elements[2].options").isObject().doesNotContainKey("fileExtension");
        assertThatJson(response).inPath("$.elements[2].providedOptions").isArray().containsExactly("fileExtension");
        assertThatJson(response).inPath("$.elements[2].options.isWriter").isBoolean().isTrue();
    }

    @SuppressWarnings("resource")
    NodeParametersInput setupFileSystemMocks(final String fileSystemType, final String fileSystemSpecifier) {
        final var context = Mockito.mock(NodeParametersInput.class);
        final var spec = Mockito.mock(FileSystemPortObjectSpec.class);
        final var location = Mockito.mock(FSLocation.class);
        Mockito.when(location.getFileSystemSpecifier()).thenReturn(Optional.of(fileSystemSpecifier));
        Mockito.when(spec.getFileSystemType()).thenReturn(fileSystemType);
        Mockito.when(spec.getFSLocationSpec()).thenReturn(location);
        Mockito.when(spec.getFileSystemConnection()).thenReturn(Optional.of(Mockito.mock(FSConnection.class)));
        Mockito.when(context.getInPortSpec(0)).thenReturn(Optional.of(spec));
        Mockito.when(context.getInPortTypes()).thenReturn(new PortType[]{FileSystemPortObject.TYPE});

        return context;
    }

    @Test
    void testFileReaderWidget() {
        class FileWriterWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @FileReaderWidget(fileExtensions = {"txt", "csv"})
            FileSelection m_fileReader;

        }

        var fileSystemType = "myFileSystemType";
        var fileSystemSpecifier = "fileSystemSpecifier";
        var context = setupFileSystemMocks(fileSystemType, fileSystemSpecifier);

        var response = buildTestUiSchema(FileWriterWidgetTestSettings.class, context);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("fileReader");
        assertThatJson(response).inPath("$.elements[0].options.fileExtensions").isArray().containsExactly("txt", "csv");
        assertThatJson(response).inPath("$.elements[0].options.portIndex").isNumber().isZero();
        assertThatJson(response).inPath("$.elements[0].options.fileSystemType").isString().isEqualTo(fileSystemType);
        assertThatJson(response).inPath("$.elements[0].options.fileSystemSpecifier").isString()
            .isEqualTo(fileSystemSpecifier);
        assertThatJson(response).inPath("$.elements[0].options").isObject()
            .doesNotContainKey("fileSystemConnectionMissing");
    }

    @Test
    void testDateTimeFormatPickerWidget() {

        class DateTimeFormatProvider extends ComprehensiveDateTimeFormatProvider {

            @SuppressWarnings("unused")
            DateTimeFormatProvider() {
                super(List.of("yyyy"));
            }

            @Override
            protected ZonedDateTime getTimeForExamples() {
                return ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("Africa/Cairo"));
            }

        }

        class DateTimeFormatPickerWidgetTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @DateTimeFormatPickerWidget(formatProvider = DateTimeFormatProvider.class)
            String m_formatPickerField;

            @Widget(title = "", description = "")
            @DateTimeFormatPickerWidget(formatProvider = DateTimeFormatProvider.class)
            TemporalFormat m_formatPickerFieldWithTemporalFormat;

            @Widget(title = "", description = "")
            @DateTimeFormatPickerWidget
            TemporalFormat m_formatPickerFieldWithTemporalFormat2;
        }

        var response = buildTestUiSchema(DateTimeFormatPickerWidgetTestSettings.class);

        // first test the one of type string
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("formatPickerField");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dateTimeFormat");
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("dateTimeFormats");
        assertThatJson(response).inPath("$.elements[0].options.externalValidationHandler").isString()
            .isEqualTo(DateTimeStringFormatValidation.class.getName());

        // then the one of type TemporalFormat
        assertThatJson(response).inPath("$.elements[1].scope").isString()
            .contains("formatPickerFieldWithTemporalFormat");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("dateTimeFormatWithType");
        assertThatJson(response).inPath("$.elements[1].providedOptions").isArray().containsExactly("dateTimeFormats");
        assertThatJson(response).inPath("$.elements[1].options.externalValidationHandler").isString()
            .isEqualTo(DateTimeTemporalFormatValidation.class.getName());

        // and the repeat with no format provider
        assertThatJson(response).inPath("$.elements[2].scope").isString()
            .contains("formatPickerFieldWithTemporalFormat2");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("dateTimeFormatWithType");
        assertThatJson(response).inPath("$.elements[2].providedOptions").isArray().containsExactly("dateTimeFormats");
    }

    @Test
    void testTextAreaWidget() {
        class TextAreaWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @TextAreaWidget
            String m_textAreaDefault;

            @Widget(title = "", description = "")
            @TextAreaWidget(rows = 10)
            String m_textAreaIncreasedRows;
        }

        var response = buildTestUiSchema(TextAreaWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("textAreaDefault");
        assertThatJson(response).inPath("$.elements[0].options.rows").isNumber().isEqualTo(BigDecimal.valueOf(4));
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo(Format.TEXT_AREA);

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("textAreaIncreasedRows");
        assertThatJson(response).inPath("$.elements[1].options.rows").isNumber().isEqualTo(BigDecimal.valueOf(10));
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo(Format.TEXT_AREA);
    }

    @Test
    void testMultiFileReader() {
        class MultiFileChooserTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @FileReaderWidget(fileExtensions = {"txt", "csv"})
            MultiFileSelection<MyVerySpecialTestFilterSettings> m_myReader;
        }

        var fileSystemType = "myFileSystemType";
        var fileSystemSpecifier = "fileSystemSpecifier";
        var context = setupFileSystemMocks(fileSystemType, fileSystemSpecifier);

        var response = buildTestUiSchema(MultiFileChooserTestSettings.class, context);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("myReader");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("multiFileChooser");
        assertThatJson(response).inPath("$.elements[0].options.fileSystemType").isString().isEqualTo(fileSystemType);
        assertThatJson(response).inPath("$.elements[0].options.fileExtensions").isArray().containsExactly("txt", "csv");
        assertThatJson(response).inPath("$.elements[0].options.fileSystemSpecifier").isString()
            .isEqualTo(fileSystemSpecifier);
        assertThatJson(response).inPath("$.elements[0].options.filterSubUiSchema").isObject().containsKey("elements");
        // custom filter class has one field
        assertThatJson(response).inPath("$.elements[0].options.filterSubUiSchema.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].options.filterSubUiSchema.elements[0]") //
            .isObject() //
            .containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].options.filterSubUiSchema.elements[0].scope") //
            .isString() //
            .contains("someVeryImportantField"); // name of the field inside the filter class we're using for this test

    }

    static final class TestStringProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new IllegalStateException("This method should never be called");
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            throw new IllegalStateException("This method should never be called");
        }

    }

    private static final class MinLenValidation extends MinLengthValidation {
        @Override
        public int getMinLength() {
            return -42;
        }
    }

    private static final class MaxLenValidation extends MaxLengthValidation {
        @Override
        public int getMaxLength() {
            return 42;
        }
    }

    private static final class CustomPatternValidation extends PatternValidation {
        @Override
        public String getPattern() {
            return "a.*";
        }
    }

    @Test
    void testTextInputWidget() {
        class TextInputWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @TextInputWidget(placeholder = "Bond")
            String m_textInputPlaceholder;

            @Widget(title = "", description = "")
            @TextInputWidget(placeholderProvider = TestStringProvider.class)
            String m_textInputPlaceholderProvider;

            @Widget(title = "", description = "")
            @TextInputWidget(minLengthValidation = MinLenValidation.class)
            String m_minLength;

            @Widget(title = "", description = "")
            @TextInputWidget(maxLengthValidation = MaxLenValidation.class)
            String m_maxLength;

            @Widget(title = "", description = "")
            @TextInputWidget(patternValidation = CustomPatternValidation.class)
            String m_pattern;

            @Widget(title = "", description = "")
            @TextInputWidget(minLengthValidation = MinLenValidation.class, maxLengthValidation = MaxLenValidation.class,
                patternValidation = CustomPatternValidation.class)
            String m_multipleValidations;
        }

        var response = buildTestUiSchema(TextInputWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("textInputPlaceholder");
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString().isEqualTo("Bond");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("textInputPlaceholderProvider");
        assertThatJson(response).inPath("$.elements[1].providedOptions").isArray().containsExactly("placeholder");

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("minLength");
        assertThatJson(response).inPath("$.elements[2].options.validation.minLength.parameters.minLength").isNumber()
            .isEqualTo(BigDecimal.valueOf(-42));
        assertThatJson(response).inPath("$.elements[2].options.validation.minLength.errorMessage").isString()
            .isEqualTo("The string must be at least -42 characters long.");

        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("maxLength");
        assertThatJson(response).inPath("$.elements[3].options.validation.maxLength.parameters.maxLength").isNumber()
            .isEqualTo(BigDecimal.valueOf(42));
        assertThatJson(response).inPath("$.elements[3].options.validation.maxLength.errorMessage").isString()
            .isEqualTo("The string must not exceed 42 characters.");

        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("pattern");
        assertThatJson(response).inPath("$.elements[4].options.validation.pattern.parameters.pattern").isString()
            .isEqualTo("a.*");
        assertThatJson(response).inPath("$.elements[4].options.validation.pattern.errorMessage").isString()
            .isEqualTo("The string must match the pattern: a.*");

        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("multipleValidations");
        assertThatJson(response).inPath("$.elements[5].options.validation").isObject().containsKeys("minLength",
            "maxLength", "pattern");
    }

    @Test
    void testCharacterTypeDefaultValidation() {
        class CharacterDefaultValidationTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            char m_primitiveChar;

            @Widget(title = "", description = "")
            Character m_characterWrapper;
        }

        var response = buildTestUiSchema(CharacterDefaultValidationTestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("primitiveChar");
        assertThatJson(response).inPath("$.elements[0].options.validation.pattern.parameters.pattern").isString()
            .isEqualTo("^.$");
        assertThatJson(response).inPath("$.elements[0].options.validation.pattern.errorMessage").isString()
            .isEqualTo("The string must be a single character.");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("characterWrapper");
        assertThatJson(response).inPath("$.elements[1].options.validation.pattern.parameters.pattern").isString()
            .isEqualTo(".{0,1}");
        assertThatJson(response).inPath("$.elements[1].options.validation.pattern.errorMessage").isString()
            .isEqualTo("Only one character is allowed.");
    }

    @Test
    void testCharacterTypeValidationConstraints() {
        class CharacterWithMinLengthValidationTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @TextInputWidget(minLengthValidation = MinLenValidation.class)
            char m_charWithMinLength;
        }

        assertThrows(UiSchemaGenerationException.class,
            () -> buildTestUiSchema(CharacterWithMinLengthValidationTestSettings.class));
    }

    @Test
    void testCharacterTypeMaxLengthValidationConstraints() {
        class CharacterWithMaxLengthValidationTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @TextInputWidget(maxLengthValidation = MaxLenValidation.class)
            Character m_charWithMaxLength;
        }

        assertThrows(UiSchemaGenerationException.class,
            () -> buildTestUiSchema(CharacterWithMaxLengthValidationTestSettings.class));
    }

    @Test
    void testCharacterTypeWithCustomPatternValidation() {
        class CharacterWithCustomPatternValidationTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @TextInputWidget(patternValidation = CustomPatternValidation.class)
            char m_charWithCustomPattern;
        }

        var response = buildTestUiSchema(CharacterWithCustomPatternValidationTestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("charWithCustomPattern");
        assertThatJson(response).inPath("$.elements[0].options.validation.pattern.parameters.pattern").isString()
            .isEqualTo("a.*");
        assertThatJson(response).inPath("$.elements[0].options.validation.pattern.errorMessage").isString()
            .isEqualTo("The string must match the pattern: a.*");
    }

    @Test
    void testInternalArrayWidget() {
        class InternalArrayWidgetTestSettings implements NodeParameters {

            static final class ElementSettings implements NodeParameters {

                @Widget(title = "Element value", description = "")
                @Effect(predicate = ArrayWidgetInternal.ElementIsEdited.class, type = EffectType.SHOW)
                String m_elementValue;
            }

            @ArrayWidgetInternal(withEditAndReset = true, titleProvider = TestStringProvider.class,
                subTitleProvider = TestStringProvider.class)
            @Widget(title = "title", description = "description")
            ElementSettings[] m_elementSettings;

        }

        var response = buildTestUiSchema(InternalArrayWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("elementSettings");
        assertThatJson(response).inPath("$.elements[0].options.withEditAndReset").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("arrayElementTitle",
            "elementSubTitle");
        assertThatJson(response).inPath("$.elements[0].options.withEditAndReset").isBoolean().isTrue();

        assertThatJson(response).inPath("$.elements[0].options.detail[0].scope").isString().contains("elementValue");
        assertThatJson(response).inPath("$.elements[0].options.detail[0].rule.condition.scope").isString()
            .isEqualTo("#/properties/_edit");

    }

    @Test
    void testTextMessage() {
        class TestSettings implements NodeParameters {

            static final class MyTextMessageProvider implements TextMessage.SimpleTextMessageProvider {

                @Override
                public boolean showMessage(final NodeParametersInput context) {
                    return true;
                }

                @Override
                public String title() {
                    return "My message";
                }

                @Override
                public String description() {
                    return "My description";
                }

                @Override
                public MessageType type() {
                    return MessageType.INFO;
                }

            }

            @TextMessage(MyTextMessageProvider.class)
            Void m_textMessage;
        }

        var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("scope");
        assertThatJson(response).inPath("$.elements[0].id").isString().contains("textMessage");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("textMessage");
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("message");

    }

    static final class CustomStaticMinValidation extends MinValidation {

        @Override
        public double getMin() {
            return -42.0;
        }
    }

    static final class CustomStaticMaxValidation extends MaxValidation {

        @Override
        public boolean isExclusive() {
            return true;
        }

        @Override
        public double getMax() {
            return 42.0;
        }
    }

    static final class CustomDynamicMinValidation implements StateProvider<MinValidation> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new IllegalStateException("This method should never be called");

        }

        @Override
        public MinValidation computeState(final NodeParametersInput context) {
            throw new IllegalStateException("This method should never be called");
        }

    }

    static final class CustomDynamicMaxValidation implements StateProvider<MaxValidation> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new IllegalStateException("This method should never be called");

        }

        @Override
        public MaxValidation computeState(final NodeParametersInput context) {
            throw new IllegalStateException("This method should never be called");
        }

    }

    @Test
    void testNumberInputWidget() {

        class NumberInputWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidation = CustomStaticMinValidation.class)
            double m_numberInputMin;

            @Widget(title = "", description = "")
            @NumberInputWidget(maxValidation = CustomStaticMaxValidation.class)
            int m_numberInputMax;

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidationProvider = CustomDynamicMinValidation.class)
            double m_numberInputMinProvider;

            @Widget(title = "", description = "")
            @NumberInputWidget(maxValidationProvider = CustomDynamicMaxValidation.class)
            int m_numberInputMaxProvider;

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
            int m_nonNegative;

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
            int m_positiveInteger;

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
            Duration m_positiveDuration;
        }

        var response = buildTestUiSchema(NumberInputWidgetTestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("numberInputMin");
        assertThatJson(response).inPath("$.elements[0].options.validation.min.parameters.min").isNumber()
            .isEqualTo(BigDecimal.valueOf(-42.0));
        assertThatJson(response).inPath("$.elements[0].options.validation.min.parameters.isExclusive").isBoolean()
            .isFalse();
        assertThatJson(response).inPath("$.elements[0].options.validation.min.errorMessage").isString()
            .isEqualTo("The value must be at least -42.");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("numberInputMax");
        assertThatJson(response).inPath("$.elements[1].options.validation.max.parameters.max").isNumber()
            .isEqualTo(BigDecimal.valueOf(42.0));
        assertThatJson(response).inPath("$.elements[1].options.validation.max.parameters.isExclusive").isBoolean()
            .isTrue();
        assertThatJson(response).inPath("$.elements[1].options.validation.max.errorMessage").isString()
            .isEqualTo("The value must be less than 42.");

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("numberInputMinProvider");
        assertThatJson(response).inPath("$.elements[2].providedOptions").isArray().containsExactly("validation.min");

        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("numberInputMaxProvider");
        assertThatJson(response).inPath("$.elements[3].providedOptions").isArray().containsExactly("validation.max");

        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("nonNegative");
        assertThatJson(response).inPath("$.elements[4].options.validation.min.parameters.min").isNumber()
            .isEqualTo(BigDecimal.valueOf(0.0));
        assertThatJson(response).inPath("$.elements[4].options.validation.min.parameters.isExclusive").isBoolean()
            .isFalse();
        assertThatJson(response).inPath("$.elements[4].options.validation.min.errorMessage").isString()
            .isEqualTo("The value must be at least 0.");

        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("positiveInteger");
        assertThatJson(response).inPath("$.elements[5].options.validation.min.parameters.min").isNumber()
            .isEqualTo(BigDecimal.valueOf(1.0));
        assertThatJson(response).inPath("$.elements[5].options.validation.min.parameters.isExclusive").isBoolean()
            .isFalse();
        assertThatJson(response).inPath("$.elements[5].options.validation.min.errorMessage").isString()
            .isEqualTo("The value must be at least 1.");

        assertThatJson(response).inPath("$.elements[6].scope").isString().contains("positiveDuration");
        assertThatJson(response).inPath("$.elements[6].options.validation.min.parameters.min").isNumber()
            .isEqualTo(BigDecimal.valueOf(1.0));
        assertThatJson(response).inPath("$.elements[6].options.validation.min.parameters.isExclusive").isBoolean()
            .isFalse();
        assertThatJson(response).inPath("$.elements[6].options.validation.min.errorMessage").isString()
            .isEqualTo("The value must be at least 1.");
    }

    private static void assertNumericValidation(final ObjectNode response, final int elementIndex, final String scope,
        final BigDecimal min, final BigDecimal max) {
        final var optionsPath = String.format("$.elements[%d].options", elementIndex);
        assertThatJson(response).inPath(String.format("$.elements[%d].scope", elementIndex)).isString().contains(scope);

        assertThatJson(response).inPath(String.format("%s.validation.min.parameters.min", optionsPath)).isNumber()
            .isEqualTo(min);
        assertThatJson(response).inPath(String.format("%s.validation.min.parameters.isExclusive", optionsPath))
            .isBoolean().isFalse();

        assertThatJson(response).inPath(String.format("%s.validation.max.parameters.max", optionsPath)).isNumber()
            .isEqualTo(max);
        assertThatJson(response).inPath(String.format("%s.validation.max.parameters.isExclusive", optionsPath))
            .isBoolean().isFalse();
    }

    @Test
    void testNumericDefaultValidations() {

        class NumberInputWidgetTestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            byte m_byte;

            @Widget(title = "", description = "")
            int m_int;

            @Widget(title = "", description = "")
            long m_long;

            @Widget(title = "", description = "")
            @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
            int m_overwrittenIntValidation;
        }

        var response = buildTestUiSchema(NumberInputWidgetTestSettings.class);

        assertNumericValidation(response, 0, "byte", BigDecimal.valueOf(-128.0), BigDecimal.valueOf(127.0));
        assertNumericValidation(response, 1, "int", BigDecimal.valueOf(-2147483648.0),
            BigDecimal.valueOf(2147483647.0));
        assertNumericValidation(response, 2, "long", BigDecimal.valueOf(-9007199254740991.0),
            BigDecimal.valueOf(9007199254740991.0));
        assertThatJson(response).inPath("$.elements[2].options.validation.min.errorMessage")
            .isEqualTo("Value too small to process without risking precision loss (< -9007199254740991).");
        assertThatJson(response).inPath("$.elements[2].options.validation.max.errorMessage")
            .isEqualTo("Value too large to process without risking precision loss (> 9007199254740991).");
        assertNumericValidation(response, 3, "overwrittenIntValidation", BigDecimal.valueOf(0.0),
            BigDecimal.valueOf(2147483647.0));
    }

    static class RegularChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return List.of("Regular 1", "Regular 2");
        }
    }

    @Test
    void testSingleSelection() {

        enum TestSpecialChoices {
                @Label("Special 1") //
                SPECIAL1, //
                @Label(value = "Special 2", description = "with description") //
                SPECIAL2;
        }

        class SingleSelectionSettings implements NodeParameters {

            @Widget(title = "", description = "")
            @ChoicesProvider(RegularChoicesProvider.class)
            StringOrEnum<TestSpecialChoices> m_singleSelection;
        }

        final var response = buildTestUiSchema(SingleSelectionSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("singleSelection");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("singleSelection");

        assertThatJson(response).inPath("$.elements[0].options.specialChoices").isArray()
            .containsExactly(new StringChoice("SPECIAL1", "Special 1"), new StringChoice("SPECIAL2", "Special 2"));

        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("possibleValues");
    }

    @Test
    void testDynamicSettingsWidget() {
        class TestSettings implements NodeParameters {

            static final class SomeClass implements DynamicSettingsWidget.ImperativeDialogProvider {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    throw new UnsupportedOperationException("This method should not be called in this test");
                }

                @Override
                public Pair<Map<String, Object>, DialogElementRendererSpec<?>> computeSettingsAndDialog(
                    final NodeParametersInput context) throws StateComputationFailureException {
                    throw new UnsupportedOperationException("This method should not be called in this test");
                }

            }

            @DynamicSettingsWidget(SomeClass.class)
            Map<String, Object> m_dynamicSettingsField;

        }

        final var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("dynamicSettingsField");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dynamicInput");
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().containsExactly("dynamicSettings");
    }

    @Test
    void testMapFieldThrowsIfWithoutDynamicSettingsWidget() {
        class TestSettings implements NodeParameters {

            @Widget(title = "", description = "")
            Map<String, String> m_mapField;
        }

        assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(TestSettings.class));
    }

    @Test
    void testThatDbTableAdapterSetsSupportsCatalogues() {

        @DBTableAdapterProvider(DummyDbAdapterWithoutCatalogues.class)
        class SettingsWithoutCatalogues implements NodeParameters {
            @Widget(title = "", description = "")
            DBTableSelection m_dbTableSelection;
        }

        var context = Mockito.mock(NodeParametersInput.class);
        var response = buildTestUiSchema(SettingsWithoutCatalogues.class, context);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("dbTableSelection");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dbTableChooser");
        assertThatJson(response).inPath("$.elements[0].options.dbConnected").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].options.catalogsSupported").isBoolean().isFalse();

        @DBTableAdapterProvider(DummyDbAdapterWithCatalogues.class)
        class SettingsWithCatalogues implements NodeParameters {
            @Widget(title = "", description = "")
            DBTableSelection m_dbTableSelection;
        }

        response = buildTestUiSchema(SettingsWithCatalogues.class, context);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("dbTableSelection");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dbTableChooser");
        assertThatJson(response).inPath("$.elements[0].options.dbConnected").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].options.catalogsSupported").isBoolean().isTrue();
    }

    static class DummyDbAdapterWithoutCatalogues extends DBTableAdapter {

        DummyDbAdapterWithoutCatalogues(final Supplier<PortObjectSpec[]> inputPortSpecSupplier) {
            super(inputPortSpecSupplier);
        }

        @Override
        public boolean isDbConnected() {
            return true;
        }

        @Override
        public Optional<List<String>> listCatalogs() throws SQLException {
            return Optional.empty();
        }

        @Override
        public List<String> listSchemas(final String catalogue) throws SQLException {
            return List.of();
        }

        @Override
        public List<String> listTables(final String catalogue, final String schema) throws SQLException {
            return List.of();
        }
    }

    static class DummyDbAdapterWithCatalogues extends DummyDbAdapterWithoutCatalogues {

        DummyDbAdapterWithCatalogues(final Supplier<PortObjectSpec[]> inputPortSpecSupplier) {
            super(inputPortSpecSupplier);
        }

        @Override
        public boolean isDbConnected() {
            return true;
        }

        @Override
        public Optional<List<String>> listCatalogs() throws SQLException {
            return Optional.of(List.of("catalog1", "catalog2"));
        }
    }
}
