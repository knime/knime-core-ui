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
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.knime.core.node.Node;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataServiceContextTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil.UpdateResult;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeStringFormatValidation;

import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Tests DefaultNodeSettingsService.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class DefaultNodeDialogDataServiceImplTest {

    static class TestDefaultNodeSettings implements DefaultNodeSettings {
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
        getDataService(final Class<? extends DefaultNodeSettings> modelSettingsClass) {
        return new DefaultNodeDialogDataServiceImpl(Map.of(SettingsType.MODEL, modelSettingsClass));
    }

    private static DefaultNodeDialogDataServiceImpl getDataService(
        final Class<? extends DefaultNodeSettings> modelSettingsClass,
        final Class<? extends DefaultNodeSettings> viewSettingsClass) {
        return new DefaultNodeDialogDataServiceImpl(
            Map.of(SettingsType.MODEL, modelSettingsClass, SettingsType.VIEW, viewSettingsClass));

    }

    @Nested
    class UpdatesDataServiceTest {

        static final class MyValueRef implements Reference<String> {
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
            public String computeState(final DefaultNodeSettingsContext context) {
                return m_dependencySupplier.get();
            }

        }

        @Test
        void testSingleUpdate() throws ExecutionException, InterruptedException {

            class UpdateSettings implements DefaultNodeSettings {

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
            final var result = (List<UpdateResult<String>>)(resultWrapper.result());
            assertThat(result).hasSize(1);
            assertThat(((TextNode)result.get(0).values().get(0).value()).textValue())
                .isEqualTo(testDepenenciesFooValue);
            assertThat(result.get(0).scope()).isEqualTo("#/properties/model/properties/updatedWidget");
        }

        @Test
        void testUiStateProvider() throws ExecutionException, InterruptedException {

            class UpdateSettings implements DefaultNodeSettings {

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
                    public String computeState(final DefaultNodeSettingsContext context) {
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
            final var result = (List<UpdateResult<String>>)(resultWrapper.result());
            assertThat(result).hasSize(1);
            assertThat(result.get(0).values().get(0).value()).isEqualTo(testDepenencyValue);
            assertThat(result.get(0).scope()).isNull();
            assertThat(result.get(0).id()).isEqualTo(UpdateSettings.MyFileExtensionProvider.class.getName());
        }

        @Test
        void testAbortUiStateProviderExecution() throws ExecutionException, InterruptedException {

            class UpdateSettings implements DefaultNodeSettings {

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
                    public String computeState(final DefaultNodeSettingsContext context)
                        throws StateComputationFailureException {
                        final var value = m_valueSupplier.get();
                        if (value.contains("throw")) {
                            throw new StateComputationFailureException("Value must not contain throw");
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
                    public String computeState(final DefaultNodeSettingsContext context) {
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
            var result = (List<UpdateResult<String>>)(resultWrapper.result());
            assertThat(result).hasSize(2);

            assertThat(((TextNode)result.get(0).values().get(0).value()).textValue())
                .isEqualTo(testWorkingDependencyValue);
            assertThat(result.get(0).scope()).isEqualTo("#/properties/model/properties/valueUpdateField");
            assertThat(result.get(0).id()).isNull();

            assertThat(result.get(1).values().get(0).value()).isEqualTo(testWorkingDependencyValue);
            assertThat(result.get(1).scope()).isNull();
            assertThat(result.get(1).id()).isEqualTo(UpdateSettings.MyPlaceholderProvider.class.getName());

            final String testThrowingDependencyValue = "throw";
            final var testThrowingDependency =
                List.of(new IndexedValue<String>(List.of(), testThrowingDependencyValue));
            resultWrapper =
                dataService.update2("widgetId", valueRefTrigger, Map.of(valueRefScope, testThrowingDependency));
            result = (List<UpdateResult<String>>)(resultWrapper.result());
            assertThat(result).hasSize(0);
        }

        static final class MyFirstValueRef implements Reference<String> {
        }

        static final class MySecondValueRef implements Reference<String> {
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
            public CommonFirstState computeState(final DefaultNodeSettingsContext context) {
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
            public String computeState(final DefaultNodeSettingsContext context) {
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
            public String computeState(final DefaultNodeSettingsContext context) {
                return m_pairProvider.get().second();
            }
        }

        @Test
        void testMultipleUpdatesWithOneHandler() throws ExecutionException, InterruptedException {

            class UpdateSettings implements DefaultNodeSettings {

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
            final var result = (List<UpdateResult<String>>)(resultWrapper.result());
            assertThat(result).hasSize(2);
            final var first = result.get(0);
            assertThat(((TextNode)first.values().get(0).value()).textValue())
                .isEqualTo(testDependenciesFooValue + "_first");
            assertThat(first.scope()).isEqualTo("#/properties/model/properties/firstUpdatedWidget");
            final var second = result.get(1);
            assertThat(((TextNode)second.values().get(0).value()).textValue())
                .isEqualTo(testDepenenciesBarValue + "_second");
            assertThat(second.scope()).isEqualTo("#/properties/model/properties/secondUpdatedWidget");
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
                final DefaultNodeSettingsContext context) {
                return new ButtonChange<>(currentValue, TestButtonStates.FIRST);

            }

            @Override
            public ButtonChange<String, TestButtonStates> invoke(final TestButtonStates state,
                final TestDefaultNodeSettings settings, final DefaultNodeSettingsContext context) {
                return new ButtonChange<>(settings.m_foo, state);
            }

        }

        static class GenericTypesUpdateHandler extends IntermediateSuperUpdateHandler<TestDefaultNodeSettings, String> {

            @Override
            public ButtonChange<String, TestButtonStates> update(final TestDefaultNodeSettings settings,
                final DefaultNodeSettingsContext context) throws WidgetHandlerException {
                return new ButtonChange<>(settings.m_foo, TestButtonStates.SECOND);
            }

        }

        @Test
        void testInitializeButton() throws ExecutionException, InterruptedException {

            class ButtonSettings implements DefaultNodeSettings {
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

            class ButtonSettings implements DefaultNodeSettings {
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

        static class ButtonAndCredentialsSettings implements DefaultNodeSettings {

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
                final DefaultNodeSettingsContext context) throws WidgetHandlerException {
                return null;
            }

            @Override
            public ButtonChange<String, TestButtonStates> invoke(final TestButtonStates state,
                final ButtonAndCredentialsSettings settings, final DefaultNodeSettingsContext context)
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

            class ButtonSettings implements DefaultNodeSettings {
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

            class DateTimeFormatPickerSettings implements DefaultNodeSettings {
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
    }

}
