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
 *   Aug 4, 2025 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import static org.knime.core.webui.node.dialog.SettingsType.MODEL;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.map;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigBooleanEntry;
import org.knime.core.node.config.base.ConfigByteEntry;
import org.knime.core.node.config.base.ConfigCharEntry;
import org.knime.core.node.config.base.ConfigDoubleEntry;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.config.base.ConfigFloatEntry;
import org.knime.core.node.config.base.ConfigIntEntry;
import org.knime.core.node.config.base.ConfigLongEntry;
import org.knime.core.node.config.base.ConfigPasswordEntry;
import org.knime.core.node.config.base.ConfigShortEntry;
import org.knime.core.node.config.base.ConfigStringEntry;
import org.knime.core.node.config.base.ConfigTransientStringEntry;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogDataServiceUtil;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogUIExtension;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DefaultDialogDataConverter;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl.FlowVariableDataServiceImpl;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CheckboxRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CredentialsRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.IntegerRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Fallback dialog for native nodes with widgets for every leaf of the {@link NodeSettings}-tree. The widget is
 * determined by the type (cp. {@link ConfigEntries}).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
final class FallbackDialogFactory implements NodeDialogFactory {

    static final String FALLBACK_DIALOG_ENABLED_SYS_PROP = "org.knime.ui.enableFallbackDialog";

    static Boolean fallbackDialogEnabled;

