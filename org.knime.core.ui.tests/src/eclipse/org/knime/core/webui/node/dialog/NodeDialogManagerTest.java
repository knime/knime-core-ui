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
 *  NodeDialog, and NodeDialog) and that only interoperate with KNIME through
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
 *   Oct 17, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.node.ui.NodeDialogTestUtil.createNodeDialog;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.util.DefaultConfigurationLayoutCreator;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.node.view.NodeViewManagerTest;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.dialog.NodeDialogNodeFactory;
import org.knime.testing.node.dialog.NodeDialogNodeModel;
import org.knime.testing.node.dialog.NodeDialogNodeView;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests for {@link NodeDialogManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class NodeDialogManagerTest {

    private WorkflowManager m_wfm;

    /**
     * Clears the caches and files of the {@link NodeDialogManager}.
     */
    @BeforeEach
    @AfterEach
    public void clearNodeDialogManagerCachesAndFiles() {
        NodeDialogManager.getInstance().clearCaches();
    }

    @SuppressWarnings("javadoc")
    @BeforeEach
    public void createEmptyWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @SuppressWarnings("javadoc")
    @AfterEach
    public void disposeWorkflow() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    /**
     * Tests multiple {@link NodeDialogManager}-methods using a simple node dialog.
     */
    @Test
    void testSimpleNodeDialogNode() {
        var page = Page.create().fromString(() -> "test page content").relativePath("index.html");
        var hasDialog = new AtomicBoolean(true);
        NativeNodeContainer nc = createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(page), hasDialog::get);

        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node expected to have a node dialog").isTrue();
        var nodeDialogManager = NodeDialogManager.getInstance();
        assertThat(nodeDialogManager.getPageResourceManager().getPage(NodeWrapper.of(nc))).isSameAs(page);
        assertThat(NodeDialogManager.getInstance().getPageResourceManager().getPageId(NodeWrapper.of(nc)))
            .isEqualTo(nc.getID().toString().replace(":", "_"));

        assertThat(NodeDialogManager.getInstance().getDataServiceManager().callInitialDataService(NodeWrapper.of(nc)))
            .isEqualTo("{\"result\":\"test settings\"}");
        assertThat(nodeDialogManager.getPageResourceManager().getPage(NodeWrapper.of(nc)).isCompletelyStatic())
            .isFalse();

        hasDialog.set(false);
        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node not expected to have a node dialog").isFalse();

    }

    @Test
    void deactivatesNodeSettingsServiceOnDataServiceDeactivate() {
        final var nodeSettingsService = mock(NodeSettingsService.class);
        var page = Page.create().fromString(() -> "test page content").relativePath("index.html");
        NativeNodeContainer nc =
            createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(page, nodeSettingsService, null));
        final var nodeDialogManager = NodeDialogManager.getInstance();
        final var dataServiceManager = nodeDialogManager.getDataServiceManager();
        final var nodeWrapper = NodeWrapper.of(nc);
        dataServiceManager.callInitialDataService(nodeWrapper);

        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                assertThat(NodeContext.getContext().getNodeContainer()).isSameAs(nc);
                return null;
            }
        }).when(nodeSettingsService).deactivate();

        dataServiceManager.deactivateDataServices(nodeWrapper);

        verify(nodeSettingsService, times(1)).deactivate();
    }

    /**
     * Tests a {@link SubNodeContainer} dialog
     *
     * @throws IOException
     */
    @Test
    void testSubNodeContainerDialog() throws IOException {
        final var uiModeProperty = "org.knime.component.ui.mode";
        var componentUiMode = System.setProperty(uiModeProperty, "js");
        var bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        var serviceRegistration = bundleContext.registerService(DefaultConfigurationLayoutCreator.class.getName(),
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

        try {
            // build workflow
            var wfm = WorkflowManagerUtil.createEmptyWorkflow();
            var nnc = WorkflowManagerUtil.createAndAddNode(wfm, new TestConfigurationNodeFactory());
            var nonSupportedNNC =
                WorkflowManagerUtil.createAndAddNode(wfm, new TestNonSupportedConfigurationNodeFactory());

            var componentId = wfm.collapseIntoMetaNode(new NodeID[]{nnc.getID(), nonSupportedNNC.getID()},
                new WorkflowAnnotationID[0], "TestComponent").getCollapsedMetanodeID();
            wfm.convertMetaNodeToSubNode(componentId);

            SubNodeContainer component = (SubNodeContainer)wfm.getNodeContainer(componentId);
            NativeNodeContainer configurationNode =
                findSubNodeWithName(component, "Configuration Node (used in tests)");

            assertThat(NodeDialogManager.hasNodeDialog(component)).as("node expected to have a node dialog").isTrue();
            var nodeDialogManager = NodeDialogManager.getInstance();
            assertThat(nodeDialogManager.getPageResourceManager().getPage(NodeWrapper.of(component)).getRelativePath())
                .isEqualTo("dist/NodeDialog.js");

            var pageId = NodeDialogManager.getInstance().getPageResourceManager().getPageId(NodeWrapper.of(component));
            assertThat(pageId).isEqualTo("defaultdialog");

            var initialData = NodeDialogManager.getInstance().getDataServiceManager()
                .callInitialDataService(NodeWrapper.of(component));
            var resultAsJson = (ObjectNode)new ObjectMapper().readTree(initialData);
            var nodeIndex = configurationNode.getID().getIndex();
            assertInitialSubNodeContainerData(resultAsJson, nodeIndex);

            final var toBeApplied = getObject(resultAsJson, "result");
            setTestData(nodeIndex, toBeApplied, "test data");
            final var toBeAppliedString = new ObjectMapper().writeValueAsString(toBeApplied);
            final var componentWrapper = NodeWrapper.of(component);
            final var dataServiceManager = nodeDialogManager.getDataServiceManager();

            // Applying not possible ...

            final var errorResult = dataServiceManager.callApplyDataService(componentWrapper, toBeAppliedString);
            var errorResultAsJson = (ObjectNode)new ObjectMapper().readTree(errorResult);
            assertThatJson(errorResultAsJson).inPath("$.isApplied").isBoolean().isFalse();
            assertThatJson(errorResultAsJson).inPath("$.error").isString()
                .isEqualTo("Failed to apply due to a non-supported configuration that was not previously configured.");

            // ... first we need to remove the non-supported node ...
            component.getWorkflowManager()
                .removeNode(findSubNodeWithName(component, "Non-supported Configuration Node (used in tests)").getID());

            // ... now it works
            dataServiceManager.callApplyDataService(componentWrapper, toBeAppliedString);

            // check node model settings
            final var savedData = ((TestConfigurationNodeFactory.TestConfigNodeModel)configurationNode.getNodeModel())
                .getDialogValue().m_data;

            assertThat(savedData).isEqualTo("test data");

        } finally {
            if (componentUiMode != null) {
                System.setProperty(uiModeProperty, componentUiMode);
            } else {
                System.clearProperty(uiModeProperty);
            }
            serviceRegistration.unregister();
        }
    }

    private static NativeNodeContainer findSubNodeWithName(final SubNodeContainer component, final String nodeName) {
        return (NativeNodeContainer)component.getWorkflowManager().getNodeContainers()
            .stream().filter(nc -> nc.getName().equals(nodeName)).findFirst().orElseThrow();
    }

    private static void assertInitialSubNodeContainerData(final JsonNode resultAsJson, final int nodeIndex) {
        assertThatJson(resultAsJson).inPath("$.result.data.model.%s.data".formatted(nodeIndex))
            .isEqualTo("default from model");
        assertThatJson(resultAsJson)
            .inPath("$.result.schema.properties.model.properties.%s.properties.data.title".formatted(nodeIndex))
            .isEqualTo("Test Configuration Node");
        assertThatJson(resultAsJson).inPath("$.result.ui_schema.elements[0].scope")
            .isEqualTo("#/properties/model/properties/%s/properties/data".formatted(nodeIndex));
        assertThatJson(resultAsJson).inPath("$.result.ui_schema.elements[1].type")
            .isEqualTo("ConfigurationNodeNotSupported");
        assertThatJson(resultAsJson).inPath("$.result.ui_schema.elements[1].options.nodeName")
            .isEqualTo("Non-supported Configuration Node (used in tests)");

    }

    private static void setTestData(final int nodeIndex, final ObjectNode result, final String testData) {
        final var resultData = getObject(result, "data");
        final var modelSettings = getObject(resultData, "model");
        final var configSettings = getObject(modelSettings, String.valueOf(nodeIndex));
        configSettings.put("data", testData);
    }

    private static ObjectNode getObject(final ObjectNode node, final String key) {
        return (ObjectNode)node.get(key);
    }

    /**
     * Tests {@link NodeDialogManager#getPagePath(NodeWrapper)}.
     */
    @Test
    void testGetNodeDialogPagePath() {
        var staticPage = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResourceFile("resource.html");
        var dynamicPage = Page.create().fromString(() -> "page content").relativePath("page.html")
            .addResourceFromString(() -> "resource content", "resource.html");
        var nnc = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage)));
        var nnc2 = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(staticPage)));
        var nnc3 = NodeWrapper.of(createNodeWithNodeDialog(m_wfm, () -> createNodeDialog(dynamicPage)));
        var pageResourceManager = NodeDialogManager.getInstance().getPageResourceManager();
        String path = pageResourceManager.getPagePath(nnc);
        String path2 = pageResourceManager.getPagePath(nnc2);
        pageResourceManager.clearPageCache();
        String path3 = pageResourceManager.getPagePath(nnc3);
        String path4 = pageResourceManager.getPagePath(nnc3);
        assertThat(path).as("url of static pages not expected to change").isEqualTo(path2);
        assertThat(path).as("url of dynamic pages expected to change between node instances").isNotEqualTo(path3);
        assertThat(path3).as("url of dynamic pages not expected for same node instance (without node state change)")
            .isEqualTo(path4);
        assertThat(path).isEqualTo("uiext-dialog/org.knime.testing.node.dialog.NodeDialogNodeFactory/page.html");
    }

    /**
     * Tests {@link NodeDialogManager#hasNodeDialog(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeDialogManager#getNodeDialog(org.knime.core.node.workflow.NodeContainer)} for a node without a node
     * view.
     */
    @Test
    void testNodeWithoutNodeDialog() {
        NativeNodeContainer nc = createAndAddNode(m_wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat(NodeDialogManager.hasNodeDialog(nc)).as("node not expected to have a node dialog").isFalse();
        final var nodeDialogManager = NodeDialogManager.getInstance();
        Assertions.assertThatThrownBy(() -> nodeDialogManager.getNodeDialog(nc))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests {@link NodeDialogManager#callInitialDataService(NodeContainer)},
     * {@link NodeDialogManager#callRpcDataService(NodeContainer, String)} and
     * {@link NodeDialogManager#callApplyDataService(NodeContainer, String)}
     *
     * @throws IOException
     * @throws InvalidSettingsException
     */
    @Test
    void testCallDataServices() throws IOException, InvalidSettingsException {
        var page = Page.create().fromString(() -> "test page content").relativePath("index.html");
        Supplier<NodeDialog> nodeDialogSupplier = () -> createNodeDialog(page, new NodeSettingsService() { // NOSONAR

            @Override
            public void toNodeSettings(final String s,
                final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
                final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
                var split = s.split(",");
                settings.get(SettingsType.MODEL).addString(split[0], split[1]);
                settings.get(SettingsType.VIEW).addString(split[0], split[1]);
                DataServiceContext.get().addWarningMessage("test warning");
                DataServiceContext.get().addWarningMessage("another test warning");
            }

            @Override
            public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
                final PortObjectSpec[] specs) {
                assertThat(settings.size()).isEqualTo(2);
                return "the node settings";
            }

        }, RpcDataService.builder(new TestService()).build());

        var nc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm, nodeDialogSupplier);
        var nncWrapper = NodeWrapper.of(nc);
        var objectMapper = ObjectMapperUtil.getInstance().getObjectMapper();

        var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();
        assertThat(dataServiceManager.callInitialDataService(nncWrapper))
            .isEqualTo("{\"result\":\"the node settings\"}");
        assertThat(
            dataServiceManager.callRpcDataService(nncWrapper, RpcDataService.jsonRpcRequest("method", "test param")))
                .contains("\"result\":\"test param\"");
        // apply data, i.e. settings
        final var validResult = dataServiceManager.callApplyDataService(nncWrapper, "key,node settings value");
        final var validJsonResult = objectMapper.readTree(validResult);

        assertThatJson(validJsonResult).inPath("$.isApplied").isBoolean().isTrue();

        // check warning messages
        assertThatJson(validJsonResult).inPath("$.warningMessages").isArray().containsExactly("test warning",
            "another test warning");

        // check node model settings
        var modelSettings = ((NodeDialogNodeModel)nc.getNode().getNodeModel()).getLoadNodeSettings();
        assertThat(modelSettings.getString("key")).isEqualTo("node settings value");
        assertThat(nc.getNodeSettings().getNodeSettings("model").getString("key")).isEqualTo("node settings value");

        // check view settings
        var viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings).isNull(); // no view settings available without updating the node view
        NodeViewManager.getInstance().updateNodeViewSettings(nc);
        viewSettings = getNodeViewSettings(nc);
        assertThat(viewSettings.getString("key")).isEqualTo("node settings value");
        assertThat(nc.getNodeSettings().getNodeSettings("view").getString("key")).isEqualTo("node settings value");

        // check error on apply settings
        final var errorMessage = "ERROR,invalid";
        final var invalidResult = dataServiceManager.callApplyDataService(nncWrapper, errorMessage);
        final var invalidJsonResult = objectMapper.readTree(invalidResult);
        assertThatJson(invalidJsonResult).inPath("$.isApplied").isBoolean().isFalse();
        assertThatJson(invalidJsonResult).inPath("$.error").isString()
            .isEqualTo(NodeDialogNodeModel.VALIDATION_ERROR_MESSAGE);
    }

    /**
     * Makes sure that the initial data is properly returned for a node directly connected to a inner metanode output
     * port (UIEXT-777).
     */
    @Test
    void callInitialDataServiceForNodeConnectedToMetanodeParent() {
        var metanode = m_wfm.createAndAddSubWorkflow(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE}, "Metanode");
        var nnc = createAndAddNode(metanode, new NodeDialogNodeFactory(
            () -> createNodeDialog(Page.create().fromString(() -> "page content").relativePath("index.html")), 1));
        metanode.addConnection(metanode.getID(), 0, nnc.getID(), 1);

        assertThat(NodeDialogManager.getInstance().getDataServiceManager().callInitialDataService(NodeWrapper.of(nnc)))
            .isEqualTo("{\"result\":\"test settings\"}");
    }

    private static NodeSettingsRO getNodeViewSettings(final NodeContainer nc) {
        return ((NodeDialogNodeView)NodeViewManagerTest.getNodeView(nc)).getLoadNodeSettings();
    }

    /**
     * Helper to create a node with a {@link NodeDialog}.
     *
     * @param wfm the workflow to create the node in
     * @param nodeDialogCreator function to create the node dialog instance
     * @return the newly created node container
     */
    public static NativeNodeContainer createNodeWithNodeDialog(final WorkflowManager wfm,
        final Supplier<NodeDialog> nodeDialogCreator) {
        return createAndAddNode(wfm, new NodeDialogNodeFactory(nodeDialogCreator));
    }

    private static NativeNodeContainer createNodeWithNodeDialog(final WorkflowManager wfm,
        final Supplier<NodeDialog> nodeDialogCreator, final BooleanSupplier hasDialog) {
        return createAndAddNode(wfm, new NodeDialogNodeFactory(nodeDialogCreator, hasDialog));
    }

    public static class TestService {

        public String method(final String param) {
            return param;
        }

    }

}
