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
 *   June 6, 2025 (hornm): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.SubNodeContainerDialogSettingsDocumentBuilder.CFG_KEY;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.util.DefaultConfigurationLayoutCreator;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.PersistSchema;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.WebDialogNodeRepresentation.DefaultWebDialogNodeRepresentation;
import org.knime.core.webui.node.dialog.WebDialogValue;
import org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.ApplyDataTest.ApplyDataBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.DynamiclyConfigurableTestConfigNodeFactory.DynamiclyConfigurableTestConfigNodeModel;
import org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.DynamiclyConfigurableTestConfigNodeFactory.DynamiclyConfigurableTestConfigNodeValue;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.internal.VariableSettings;
import org.knime.core.webui.node.dialog.utils.AbstractSettingsDocumentBuilder;
import org.knime.core.webui.node.dialog.utils.FlowVariablesInputNodeFactory;
import org.knime.core.webui.node.dialog.utils.TestConfigurationNodeFactoryTemplate;
import org.knime.node.parameters.migration.ParametersLoader;
import org.knime.testing.util.WorkflowManagerUtil;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Integration tests for {@link SubNodeContainerDialog}-related logic.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class SubNodeContainerDialogTest {

    private WorkflowManager m_wfm;

    private SubNodeContainer m_snc;

    private DynamiclyConfigurableTestConfigNodeModel m_configurationNodeModel;

    private DataServiceManager<NodeWrapper> m_dataServiceManager;

    private NativeNodeContainer m_previousNode;

    private static final ObjectMapper TEST_MAPPER = new ObjectMapper();

    private static String previousComponentUiMode;

    private static ServiceRegistration<?> serviceRegistration;

    static final String UI_MODE_PROP = "org.knime.component.ui.mode";

    static void setComponentUiMode(final String mode) {
        System.setProperty(UI_MODE_PROP, mode);
    }

    @BeforeAll
    static void setUpComponentUiModeJs() {
        previousComponentUiMode = System.setProperty(UI_MODE_PROP, "js");

        var bundleContext = FrameworkUtil.getBundle(SubNodeContainerDialogTest.class).getBundleContext();
        serviceRegistration = bundleContext.registerService(DefaultConfigurationLayoutCreator.class.getName(),
            new DefaultConfigurationLayoutCreator() { // NOSONAR

                @Override
                public String createDefaultConfigurationLayout(final Map<NodeIDSuffix, DialogNode> configurationNodes)
                    throws IOException {
                    return null;
                }

                @Override
                public void addUnreferencedDialogNodes(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider,
                    final Map<NodeIDSuffix, DialogNode> allNodes) {
                    //
                }

                @Override
                public void updateConfigurationLayout(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider) {
                    //
                }

                @Override
                public List<Integer> getConfigurationOrder(
                    final SubnodeContainerConfigurationStringProvider configurationStringProvider,
                    final Map<NodeID, DialogNode> nodes, final WorkflowManager wfm) {
                    return Collections.singletonList(0);
                }

            }, new Hashtable<>());
    }

    @AfterAll
    static void tearDownComponentUiModeJs() {
        if (previousComponentUiMode != null) {
            System.setProperty(UI_MODE_PROP, previousComponentUiMode);
        } else {
            System.clearProperty(UI_MODE_PROP);
        }

        serviceRegistration.unregister();
    }

    @BeforeEach
    void createWorkflowAndAddNode() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var nnc = WorkflowManagerUtil.createAndAddNode(m_wfm, new DynamiclyConfigurableTestConfigNodeFactory());
        var componentId =
            m_wfm.collapseIntoMetaNode(new NodeID[]{nnc.getID()}, new WorkflowAnnotationID[0], "TestComponent")
                .getCollapsedMetanodeID();
        m_wfm.convertMetaNodeToSubNode(componentId);

        m_snc = (SubNodeContainer)m_wfm.getNodeContainer(componentId);

        m_previousNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new FlowVariablesInputNodeFactory());
        m_wfm.addConnection(m_previousNode.getID(), 0, m_snc.getID(), 0);
        m_wfm.executeAllAndWaitUntilDone();

        final var configurationNodeContainer = findConfigurationNode(m_snc);
        m_configurationNodeModel =
            (DynamiclyConfigurableTestConfigNodeModel)configurationNodeContainer.getNode().getNodeModel();
        m_dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();

        NodeContext.pushContext(m_snc);
    }

    private JsonNode getInitialData() throws IOException {
        return getInitialData(m_dataServiceManager, m_snc);
    }

    private static JsonNode getInitialData(final DataServiceManager<NodeWrapper> dataServiceManager,
        final SingleNodeContainer snc) throws IOException {
        final var jsonNode = TEST_MAPPER.readTree(dataServiceManager.callInitialDataService(NodeWrapper.of(snc)));
        if (jsonNode.has("internalError")) {
            throw new IOException("Error while retrieving initial data: "
                + TEST_MAPPER.writeValueAsString(jsonNode.get("internalError")));
        }
        return jsonNode;
    }

    private JsonNode callApplyData(final JsonNode applyData) throws IOException {
        var result = m_dataServiceManager.callApplyDataService(NodeWrapper.of(m_snc), applyData.toString());
        return TEST_MAPPER.readTree(result);
    }

    private JsonNode callRPCData(final String method, final JsonNode... params) throws IOException {
        var result = m_dataServiceManager.callRpcDataService(NodeWrapper.of(m_snc),
            RpcDataService.jsonRpcRequest(method, params));
        return TEST_MAPPER.readTree(result);
    }

    private void makeFlowVariablesAvailable(final FlowVariable... flowVariables) {
        final var flowObjectStack = m_previousNode.getOutgoingFlowObjectStack();
        for (var flowVar : flowVariables) {
            flowObjectStack.push(flowVar);
        }
        m_wfm.resetAndConfigureNode(m_snc.getID());
        m_wfm.executeAllAndWaitUntilDone();
    }

    private static NativeNodeContainer findConfigurationNode(final SubNodeContainer component) {
        return (NativeNodeContainer)component.getWorkflowManager().getNodeContainers().stream()
            .filter(nc -> nc.getName().equals("DynamiclyConfigurableTestConfig")).findFirst().orElseThrow();
    }

    @AfterEach
    void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
        NodeContext.removeLastContext();
    }

    @Nested
    class InitialDataTest {

        @Test
        void testInitialDataFromDefaultValue() throws IOException, InvalidSettingsException {

            new InitialDataBuilder("default", "empty") //
                .build(m_snc, m_wfm, m_configurationNodeModel);

            var initialData = getInitialData();
            assertThatJson(initialData).inPath(InitialDataBuilder.PATH_TO_KEY).isEqualTo("default");

        }

        @Test
        void testInitialDataFromCurrentValue() throws IOException, InvalidSettingsException {
            new InitialDataBuilder("default", "empty") //
                .withCurrentValue("current") //
                .build(m_snc, m_wfm, m_configurationNodeModel);

            var initialData = getInitialData();

            assertThatJson(initialData).inPath(InitialDataBuilder.PATH_TO_KEY).isEqualTo("current");

        }

        @Test
        void testInitialDataWithFlowVariables() throws IOException, InvalidSettingsException {
            final var flowVarName = "flowVarName(testInitialDataWithFlowVariables)";
            new InitialDataBuilder("default", "empty")//
                .withCurrentValue("current") //
                .modifyAtCfgKey(b -> b.withControllingFlowVariableName(flowVarName).build())
                .build(m_snc, m_wfm, m_configurationNodeModel);
            makeFlowVariablesAvailable(new FlowVariable(flowVarName, "flowVariableValue"));

            var initialData = getInitialData();

            assertThatJson(initialData).inPath(InitialDataBuilder.PATH_TO_KEY).isEqualTo("flowVariableValue");

        }

        @MethodSource("persistSchema")
        @ParameterizedTest
        void testPersistSchema(final PersistSchema persistSchema, final ObjectNode expectedJson)
            throws IOException, InvalidSettingsException {
            new InitialDataBuilder("default", "empty") //
                .withPersistSchema(persistSchema)//
                .build(m_snc, m_wfm, m_configurationNodeModel);

            var initialData = getInitialData();
            assertThatJson(initialData).inPath("$.result.persist.properties.model.properties.3")
                .isEqualTo(expectedJson);
        }

        /**
         * Provides different persist schemas to test the initial data.
         *
         * @return a stream of persist schemas and their expected JSON representation
         */
        static Stream<Arguments> persistSchema() {
            return Stream.of(//
                Arguments.of(new PersistSchema.PersistLeafSchema() {
                    @Override
                    public Optional<String> getConfigKey() {
                        return Optional.of("customKey");
                    }
                }, createExpectedPersistSchemaWithConfigKey()), //
                Arguments.of(new PersistSchema.PersistLeafSchema() {
                    @Override
                    public Optional<String[][]> getConfigPaths() {
                        return Optional.of(new String[][]{{"custom", "path"}});
                    }
                }, createExpectedPersistSchemaWithConfigPaths()), //
                Arguments.of(new PersistSchema.PersistTreeSchema.PersistTreeSchemaRecord(
                    Map.of("foo", new PersistSchema.PersistLeafSchema() {
                        @Override
                        public Optional<String> getConfigKey() {
                            return Optional.of("fooKey");
                        }
                    })),

                    createNestedExpectedPersistSchemaJson()) //

            );
        }

        private static ObjectNode createExpectedPersistSchemaWithConfigKey() {
            final var obj = TEST_MAPPER.createObjectNode();
            obj.put("type", "leaf").put("configKey", "customKey");
            return obj;
        }

        private static ObjectNode createExpectedPersistSchemaWithConfigPaths() {
            final var obj = TEST_MAPPER.createObjectNode();
            obj.put("type", "leaf").putArray("configPaths").addArray().add("custom").add("path");
            return obj;
        }

        private static ObjectNode createNestedExpectedPersistSchemaJson() {
            final var obj = TEST_MAPPER.createObjectNode();
            obj.put("type", "object").putObject("properties").set("foo",
                TEST_MAPPER.createObjectNode().put("type", "leaf").put("configKey", "fooKey"));
            return obj;
        }

        /**
         * Modifies and sets node settings of the configuration node model.
         */
        static class InitialDataBuilder extends SubNodeContainerDialogSettingsDocumentBuilder<InitialDataBuilder> {

            private PersistSchema m_persistSchema;

            InitialDataBuilder(final String defaultValue, final String emptyValue) {
                super(defaultValue, emptyValue);
            }

            static final String PATH_TO_KEY = "$.result.data.model.3.key";

            InitialDataBuilder withPersistSchema(final PersistSchema persistSchema) {
                m_persistSchema = persistSchema;
                return super.thisAsT();
            }

            void build(final SubNodeContainer snc, final WorkflowManager wfm,
                final DynamiclyConfigurableTestConfigNodeModel model) throws InvalidSettingsException {
                super.setUpConfigurationModel(model);
                if (m_persistSchema != null) {
                    model.getDialogRepresentation().setPersistSchema(m_persistSchema);
                }
                modifySettings(snc, wfm);
            }

            public void modifySettings(final SubNodeContainer snc, final WorkflowManager wfm)
                throws InvalidSettingsException {
                if (super.getCustomSettingsAt().isEmpty()) {
                    return;
                }
                final var nodeSettings = snc.getNodeSettings();
                NodeSettingsBuilderUtils.resolveCustomSettingsAt(nodeSettings, super.getCustomSettingsAt());

                wfm.loadNodeSettings(snc.getID(), nodeSettings);
            }

        }

    }

    @Nested
    class ApplyDataTest {

        @Test
        void testApplyData() throws IOException, InvalidSettingsException {

            final var toBeApplied = new ApplyDataBuilder() //
                .withToBeAppliedValue("toBeApplied") //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);

            callApplyData(toBeApplied);

            final var appliedSettings = m_snc.getNodeSettings();

            assertThat(appliedSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()).getNodeSettings("3")
                .getString(SubNodeContainerDialogSettingsDocumentBuilder.CFG_KEY)).isEqualTo("toBeApplied");

        }

        @Test
        void testApplyDataWithFlowVariables() throws IOException, InvalidSettingsException {

            String flowVarName = "flowVar";
            final var toBeApplied = new ApplyDataBuilder() //
                .withToBeAppliedValue("toBeApplied") //
                .modifyAtCfgKey(b -> {
                    return b.withControllingFlowVariableName(flowVarName).build();
                }) //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);

            callApplyData(toBeApplied);

            final var appliedSettings = m_snc.getNodeSettings();

            assertThat(ApplyDataBuilder.getStringValueAtAppliedSettings(appliedSettings)).isEqualTo("toBeApplied");

            assertThat(ApplyDataBuilder.getVariableAtAppliedSettings(appliedSettings)
                .getString(VariableSettings.USED_VARIABLE_CFG_KEY)).isEqualTo(flowVarName);
        }

        @Test
        void testApplyDataWithFlowVariablesAndPreviousSettings() throws IOException, InvalidSettingsException {

            final var previousValue = "previousValue";
            final var previousSettings = new ApplyDataBuilder() //
                .withToBeAppliedValue(previousValue) //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);

            final var flowVarName = "flowVar";

            final var toBeApplied = new ApplyDataBuilder() //
                .withToBeAppliedValue("toBeApplied (ignored)") //
                .modifyAtCfgKey(b -> b.withControllingFlowVariableName(flowVarName).build()) //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);

            callApplyData(previousSettings);
            callApplyData(toBeApplied);

            final var appliedSettings = m_snc.getNodeSettings();

            /**
             * Now, in contrast to {@link testApplyDataWithFlowVariables}, the previous value should not be overwritten
             * in case a flow variable is set.
             */
            assertThat(ApplyDataBuilder.getStringValueAtAppliedSettings(appliedSettings)).isEqualTo(previousValue);

            assertThat(ApplyDataBuilder.getVariableAtAppliedSettings(appliedSettings)
                .getString(VariableSettings.USED_VARIABLE_CFG_KEY)).isEqualTo(flowVarName);

        }

        /**
         * Creates and returns the apply data JSON.
         */
        static class ApplyDataBuilder extends SubNodeContainerDialogSettingsDocumentBuilder<ApplyDataBuilder> {

            ApplyDataBuilder() {
                super("default", "empty");
            }

            ApplyDataBuilder withToBeAppliedValue(final String value) {
                super.modifyAt(SettingsType.MODEL, List.of("3", KEY), b -> b.setStringValue(value).build());
                return super.thisAsT();
            }

            /**
             * Builds the apply data JSON.
             *
             * @return the JSON node containing the apply data
             */
            JsonNode build(final DynamiclyConfigurableTestConfigNodeModel model,
                final DataServiceManager<NodeWrapper> dataServiceManager, final SingleNodeContainer snc)
                throws IOException, InvalidSettingsException {
                super.setUpConfigurationModel(model);
                final var jsonNode = (ObjectNode)getInitialData(dataServiceManager, snc).get("result");
                JsonBuilderUtils.resolveCustomSettingsAt(jsonNode, getCustomSettingsAt());
                return jsonNode;
            }

            static String getStringValueAtAppliedSettings(final NodeSettingsRO appliedSettings)
                throws InvalidSettingsException {
                return appliedSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()).getNodeSettings("3")
                    .getString(SubNodeContainerDialogSettingsDocumentBuilder.CFG_KEY);
            }

            static NodeSettingsRO getVariableAtAppliedSettings(final NodeSettingsRO appliedSettings)
                throws InvalidSettingsException {
                return appliedSettings.getNodeSettings("variables").getNodeSettings("tree").getNodeSettings("3")
                    .getNodeSettings(SubNodeContainerDialogSettingsDocumentBuilder.CFG_KEY);
            }

        }
    }

    @Nested
    class FlowVariableDataServiceTest {

        @Test
        void testGetAvailableFlowVariables() throws IOException, InvalidSettingsException {
            final var applyData = new ApplyDataBuilder() //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);
            makeFlowVariablesAvailable(new FlowVariable("anInteger", 1));
            final var availableFlowVariables = callRPCData("flowVariables.getAvailableFlowVariables",
                TextNode.valueOf(TEST_MAPPER.writeValueAsString(applyData)),
                TEST_MAPPER.valueToTree(List.of("model", "3", CFG_KEY)));
            assertThatJson(availableFlowVariables).inPath("$.result[1].name").isString().isEqualTo("anInteger");
            assertThatJson(availableFlowVariables).inPath("$.result[1].type.id").isString().isEqualTo("INTEGER");
            assertThatJson(availableFlowVariables).inPath("$.result[1].type.text").isString().isEqualTo("IntType");
        }

        @Test
        void testGetFlowVariableOverrideValue() throws IOException, InvalidSettingsException {
            final var flowVarName = "flowVarName";
            final var flowVarValue = "flowVariableValue";
            makeFlowVariablesAvailable(new FlowVariable(flowVarName, flowVarValue));
            final var applyData = new ApplyDataBuilder() //
                .modifyAtCfgKey(b -> b.withControllingFlowVariableName(flowVarName).build()) //
                .build(m_configurationNodeModel, m_dataServiceManager, m_snc);
            final var flowVariableOverrideValue = callRPCData("flowVariables.getFlowVariableOverrideValue",
                TextNode.valueOf(TEST_MAPPER.writeValueAsString(applyData)),
                TEST_MAPPER.valueToTree(List.of("model", "3", SubNodeContainerDialogSettingsDocumentBuilder.KEY)));
            assertThatJson(flowVariableOverrideValue).inPath("$.result").isString().isEqualTo(flowVarValue);
        }

    }

    /**
     * Builds a configuration model with a configuration operating on a single string value.
     *
     */
    static abstract class SubNodeContainerDialogSettingsDocumentBuilder<T extends SubNodeContainerDialogSettingsDocumentBuilder<T>>
        extends AbstractSettingsDocumentBuilder<T> {

        static final String KEY = "key";

        static final String CFG_KEY = "cfg_key";

        private final DynamiclyConfigurableTestConfigNodeValue m_defaultValue =
            new DynamiclyConfigurableTestConfigNodeValue();

        private final DynamiclyConfigurableTestConfigNodeValue m_emptyValue =
            new DynamiclyConfigurableTestConfigNodeValue();

        private DynamiclyConfigurableTestConfigNodeValue m_dialogValue;

        SubNodeContainerDialogSettingsDocumentBuilder(final String defaultValue, final String emptyValue) {
            m_defaultValue.setValue(defaultValue);
            m_emptyValue.setValue(emptyValue);
        }

        T withCurrentValue(final String currentValue) {
            m_dialogValue = new DynamiclyConfigurableTestConfigNodeValue();
            m_dialogValue.setValue(currentValue);
            return super.thisAsT();
        }

        T modifyAtCfgKey(final Function<SettingsAtPath.Builder, SettingsAtPath> subBuilder) {
            return super.modifyAt(SettingsType.MODEL, List.of("3", CFG_KEY), subBuilder);
        }

        T modifyAtJsonKey(final Function<SettingsAtPath.Builder, SettingsAtPath> subBuilder) {
            return super.modifyAt(SettingsType.MODEL, List.of("3", KEY), subBuilder);
        }

        private void setUpConfigurationModel(final DynamiclyConfigurableTestConfigNodeModel model) {
            Stream.of(m_defaultValue, m_emptyValue, m_dialogValue).filter(Objects::nonNull).forEach(dialogValue -> {
                dialogValue.setToDialogJson(value -> TEST_MAPPER.createObjectNode().put(KEY, (String)value));
                dialogValue.setFromDialogJson(json -> dialogValue.setValue(json.get(KEY).asText()));
                dialogValue.setSaveToNodeSettings((value, settings) -> settings.addString(CFG_KEY, (String)value));
                dialogValue.setLoadFromNodeSettings(settings -> settings.getString(CFG_KEY));
            });

            final var rendererSpec = new TextRendererSpec() {

                @Override
                public String getTitle() {
                    return "Test Configuration Node";
                }
            }.at("key");

            final var representation =
                new DynamiclyConfigurableTestConfigNodeFactory.DynamiclyConfigurableTestConfigNodeRepresentation(
                    rendererSpec);
            model.setDefaultValue(m_defaultValue);
            model.setEmptyValue(m_emptyValue);
            if (m_dialogValue != null) {
                model.setDialogValue(m_dialogValue);
            }
            model.setRepresentation(representation);
        }

    }

    static class DynamiclyConfigurableTestConfigNodeFactory extends TestConfigurationNodeFactoryTemplate {

        @Override
        public NodeModel createNodeModel() {
            return new DynamiclyConfigurableTestConfigNodeModel();
        }

        static class DynamiclyConfigurableTestConfigNodeModel extends
            TestConfigurationNodeFactoryTemplate.TestConfigNodeModel<DynamiclyConfigurableTestConfigNodeRepresentation, DynamiclyConfigurableTestConfigNodeValue> {

            private DynamiclyConfigurableTestConfigNodeValue m_emptyValue;

            private DynamiclyConfigurableTestConfigNodeRepresentation m_representation;

            private DynamiclyConfigurableTestConfigNodeValue m_defaultValue;

            private DynamiclyConfigurableTestConfigNodeValue m_currentValue;

            private String m_paramName;

            protected DynamiclyConfigurableTestConfigNodeModel() {
                super();
            }

            @Override
            public DynamiclyConfigurableTestConfigNodeRepresentation getDialogRepresentation() {
                return m_representation;
            }

            @Override
            public DynamiclyConfigurableTestConfigNodeValue createEmptyDialogValue() {
                return m_emptyValue;
            }

            @Override
            public DynamiclyConfigurableTestConfigNodeValue getDefaultValue() {
                return m_defaultValue;
            }

            @Override
            public DynamiclyConfigurableTestConfigNodeValue getDialogValue() {
                return m_currentValue;
            }

            @Override
            public void setDialogValue(final DynamiclyConfigurableTestConfigNodeValue value) {
                m_currentValue = value;

            }

            @Override
            public String getParameterName() {
                return m_paramName;
            }

            void setRepresentation(final DynamiclyConfigurableTestConfigNodeRepresentation representation) {
                m_representation = representation;
            }

            void setDefaultValue(final DynamiclyConfigurableTestConfigNodeValue dialogValue) {
                m_defaultValue = dialogValue;
            }

            public void setEmptyValue(final DynamiclyConfigurableTestConfigNodeValue dialogValue) {
                m_emptyValue = dialogValue;
            }

        }

        static class DynamiclyConfigurableTestConfigNodeRepresentation implements
            TestConfigurationNodeFactoryTemplate.TestConfigNodeRepresentation<DynamiclyConfigurableTestConfigNodeValue>,
            DefaultWebDialogNodeRepresentation<DynamiclyConfigurableTestConfigNodeValue> {

            private final DialogElementRendererSpec m_webUIDialogElementRendererSpec;

            private PersistSchema m_persistSchema;

            DynamiclyConfigurableTestConfigNodeRepresentation(
                final DialogElementRendererSpec webUIDialogElementRendererSpec) {
                m_webUIDialogElementRendererSpec = webUIDialogElementRendererSpec;
            }

            public void setPersistSchema(final PersistSchema persistSchema) {
                m_persistSchema = persistSchema;
            }

            @Override
            public DialogElementRendererSpec getWebUIDialogElementRendererSpec() {
                return m_webUIDialogElementRendererSpec;
            }

            @Override
            public Optional<PersistSchema> getPersistSchema() {
                return Optional.ofNullable(m_persistSchema);
            }

        }

        static class DynamiclyConfigurableTestConfigNodeValue
            implements TestConfigurationNodeFactoryTemplate.TestConfigNodeValue, WebDialogValue {

            private Object m_value;

            private BiConsumer<Object, NodeSettingsWO> m_saveToNodeSettings;

            private ParametersLoader<Object> m_loadFromNodeSettings;

            private Function<Object, JsonNode> m_toDialogJson;

            private Consumer<JsonNode> m_fromDialogJson;

            @Override
            public void saveToNodeSettings(final NodeSettingsWO settings) {
                if (m_saveToNodeSettings != null) {
                    m_saveToNodeSettings.accept(m_value, settings);
                }

            }

            @Override
            public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (m_loadFromNodeSettings != null) {
                    setValue(m_loadFromNodeSettings.load(settings));
                }

            }

            @Override
            public JsonNode toDialogJson() throws IOException {
                if (m_toDialogJson == null) {
                    throw new IOException("No toDialogJson function set");
                }
                return m_toDialogJson.apply(m_value);
            }

            @Override
            public void fromDialogJson(final JsonNode json) throws IOException {
                if (m_fromDialogJson == null) {
                    throw new IOException("No fromDialogJson function set");
                }
                m_fromDialogJson.accept(json);

            }

            void setValue(final Object value) {
                m_value = value;
            }

            void setSaveToNodeSettings(final BiConsumer<Object, NodeSettingsWO> saveToNodeSettings) {
                m_saveToNodeSettings = saveToNodeSettings;
            }

            void setLoadFromNodeSettings(final ParametersLoader<Object> loadFromNodeSettings) {
                m_loadFromNodeSettings = loadFromNodeSettings;
            }

            void setToDialogJson(final Function<Object, JsonNode> toDialogJson) {
                m_toDialogJson = toDialogJson;
            }

            void setFromDialogJson(final Consumer<JsonNode> fromDialogJson) {
                m_fromDialogJson = fromDialogJson;
            }

        }

    }

}