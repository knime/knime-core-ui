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
 *   Nov 16, 2023 (benjamin): created
 */
package org.knime.scripting.editor;

import static org.knime.scripting.editor.ScriptingNodeSettings.SCRIPT_CFG_KEY;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link NodeSettingsService} implementation for scripting editor dialogs.
 * <ul>
 * <li>Provides the setting "script" to the dialog</li>
 * <li>Provides the name of the variable used for the "script" setting to the dialog (if configured)</li>
 * <li>Saves the setting "script" to the {@link NodeSettingsWO} on apply</li>
 * </ul>
 *
 * To be used together with {@link ScriptingNodeSettings}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // NodeSettingsService is no yet public API
public class ScriptingNodeSettingsService implements NodeSettingsService {

    private static final String SCRIPT_JSON_KEY = "script";

    private static final String SCRIPT_USED_FLOW_VAR_JSON_KEY = "scriptUsedFlowVariable";

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperUtil.getInstance().getObjectMapper();

    private final SettingsType m_scriptSettingsType;

    /**
     * @param scriptSettingsType the type of settings to use for the user script
     */
    public ScriptingNodeSettingsService(final SettingsType scriptSettingsType) {
        m_scriptSettingsType = scriptSettingsType;
    }

    /**
     * Override this method to load additional settings when loading settings for the dialog. Read them from the
     * settings objects and write them to the JSON object.
     *
     * @param settings the settings to read from. Forwarded from
     *            {@link NodeSettingsService#fromNodeSettings(Map, PortObjectSpec[])}.
     * @param specs the specs. Forwarded from {@link NodeSettingsService#fromNodeSettings(Map, PortObjectSpec[])}.
     * @param settingsJson a JSON object to write to. This object will be sent to the dialog.
     */
    protected void putAdditionalSettingsToJson(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs, final ObjectNode settingsJson) {
        // Nothing to do in the default implementation
    }

    /**
     * Override this method to save additional settings when applying dialog settings. Read them from the JSON object
     * and write them to the settings objects.
     *
     * @param settingsJson a JSON object to read from. This object was provided by the dialog.
     * @param settings the settings to write to. Forwarded from {@link NodeSettingsService#toNodeSettings(String, Map)}.
     *
     */
    protected void addAdditionalSettingsToNodeSettings(final ObjectNode settingsJson,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        // Nothing to do in the default implementation
    }

    @Override
    public final String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        try {
            var settingsForScript = settings.get(m_scriptSettingsType);
            var script = settingsForScript.getString(SCRIPT_CFG_KEY);

            String scriptUsedFlowVariable = null;
            if (settingsForScript.isVariableSetting(SCRIPT_CFG_KEY)) {
                scriptUsedFlowVariable = settingsForScript.getUsedVariable(SCRIPT_CFG_KEY);
            }

            // Construct the JSON output
            var settingsJson = OBJECT_MAPPER.createObjectNode() //
                .put(SCRIPT_JSON_KEY, script) //
                .put(SCRIPT_USED_FLOW_VAR_JSON_KEY, scriptUsedFlowVariable);
            putAdditionalSettingsToJson(settings, specs, settingsJson);

            return settingsJson.toString();
        } catch (InvalidSettingsException e) {
            // IllegalSettings: Should not happen because we do not save invalid settings
            throw new IllegalStateException(e);
        }
    }

    @Override
    public final void toNodeSettings(final String textSettings,
        final Map<SettingsType, NodeSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        try {
            var settingsJson = (ObjectNode)OBJECT_MAPPER.readTree(textSettings);
            var script = settingsJson.get(SCRIPT_JSON_KEY).asText();
            settings.get(m_scriptSettingsType).addString(SCRIPT_CFG_KEY, script);
            addAdditionalSettingsToNodeSettings(settingsJson, settings);
        } catch (JsonProcessingException e) {
            // Should not happen because the frontend gives a correct JSON settings
            throw new IllegalStateException(e);
        }
    }
}
