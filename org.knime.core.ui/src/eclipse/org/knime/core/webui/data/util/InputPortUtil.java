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
 *   Aug 30, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.data.util;

import java.util.Arrays;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;

/**
 * Utilities around a nodes input {@link PortObjectSpec}s and {@link PortObject}s
 *
 * @noreference
 *
 * @author Paul Bärnreuther
 */
public final class InputPortUtil {

    private InputPortUtil() {
        // utility
    }

    /**
     * @param nc to extract the input specs from
     * @return the associated array of input {@link PortObjectSpec PortObjectSpecs} excluding the flow variables port.
     *         NOTE: array of specs can contain {@code null} values, e.g., if input port is not connected or inactive!
     */
    public static PortObjectSpec[] getInputSpecsExcludingVariablePort(final NodeContainer nc) {
        final var allSpecs = getInputsIncludingVariablePort(nc).portObjectSpecs();
        // copy input port object specs, ignoring the 0-variable port:
        return Arrays.copyOfRange(allSpecs, 1, allSpecs.length);
    }

    /**
     * @param nc the node container to extract the input specs from
     * @return the associated array of input {@link PortObjectSpec PortObjectSpecs} including the flow variables port.
     */
    public static PortObjectSpec[] getInputSpecsIncludingVariablePort(final NodeContainer nc) {
        return getInputsIncludingVariablePort(nc).portObjectSpecs();
    }

    /**
     * @param nc node container to extract the input objects from
     * @return
     */
    public static PortObject[] getInputPortObjectsExcludingVariablePort(final NodeContainer nc) {
        final var allPortObjects = getInputsIncludingVariablePort(nc).portObjects;
        // copy input port objects, ignoring the 0-variable port:
        return Arrays.copyOfRange(allPortObjects, 1, allPortObjects.length);
    }

    /**
     * @param nc the node container to extract the input specs from
     * @return the associated array of input {@link PortObjectSpec PortObjectSpecs} including the flow variables port.
     */
    private static NodeInput getInputsIncludingVariablePort(final NodeContainer nc) {

        final var wfm = nc.getParent();

        final var rawSpecs = new PortObjectSpec[nc.getNrInPorts()];
        final var rawInputObject = new PortObject[nc.getNrInPorts()];

        for (var cc : wfm.getIncomingConnectionsFor(nc.getID())) {
            var sourceId = cc.getSource();
            NodeOutPort outPort;
            if (sourceId.equals(wfm.getID())) {
                outPort = wfm.getWorkflowIncomingPort(cc.getSourcePort());
            } else {
                outPort = wfm.getNodeContainer(sourceId).getOutPort(cc.getSourcePort());
            }

            var portObject = outPort.getPortObject();
            var spec = outPort.getPortObjectSpec();

            if (spec instanceof InactiveBranchPortObjectSpec) {
                rawInputObject[cc.getDestPort()] = null;
                rawSpecs[cc.getDestPort()] = null;
            } else {
                rawInputObject[cc.getDestPort()] = portObject;
                rawSpecs[cc.getDestPort()] = spec;
            }
        }
        return new NodeInput(rawSpecs, rawInputObject);
    }

    private record NodeInput(PortObjectSpec[] portObjectSpecs, PortObject[] portObjects) { //NOSONAR
        // record to store port objects and specs
    }

}
