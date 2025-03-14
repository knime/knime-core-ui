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

import java.util.function.Supplier;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.NodeContainerWrapper;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.webui.node.port.PortContext;

/**
 * Wrapper for a node ({@link NodeContainer}). The only purpose is to be able to represent a node <i>and</i> optionally
 * something else together with that node (e.g. a node and a port idx) at the same time such that the
 * {@link AbstractNodeUIManager} can operate on both (e.g. node and port).
 *
 * Implementations must also implement the {@link #equals(Object)} and {@link #hashCode()} methods.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface NodeWrapper {

    /**
     * @return the wrapped node
     */
    NodeContainer get();

    /**
     * @return a {@link NodeContainerUI} instance; need in order to make the dialogs work in the remote workflow editor
     */
    default NodeContainerUI getNCUI() {
        return NodeContainerWrapper.wrap(get());
    }

    /**
     * Runs an operation within the context 'compatible' with this node wrapper (e.g. {@link NodeContext} or
     * {@link PortContext})
     *
     * @param <T>
     *
     * @param supplier the operation to run
     * @return the object the supplier returns
     */
    <T> T getWithContext(Supplier<T> supplier);

    /**
     * Convenience method to create a {@link NodeWrapper}-instance.
     *
     * @param nc
     * @return a new instance
     */
    static NodeWrapper of(final NodeContainer nc) {
        return of(NodeContainerWrapper.wrap(nc));
    }

    /**
     * Convenience method to create a {@link NodeWrapper}-instance.
     *
     * @param nc
     * @return a new instance
     */
    static NodeWrapper of(final NodeContainerUI nc) {
        return new NodeWrapper() { // NOSONAR

            @Override
            public NodeContainer get() {
                if (Wrapper.wraps(nc, NodeContainer.class)) {
                    return Wrapper.unwrapNC(nc);
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public NodeContainerUI getNCUI() {
                return nc;
            }

            @Override
            public <T> T getWithContext(final Supplier<T> supplier) {
                NodeContext.pushContext(nc);
                try {
                    return supplier.get();
                } finally {
                    NodeContext.removeLastContext();
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

                var w = (NodeWrapper)o;
                return nc.equals(w.getNCUI());
            }

            @Override
            public int hashCode() {
                return nc.hashCode();
            }
        };

    }

    /**
     * Implemented by {@link NodeFactory NodeFactories} in order to provide a custom node-wrapper-type-id as returned by
     * {@link NodeWrapper#getNodeWrapperTypeId()}. Usually only necessary for testing purposes.
     */
    interface CustomNodeWrapperTypeIdProvider {
        String getNodeWrapperTypeId(NativeNodeContainer nnc);
    }
}
