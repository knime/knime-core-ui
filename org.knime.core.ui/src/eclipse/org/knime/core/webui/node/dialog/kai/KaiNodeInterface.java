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
 *   Dec 13, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.kai;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeDialog.OnApplyNodeModifier;
import org.knime.core.webui.node.dialog.SettingsType;

/**
 * Interface that can be implemented by dialogs to expose their settings to K-AI.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface KaiNodeInterface {

    /**
     * Specifies how an LLM can interact with the node.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param systemMessage that explains the node and how the response should be defined
     * @param outputSchema optional JSON schema defining the model response. If the schema is given, the response given
     *            to {@link KaiNodeInterface#applyConfigureResponse(String, Map, Map)} will be a JSON string.
     */
    record ConfigurePrompt(String systemMessage, String outputSchema) {

        // TODO tool declarations

    }

    // TODO method that executes tool calls

    /**
     * @return the required settings types
     */
    Set<SettingsType> getSettingsTypes();

    /**
     * @return see {@link OnApplyNodeModifier}
     */
    default Optional<OnApplyNodeModifier> getOnApplyNodeModifier() {
        return Optional.empty();
    }

    /**
     * @param settings of the node from which to get the prompt
     * @param specs of the node inputs
     * @return the specification for how the model can interact with the node
     */
    ConfigurePrompt getConfigurePrompt(Map<SettingsType, NodeAndVariableSettingsRO> settings, PortObjectSpec[] specs);

    /**
     * Applies the model response to a node.
     *
     * @param response of the LLM. JSON if the {@link #getConfigurePrompt(Map, PortObjectSpec[])} declares an output
     *            schema
     * @param previousSettings of the node
     * @param settings to write based on the response
     * @throws InvalidSettingsException if the response cannot be applied to the node
     */
    void applyConfigureResponse(final String response,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException;

}
