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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.DataServiceContextTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.NodeDialogServiceRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.validation.CustomValidationContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil.UpdateResult;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeStringFormatValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.text.TextInputWidget;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("java:S2698") // we accept assertions without messages
class DefaultNodeDialogDataServiceImplTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static class TestDefaultNodeSettings implements NodeParameters {
        @Widget(title = "", description = "")
        String m_foo = "bar";
    }

    final static PortObjectSpec[] PORT_OBJECT_SPECS = new PortObjectSpec[0];

    @BeforeAll
    static void initDataServiceContext() {
        DataServiceContextTest.initDataServiceContext(null, () -> PORT_OBJECT_SPECS);
    }

    @AfterAll
    static void removeDataServiceContext() {
        DataServiceContextTest.removeDataServiceContext();
    }

    private static DefaultNodeDialogDataServiceImpl
        getDataService(final Class<? extends NodeParameters> modelSettingsClass) {
        return new DefaultNodeDialogDataServiceImpl(Map.of(SettingsType.MODEL, modelSettingsClass), null);
    }

    private static Pair<DefaultNodeDialogDataServiceImpl, NodeDialogServiceRegistry>
        getDataServiceWithRegistry(final Class<? extends NodeParameters> modelSettingsClass) {
        final var fileSystemConnector = new FileSystemConnector();
        final var validationContext = new CustomValidationContext();
        final var registry = new NodeDialogServiceRegistry(fileSystemConnector, validationContext);
        return new Pair<>(
            new DefaultNodeDialogDataServiceImpl(Map.of(SettingsType.MODEL, modelSettingsClass), registry), registry);
    }

    @Nested
    class UpdatesDataServiceTest {

        static final class MyValueRef implements ParameterReference<String> {
        }

        private static final class TestStateProvider implements StateProvider<String> {

            private Supplier<String> m_dependencySupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_dependencySupplier = initializer.computeFromValueSupplier(MyValueRef.class);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String computeState(final NodeParametersInput context) {
                return m_dependencySupplier.get();
            }

        }

        @Test
        void testSingleUpdate() throws ExecutionException, InterruptedException {

            class UpdateSettings implements NodeParameters {

                @Widget(title = "", description = "")
                @ValueReference(MyValueRef.class)
                String m_dependency;

                @Widget(title = "", description = "")
                @ValueProvider(TestStateProvider.class)
                String m_updatedWidget;

            }

            final String testDepenenciesFooValue = "custom value";
            final var testDependencyFoo = List.of(new IndexedValue<String>(List.of(), testDepenenciesFooValue));
            final var dataService = getDataService(UpdateSettings.class);
            final var valueRefScope = "#/properties/model/properties/dependency";
            final var valueRefTrigger = new Trigger.ValueTrigger(valueRefScope);
            final var resultWrapper =
                dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testDependencyFoo));
            final var result = MAPPER.valueToTree(resultWrapper.result());
            assertThatJson(result).inPath("$").isArray().hasSize(1);
            assertThatJson(result).inPath("$[0].scope").isEqualTo("#/properties/model/properties/updatedWidget");
            assertThatJson(result).inPath("$[0].values[0].value").isEqualTo(testDepenenciesFooValue);
        }

        @Test
        void testUiStateProvider() throws ExecutionException, InterruptedException {

            class UpdateSettings implements NodeParameters {

                @Widget(title = "", description = "")
                @ValueReference(MyValueRef.class)
                String m_dependency;

                static final class MyFileExtensionProvider implements StateProvider<String> {

                    private Supplier<String> m_valueSupplier;

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        m_valueSupplier = initializer.computeFromValueSupplier(MyValueRef.class);

                    }

                    @Override
                    public String computeState(final NodeParametersInput context) {
                        return m_valueSupplier.get();
                    }

                }

                @LocalFileWriterWidget(fileExtensionProvider = MyFileExtensionProvider.class)
                String m_updatedWidget;

            }

            final String testDepenencyValue = "custom value";
            final var testDependency = List.of(new IndexedValue<String>(List.of(), testDepenencyValue));

            final var dataService = getDataService(UpdateSettings.class);
            final var valueRefScope = "#/properties/model/properties/dependency";
            final var valueRefTrigger = new Trigger.ValueTrigger(valueRefScope);
            final var resultWrapper =
                dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testDependency));
            final var result = MAPPER.valueToTree(resultWrapper.result());

            assertThatJson(result).inPath("$").isArray().hasSize(1);
            assertThatJson(result).inPath("$[0].scope").isEqualTo("#/properties/model/properties/updatedWidget");
            assertThatJson(result).inPath("$[0].values[0].value").isEqualTo(testDepenencyValue);
            assertThatJson(result).inPath("$[0].providedOptionName").isEqualTo("fileExtension");
        }

        @Test
        void testAbortUiStateProviderExecution() throws ExecutionException, InterruptedException {

            class UpdateSettings implements NodeParameters {

                @Widget(title = "", description = "")
                @ValueReference(MyValueRef.class)
                String m_reference;

                static final class ThrowingValueProvider implements StateProvider<String> {

                    private Supplier<String> m_valueSupplier;

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        m_valueSupplier = initializer.computeFromValueSupplier(MyValueRef.class);

                    }

                    @Override
                    public String computeState(final NodeParametersInput context)
                        throws StateComputationFailureException {
                        final var value = m_valueSupplier.get();
                        if (value.contains("throw")) {
                            throw new StateComputationFailureException();
                        }
                        return m_valueSupplier.get();
                    }

                }

                @Widget(title = "", description = "")
                @ValueProvider(ThrowingValueProvider.class)
                String m_valueUpdateField;

                static final class MyPlaceholderProvider implements StateProvider<String> {

                    private Supplier<String> valueSupplier;

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeBeforeOpenDialog();
                        valueSupplier = initializer.computeFromProvidedState(ThrowingValueProvider.class);
                    }

                    @Override
                    public String computeState(final NodeParametersInput context) {
                        return String.format("%s", valueSupplier.get());
                    }
                }

                @Widget(title = "Test3", description = "")
                @TextInputWidget(placeholderProvider = MyPlaceholderProvider.class)
                public String m_otherUpdateField;

            }
            final var dataService = getDataService(UpdateSettings.class);

            final String testWorkingDependencyValue = "run";
            final var testWorkingDependency = List.of(new IndexedValue<String>(List.of(), testWorkingDependencyValue));

            final var valueRefScope = "#/properties/model/properties/reference";
            final var valueRefTrigger = new Trigger.ValueTrigger(valueRefScope);

            var resultWrapper =
                dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testWorkingDependency));
            var result = MAPPER.valueToTree(resultWrapper.result());

            assertThatJson(result).inPath("$").isArray().hasSize(2);
            assertThatJson(result).inPath("$[0].scope").isEqualTo("#/properties/model/properties/otherUpdateField");
            assertThatJson(result).inPath("$[0].values[0].value").isEqualTo(testWorkingDependencyValue);
            assertThatJson(result).inPath("$[0].providedOptionName").isEqualTo("placeholder");
            assertThatJson(result).inPath("$[1].scope").isEqualTo("#/properties/model/properties/valueUpdateField");
            assertThatJson(result).inPath("$[1].values[0].value").isEqualTo(testWorkingDependencyValue);

            final String testThrowingDependencyValue = "throw";
            final var testThrowingDependency =
                List.of(new IndexedValue<String>(List.of(), testThrowingDependencyValue));
            resultWrapper =
                dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testThrowingDependency));
            assertThat((List<UpdateResult>)(resultWrapper.result())).hasSize(0);
        }

        static final class MyFirstValueRef implements ParameterReference<String> {
        }

        static final class MySecondValueRef implements ParameterReference<String> {
        }

        record CommonFirstState(String first, String second) {
        }

        private static final class CommonFirstStateProvider implements StateProvider<CommonFirstState> {

            private Supplier<String> m_secondDependencyProvider;

            private Supplier<String> m_firstDependencyProvider;

            /**
             * {@inheritDoc}
             */
            @Override
            public void init(final StateProviderInitializer initializer) {
                m_firstDependencyProvider = initializer.computeFromValueSupplier(MyFirstValueRef.class);
                m_secondDependencyProvider = initializer.getValueSupplier(MySecondValueRef.class);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public CommonFirstState computeState(final NodeParametersInput context) {
                return new CommonFirstState(m_firstDependencyProvider.get() + "_first",
                    m_secondDependencyProvider.get() + "_second");
            }

        }

        static final class FirstResolver implements StateProvider<String> {

            private Supplier<CommonFirstState> m_pairProvider;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_pairProvider = initializer.computeFromProvidedState(CommonFirstStateProvider.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                return m_pairProvider.get().first();
            }

        }

        static final class SecondResolver implements StateProvider<String> {
            private Supplier<CommonFirstState> m_pairProvider;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_pairProvider = initializer.computeFromProvidedState(CommonFirstStateProvider.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                return m_pairProvider.get().second();
            }
        }

        @Test
        void testMultipleUpdatesWithOneHandler() throws ExecutionException, InterruptedException {

            class UpdateSettings implements NodeParameters {

                @Widget(title = "", description = "")
                @ValueReference(MyFirstValueRef.class)
                String m_foo;

                @Widget(title = "", description = "")
                @ValueReference(MySecondValueRef.class)
                String m_bar;

                @Widget(title = "", description = "")
                @ValueProvider(FirstResolver.class)
                String m_firstUpdatedWidget;

                @Widget(title = "", description = "")
                @ValueProvider(SecondResolver.class)
                String m_secondUpdatedWidget;

            }

            final String testDependenciesFooValue = "custom value 1";
            final var testDependenciesFoo = List.of(new IndexedValue<String>(List.of(), testDependenciesFooValue));
            final String testDepenenciesBarValue = "custom value 2";
            final var testDepenenciesBar = List.of(new IndexedValue<String>(List.of(), testDepenenciesBarValue));

            final var dataService = getDataService(UpdateSettings.class);
            final var myFirstValueRefScope = "#/properties/model/properties/foo";
            final var mySecondValueRefScope = "#/properties/model/properties/bar";
            final var myFirstValueRefTrigger = new Trigger.ValueTrigger(myFirstValueRefScope);
            final var resultWrapper = dataService.update2("widgetId", myFirstValueRefTrigger,
                Map.of(myFirstValueRefScope, testDependenciesFoo, mySecondValueRefScope, testDepenenciesBar));
            final var result = MAPPER.valueToTree(resultWrapper.result());

            assertThatJson(result).inPath("$").isArray().hasSize(2);
            assertThatJson(result).inPath("$[0].scope").isEqualTo("#/properties/model/properties/firstUpdatedWidget");
            assertThatJson(result).inPath("$[0].values[0].value").isEqualTo(testDependenciesFooValue + "_first");
            assertThatJson(result).inPath("$[1].scope").isEqualTo("#/properties/model/properties/secondUpdatedWidget");
            assertThatJson(result).inPath("$[1].values[0].value").isEqualTo(testDepenenciesBarValue + "_second");

        }
    }

    @Nested
    class ButtonDataServiceTest {

        enum TestButtonStates {
                FIRST, SECOND
        }

        abstract static class IntermediateSuperType<A, B> implements ButtonActionHandler<B, A, TestButtonStates> {

            @Override
            public Class<TestButtonStates> getStateMachine() {
                return TestButtonStates.class;
            }

        }

        abstract static class IntermediateSuperUpdateHandler<A, B>
            implements ButtonUpdateHandler<B, A, TestButtonStates> {

        }

        static class GenericTypesTestHandler extends IntermediateSuperType<TestDefaultNodeSettings, String> {

            @Override
            public ButtonChange<String, TestButtonStates> initialize(final String currentValue,
                final NodeParametersInput context) {
                return new ButtonChange<>(currentValue, TestButtonStates.FIRST);

            }

            @Override
            public ButtonChange<String, TestButtonStates> invoke(final TestButtonStates state,
                final TestDefaultNodeSettings settings, final NodeParametersInput context) {
                return new ButtonChange<>(settings.m_foo, state);
            }

        }

        static class GenericTypesUpdateHandler extends IntermediateSuperUpdateHandler<TestDefaultNodeSettings, String> {

            @Override
            public ButtonChange<String, TestButtonStates> update(final TestDefaultNodeSettings settings,
                final NodeParametersInput context) throws WidgetHandlerException {
                return new ButtonChange<>(settings.m_foo, TestButtonStates.SECOND);
            }

        }

        @Test
        void testInitializeButton() throws ExecutionException, InterruptedException {

            class ButtonSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = GenericTypesTestHandler.class)
                String m_button;
            }

            final var dataService = getDataService(ButtonSettings.class);
            final String currentState = "currentState";
            final var result =
                dataService.initializeButton("widgetId", GenericTypesTestHandler.class.getName(), currentState);
            @SuppressWarnings("unchecked")
            final var buttonChange = (ButtonChange<String, TestButtonStates>)result.result();
            assertThat(buttonChange.settingValue()).isEqualTo(currentState);
        }

        @Test
        void testInvokeButtonAction() throws ExecutionException, InterruptedException {

            class ButtonSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = GenericTypesTestHandler.class)
                String m_button;
            }

            final var testDepenenciesFooValue = "custom value";
            final var dataService = getDataService(ButtonSettings.class);
            final var result = dataService.invokeButtonAction("widgetId", GenericTypesTestHandler.class.getName(),
                "FIRST", Map.of("foo", testDepenenciesFooValue));
            @SuppressWarnings("unchecked")
            final var buttonChange = (ButtonChange<String, TestButtonStates>)result.result();
            assertThat(buttonChange.buttonState()).isEqualTo(TestButtonStates.FIRST);
            assertThat(buttonChange.settingValue()).isEqualTo(testDepenenciesFooValue);
        }

        static class ButtonAndCredentialsSettings implements NodeParameters {

            Credentials m_credentials;

            @Widget(title = "", description = "")
            @ButtonWidget(actionHandler = CredentialsButtonTestHandler.class)
            String m_button;
        }

        static class CredentialsButtonTestHandler
            implements ButtonActionHandler<String, ButtonAndCredentialsSettings, TestButtonStates> {

            static String EXPECTED_PASSWORD = "myFlowVarPassword";

            @Override
            public ButtonChange<String, TestButtonStates> initialize(final String currentValue,
                final NodeParametersInput context) throws WidgetHandlerException {
                return null;
            }

            @Override
            public ButtonChange<String, TestButtonStates> invoke(final TestButtonStates state,
                final ButtonAndCredentialsSettings settings, final NodeParametersInput context)
                throws WidgetHandlerException {
                assertThat(settings.m_credentials.getPassword()).isEqualTo(EXPECTED_PASSWORD);
                return new ButtonChange<>(TestButtonStates.FIRST);
            }

            @Override
            public Class<TestButtonStates> getStateMachine() {
                return TestButtonStates.class;
            }

        }

        @Test
        void testInvokeButtonActionWithCredentialsDependencies() throws ExecutionException, InterruptedException {

            String flowVarName = "myFlowVariable";

            final var nodeContainer = mock(NativeNodeContainer.class);
            final var credentialsProvider = mockCredentialsProvider(nodeContainer);
            mockPasswordResult(CredentialsButtonTestHandler.EXPECTED_PASSWORD, credentialsProvider);
            final var dataService = getDataService(ButtonAndCredentialsSettings.class);

            NodeContext.pushContext(nodeContainer);
            try {
                dataService.invokeButtonAction("widgetId", CredentialsButtonTestHandler.class.getName(), "FIRST",
                    Map.of("credentials", Map.of("flowVariableName", flowVarName), //
                        "button", "buttonValue"));

            } finally {
                NodeContext.removeLastContext();
            }

            verify(credentialsProvider).get(flowVarName);
        }

        private void mockPasswordResult(final String credentialsFlowVariablePassword,
            final CredentialsProvider credentialsProvider) {
            final var iCredentials = createICredentials(credentialsFlowVariablePassword);
            when(credentialsProvider.get(anyString())).thenReturn(iCredentials);
        }

        private CredentialsProvider mockCredentialsProvider(final NativeNodeContainer nodeContainer) {
            final var node = mock(Node.class);
            when(nodeContainer.getNode()).thenReturn(node);
            final var credentialsProvider = mock(CredentialsProvider.class);
            when(node.getCredentialsProvider()).thenReturn(credentialsProvider);
            return credentialsProvider;
        }

        private static ICredentials createICredentials(final String password) {
            final var iCredentials = new ICredentials() {

                @Override
                public String getPassword() {
                    return password;
                }

                @Override
                public String getName() {
                    return null;
                }

                @Override
                public String getLogin() {
                    return null;
                }
            };
            return iCredentials;
        }

        @Test
        void testUpdate() throws ExecutionException, InterruptedException {

            class ButtonSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @ButtonWidget(actionHandler = GenericTypesTestHandler.class,
                    updateHandler = GenericTypesUpdateHandler.class)
                String m_button;
            }

            final var testDepenenciesFooValue = "custom value";
            final var dataService = getDataService(ButtonSettings.class);
            final var result = dataService.update("widgetId", GenericTypesUpdateHandler.class.getName(),
                Map.of("foo", testDepenenciesFooValue));
            @SuppressWarnings("unchecked")
            final var buttonChange = (ButtonChange<String, TestButtonStates>)result.result();
            assertThat(buttonChange.settingValue()).isEqualTo(testDepenenciesFooValue);
        }
    }

    @Nested
    class ValidationDataServiceTest {

        @Test
        void testValidationExecution() throws ExecutionException, InterruptedException {

            class DateTimeFormatPickerSettings implements NodeParameters {
                @Widget(title = "", description = "")
                @DateTimeFormatPickerWidget
                String m_dateTimeFormat;
            }

            final var dataService = getDataService(DateTimeFormatPickerSettings.class);
            final var resultValidFormat =
                dataService.performExternalValidation(DateTimeStringFormatValidation.class.getName(), "MM/DD/YYYY");
            assertThat(resultValidFormat.result()).isEmpty();

            final var resultInvalidFormat =
                dataService.performExternalValidation(DateTimeStringFormatValidation.class.getName(), "MM/DDDD/YYYY");
            assertThat(resultInvalidFormat.result().get()).isEqualTo("Invalid format: Too many pattern letters: D");
        }

        static final ValidationCallback<String> VALIDATION_CALLBACK = value -> {
            if (value == null || value.isEmpty()) {
                throw new InvalidSettingsException("Field cannot be empty");
            }
        };

        @Test
        void testCustomValidationWithValidInput() throws ExecutionException, InterruptedException {

            class CustomValidationSettings implements NodeParameters {

                static final class TestValidationProvider implements StateProvider<ValidationCallback<String>> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeBeforeOpenDialog();
                    }

                    @Override
                    public ValidationCallback<String> computeState(final NodeParametersInput context) {
                        return VALIDATION_CALLBACK;
                    }
                }

                @Widget(title = "", description = "")
                @CustomValidation(TestValidationProvider.class)
                String m_validatedField;
            }

            final var dataServiceAndRegistry = getDataServiceWithRegistry(CustomValidationSettings.class);
            final var dataService = dataServiceAndRegistry.getFirst();
            final var registry = dataServiceAndRegistry.getSecond();

            final var callback = VALIDATION_CALLBACK;
            final var scope = "#/properties/model/properties/validatedField";
            final var indices = List.<Integer> of();
            final var validatorId = registry.customValidationContext().registerValidator(callback);

            // Test with valid input
            final var result = dataService.performCustomValidation(validatorId, "valid input");

            assertThat(result.result()).isEmpty(); // No error
        }

        @Test
        void testCustomValidationWithInvalidInput() throws ExecutionException, InterruptedException {

            class CustomValidationSettings implements NodeParameters {

                static final class TestValidationProvider implements StateProvider<ValidationCallback<String>> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        initializer.computeBeforeOpenDialog();
                    }

                    @Override
                    public ValidationCallback<String> computeState(final NodeParametersInput context) {
                        return VALIDATION_CALLBACK;
                    }
                }

                @Widget(title = "", description = "")
                @CustomValidation(TestValidationProvider.class)
                String m_validatedField;
            }

            final var dataServiceAndRegistry = getDataServiceWithRegistry(CustomValidationSettings.class);
            final var dataService = dataServiceAndRegistry.getFirst();
            final var registry = dataServiceAndRegistry.getSecond();

            final var callback = VALIDATION_CALLBACK;
            final var scope = "#/properties/model/properties/validatedField";
            final var indices = List.<Integer> of();
            final var validatorId = registry.customValidationContext().registerValidator(callback);

            // Test with invalid input (empty)
            final var result = dataService.performCustomValidation(validatorId, "");

            assertThat(result.result().get()).isEqualTo("Field cannot be empty");
        }

        @Test
        void testCustomValidationWithUnknownId() {

            class CustomValidationSettings implements NodeParameters {
                @Widget(title = "", description = "")
                String m_validatedField;
            }

            final var dataService = getDataServiceWithRegistry(CustomValidationSettings.class).getFirst();

            assertThat(
                assertThrows(ExecutionException.class, () -> dataService.performCustomValidation("unknown-id", "value"))
                    .getMessage()).contains("No validator found for id unknown-id");
        }
    }

}
