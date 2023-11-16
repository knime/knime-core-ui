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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;

/**
 * Generic scripting settings which only contain the setting "script". The setting can be saved to the
 * {@link SettingsType#MODEL model} settings or {@link SettingsType#VIEW view} settings.
 *
 * To be used together with {@link ScriptingNodeSettingsService}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
public class ScriptingNodeSettings {

    static final String SCRIPT_CFG_KEY = "script";

    private String m_script;

    private final SettingsType m_scriptSettingsType;

    /**
     * @param script the initial user script
     * @param scriptSettingsType the type of settings to use for the user script
     */
    public ScriptingNodeSettings(final String script, final SettingsType scriptSettingsType) {
        m_script = script;
        m_scriptSettingsType = scriptSettingsType;
    }

    /**
     * @return the script
     */
    public String getScript() {
        return m_script;
    }

    /**
     * Save the {@link SettingsType#MODEL model} settings. Adds the script if it is configured to be saved in the model
     * settings.
     *
     * @param settings the model settings
     */
    public void saveModelSettingsTo(final NodeSettingsWO settings) {
        saveSettingsTo(SettingsType.MODEL, settings);
    }

    /**
     * Save the {@link SettingsType#VIEW view} settings. Adds the script if it is configured to be saved in the view
     * settings.
     *
     * @param settings the view settings
     */
    public void saveViewSettingsTo(final NodeSettingsWO settings) {
        saveSettingsTo(SettingsType.VIEW, settings);
    }

    /**
     * Loads the {@link SettingsType#MODEL model} settings. Loads the script if it is configured to be saved in the
     * model settings.
     *
     * @param settings the model settings
     * @throws InvalidSettingsException if the "script" key is not available
     */
    public void loadModelSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettings(SettingsType.MODEL, settings);
    }

    /**
     * Loads the {@link SettingsType#VIEW view} settings. Loads the script if it is configured to be saved in the view
     * settings.
     *
     * @param settings the view settings
     * @throws InvalidSettingsException if the "script" key is not available
     */
    public void loadViewSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettings(SettingsType.VIEW, settings);
    }

    private void saveSettingsTo(final SettingsType type, final NodeSettingsWO settings) {
        if (m_scriptSettingsType == type) {
            settings.addString(SCRIPT_CFG_KEY, m_script);
        }
    }

    private void loadSettings(final SettingsType type, final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_scriptSettingsType == type) {
            m_script = settings.getString(SCRIPT_CFG_KEY);
        }
    }
}
