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

import java.util.Map;

import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.webui.node.dialog.defaultdialog.components.SubNodeContainerNodeDialog;

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

    /**
     * Depending on the node container, it is determined if a webUI node dialog is present.
     *
     * @return whether a webUI dialog can be displayed for this component
     */
    public boolean hasNodeDialog() {
        return isSubNodeContainerNodeDialogEnabled() && !getConfigurationNodes(m_snc).isEmpty();
    }

    /**
     * @return Create the dialog containing all the dialog elements that were found in the {@link SubNodeContainer}
     */
    public NodeDialog createNodeDialog() {
        return new SubNodeContainerNodeDialog(m_snc);
    }

    /**
     * Get all configuration nodes from a subNodeContainer
     *
     * @param snc the container
     * @return a map from nodeId to configuration nodes
     */
    @SuppressWarnings("rawtypes")
    public static Map<NodeID, DialogNode> getConfigurationNodes(final SubNodeContainer snc) {
        var wfm = snc.getWorkflowManager();
        Map<NodeID, DialogNode> nodes = wfm.findNodes(DialogNode.class, new NodeModelFilter<DialogNode>() { // NOSONAR
            @Override
            public boolean include(final DialogNode nodeModel) {
                return !nodeModel.isHideInDialog();
            }
        }, false);
        return nodes;
    }

}
