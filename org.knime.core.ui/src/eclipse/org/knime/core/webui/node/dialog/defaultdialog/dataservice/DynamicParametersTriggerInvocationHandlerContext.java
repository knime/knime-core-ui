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
 *   Oct 13, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;

/**
 * An instance of this class holds dynamically registered TriggerInvocationHandlers for dynamic parameters.
 *
 * @author Paul Bärnreuther
 */
public final class DynamicParametersTriggerInvocationHandlerContext {

    final Map<String, Supplier<TriggerInvocationHandler<String>>> m_triggerInvocationHandlers =
        new ConcurrentHashMap<>();

    final Map<String, TriggerInvocationHandler<String>> m_cachedTriggerInvocationHandlers = new ConcurrentHashMap<>();

    /**
     * Registers a triggerInvocationHandler for part of the dialog and returns a unique UUID as the id to call that
     * handler.
     *
     * @param triggerInvocationHandler the handler to invoke for trigger invocations
     * @return a unique UUID that can be used as id to invoke the handler
     */
    public String
        registerTriggerInvocationHandler(final Supplier<TriggerInvocationHandler<String>> triggerInvocationHandler) {

        final var validatorId = UUID.randomUUID().toString();
        m_triggerInvocationHandlers.put(validatorId, triggerInvocationHandler);

        return validatorId;
    }

    /**
     * Gets the trigger invocation handler for the given id. Any id given to the frontend has been registered before via
     * {@link #registerTriggerInvocationHandler(Supplier)} before.
     *
     * @param id the id of the trigger invocation handler
     * @return the trigger invocation handler for the given id
     */
    public TriggerInvocationHandler<String> getTriggerInvocationHandler(final String id) {
        return m_cachedTriggerInvocationHandlers.computeIfAbsent(id, key -> m_triggerInvocationHandlers.get(key).get());
    }

    /**
     * Clears all registered validators.
     */
    public void clear() {
        m_triggerInvocationHandlers.clear();
        m_cachedTriggerInvocationHandlers.clear();
    }
}
