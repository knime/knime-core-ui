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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.webui.UIExtension;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.PagePathSegments;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.node.PageResourceManager.CreatedPage;
import org.knime.core.webui.node.PageResourceManager.PageType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil;
import org.knime.core.webui.node.util.NodeCleanUpCallback;

/**
 * Manages (web-ui) node dialog instances and provides associated functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class NodeDialogManager {

    private static NodeDialogManager instance;

    /**
     * Registry mapping node factory class names to dialog factories. Used to provide WebUI dialogs for nodes whose
     * factories are in bundles that cannot depend on knime-core-ui (e.g. knime-core). Registration happens from
     * GatewayImplPlugin to avoid early class loading.
     */
    private static final Map<String, NodeDialogFactory> REGISTERED_DIALOG_FACTORIES = new HashMap<>();

    private final Map<NodeContainer, DialogUIExtension> m_nodeDialogAdapterMap = new WeakHashMap<>();

    private final PageResourceManager<NodeWrapper> m_pageResourceManager = new PageResourceManager<>(PageType.DIALOG,
        nw -> new CreatedPage(getNodeDialog(nw.getNCUI()).getUIExtension().getPage(),
            PagePathSegments.getStaticPageId(nw.getNCUI())));

    private final DataServiceManager<NodeWrapper> m_dataServiceManager =
        new DataServiceManager<>(nw -> getNodeDialog(nw.get()).getDataServiceProvider());

    /**
     * Returns the singleton instance for this class.
     *
     * @return the singleton instance
     */
    public static synchronized NodeDialogManager getInstance() {
        if (instance == null) {
            instance = new NodeDialogManager();
        }
        return instance;
    }

    private NodeDialogManager() {
        // singleton
    }

    /**
     * Registers a {@link NodeDialogFactory} for a node factory identified by its class name. This is used to provide
     * WebUI dialogs for node factories that cannot implement {@link NodeDialogFactory} directly (e.g. because they
     * reside in a bundle that cannot depend on knime-core-ui).
     *
     * @param factoryClassName the fully qualified class name of the node factory
     * @param dialogFactory the dialog factory to use for nodes created by that factory
     */
    public static synchronized void registerNodeDialogFactory(final String factoryClassName,
        final NodeDialogFactory dialogFactory) {
        REGISTERED_DIALOG_FACTORIES.put(factoryClassName, dialogFactory);
    }

    /**
     * @param nc the node to check
     * @return whether the node provides a node dialog
     */
    public static boolean hasNodeDialog(final NodeContainer nc) {
        if (nc instanceof NativeNodeContainer nnc) {
            final var node = nnc.getNode();
            if (FallbackDialogFactory.isFallbackDialogEnabled() && node.hasDialog()) {
                return true;
            }
            var nodeFactory = node.getFactory();
            if (nodeFactory instanceof NodeDialogFactory nodeDialogFactory && nodeDialogFactory.hasNodeDialog()) {
                return true;
            }
            if (REGISTERED_DIALOG_FACTORIES.containsKey(nodeFactory.getClass().getName())) {
                return true;
            }
            return JobManagerParametersUtil.hasJobManagerSettings(nc.getNodeSettings());
        } else if (nc instanceof SubNodeContainer snc) {
            return new SubNodeContainerDialogFactory(snc).hasNodeDialog();
        } else {
            return false;
        }
    }

    interface DialogUIExtension {

        /**
         * @return the {@link UIExtension} for this dialog
         */
        UIExtension getUIExtension();

        /**
         * @return the {@link DataServiceProvider} for this dialog
         */
        DataServiceProvider getDataServiceProvider();

        /**
         * Indicates whether the dialog (optionally) wants/needs to be displayed in an enlarged display area.
         *
         * @return if {@code true} if it can be enlarged, otherwise {@code false}
         */
        default boolean canBeEnlarged() {
            return false;
        }

    }

    /**
     * Gets the {@link NodeContainerNodeDialogAdapter} for a given node.
     *
     * @param nc the node to create the node dialog from
     * @return a node dialog instance
     * @throws IllegalArgumentException if the passed node does not provide a node dialog
     */
    DialogUIExtension getNodeDialog(final NodeContainer nc) {
        if (!hasNodeDialog(nc)) {
            throw new IllegalArgumentException("The node " + nc.getNameWithID() + " doesn't provide a node dialog");
        }

        if (nc instanceof NativeNodeContainer nnc) {
            return m_nodeDialogAdapterMap.computeIfAbsent(nc, id -> {
                NodeCleanUpCallback.builder(nnc, () -> removeNodeDialogAdapter(nnc)).build();
                return createNativeNodeDialog(nnc);
            });
        } else if (nc instanceof SubNodeContainer snc) {
            return m_nodeDialogAdapterMap.computeIfAbsent(nc, id -> {
                NodeCleanUpCallback.builder(nc, () -> removeNodeDialogAdapter(nc)).build();
                return createSubNodeContainerDialog(snc);
            });
        } else {
            throw new IllegalArgumentException("The node " + nc.getNameWithID() + " is no supported node container");
        }
    }

    /**
     * Gets the {@link NodeContainerNodeDialogAdapter} for a given {@link NodeContainerUI} - need in order to make the
     * dialogs work in the remote workflow editor.
     *
     * @param nc the node to create the node dialog from
     * @return a node dialog instance
     * @throws IllegalArgumentException if the passed node does not provide a node dialog
     */
    DialogUIExtension getNodeDialog(final NodeContainerUI ncUI) {
        var nc = Wrapper.unwrapOptional(ncUI, NodeContainer.class).orElse(null);
        if (nc != null) {
            return getNodeDialog(nc);
        } else if (ncUI instanceof NativeNodeContainerUI nncUI) {
            return nncUI.getNodeDialog().map(NodeContainerUINodeDialogAdapter::new)
                .orElseThrow(() -> new IllegalArgumentException(
                    "The node " + nncUI.getNameWithID() + " doesn't provide a node dialog"));
        } else {
            throw new IllegalArgumentException("The node " + ncUI.getNameWithID() + " is no supported node container");
        }
    }

    private void removeNodeDialogAdapter(final NodeContainer nnc) {
        m_nodeDialogAdapterMap.remove(nnc);
    }

    private static DialogUIExtension createNativeNodeDialog(final NativeNodeContainer nnc) {
        final var node = nnc.getNode();
        var fac = node.getFactory();
        NodeContext.pushContext(nnc);
        try {
            NodeDialog nodeDialog;
            if (fac instanceof NodeDialogFactory df) {
                nodeDialog = df.createNodeDialog();
            } else {
                var registeredFactory = REGISTERED_DIALOG_FACTORIES.get(fac.getClass().getName());
                if (registeredFactory != null) {
                    nodeDialog = registeredFactory.createNodeDialog();
                } else if (FallbackDialogFactory.isFallbackDialogEnabled() && node.hasDialog()) {
                    nodeDialog = new FallbackDialogFactory(nnc).createNodeDialog();
                } else {
                    nodeDialog = new DefaultNodeDialog();
                }
            }
            return new NodeContainerNodeDialogAdapter(nnc, nodeDialog);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private static DialogUIExtension createSubNodeContainerDialog(final SubNodeContainer snc) {
        NodeContext.pushContext(snc);
        try {
            return new NodeContainerNodeDialogAdapter(snc, new SubNodeContainerDialogFactory(snc).createNodeDialog());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * @return the {@link DataServiceManager} instance
     */
    public DataServiceManager<NodeWrapper> getDataServiceManager() {
        return m_dataServiceManager;
    }

    /**
     * @return the {@link PageResourceManager} instance
     */
    public PageResourceManager<NodeWrapper> getPageResourceManager() {
        return m_pageResourceManager;
    }

    /**
     * For testing purposes only.
     */
    void clearCaches() {
        m_nodeDialogAdapterMap.clear();
        m_pageResourceManager.clearPageCache();
    }

    /**
     * @param dialog
     *
     * @return a legacy flow variable node dialog
     */
    public static NodeDialogPane createLegacyFlowVariableNodeDialog(final NodeDialog dialog) {
        return new NodeContainerNodeDialogAdapter((NativeNodeContainer)NodeContext.getContext().getNodeContainer(),
            dialog).createLegacyFlowVariableNodeDialog();
    }

    /**
     * @param nc
     * @return see {@link NodeDialog#canBeEnlarged()}
     */
    public boolean canBeEnlarged(final NodeContainerUI nc) {
        return getNodeDialog(nc).canBeEnlarged();
    }

    /**
     * @param nc
     * @return see {@link NodeDialog#canBeEnlarged()}
     */
    public boolean canBeEnlarged(final NodeContainer nc) {
        return getNodeDialog(nc).canBeEnlarged();
    }

}
