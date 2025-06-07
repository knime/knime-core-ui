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

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.WebDialogNodeRepresentation.DefaultWebDialogNodeRepresentation;
import org.knime.core.webui.node.dialog.WebDialogValue;
import org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.DynamiclyConfigurableTestConfigNodeFactory.DynamiclyConfigurableTestConfigNodeModel;
import org.knime.core.webui.node.dialog.defaultdialog.SubNodeContainerDialogTest.DynamiclyConfigurableTestConfigNodeFactory.DynamiclyConfigurableTestConfigNodeValue;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsLoader;
import org.knime.core.webui.node.dialog.utils.AbstractSettingsDocumentBuilder;
import org.knime.core.webui.node.dialog.utils.FlowVariablesInputNodeFactory;
import org.knime.core.webui.node.dialog.utils.TestConfigurationNodeFactoryTemplate;
import org.knime.testing.util.WorkflowManagerUtil;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

            new SimpleInitialDataBuilder("default", "empty") //
                .build(m_snc, m_wfm, m_configurationNodeModel);

            var initialData = getInitialData();
            assertThatJson(initialData).inPath(SimpleInitialDataBuilder.PATH_TO_KEY).isEqualTo("default");

        }

        @Test
        void testInitialDataFromCurrentValue() throws IOException, InvalidSettingsException {
            new SimpleInitialDataBuilder("default", "empty") //
                .withCurrentValue("current") //
                .build(m_snc, m_wfm, m_configurationNodeModel);

            var initialData = getInitialData();

            assertThatJson(initialData).inPath(SimpleInitialDataBuilder.PATH_TO_KEY).isEqualTo("current");

        }

        @Test
        void testInitialDataWithFlowVariables() throws IOException, InvalidSettingsException {
            final var flowVarName = "flowVarName(testInitialDataWithFlowVariables)";
            new SimpleInitialDataBuilder("default", "empty")//
                .withCurrentValue("current") //
                .modify(b -> b.withControllingFlowVariableName(flowVarName).build())
                .build(m_snc, m_wfm, m_configurationNodeModel);
            makeFlowVariablesAvailable(new FlowVariable(flowVarName, "flowVariableValue"));

            var initialData = getInitialData();

            assertThatJson(initialData).inPath(SimpleInitialDataBuilder.PATH_TO_KEY).isEqualTo("flowVariableValue");

        }

        static class SimpleInitialDataBuilder extends AbstractSettingsDocumentBuilder<SimpleInitialDataBuilder> {

            static final String PATH_TO_KEY = "$.result.data.model.3.key";

            private final DynamiclyConfigurableTestConfigNodeValue m_defaultValue =
                new DynamiclyConfigurableTestConfigNodeValue();

            private final DynamiclyConfigurableTestConfigNodeValue m_emptyValue =
                new DynamiclyConfigurableTestConfigNodeValue();

            private DynamiclyConfigurableTestConfigNodeValue m_dialogValue;

            SimpleInitialDataBuilder(final String defaultValue, final String emptyValue) {
                m_defaultValue.setValue(defaultValue);
                m_emptyValue.setValue(emptyValue);
            }

            SimpleInitialDataBuilder withCurrentValue(final String currentValue) {
                m_dialogValue = new DynamiclyConfigurableTestConfigNodeValue();
                m_dialogValue.setValue(currentValue);
                return this;
            }

            SimpleInitialDataBuilder modify(final Function<SettingsAtPath.Builder, SettingsAtPath> subBuilder) {
                return super.modifyAt(SettingsType.MODEL, List.of("3", "key"), subBuilder);
            }

            void build(final SubNodeContainer snc, final WorkflowManager wfm,
                final DynamiclyConfigurableTestConfigNodeModel model) throws InvalidSettingsException {
                setUpConfigurationModel(model);
                modifySettings(snc, wfm);
            }

            private void setUpConfigurationModel(final DynamiclyConfigurableTestConfigNodeModel model) {
                Stream.of(m_defaultValue, m_emptyValue, m_dialogValue).filter(Objects::nonNull).forEach(dialogValue -> {
                    dialogValue.setToDialogJson(value -> TEST_MAPPER.createObjectNode().put("key", (String)value));
                    dialogValue.setSaveToNodeSettings((value, settings) -> settings.addString("key", (String)value));
                    dialogValue.setLoadFromNodeSettings(settings -> settings.getString("key"));
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

        private JsonNode getInitialData() throws IOException {
            final var jsonNode =
                TEST_MAPPER.readTree(m_dataServiceManager.callInitialDataService(NodeWrapper.of(m_snc)));
            if (jsonNode.has("internalError")) {
                throw new IOException("Error while retrieving initial data: "
                    + TEST_MAPPER.writeValueAsString(jsonNode.get("internalError")));
            }
            return jsonNode;
        }

    }

    @Nested
    class ApplyDataTest {

        @Test
        void testApplyData() throws IOException, InvalidSettingsException {
        }

        private JsonNode callApplyData(final JsonNode applyData) throws IOException {
            var result = m_dataServiceManager.callApplyDataService(NodeWrapper.of(m_snc), applyData.toString());
            return TEST_MAPPER.readTree(result);
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

            DynamiclyConfigurableTestConfigNodeRepresentation(
                final DialogElementRendererSpec webUIDialogElementRendererSpec) {
                m_webUIDialogElementRendererSpec = webUIDialogElementRendererSpec;
            }

            @Override
            public DialogElementRendererSpec getWebUIDialogElementRendererSpec() {
                return m_webUIDialogElementRendererSpec;
            }

        }

        static class DynamiclyConfigurableTestConfigNodeValue
            implements TestConfigurationNodeFactoryTemplate.TestConfigNodeValue, WebDialogValue {

            private Object m_value;

            private BiConsumer<Object, NodeSettingsWO> m_saveToNodeSettings;

            private SettingsLoader<Object> m_loadFromNodeSettings;

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

            void setLoadFromNodeSettings(final SettingsLoader<Object> loadFromNodeSettings) {
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