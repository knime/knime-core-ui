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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.TestButtonActionHandler.TestStates;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.NameFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComboBoxWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget.IntervalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget.DoubleProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RichTextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage.MessageType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.PasswordWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.UsernameWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DeclaringDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.InternalArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.OverwriteDialogTitle;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
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
        class DefaultStylesSettings implements DefaultNodeSettings {
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
            ColumnSelection m_columnSelection;

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
            NameFilter m_nameFilter;

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
        }

        var response = buildTestUiSchema(DefaultStylesSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("options");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("boolean");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("checkbox");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("enum");
        assertThatJson(response).inPath("$.elements[2]").isObject().doesNotContainKey("options");
        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("columnFilter");
        assertThatJson(response).inPath("$.elements[3].options.format").isString().isEqualTo("columnFilter");
        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("columnSelection");
        assertThatJson(response).inPath("$.elements[4].options.format").isString().isEqualTo("columnSelection");
        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("localDate");
        assertThatJson(response).inPath("$.elements[5].options.format").isString().isEqualTo("localDate");
        assertThatJson(response).inPath("$.elements[6].scope").isString().contains("localTime");
        assertThatJson(response).inPath("$.elements[6].options.format").isString().isEqualTo("localTime");
        assertThatJson(response).inPath("$.elements[7].scope").isString().contains("credentials");
        assertThatJson(response).inPath("$.elements[7].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[8].scope").isString().contains("legacyCredentials");
        assertThatJson(response).inPath("$.elements[8].options.format").isString().isEqualTo("legacyCredentials");
        assertThatJson(response).inPath("$.elements[9].scope").isString().contains("fileSelection");
        assertThatJson(response).inPath("$.elements[9].options.format").isString().isEqualTo("fileChooser");
        assertThatJson(response).inPath("$.elements[10].scope").isString().contains("nameFilter");
        assertThatJson(response).inPath("$.elements[10].options.format").isString().isEqualTo("nameFilter");

        // tests for interval default formats
        assertThatJson(response).inPath("$.elements[11].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[11].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[11].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.DATE_OR_TIME.name());

        assertThatJson(response).inPath("$.elements[12].scope").isString().contains("variableLengthInterval");
        assertThatJson(response).inPath("$.elements[12].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[12].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.DATE.name());

        assertThatJson(response).inPath("$.elements[13].scope").isString().contains("fixedLengthInterval");
        assertThatJson(response).inPath("$.elements[13].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[13].options.intervalType").isString()
            .isEqualTo(IntervalWidget.IntervalType.TIME.name());

        // tests for zoned date time
        assertThatJson(response).inPath("$.elements[14].scope").isString().contains("zonedDateTime");
        assertThatJson(response).inPath("$.elements[14].options.format").isString().isEqualTo("zonedDateTime");
        assertThatJson(response).inPath("$.elements[14].options.possibleValues").isArray();
        assertThatJson(response).inPath("$.elements[15].options.showMilliseconds").isBoolean().isTrue();

        // tests for local date time
        assertThatJson(response).inPath("$.elements[15].scope").isString().contains("localDateTime");
        assertThatJson(response).inPath("$.elements[15].options.format").isString().isEqualTo("dateTime");
        assertThatJson(response).inPath("$.elements[15].options.showMilliseconds").isBoolean().isTrue();
    }

    @Test
    void testHidableStringSetting() {
        class HidableStringSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @TextInputWidget(optional = true)
            String m_string;

        }

        var response = buildTestUiSchema(HidableStringSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0].options.hideOnNull").isBoolean().isTrue();

    }

    @Test
    void testHidableChoicesWidgetSetting() {
        class HidableStringSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ChoicesWidget(choices = TestChoicesProvider.class, optional = true)
            String m_string = "TestString";
        }

        var response = buildTestUiSchema(HidableStringSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0].options.hideOnNull").isBoolean().isTrue();

    }

    @Test
    void testComboBoxFormat() {
        class ComboBoxFormatSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            String[] m_comboBox;

            @Widget(title = "", description = "")
            @ChoicesWidget(choices = TestChoicesProvider.class)
            @ComboBoxWidget
            String[] m_comboBoxWithChoices;

        }

        var response = buildTestUiSchema(ComboBoxFormatSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("comboBox");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo(Format.COMBO_BOX);

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("comboBoxWithChoices");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo(Format.COMBO_BOX);
        assertThatJson(response).inPath("$.elements[1].options.possibleValues").isArray().hasSize(0);
    }

    @Test
    void testSortListFormat() {
        class ComboBoxFormatSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ChoicesWidget(choices = TestChoicesProvider.class)
            @SortListWidget
            String[] m_sortList;

        }

        var response = buildTestUiSchema(ComboBoxFormatSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("sortList");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo(Format.SORT_LIST);

    }

    @Test
    void testAdvancedSettings() {
        class AdvancedSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "", advanced = true)
            ColumnSelection m_foo;

            @Widget(title = "", description = "")
            ColumnSelection m_bar;

        }
        var response = buildTestUiSchema(AdvancedSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("foo");
        assertThatJson(response).inPath("$.elements[0].options.isAdvanced").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("bar");
        assertThatJson(response).inPath("$.elements[1]").isObject().doesNotContainKey("isAdvanced");
    }

    @Test
    void testRadioButtonWidget() {
        class RadioButtonsSettings implements DefaultNodeSettings {

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
            public IntervalType computeState(final DefaultNodeSettingsContext context) {
                return IntervalType.DATE;
            }
        }

        class SimpleTimeStateProvider implements StateProvider<IntervalType> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public IntervalType computeState(final DefaultNodeSettingsContext context) {
                return IntervalType.TIME;
            }
        }

        class IntervalSettings implements DefaultNodeSettings {

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
        assertThatJson(response).inPath("$.elements[0].options.intervalTypeProvider").isString()
            .isEqualTo(SimpleDateStateProvider.class.getName());
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("interval");
        assertThatJson(response).inPath("$.elements[1].options.intervalTypeProvider").isString()
            .isEqualTo(SimpleTimeStateProvider.class.getName());
    }

    @Test
    void testValueSwitchWidget() {
        class ValueSwitchSettings implements DefaultNodeSettings {

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
        class NonApplicableStyleSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @RadioButtonsWidget()
            String m_prop;
        }
        assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(NonApplicableStyleSettings.class));
    }

    @Test
    void testShowSortButtonsTest() {
        @SuppressWarnings("unused")
        class ArrayElement implements WidgetGroup {
            String m_field1;

            int m_field2;
        }

        class ShowSortButtonsTestSettings implements DefaultNodeSettings {
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
        class HideTitleSettings implements DefaultNodeSettings {
            @Widget(title = "foo2", description = "", hideControlHeader = true)
            String m_foo2;
        }

        var response = buildTestUiSchema(HideTitleSettings.class);
        assertThatJson(response).inPath("$.elements[0].options").isObject().containsKey("hideControlHeader");
    }

    @Test
    void overwriteTitle() {
        final String title = "Overwritten";

        class OverwriteTitleSettings implements DefaultNodeSettings {
            @Widget(title = "foo1", description = "")
            String m_foo1;

            @Widget(title = "foo2", description = "")
            @OverwriteDialogTitle(title)
            String m_foo2;

            @Widget(title = "foo3", description = "")
            @OverwriteDialogTitle("")
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

        class HasFixedSizeTestSettings implements DefaultNodeSettings {
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
            public ArrayElement computeState(final DefaultNodeSettingsContext context) {
                throw new IllegalStateException();
            }

        }

        class ElementDefaultValueProviderTestSettings implements DefaultNodeSettings {
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
        assertThatJson(response).inPath("$.elements[1].options.elementDefaultValueProvider").isString()
            .isEqualTo(ElementDefaultValueProvider.class.getName());
    }

    @Nested
    class ButtonWidgetOptionsTest {

        static class EmptyButtonTestSettings {

        }

        static class ButtonActionHandlerWithoutDependencies extends TestButtonActionHandler<EmptyButtonTestSettings> {

        }

        @Test
        void testDefaultButtonWidgetOptions() {
            class ButtonWidgetDefaultTestSettings implements DefaultNodeSettings {

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
            class ButtonWidgetOptionsTestSettings implements DefaultNodeSettings {
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
            class ButtonWidgetDefaultTestSettings implements DefaultNodeSettings {
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

        class ButtonWidgetWithDependenciesTestSettings implements DefaultNodeSettings {
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
                final DefaultNodeSettingsContext context) throws WidgetHandlerException {
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

        class ButtonWidgetWithMissingDependenciesTestSettings implements DefaultNodeSettings {
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

        class ButtonWidgetWithAmbigousDependenciesTestSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = ButtonActionHandlerWithAmbiguousDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        class SecondSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithAmbigous implements DefaultNodeSettings {

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

        class ButtonWidgetWithDisAmbigousDependenciesTestSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = TestButtonActionHandlerWithDisAmbiguousDependencies.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithSpecification implements DefaultNodeSettings {

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

        class ButtonWidgetWithWrongTypeDependenciesTestSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = TestButtonActionHandlerWithWrongType.class)
            String m_foo;

            @Widget(title = "", description = "")
            Boolean m_otherSetting1;
        }

        static class OtherSettingsWithWrongType implements DefaultNodeSettings {
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
        class SimpleButtonWidgetTestSettings implements DefaultNodeSettings {

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
        class TimeZoneDefaultTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            ZoneId m_zoneId;
        }

        var response = buildTestUiSchema(TimeZoneDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dropDown");
        assertThatJson(response).inPath("$.elements[0].options.possibleValues").isArray().isNotEmpty();
        var elementForUtcTimeZone = new IdAndText("UTC", "UTC");
        assertThatJson(response).inPath("$.elements[0].options.possibleValues").isArray()
            .contains(elementForUtcTimeZone);
    }

    @Test
    void testTimeZoneWidgetOptionalChoicesOptions() {
        class TimeZoneDefaultTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ChoicesWidget(optional = true)
            ZoneId m_zoneId;
        }

        var response = buildTestUiSchema(TimeZoneDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.hideOnNull").isBoolean().isTrue();
    }

    static final class TimeZoneIdProvider implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return new String[]{"UTC", "Europe/Berlin", "America/New_York"};
        }
    }

    @Test
    void testTimeZoneWidgetCustomChoicesProviderOptions() {

        class TimeZoneDefaultTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ChoicesWidget(choices = TimeZoneIdProvider.class)
            ZoneId m_zoneId;
        }

        var response = buildTestUiSchema(TimeZoneDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("scope");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("dropDown");
    }

    @Test
    void testDateWidgetOptions() {
        class DateTimeDefaultTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @DateWidget(minDate = "2023-06-12", maxDate = "2023-06-14")
            LocalDate m_date;
        }

        var response = buildTestUiSchema(DateTimeDefaultTestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().containsKey("options");
        assertThatJson(response).inPath("$.elements[0].options.minimum").isString().isEqualTo("2023-06-12");
        assertThatJson(response).inPath("$.elements[0].options.maximum").isString().isEqualTo("2023-06-14");
    }

    @Test
    void testRichTextInputWidget() {
        class RichTextInputWidgetSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @RichTextInputWidget
            String m_richTextContent;

            @Widget(title = "", description = "")
            @RichTextInputWidget(useFlowVarTemplates = true)
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
        public Boolean computeState(final DefaultNodeSettingsContext context) {
            return true;
        }

    }

    static final class MyHasUsernameProvider implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
        }

        @Override
        public Boolean computeState(final DefaultNodeSettingsContext context) {
            return true;
        }

    }

    @Test
    void testCredentials() {
        class CredentialsWidgetSettings implements DefaultNodeSettings {
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
            @CredentialsWidget(hasPasswordProvider = MyHasPasswordProvider.class,
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
        assertThatJson(response).inPath("$.elements[0].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[0].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[0].options.hideUsername").isAbsent();
    }

    private static void assertResponsePassword(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("password");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[1].options.passwordLabel").isString().isEqualTo("myPasswordLabel");
        assertThatJson(response).inPath("$.elements[1].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[1].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[1].options.hideUsername").isBoolean().isTrue();
    }

    private static void assertResponseUsername(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("username");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[2].options.usernameLabel").isString().isEqualTo("myUsernameLabel");
        assertThatJson(response).inPath("$.elements[2].options.hidePassword").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[2].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[2].options.hideUsername").isAbsent();
    }

    private static void assertResponseCredentialsWithSecondFactor(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("credentialsWithSecondFactor");
        assertThatJson(response).inPath("$.elements[3].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[3].options.secondFactorLabel").isString()
            .isEqualTo("mySecondFactorLabel");
        assertThatJson(response).inPath("$.elements[3].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[3].options.showSecondFactor").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[3].options.hideUsername").isAbsent();
    }

    private static void assertResponsePasswordWithSecondFactor(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("passwordWithSecondFactor");
        assertThatJson(response).inPath("$.elements[4].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[4].options.secondFactorLabel").isString()
            .isEqualTo("mySecondFactorLabel");
        assertThatJson(response).inPath("$.elements[4].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[4].options.showSecondFactor").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[4].options.hideUsername").isBoolean().isTrue();
    }

    private static void assertResponseWithStateProviders(final ObjectNode response) {
        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("withStateProviders");
        assertThatJson(response).inPath("$.elements[5].options.format").isString().isEqualTo("credentials");
        assertThatJson(response).inPath("$.elements[5].options.secondFactorLabel").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.hidePassword").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.hideUsername").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.showSecondFactor").isAbsent();
        assertThatJson(response).inPath("$.elements[5].options.hasUsernameProvider").isString()
            .isEqualTo(MyHasUsernameProvider.class.getName());
        assertThatJson(response).inPath("$.elements[5].options.hasPasswordProvider").isString()
            .isEqualTo(MyHasPasswordProvider.class.getName());

    }

    @Test
    void testThrowsIfUsernameWidget() {
        class CredentialsWidgetSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @PasswordWidget(passwordLabel = "myPasswordLabel")
            @UsernameWidget("myUsernameLabel")
            Credentials m_credentials;
        }

        assertThrows(UiSchemaGenerationException.class, () -> buildTestUiSchema(CredentialsWidgetSettings.class));

    }

    @Test
    void testLocalFileReaderWidget() {
        class LocalFileReaderWidgetTestSettings implements DefaultNodeSettings {

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

    static final class MyValueRef implements Reference<String> {
    }

    static final class MyFileExtensionProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnValueChange(MyValueRef.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            throw new RuntimeException("Should not be called within this test");
        }

    }

    @Test
    void testLocalFileWriterWidget() {
        class LocalFileWriterWidgetTestSettings implements DefaultNodeSettings {

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
        assertThatJson(response).inPath("$.elements[2].options.fileExtensionProvider").isString()
            .isEqualTo(MyFileExtensionProvider.class.getName());
        assertThatJson(response).inPath("$.elements[2].options.isWriter").isBoolean().isTrue();
    }

    @Test
    void testFileWriterWidget() {
        class FileWriterWidgetTestSettings implements DefaultNodeSettings {

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
        assertThatJson(response).inPath("$.elements[2].options.fileExtensionProvider").isString()
            .isEqualTo(MyFileExtensionProvider.class.getName());
        assertThatJson(response).inPath("$.elements[2].options.isWriter").isBoolean().isTrue();
    }

    @SuppressWarnings("resource")
    @Test
    void testFileReaderWidget() {
        class FileWriterWidgetTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @FileReaderWidget(fileExtensions = {"txt", "csv"})
            FileSelection m_fileReader;

        }
        final var fileSystemType = "myFileSystemType";
        final var fileSystemSpecifier = "fileSystemSpecifier";
        final var context = Mockito.mock(DefaultNodeSettingsContext.class);
        final var spec = Mockito.mock(FileSystemPortObjectSpec.class);
        final var location = Mockito.mock(FSLocation.class);
        Mockito.when(location.getFileSystemSpecifier()).thenReturn(Optional.of(fileSystemSpecifier));
        Mockito.when(spec.getFileSystemType()).thenReturn(fileSystemType);
        Mockito.when(spec.getFSLocationSpec()).thenReturn(location);
        Mockito.when(spec.getFileSystemConnection()).thenReturn(Optional.of(Mockito.mock(FSConnection.class)));
        Mockito.when(context.getPortObjectSpec(0)).thenReturn(Optional.of(spec));
        Mockito.when(context.getInPortTypes()).thenReturn(new PortType[]{FileSystemPortObject.TYPE});
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

            @Override
            protected Locale getLocaleForExamples() {
                return Locale.ITALIAN;
            }
        }

        class DateTimeFormatPickerWidgetTestSettings implements DefaultNodeSettings {
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
        assertThatJson(response).inPath("$.elements[0].options.formatProvider").isString()
            .isEqualTo(DateTimeFormatProvider.class.getName());

        // then the one of type TemporalFormat
        assertThatJson(response).inPath("$.elements[1].scope").isString()
            .contains("formatPickerFieldWithTemporalFormat");
        assertThatJson(response).inPath("$.elements[1].options.format").isString().isEqualTo("dateTimeFormatWithType");
        assertThatJson(response).inPath("$.elements[1].options.formatProvider").isString()
            .isEqualTo(DateTimeFormatProvider.class.getName());

        // and the repeat with no format provider
        assertThatJson(response).inPath("$.elements[2].scope").isString()
            .contains("formatPickerFieldWithTemporalFormat2");
        assertThatJson(response).inPath("$.elements[2].options.format").isString().isEqualTo("dateTimeFormatWithType");
        assertThatJson(response).inPath("$.elements[2].options.formatProvider").isString()
            .isEqualTo(ComprehensiveDateTimeFormatProvider.class.getName());
    }

    static final class TestStringProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new IllegalStateException("This method should never be called");
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            throw new IllegalStateException("This method should never be called");
        }

    }

    @Test
    void testTextInputWidget() {
        class TextInputWidgetTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @TextInputWidget(placeholder = "Bond")
            String m_textInputPlaceholder;

            @Widget(title = "", description = "")
            @TextInputWidget(placeholderProvider = TestStringProvider.class)
            String m_textInputPlaceholderProvider;

            @Widget(title = "", description = "")
            @TextInputWidget(optional = true)
            String m_textInputOptional;
        }

        var response = buildTestUiSchema(TextInputWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("textInputPlaceholder");
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString().isEqualTo("Bond");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("textInputPlaceholderProvider");
        assertThatJson(response).inPath("$.elements[1].options.placeholderProvider").isString()
            .isEqualTo(TestStringProvider.class.getName());

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("textInputOptional");
        assertThatJson(response).inPath("$.elements[2].options.hideOnNull").isBoolean().isTrue();
    }

    @Test
    void testInternalArrayWidget() {
        class InternalArrayWidgetTestSettings implements DefaultNodeSettings {

            static final class ElementSettings implements DefaultNodeSettings {

                @Widget(title = "Element value", description = "")
                @Effect(predicate = InternalArrayWidget.ElementIsEdited.class, type = EffectType.SHOW)
                String m_elementValue;
            }

            @InternalArrayWidget(withEditAndReset = true, titleProvider = TestStringProvider.class,
                subTitleProvider = TestStringProvider.class)
            @Widget(title = "title", description = "description")
            ElementSettings[] m_elementSettings;

        }

        var response = buildTestUiSchema(InternalArrayWidgetTestSettings.class);
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("elementSettings");
        assertThatJson(response).inPath("$.elements[0].options.withEditAndReset").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].options.elementTitleProvider").isString()
            .isEqualTo(TestStringProvider.class.getName());
        assertThatJson(response).inPath("$.elements[0].options.elementSubTitleProvider").isString()
            .isEqualTo(TestStringProvider.class.getName());
        assertThatJson(response).inPath("$.elements[0].options.withEditAndReset").isBoolean().isTrue();

        assertThatJson(response).inPath("$.elements[0].options.detail[0].scope").isString().contains("elementValue");
        assertThatJson(response).inPath("$.elements[0].options.detail[0].rule.condition.scope").isString()
            .isEqualTo("#/properties/_edit");

    }

    @Test
    void testTextMessage() {
        class TestSettings implements DefaultNodeSettings {

            static final class MyTextMessageProvider implements TextMessage.SimpleTextMessageProvider {

                @Override
                public boolean showMessage(final DefaultNodeSettingsContext context) {
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
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("textMessage");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("textMessage");
        assertThatJson(response).inPath("$.elements[0].options.messageProvider").isString()
            .isEqualTo(TestSettings.MyTextMessageProvider.class.getName());

    }

    static final class TestProvider implements DoubleProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new IllegalStateException("This method should never be called");

        }

        @Override
        public Double computeState(final DefaultNodeSettingsContext context) {
            throw new IllegalStateException("This method should never be called");
        }

    }

    @Test
    void testNumberInputWidget() {
        class NumberInputWidgetTestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @NumberInputWidget(min = -42)
            double m_numberInputMin;

            @Widget(title = "", description = "")
            @NumberInputWidget(max = 42)
            int m_numberInputMax;

            @Widget(title = "", description = "")
            @NumberInputWidget(minProvider = TestProvider.class)
            double m_numberInputMinProvider;

            @Widget(title = "", description = "")
            @NumberInputWidget(maxProvider = TestProvider.class)
            int m_numberInputMaxProvider;
        }

        var response = buildTestUiSchema(NumberInputWidgetTestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("numberInputMin");
        assertThatJson(response).inPath("$.elements[0].options.min").isNumber().isEqualTo(BigDecimal.valueOf(-42.0));

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("numberInputMax");
        assertThatJson(response).inPath("$.elements[1].options.max").isNumber().isEqualTo(BigDecimal.valueOf(42.0));

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("numberInputMinProvider");
        assertThatJson(response).inPath("$.elements[2].options.minProvider").isString()
            .isEqualTo(TestProvider.class.getName());

        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("numberInputMaxProvider");
        assertThatJson(response).inPath("$.elements[3].options.maxProvider").isString()
            .isEqualTo(TestProvider.class.getName());
    }

}
