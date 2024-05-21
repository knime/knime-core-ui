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
 *   Sep 13, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.knime.core.webui.node.view.NodeViewTest.createNodeView;
import static org.knime.core.webui.node.view.NodeViewTest.createTableView;
import static org.knime.core.webui.page.PageTest.BUNDLE_ID;
import static org.knime.testing.util.WorkflowManagerUtil.createAndAddNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.interactive.ReExecutable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.node.view.table.selection.SelectionTranslationService;
import org.knime.core.webui.page.Page;
import org.knime.testing.node.view.NodeViewNodeFactory;
import org.knime.testing.node.view.NodeViewNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link NodeViewManager}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class NodeViewManagerTest {

    private WorkflowManager m_wfm;

    /**
     * Clears the caches and files of the {@link NodeViewManager}.
     */
    @BeforeEach
    @AfterEach
    public void clearNodeViewManagerCachesAndFiles() {
        NodeViewManager.getInstance().clearCaches();
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
     * Tests multiple {@link NodeViewManager}-methods using a simple node view.
     */
    @Test
    void testSimpleNodeViewNode() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var hasView = new AtomicBoolean(true);
        NativeNodeContainer nc = createNodeWithNodeView(m_wfm, m -> createNodeView(page), hasView::get);
        m_wfm.executeAllAndWaitUntilDone();

        assertThat(NodeViewManager.hasNodeView(nc)).as("node expected to have a node view").isTrue();
        var nodeView = NodeViewManager.getInstance().getNodeView(nc);
        assertThat(nodeView.getPage() == page).isTrue();

        final var dataServiceManager = NodeViewManager.getInstance().getDataServiceManager();
        Assertions.assertThatThrownBy(() -> dataServiceManager.callInitialDataService(NodeWrapper.of(nc)))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("No initial data service available");
        assertThat(nodeView.getPage().isCompletelyStatic()).isFalse();
        assertThat(NodeViewManager.getInstance().getPageResourceManager().getPageId(NodeWrapper.of(nc)))
            .isEqualTo(nc.getID().toString().replace(":", "_"));

        hasView.set(false);
        assertThat(NodeViewManager.hasNodeView(nc)).as("node not expected to have a node view").isFalse();
    }

    /**
     * Makes sure that view settings are loaded from the node into the node view when created for the first time.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testLoadViewSettingsOnViewCreation() throws InvalidSettingsException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        AtomicReference<NodeSettingsRO> loadedNodeSettings = new AtomicReference<>();
        NativeNodeContainer nc = createNodeWithNodeView(m_wfm, m -> new NodeView() { // NOSONAR

            @Override
            public Optional<InitialDataService<?>> createInitialDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<RpcDataService> createRpcDataService() {
                return Optional.empty();
            }

            @Override
            public Optional<ApplyDataService<?>> createApplyDataService() {
                return Optional.empty();
            }

            @Override
            public Page getPage() {
                return page;
            }

            @Override
            public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                //
            }

            @Override
            public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
                assertThat(NodeContext.getContext().getNodeContainer()).isNotNull();
                loadedNodeSettings.set(settings);
            }

        });

        // prepare view settings
        var settings = new NodeSettings("node_settings");
        m_wfm.saveNodeSettings(nc.getID(), settings);
        var viewSettings = new NodeSettings("view");
        viewSettings.addString("view setting key", "view setting value");
        settings.addNodeSettings(viewSettings);
        settings.addNodeSettings(new NodeSettings("model"));
        m_wfm.loadNodeSettings(nc.getID(), settings);

        // test
        NodeViewManager.getInstance().updateNodeViewSettings(nc);
        assertThat(loadedNodeSettings.get().containsKey("view setting key")).isTrue();
    }

    /**
     * Tests {@link NodeViewManager#getPagePath(NodeWrapper)}.
     */
    @Test
    void testGetNodeViewPagePath() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        var nnc = NodeWrapper.of(createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage)));
        var nnc2 = NodeWrapper.of(createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage)));
        var nnc3 = NodeWrapper.of(createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage)));
        m_wfm.executeAllAndWaitUntilDone();
        var pageResourceManager = NodeViewManager.getInstance().getPageResourceManager();
        String path = pageResourceManager.getPagePath(nnc);
        String path2 = pageResourceManager.getPagePath(nnc2);
        pageResourceManager.clearPageCache();
        String path3 = pageResourceManager.getPagePath(nnc3);
        String path4 = pageResourceManager.getPagePath(nnc3);
        assertThat(path).as("path of static pages not expected to change").isEqualTo(path2);
        assertThat(path).as("path of dynamic pages expected to change between node instances").isNotEqualTo(path4);
        assertThat(path3)
            .as("path of dynamic pages not expected to change for same node instance (without node state change)")
            .isEqualTo(path4);
        assertThat(path).isEqualTo("uiext-view/"
            + NodeViewNodeFactory.getNodeWrapperTypeIdStatic((NativeNodeContainer)nnc.get()) + "/page.html");
        String baseUrl = pageResourceManager.getBaseUrl();
        assertThat(baseUrl).isEqualTo("http://org.knime.core.ui.view/");
    }

    /**
     * Tests {@link PageResourceManager#getPagePath(NodeWrapper)}.
     */
    @Test
    void testGetNodeViewPageResource() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        var dynamicPage = Page.builder(() -> "page content", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        var nnc = createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage));
        var nnc2 = createNodeWithNodeView(m_wfm, m -> createNodeView(dynamicPage));
        m_wfm.executeAllAndWaitUntilDone();

        var pageResourceManager = NodeViewManager.getInstance().getPageResourceManager();
        assertThat(pageResourceManager.getPagePath(NodeWrapper.of(nnc)))
            .isEqualTo("uiext-view/" + NodeViewNodeFactory.getNodeWrapperTypeIdStatic(nnc) + "/page.html");

        String path = pageResourceManager.getPagePath(NodeWrapper.of(nnc));
        assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1);
        var resourcePrefix1 = "uiext-view/" + NodeViewNodeFactory.getNodeWrapperTypeIdStatic(nnc);
        assertThat(path).isEqualTo(resourcePrefix1 + "/page.html");
        testGetNodeViewPageResource(resourcePrefix1);

        pageResourceManager.clearPageCache();

        String path2 = pageResourceManager.getPagePath(NodeWrapper.of(nnc2));
        assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1);
        var resourcePrefix2 = "uiext-view/" + nnc2.getID().toString().replace(":", "_") + "/"
            + System.identityHashCode(nnc2.getNodeAndBundleInformation());
        assertThat(path2).isEqualTo(resourcePrefix2 + "/page.html");
        testGetNodeViewPageResource(resourcePrefix2);

        m_wfm.removeNode(nnc.getID());
        // make sure that the pages are removed from the cache after the node has been deleted)
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1));
    }

    private static void testGetNodeViewPageResource(final String resourcePrefix) {
        var pageResourceManager = NodeViewManager.getInstance().getPageResourceManager();
        assertThat(pageResourceManager.getPageResource(resourcePrefix + "/page.html")).isPresent();
        assertThat(pageResourceManager.getPageResource(resourcePrefix + "/resource.html")).isPresent();
        assertThat(pageResourceManager.getPageResource("/test")).isEmpty();
        assertThat(pageResourceManager.getPageResource("test")).isEmpty();
        assertThat(pageResourceManager.getPageResource("test/test")).isEmpty();
    }

    /**
     * Tests {@link NodeViewManager#hasNodeView(org.knime.core.node.workflow.NodeContainer)} and
     * {@link NodeViewManager#getNodeView(org.knime.core.node.workflow.NodeContainer)} for a node without a node view.
     */
    @Test
    void testNodeWithoutNodeView() {
        NativeNodeContainer nc = createAndAddNode(m_wfm, new VirtualSubNodeInputNodeFactory(null, new PortType[0]));
        assertThat(NodeViewManager.hasNodeView(nc)).as("node not expected to have a node view").isFalse();
        Assertions.assertThatThrownBy(() -> NodeViewManager.getInstance().getNodeView(nc))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Makes sure that the page-cache is cleaned up after node removal or closing the workflow for dynamic pages.
     *
     * @throws URISyntaxException
     */
    @Test
    void testNodeCleanUpDynamicPage() throws URISyntaxException {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nc = NodeWrapper.of(createNodeWithNodeView(m_wfm, m -> createNodeView(page)));
        var nodeViewManager = NodeViewManager.getInstance();

        // remove node
        nodeViewManager.getPageResourceManager().getPagePath(nc);
        assertThat(nodeViewManager.getNodeViewMapSize()).isEqualTo(1);
        assertThat(nodeViewManager.getPageResourceManager().getPageCacheSize()).isEqualTo(1);
        m_wfm.removeNode(nc.get().getID());
        untilAsserted(() -> {
            assertThat(nodeViewManager.getNodeViewMapSize()).isZero();
            assertThat(nodeViewManager.getPageResourceManager().getPageCacheSize()).isEqualTo(0);
        });

        // close workflow
        nodeViewManager.getPageResourceManager().getPagePath(nc);
        assertThat(nodeViewManager.getNodeViewMapSize()).isEqualTo(1);
        assertThat(nodeViewManager.getPageResourceManager().getPageCacheSize()).isEqualTo(1);
        m_wfm.getParent().removeProject(m_wfm.getID());
        untilAsserted(() -> {
            assertThat(nodeViewManager.getNodeViewMapSize()).isZero();
            assertThat(nodeViewManager.getPageResourceManager().getPageCacheSize()).isEqualTo(0);
        });
    }

    /**
     * Makes sure that the page-cache is cleaned up after node removal or closing the workflow for static pages.
     */
    @Test
    void testNodeCleanUpStaticPage() {
        var staticPage = Page.builder(BUNDLE_ID, "files", "page.html").build();
        var nc = NodeWrapper.of(createNodeWithNodeView(m_wfm, m -> createNodeView(staticPage)));
        var pageResourceManager = NodeViewManager.getInstance().getPageResourceManager();

        // remove node
        pageResourceManager.getPagePath(nc);
        assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1);
        m_wfm.removeNode(nc.get().getID());
        untilAsserted(() -> assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1));

        // close workflow
        pageResourceManager.getPagePath(nc);
        assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1);
        m_wfm.getParent().removeProject(m_wfm.getID());
        untilAsserted(() -> assertThat(pageResourceManager.getPageCacheSize()).isEqualTo(1));
    }

    private static void untilAsserted(final ThrowingRunnable assertion) {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(assertion);
    }

    /**
     * Tests {@link DataServiceManager#callInitialDataService(NodeWrapper))},
     * {@link DataServiceManager#callRpcDataService(NodeWrapper, String))} and
     * {@link DataServiceManager#callApplyDataService(NodeWrapper, String))}
     */
    @Nested
    class DataServiceCallsTest {

        private NodeWrapper m_nc;

        @BeforeEach
        @SuppressWarnings({"unchecked", "rawtypes"})
        void constructNodeWrapperWithDataServices() {
            var page = Page.builder(() -> "test page content", "index.html").build();
            Function<NodeViewNodeModel, NodeView> nodeViewCreator =
                m -> createNodeView(page, InitialDataService.builder(() -> "init service").build(),
                    () -> RpcDataService.builder(new TestService()).build(),
                    ApplyDataService.builder((ReExecutable)(d, b) -> {
                        throw new UnsupportedOperationException("re-execute data service");
                    }).build());

            m_nc = NodeWrapper.of(NodeViewManagerTest.createNodeWithNodeView(m_wfm, nodeViewCreator));
        }

        @Test
        void testCallDataServices() {
            var dataServiceManager = NodeViewManager.getInstance().getDataServiceManager();
            assertThat(dataServiceManager.callInitialDataService(m_nc)).isEqualTo("{\"result\":\"init service\"}");
            assertRpcDataServiceCall(dataServiceManager, m_nc, "test param", 1);
            Assertions.assertThatThrownBy(() -> dataServiceManager.callApplyDataService(m_nc, "ERROR,test"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Can't reexecute executing nodes.");
        }

        @Test
        void testResetsRpcDataServiceOnInitialDataServiceCall() {
            var dataServiceManager = NodeViewManager.getInstance().getDataServiceManager();
            dataServiceManager.callInitialDataService(m_nc);
            assertRpcDataServiceCall(dataServiceManager, m_nc, "test param", 1);
            assertRpcDataServiceCall(dataServiceManager, m_nc, "test param", 2);
            dataServiceManager.callInitialDataService(m_nc);
            assertRpcDataServiceCall(dataServiceManager, m_nc, "test param", 1);
        }

        private static void assertRpcDataServiceCall(final DataServiceManager<NodeWrapper> dataServiceManager,
            final NodeWrapper nc, final String param, final int expectedNumberOfCalls) {
            assertThat(dataServiceManager.callRpcDataService(nc, RpcDataService.jsonRpcRequest("method", param)))
                .contains(
                    String.format("\"result\":\"%s\"", TestService.methodWithNumber(param, expectedNumberOfCalls)));
        }

    }

    /**
     * Tests {@link NodeViewManager#callSelectionTranslationService(NodeContainer, Set)} and
     * {@link NodeViewManager#callSelectionTranslationService(NodeContainer, List)}.
     */
    @Test
    void testCallSelectionTranslationService() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nodeView = createTableView(page, null, null, null, new SelectionTranslationService() {
            @Override
            public Set<RowKey> toRowKeys(final List<String> selection) throws IOException {
                throw new IOException(selection.toString());
            }

            @Override
            public List<String> fromRowKeys(final Set<RowKey> rowKeys) throws IOException {
                throw new IOException(rowKeys.toString());
            }
        });
        var nc = NodeViewManagerTest.createNodeWithNodeView(m_wfm, m -> nodeView);

        var nw = NodeWrapper.of(nc);
        final var tableViewManager = NodeViewManager.getInstance().getTableViewManager();
        final var invalidSelection = Collections.singletonList("foo");
        assertThatThrownBy(() -> tableViewManager.callSelectionTranslationService(nw, invalidSelection))
            .isInstanceOf(IOException.class).hasMessage("[foo]");
        final var invalidRowKeys = Set.of(new RowKey("bar"));
        assertThatThrownBy(() -> tableViewManager.callSelectionTranslationService(nw, invalidRowKeys))
            .isInstanceOf(IOException.class).hasMessage("[bar]");

    }

    /**
     * Tests {@link NodeViewManager#callSelectionTranslationService(NodeContainer, Set)} and
     * {@link NodeViewManager#callSelectionTranslationService(NodeContainer, List)}.
     */
    @Test
    void testCallSelectionTranslationServiceThrowsIfNotATableView() {
        var page = Page.builder(() -> "test page content", "index.html").build();
        var nodeView = createNodeView(page);
        var nc = NodeViewManagerTest.createNodeWithNodeView(m_wfm, m -> nodeView);

        var nw = NodeWrapper.of(nc);
        assertThatThrownBy(() -> NodeViewManager.getInstance().getTableViewManager().callSelectionTranslationService(nw,
            Collections.singletonList("foo"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NodeViewManager.getInstance().getTableViewManager().callSelectionTranslationService(nw,
            Set.of(new RowKey("bar")))).isInstanceOf(IllegalArgumentException.class);

    }

    /**
     * Tests {@link PageIdType#getIdForNodeExecutionCycle(NativeNodeContainer)}.
     */
    @Test
    void testGetIdForNodeExecutionCycle() {
        var nnc = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 0));
        m_wfm.executeAllAndWaitUntilDone();

        // make sure that the 'node execution cycle id' changes with every re-execution
        assertThat(nnc.getNodeContainerState().isExecuted()).isTrue();
        var idForNodeExecutionCycle = NodeViewManager.getIdForNodeExecutionCycle(nnc);
        assertThat(NodeViewManager.getIdForNodeExecutionCycle(nnc)).isEqualTo(idForNodeExecutionCycle);
        m_wfm.resetAndConfigureAll();
        m_wfm.executeAllAndWaitUntilDone();
        assertThat(NodeViewManager.getIdForNodeExecutionCycle(nnc)).isNotEqualTo(idForNodeExecutionCycle);
    }

    /**
     * Tests {@link NodeViewManager#getInputDataTableSpecIfTableView(NodeContainer)}.
     */
    @Test
    void testGetInputDataTableSpecIfTableView() {
        var sourceNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new NodeViewNodeFactory(0, 1));
        var metanode =
            m_wfm.createAndAddSubWorkflow(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{}, "metanode");
        var viewNode = WorkflowManagerUtil.createAndAddNode(metanode, new NodeViewNodeFactory(1, 1));
        var viewNode2 = WorkflowManagerUtil.createAndAddNode(metanode, new NodeViewNodeFactory(1, 0));
        m_wfm.addConnection(sourceNode.getID(), 1, metanode.getID(), 0);
        metanode.addConnection(metanode.getID(), 0, viewNode.getID(), 1);
        metanode.addConnection(viewNode.getID(), 1, viewNode2.getID(), 1);

        // view node directly connected to inner metanode input port
        assertThat(NodeViewManager.getInstance().getInputDataTableSpecIfTableView(viewNode).get())
            .isEqualTo(sourceNode.getOutPort(1).getPortObjectSpec());

        assertThat(NodeViewManager.getInstance().getInputDataTableSpecIfTableView(viewNode2).get())
            .isEqualTo(viewNode.getOutPort(1).getPortObjectSpec());
    }

    /**
     * Helper to create a node with a {@link NodeView}.
     *
     * @param wfm the workflow to create the node in
     * @param nodeViewCreator function to create the node view instance
     * @return the newly created node container
     */
    public static NativeNodeContainer createNodeWithNodeView(final WorkflowManager wfm,
        final Function<NodeViewNodeModel, NodeView> nodeViewCreator) {
        return createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator));
    }

    private static NativeNodeContainer createNodeWithNodeView(final WorkflowManager wfm,
        final Function<NodeViewNodeModel, NodeView> nodeViewCreator, final BooleanSupplier hasView) {
        return createAndAddNode(wfm, new NodeViewNodeFactory(nodeViewCreator, hasView));
    }

    public static class TestService {

        int methodCalls;

        public String method(final String param) {
            methodCalls += 1;
            return methodWithNumber(param, methodCalls);
        }

        public static String methodWithNumber(final String param, final int numMethodCalls) {
            return String.format("param: %s, methodCalls: %s", param, numMethodCalls);
        }

    }

    /**
     * Helper to give access to a node's {@link NodeView} for testing.
     *
     * @param nc
     * @return the node view instance
     */
    public static NodeView getNodeView(final NodeContainer nc) {
        return NodeViewManager.getInstance().getNodeView(nc);
    }

}
