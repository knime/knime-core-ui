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
 *   22 Mar 2022 (marcbux): created
 */
package org.knime.core.webui.data;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.webui.data.util.InputPortUtil;

/**
 * A {@link DataServiceContext} allows to report warning messages during a data service invocation or assembly of
 * initial data. These warning messages can then be obtained by the {@link RpcDataService} or {@link InitialDataService}
 * and passed to the frontend for display along a valid result.
 *
 * Furthermore, it allows one to created {@link BufferedDataTable BufferedDataTables} within the context of the
 * underlying node.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DataServiceContext {

    private static final ThreadLocal<DataServiceContext> CONTEXT = new ThreadLocal<>();

    private static boolean allowOverwrite = true;

    /**
     * @return the {@link DataServiceContext} for the current thread, potentially creating a new one in the process.
     */
    public static DataServiceContext get() {
        var context = CONTEXT.get();
        if (context == null) {
            init(null, null);
            return CONTEXT.get();
        } else {
            return context;
        }
    }

    static void init(final NodeContainer nc) {
        if (nc instanceof SingleNodeContainer snc) {
            final var inputSpecsSupplier =
                new CachingSupplier<>(() -> InputPortUtil.getInputSpecsExcludingVariablePort(nc));
            init(new CachingSupplier<>(snc::createExecutionContext), inputSpecsSupplier);
        } else {
            init(null, null);
        }
    }

    static void init(final CachingSupplier<ExecutionContext> execSupplier,
        final CachingSupplier<PortObjectSpec[]> specsSupplier) {
        if (CONTEXT.get() != null && !allowOverwrite) {
            return;
        }
        CONTEXT.set(new DataServiceContext(execSupplier, specsSupplier));
    }

    /**
     * Inits the context state and fixes it until it's this method is called again (i.e. not other init-call except this
     * one will overwrite the context state (for the respective thread local) or it's cleared ({@link #remove()}.
     *
     * @param execSupplier
     * @param specsSupplier
     * @param dependencies
     */
    static void initForTesting(final CachingSupplier<ExecutionContext> execSupplier,
        final CachingSupplier<PortObjectSpec[]> specsSupplier) {
        allowOverwrite = false;
        CONTEXT.set(new DataServiceContext(execSupplier, specsSupplier));
    }

    private final List<String> m_warningMessages = new ArrayList<>();

    private final CachingSupplier<ExecutionContext> m_execSupplier;

    private final CachingSupplier<PortObjectSpec[]> m_specsSupplier;

    private DataServiceContext(final CachingSupplier<ExecutionContext> execSupplier,
        final CachingSupplier<PortObjectSpec[]> specsSupplier) {
        m_execSupplier = execSupplier;
        m_specsSupplier = specsSupplier;
    }

    /**
     * Adds another warning message to the list of warning messages.
     *
     * @param warningMessage a warning message
     */
    public synchronized void addWarningMessage(final String warningMessage) {
        m_warningMessages.add(warningMessage);
    }

    /**
     * @return a list of warnings that occurred while invoking the data service
     */
    public synchronized String[] getWarningMessages() {
        return m_warningMessages.toArray(new String[0]);
    }

    /**
     * Removes all the previously set warning message.
     */
    public synchronized void clearWarningMessages() {
        m_warningMessages.clear();
    }

    /**
     * @return the execution context
     * @throws IllegalStateException if there is no execution context available
     */
    public synchronized ExecutionContext getExecutionContext() {
        if (m_execSupplier != null) {
            return m_execSupplier.get();
        }
        throw new IllegalStateException("No execution context available");
    }

    /**
     * @return the input specs excluding the flow variable port
     */
    public PortObjectSpec[] getInputSpecs() {
        if (m_specsSupplier != null) {
            return m_specsSupplier.get();
        }
        throw new IllegalStateException("No spec supplier has been initialized within the data service context.");
    }

    /**
     * Get the implementation of an additional dependency.
     *
     * @param <T> the type of the dependency interface
     * @param clazz the class of the dependency interface
     * @return an implementation of the dependency or <code>null</code> if there is no implementation of the dependency
     *         in the current context
     */
    @SuppressWarnings("static-method") // non-static to be consistent with the other methods
    public <T> T getOtherDependency(final Class<T> clazz) {
        return DataServiceDependencies.getDependency(clazz);
    }

    /**
     * Removes the entire context for the current thread.
     */
    static void remove() {
        CONTEXT.remove();
        allowOverwrite = true;
    }

}