    static boolean isFallbackDialogEnabled() {
        if (fallbackDialogEnabled == null) {
            var sysProp = System.getProperty(FALLBACK_DIALOG_ENABLED_SYS_PROP);
            if (sysProp != null) {
                fallbackDialogEnabled = Boolean.parseBoolean(sysProp);
            } else {
                fallbackDialogEnabled = GraphicsEnvironment.isHeadless();
            }
        }
        return fallbackDialogEnabled.booleanValue();

    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final NativeNodeContainer m_nnc;

    FallbackDialogFactory(final NativeNodeContainer nnc) {
        m_nnc = nnc;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new FallbackDialog(m_nnc);
    }

    private static class FallbackDialog implements NodeDialog, DefaultNodeDialogUIExtension {

        private final FallbackDialogSettingsService m_settingsService;

        private final DefaultDialogDataConverter m_dataConverter;

        FallbackDialog(final NativeNodeContainer nnc) {
            m_settingsService = new FallbackDialogSettingsService();
            m_dataConverter = new DefaultDialogDataConverter() {

                @Override
                public NodeSettings dataJsonToNodeSettings(final JsonNode dataJson, final SettingsType type)
                    throws InvalidSettingsException {
                    var schema = nnc.getNodeSettings().getNodeSettings("model");
                    return FallbackDialogFactory.toNodeSettings(schema, dataJson.get("model"));
                }

                @Override
                public JsonNode nodeSettingsToDataJson(final SettingsType type, final NodeSettingsRO nodeSettings,
                    final NodeParametersInput context) throws InvalidSettingsException {
                    var configInfos = collectConfigInfos(nodeSettings);
                    final var dataJson = MAPPER.createObjectNode();
                    for (var configInfo : configInfos) {
                        final JsonNode valueJson = MAPPER.valueToTree(configInfo.value);
                        setAtPath(dataJson, configInfo.path, valueJson);
                    }
                    return dataJson;
                }

            };
        }

        @Override
        public Set<SettingsType> getSettingsTypes() {
            return Set.of(SettingsType.MODEL);
        }

        @Override
        public NodeSettingsService getNodeSettingsService() {
            return m_settingsService;
        }

        @Override
        public Optional<RpcDataService> createRpcDataService() {
            final var flowVariableService = new FlowVariableDataServiceImpl(m_dataConverter);
            return Optional.of(RpcDataService.builder() //
                .addService("flowVariables", flowVariableService) //
                .build() //
            );
        }

    }

    private static class FallbackDialogSettingsService implements NodeSettingsService {

        @Override
        public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
            final PortObjectSpec[] specs) {
            final var data = MAPPER.createObjectNode();
            final var modelSettingsJson = data.putObject(MODEL.getConfigKey());
            final var schema = MAPPER.createObjectNode();
            final var uiSchema = MAPPER.createObjectNode();
            final var uiSchemaElements = uiSchema.putArray(UiSchema.TAG_ELEMENTS);
            var renderers = new ArrayList<DialogElementRendererSpec>();

            var configInfos = collectConfigInfos(settings.get(SettingsType.MODEL));
            for (var configInfo : configInfos) {
                final JsonNode valueJson = MAPPER.valueToTree(configInfo.value);
                setAtPath(modelSettingsJson, configInfo.path, valueJson);
                if (configInfo.renderer != null) {
                    var renderer = configInfo.renderer.at(SettingsType.MODEL.getConfigKey());
                    renderers.add(renderer);
                    uiSchemaElements.addObject().setAll(RendererToJsonFormsUtil.toUiSchemaElement(renderer));
                    RendererToJsonFormsUtil.toSchemaConstructor(renderer).apply(schema);
                }
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

            final var context = createContext(specs);
            return new DefaultNodeDialogDataServiceUtil.InitialDataBuilder(jsonFormsSettings).withUpdates(
                (root, dataJson) -> UpdatesUtil.addImperativeUpdates(root, renderers, dataJson, createContext(specs)))
                .withFlowVariables(map(settings), context).build();
        }

        @Override
        public void toNodeSettings(final String jsonSettings,
            final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
            final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {

            JsonNode rootJson;
            try {
                rootJson = MAPPER.readTree(jsonSettings);
            } catch (JsonProcessingException ex) {
                // should never happen
                throw new InvalidSettingsException("Failed to parse JSON settings: " + ex.getMessage(), ex);
            }
            var modelJson = rootJson.get("data").get(MODEL.getConfigKey());

            var schema = new NodeSettings("ignored");
            previousSettings.get(SettingsType.MODEL).copyTo(schema);
            var extractedModelSettings = FallbackDialogFactory.toNodeSettings(schema, modelJson);

            alignSettingsWithFlowVariables(previousSettings, rootJson, extractedModelSettings);

            extractedModelSettings.copyTo(settings.get(MODEL));
            VariableSettingsUtil.rootJsonToVariableSettings(rootJson, map(settings));
        }

        private static NodeParametersInput createContext(final PortObjectSpec[] specs) {
            return NodeParametersUtil.createDefaultNodeSettingsContext(specs);
        }

        private static void alignSettingsWithFlowVariables(
            final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings, final JsonNode root,
            final NodeSettings extractedModelSettings) {
            final var extractedVariableSettings = VariableSettingsUtil.extractVariableSettings(Set.of(MODEL), root);
            NodeSettingsCorrectionUtil.correctNodeSettingsRespectingFlowVariables(new ConfigMappings(List.of()),
                extractedModelSettings, previousSettings.get(MODEL), extractedVariableSettings.get(MODEL));
        }

    }

    /**
     * Turns a given JSON object into a {@link NodeSettings}-object. Since the JSON object lags the complete type
     * information required for the settings, another {@link NodeSettings}-object (called 'schema') of the same
     * structure must be supplied where to get the type-infos from (i.e. ConfigEntries).
     *
     * @param schema node settings serving as a schema where only the settings-structure and types are taken into
     *            account but not the actual values
     * @param jsonSettings
     * @return
     */
    private static NodeSettings toNodeSettings(final NodeSettings schema, final JsonNode jsonSettings) {
        final var extractedModelSettings = new NodeSettings("extracted model settings");
        BiConsumer<String[], AbstractConfigEntry> visitor = (path, entry) -> {
            var leaf = jsonSettings;
            for (int i = 0; i < path.length; i++) {
                leaf = leaf.get(path[i]);
            }
            var type = entry.getType();
            AbstractConfigEntry newEntry;
            var key = path[path.length - 1];
            switch (type) {
                case xboolean -> {
                    newEntry = new ConfigBooleanEntry(key, leaf.booleanValue());
                }
                case xint -> {
                    newEntry = new ConfigIntEntry(key, leaf.intValue());
                }
                case xdouble -> {
                    newEntry = new ConfigDoubleEntry(key, leaf.doubleValue());
                }
                case xfloat -> {
                    newEntry = new ConfigFloatEntry(key, leaf.floatValue());
                }
                case xlong -> {
                    newEntry = new ConfigLongEntry(key, leaf.longValue());
                }
                case xshort -> {
                    newEntry = new ConfigShortEntry(key, (short)leaf.intValue());
                }
                case xbyte -> {
                    newEntry = new ConfigByteEntry(key, (byte)leaf.intValue());
                }
                case xstring -> {
                    newEntry = new ConfigStringEntry(key, leaf.asText());
                }
                case xchar -> {
                    newEntry = new ConfigCharEntry(key, leaf.asText());
                }
                case xpassword -> {
                    newEntry = new ConfigPasswordEntry(key, leaf.asText());
                }
                case xtransientstring -> {
                    newEntry = new ConfigTransientStringEntry(key, leaf.asText());
                }
                default -> {
                    throw new UnsupportedOperationException(
                        "Unsupported config type: " + type + " for path: " + Arrays.toString(path));
                }
            }
            setAtPath(extractedModelSettings, path, newEntry);

        };
        traverseConfig(schema, new String[0], visitor);
        return extractedModelSettings;
    }

    private static List<ConfigInfo> collectConfigInfos(final NodeSettingsRO settings) {

        var infos = new ArrayList<ConfigInfo>();
        BiConsumer<String[], AbstractConfigEntry> visitor = (path, entry) -> {
            DialogElementRendererSpec renderer;
            Object value;
            var type = entry.getType();
            var pathString = Arrays.stream(path).collect(Collectors.joining("/"));
            switch (type) {
                case xboolean -> {
                    renderer = new CheckboxRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigBooleanEntry)entry).getBoolean();
                }
                case xint -> {
                    renderer = new IntegerRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigIntEntry)entry).getInt();
                }
                case xdouble -> {
                    renderer = new NumberRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigDoubleEntry)entry).getDouble();
                }
                case xfloat -> {
                    renderer = new NumberRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigFloatEntry)entry).getFloat();
                }
                case xlong -> {
                    renderer = new IntegerRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigLongEntry)entry).getLong();
                }
                case xshort -> {
                    renderer = new IntegerRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigShortEntry)entry).getShort();
                }
                case xbyte -> {
                    renderer = new IntegerRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigByteEntry)entry).getByte();
                }
                case xstring -> {
                    renderer = new TextRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = entry.toStringValue();
                }
                case xchar -> {
                    renderer = new TextRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = entry.toStringValue();
                }
                case xpassword -> {
                    renderer = new CredentialsRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = ((ConfigPasswordEntry)entry).getPassword();
                }
                case xtransientstring -> {
                    renderer = new TextRendererSpec() {

                        @Override
                        public String getTitle() {
                            return pathString;
                        }
                    };
                    value = entry.toStringValue();
                }
                default -> {
                    throw new IllegalStateException();
                }
            }
            var isInternal = Arrays.stream(path).anyMatch(p -> p.endsWith(SettingsModel.CFGKEY_INTERNAL));
            infos.add(new ConfigInfo(path, value, isInternal ? null : renderer.at(path)));
        };
        var tmp = new NodeSettings("ignored");
        settings.copyTo(tmp);
        traverseConfig(tmp, new String[0], visitor);
        return infos;
    }

    private static record ConfigInfo(String[] path, Object value, DialogElementRendererSpec renderer) {
        //
    }

    private static void traverseConfig(final Config config, final String[] path,
        final BiConsumer<String[], AbstractConfigEntry> visitor) {
        var iterator = config.iterator();

        while (iterator.hasNext()) {
            final var key = iterator.next();
            final var entry = config.getEntry(key);
            final var type = entry.getType();

            var newPath = ArrayUtils.insert(path.length, path, key);
            if (type == ConfigEntries.config) {
                traverseConfig((Config)entry, newPath, visitor);
            } else {
                visitor.accept(newPath, entry);
            }
        }
    }

    private static void setAtPath(final ObjectNode json, final String[] path, final JsonNode value) {

        ObjectNode currentNode = json;
        for (int i = 0; i < path.length - 1; i++) {
            var nextNode = currentNode.has(path[i]) ? (ObjectNode)currentNode.get(path[i]) : MAPPER.createObjectNode();
            currentNode.set(path[i], nextNode);
            currentNode = nextNode;
        }
        currentNode.set(path[path.length - 1], value);
    }

    private static void setAtPath(final NodeSettings settings, final String[] path, final AbstractConfigEntry value) {

        var currentSettings = settings;
        for (int i = 0; i < path.length - 1; i++) {
            NodeSettings nextSettings = null;
            try {
                nextSettings = currentSettings.containsKey(path[i]) ? currentSettings.getNodeSettings(path[i])
                    : new NodeSettings(path[i]);
            } catch (InvalidSettingsException ex) {
                // should never happen
            }
            currentSettings.addNodeSettings(nextSettings);
            currentSettings = nextSettings;
        }
        currentSettings.addEntry(value);
    }
}
