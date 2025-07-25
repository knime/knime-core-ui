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
 *   30 Jul 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jobmanager;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

/**
 * Utility class to combine functionality for the job manager settings used by native and sub nodes.
 *
 * @author Robin Gerling
 */
public class JobManagerParametersUtil {

    /**
     * Pseudo-factory that returns null - the job manager value that indicates that the parent component's or workflow's
     * job manager should be used if applicable or otherwise the appropriate standard job manager provided by
     * {@link NodeExecutionJobManagerPool#getDefaultJobManagerFactory(Class)}.
     */
    public static final NodeExecutionJobManagerFactory DEFAULT_JOB_MANAGER_FACTORY = new NodeExecutionJobManagerFactory() {
        @Override
        public String getID() {
            return getClass().getName();
        }

        @Override
        public String getLabel() {
            return "Default Job Manager";
        }

        @Override
        public NodeExecutionJobManager getInstance() {
            return null;
        }
    };

    /**
     * The key for the job manager settings root in the frontend using dashes instead of dots
     */
    public static final String JOB_MANAGER_KEY_FE = replaceDotsByDashes(IOConst.JOB_MANAGER_KEY.get());

    /**
     * The key for the job manager factory id setting in the frontend using dashes instead of dots
     */
    public static final String JOB_MANAGER_FACTORY_ID_KEY_FE =
        replaceDotsByDashes(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get());

    /**
     * The key for the sub settings of a job manager in the frontend using dashes instead of dots
     */
    public static final String JOB_MANAGER_SETTINGS_KEY_FE =
        replaceDotsByDashes(IOConst.JOB_MANAGER_SETTINGS_KEY.get());

    private static String replaceDotsByDashes(final String str) {
        return str.replace(".", "-");
    }

    /**
     * Checks whether the node settings contain the job manager settings root key
     *
     * @param settings the node settings to check for the job manager root key
     * @return whether the settings contain the job manager root key
     */
    public static boolean hasJobManagerSettings(final NodeSettingsRO settings) {
        try {
            final var jobManagerSettings = settings.getNodeSettings(IOConst.JOB_MANAGER_KEY.get());
            return jobManagerSettings.containsKey(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get());
        } catch (InvalidSettingsException ex) { //NOSONAR
            return false;
        }
    }

}
