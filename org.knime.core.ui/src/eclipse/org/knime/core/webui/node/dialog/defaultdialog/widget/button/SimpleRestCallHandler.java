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
 *  propagated with or for interopection with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 19, 2026 (GitHub Copilot): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.button;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonChange;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonState;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleRestCallHandler.RestCallStates;
import org.knime.node.parameters.NodeParametersInput;

/**
 * Example action handler that returns a simple timestamped message.
 * This demonstrates the basic pattern for implementing REST call button handlers.
 *
 * @author GitHub Copilot
 */
public final class SimpleRestCallHandler implements ButtonActionHandler<String, Object, RestCallStates> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * State machine for simple REST call buttons.
     */
    public enum RestCallStates {
        /**
         * Initial state - button is ready to be clicked
         */
        @ButtonState(text = "Execute", nextState = "READY")
        READY,
        /**
         * State after successful execution - button can be clicked again
         */
        @ButtonState(text = "Execute", nextState = "READY")
        DONE
    }

    @Override
    public Class<RestCallStates> getStateMachine() {
        return RestCallStates.class;
    }

    @Override
    public ButtonChange<String, RestCallStates> initialize(final String currentValue,
        final NodeParametersInput context) {
        // Initialize button to READY state
        return new ButtonChange<>(RestCallStates.READY);
    }

    @Override
    public ButtonChange<String, RestCallStates> invoke(final RestCallStates buttonState, final Object settings,
        final NodeParametersInput context) {
        // You can access current dialog settings through context if needed
        final String timestamp = LocalDateTime.now().format(FORMATTER);
        final String message = String.format("Button clicked at %s\n\nThis is a simple demonstration.\n\n"
            + "In a real implementation, you would:\n"
            + "- Make REST API calls to external services\n"
            + "- Access other settings via context\n"
            + "- Return results or error messages", 
            timestamp);
        
        // Return new message value and transition to DONE state
        return new ButtonChange<>(message, RestCallStates.DONE);
    }
}
