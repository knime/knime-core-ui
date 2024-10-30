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
 *   Feb 22, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage.MessageType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.InternalArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class UpdatesUtilTest {

    private static DefaultNodeSettingsContext createDefaultNodeSettingsContext() {
        return DefaultNodeSettingsContext.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
            new PortObjectSpec[]{null}, null, null);
    }

    static ObjectNode buildUpdates(final Map<SettingsType, WidgetGroup> settings) {
        final var objectNode = new ObjectMapper().createObjectNode();
        final Map<SettingsType, Class<? extends WidgetGroup>> settingsClasses =
            MapValuesUtil.mapValues(settings, WidgetGroup::getClass);
        UpdatesUtil.addUpdates(
            objectNode, settingsClasses.entrySet().stream()
                .map(e -> new WidgetTreeFactory().createTree(e.getValue(), e.getKey())).toList(),
            settings, createDefaultNodeSettingsContext());
        return objectNode;
    }

    @SuppressWarnings("javadoc")
    public static ObjectNode buildUpdates(final WidgetGroup settings) {
        return buildUpdates(Map.of(SettingsType.MODEL, settings));
    }

    @Test
    void testValueUpdates() {

        class TestSettings implements DefaultNodeSettings {

            public TestSettings() {

            }

            class DependencyA implements Reference<Integer> {

            }

            @Widget(title = "", description = "")
            @ValueReference(DependencyA.class)
            int dependency;

            static class MyWidgetGroup implements WidgetGroup {
                @Widget(title = "", description = "")
                String anotherDependency;

            }

            class DependencyB implements Reference<MyWidgetGroup> {

            }

            @ValueReference(DependencyB.class)
            MyWidgetGroup anotherDependency;

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(DependencyA.class);
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(TestStateProvider.class)
            String target;

            static final class AnotherTestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.getValueSupplier(DependencyA.class);
                    initializer.computeOnValueChange(DependencyB.class);
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");

                }
            }

            @Widget(title = "", description = "")
            @ValueProvider(AnotherTestStateProvider.class)
            String anotherTarget;
        }

        final var response = buildUpdates(new TestSettings());

        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(2);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString()
            .isEqualTo(TestSettings.DependencyA.class.getName());
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/dependency"));
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(1);
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0].scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/dependency"));
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0].id").isString()
            .isEqualTo(TestSettings.DependencyA.class.getName());

        assertThatJson(response).inPath("$.globalUpdates[1].trigger.id").isString()
            .isEqualTo(TestSettings.DependencyB.class.getName());
        assertThatJson(response).inPath("$.globalUpdates[1].trigger.scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/anotherDependency"));
        assertThatJson(response).inPath("$.globalUpdates[1].dependencies").isArray().hasSize(1);
        assertThatJson(response).inPath("$.globalUpdates[1].dependencies[0].scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/dependency"));
        assertThatJson(response).inPath("$.globalUpdates[1].dependencies[0].id").isString()
            .isEqualTo(TestSettings.DependencyA.class.getName());

    }

    @Test
    void testThrowsRuntimeExceptionOnWrongTypeForValueRef() {

        class WrongTypeReferenceSettings implements DefaultNodeSettings {
            WrongTypeReferenceSettings() {

            }

            class IntegerReference implements Reference<Integer> {

            }

            @Widget(title = "", description = "")
            @ValueReference(IntegerReference.class)
            String dependency;

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(IntegerReference.class);
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(TestStateProvider.class)
            String target;
        }

        final var settings = new WrongTypeReferenceSettings();

        assertThat(assertThrows(UiSchemaGenerationException.class, () -> buildUpdates(settings)).getMessage())
            .isEqualTo(
                "The generic type \"Integer\" of the Reference \"IntegerReference\" does not match the type \"String\" of the annotated field");

    }

    @Test
    void testThrowsRuntimeExceptionOnWrongTypeForValueProvider() {

        class WrongTypeReferenceSettings implements DefaultNodeSettings {
            WrongTypeReferenceSettings() {

            }

            class MyReference implements Reference<String> {

            }

            @Widget(title = "", description = "")
            @ValueReference(MyReference.class)
            String dependency;

            static final class IntegerStateProvider implements StateProvider<Integer> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(MyReference.class);
                }

                @Override
                public Integer computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(IntegerStateProvider.class)
            String target;
        }

        final var settings = new WrongTypeReferenceSettings();

        assertThat(assertThrows(UiSchemaGenerationException.class, () -> buildUpdates(settings)).getMessage())
            .isEqualTo(
                "The generic type \"Integer\" of the StateProvider \"IntegerStateProvider\" does not match the type \"String\" of the annotated field");

    }

    @Test
    void testThrowsRuntimeExceptionOnDanglingReference() {

        class DanglingReferenceSettings implements DefaultNodeSettings {

            DanglingReferenceSettings() {

            }

            class DanglingReference implements Reference<Integer> {

            }

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(DanglingReference.class);
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(TestStateProvider.class)
            String target;
        }

        final var settings = new DanglingReferenceSettings();

        assertThat(assertThrows(RuntimeException.class, () -> buildUpdates(settings)).getMessage())
            .isEqualTo("The value reference DanglingReference is used in a state provider but could not be found. "
                + "It should be used as @ValueReference for some field.");

    }

    void testSimpleButtonWidgetUpdate() {

        @SuppressWarnings("unused")
        class TestSettings implements DefaultNodeSettings {

            TestSettings() {

            }

            class MyButtonRef implements ButtonReference {

            }

            @Widget(title = "", description = "")
            @SimpleButtonWidget(ref = MyButtonRef.class)
            Void m_button;

            class MyButtonStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeOnButtonClick(MyButtonRef.class);
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    throw new RuntimeException("Should not be called in this test");
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(MyButtonStateProvider.class)
            String m_updated;
        }

        final var settings = new TestSettings();
        final var response = buildUpdates(settings);

        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(2);
        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().doesNotContainKey("scope");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString()
            .isEqualTo(TestSettings.MyButtonRef.class.getName());
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(0);
    }

    @Test
    void testUpdateBeforeOpenDialog() {
        @SuppressWarnings("unused")
        class TestSettings implements DefaultNodeSettings {

            TestSettings() {

            }

            static final class MySetting {

                final String m_value;

                MySetting(final String value) {
                    m_value = value;
                }

            }

            static final class MyInitialFileExtensionProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                public static final String RESULT = "txt";

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @LocalFileWriterWidget(fileExtensionProvider = MyInitialFileExtensionProvider.class)
            String m_localFileWriter;

            static final class MyValueProvider implements StateProvider<MySetting> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                public static final String RESULT = "updated string";

                @Override
                public MySetting computeState(final DefaultNodeSettingsContext context) {
                    return new MySetting(RESULT);
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(MyValueProvider.class)
            MySetting m_valueUpdateSetting;

        }
        final var settings = new TestSettings();
        final var response = buildUpdates(settings);

        assertThatJson(response).inPath("$").isObject().doesNotContainKey("globalUpdates");
        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(2);
        assertThatJson(response).inPath("$.initialUpdates[0].scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/valueUpdateSetting"));
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isObject().containsEntry("value",
            TestSettings.MyValueProvider.RESULT);
        assertThatJson(response).inPath("$.initialUpdates[1].id").isString()
            .isEqualTo(TestSettings.MyInitialFileExtensionProvider.class.getName());
        assertThatJson(response).inPath("$.initialUpdates[1].values[0].value").isString()
            .isEqualTo(TestSettings.MyInitialFileExtensionProvider.RESULT);
    }

    @Test
    void testUpdateBeforeOpenDialogWithDependency() {

        class TestSettings implements DefaultNodeSettings {

            TestSettings() {
            }

            static final class MyValueRef implements Reference<String> {

            }

            static final class MyOtherValueRef implements Reference<String> {

            }

            @Widget(title = "", description = "")
            @ValueReference(MyOtherValueRef.class)
            String m_otherSetting = "foo";

            static final class MyValueProvider implements StateProvider<String> {

                Supplier<String> m_valueSupplier;

                Supplier<String> m_otherValueSupplier;

                @Override
                public void init(final StateProviderInitializer initializer) {
                    m_valueSupplier = initializer.getValueSupplier(MyValueRef.class);
                    m_otherValueSupplier = initializer.computeFromValueSupplier(MyOtherValueRef.class);
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    return String.format("{self:%s,other:%s}", m_valueSupplier.get(), m_otherValueSupplier.get());
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(MyValueProvider.class)
            @ValueReference(MyValueRef.class)
            String m_valueUpdateSetting;

        }
        final var settings = new TestSettings();
        final var response = buildUpdates(settings);
        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
        assertThatJson(response).inPath("$.initialUpdates[0].scopes").isArray()
            .isEqualTo(List.of("#/properties/model/properties/valueUpdateSetting"));
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString()
            .isEqualTo("{self:null,other:foo}");

    }

    @Test
    void testUpdateAfterOpenDialog() {
        class TestSettings implements DefaultNodeSettings {

            TestSettings() {
            }

            static final class MyInitialFileExtensionProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeAfterOpenDialog();
                }

                public static final String RESULT = "txt";

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @LocalFileWriterWidget(fileExtensionProvider = MyInitialFileExtensionProvider.class)
            String m_localFileWriter;

            static final class MyValueProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeAfterOpenDialog();
                }

                public static final String RESULT = "updated string";

                @Override
                public String computeState(final DefaultNodeSettingsContext context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(MyValueProvider.class)
            String m_valueUpdateSetting;

        }
        final var settings = new TestSettings();
        final var response = buildUpdates(settings);

        assertThatJson(response).inPath("$").isObject().doesNotContainKey("initialUpdates");
        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().doesNotContainKey("scope");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.triggerInitially").isBoolean().isTrue();
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString().isEqualTo("after-open-dialog");
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(0);
    }

    @Nested
    class UIStateUpdateTest {

        static final class MyStringProvider implements StateProvider<String> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
            }

            @Override
            public String computeState(final DefaultNodeSettingsContext context) {
                throw new IllegalStateException("Should not be called within this test");
            }

        }

        @Test
        void testFileWriterWidgetFileExtensionProvider() {

            class TestSettings implements DefaultNodeSettings {

                @FileWriterWidget(fileExtensionProvider = MyStringProvider.class)
                FileSelection m_fileChooser;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        @Test
        void testLocalFileWriterWidgetFileExtensionProvider() {

            class TestSettings implements DefaultNodeSettings {

                @LocalFileWriterWidget(fileExtensionProvider = MyStringProvider.class)
                String m_fileChooser;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        @Test
        void testTextInputWidgetPlaceholderProvider() {

            class TestSettings implements DefaultNodeSettings {

                @TextInputWidget(placeholderProvider = MyStringProvider.class)
                String m_textInput;

            }

            final var settings = new TestSettings();

            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        @Test
        void testTextMessageProvider() {
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

            final Map<SettingsType, WidgetGroup> settings = Map.of(SettingsType.MODEL, new TestSettings());
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
        }

        @Test
        void testArrayWidgetElementDefaultValueProvider() {

            class TestSettings implements DefaultNodeSettings {

                static class ElementSettings implements WidgetGroup {

                }

                static final class MyElementDefaultValueProvider implements StateProvider<ElementSettings> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeAfterOpenDialog();
                    }

                    @Override
                    public ElementSettings computeState(final DefaultNodeSettingsContext context) {
                        return null;
                    }

                }

                @ArrayWidget(elementDefaultValueProvider = MyElementDefaultValueProvider.class)
                ElementSettings[] m_array;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        @Test
        void testInternalArrayLayoutProviders() {

            class TestSettings implements DefaultNodeSettings {

                static class ElementSettings implements WidgetGroup {

                }

                @InternalArrayWidget(titleProvider = MyStringProvider.class, subTitleProvider = MyStringProvider.class)
                ElementSettings[] m_array;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        static final class TestStringChoicesProvider implements StringChoicesStateProvider {

            static final String[] RESULT = new String[]{"A", "B", "C"};

            @Override
            public String[] choices(final DefaultNodeSettingsContext context) {
                return RESULT;
            }

        }

        @Test
        void testChoicesWidgetStringChoicesStateProvider() {
            class TestSettings implements DefaultNodeSettings {

                @ChoicesWidget(choicesProvider = TestStringChoicesProvider.class)
                String m_string;

            }
            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].id").isString()
                .isEqualTo(TestStringChoicesProvider.class.getName());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(3);
            List.of(0, 1, 2).forEach(i -> {
                assertThatJson(response).inPath(String.format("$.initialUpdates[0].values[0].value[%s].id", i))
                    .isString().isEqualTo(TestStringChoicesProvider.RESULT[i]);
                assertThatJson(response).inPath(String.format("$.initialUpdates[0].values[0].value[%s].text", i))
                    .isString().isEqualTo(TestStringChoicesProvider.RESULT[i]);
            });

        }

        static final class TestColumnChoicesProvider implements ColumnChoicesStateProvider {

            @Override
            public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
                return new DataColumnSpec[]{new DataColumnSpecCreator("A", StringCell.TYPE).createSpec()};
            }

        }

        @Test
        void testChoicesWidgetColumnChoicesStateProvider() {
            class TestSettings implements DefaultNodeSettings {

                @ChoicesWidget(choicesProvider = TestColumnChoicesProvider.class)
                ColumnSelection m_columnSelection;

            }
            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].id").isString()
                .isEqualTo(TestColumnChoicesProvider.class.getName());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].id").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].text").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.id").isString()
                .isEqualTo(StringCell.TYPE.getPreferredValueClass().getName());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.text").isString()
                .isEqualTo(StringCell.TYPE.getName());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].compatibleTypes").isArray()
                .hasSize(3);
        }
    }

    @Nested
    class ArrayLayoutUpdatesTest {

        @Test
        void testUpdateWithDependenciesInsideArrayElements() {

            class TestSettings implements DefaultNodeSettings {
                static final class DependencyOutsideArray implements Reference<String> {
                }

                static final class ElementSettings implements DefaultNodeSettings {
                    static final class DependencyInsideArray implements Reference<String> {
                    }

                    @ValueReference(DependencyInsideArray.class)
                    String m_dependencyInsideArray;

                    static final class TriggerReference implements ButtonReference {
                    }

                    @SimpleButtonWidget(ref = TriggerReference.class)
                    Void m_trigger;

                    static final class MyProvider implements StateProvider<String> {

                        @Override
                        public void init(final StateProviderInitializer initializer) {
                            initializer.computeOnButtonClick(TriggerReference.class);
                            initializer.getValueSupplier(DependencyInsideArray.class);
                            initializer.getValueSupplier(DependencyOutsideArray.class);

                        }

                        @Override
                        public String computeState(final DefaultNodeSettingsContext context) {
                            throw new IllegalStateException("Should not be called in this test");
                        }

                    }

                    @ValueProvider(MyProvider.class)
                    String m_effectField;

                }

                @ValueReference(DependencyOutsideArray.class)
                String m_dependencyOutsideArray;

                @SuppressWarnings("unused")
                ElementSettings[] m_array;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString()
                .isEqualTo(TestSettings.ElementSettings.TriggerReference.class.getName());
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(2);
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0].id").isString()
                .isEqualTo(TestSettings.DependencyOutsideArray.class.getName());
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0].scopes").isArray()
                .isEqualTo(List.of("#/properties/model/properties/dependencyOutsideArray"));
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[1].id").isString()
                .isEqualTo(TestSettings.ElementSettings.DependencyInsideArray.class.getName());
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[1].scopes").isArray()
                .isEqualTo(List.of("#/properties/model/properties/array", "#/properties/dependencyInsideArray"));

        }

        @Test
        void testInitialTriggerWithDependencyInsideArray() {

            class TestSettings implements DefaultNodeSettings {

                static final class ElementSettings implements DefaultNodeSettings {
                    static final class DependencyInsideArray implements Reference<String> {
                    }

                    ElementSettings(final String dependency) {
                        m_dependencyInsideArray = dependency;
                    }

                    @ValueReference(DependencyInsideArray.class)
                    String m_dependencyInsideArray;

                    static final class MyProvider implements StateProvider<String> {

                        private Supplier<String> m_valueSupplier;

                        @Override
                        public void init(final StateProviderInitializer initializer) {
                            initializer.computeBeforeOpenDialog();
                            m_valueSupplier = initializer.getValueSupplier(DependencyInsideArray.class);

                        }

                        @Override
                        public String computeState(final DefaultNodeSettingsContext context) {
                            return m_valueSupplier.get();
                        }

                    }

                    @ValueProvider(MyProvider.class)
                    String m_effectField;

                }

                @SuppressWarnings("unused")
                ElementSettings[] m_array =
                    new ElementSettings[]{new ElementSettings("foo"), new ElementSettings("bar")};

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].values").isArray().hasSize(2);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].indices[0]").isNumber().isZero();
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo("foo");
            assertThatJson(response).inPath("$.initialUpdates[0].values[1].indices[0]").isNumber()
                .isEqualTo(new BigDecimal(1));
            assertThatJson(response).inPath("$.initialUpdates[0].values[1].value").isString().isEqualTo("bar");

        }

    }

    @Test
    void testInternalArrayWidgetElementResetButtonId() {

        class InternalArrayWidgetTestSettings implements DefaultNodeSettings {

            static final class ElementSettings implements DefaultNodeSettings {
                static final class ElementValueResetter implements StateProvider<String> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeOnButtonClick(InternalArrayWidget.ElementResetButton.class);
                    }

                    @Override
                    public String computeState(final DefaultNodeSettingsContext context) {
                        return null;
                    }

                }

                @ValueProvider(ElementValueResetter.class)
                @Widget(title = "Element value", description = "")
                String m_elementValue;
            }

            @InternalArrayWidget(withEditAndReset = true)
            @Widget(title = "title", description = "description")
            ElementSettings[] m_elementSettings;

        }

        final Map<SettingsType, WidgetGroup> settings =
            Map.of(SettingsType.MODEL, new InternalArrayWidgetTestSettings());
        final var response = buildUpdates(settings);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString().isEqualTo("ElementResetButton");

    }
}