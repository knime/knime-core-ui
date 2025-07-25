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

import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.internal.SettingsApplier;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

/**
 * Utility class to handle native node specific job manager functionality. Currently only the default job managers is
 * supported. Selected non-default job managers must be removed to be able to apply the dialog.
 *
 * @author Robin Gerling
 */
public class JobManagerParametersNativeNodeUtil {

    /**
     * Transforms the job manager settings for further processing.<br/>
     * Three options are handled:<br/>
     * 1. settings do not contain job manager sub settings -> keep them as is<br/>
     * 2. settings contain the factoryid of default manager -> remove factory id from the settings<br/>
     * 3. settings contain the factoryid of another manager -> throw exception because they are not supported
     *
     * {@link SettingsApplier#handleJobManagerSettings} further handles the job manager settings
     *
     * @param extractedNodeSettings
     * @throws InvalidSettingsException
     */
    public static final void toNodeSettings(final Map<SettingsType, NodeSettings> extractedNodeSettings)
        throws InvalidSettingsException {
        if (!extractedNodeSettings.containsKey(SettingsType.JOB_MANAGER)) {
            return;
        }

        final var jobManagerSettings = extractedNodeSettings.get(SettingsType.JOB_MANAGER);
        if (!jobManagerSettings.containsKey(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get())) {
            return;
        }

        final var selectedFactoryId = jobManagerSettings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get());
        if (selectedFactoryId.isBlank() || selectedFactoryId.equals(DEFAULT_JOB_MANAGER_FACTORY.getID())) {
            extractedNodeSettings.put(SettingsType.JOB_MANAGER,
                new NodeSettings(SettingsType.JOB_MANAGER.getConfigKey()));
        } else {
            throw new InvalidSettingsException(
                "Custom job managers for nodes are not supported. Please select the default job manager.");
        }

    }

}
