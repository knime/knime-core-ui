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
 *   Feb 19, 2026 (GitHub Copilot): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.hiddenfeaturesnode;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonState;
import org.knime.node.parameters.widget.button.RestCallButton;
import org.knime.node.parameters.widget.button.RestCallButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleRestCallHandler;
import org.knime.core.webui.node.dialog.defaultdialog.hiddenfeaturesnode.HiddenFeatureRestCallButtonParameters.StatusCheckHandler.StatusCheckStates;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Section;

/**
 * Parameters class demonstrating the RestCallButton widget for executing REST API calls.
 *
 * @author GitHub Copilot
 */
public class HiddenFeatureRestCallButtonParameters implements NodeParameters {

    @Section(title = "REST Call Button Examples")
    interface RestCallButtonSection {
    }

    @Section(title = "Custom Handler Examples")
    @After(RestCallButtonSection.class)
    interface CustomHandlerSection {
    }

    // === Basic Examples ===

    @Widget(title = "Simple REST Call Button", 
            description = "Click the button to execute a simple action and display the result")
    @RestCallButtonWidget(
        buttonText = "Execute",
        actionHandler = SimpleRestCallHandler.class
    )
    RestCallButton m_simpleCall = new RestCallButton("Initial message. Click button above to execute.");

    @Widget(title = "No Loading Indicator", 
            description = "REST call button without loading spinner")
    @RestCallButtonWidget(
        buttonText = "Execute",
        actionHandler = SimpleRestCallHandler.class,
        showLoadingIndicator = false
    )
    RestCallButton m_noLoadingCall = new RestCallButton();

    // === Custom Handler Example ===

    @Widget(title = "API Status Checker", 
            description = "Simulates checking an API service status")
    @RestCallButtonWidget(
        buttonText = "Check Status",
        actionHandler = StatusCheckHandler.class
    )
    RestCallButton m_statusCheck = new RestCallButton("Status: Unknown\n\nClick 'Check Status' to query.");

    /**
     * Example custom handler that simulates a status check.
     */
    public static final class StatusCheckHandler 
            implements ButtonActionHandler<String, Object, StatusCheckStates> {

        /**
         * State machine for status check button.
         */
        public enum StatusCheckStates {
            /**
             * Initial state - button is ready to be clicked
             */
            @ButtonState(text = "Check Status", nextState = "READY")
            READY,
            /**
             * State after successful check - button can be clicked again
             */
            @ButtonState(text = "Check Status", nextState = "READY")
            DONE
        }

        @Override
        public Class<StatusCheckStates> getStateMachine() {
            return StatusCheckStates.class;
        }

        @Override
        public ButtonChange<String, StatusCheckStates> initialize(final String currentValue,
            final NodeParametersInput context) {
            // Initialize button to READY state
            return new ButtonChange<>(StatusCheckStates.READY);
        }

        @Override
        public ButtonChange<String, StatusCheckStates> invoke(final StatusCheckStates buttonState,
            final Object settings, final NodeParametersInput context) {
            // Simulate a status check
            final var random = new java.util.Random();
            final var status = random.nextBoolean() ? "ONLINE" : "OFFLINE";
            final var responseTime = random.nextInt(500) + 100;
            final var timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            final String message = String.format("Status: %s\nResponse Time: %d ms\nLast Checked: %s\n\n"
                + "This demonstrates how to create custom REST call handlers.\n"
                + "You can access other dialog settings via context.",
                status, responseTime, timestamp);
            
            // Return new message value and transition to DONE state
            return new ButtonChange<>(message, StatusCheckStates.DONE);
        }
    }
}
