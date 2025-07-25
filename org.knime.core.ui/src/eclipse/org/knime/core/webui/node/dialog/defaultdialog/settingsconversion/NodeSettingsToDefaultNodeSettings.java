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
 *   Aug 30, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.settingsconversion;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.internal.LoadWarningsUtil;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.NodeParameters;

/**
 * This class can be used to transform {@link NodeSettings} first to {@link NodeParameters} and then further to
 * {@link JsonFormsSettings}. If the first step of this transformation fails, new {@link NodeParameters} are
 * constructed instead.
 *
 * @author Paul Bärnreuther
 */
public final class NodeSettingsToDefaultNodeSettings {

    private final NodeParametersInput m_context;

    private final Map<SettingsType, Class<? extends NodeParameters>> m_settingsClasses;

    /**
     * @param context
     * @param settingsClasses a map associating settings types with {@link NodeParameters}
     */
    public NodeSettingsToDefaultNodeSettings(final NodeParametersInput context,
        final Map<SettingsType, Class<? extends NodeParameters>> settingsClasses) {
        m_settingsClasses = settingsClasses;
        m_context = context;
    }

    /**
     * Transforms the given node settings to {@link JsonFormsSettings}.
     *
     * @param settings
     * @return the JSON forms representation of the settings
     * @throws InvalidSettingsException if the intermediate transformation of the settings to
     *             {@link NodeParameters} failed.
     */
    public Map<SettingsType, NodeParameters> nodeSettingsToDefaultNodeSettings(
        final Map<SettingsType, NodeSettingsRO> settings) throws InvalidSettingsException {
        try {
            return allNodeSettingsToDefaultNodeSettings(settings, this::fromNodeSettingsToDefaultNodeSettings);
        } catch (GetSettings.UncheckedExceptionCausedByInvalidSettings ex) { //NOSONAR
            throw ex.getInvalidSettingsException();
        }
    }

    /**
     * Transforms the given node settings to {@link JsonFormsSettings} using a default for the intermediate
     * transformation to {@link NodeParameters} in case it fails.
     *
     * @param settings
     * @return the JSON forms representation of the settings
     */
    public Map<SettingsType, NodeParameters>
        nodeSettingsToDefaultNodeSettingsOrDefault(final Map<SettingsType, NodeSettingsRO> settings) {
        return allNodeSettingsToDefaultNodeSettings(settings, this::fromNodeSettingsToDefaultNodeSettingsOrDefault);
    }

    private static Map<SettingsType, NodeParameters> allNodeSettingsToDefaultNodeSettings(
        final Map<SettingsType, NodeSettingsRO> settings, final GetSettings getSettings)
        throws GetSettings.UncheckedExceptionCausedByInvalidSettings {
        final Map<SettingsType, NodeParameters> loadedSettings = new HashMap<>();
        for (var entry : settings.entrySet()) {
            final var type = entry.getKey();
            final var nodeSettings = entry.getValue();
            loadedSettings.put(type, getSettings.getDefaultNodeSettingsUnchecked(type, nodeSettings));
        }
        return loadedSettings;
    }

    private NodeParameters fromNodeSettingsToDefaultNodeSettings(final SettingsType type,
        final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        return NodeParametersUtil.loadSettings(nodeSettings, m_settingsClasses.get(type));
    }

    private NodeParameters fromNodeSettingsToDefaultNodeSettingsOrDefault(final SettingsType type,
        final NodeSettingsRO nodeSettings) {
        try {
            return fromNodeSettingsToDefaultNodeSettings(type, nodeSettings);
        } catch (InvalidSettingsException ex) {
            LoadWarningsUtil.warnAboutDefaultSettingsBeingUsedInstead(ex);
            return NodeParametersUtil.createSettings(m_settingsClasses.get(type), m_context);
        }
    }

    /**
     * Convenience interface for enabling running getDefaultNodeSettings unchecked helping with de-duplication
     */
    @FunctionalInterface
    private interface GetSettings {
        NodeParameters getDefaultNodeSettings(SettingsType type, NodeSettingsRO nodeSettings)
            throws InvalidSettingsException;

        default NodeParameters getDefaultNodeSettingsUnchecked(final SettingsType type,
            final NodeSettingsRO nodeSettings) throws UncheckedExceptionCausedByInvalidSettings {
            try {
                return getDefaultNodeSettings(type, nodeSettings);
            } catch (InvalidSettingsException ex) {
                /**
                 * Transform into unchecked exception in order to be transformed back or caught to apply defaults later,
                 * while allowing different signatures in these methods.
                 */
                throw new UncheckedExceptionCausedByInvalidSettings(ex);
            }
        }

        /**
         * A simple wrapper for making {@link InvalidSettingsException} temporarily unchecked in order to help
         * de-duplicate the code in this class.
         */
        final class UncheckedExceptionCausedByInvalidSettings extends RuntimeException {

            private final InvalidSettingsException m_ex;

            private static final long serialVersionUID = 1L;

            UncheckedExceptionCausedByInvalidSettings(final InvalidSettingsException ex) {
                super(ex);
                m_ex = ex;
            }

            InvalidSettingsException getInvalidSettingsException() {
                return m_ex;
            }

        }

    }

}
