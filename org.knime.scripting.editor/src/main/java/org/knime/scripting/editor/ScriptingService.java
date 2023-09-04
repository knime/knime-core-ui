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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
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
public abstract class ScriptingService {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ScriptingService.class);

    private final LanguageServerStarter m_languageServerCreator;

    // TODO(AP-19341) Replace the event queue with Java->JS events
    private final BlockingQueue<Event> m_eventQueue;

    private final Optional<WorkflowControl> m_workflowControl;

    private final Predicate<FlowVariable> m_flowVariableFilter;

    private Optional<LanguageServerProxy> m_languageServer;

    /**
     * Create a new {@link ScriptingService} without a language server.
     */
    public ScriptingService() {
        this(null, x -> true);
    }

    /**
     * Create a new {@link ScriptingService}.
     *
     * @param languageServerCreator create a language server that will be available to the frontend. Can be
     *            <code>null</code>.
     * @param flowVariableFilter filters flowvariable given a set of allowed types.
     */
    public ScriptingService(final LanguageServerStarter languageServerCreator,
        final Predicate<FlowVariable> flowVariableFilter) {
        m_languageServerCreator = Optional.ofNullable(languageServerCreator).orElse(() -> null);
        m_languageServer = Optional.empty();
        m_eventQueue = new LinkedBlockingQueue<>();
        m_workflowControl = Optional.ofNullable(NodeContext.getContext()) //
            .map(NodeContext::getNodeContainer) //
            .map(WorkflowControl::new);
        m_flowVariableFilter = flowVariableFilter;
    }

    /**
     * @return available flow variables that match the flow variable filter
     */
    public Collection<FlowVariable> getFlowVariables() {
        var flowVars = getWorkflowControl().getFlowObjectStack().getAllAvailableFlowVariables().values();
        return flowVars.stream() //
            .filter(m_flowVariableFilter) //
            .collect(Collectors.toList());
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
    abstract public RpcService getJsonRpcService();

    /**
     * Deactivate the service. This stops the language server and clears the event queue.
     */
    public void onDeactivate() {
        m_languageServer.ifPresent(LanguageServerProxy::close);
        m_languageServer = Optional.empty();
        m_eventQueue.clear();
    }

    /** The service that provides its methods via JSON-RPC to the frontend. */
    public abstract class RpcService {

        /**
         * Provides code aliases for flow variables, input objects and output objects
         *
         * @author Rupert Ettrich
         */
        public interface CodeAliasProvider {
            /**
             *
             * @param flowVariableName The name of the flow variable for which the code alias should be retrieved
             * @return code alias for a specific flow variable if {@code flowVariableName} is provided, or code alias
             *         for the flow variable object if {@code flowVariableName} is null
             */
            default String getFlowVariableCodeAlias(final String flowVariableName) {
                return null;
            }

            /**
             *
             * @param index The index of the input object
             * @param type The type of input object, e.g. Table
             * @param subItemName The name of the sub item, e.g. a column in a table
             * @return code alias for an input object of a specific type. If {@code subItemName} is not null, then the
             *         code alias for the sub item will be returned
             */
            default String getInputObjectCodeAlias(final int index, final String type, final String subItemName) {
                return null;
            }

            /**
             * @param index the index of the output object
             * @param type The type of output object, e.g. Table
             * @param subItemName The name of the sub item, e.g. a column in a table
             * @return code alias for an input object of a specific type. If {@code subItemName} is not null, then the
             *         code alias for the sub item will be returned
             */
            default String getOutputObjectCodeAlias(final int index, final String type, final String subItemName) {
                return null;
            }
        }

        public abstract CodeAliasProvider getCodeAliasProvider();

        // NB: The UI Extension service throws an timeout after 10000ms
        private static final int GET_EVENT_TIMEOUT_MS = 2000;

        /**
         * @return information about the flow variables, all available flow variables are listed as subitems in the
         *         {@link InputOutputModel }
         */
        public abstract InputOutputModel getFlowVariableInputs();

        /**
         * @return Information about all input ports. Each port is returned as an {@link InputOutputModel }.
         */
        public abstract List<InputOutputModel> getInputObjects();

        /**
         * @return Information about all output ports. Each port is returned as an {@link InputOutputModel }.
         */
        public abstract List<InputOutputModel> getOutputObjects();

        /**
         * Remove the next event for the frontend and return it.
         *
         * @return the next event for the frontend
         */
        public Optional<Event> getEvent() {
            try {
                return Optional.ofNullable(m_eventQueue.poll(GET_EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (final InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for the next event", e);
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }

        /**
         * Start the LPS server which will receive messages via {@link #sendLanguageServerMessage(String)} and send
         * events of the "language-server" type.
         *
         * @return the status of the language server connection
         */
        @SuppressWarnings("resource") // language server proxy closed by #onDeactivate
        public LanguageServerStatus connectToLanguageServer() {
            try {
                m_languageServer = Optional.ofNullable(m_languageServerCreator.start());
                m_languageServer.ifPresent( //
                    // React to language server messages
                    ls -> ls.setMessageListener( //
                        m -> sendEvent("language-server", m) //
                    ) //
                );
                return new LanguageServerStatus(LanguageServerStatusKind.RUNNING);
            } catch (IOException e) {
                LOGGER.warn("Starting the language server failed: " + e.getMessage(), e);
                return new LanguageServerStatus(LanguageServerStatusKind.ERROR,
                    "Starting the language server failed: " + e.getMessage());
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
        public final String type; // NOSONAR

        /** Additional data that will be available to the handler in the frontend */
        public final Object data; // NOSONAR

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
        public final String text; // NOSONAR

        public final boolean stderr; // NOSONAR

        @SuppressWarnings("hiding")
        public ConsoleText(final String text, final boolean stderr) {
            this.text = text;
            this.stderr = stderr;
        }
    }

    /** Information about a flow variable */
    @SuppressWarnings("javadoc")
    public static class FlowVariableInput {

        public final String name; // NOSONAR

        public final String value; // NOSONAR

        public final String type; // NOSONAR

        @SuppressWarnings({"hiding"})
        public FlowVariableInput(final String name, final String value, final String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
    }

    @SuppressWarnings("javadoc")
    public enum LanguageServerStatusKind {
            RUNNING, ERROR;
    }

    /**
     * Status result when connecting to a language server
     *
     * @param status the status kind
     * @param message an optional error message
     */
    public record LanguageServerStatus(LanguageServerStatusKind status, String message) {
        LanguageServerStatus(final LanguageServerStatusKind status) {
            this(status, null);
        }
    }

    /**
     * A supplier that starts an LSP server.
     */
    @FunctionalInterface
    public interface LanguageServerStarter {
        /**
         * Start the LSP server.
         *
         * @return a proxy object which can communicate with the server
         * @throws IOException if an I/O error occurs starting the process
         */
        LanguageServerProxy start() throws IOException;
    }

    /**
     * An item in an InputOutputModel, e.g. for table columns
     *
     * @param name The name of the sub item
     * @param type The display name of the type of the sub item
     * @param codeAlias The code alias needed to access this item in the code
     */
    public static record InputOutputModelSubItem(String name, String type, String codeAlias) {

    }

    /**
     * An item that will be displayed in the input/output panel of the script editor
     *
     * @param name The name of the item
     * @param codeAlias The code alias needed to access this item in the code
     * @param subItems A (possibly empty) list of sub items
     */
    public static record InputOutputModel( // NOSONAR: we don't need hash and equals
        String name, //
        String codeAlias, //
        InputOutputModelSubItem[] subItems) {
    }

    @SuppressWarnings("javadoc")
    public static class FlowVariableInputWithCodeAlias extends FlowVariableInput {
        public final String codeAlias;

        @SuppressWarnings({"hiding"})
        public FlowVariableInputWithCodeAlias(final String name, final String value, final String type,
            final String codeAlias) {
            super(name, value, type);
            this.codeAlias = codeAlias;
        }

        @SuppressWarnings({"hiding"})
        public FlowVariableInputWithCodeAlias(final FlowVariableInput flowVar, final String codeAlias) {
            super(flowVar.name, flowVar.value, flowVar.type);
            this.codeAlias = codeAlias;
        }
    }
}
