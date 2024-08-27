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

import java.util.Map;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.VariableSettingsWO;

/**
 * Generic scripting settings. The setting can be saved to the {@link SettingsType#MODEL model} settings or
 * {@link SettingsType#VIEW view} settings.
 *
 * To be used together with {@link GenericInitialDataBuilder} to build a {@link ScriptingNodeSettingsService}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // SettingsType is not yet public API
public abstract class ScriptingNodeSettings {

    protected final SettingsType m_scriptSettingsType;

    /**
     * @param scriptSettingsType the type of settings to use for the user script
     */
    public ScriptingNodeSettings(final SettingsType scriptSettingsType) {
        m_scriptSettingsType = scriptSettingsType;
    }

    public void saveSettingsTo(final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        saveSettingsTo(settings.get(m_scriptSettingsType));
    }

    public void loadSettingsFrom(final Map<SettingsType, NodeAndVariableSettingsRO> settings)
        throws InvalidSettingsException {

        loadSettingsFrom(settings.get(m_scriptSettingsType));
    }

    public void validate(final Map<SettingsType, NodeAndVariableSettingsRO> settings) throws InvalidSettingsException {
        loadSettingsFrom(settings);
    }

    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettingsFrom(settings);
    }

    public abstract void saveSettingsTo(final NodeSettingsWO settings);

    public abstract void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Get the name of the flow variable that is used to override the value of the given key. Returns an empty optional
     * if no flow variable is used to override the value.
     *
     * @param settings
     * @param key
     * @return an optional containing the name of the overriding flow variable, or empty if there isn't one
     */
    public static Optional<String> getOverridingFlowVariableName(final NodeAndVariableSettingsRO settings,
        final String key) {

        try {
            String overridingFlowVarName = null;
            if (settings.isVariableSetting(key) && (overridingFlowVarName = settings.getUsedVariable(key)) != null) {
                return Optional.of(overridingFlowVarName);
            } else {
                return Optional.empty();
            }
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Implementation error - failed to query if "
                + "a flow variable was used for key: " + key + " in the settings.", ex);
        }
    }

    /**
     * Check if the value of the given key is overridden by a flow variable.
     *
     * @param settings
     * @param key
     * @return true if the value of the key is overridden by a flow variable, false otherwise
     */
    public static boolean isOverriddenByFlowVariable(final NodeAndVariableSettingsRO settings, final String key) {
        try {
            return settings.isVariableSetting(key) && settings.getUsedVariable(key) != null;
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Implementation error - failed to query if "
                + "a flow variable was used for key: " + key + " in the settings.", ex);
        }
    }

    /**
     * Copies all variable settings from the previous settings to the new settings.
     *
     * @param previousSettings
     * @param settings
     */
    public static void copyVariableSettings(final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        for (var settingType : settings.keySet()) {
            copyVariableSettings(previousSettings.get(settingType), settings.get(settingType));
        }
    }

    /**
     * Utility method to copy all variable settings from a the previous settings to the new settings
     *
     * @param from the previous settings
     * @param to the new settings
     */
    public static void copyVariableSettings(final VariableSettingsRO from, final VariableSettingsWO to) {

        try {
            for (String key : from.getVariableSettingsIterable()) {
                if (from.isVariableSetting(key)) {
                    copyVariableSetting(from, to, key);
                } else {
                    copyVariableSettings(from.getVariableSettings(key), to.getOrCreateVariableSettings(key));
                }
            }
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(
                "Implementation error: failed to copy variable settings from previous settings to new settings", e);
        }
    }

    /**
     * Copies a single variable setting from the previous settings to the new settings
     *
     * @param from the previous settings
     * @param to the new settings
     * @param key the key of the variable setting to copy
     *
     * @throws InvalidSettingsException if the key doesn't correspond to a setting
     */
    public static void copyVariableSetting(final VariableSettingsRO from, final VariableSettingsWO to, final String key)
        throws InvalidSettingsException {

        var usedVariable = from.getUsedVariable(key);
        if (usedVariable != null) {
            to.addUsedVariable(key, usedVariable);
        }
        var exposedVariable = from.getExposedVariable(key);
        if (exposedVariable != null) {
            to.addExposedVariable(key, exposedVariable);
        }
    }
}
