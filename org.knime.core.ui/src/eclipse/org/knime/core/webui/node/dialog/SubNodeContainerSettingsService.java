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
package org.knime.core.webui.node.dialog;

import static org.knime.core.node.workflow.SubNodeContainer.getDialogNodeParameterName;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toSchemaConstructor;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toUiSchemaElement;
import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil.textToJson;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogDataServiceUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.PasswordHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class SubNodeContainerSettingsService implements NodeSettingsService {

    @SuppressWarnings("rawtypes")
    record DialogSubNode(NodeContainer nc, DialogNode dialogNode, String paramName) {

        DialogSubNode(final NodeContainer nc, final DialogNode node) {
            this(nc, node, getDialogNodeParameterName(node, nc.getID()));
        }

    }

    private final Supplier<List<DialogSubNode>> m_orderedDialogNodes;

    SubNodeContainerSettingsService(final Supplier<List<DialogSubNode>> orderedDialogNodes) {
        m_orderedDialogNodes = orderedDialogNodes;
    }

    static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final var data = FACTORY.objectNode();
        final var modelSettingsJson = data.putObject(SettingsType.MODEL.getConfigKey());
        final var schema = FACTORY.objectNode();
        final var uiSchema = FACTORY.objectNode();
        final var uiSchemaElements = uiSchema.putArray(UiSchema.TAG_ELEMENTS);

        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            final var paramName = dialogSubNode.paramName;
            final var dialogNode = dialogSubNode.dialogNode;

            final var valueJson = wrapWithContext(() -> getValueJson(dialogNode), dialogSubNode.nc);
            modelSettingsJson.set(paramName, valueJson);

            final var renderer = getRepresentation(dialogNode).getWebUIDialogControlSpec()
                .at(SettingsType.MODEL.getConfigKey(), paramName);
            uiSchemaElements.addObject().setAll(toUiSchemaElement(renderer));
            toSchemaConstructor(renderer).apply(schema);
        }
        final var jsonFormsSettings = new JsonFormsSettings() {

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
        return new DefaultNodeDialogDataServiceUtil.InitialDataBuilder(jsonFormsSettings).build();

    }

    @SuppressWarnings("rawtypes")
    private static WebDialogNodeRepresentation getRepresentation(final DialogNode dialogNode) {
        return CheckUtils.checkCast(dialogNode.getDialogRepresentation(), WebDialogNodeRepresentation.class,
            IllegalStateException::new,
            "WebUI component dialog is only used when all dialog sub nodes are webUI controls");
    }

    @SuppressWarnings("rawtypes")
    private static JsonNode getValueJson(final DialogNode dialogNode) {
        final var value = Optional.ofNullable(dialogNode.getDialogValue()).orElseGet(dialogNode::getDefaultValue);
        return extractJsonFromWebDialogValue(value);
    }

    private static JsonNode extractJsonFromWebDialogValue(final DialogNodeValue value) {
        try {
            return extractJsonFromWebDialogValueOrThrow(value);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to extract json content from dialog value.", ex);
        }
    }

    private static JsonNode extractJsonFromWebDialogValueOrThrow(final DialogNodeValue value) throws IOException {
        if (value instanceof WebDialogValue webDialogValue) {
            return webDialogValue.toDialogJson();
        } else {
            throw new IllegalStateException(String.format("value needs to be %s, but is %s",
                WebDialogValue.class.getSimpleName(), value.getClass().getSimpleName()));
        }
    }

    @Override
    public void toNodeSettings(final String jsonSettings, //
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings, //
        final Map<SettingsType, NodeAndVariableSettingsWO> settings //
    ) {

        JsonNode newSettingsJson = textToJson(jsonSettings);

        var modelSettings = settings.get(SettingsType.MODEL);
        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            final var inputJson = newSettingsJson.get("data").get("model").get(dialogSubNode.paramName);
            final var dialogValue =
                wrapWithContext(() -> loadValueFromJson(dialogSubNode.dialogNode, inputJson), dialogSubNode.nc);

            dialogValue.saveToNodeSettings(modelSettings.addNodeSettings(dialogSubNode.paramName));
        }
    }

    private static <T> T wrapWithContext(final Supplier<T> supplier, final NodeContainer nc) {
        try {
            NodeContext.pushContext(nc);
            return supplier.get();
        } finally {
            NodeContext.removeLastContext();
        }
    }

    @SuppressWarnings("rawtypes")
    private static DialogNodeValue loadValueFromJson(final DialogNode dialogNode, final JsonNode inputJson) {
        final var value = dialogNode.createEmptyDialogValue();
        final var webDialogValue = CheckUtils.checkCast(value, WebDialogValue.class, IllegalStateException::new,
            "Dialog node values for WebUI dialog controls are of type WebDialogValue. "
                + "This is type-safe since the representation is a WebDialogNodeRepresentation.");
        try {
            webDialogValue.fromDialogJson(inputJson);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse the settings provided by the dialog", e);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * We need to clean up the passwords which were stored during serialization in {@link #fromNodeSettings} for the
     * credentials configuration.
     */
    @Override
    public void deactivate() {
        m_orderedDialogNodes.get().stream().map(DialogSubNode::nc).map(NodeContainer::getID)
            .forEach(PasswordHolder::removeAllPasswordsOfDialog);
    }

}
