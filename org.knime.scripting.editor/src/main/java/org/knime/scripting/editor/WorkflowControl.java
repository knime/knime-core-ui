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

import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;

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
     * @return the number of input port of the node
     */
    public int getNrInPorts() {
        return m_nc.getNrInPorts();
    }

    /**
     * @return the number of output ports of the node
     */
    public int getNrOutPorts() {
        return m_nc.getNrOutPorts();
    }

    public NodeModel getNodeModel() {
        return ((NativeNodeContainer)m_nc).getNodeModel();
    }

    /**
     * @return the {@link FlowObjectStack} that is available at this node
     */
    public FlowObjectStack getFlowObjectStack() {
        return m_nc.getFlowObjectStack();
    }

    private static interface ConnectedPortConsumer {
        void accept(int destPortIdx, NodeOutPort outPort);
    }

    private void runForEachIncomingConnection(final ConnectedPortConsumer fn) {
        final var wfm = m_nc.getParent();
        for (var cc : wfm.getIncomingConnectionsFor(m_nc.getID())) {
            fn.accept(cc.getDestPort(), wfm.getNodeContainer(cc.getSource()).getOutPort(cc.getSourcePort()));
        }
    }

    /**
     * @return The inputs for the node (excluding the flow variable port). For ports with no port objects available the
     *         value will be <code>null</code>.
     */
    public PortObject[] getInputData() {
        final var inData = new PortObject[m_nc.getNrInPorts() - 1];
        runForEachIncomingConnection((destPortIdx, outPort) -> {
            if (destPortIdx == 0) {
                // Flow variable port
                return;
            }
            inData[destPortIdx - 1] = outPort.getPortObject();
        });
        return inData;
    }

    /**
     * @return The input specs for the node (excluding the flow variable port). For ports with no specs available the
     *         value will be <code>null</code>.
     */
    public PortObjectSpec[] getInputSpec() {
        final var inSpecs = new PortObjectSpec[m_nc.getNrInPorts() - 1];
        runForEachIncomingConnection((destPortIdx, outPort) -> {
            if (destPortIdx == 0) {
                // Flow variable port
                return;
            }
            inSpecs[destPortIdx - 1] = outPort.getPortObjectSpec();
        });
        return inSpecs;
    }

    public InputPortInfo[] getInputInfo() {
    }

    public record InputPortInfo(PortType type, Optional<PortObjectSpec> spec) {
    }

    /**
     * @return the output port types for the node
     */
    public PortType[] getOutputPortTypes() {
        return IntStream.range(0, m_nc.getNrOutPorts()) //
            .mapToObj(i -> m_nc.getOutPort(i).getPortType()) //
            .toArray(PortType[]::new);
    }

    /**
     * @return the input port types for the node
     */
    public PortType[] getInputPortTypes() {
        // TODO still needed? Rewrite to one-liner
        final int numInputPorts = m_nc.getNrInPorts();
        final PortType[] inputPortTypes = new PortType[numInputPorts];
        for (int i = 0; i < numInputPorts; i++) {
            inputPortTypes[i] = m_nc.getInPort(i).getPortType();
        }
        return inputPortTypes;
    }

}
