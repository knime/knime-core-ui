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
package org.knime.core.webui.node.dialog.scripting;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import org.knime.core.node.NodeLogger;
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

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowControl.class);

    private final NodeContainer m_nc;

    /**
     * Create a new WorkflowControl for the given scripting node.
     *
     * @param nc the {@link NodeContainer} of the scripting node
     */
    public WorkflowControl(final NodeContainer nc) {
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

    /**
     * @return the {@link NodeModel} of the node
     */
    public NodeModel getNodeModel() {
        return ((NativeNodeContainer)m_nc).getNodeModel();
    }

    /**
     * @return the {@link FlowObjectStack} that is available at this node
     */
    public FlowObjectStack getFlowObjectStack() {
        return m_nc.getFlowObjectStack();
    }

    /**
     * @return The inputs for the node (excluding the flow variable port). For ports with no port objects available the
     *         value will be <code>null</code>.
     */
    public PortObject[] getInputData() {
        final var inData = new PortObject[getNrInPorts() - 1];
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
        final var inSpecs = new PortObjectSpec[getNrInPorts() - 1];
        runForEachIncomingConnection((destPortIdx, outPort) -> {
            if (destPortIdx == 0) {
                // Flow variable port
                return;
            }
            inSpecs[destPortIdx - 1] = outPort.getPortObjectSpec();
        });
        return inSpecs;
    }

    /**
     * @return the {@link InputPortInfo} for each input port of the node (excluding the flow variable port)
     */
    public InputPortInfo[] getInputInfo() {
        var inputTypes = getInputPortTypes();
        var inputSpec = getInputSpec();
        return IntStream.range(0, getNrInPorts() - 1) //
            .mapToObj(i -> new InputPortInfo(inputTypes[i + 1], inputSpec[i])) //
            .toArray(InputPortInfo[]::new);
    }

    /**
     * @return the output port types for the node
     */
    public PortType[] getOutputPortTypes() {
        return IntStream.range(0, getNrOutPorts()) //
            .mapToObj(i -> m_nc.getOutPort(i).getPortType()) //
            .toArray(PortType[]::new);
    }

    /**
     * @return the input port types for the node
     */
    public PortType[] getInputPortTypes() {
        return IntStream.range(0, getNrInPorts()) //
            .mapToObj(i -> m_nc.getInPort(i).getPortType()) //
            .toArray(PortType[]::new);
    }

    /**
     * States that an input port can be in.
     */
    public enum ConnectionStatus {
            /** The input is not connected */
            MISSING_CONNECTION,
            /** The input is connected, but the predecessor is not configured */
            UNCONFIGURED_CONNECTION,
            /** The input is connected and configured, but the predecessor is not executed */
            UNEXECUTED_CONNECTION,
            /** The input is connected, configured, and executed */
            OK
    }

    /**
     * A record to store the connection information of an input port.
     *
     * @param status the current status of the connection
     * @param isOptional flag to indicate if the port is optional
     *
     */
    public record InputConnectionInfo(ConnectionStatus status, boolean isOptional) {
    }

    /**
     * @return port information of each port.
     */
    public InputConnectionInfo[] getInputConnectionInfo() {
        var inputPortTypes = getInputPortTypes();

        var inputConnectionInfo = new InputConnectionInfo[getNrInPorts()];
        Arrays.setAll(inputConnectionInfo,
            i -> new InputConnectionInfo(ConnectionStatus.MISSING_CONNECTION, inputPortTypes[i].isOptional()));

        runForEachIncomingConnection((destPortIdx, outPort) -> inputConnectionInfo[destPortIdx] =
            new InputConnectionInfo(getPortConnectionStatus(outPort), inputPortTypes[destPortIdx].isOptional()));

        return inputConnectionInfo;
    }

    private static ConnectionStatus getPortConnectionStatus(final NodeOutPort outPort) {
        if (outPort.getPortObjectSpec() == null) {
            return ConnectionStatus.UNCONFIGURED_CONNECTION;
        }
        if (outPort.getPortObject() == null) {
            return ConnectionStatus.UNEXECUTED_CONNECTION;
        }
        return ConnectionStatus.OK;
    }

    /**
     * Information about an input port.
     *
     * @param portType the port type
     * @param portSpec the spec which might be <code>null</code> if the spec are not known
     */
    public static record InputPortInfo(PortType portType, PortObjectSpec portSpec) {
    }

    private interface ConnectedPortConsumer {
        void accept(int destPortIdx, NodeOutPort outPort);
    }

    private void runForEachIncomingConnection(final ConnectedPortConsumer fn) {
        final var wfm = m_nc.getParent();
        for (var cc : wfm.getIncomingConnectionsFor(m_nc.getID())) {
            var sourceNode = cc.getSource();
            var sourceIdx = cc.getSourcePort();
            var destIdx = cc.getDestPort();

            switch (cc.getType()) {

                case STD:
                    fn.accept(destIdx, wfm.getNodeContainer(sourceNode).getOutPort(sourceIdx));
                    break;

                case WFMIN: // NOSONAR: Moving the code somewhere else would make it more complicated
                    if (!Objects.equals(wfm.getID(), sourceNode)) {
                        LOGGER.errorWithFormat(
                            "Incoming connection %d from workflow input %s not matching the current workflow %s. "
                                + "Ignoring connection.",
                            destIdx, sourceNode, wfm.getID());
                    }
                    fn.accept(destIdx, wfm.getInPort(sourceIdx).getUnderlyingPort());
                    break;

                case WFMOUT:
                    // This cannot happen because it is an incoming connection of the current node
                    // Therefore it cannot be incoming to the workflow output
                    LOGGER.errorWithFormat("Incoming connection %d from workflow output %s. "
                        + "This is an illegal state. Ignoring connection.", destIdx, sourceNode, wfm.getID());
                    break;

                case WFMTHROUGH:
                    // This cannot happen because it is an incoming connection of the current node
                    LOGGER.errorWithFormat("Incoming connection %d is a connection through a workflow. "
                        + "This is an illegal state. Ignoring connection.", destIdx, sourceNode, wfm.getID());
                    break;
            }
        }
    }
}
