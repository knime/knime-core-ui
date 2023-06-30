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
 *   Jul 18, 2022 (hornm): created
 */
package org.knime.gateway.api.entity;

import org.knime.core.webui.node.NodePortWrapper;
import org.knime.core.webui.node.PageResourceManager.PageType;
import org.knime.core.webui.node.port.PortView;
import org.knime.core.webui.node.port.PortViewManager;

/**
 * Port view entity containing the info required by the UI (i.e. frontend) to be able to display a port view.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class PortViewEnt extends NodeUIExtensionEnt<NodePortWrapper> {

    private final PortView m_portView;

    /**
     * @param wrapper
     * @param manager
     * @param portView
     */
    // Note on why we need to _pass_ a PortView-instance here instead of requesting it internally:
    // It helps to avoid problems of deleting the respective node (or closing the workflow) while the
    // PortViewEnt is being instantiated (which can take some time, e.g., when calculating statistics
    // which is delivered with the initial data). With the change we avoid the creation of
    // PortView-instances again (because it's now passed in).
    public PortViewEnt(final NodePortWrapper wrapper, final PortViewManager manager, final PortView portView) {
        super(wrapper, manager, manager, PageType.PORT);
        m_portView = portView;
    }

    /**
     * @return custom styling of the iframe that displays the port view's page
     */
    public String getIFrameStyle() {
        var dims = m_portView.getDimension().orElse(null);
        var width = dims == null ? "100%" : (dims.widthInPx() + "px");
        var height = dims == null ? "100%" : (dims.heightInPx() + "px");
        return String.format("border:none;width:%s;height:%s;", width, height);
    }

}
