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
 *   Aug 10, 2022 (benjamin): created
 */
package org.knime.scripting.editor;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer;

/**
 * A utility to control the workflow of a scripting node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class WorkflowControl {

    private final NodeContainer m_nc;

    WorkflowControl(final NodeContainer nc) {
        m_nc = nc;
    }

    /**
     * @return the node container
     */
    public NodeContainer getNC() {
        return m_nc;
    }

    /**
     * @return the inputs for the node
     */
    public PortObject[] getInputData() {
        // TODO(AP-19333) handle if source nodes are not executed
        // TODO(AP-19333) handle ports that are not yet loaded
        // TODO(AP-19333) handle flow variables
        final var wfm = m_nc.getParent();

        final PortObject[] inData = new PortObject[m_nc.getNrInPorts() - 1];
        for (var cc : wfm.getIncomingConnectionsFor(m_nc.getID())) {
            if (cc.getDestPort() == 0) {
                // Flow variable port
                continue;
            }
            inData[cc.getDestPort() - 1] =
                wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()).getPortObject();
        }
        return inData;
    }

    /**
     * @return the input specs for the node
     */
    public PortObjectSpec[] getInputSpec() {
        // TODO(AP-19333) combine with getInputData?
        // TODO(AP-19333) handle ports that are not loaded yet
        // TODO(AP-19333) handle inputs without specs
        final var wfm = m_nc.getParent();

        final PortObjectSpec[] inSpecs = new PortObjectSpec[m_nc.getNrInPorts() - 1];
        for (var cc : wfm.getIncomingConnectionsFor(m_nc.getID())) {
            if (cc.getDestPort() == 0) {
                // Flow variable port
                continue;
            }
            inSpecs[cc.getDestPort() - 1] =
                wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()).getPortObjectSpec();
        }
        return inSpecs;
    }
}
