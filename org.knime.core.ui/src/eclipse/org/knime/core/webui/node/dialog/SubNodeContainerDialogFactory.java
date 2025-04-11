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
 *   28 Jul 2022 (Carsten Haubold): created
 */
package org.knime.core.webui.node.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.util.ConfigurationLayoutUtil;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.SubNodeContainerSettingsService.DialogSubNode;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogUIExtension;

/**
 * The SubNodeContainerDialogFactory creates a {@link NodeDialog} for all the configuration nodes inside a
 * {@link SubNodeContainer} by parsing the {@link DialogNodeRepresentation}s of those nodes and converting those to
 * jsonforms which is parsed by the NodeDialog page.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class SubNodeContainerDialogFactory {

    private static final String SUB_NODE_CONTAINER_UI_MODE_PROPERTY = "org.knime.component.ui.mode";

    private static final String SUB_NODE_CONTAINER_UI_MODE_SWING = "swing";

    private static final String SUB_NODE_CONTAINER_UI_MODE_JS = "js";

    private static final String SUB_NODE_CONTAINER_UI_MODE_DEFAULT = SUB_NODE_CONTAINER_UI_MODE_SWING;

    private final SubNodeContainer m_snc;

    private static String getSubNodeContainerUiMode() {
        var mode = System.getProperty(SUB_NODE_CONTAINER_UI_MODE_PROPERTY);
        if (mode == null
            || (!mode.equals(SUB_NODE_CONTAINER_UI_MODE_SWING) && !mode.equals(SUB_NODE_CONTAINER_UI_MODE_JS))) {
            return SUB_NODE_CONTAINER_UI_MODE_DEFAULT;
        }
        return mode;
    }

    /**
     * @return true if JS-based {@link SubNodeContainer} {@link NodeDialog}s should be used
     */
    public static boolean isSubNodeContainerNodeDialogEnabled() {
        return SUB_NODE_CONTAINER_UI_MODE_JS.equals(getSubNodeContainerUiMode());
    }

    /**
     * Initialize a SubNodeContainerDialogFactory with the {@link SubNodeContainer} for which the dialog should be
     * constructed.
     *
     * @param snc The SubNodeContainer for which the dialog will be built
     */
    public SubNodeContainerDialogFactory(final SubNodeContainer snc) {
        m_snc = snc;
    }

    boolean hasNodeDialog() {
        return isSubNodeContainerNodeDialogEnabled() && !getConfigurationNodes(m_snc).isEmpty();
    }

    /**
     * @return Create the dialog containing all the dialog elements that were found in the {@link SubNodeContainer}
     */
    NodeDialog createNodeDialog() {
        return new SubNodeContainerNodeDialog(m_snc);
    }

    @SuppressWarnings("rawtypes")
    private static Map<NodeID, DialogNode> getConfigurationNodes(final SubNodeContainer snc) {
        var wfm = snc.getWorkflowManager();
        Map<NodeID, DialogNode> nodes = wfm.findNodes(DialogNode.class, new NodeModelFilter<DialogNode>() { // NOSONAR
            @Override
            public boolean include(final DialogNode nodeModel) {
                return !nodeModel.isHideInDialog();
            }
        }, false);
        return nodes;
    }

    private static class SubNodeContainerNodeDialog implements NodeDialog, DefaultNodeDialogUIExtension {

        final NodeSettingsService m_settingsService;

        SubNodeContainerNodeDialog(final SubNodeContainer snc) {
            m_settingsService = new SubNodeContainerSettingsService(() -> getOrderedConfigurationNodes(snc));
        }

        @Override
        public NodeSettingsService getNodeSettingsService() {
            return m_settingsService;
        }

        @Override
        public boolean canBeEnlarged() {
            return false;
        }

        private static List<DialogSubNode> getOrderedConfigurationNodes(final SubNodeContainer snc) {
            @SuppressWarnings("rawtypes")
            Map<NodeID, DialogNode> nodes = getConfigurationNodes(snc);
            return getNodeOrder(snc, nodes).stream().map(nodeId -> new DialogSubNode(nodes.get(nodeId), nodeId))
                .toList();
        }

        /**
         * Sort the dialog node IDs according to the user provided preference.
         *
         * Note: The ordering is requested each time the dialog is opened. Otherwise, the ordering would stay as it was
         * when the dialog was first created, because they are cached.
         */
        @SuppressWarnings("rawtypes")
        private static List<NodeID> getNodeOrder(final SubNodeContainer snc, final Map<NodeID, DialogNode> nodes) {
            List<Integer> order = ConfigurationLayoutUtil.getConfigurationOrder(
                snc.getSubnodeConfigurationLayoutStringProvider(), nodes, snc.getWorkflowManager());

            // Will contain the nodes in the ordering given by `order`.
            // Nodes not mentioned in `order` will be placed at the end in arbitrary order.
            TreeMap<Integer, NodeID> orderedNodeIDs = new TreeMap<>();
            List<NodeID> unorderedNodeIDs = new ArrayList<>(nodes.size());
            nodes.forEach((nodeId, node) -> {
                int targetIndex = order.indexOf(nodeId.getIndex());
                if (targetIndex == -1) {
                    unorderedNodeIDs.add(nodeId);
                } else {
                    orderedNodeIDs.put(targetIndex, nodeId);
                }
            });
            List<NodeID> res = new ArrayList<>();
            res.addAll(orderedNodeIDs.values()); // `values` is ordered
            res.addAll(unorderedNodeIDs);
            return res;
        }

        @Override
        public Set<SettingsType> getSettingsTypes() {
            return Set.of(SettingsType.MODEL);
        }

        @Override
        public Optional<RpcDataService> createRpcDataService() {
            return Optional.empty();
        }

    }

}
