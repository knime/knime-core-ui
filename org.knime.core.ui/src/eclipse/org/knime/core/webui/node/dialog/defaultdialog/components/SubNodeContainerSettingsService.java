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
package org.knime.core.webui.node.dialog.defaultdialog.components;

import static org.knime.core.node.workflow.SubNodeContainer.getDialogNodeParameterName;
import static org.knime.core.webui.node.dialog.SettingsType.JOB_MANAGER;
import static org.knime.core.webui.node.dialog.SettingsType.MODEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toSchemaConstructor;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toUiSchemaElement;
import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil.textToJson;
import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil.rootJsonToVariableSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.PersistSchema;
import org.knime.core.webui.node.dialog.PersistSchema.PersistTreeSchema.PersistTreeSchemaRecord;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.WebDialogNodeRepresentation;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogDataServiceUtil;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersPersistUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersSubNodeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.PasswordHolder;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("rawtypes")
public final class SubNodeContainerSettingsService implements NodeSettingsService {

    static final NodeLogger LOGGER = NodeLogger.getLogger(SubNodeContainerSettingsService.class);

    /**
     * One of the configurations that is to be shown in a component dialog.
     *
     * @param nc the node container of the sub node
     * @param dialogNode the dialog node implementation of the sub node
     * @param paramName the name where this sub node's value is stored in the model settings
     */
    public record DialogSubNode(NodeContainer nc, DialogNode dialogNode, String paramName) {

        DialogSubNode(final NodeContainer nc, final DialogNode node) {
            this(nc, node, getDialogNodeParameterName(node, nc.getID()));
        }

    }

    private final Supplier<List<DialogSubNode>> m_orderedDialogNodes;

    SubNodeContainerSettingsService(final Supplier<List<DialogSubNode>> orderedDialogNodes) {
        m_orderedDialogNodes = orderedDialogNodes;
    }

    static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /**
     * The collection of renderers constructed with the last call to {@link DialogSubNode#fromNodeSettings}
     */
    Collection<DialogElementRendererSpec> m_renderers;

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final var data = FACTORY.objectNode();
        final var modelSettingsJson = data.putObject(MODEL.getConfigKey());
        final var schema = FACTORY.objectNode();
        final var uiSchema = FACTORY.objectNode();
        final var uiSchemaElements = uiSchema.putArray(UiSchema.TAG_ELEMENTS);
        m_renderers = new ArrayList<>();
        var persistSchemas = new HashMap<String, PersistSchema>();

        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            final var paramName = dialogSubNode.paramName;
            final var dialogNode = dialogSubNode.dialogNode;
            if (!(dialogNode.getDialogRepresentation() instanceof WebDialogNodeRepresentation)) {
                addNonSupportedConfiguration(uiSchemaElements, dialogSubNode.nc.getName());
                continue;
            }

            final var valueJson = wrapWithContext(() -> getValueJson(dialogNode), dialogSubNode.nc);
            modelSettingsJson.set(paramName, valueJson);

