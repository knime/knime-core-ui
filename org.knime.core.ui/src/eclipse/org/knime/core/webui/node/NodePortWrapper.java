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
package org.knime.core.webui.node;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.port.PortContext;

import com.google.common.base.Objects;

/**
 * A {@link NodeWrapper} that identifies an output port on an instantiated node.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 */
public interface NodePortWrapper extends NodeWrapper {

    /**
     * Convenience method to create a {@link NodePortWrapper}-instance.
     *
     * @param nc The node under consideration
     * @param portIdx The index of the port
     * @param viewIdx The index of the port view
     * @return a new instance
     */
    static NodePortWrapper of(final NodeContainer nc, final int portIdx, final int viewIdx) {
        return new NodePortWrapper() { // NOSONAR

            @Override
            public NodeContainer get() {
                return nc;
            }

            @Override
            public int getPortIdx() {
                return portIdx;
            }

            @Override
            public int getViewIdx() {
                return viewIdx;
            }

            @Override
            public List<NodeContainer> getNodesConnectedToOutputPorts() {
                if (nc instanceof WorkflowManager wfm && nc.getNrOutPorts() > 0) {
                    return IntStream.range(0, nc.getNrOutPorts())//
                        .mapToObj(wfm::getOutPort)//
                        .map(NodeOutPort::getConnectedNodeContainer)//
                        .map(NodeContainer.class::cast)//
                        .toList();
                }
                return Collections.emptyList();
            }

            @Override
            public <T> T getWithContext(final Supplier<T> supplier) {
                PortContext.pushContext(nc.getOutPort(portIdx));
                try {
                    return supplier.get();
                } finally {
                    PortContext.removeLastContext();
                }
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null) {
                    return false;
                }
                if (getClass() != o.getClass()) {
                    return false;
                }
                var w = (NodePortWrapper)o;
                return Objects.equal(nc, w.get()) //
                    && portIdx == w.getPortIdx() //
                    && viewIdx == w.getViewIdx() //
                    && Objects.equal(this.getNodesConnectedToOutputPorts(), w.getNodesConnectedToOutputPorts());
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder()//
                    .append(nc)//
                    .append(portIdx)//
                    .append(viewIdx)//
                    .append(this.getNodesConnectedToOutputPorts())//
                    .toHashCode();
            }
        };
    }

    /**
     * @return the port index
     */
    int getPortIdx();

    /**
     * @return the view index
     */
    int getViewIdx();

    /**
     * To be able to know whether the nodes internally connected to a metanodes output ports, we need to track them.
     * Otherwise the node port view cache might yield invalid port views, see NXT-1950.
     *
     * @return The list of nodes internally connected to a metanodes output ports; empty if not a metanode
     */
    List<NodeContainer> getNodesConnectedToOutputPorts();
}
