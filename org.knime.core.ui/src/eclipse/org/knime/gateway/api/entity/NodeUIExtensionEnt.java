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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerParent;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.page.PageUtil.PageType;

/**
 * Super classes for node-ui-extension entities, e.g., node view and node dialog.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <N> TODO
 */
public class NodeUIExtensionEnt<N extends NodeWrapper> {

    private final String m_nodeId;

    private final String m_workflowId;

    private final String m_projectId;

    private final String m_initialData;

    private final ResourceInfoEnt m_resourceInfo;

    private final PageType m_pageType;

    /**
     * @param nodeWrapper
     * @param pageResourceManager
     * @param dataServiceManager
     * @param pageType
     */
    protected NodeUIExtensionEnt(final N nodeWrapper, final PageResourceManager<N> pageResourceManager,
        final DataServiceManager<N> dataServiceManager, final PageType pageType) {
        var nc = nodeWrapper.get();
        WorkflowManager wfm = nc.getParent();
        WorkflowManager projectWfm = wfm.getProjectWFM();

        m_projectId = projectWfm.getNameWithID();

        NodeContainerParent ncParent = wfm.getDirectNCParent();
        boolean isComponentProject = projectWfm.isComponentProjectWFM();
        if (ncParent instanceof SubNodeContainer) {
            // it's a component's workflow
            m_workflowId = new NodeIDEnt(((SubNodeContainer)ncParent).getID(), isComponentProject).toString();
        } else {
            m_workflowId = new NodeIDEnt(wfm.getID(), isComponentProject).toString();
        }
        m_nodeId = new NodeIDEnt(nc.getID(), isComponentProject).toString();

        if (dataServiceManager != null
            && dataServiceManager.getDataServiceOfType(nodeWrapper, TextInitialDataService.class).isPresent()) {
            m_initialData = dataServiceManager.callTextInitialDataService(nodeWrapper);
        } else {
            m_initialData = null;
        }

        if (pageResourceManager != null) {
            var baseUrl = pageResourceManager.getBaseUrl().orElse(null);
            var debugUrl = pageResourceManager.getDebugUrl(nodeWrapper).orElse(null);
            var path = pageResourceManager.getPagePath(nodeWrapper);
            var page = pageResourceManager.getPage(nodeWrapper);
            String id = pageResourceManager.getPageId(nodeWrapper, page);
            m_resourceInfo = new ResourceInfoEnt(id, baseUrl, debugUrl, path, page.getContentType());
        } else {
            m_resourceInfo = null;
        }

        m_pageType = pageType;
    }

    /**
     * @return the workflow project id
     */
    public final String getProjectId() {
        return m_projectId;
    }

    /**
     * @return the id of the (sub-)workflow
     */
    public final String getWorkflowId() {
        return m_workflowId;
    }

    /**
     * @return the id of the node
     */
    public final String getNodeId() {
        return m_nodeId;
    }

    /**
     * @return see {@link ResourceInfoEnt}
     */
    public final ResourceInfoEnt getResourceInfo() {
        return m_resourceInfo;
    }

    /**
     * @return initial data
     */
    public String getInitialData() {
        return m_initialData;
    }

    /**
     * @return the type of the node ui extension
     */
    public String getExtensionType() {
        return m_pageType.toString();
    }

}
