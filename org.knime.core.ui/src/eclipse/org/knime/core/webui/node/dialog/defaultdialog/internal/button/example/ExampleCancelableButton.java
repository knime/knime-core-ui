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
 *   26 Jan, 2026 (Tim Crundall): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.button.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.example.ExampleCancelableButton.ActionTempState.CopyToFinishedUuidOnceActionCompletes;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;

/**
 * An example implementation of a {@link ButtonActionHandler} that supports cancelable actions,
 * which can be directly copied into a NodeParameters.
 *
 * This is a temporary workaround until proper support for cancelable actions is added to the framework.
 *
 * It relies on the mechanism that value providers triggered together are returned to the client
 * together. This ensures a reliable means to notify the frontend AP that an action has finished
 * (either normally or via cancellation) by updating a UUID value.
 *
 * The typical control flow is as follows:
 * 1) The "Action button" is pressed, which triggers a new value for @{RunningActionUuid}
 * 2) The new value of @{RunningActionUuid} triggers a comparison with @{FinishedActionUuid} to hide
 * the "Action button" and show the "Cancel button" via the @{UuidsMatchRef} field. It is also
 * copied to the @{RunningActionUuidCopy}.
 * 3) The new value of @{RunningActionUuidCopy} triggers the long-running action to start via the
 * @{SetSettingFromActionResult} provider. It also copies the uuid value to @{FinishedActionUuid}
 * (but only once the action is complete or cancelled).
 * 4) The new value of @{FinishedActionUuid} triggers a comparison with @{RunningActionUuid} which
 * will now be equal, hiding the "Cancel button" and showing the "Action button" again. The result
 * of the action is also set to the associated result field.
 * 5) If the "Cancel button" is pressed while the action is running, it interrupts the thread.
 *
 * Note that the long-running action must periodically check whether the thread has been interrupted.
 *
 * Also note that this implementation only supports a single cancelable action per node. This is
 * enforced by linking the "Action button" to the @{CopyToFinishedUuidOnceActionCompletes} provider.
 * The frontend will thus disable the "Action button" until the action is complete or cancelled.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 */
public class ExampleCancelableButton implements NodeParameters {

    @Section(title = "Buttons")
    interface Buttons {
    }

    @Section(title = "Hidden State")
    @After(Buttons.class)
    interface HiddenState {
    }

    @Section(title = "Action Result")
    @After(HiddenState.class)
    interface ActionResult {
    }

    @Widget(title = "Perform action",
        description = "Press this button to start the action. Will take ~3 seconds to complete.")
    @SimpleButtonWidget(
        ref = PerformActionButtonRef.class,
        runFinishedProvider = CopyToFinishedUuidOnceActionCompletes.class
    )
    @Effect(predicate = ActionTempState.UuidsMatchRef.class, type = EffectType.SHOW)
    @Layout(Buttons.class)
    Void m_performActionButton;

    interface PerformActionButtonRef extends ButtonReference {
    }

    @Widget(title = "Cancel", description = "Press this button to cancel the action that is currently being performed.")
    @SimpleButtonWidget(ref = CancelButtonRef.class)
    @Effect(predicate = ActionTempState.UuidsMatchRef.class, type = EffectType.HIDE)
    @Layout(Buttons.class)
    Void m_cancelButton;

    interface CancelButtonRef extends ButtonReference {
    }

    @Widget(title = "Action result", description = """
            The resulting setting from the action.
            """)
    @ValueProvider(SetSettingFromActionResult.class)
    @ValueReference(PropertiesRef.class)
    @Layout(ActionResult.class)
    String m_resultOfAction = "not pressed yet";

    interface PropertiesRef extends ParameterReference<String> {
    }

    @Persistor(ActionTempState.DoNotPersist.class)
    ActionTempState m_actionState = new ActionTempState();

    static final class SetSettingFromActionResult implements StateProvider<String> {

        final static Map<NodeID, Thread> THREAD_PER_NODE = new ConcurrentHashMap<>();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromValueSupplier(ActionTempState.RunningActionUuidCopyRef.class);
        }

        static NodeID getCurrentNodeID() {
            var context = NodeContext.getContext();
            if (context == null) {
                // Provided for testing
                return new NodeID(999);
            }
            return context.getNodeContainer().getID();
        }

