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
 *   Oct 15, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersWithFallbackProvider;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Create a (boilerplate) extension of this class for a specific subinterface of {@link DynamicNodeParameters} to enable
 * fallback dialog support for a field that uses dynamic parameters.
 *
 * Use it together with {@link DynamicParametersWithFallbackProvider} provider to define fallback support also when
 * dynamically updating settings in the dialog.
 *
 * @author Paul Bärnreuther
 */
public abstract class FallbackDialogNodeParameters implements DynamicNodeParameters {

    /**
     * Creates a new instance of {@link FallbackDialogNodeParameters}.
     *
     * @param nodeSettings the node settings to construct a fallback dialog from
     */
    @SuppressWarnings("unchecked")
    protected FallbackDialogNodeParameters(final NodeSettingsRO nodeSettings) {
        if (nodeSettings instanceof NodeAndVariableSettingsRO) {
            m_nodeSettings = ((Supplier<NodeSettings>)nodeSettings).get();
        } else {
            m_nodeSettings = (NodeSettings)nodeSettings;
        }

    }

    private NodeSettings m_nodeSettings;

    /**
     * @return the nodeSettings used for the fallback dialog
     */
    public NodeSettings getNodeSettings() {
        return m_nodeSettings;
    }

    /**
     * Saves the current settings as JSON object that contains an ID to look up the schema used to create the dialog.
     *
     * @return the JSON object representing the current settings
     */
    public ObjectNode toJson() {
        final var nodeSettings = getNodeSettings();
        return cacheFallbackDialogSchema(FallbackDialogUtils.toJson(nodeSettings), nodeSettings);
    }

    /**
     * Creates a {@link JsonFormsSettings} object that contains the schema and UI schema to create a fallback dialog as
     * well as the current data to be used in that dialog.
     *
     * @return the {@link JsonFormsSettings} object
     */
    public JsonFormsSettings toJsonFormsSettings() {
        final var nodeSettings = getNodeSettings();
        final var jsonFormsSettings = FallbackDialogUtils
            .toJsonFormsSettings(nodeSettings, UnaryOperator.identity(), UnaryOperator.identity()).getSecond();
        final var data = cacheFallbackDialogSchema((ObjectNode)jsonFormsSettings.getData(), nodeSettings);
        final var schema = jsonFormsSettings.getSchema();
        final var uiSchema = jsonFormsSettings.getUiSchema();

        return new JsonFormsSettings() {

            @Override
            public ObjectNode getSchema() {
                return schema;
            }

            @Override
            public ObjectNode getUiSchema() {
                return uiSchema;
            }

            @Override
            public JsonNode getData() {
                return data;
            }

        };
    }

    /**
     * To be used only if the JSON object has been constructed using {@link #toJson} or {@link #toJsonFormsSettings}.
     *
     * @param json the JSON object containing an id to look up the schema
     * @return the node settings extracted from the JSON object
     */
    public static NodeSettings toNodeSettings(final ObjectNode json) {
        var schema = getCachedFallbackDialogSchema(json);
        return FallbackDialogUtils.toNodeSettings(schema, json);
    }

    private static final String FALLBACK_DIALOG_ID_TAG = "fallbackDialogID";

    static final Map<NodeID, Map<String, NodeSettings>> SCHEMA_CACHE = new HashMap<>();

    static ObjectNode cacheFallbackDialogSchema(final ObjectNode json, final NodeSettings nodeSettings) {
        final var id = UUID.randomUUID().toString();
        json.put(FALLBACK_DIALOG_ID_TAG, id);
        SCHEMA_CACHE.computeIfAbsent(getNodeId(), k -> new HashMap<>()).put(id, nodeSettings);
        return json;
    }

    private static NodeSettings getCachedFallbackDialogSchema(final ObjectNode json) {
        final var id = json.get(FALLBACK_DIALOG_ID_TAG).asText();
        final var nodeId = getNodeId();
        if (SCHEMA_CACHE.containsKey(nodeId)) {
            return SCHEMA_CACHE.get(nodeId).get(id);
        }
        throw new IllegalStateException("No cached schema for node " + nodeId + " and id " + id);
    }

    private static NodeID getNodeId() {
        return NodeContext.getContext().getNodeContainer().getID();
    }

    /**
     * Called when the dialog is closed to prevent memory leaks.
     *
     * @param nodeId the node ID to clear the cache for
     */
    public static void clearNodeSettingsCache(final NodeID nodeId) {
        SCHEMA_CACHE.remove(nodeId);
    }

}
