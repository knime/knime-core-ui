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
 *   Apr 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_DATA;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_SCHEMA;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_UI_SCHEMA;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common functionality for data communication of the {@link DefaultNodeDialogUIExtension}.
 *
 * @author Paul Bärnreuther
 */
public final class DefaultNodeDialogDataServiceUtil {

    private DefaultNodeDialogDataServiceUtil() {
        // utility class
    }

    /**
     * Used for creating initial data suitable for default node dialogs.
     */
    public static class InitialDataBuilder {

        /**
         * Currently duplicated from {@link PersistUtil}. To be deduplicated with UIEXT-2571
         */
        static final String PERSIST_KEY = "persist";

        /**
         * Currently duplicated from {@link VariableSettingsUtil}. To be deduplicated with UIEXT-2571
         */
        static final String FLOW_VARIABLE_SETTINGS_KEY = "flowVariableSettings";

        private final JsonFormsSettings m_jsonFormsSettings;

        private ObjectNode m_flowVariableSettings;

        private ObjectNode m_persistSchema;

        /**
         * Creates a new builder for the initial data.
         *
         * @param jsonFormsSettings the base settings
         */
        public InitialDataBuilder(final JsonFormsSettings jsonFormsSettings) {
            m_jsonFormsSettings = jsonFormsSettings;

        }

        /**
         * With this method, setting flow variables in the dialog can be enabled.
         *
         * @param flowVariableSettings an object containing the currently set flow variables
         * @param persistSchema the schema defining which variables are (to be) set where in the dialog
         */
        public void withFlowVariables(final ObjectNode flowVariableSettings, final ObjectNode persistSchema) {
            m_flowVariableSettings = flowVariableSettings;
            m_persistSchema = persistSchema;
        }

        ObjectNode buildJson() {
            return jsonFormsSettingsToJson(m_jsonFormsSettings, JsonFormsDataUtil.getMapper());
        }

        /**
         * @return the built initial data
         */
        public String build() {
            final var rootNode = buildJson();
            final var flowVariableSettings = m_flowVariableSettings == null
                ? JsonFormsDataUtil.getMapper().createObjectNode() : m_flowVariableSettings;
            rootNode.set(FLOW_VARIABLE_SETTINGS_KEY, flowVariableSettings);
            final var persistSchema = m_persistSchema == null ? createHidingPersistSettings() : m_persistSchema;
            rootNode.set(PERSIST_KEY, persistSchema);
            return TextToJsonUtil.jsonToString(rootNode);
        }

        /**
         * Currently duplicated from {@link PersistUtil}. To be deduplicated with UIEXT-2571
         */
        private static ObjectNode createHidingPersistSettings() {
            final var objectNode = JsonFormsDataUtil.getMapper().createObjectNode();
            objectNode.put("type", "object");
            objectNode.putObject("properties");
            objectNode.putArray("propertiesConfigPaths");
            return objectNode;
        }

        private static ObjectNode jsonFormsSettingsToJson(final JsonFormsSettings jsonFormsSettings,
            final ObjectMapper mapper) {
            final var root = mapper.createObjectNode();
            root.set(FIELD_NAME_DATA, jsonFormsSettings.getData());
            root.set(FIELD_NAME_SCHEMA, jsonFormsSettings.getSchema());
            root.set(FIELD_NAME_UI_SCHEMA, jsonFormsSettings.getUiSchema());
            return root;
        }
    }

}
