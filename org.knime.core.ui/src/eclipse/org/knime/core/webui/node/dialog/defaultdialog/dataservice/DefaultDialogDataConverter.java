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
 *   Oct 24, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl.FlowVariableDataServiceImpl;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A settings converter as used by the {@link FlowVariableDataServiceImpl}
 *
 * @noimplement because it leaks implementation details (jackson)
 * @noreference because it leaks implementation details (jackson)
 *
 * @author Paul Bärnreuther
 */
public interface DefaultDialogDataConverter {

    /**
     * Transforms the JSON representation of the data form the front-end to node settings of a certain settings type.
     *
     * This is used by the {@link FlowVariableDataServiceImpl}
     * <ul>
     * <li>to find out which variables are compatible with which settings by getting the variable types that can
     * overwrite a setting
     * ({@link VariableTypeRegistry#getOverwritingTypes(org.knime.core.node.config.Config, String)}).</li>
     * <li>to overwrite a setting of the {@link NodeSettings} with a variable and return the overwritten value for
     * displaying it in the frontend.</li>
     * </ul>
     *
     * @param dataJson the JSON representation of the data. The top level keys are values of
     *            {@link SettingsType#getConfigKey()}, i.e. either "model" or "view" and it is given that the key for
     *            the given type exists.
     * @param type the type of the to be extracted node settings
     * @return the node settings of the given type
     * @throws InvalidSettingsException if creating node settings from the JSON is not possible
     */
    NodeSettings dataJsonToNodeSettings(final JsonNode dataJson, final SettingsType type)
        throws InvalidSettingsException;

    /**
     * Transforms node settings to the data representation given to the front-end.
     *
     * This is used by the {@link FlowVariableDataServiceImpl} to get the value of settings overwritten by flow
     * variables after the user has selected a variable. The value is returned to the frontend and displayed to the
     * user.
     *
     * @param type the type of settings that is used
     * @param nodeSettings the input node settings of the given type
     * @param context
     * @return The resulting representation of the data as JSON. Its top level keys have to be values of
     *         {@link SettingsType#getConfigKey()} containing the key of the given type.
     * @throws InvalidSettingsException if loading the settings to JSON is not possible
     */
    JsonNode nodeSettingsToDataJson(SettingsType type, NodeSettingsRO nodeSettings, NodeParametersInput context)
        throws InvalidSettingsException;

}
