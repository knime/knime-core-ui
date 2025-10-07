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
 *   Aug 28, 2023 (hornm): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.webui.node.dialog.SettingsType.MODEL;
import static org.knime.core.webui.node.dialog.SettingsType.VIEW;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ModelSettings.MODEL_SETTING;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ModelSettings.MODEL_SETTING_CFG;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ModelSettings.MODEL_SETTING_INVALID_VALUE;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.DEFAULT_VIEW_SETTING;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.NESTED_VIEW_SETTING_CFG_PATH;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.NESTED_VIEW_SETTING_CFG_PATH_2;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.NESTED_VIEW_SETTING_PATH;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.OBJECT_SETTING;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.VIEW_SETTING;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.VIEW_SETTING_CFG;
import static org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogTest.ViewSettings.VIEW_SETTING_INVALID_VALUE;
import static org.knime.testing.node.dialog.NodeDialogNodeModel.VALIDATION_ERROR_MESSAGE;
import static org.knime.testing.node.ui.NodeDialogTestUtil.createNodeDialog;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.NodeDialogManagerTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.utils.AbstractSettingsDocumentBuilder;
import org.knime.core.webui.node.dialog.utils.FlowVariablesInputNodeFactory;
import org.knime.core.webui.page.Page;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Integration tests for {@link DefaultNodeDialog}-related logic.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class DefaultNodeDialogTest {


    static class ModelSettings implements NodeParameters {

        static final String MODEL_SETTING_CFG = NodeDialogNodeModel.VALIDATED_MODEL_SETTING_CFG;

        static final String MODEL_SETTING = "modelSetting";

        /**
         * When this value is set for m_modelSetting, model validation will fail (see
         * {@link NodeDialogNodeModel#validateSettings}).
         */
        static final String MODEL_SETTING_INVALID_VALUE = NodeDialogNodeModel.INVALID_VALUE;

        static final String DEFAULT_MODEL_SETTING = "1";

        @Widget(title = "", description = "")
        @Persist(configKey = MODEL_SETTING_CFG)
        String m_modelSetting = DEFAULT_MODEL_SETTING;

    }

    static class ViewSettings implements NodeParameters {

        enum MyEnum {
                A, B, C
        }

        static final String VIEW_SETTING_CFG = "view setting";

        static final String VIEW_SETTING = "viewSetting";

        static final String VIEW_SETTING_INVALID_VALUE = "NOT_A_B_OR_C";

        static final MyEnum DEFAULT_VIEW_SETTING = MyEnum.A;

        @Widget(title = "", description = "")
        @Persist(configKey = VIEW_SETTING_CFG)
        MyEnum m_viewSetting = DEFAULT_VIEW_SETTING;

        private static final String NESTED = "nestedViewSetting";

        private static final String NESTED_CFG = "nested";

        static final List<String> NESTED_VIEW_SETTING_CFG_PATH = List.of(//
            NESTED_CFG, //
            NestedViewSettings.NESTED_VIEW_SETTING_CFG//
        );

        static final List<String> NESTED_VIEW_SETTING_PATH = List.of(//
            NESTED, //
            NestedViewSettings.NESTED_VIEW_SETTING//
        );

        static final List<String> NESTED_VIEW_SETTING_CFG_PATH_2 = List.of(//
            NESTED_CFG, //
            NestedViewSettings.NESTED_VIEW_SETTING_2_CFG//
        );

        static final List<String> NESTED_VIEW_SETTING_PATH_2 = List.of(//
            NESTED, //
            NestedViewSettings.NESTED_VIEW_SETTING_2//
        );

        @Widget(title = "", description = "")
        @Persist(configKey = NESTED_CFG)
        NestedViewSettings m_nestedViewSetting = new NestedViewSettings();

        static final String OBJECT_SETTING = "objectSetting";

        @Persist(configKey = OBJECT_SETTING)
        Credentials m_objectSetting = new Credentials();
    }

    static class NestedViewSettings implements NodeParameters {
        static final String NESTED_VIEW_SETTING_CFG = "nested view setting";

        static final String NESTED_VIEW_SETTING = "nestedViewSettings";

        @Widget(title = "", description = "")
        @Persist(configKey = NESTED_VIEW_SETTING_CFG)
        String m_nestedViewSettings = "3";

        static final String NESTED_VIEW_SETTING_2_CFG = "nested view setting 2";

        static final String NESTED_VIEW_SETTING_2 = "nestedViewSettings2";

        @Widget(title = "", description = "")
        @Persist(configKey = NESTED_VIEW_SETTING_2_CFG)
        String m_nestedViewSettings2 = "4";

    }

    private WorkflowManager m_wfm;

    private NativeNodeContainer m_nnc;

    private NativeNodeContainer m_previousNode;

    @BeforeEach
    void createWorkflowAndAddNode() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();

        final var settingsClasses =
            Map.of(SettingsType.MODEL, ModelSettings.class, SettingsType.VIEW, ViewSettings.class);
        final var defaultNodeSettingsService = new DefaultNodeSettingsService(settingsClasses, null);
        Supplier<NodeDialog> nodeDialogCreator =
            () -> createNodeDialog(Page.create().fromString(() -> "page content").relativePath("page.html"),
                defaultNodeSettingsService, null);
        m_nnc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm, nodeDialogCreator);

        m_previousNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new FlowVariablesInputNodeFactory());
        m_wfm.addConnection(m_previousNode.getID(), 0, m_nnc.getID(), 0);
        m_wfm.executeAllAndWaitUntilDone();
        NodeContext.pushContext(m_nnc);

    }

    @AfterEach
    void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
        // Necessary for the serialization of credentials
        NodeContext.removeLastContext();
    }

    private void makeFlowVariablesAvailable(final FlowVariable... flowVariables) {
        final var flowObjectStack = m_previousNode.getOutgoingFlowObjectStack();
        for (var flowVar : flowVariables) {
            flowObjectStack.push(flowVar);
        }
        m_wfm.resetAndConfigureNode(m_nnc.getID());
        m_wfm.executeAllAndWaitUntilDone();
    }

    static final ObjectMapper TEST_MAPPER = new ObjectMapper();

    @Nested
    class InitialDataTest {

        final static FlowVariable validFlowVariable1 = new FlowVariable("flow variable 1", "foo");

        // "B" is valid, as we overwrite "MyEnum" above
        final static FlowVariable validFlowVariable2 = new FlowVariable("flow variable 2", "B");

        final static FlowVariable validFlowVariable3 = new FlowVariable("flow variable 3", "bar");

        final static String exposedVariable = "exposed var name";

        @Test
        void testFlowVariableOverridingModelSetting() throws JsonProcessingException {

            final var flowVariable = new FlowVariable("flow variable", "foo");
            new TestNodeSettingsBuilder()//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())
                .buildAndSaveTo(m_nnc);
            makeFlowVariablesAvailable(flowVariable);

            var initialData = getInitialData();
            assertFlowVariableSettings(initialData, """
                    {
                      "model.model setting": {
                        "controllingFlowVariableAvailable": true,
                        "controllingFlowVariableFlawed": false,
                        "controllingFlowVariableName": "flow variable"
                      }
                    }
                    """);
            assertStringInData(initialData, MODEL, MODEL_SETTING, flowVariable.getStringValue());
        }

        @Test
        void testFlowVariableOverridingViewSetting() throws JsonProcessingException {

            final var flowVariable = new FlowVariable("flow variable", "B");
            new TestNodeSettingsBuilder()//
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())
                .buildAndSaveTo(m_nnc);
            makeFlowVariablesAvailable(flowVariable);

            var initialData = getInitialData();
            assertFlowVariableSettings(initialData, """
                    {
                      "view.view setting": {
                        "controllingFlowVariableAvailable": true,
                        "controllingFlowVariableFlawed": false,
                        "controllingFlowVariableName": "flow variable"
                      }
                    }
                    """);
            assertStringInData(initialData, VIEW, VIEW_SETTING, flowVariable.getStringValue());
        }

        @Test
        void testExposedFlowVariable() throws JsonProcessingException {
            final var exposedFlowVariableName = "exposed var name";
            new TestNodeSettingsBuilder()//
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.withExposedFlowVariableName(exposedFlowVariableName).build())
                .buildAndSaveTo(m_nnc);
            var initialData = getInitialData();
            assertFlowVariableSettings(initialData, """
                    {
                      "view.view setting": {
                        "exposedFlowVariableName": "exposed var name",
                        "controllingFlowVariableFlawed": false
                      }
                    }
                    """);
        }

        @Test
        void testNestedSettings() throws JsonProcessingException {
            final var flowVariable = new FlowVariable("flow variable", "foo");
            new TestNodeSettingsBuilder()//
                .modifyAt(VIEW, NESTED_VIEW_SETTING_CFG_PATH,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())
                .buildAndSaveTo(m_nnc);
            makeFlowVariablesAvailable(flowVariable);
            var initialData = getInitialData();
            assertFlowVariableSettings(initialData, """
                    {
                      "view.nested.nested view setting": {
                        "controllingFlowVariableAvailable": true,
                        "controllingFlowVariableFlawed": false,
                        "controllingFlowVariableName": "flow variable"
                      }
                    }
                    """);
            assertStringInData(initialData, VIEW, NESTED_VIEW_SETTING_PATH, flowVariable.getStringValue());
        }

        @Test
        void testFlowVariableAtNonExistingSetting() throws JsonProcessingException {
            final var flowVariable = new FlowVariable("flow variable", "foo");
            new TestNodeSettingsBuilder()//
                .modifyAt(VIEW, "nonExistingSetting",
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())
                .modifyAt(VIEW, List.of("non", "existing", "path"),
                    b -> b.withExposedFlowVariableName("exposed").build())
                .buildAndSaveTo(m_nnc);
            makeFlowVariablesAvailable(flowVariable);
            var initialData = getInitialData();
            assertFlowVariableSettings(initialData, """
                    {
                      "view.nonExistingSetting": {
                        "controllingFlowVariableAvailable": true,
                        "controllingFlowVariableFlawed": false,
                        "controllingFlowVariableName": "flow variable"
                      }
                    }
                    """);
        }

        @Test
        void testInitialDataWithInvalidSettings() throws JsonMappingException, JsonProcessingException {
            new TestNodeSettingsBuilder()//
                .withNonDefaultModelSettings(createTestModelSettings())
                .withNonDefaultViewSettings(createTestViewSettings())
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.setStringValue(VIEW_SETTING_INVALID_VALUE).build())//
                .buildAndSaveTo(m_nnc);

            var initialData = getInitialData();

            assertWarningMessages(initialData,
                warning -> assertThatJson(warning).isString().contains("default settings", VIEW_SETTING_INVALID_VALUE));

            assertTestModelSettings(initialData);
            assertStringInData(initialData, VIEW, VIEW_SETTING, DEFAULT_VIEW_SETTING.name());
        }

        @Test
        void testNonAvailableFlowVariable() throws JsonProcessingException {
            final String nonExistingFlowVariable = "nonExistingFlowVariable";
            new TestNodeSettingsBuilder()//
                .withNonDefaultViewSettings(createTestViewSettings()).modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(nonExistingFlowVariable).build())
                .buildAndSaveTo(m_nnc);

            var initialData = getInitialData();

            assertWarningMessages(initialData,
                warning -> assertThatJson(warning).isString().contains("flow variable", nonExistingFlowVariable));
            assertTestViewSettings(initialData);

        }

        @Test
        void testInitialDataWithFlawedFlowVariableSettings() throws InvalidSettingsException, IOException {
            final var inValidFlowVariable = new FlowVariable("flow variable", VIEW_SETTING_INVALID_VALUE);
            new TestNodeSettingsBuilder()//
                .withNonDefaultViewSettings(createTestViewSettings())
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(inValidFlowVariable.getName()).build())//
                .buildAndSaveTo(m_nnc);
            makeFlowVariablesAvailable(inValidFlowVariable);

            var initialData = getInitialData();

            assertWarningMessages(initialData,
                warning -> assertThatJson(warning).isString().contains("flow variable", "NOT_A_B_OR_C"));
            assertTestViewSettings(initialData);
        }

        private static final ViewSettings.MyEnum CUSTOM_VIEW_SETTING_VALUE = ViewSettings.MyEnum.B;

        static final String CUSTOM_MODEL_SETTING_VALUE = "custom model value";

        private static ViewSettings createTestViewSettings() {
            final var viewSettings = new ViewSettings();
            viewSettings.m_viewSetting = CUSTOM_VIEW_SETTING_VALUE;
            return viewSettings;
        }

        private ModelSettings createTestModelSettings() {
            final var modelSettings = new ModelSettings();
            modelSettings.m_modelSetting = CUSTOM_MODEL_SETTING_VALUE;
            return modelSettings;
        }

        private static void assertTestModelSettings(final JsonNode initialData) {
            assertStringInData(initialData, MODEL, MODEL_SETTING, CUSTOM_MODEL_SETTING_VALUE);
        }

        private static void assertTestViewSettings(final JsonNode initialData) {
            assertStringInData(initialData, VIEW, VIEW_SETTING, CUSTOM_VIEW_SETTING_VALUE.name());
        }

        private static void assertFlowVariableSettings(final JsonNode initialData,
            final String expectedFlowVariableSettings) throws JsonMappingException, JsonProcessingException {
            final var actualFlowVariableSettings = initialData.get("result").get("flowVariableSettings");
            assertThatJson(actualFlowVariableSettings).isEqualTo(expectedFlowVariableSettings);
        }

        private static void assertStringInData(final JsonNode initialData, final SettingsType type, final String key,
            final String expectedValue) {
            assertStringInData(initialData, type, List.of(key), expectedValue);
        }

        private static void assertStringInData(final JsonNode initialData, final SettingsType type,
            final List<String> keys, final String expectedValue) {
            final var path = String.join(".", //
                Stream.concat(Stream.of("$", "result", "data", type.getConfigKeyFrontend()), keys.stream()).toList());
            assertThatJson(initialData).inPath(path).isString().isEqualTo(expectedValue);
        }

        private JsonNode getInitialData() throws JsonProcessingException {
            final var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();
            return TEST_MAPPER.readTree(dataServiceManager.callInitialDataService(NodeWrapper.of(m_nnc)));
        }

        @SafeVarargs
        private static void assertWarningMessages(final JsonNode initialData,
            final Consumer<JsonNode>... warningAssertions) throws JsonMappingException, JsonProcessingException {
            final var warningsJson = initialData.get("warningMessages");
            assertThatJson(warningsJson).isArray().hasSameSizeAs(warningAssertions);
            for (int i = 0; i < warningAssertions.length; i++) {
                warningAssertions[i].accept(((ArrayNode)warningsJson).get(i));
            }
        }

    }

    @Nested
    class ApplyDataTest {

        @BeforeEach
        void addValidFlowVariables() {
            // makeFlowVariablesAvailable(validFlowVariable1, validFlowVariable2, validFlowVariable3);
        }

        @Test
        void testApplyData() throws IOException, InvalidSettingsException {

            final var customModelSettings = new ModelSettings();
            customModelSettings.m_modelSetting = "custom model setting";
            final var customViewSettings = new ViewSettings();
            customViewSettings.m_viewSetting = ViewSettings.MyEnum.B;
            customViewSettings.m_nestedViewSetting.m_nestedViewSettings = "custom nested view setting";
            customViewSettings.m_nestedViewSetting.m_nestedViewSettings2 = "custom nested view setting 2";

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(customModelSettings)//
                .withNonDefaultViewSettings(customViewSettings)//
                .build();

            callApplyData(applyData);

            var expectedNodeSettings = new TestNodeSettingsBuilder() //
                .withNonDefaultModelSettings(customModelSettings)//
                .withNonDefaultViewSettings(customViewSettings)//
                .buildNodeSettings();

            for (var key : List.of("model", "view")) {
                assertSubNodeSettingsForKey(expectedNodeSettings, key);
            }

        }

        static final ModelSettings PREVIOUS_MODEL_SETTINGS = createPreviousModelSettings();

        static final ViewSettings PREVIOUS_VIEW_SETTINGS = createPreviousViewSettings();

        static ModelSettings createPreviousModelSettings() {
            final var previousModelSettings = new ModelSettings();
            previousModelSettings.m_modelSetting = "previous model setting";
            return previousModelSettings;
        }

        static ViewSettings createPreviousViewSettings() {
            final var previousViewSettings = new ViewSettings();
            previousViewSettings.m_viewSetting = ViewSettings.MyEnum.B;
            previousViewSettings.m_nestedViewSetting.m_nestedViewSettings = "previous nested view setting";
            previousViewSettings.m_nestedViewSetting.m_nestedViewSettings2 = "previous nested view setting 2";
            return previousViewSettings;
        }

        @BeforeEach
        void savePreviousNodeSettings() {
            new TestNodeSettingsBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .buildAndSaveTo(m_nnc);
        }

        static final ModelSettings APPLIED_MODEL_SETTINGS = createAppliedModelSettings();

        static final ViewSettings APPLIED_VIEW_SETTINGS = createAppliedViewSettings();

        static ModelSettings createAppliedModelSettings() {
            final var appliedModelSettings = new ModelSettings();
            appliedModelSettings.m_modelSetting = "applied model setting";
            return appliedModelSettings;
        }

        static ViewSettings createAppliedViewSettings() {
            final var appliedViewSettings = new ViewSettings();
            appliedViewSettings.m_viewSetting = ViewSettings.MyEnum.C;
            appliedViewSettings.m_nestedViewSetting.m_nestedViewSettings = "applied nested view setting";
            appliedViewSettings.m_nestedViewSetting.m_nestedViewSettings2 = "applied nested view setting 2";
            return appliedViewSettings;
        }

        @Test
        void testApplyFlowVariables() throws IOException, InvalidSettingsException {

            final var flowVariable1 = new FlowVariable("flow variable 1", "flow var model setting");
            final var flowVariable2 = new FlowVariable("flow variable 2", "A");
            final var flowVariable3 = new FlowVariable("flow variable 3", "flow var nested view setting");
            final var exposedVariableName = "exposed var name";

            makeFlowVariablesAvailable(flowVariable1, flowVariable2, flowVariable3);

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(APPLIED_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(APPLIED_VIEW_SETTINGS)//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable1.getName()).build())//
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable2.getName()).build())//
                .modifyAt(VIEW, NESTED_VIEW_SETTING_CFG_PATH,
                    b -> b.withControllingFlowVariableName(flowVariable3.getName()).build())//
                .modifyAt(VIEW, NESTED_VIEW_SETTING_CFG_PATH_2,
                    b -> b.withExposedFlowVariableName(exposedVariableName).build())//
                .build();

            callApplyData(applyData);

            final var expectedModelSettings = new ModelSettings();
            expectedModelSettings.m_modelSetting = PREVIOUS_MODEL_SETTINGS.m_modelSetting;
            final var expectedViewSettings = new ViewSettings();
            expectedViewSettings.m_viewSetting = PREVIOUS_VIEW_SETTINGS.m_viewSetting;
            expectedViewSettings.m_nestedViewSetting.m_nestedViewSettings =
                PREVIOUS_VIEW_SETTINGS.m_nestedViewSetting.m_nestedViewSettings;
            /**
             * Only this value is expected to be the applied one, since no controlling flow variable is set
             */
            expectedViewSettings.m_nestedViewSetting.m_nestedViewSettings2 =
                APPLIED_VIEW_SETTINGS.m_nestedViewSetting.m_nestedViewSettings2;

            var expectedNodeSettings = new TestNodeSettingsBuilder() //
                .withNonDefaultModelSettings(expectedModelSettings)//
                .withNonDefaultViewSettings(expectedViewSettings)//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable1.getName()).build())//
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable2.getName()).build())//
                .modifyAt(VIEW, NESTED_VIEW_SETTING_CFG_PATH,
                    b -> b.withControllingFlowVariableName(flowVariable3.getName()).build())//
                .modifyAt(VIEW, NESTED_VIEW_SETTING_CFG_PATH_2,
                    b -> b.withExposedFlowVariableName(exposedVariableName).build())
                .buildNodeSettings();
            for (var key : List.of("model", "variables", "view", "view_variables")) {
                assertSubNodeSettingsForKey(expectedNodeSettings, key);
            }

        }

        @Test
        void testApplyDataWithOnlyViewChangesStaysExecuted() throws IOException, InvalidSettingsException {

            final var flowVariable = new FlowVariable("flow variable", "B");
            makeFlowVariablesAvailable(flowVariable);

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(APPLIED_VIEW_SETTINGS)//
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())//
                .build();

            assertTrue(m_nnc.getNodeContainerState().isExecuted());

            callApplyData(applyData);

            assertTrue(m_nnc.getNodeContainerState().isExecuted());

            var expectedNodeSettings = new TestNodeSettingsBuilder()
                .modifyAt(VIEW, VIEW_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).build())//
                .buildNodeSettings();
            assertSubNodeSettingsForKey(expectedNodeSettings, "view_variables");
        }

        @Test
        void testApplyDataResetsObjectSettingsToPreviousStateWhenOverwritten()
            throws IOException, InvalidSettingsException {

            final var credentialsFlowVariable = CredentialsStore.newCredentialsFlowVariable("credentials flow variable",
                "varUsername", "varPassword", "varSecondFactor");
            makeFlowVariablesAvailable(credentialsFlowVariable);

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(VIEW, OBJECT_SETTING,
                    b -> b.withControllingFlowVariableName(credentialsFlowVariable.getName()).build()) //
                .modifyAt(VIEW, List.of(OBJECT_SETTING, "username"), b -> b.setStringValue("applied username").build())//
                .build();

            callApplyData(applyData);
            final var savedNodeSettings = m_nnc.getNodeSettings();
            assertThat(savedNodeSettings.getNodeSettings("view").getNodeSettings("objectSetting").getString("login"))
                .isEmpty();
        }

        @Test
        void testApplyDataResetsNodeOnFlawedViewVariables() throws IOException {

            final var flowVariable = new FlowVariable("flow variable", "B");
            makeFlowVariablesAvailable(flowVariable);

            assertTrue(m_nnc.getNodeContainerState().isExecuted());

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(VIEW, OBJECT_SETTING,
                    b -> b.withControllingFlowVariableName(flowVariable.getName()).asFlawedFlowVariable().build()) //
                .build();

            callApplyData(applyData);

            assertFalse(m_nnc.getNodeContainerState().isExecuted());
        }

        @Test
        void testApplyDataResetsNodeOnExposedVariableChange() throws IOException {

            new TestNodeSettingsBuilder()//
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.withExposedFlowVariableName("previous name").build())//
                .buildAndSaveTo(m_nnc);

            assertTrue(m_nnc.getNodeContainerState().isExecuted());

            final var applyData = new ApplyDataBuilder()//
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.withExposedFlowVariableName("new name").build()) //
                .build();

            callApplyData(applyData);

            assertFalse(m_nnc.getNodeContainerState().isExecuted());
        }

        @Test
        void testApplyDataResetsNodeOnSettingsUnderlyingExposedVariableChange() throws IOException {

            final var previousViewSettings = createPreviousViewSettings();

            new TestNodeSettingsBuilder()//
                .withNonDefaultViewSettings(previousViewSettings)//
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.withExposedFlowVariableName("previous name").build())//
                .buildAndSaveTo(m_nnc);

            assertTrue(m_nnc.getNodeContainerState().isExecuted());

            previousViewSettings.m_viewSetting = ViewSettings.MyEnum.C; // not B

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultViewSettings(previousViewSettings)//
                .modifyAt(VIEW, VIEW_SETTING_CFG, b -> b.withExposedFlowVariableName("previous name").build())//
                .build();

            callApplyData(applyData);

            assertFalse(m_nnc.getNodeContainerState().isExecuted());
        }

        @Test
        void testApplyDataWithInvalidSettings() throws IOException {

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(MODEL, MODEL_SETTING, b -> b.setStringValue(MODEL_SETTING_INVALID_VALUE).build())//
                .build();

            final var response = callApplyData(applyData);

            assertTrue(m_nnc.getNodeContainerState().isExecuted()); // i.e. not applied
            assertThatJson(response).inPath("$.error").isString().contains(VALIDATION_ERROR_MESSAGE);

        }

        @Test
        void testApplyDataWithInvalidFlowVariableSettings() throws IOException {

            final var inValidFlowVariable = new FlowVariable("flow variable", MODEL_SETTING_INVALID_VALUE);
            makeFlowVariablesAvailable(inValidFlowVariable);

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(inValidFlowVariable.getName()).build())//
                .build();

            final var response = callApplyData(applyData);

            assertFalse(m_nnc.getNodeContainerState().isExecuted()); // i.e. applied
            assertThatJson(response).inPath("$.warningMessages[0]").isString().contains("flow variable",
                VALIDATION_ERROR_MESSAGE);

        }

        @Test
        void testApplyDataWithInvalidSettingsButValidFlowVariables() throws IOException, InvalidSettingsException {
            clearPreviousNodeSettings();

            final var validFlowVariable = new FlowVariable("flow variable", "Something valid");
            makeFlowVariablesAvailable(validFlowVariable);

            final var applyData = new ApplyDataBuilder()//
                .modifyAt(MODEL, MODEL_SETTING, b -> b.setStringValue(MODEL_SETTING_INVALID_VALUE).build())//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(validFlowVariable.getName()).build())//
                .build();

            final var response = callApplyData(applyData);

            assertFalse(m_nnc.getNodeContainerState().isExecuted()); // i.e. applied
            assertThatJson(response).inPath("$.warningMessages[0]").isString()
                .contains("settings overridden by flow variables have been saved as underlying manual settings");
        }

        @Test
        void testApplyDataWithInvalidSettingsAndInvalidFlowVariables() throws IOException, InvalidSettingsException {
            clearPreviousNodeSettings();

            final var inValidFlowVariable = new FlowVariable("flow variable", MODEL_SETTING_INVALID_VALUE);
            makeFlowVariablesAvailable(inValidFlowVariable);

            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(MODEL, MODEL_SETTING_CFG,
                    b -> b.withControllingFlowVariableName(inValidFlowVariable.getName()).build())//
                .modifyAt(MODEL, MODEL_SETTING, b -> b.setStringValue(MODEL_SETTING_INVALID_VALUE).build())//
                .build();

            final var response = callApplyData(applyData);

            assertTrue(m_nnc.getNodeContainerState().isExecuted()); // i.e. not applied
            assertThatJson(response).inPath("$.error").isString() //
                .contains("flow variable") //
                .contains(VALIDATION_ERROR_MESSAGE);
        }

        @Test
        void testApplyDataWithInvalidSettingsAndNoControllingFlowVariables() throws IOException, IllegalStateException {
            final var applyData = new ApplyDataBuilder()//
                .withNonDefaultModelSettings(PREVIOUS_MODEL_SETTINGS)//
                .withNonDefaultViewSettings(PREVIOUS_VIEW_SETTINGS)//
                .modifyAt(MODEL, MODEL_SETTING_CFG, b -> b.withExposedFlowVariableName("only exposed").build())//
                .modifyAt(MODEL, MODEL_SETTING, b -> b.setStringValue(MODEL_SETTING_INVALID_VALUE).build())//
                .build();

            final var response = callApplyData(applyData);

            assertTrue(m_nnc.getNodeContainerState().isExecuted()); // i.e. not applied
            assertThatJson(response).inPath("$.error").isString() //
                .doesNotContain("flow variable") //
                .contains(VALIDATION_ERROR_MESSAGE);
        }

        private JsonNode callApplyData(final JsonNode applyData) throws IOException {
            final var result = NodeDialogManager.getInstance().getDataServiceManager()
                .callApplyDataService(NodeWrapper.of(m_nnc), applyData.toString());
            return TEST_MAPPER.readTree(result);
        }

        private void assertSubNodeSettingsForKey(final NodeSettings expected, final String key)
            throws InvalidSettingsException {
            assertThat(m_nnc.getNodeSettings().getNodeSettings(key)).isEqualTo(expected.getNodeSettings(key));
        }

    }

    private void clearPreviousNodeSettings() throws InvalidSettingsException {
        final var existingSettings = m_nnc.getNodeSettings();
        existingSettings.addNodeSettings(SettingsType.MODEL.getConfigKey());
        existingSettings.addNodeSettings(SettingsType.VIEW.getConfigKey());
        TestNodeSettingsBuilder.loadSettingsIntoNode(existingSettings, m_nnc);
    }

    private abstract static class DefaultDialogSettingsDocumentBuilder<T extends AbstractSettingsDocumentBuilder<T>>
        extends AbstractSettingsDocumentBuilder<T> {

        ModelSettings m_modelSettings = new ModelSettings();

        ViewSettings m_viewSettings = new ViewSettings();

        DefaultDialogSettingsDocumentBuilder() {
        }

        T withNonDefaultModelSettings(final ModelSettings modelSettings) {
            m_modelSettings = modelSettings;
            return super.thisAsT();
        }

        T withNonDefaultViewSettings(final ViewSettings viewSettings) {
            m_viewSettings = viewSettings;
            return super.thisAsT();
        }

    }

    private final static class ApplyDataBuilder extends DefaultDialogSettingsDocumentBuilder<ApplyDataBuilder> {

        ObjectNode build() {
            final var root = new ObjectMapper().createObjectNode();
            root.putObject("flowVariableSettings");
            final var data = root.putObject("data");
            data.set("model", JsonFormsDataUtil.toJsonData(super.m_modelSettings));
            data.set("view", JsonFormsDataUtil.toJsonData(super.m_viewSettings));

            JsonBuilderUtils.resolveCustomSettingsAt(root, super.getCustomSettingsAt());
            return root;
        }

    }

    private final static class TestNodeSettingsBuilder
        extends DefaultDialogSettingsDocumentBuilder<TestNodeSettingsBuilder> {

        /**
         * Return void. Instead, the settings are loaded to the container.
         */
        void buildAndSaveTo(final NativeNodeContainer nnc) {
            final var nodeSettings = buildNodeSettings(nnc);
            try {
                loadSettingsIntoNode(nodeSettings, nnc);
            } catch (InvalidSettingsException ex) {
                throw new RuntimeException(ex); //NOSONAR
            }
        }

        /**
         * Build without loading to node container. Only used for comparison.
         */
        NodeSettings buildNodeSettings() {
            return buildNodeSettings(null);
        }

        private NodeSettings buildNodeSettings(final NativeNodeContainer nnc) {
            var nodeSettings = initializeNodeSettings(nnc);
            NodeSettingsBuilderUtils.resolveCustomSettingsAt(nodeSettings, super.getCustomSettingsAt());
            return nodeSettings;

        }

        private NodeSettings initializeNodeSettings(final NativeNodeContainer nnc) {
            var nodeSettings = new NodeSettings("configuration");
            if (nnc != null) {
                nnc.getParent().saveNodeSettings(nnc.getID(), nodeSettings);
            }
            var modelSettings = nodeSettings.addNodeSettings("model");
            var viewSettings = nodeSettings.addNodeSettings("view");
            NodeParametersUtil.saveSettings(ModelSettings.class, super.m_modelSettings, modelSettings);
            NodeParametersUtil.saveSettings(ViewSettings.class, super.m_viewSettings, viewSettings);
            return nodeSettings;
        }

        private static void loadSettingsIntoNode(final NodeSettings nodeSettings, final NativeNodeContainer nnc)
            throws InvalidSettingsException {
            nnc.getParent().loadNodeSettings(nnc.getID(), nodeSettings);
            nnc.getParent().executeAllAndWaitUntilDone();
        }

    }

}
