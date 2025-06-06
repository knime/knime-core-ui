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

import java.util.Map;
import java.util.function.BiConsumer;

import org.knime.core.webui.node.dialog.PersistSchema;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;

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

        private BiConsumer<ObjectNode, ObjectNode> m_addUpdates;

        /**
         * Creates a new builder for the initial data.
         *
         * @param jsonFormsSettings the base settings
         */
        public InitialDataBuilder(final JsonFormsSettings jsonFormsSettings) {
            m_jsonFormsSettings = jsonFormsSettings;

        }

        /**
         * Set flow variables to display them correctly initially in the dialog.
         *
         * @param flowVariables as a map indexed by settings type
         * @param context the current context
         * @return this builder
         */
        public InitialDataBuilder withFlowVariables(final Map<SettingsType, VariableSettingsRO> flowVariables,
            final DefaultNodeSettingsContext context) {
            m_flowVariableSettings = VariableSettingsUtil.getVariableSettingsJson(flowVariables, context);
            return this;
        }

        /**
         * Sets the persist schema to be used for the dialog.
         *
         * @param persistSchema the persist schema to be used for the dialog
         * @return this builder
         */
        public InitialDataBuilder withPersistSchema(final PersistSchema persistSchema) {
            m_persistSchema = PersistSchemaToJsonUtil.transformToJson(persistSchema);
            return this;
        }

        /**
         * @param addUpdates a callback consuming the root JSON as first and the data JSON as second argument
         * @return this builder
         */
        public InitialDataBuilder withUpdates(final BiConsumer<ObjectNode, ObjectNode> addUpdates) {
            m_addUpdates = addUpdates;
            return this;
        }

        ObjectNode buildJson() {
            final var root = JsonFormsDataUtil.getMapper().createObjectNode();
            final var data = (ObjectNode)m_jsonFormsSettings.getData();
            root.set(FIELD_NAME_DATA, data);
            root.set(FIELD_NAME_SCHEMA, m_jsonFormsSettings.getSchema());
            root.set(FIELD_NAME_UI_SCHEMA, m_jsonFormsSettings.getUiSchema());
            if (m_addUpdates != null) {
                m_addUpdates.accept(root, data);
            }
            final var flowVariableSettings = m_flowVariableSettings == null
                ? JsonFormsDataUtil.getMapper().createObjectNode() : m_flowVariableSettings;
            root.set(FLOW_VARIABLE_SETTINGS_KEY, flowVariableSettings);

            if (m_persistSchema != null) {
                root.set(PERSIST_KEY, m_persistSchema);
            }
            return root;
        }

        /**
         * @return the built initial data
         */
        public String build() {
            final var rootNode = buildJson();
            return TextToJsonUtil.jsonToString(rootNode);
        }

    }

}
