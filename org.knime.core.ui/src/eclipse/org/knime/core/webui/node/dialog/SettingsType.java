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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import java.util.Arrays;
import java.util.Optional;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil;
import org.knime.core.webui.node.view.NodeView;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

/**
 * A settings type (usually associated with {@link NodeSettings} instances) denotes whether certain settings are going
 * to be loaded into a {@link NodeModel} or a {@link NodeView}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public enum SettingsType {

        /**
         * Type for settings that belong to the {@link NodeModel}.
         */
        MODEL("model", "variables"),

        /**
         * Type for settings that belong to a {@link NodeView}.
         */
        VIEW("view", "view_variables"),

        /**
         * Type for job manager settings.
         */
        JOB_MANAGER(IOConst.JOB_MANAGER_KEY.get(), JobManagerParametersUtil.JOB_MANAGER_KEY_FE, "no_variables");

    private final String m_configKey;

    private final String m_configKeyFrontend;

    private final String m_variablesConfigKey;

    private SettingsType(final String configKey, final String configKeyFrontend, final String variablesConfigKey) {
        m_configKey = configKey;
        m_configKeyFrontend = configKeyFrontend;
        m_variablesConfigKey = variablesConfigKey;
    }

    private SettingsType(final String configKey, final String variablesConfigKey) {
        m_configKey = configKey;
        m_configKeyFrontend = configKey;
        m_variablesConfigKey = variablesConfigKey;
    }

    /**
     * @return the config key used to store the settings with the node
     */
    public String getConfigKey() {
        return m_configKey;
    }

    /**
     * @return the config key used to reference the settings in the frontend
     */
    public String getConfigKeyFrontend() {
        return m_configKeyFrontend;
    }

    /**
     * Inverse method of {@link #getConfigKeyFrontend()}
     *
     * @param configKey the config key used to store the settings with the node
     * @return the settings type for the given config key
     */
    public static Optional<SettingsType> fromConfigKey(final String configKey) {
        return Arrays.stream(values()).filter(type -> type.getConfigKeyFrontend().equals(configKey)).findFirst();
    }

    /**
     * @return the config key used to store variable settings (i.e. flow variables controlling settings or settings
     *         exposed as flow variables) with the node
     */
    public String getVariablesConfigKey() {
        return m_variablesConfigKey;
    }

    /**
     * Whether the settings of this type are the same or specific to a node.
     *
     * @return true if the settings are node specific, false otherwise
     */
    public boolean isNodeSpecific() {
        return this != JOB_MANAGER;
    }

}
