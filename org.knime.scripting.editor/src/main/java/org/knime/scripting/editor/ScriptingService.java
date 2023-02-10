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
 *   Jul 22, 2022 (benjamin): created
 */
package org.knime.scripting.editor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.scripting.editor.lsp.LanguageServerProxy;

/**
 * A base scripting service that provides an JSON-RPC endpoint for a scripting dialog with {@link #getJsonRpcService()}.
 * An optional language server is made available to the frontend client and console messages can be sent to the
 * frontend.
 *
 * Extend the {@link ScriptingService} to implement additional functionality for a specific scripting dialog.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class ScriptingService {

    private final Optional<LanguageServerProxy> m_languageServer;

    // TODO(AP-19341) Replace the event queue with Java->JS events
    private final BlockingQueue<Event> m_eventQueue;

    private final Optional<WorkflowControl> m_workflowControl;

    private final Predicate<FlowVariable> m_flowVariableFilter;

    /** Information about a flow variable */
    public static class FlowVariableInput {

        public final String name;

        public final String value;

        @SuppressWarnings({"hiding", "javadoc"})
        public FlowVariableInput(final String name, final String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Create a new {@link ScriptingService} without a language server.
     */
    public ScriptingService() {
        this(null, x -> true);
    }

    /**
     * Create a new {@link ScriptingService}.
     *
     * @param languageServer the language server that will be made available to the frontend client
     * @param flowVariableFilter filters flowvariable given a set of allowed types.
     */
    public ScriptingService(final LanguageServerProxy languageServer, final Predicate<FlowVariable> flowVariableFilter) {
        m_languageServer = Optional.ofNullable(languageServer);
        m_eventQueue = new LinkedBlockingQueue<>();
        m_workflowControl = Optional.ofNullable(NodeContext.getContext()) //
            .map(NodeContext::getNodeContainer) //
            .map(WorkflowControl::new);

        // TODO(AP-19338) LSP lifetime
        // React to language server messages
        m_languageServer.ifPresent( //
            ls -> ls.setMessageListener( //
                m -> sendEvent("language-server", m) //
            ) //
        );
        m_flowVariableFilter = flowVariableFilter;
    }

    /**
     * @return Map to all available FlowVariables. Eventually filtered.
     */
    public  Map<String, FlowVariable> getFlowVariablesForScript() {
        return getWorkflowControl().getFlowObjectStack().getAllAvailableFlowVariables();
    }

    /**
     * @return the workflowControl
     */
    protected WorkflowControl getWorkflowControl() {
        return m_workflowControl
            .orElseThrow(() -> new IllegalStateException("Trying to control the workflow of a scripting service that "
                + "was not created in the node context is not supported. This is an implementation error."));
    }


    /**
     * Send a new event to the frontend. The data is serialized to JSON.
     *
     * @param type a unique type identifier that tells the frontend how to handle the event
     * @param data additional data that will be serialized to JSON and made available to the handler in the frontend
     */
    protected void sendEvent(final String type, final Object data) {
        m_eventQueue.add(new Event(type, data));
    }

    /**
     * Send the given text to the console of the frontend.
     *
     * @param text the text that should be added to the console
     */
    protected void addConsoleOutputEvent(final ConsoleText text) {
        sendEvent("console", text);
    }

    /**
     * @return the service that provides its methods via JSON-RPC to the frontend
     */
    public JsonRpcService getJsonRpcService() {
        return new JsonRpcService();
    }

    /** The service that provides its methods via JSON-RPC to the frontend. */
    public class JsonRpcService {

        /**
         * @return Filtered FlowVariables. Filter provided by subclass.
         */
        public List<FlowVariableInput> getFlowVariables() {
            return getFlowVariablesForScript().values().stream().filter(m_flowVariableFilter)
                    .map(f -> new FlowVariableInput(f.getName(), f.getValueAsString())).collect(Collectors.toList());
        }


        /**
         * Remove the next event for the frontend and return it.
         *
         * @return the next event for the frontend
         */
        public Optional<Event> getEvent() {
            if (m_eventQueue.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(m_eventQueue.poll());
            }
        }

        /**
         * Send the given message to the language server. Do nothing if no language server is available.
         *
         * @param message the JSON-RPC message for the language server
         */
        public void sendLanguageServerMessage(final String message) {
            m_languageServer.ifPresent(ls -> ls.sendMessage(message));
        }
    }

    /**
     * An event that is serialized to JSON and sent to the frontend. Use
     * {@link ScriptingService#sendEvent(String, Object)} to send a new event to the frontend.
     */
    public static final class Event {

        /** A unique type identifier that tells the frontend how to handle the event */
        public final String type;

        /** Additional data that will be available to the handler in the frontend */
        public final Object data;

        @SuppressWarnings("hiding")
        private Event(final String type, final Object data) {
            this.type = type;
            this.data = data;
        }
    }

    /**
     * A snippet of text that should be logged in the output console. Note that the snippet does not have to be a whole
     * line. Also the text snippet can be part of the standard output (if {@link ConsoleText#stderr} is false of part of
     * the standard error (otherwise).
     */
    @SuppressWarnings("javadoc")
    public static final class ConsoleText {
        public final String text;

        public final boolean stderr;

        @SuppressWarnings("hiding")
        public ConsoleText(final String text, final boolean stderr) {
            this.text = text;
            this.stderr = stderr;
        }
    }
}