            final var representation = getRepresentation(dialogNode);
            final var renderer = representation.getWebUIDialogElementRendererSpec().at(MODEL.getConfigKey(), paramName);
            representation.getPersistSchema().ifPresent(p -> persistSchemas.put(paramName, (PersistSchema)p));
            m_renderers.add(renderer);
            uiSchemaElements.addObject().setAll(toUiSchemaElement(renderer));
            toSchemaConstructor(renderer).apply(schema);
        }

        final var modelPersistSchema = new PersistTreeSchemaRecord(persistSchemas);
        final var persistSchemaMap = new HashMap<String, PersistSchema>();
        persistSchemaMap.put(MODEL.getConfigKey(), modelPersistSchema);

        final var jobManagerSettings = settings.get(JOB_MANAGER);
        if (JobManagerParametersSubNodeUtil.isStreamingExtensionInstalled()
            || JobManagerParametersUtil.hasJobManagerFactoryId(jobManagerSettings)) {
            JobManagerParametersSubNodeUtil.fromNodeSettings(data, jobManagerSettings);
            JobManagerParametersSubNodeUtil.addJobManagerSection(schema, uiSchemaElements);
            JobManagerParametersPersistUtil.setPersistSchema(persistSchemaMap);
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

        final var persistSchema = new PersistTreeSchemaRecord(persistSchemaMap);

        final var context = createContext(specs);
        return new DefaultNodeDialogDataServiceUtil.InitialDataBuilder(jsonFormsSettings)
            .withUpdates(
                (root, dataJson) -> UpdatesUtil.addImperativeUpdates(root, m_renderers, dataJson, createContext(specs)))
            .withPersistSchema(persistSchema).withFlowVariables(map(settings), context).build();

    }

    private static void addNonSupportedConfiguration(final ArrayNode uiSchemaElements, final String nodeName) {
        uiSchemaElements.addObject().put(UiSchema.TAG_TYPE, "ConfigurationNodeNotSupported")
            .putObject(UiSchema.TAG_OPTIONS).put("nodeName", nodeName);

    }

    private static NodeParametersInput createContext(final PortObjectSpec[] specs) {
        return NodeParametersUtil.createDefaultNodeSettingsContext(specs);
    }

    Collection<DialogElementRendererSpec> getRendererSpecs() {
        CheckUtils.checkNotNull(m_renderers, "Unable to access dialog renderers");
        return m_renderers;
    }

    private static WebDialogNodeRepresentation getRepresentation(final DialogNode dialogNode) {
        return CheckUtils.checkCast(dialogNode.getDialogRepresentation(), WebDialogNodeRepresentation.class,
            IllegalStateException::new,
            "WebUI component dialog is only used when all dialog sub nodes are webUI controls");
    }

    private static JsonNode getValueJson(final DialogNode dialogNode) {
        final var value = Optional.ofNullable(dialogNode.getDialogValue()).orElseGet(dialogNode::getDefaultValue);
        return extractJsonFromWebDialogValueAndDialogRepresentation(value, dialogNode.getDialogRepresentation());
    }

    static JsonNode extractJsonFromWebDialogValueAndDialogRepresentation(final DialogNodeValue value,
        final DialogNodeRepresentation dialogRepresentation) {
        try {
            return extractJsonOrThrow(value, dialogRepresentation);
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Unable to extract json content from dialog value and representation: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void toNodeSettings(final String jsonSettings, //
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings, //
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {
        JsonNode root = textToJson(jsonSettings);
        final var extractedModelSettings = new NodeSettings("extracted model settings");
        final var dataJson = root.get("data");
        final var modelSettingsJson = dataJson.get("model");
        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            saveSettingsForSubNode(previousSettings, extractedModelSettings, modelSettingsJson, dialogSubNode);
        }

        alignSettingsWithFlowVariables(previousSettings, root, extractedModelSettings);

        extractedModelSettings.copyTo(settings.get(MODEL));
        rootJsonToVariableSettings(root, map(settings));

        final var extractedJobManagerSettings = JobManagerParametersSubNodeUtil.toNodeSettings(dataJson);
        extractedJobManagerSettings.copyTo(settings.get(JOB_MANAGER));
    }

    private static void alignSettingsWithFlowVariables(
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings, final JsonNode root,
        final NodeSettings extractedModelSettings) {
        final var extractedVariableSettings = VariableSettingsUtil.extractVariableSettings(Set.of(MODEL), root);
        NodeSettingsCorrectionUtil.correctNodeSettingsRespectingFlowVariables(new ConfigMappings(List.of()),
            extractedModelSettings, previousSettings.get(MODEL), extractedVariableSettings.get(MODEL));
    }

    static void saveSettingsForSubNode(final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final NodeSettingsWO modelSettings, final JsonNode modelSettingsJson, final DialogSubNode dialogSubNode)
        throws InvalidSettingsException {
        final var savedSettings = modelSettings.addNodeSettings(dialogSubNode.paramName);
        if (modelSettingsJson.has(dialogSubNode.paramName)) {
            final var inputJson = modelSettingsJson.get(dialogSubNode.paramName);
            final var dialogValue =
                wrapWithContext(() -> loadValueFromJson(dialogSubNode.dialogNode, inputJson), dialogSubNode.nc);

            dialogValue.saveToNodeSettings(savedSettings);
        } else {
            saveSettingsForNonSupportedNode(previousSettings, dialogSubNode, savedSettings);
        }
    }

    private static void saveSettingsForNonSupportedNode(
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings, final DialogSubNode dialogSubNode,
        final NodeSettingsWO savedSettings) throws InvalidSettingsException {
        try {
            final var fromPreviousSettings = previousSettings.get(MODEL).getNodeSettings(dialogSubNode.paramName);
            fromPreviousSettings.copyTo(savedSettings);
        } catch (InvalidSettingsException ex) {
            throw new InvalidSettingsException(
                "Failed to apply due to a non-supported configuration that was not previously configured.", ex);
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

    private static DialogNodeValue loadValueFromJson(final DialogNode dialogNode, final JsonNode inputJson) {
        final var value = dialogNode.createEmptyDialogValue();
        final var representation = dialogNode.getDialogRepresentation();
        try {
            setJsonOrThrow(inputJson, value, representation);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse the settings provided by the dialog", e);
        }
        return value;
    }

    private static WebDialogNodeRepresentation
        assertWebRepresentationOrThrow(final DialogNodeRepresentation dialogNodeRepresentation) {
        if (dialogNodeRepresentation instanceof WebDialogNodeRepresentation webDialogRepresentation) {
            return webDialogRepresentation;
        } else {
            throw new IllegalStateException(String.format("Representation needs to be %s, but is %s",
                WebDialogNodeRepresentation.class.getSimpleName(),
                dialogNodeRepresentation.getClass().getSimpleName()));
        }
    }

    private static JsonNode extractJsonOrThrow(final DialogNodeValue value,
        final DialogNodeRepresentation dialogNodeRepresentation) throws IOException {
        return assertWebRepresentationOrThrow(dialogNodeRepresentation).castAndTransformValueToDialogJson(value);
    }

    private static void setJsonOrThrow(final JsonNode json, final DialogNodeValue value,
        final DialogNodeRepresentation dialogNodeRepresentation) throws IOException {
        assertWebRepresentationOrThrow(dialogNodeRepresentation).castAndSetValueFromDialogJson(json, value);
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