        public static Thread getRunningForNode() {
            return THREAD_PER_NODE.get(getCurrentNodeID());
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {

            final Thread currentThread = Thread.currentThread();
            synchronized (THREAD_PER_NODE) {
                THREAD_PER_NODE.put(getCurrentNodeID(), currentThread);
            }

            // Initialize an (empty) array of characters
            List<String> res = new ArrayList<>();

            // Perform slow action in chunks
            for (var i = 0; i < 3; i++) {
                // Check for cancellation between chunks
                if (currentThread.isInterrupted()) {
                    throw new StateComputationFailureException();
                }

                try {
                    // Perform slow chunk of work
                    Thread.sleep(1000);
                    res.add(String.valueOf(i));
                } catch (InterruptedException ex) {
                    throw new StateComputationFailureException(); // NOSONAR
                }
            }

            if (currentThread.isInterrupted()) {
                throw new StateComputationFailureException();
            }

            // convert chunked result to single result
            return res.toString();
        }

        /**
         * Called on deactivation. This will interrupt any action on dialog close.
         *
         * @param nodeID the node ID
         */
        public static void terminateAndClearAllRunningThreads(final NodeID nodeID) {
            synchronized (THREAD_PER_NODE) {
                final var runningThread = THREAD_PER_NODE.get(nodeID);
                if (runningThread != null) {
                    runningThread.interrupt();
                    THREAD_PER_NODE.remove(nodeID);
                }
            }
        }

    }

    @ValueProvider(CancelCurrentThreadOnCancelButtonPress.class)
    Void m_cancelButtonTarget;

    static final class CancelCurrentThreadOnCancelButtonPress implements StateProvider<Void> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.getValueSupplier(ActionTempState.RunningActionUuidRef.class);
            initializer.computeOnButtonClick(CancelButtonRef.class);
        }

        @Override
        public Void computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {

            final var threadPerNode = SetSettingFromActionResult.THREAD_PER_NODE;
            synchronized (threadPerNode) {

                final var nodeID = SetSettingFromActionResult.getCurrentNodeID();
                Optional.ofNullable(threadPerNode.get(nodeID)).ifPresent(thread -> {
                    thread.interrupt();
                    threadPerNode.remove(nodeID);
                });

                return null;
            }
        }

    }

    static class ActionTempState implements NodeParameters {

        @Widget(title = "Action id", description = "")
        @ValueProvider(SetNewUuidWhenActionButtonPressed.class)
        @ValueReference(RunningActionUuidRef.class)
        @Layout(HiddenState.class)
        String m_runningActionUuid;

        interface RunningActionUuidRef extends ParameterReference<String> {
        }

        static final class SetNewUuidWhenActionButtonPressed implements StateProvider<String> {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(PerformActionButtonRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                return UUID.randomUUID().toString();
            }

        }

        @Widget(title = "Action id copy", description = "")
        @ValueProvider(CopyRunningActionUuidProvider.class)
        @ValueReference(RunningActionUuidCopyRef.class)
        @Layout(HiddenState.class)
        String m_runningActionUuidCopy;

        static final class CopyRunningActionUuidProvider implements StateProvider<String> {

            private Supplier<String> m_valueSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_valueSupplier = initializer.computeFromValueSupplier(RunningActionUuidRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                return m_valueSupplier.get();
            }

        }

        interface RunningActionUuidCopyRef extends ParameterReference<String> {
        }

        @Widget(title = "Finished id", description = "")
        @ValueProvider(CopyToFinishedUuidOnceActionCompletes.class)
        @ValueReference(FinishedUuidRef.class)
        @Layout(HiddenState.class)
        String m_uuidForLatestFinishedAction;

        interface FinishedUuidRef extends ParameterReference<String> {

        }

        static final class CopyToFinishedUuidOnceActionCompletes implements StateProvider<String> {

            private Supplier<String> m_valueSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_valueSupplier = initializer.computeFromValueSupplier(RunningActionUuidCopyRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                // just return the current value of the other parameter
                return m_valueSupplier.get();
            }

        }

        @Widget(title = "Ids match", description = "")
        @ValueProvider(CheckIfUuidsMatch.class)
        @ValueReference(UuidsMatchRef.class)
        @Layout(HiddenState.class)
        boolean m_theUuidsMatch = true;

        private static final class UuidsMatchRef implements BooleanReference {

        }

        static final class CheckIfUuidsMatch implements StateProvider<Boolean> {

            private Supplier<String> m_currentUuidSupplier;

            private Supplier<String> m_finishedUuidSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_currentUuidSupplier = initializer.computeFromValueSupplier(RunningActionUuidRef.class);
                m_finishedUuidSupplier = initializer.computeFromValueSupplier(FinishedUuidRef.class);
            }

            @Override
            public Boolean computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                return Objects.equals(m_currentUuidSupplier.get(), m_finishedUuidSupplier.get());
            }

        }

        static final class DoNotPersist implements NodeParametersPersistor<ActionTempState> {

            @Override
            public ActionTempState load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return new ActionTempState();
            }

            @Override
            public void save(final ActionTempState param, final NodeSettingsWO settings) {
                // Do not persist
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }

    }
}
