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
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtilTest.UIStateUpdateTest.TestDynamicParams;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DynamicParametersTriggerInvocationHandlerContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.NodeDialogServiceRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.validation.CustomValidationContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CheckboxRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class UpdatesUtilTest {

    private static NodeParametersInput createDefaultNodeSettingsContext() {
        return NodeParametersInputImpl.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
            new PortObjectSpec[]{null}, null, null);
    }

    static ObjectNode buildUpdates(final Map<SettingsType, WidgetGroup> settings) {
        return buildUpdates(settings, createDefaultNodeSettingsContext(), null);
    }

    static ObjectNode buildUpdates(final Map<SettingsType, WidgetGroup> settings, final NodeParametersInput context,
        final NodeDialogServiceRegistry serviceRegistry) {
        final var objectNode = new ObjectMapper().createObjectNode();
        final Map<SettingsType, Class<? extends WidgetGroup>> settingsClasses =
            MapValuesUtil.mapValues(settings, WidgetGroup::getClass);
        UpdatesUtil.addUpdates(
            objectNode, settingsClasses.entrySet().stream()
                .map(e -> new WidgetTreeFactory().createTree(e.getValue(), e.getKey())).toList(),
            settings, context, serviceRegistry);
        return objectNode;
    }

    @SuppressWarnings("javadoc")
    public static ObjectNode buildUpdates(final WidgetGroup settings) {
        return buildUpdates(Map.of(SettingsType.MODEL, settings));
    }

    private static ObjectNode buildUpdatesWithNullContext(final WidgetGroup settings) {
        return buildUpdates(Map.of(SettingsType.MODEL, settings), null, null);
    }

    @Test
    void testValueUpdates() {

        class TestSettings implements NodeParameters {

            public TestSettings() {

            }

            class DependencyA implements ParameterReference<Integer> {

            }

            @Widget(title = "", description = "")
            @ValueReference(DependencyA.class)
            int dependency;

            static class MyWidgetGroup implements WidgetGroup {
                @Widget(title = "", description = "")
                String anotherDependency;

            }

            class DependencyB implements ParameterReference<MyWidgetGroup> {

            }

            @ValueReference(DependencyB.class)
            MyWidgetGroup anotherDependency;

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(DependencyA.class);
                }

                @Override
                public String computeState(final NodeParametersInput context) {
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
                public String computeState(final NodeParametersInput context) {
                    throw new RuntimeException("Should not be called in this test");

                }
            }

            @Widget(title = "", description = "")
            @ValueProvider(AnotherTestStateProvider.class)
            String anotherTarget;
        }

        final var response = buildUpdates(new TestSettings());

        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(2);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().containsOnlyKeys("scope");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.scope").isString()
            .isEqualTo("#/properties/model/properties/anotherDependency");
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(1);
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0]").isString()
            .isEqualTo("#/properties/model/properties/dependency");

        assertThatJson(response).inPath("$.globalUpdates[1].trigger").isObject().containsOnlyKeys("scope");
        assertThatJson(response).inPath("$.globalUpdates[1].trigger.scope").isString()
            .isEqualTo("#/properties/model/properties/dependency");
        assertThatJson(response).inPath("$.globalUpdates[1].dependencies").isArray().hasSize(1);
        assertThatJson(response).inPath("$.globalUpdates[1].dependencies[0]").isString()
            .isEqualTo("#/properties/model/properties/dependency");

    }

    @Test
    void testNullValuedValueUpdate() {
        class TestSettings implements NodeParameters {

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
                    return null;
                }
            }

            @Widget(title = "", description = "")
            @ValueProvider(TestStateProvider.class)
            String target;
        }

        final var response = buildUpdates(new TestSettings());

        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);

    }

    @Test
    void testThrowsRuntimeExceptionOnWrongTypeForValueRef() {

        class WrongTypeReferenceSettings implements NodeParameters {
            WrongTypeReferenceSettings() {

            }

            class IntegerReference implements ParameterReference<Integer> {

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
                public String computeState(final NodeParametersInput context) {
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
                "The generic type \"Integer\" of the ParameterReference \"IntegerReference\" does not match the type \"String\" of the annotated field");

    }

    @Test
    void testThrowsRuntimeExceptionOnWrongTypeForValueProvider() {

        class WrongTypeReferenceSettings implements NodeParameters {
            WrongTypeReferenceSettings() {

            }

            class MyReference implements ParameterReference<String> {

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
                public Integer computeState(final NodeParametersInput context) {
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

        class DanglingReferenceSettings implements NodeParameters {

            DanglingReferenceSettings() {

            }

            class DanglingReference implements ParameterReference<Integer> {

            }

            static final class TestStateProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeFromValueSupplier(DanglingReference.class);
                }

                @Override
                public String computeState(final NodeParametersInput context) {
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
        class TestSettings implements NodeParameters {

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
                public String computeState(final NodeParametersInput context) {
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
    void testUpdateDependingOnTheContext() {
        class TestSettings implements NodeParameters {

            static final class OnlyProvideWhenContextIsNull implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    if (initializer.getNodeParametersInput() != null) {
                        initializer.computeBeforeOpenDialog();
                    }
                }

                @Override
                public String computeState(final NodeParametersInput context) {
                    return "Some value computed from the context";
                }

            }

            @ValueProvider(OnlyProvideWhenContextIsNull.class)
            @Widget(description = "", title = "")
            String m_settings;
        }

        final var settings = new TestSettings();

        assertThatJson(buildUpdates(settings)).inPath("$.initialUpdates").isArray().hasSize(1);
        assertThatJson(buildUpdatesWithNullContext(settings)).inPath("$.initialUpdates").isAbsent();
    }

    @Test
    void testUpdateBeforeOpenDialog() {
        @SuppressWarnings("unused")
        class TestSettings implements NodeParameters {

            TestSettings() {

            }

            static final class MySetting {

                final String m_value;

                MySetting(final String value) {
                    m_value = value;
                }

            }

            static final class MyInitialPlaceholderProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                public static final String RESULT = "some placeholder";

                @Override
                public String computeState(final NodeParametersInput context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @TextInputWidget(placeholderProvider = MyInitialPlaceholderProvider.class)
            String m_localFileWriter;

            static final class MyValueProvider implements StateProvider<MySetting> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                public static final String RESULT = "updated string";

                @Override
                public MySetting computeState(final NodeParametersInput context) {
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
        assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
            .isEqualTo("#/properties/model/properties/localFileWriter");
        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString().isEqualTo("placeholder");
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString()
            .isEqualTo(TestSettings.MyInitialPlaceholderProvider.RESULT);
        assertThatJson(response).inPath("$.initialUpdates[1].scope").isString()
            .isEqualTo("#/properties/model/properties/valueUpdateSetting");
        assertThatJson(response).inPath("$.initialUpdates[1].values[0].value").isObject().containsEntry("value",
            TestSettings.MyValueProvider.RESULT);
    }

    @Test
    void testUpdateBeforeOpenDialogWithDependency() {

        class TestSettings implements NodeParameters {

            TestSettings() {
            }

            static final class MyValueRef implements ParameterReference<String> {

            }

            static final class MyOtherValueRef implements ParameterReference<String> {

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
                public String computeState(final NodeParametersInput context) {
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
        assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
            .isEqualTo("#/properties/model/properties/valueUpdateSetting");
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString()
            .isEqualTo("{self:null,other:foo}");

    }

    @Test
    void testUpdateAfterOpenDialog() {
        class TestSettings implements NodeParameters {

            TestSettings() {
            }

            static final class MyInitialPlaceholderProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeAfterOpenDialog();
                }

                public static final String RESULT = "some placeholder";

                @Override
                public String computeState(final NodeParametersInput context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @TextInputWidget(placeholderProvider = MyInitialPlaceholderProvider.class)
            String m_localFileWriter;

            static final class MyValueProvider implements StateProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeAfterOpenDialog();
                }

                public static final String RESULT = "updated string";

                @Override
                public String computeState(final NodeParametersInput context) {
                    return RESULT;
                }

            }

            @Widget(title = "", description = "")
            @ValueProvider(MyValueProvider.class)
            String m_valueUpdateSetting;

        }
        final var settings = new TestSettings();
        final var response = buildUpdates(settings);

        assertAfterOpenDialogWithoutDependencies(response);
    }

    static void assertAfterOpenDialogWithoutDependencies(final ObjectNode response) {
        assertThatJson(response).inPath("$").isObject().doesNotContainKey("initialUpdates");
        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().doesNotContainKey("scope");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString().isEqualTo("after-open-dialog");
        assertThatJson(response).inPath("$.globalUpdates[0].triggerInitially").isBoolean().isTrue();
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
            public String computeState(final NodeParametersInput context) {
                throw new IllegalStateException("Should not be called within this test");
            }

        }

        @Test
        void testTextInputWidgetPlaceholderProvider() {

            class TestSettings implements NodeParameters {

                @TextInputWidget(placeholderProvider = MyStringProvider.class)
                String m_textInput;

            }

            final var settings = new TestSettings();

            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        @Test
        void testTextMessageProvider() {
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

            final Map<SettingsType, WidgetGroup> settings = Map.of(SettingsType.MODEL, new TestSettings());
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].id").isString()
                .isEqualTo("#/properties/model/properties/textMessage");
            assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString().isEqualTo("message");
        }

        @Test
        void testArrayWidgetElementDefaultValueProvider() {

            class TestSettings implements NodeParameters {

                static class ElementSettings implements WidgetGroup {

                }

                static final class MyElementDefaultValueProvider implements StateProvider<ElementSettings> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeAfterOpenDialog();
                    }

                    @Override
                    public ElementSettings computeState(final NodeParametersInput context) {
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

            class TestSettings implements NodeParameters {

                static class ElementSettings implements WidgetGroup {

                }

                @ArrayWidgetInternal(titleProvider = MyStringProvider.class, subTitleProvider = MyStringProvider.class)
                ElementSettings[] m_array;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        }

        static final class TestStringChoicesProvider implements StringChoicesProvider {

            static final List<String> RESULT = List.of("A", "B", "C");

            @Override
            public List<String> choices(final NodeParametersInput context) {
                return RESULT;
            }

        }

        @Test
        void testChoicesWidgetStringChoicesStateProvider() {
            class TestSettings implements NodeParameters {

                @ChoicesProvider(TestStringChoicesProvider.class)
                String m_string;

            }
            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .isEqualTo("#/properties/model/properties/string");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(3);
            List.of(0, 1, 2).forEach(i -> {
                assertThatJson(response).inPath(String.format("$.initialUpdates[0].values[0].value[%s].id", i))
                    .isString().isEqualTo(TestStringChoicesProvider.RESULT.get(i));
                assertThatJson(response).inPath(String.format("$.initialUpdates[0].values[0].value[%s].text", i))
                    .isString().isEqualTo(TestStringChoicesProvider.RESULT.get(i));
            });

        }

        enum MyEnum {
                A, //
                @Label("B_title")
                B, //
                C;
        }

        static final class MyEnumProvider implements EnumChoicesProvider<MyEnum> {

            @Override
            public List<MyEnum> choices(final NodeParametersInput context) {
                return List.of(MyEnum.A, MyEnum.B);
            }

        }

        @Test
        void testEnumChoicesProvider() {
            class TestSettings implements NodeParameters {

                @ChoicesProvider(MyEnumProvider.class)
                MyEnum m_enum;

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .isEqualTo("#/properties/model/properties/enum");
            assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
                .isEqualTo("possibleValues");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(2);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].id").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].text").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[1].id").isString().isEqualTo("B");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[1].text").isString()
                .isEqualTo("B_title");

        }

        static final class TestColumnChoicesProvider implements ColumnChoicesProvider {

            @Override
            public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
                return List.of(new DataColumnSpecCreator("A", StringCell.TYPE).createSpec());
            }

        }

        @ParameterizedTest
        @MethodSource("columnChoicesProviderSettings")
        void testChoicesWidgetColumnChoicesStateProvider(final NodeParameters settings) {

            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .startsWith("#/properties/model/properties/column");
            assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
                .isEqualTo("possibleValues");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].id").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].text").isString().isEqualTo("A");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.id").isString()
                .isEqualTo(StringCell.TYPE.getPreferredValueClass().getName());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.text").isString()
                .isEqualTo(StringCell.TYPE.getName());
        }

        static Stream<Arguments> columnChoicesProviderSettings() {

            class TestCaseChoicesProvider implements NodeParameters {

                @ChoicesProvider(TestColumnChoicesProvider.class)
                String m_columnSelection;

            }

            class TestCaseColumnFilterWidget implements NodeParameters {

                @ColumnFilterWidget(choicesProvider = TestColumnChoicesProvider.class)
                ColumnFilter m_columnFilter;
            }

            return Stream.of( //
                Arguments.of(new TestCaseChoicesProvider()), //
                Arguments.of(new TestCaseColumnFilterWidget())//
            );
        }

        @ParameterizedTest
        @MethodSource("flowVariableChoicesProviderSettings")
        void testChoicesWidgetFlowVariableChoicesStateProvider(final NodeParameters settings) {

            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .startsWith("#/properties/model/properties/flowVariable");
            assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
                .isEqualTo("possibleValues");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSize(2);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].id").isString()
                .isEqualTo("someInt");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].text").isString()
                .isEqualTo("someInt");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.id").isString()
                .isEqualTo(IntType.INSTANCE.getIdentifier());
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value[0].type.text").isString()
                .isEqualTo("IntType");
        }

        static final class TestFlowVariableChoicesProvider implements FlowVariableChoicesProvider {

            @Override
            public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
                return List.of(new FlowVariable("someInt", 123), new FlowVariable("someBoolean", BooleanType.INSTANCE));
            }

        }

        static Stream<Arguments> flowVariableChoicesProviderSettings() {

            class TestCaseChoicesProvider implements NodeParameters {

                @ChoicesProvider(TestFlowVariableChoicesProvider.class)
                String m_flowVariableSelection;

            }

            class TestCaseFlowVariableFilterWidget implements NodeParameters {

                @FlowVariableFilterWidget(choicesProvider = TestFlowVariableChoicesProvider.class)
                FlowVariableFilter m_flowVariableFilter;
            }

            return Stream.of( //
                Arguments.of(new TestCaseChoicesProvider()), //
                Arguments.of(new TestCaseFlowVariableFilterWidget())//
            );
        }

        @Test
        void testDataTypeChoicesStateProvider() {
            class TestSettings implements NodeParameters {

                @Widget(title = "Data type", description = "Select the data type to be displayed in the table")
                DataType m_dataType = StringCell.TYPE;

                static final class OnlyStringAndDoubleChoicesProvider implements DataTypeChoicesProvider {
                    @Override
                    public List<DataType> choices(final NodeParametersInput context) {
                        return List.of(StringCell.TYPE, DoubleCell.TYPE);
                    }
                }

                @Widget(title = "Data type with limited choices",
                    description = "Select the data type to be displayed in the table")
                @ChoicesProvider(OnlyStringAndDoubleChoicesProvider.class)
                DataType m_limitedChoicesDataType = StringCell.TYPE;
            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(2);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .isEqualTo("#/properties/model/properties/dataType");
            assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
                .isEqualTo("possibleValues");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isArray().hasSizeGreaterThan(2);
            assertThatJson(response).inPath("$.initialUpdates[1].scope").isString()
                .isEqualTo("#/properties/model/properties/limitedChoicesDataType");
            assertThatJson(response).inPath("$.initialUpdates[1].providedOptionName").isString()
                .isEqualTo("possibleValues");
            assertThatJson(response).inPath("$.initialUpdates[1].values[0].value").isArray().hasSize(2);
            assertThatJson(response).inPath("$.initialUpdates[1].values[0].value[0].id").isString()
                .contains(DoubleCell.class.getName());
            assertThatJson(response).inPath("$.initialUpdates[1].values[0].value[0].text").isString()
                .isEqualTo(DoubleCell.TYPE.getName());
            assertThatJson(response).inPath("$.initialUpdates[1].values[0].value[1].id").isString()
                .contains(StringCell.class.getName());
            assertThatJson(response).inPath("$.initialUpdates[1].values[0].value[1].text").isString()
                .isEqualTo(StringCell.TYPE.getName());

        }

        static final class MyDynamicMinValidation implements StateProvider<MinValidation> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
            }

            @Override
            public MinValidation computeState(final NodeParametersInput context) {
                throw new IllegalStateException("Should not be called in this test");
            }

        }

        @Test
        void testNumberInputProvider() {
            class TestSettings implements NodeParameters {

                @NumberInputWidget(minValidationProvider = MyDynamicMinValidation.class)
                double m_numberInput = 5;
            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);
        }

        @Test
        void testDynamicSettingsWidgetUpdate() {

            class TestSettings implements NodeParameters {

                static final class MyUpdater implements DynamicSettingsWidget.ImperativeDialogProvider {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeBeforeOpenDialog();
                    }

                    @Override
                    public Pair<Map<String, Object>, DialogElementRendererSpec<?>> computeSettingsAndDialog(
                        final NodeParametersInput context) throws StateComputationFailureException {
                        final var checkbox = new CheckboxRendererSpec() {

                            @Override
                            public String getTitle() {
                                return "My dynamic checkbox";
                            }
                        }.at("myCheckbox");

                        return new Pair<>(Map.of("myCheckbox", true), checkbox);
                    }

                }

                @DynamicSettingsWidget(MyUpdater.class)
                Map<String, Object> m_dynamicSettings;
            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
                .isEqualTo("#/properties/model/properties/dynamicSettings");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isObject().containsKeys("data",
                "schema", "uiSchema");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value.data").isObject()
                .containsEntry("myCheckbox", true);
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value.schema").isString()
                .contains("My dynamic checkbox");
            assertThatJson(response).inPath("$.initialUpdates[0].values[0].value.uiSchema").isString()
                .contains("checkbox");
        }

        interface TestDynamicParams extends DynamicParameters.DynamicNodeParameters {
        }

        static class TestDynamicImpl implements TestDynamicParams {
            @Widget(title = "Test Field", description = "A test field for dynamic parameters")
            public String testField = "test value";

            @Widget(title = "Test Number", description = "A test number field")
            public int testNumber = 42;
        }

        @Test
        void testDynamicInterfaceParametersUpdate() {

            class TestSettings implements NodeParameters {

                static final class MyDynamicProvider
                    implements DynamicParameters.DynamicParametersProvider<TestDynamicParams> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeAfterOpenDialog();
                    }

                    @Override
                    public ClassIdStrategy<TestDynamicParams> getClassIdStrategy() {
                        return new DefaultClassIdStrategy<>(List.of(TestDynamicImpl.class));
                    }

                    @Override
                    public TestDynamicParams computeParameters(final NodeParametersInput parametersInput)
                        throws StateComputationFailureException {
                        return new TestDynamicImpl();
                    }
                }

                @DynamicParameters(MyDynamicProvider.class)
                TestDynamicParams m_dynamicParameters;
            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);

            assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);
            assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString().isEqualTo("after-open-dialog");
            assertThatJson(response).inPath("$.globalUpdates[0].triggerInitially").isBoolean().isTrue();
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(0);
        }

    }

    @Nested
    class ArrayLayoutUpdatesTest {

        @Test
        void testUpdateWithDependenciesInsideArrayElements() {

            class TestSettings implements NodeParameters {
                static final class DependencyOutsideArray implements ParameterReference<String> {
                }

                static final class ElementSettings implements NodeParameters {
                    static final class DependencyInsideArray implements ParameterReference<String> {
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
                        public String computeState(final NodeParametersInput context) {
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
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0]").isString()
                .isEqualTo("#/properties/model/properties/array/items/properties/dependencyInsideArray");
            assertThatJson(response).inPath("$.globalUpdates[0].dependencies[1]").isString()
                .isEqualTo("#/properties/model/properties/dependencyOutsideArray");

        }

        @Test
        void testInitialTriggerWithDependencyInsideArray() {

            class TestSettings implements NodeParameters {

                static final class ElementSettings implements NodeParameters {
                    static final class DependencyInsideArray implements ParameterReference<String> {
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
                        public String computeState(final NodeParametersInput context) {
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

        @Test
        void testInitialTriggerWithDependenciesInAndOutsideTheArrayWithEmptyArray() {
            class TestSettings implements NodeParameters {

                static final class ElementSettings implements NodeParameters {
                    static final class DependencyInsideArray implements ParameterReference<String> {
                    }

                    @ValueReference(DependencyInsideArray.class)
                    String m_dependencyInsideArray;

                    static final class MyProvider implements StateProvider<String> {

                        private Supplier<String> m_valueSupplier;

                        @Override
                        public void init(final StateProviderInitializer initializer) {
                            initializer.computeBeforeOpenDialog();
                            m_valueSupplier = initializer.getValueSupplier(DependencyInsideArray.class);
                            initializer.getValueSupplier(DependencyOutsideArray.class);

                        }

                        @Override
                        public String computeState(final NodeParametersInput context) {
                            return m_valueSupplier.get();
                        }

                    }

                    @ValueProvider(MyProvider.class)
                    String m_effectField;

                }

                static final class DependencyOutsideArray implements ParameterReference<String> {
                }

                @ValueReference(DependencyOutsideArray.class)
                String m_depenencyOutsideArray;

                @SuppressWarnings("unused")
                ElementSettings[] m_array = new ElementSettings[0];

            }

            final var settings = new TestSettings();
            final var response = buildUpdates(settings);
            assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(0);
        }

    }

    @Test
    void testInternalArrayWidgetElementResetButtonId() {

        class InternalArrayWidgetTestSettings implements NodeParameters {

            static final class ElementSettings implements NodeParameters {
                static final class ElementValueResetter implements StateProvider<String> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeOnButtonClick(ArrayWidgetInternal.ElementResetButton.class);
                    }

                    @Override
                    public String computeState(final NodeParametersInput context) {
                        return null;
                    }

                }

                @ValueProvider(ElementValueResetter.class)
                @Widget(title = "Element value", description = "")
                String m_elementValue;
            }

            @ArrayWidgetInternal(withEditAndReset = true)
            @Widget(title = "title", description = "description")
            ElementSettings[] m_elementSettings;

        }

        final Map<SettingsType, WidgetGroup> settings =
            Map.of(SettingsType.MODEL, new InternalArrayWidgetTestSettings());
        final var response = buildUpdates(settings);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().containsOnlyKeys("id");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.id").isString().isEqualTo("ElementResetButton");

    }

    static final class TestConnectionProvider implements StateProvider<FSConnectionProvider> {
        @Override
        public FSConnectionProvider computeState(final NodeParametersInput context) {
            return () -> {
                throw new IllegalStateException("Should not be called in this test");
            };
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }
    }

    @Test
    void testFSConnectionProviderIsIntercepted() {

        class TestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @FileSelectionWidget(SingleFileSelectionMode.FILE)
            @WithCustomFileSystem(connectionProvider = TestConnectionProvider.class)
            String m_path;
        }
        final var fileSystemId = "myFileSystemId";
        final var fileSystemConnector = Mockito.mock(FileSystemConnector.class);
        Mockito.when(fileSystemConnector.registerCustomFileSystem(ArgumentMatchers.any(FSConnectionProvider.class),
            ArgumentMatchers.anyString(), ArgumentMatchers.anyList())).thenReturn(fileSystemId);

        final var validationContext = Mockito.mock(CustomValidationContext.class);
        final var dynamicParametersTriggerInvocationHandlerSupplier =
            Mockito.mock(DynamicParametersTriggerInvocationHandlerContext.class);
        final var serviceRegistry = new NodeDialogServiceRegistry(fileSystemConnector, validationContext,
            dynamicParametersTriggerInvocationHandlerSupplier);

        final var response = buildUpdates(Map.of(SettingsType.MODEL, new TestSettings()),
            createDefaultNodeSettingsContext(), serviceRegistry);

        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
            .isEqualTo(UpdateResultsUtil.FILE_SYSTEM_ID);
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo(fileSystemId);
    }

    @Test
    void testCustomValidationCallbackIsIntercepted() {

        class TestSettings implements NodeParameters {

            static final class TestValidationProvider implements CustomValidationProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public ValidationCallback<String> computeValidationCallback(final NodeParametersInput context) {
                    return value -> {
                        if (value == null || value.isEmpty()) {
                            throw new InvalidSettingsException("Field cannot be empty");
                        }
                    };
                }
            }

            @Widget(title = "", description = "")
            @CustomValidation(TestValidationProvider.class)
            String m_field = "initial value";
        }

        final var testValidatorId = "test-validator-uuid";
        final var fileSystemConnector = Mockito.mock(FileSystemConnector.class);
        final var validationContext = Mockito.mock(CustomValidationContext.class);
        final var dynamicParametersTriggerInvocationHandlerSupplier =
            Mockito.mock(DynamicParametersTriggerInvocationHandlerContext.class);
        Mockito.when(validationContext.registerValidator(ArgumentMatchers.any(ValidationCallback.class),
            ArgumentMatchers.eq(String.class))).thenReturn(testValidatorId);

        final var serviceRegistry = new NodeDialogServiceRegistry(fileSystemConnector, validationContext,
            dynamicParametersTriggerInvocationHandlerSupplier);
        final var response = buildUpdates(Map.of(SettingsType.MODEL, new TestSettings()),
            createDefaultNodeSettingsContext(), serviceRegistry);

        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
            .isEqualTo(UpdateResultsUtil.VALIDATOR_ID);
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo(testValidatorId);

        Mockito.verify(validationContext).registerValidator(ArgumentMatchers.any(ValidationCallback.class),
            ArgumentMatchers.eq(String.class));
    }

    @Test
    void testCustomValidationWithInitiallyInvalidValue() {

        class TestSettings implements NodeParameters {

            static final class TestValidationProvider implements CustomValidationProvider<String> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public ValidationCallback<String> computeValidationCallback(final NodeParametersInput context) {
                    return value -> {
                        if (value == null || value.isEmpty()) {
                            throw new InvalidSettingsException("Field cannot be empty");
                        }
                    };
                }
            }

            @Widget(title = "", description = "")
            @CustomValidation(TestValidationProvider.class)
            String m_field = ""; // Empty = invalid, but we don't send initial error message
        }

        final var testValidatorId = "test-validator-uuid";
        final var fileSystemConnector = Mockito.mock(FileSystemConnector.class);
        final var validationContext = Mockito.mock(CustomValidationContext.class);
        final var dynamicParametersTriggerInvocationHandlerSupplier =
            Mockito.mock(DynamicParametersTriggerInvocationHandlerContext.class);
        Mockito.when(validationContext.registerValidator(ArgumentMatchers.any(ValidationCallback.class),
            ArgumentMatchers.eq(String.class))).thenReturn(testValidatorId);

        final var serviceRegistry = new NodeDialogServiceRegistry(fileSystemConnector, validationContext,
            dynamicParametersTriggerInvocationHandlerSupplier);
        final var response = buildUpdates(Map.of(SettingsType.MODEL, new TestSettings()),
            createDefaultNodeSettingsContext(), serviceRegistry);

        // Even with initially invalid value, we only send the validator ID
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo(testValidatorId);
    }

    @Test
    void testCustomValidationWithDependencies() {

        class TestSettings implements NodeParameters {

            static final class DependentRef implements ParameterReference<Boolean> {
            }

            static final class ConditionalValidator implements CustomValidationProvider<String> {

                private Supplier<Boolean> m_enabledSupplier;

                @Override
                public void init(final StateProviderInitializer initializer) {
                    m_enabledSupplier = initializer.computeFromValueSupplier(DependentRef.class);
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public ValidationCallback<String> computeValidationCallback(final NodeParametersInput context) {
                    return value -> {
                        if (m_enabledSupplier.get() && (value == null || value.length() < 5)) {
                            throw new InvalidSettingsException("Must be at least 5 characters");
                        }
                    };
                }
            }

            @Widget(title = "", description = "")
            @ValueReference(DependentRef.class)
            boolean m_enableValidation = true;

            @Widget(title = "", description = "")
            @CustomValidation(ConditionalValidator.class)
            String m_field = "abc"; // Too short when validation enabled
        }

        final var testValidatorId = "test-validator-uuid";
        final var fileSystemConnector = Mockito.mock(FileSystemConnector.class);
        final var validationContext = Mockito.mock(CustomValidationContext.class);
        final var dynamicParametersTriggerInvocationHandlerSupplier =
            Mockito.mock(DynamicParametersTriggerInvocationHandlerContext.class);
        Mockito.when(validationContext.registerValidator(ArgumentMatchers.any(ValidationCallback.class),
            ArgumentMatchers.eq(String.class))).thenReturn(testValidatorId);

        final var serviceRegistry = new NodeDialogServiceRegistry(fileSystemConnector, validationContext,
            dynamicParametersTriggerInvocationHandlerSupplier);
        final var response = buildUpdates(Map.of(SettingsType.MODEL, new TestSettings()),
            createDefaultNodeSettingsContext(), serviceRegistry);

        // We only send the validator ID, not the initial message
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo(testValidatorId);
        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSizeGreaterThanOrEqualTo(1);
        // Verify there's a global update triggered by m_enableValidation change
        assertThatJson(response)
            .inPath("$.globalUpdates[?(@.trigger.scope == '#/properties/model/properties/enableValidation')]").isArray()
            .hasSizeGreaterThanOrEqualTo(1);
    }

    static class MyDynamicParameters implements TestDynamicParams {

        static final class TestStringChoicesProvider implements StringChoicesProvider {

            @Override
            public List<String> choices(final NodeParametersInput context) {
                return List.of("james", "bond");
            }
        }

        @ChoicesProvider(TestStringChoicesProvider.class)
        @Widget(title = "Dynamic Field", description = "A dynamic field with choices")
        String dynamicField = "dynamicValue";

    }

    @Test
    void testDynamicSettingsWidgetIsIntercepted() {

        class TestSettings implements NodeParameters {
            static class TestDataAndDialogProvider implements DynamicParametersProvider<TestDynamicParams> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public ClassIdStrategy<TestDynamicParams> getClassIdStrategy() {
                    return new DefaultClassIdStrategy<>(List.of(MyDynamicParameters.class));
                }

                @Override
                public TestDynamicParams computeParameters(final NodeParametersInput parametersInput)
                    throws StateComputationFailureException {
                    return new MyDynamicParameters();
                }
            }

            @DynamicParameters(TestDataAndDialogProvider.class)
            TestDynamicParams m_dynamicSettings;
        }

        final var settingsId = "mySettingsId";
        final var fileSystemConnector = Mockito.mock(FileSystemConnector.class);
        final var validationContext = Mockito.mock(CustomValidationContext.class);
        final var dynamicParametersContext = Mockito.mock(DynamicParametersTriggerInvocationHandlerContext.class);
        Mockito.when(dynamicParametersContext.registerTriggerInvocationHandler(ArgumentMatchers.any()))
            .thenReturn(settingsId);

        final var serviceRegistry =
            new NodeDialogServiceRegistry(fileSystemConnector, validationContext, dynamicParametersContext);

        final var response = buildUpdates(Map.of(SettingsType.MODEL, new TestSettings()),
            createDefaultNodeSettingsContext(), serviceRegistry);

        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
            .isEqualTo(JsonFormsConsts.UiSchema.TAG_DYNAMIC_SETTINGS);
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value.settingsId").isString()
            .isEqualTo(settingsId);
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value.updates").isString().contains("james")
            .contains("bond");

        Mockito.verify(dynamicParametersContext).registerTriggerInvocationHandler(ArgumentMatchers.any());
    }

}
