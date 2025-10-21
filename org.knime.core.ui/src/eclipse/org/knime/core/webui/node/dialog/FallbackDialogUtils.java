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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;
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
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CheckboxRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CredentialsRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.IntegerRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextMessageRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.util.DotSubstitutionUtil;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility methods for fallback dialog implementation. Used both for dynamically generated parts of dialogs and for the
 * {@link FallbackDialogFactory} itself.
 *
 * @author Paul Bärnreuther
 */
public final class FallbackDialogUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FallbackDialogUtils() {
        // prevent instantiation
    }

    static ObjectNode toJson(final NodeSettingsRO nodeSettings) {
        final var configInfos = collectConfigInfos(nodeSettings);
        final var objectNode = MAPPER.createObjectNode();
        for (var configInfo : configInfos) {
            final JsonNode valueJson = MAPPER.valueToTree(configInfo.value());
            setAtPath(objectNode, configInfo.path(), valueJson);
        }
        return objectNode;
    }

    static Pair<List<DialogElementRendererSpec>, JsonFormsSettings>
        toJsonFormsSettingsForModelSettings(final NodeSettingsRO settings) {
        return toJsonFormsSettings(settings, //
            data -> data.putObject(SettingsType.MODEL.getConfigKey()), //
            renderer -> renderer.at(SettingsType.MODEL.getConfigKey()));
    }

    static Pair<List<DialogElementRendererSpec>, JsonFormsSettings> toJsonFormsSettings(final NodeSettingsRO settings,
        final UnaryOperator<ObjectNode> nestData, final UnaryOperator<DialogElementRendererSpec> nestRenderer) {
        final var data = MAPPER.createObjectNode();
        final var nestedData = nestData.apply(data);
        final var schema = MAPPER.createObjectNode();
        final var uiSchema = MAPPER.createObjectNode();
        final var uiSchemaElements = uiSchema.putArray(UiSchema.TAG_ELEMENTS);
        var renderers = new ArrayList<DialogElementRendererSpec>();

        addFallbackDialogInfo(uiSchemaElements, renderers);

        var configInfos = collectConfigInfos(settings);
        for (var configInfo : configInfos) {
            final JsonNode valueJson = MAPPER.valueToTree(configInfo.value());
            setAtPath(nestedData, configInfo.path(), valueJson);
            if (configInfo.renderer() != null) {
                var renderer = nestRenderer.apply(configInfo.renderer());
                renderers.add(renderer);
                uiSchemaElements.addObject().setAll(RendererToJsonFormsUtil.toUiSchemaElement(renderer));
                RendererToJsonFormsUtil.toSchemaConstructor(renderer).apply(schema);
            }
        }
        return new Pair<>(renderers, new JsonFormsSettings() {

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

        });
    }

    private static void addFallbackDialogInfo(final ArrayNode uiSchemaElements,
        final ArrayList<DialogElementRendererSpec> renderers) {
        var textMessageRendererSpec = new TextMessageRendererSpec() {

            @Override
            public Optional<TextMessageRendererOptions> getOptions() {
                return Optional.of(new TextMessageRendererOptions() {

                    @Override
                    public Optional<Message> getMessage() {
                        return Optional.of(new Message("Auto-generated dialog",
                            """
                            You’re seeing an auto-generated dialog because the original one is not supported here yet.
                            You can still configure the node as usual. See the node description for more details.
                            """,
                            TextMessage.MessageType.INFO));
                    }

                });
            }

            @Override
            public List<String> getPathWithinValueJsonObject() {
                return List.of(SettingsType.MODEL.getConfigKeyFrontend());
            }

        };
        renderers.add(textMessageRendererSpec);
        uiSchemaElements.addObject().setAll(RendererToJsonFormsUtil.toUiSchemaElement(textMessageRendererSpec));
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
            final var pathWithReplacedDots = DotSubstitutionUtil.substituteDots(path);
            infos.add(
                new ConfigInfo(pathWithReplacedDots, value, isInternal ? null : renderer.at(pathWithReplacedDots)));
        };
        var tmp = new NodeSettings("ignored");
        settings.copyTo(tmp);
        traverseConfig(tmp, new String[0], visitor);
        return infos;
    }

    static record ConfigInfo(String[] path, Object value, DialogElementRendererSpec renderer) {
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
                throw new IllegalStateException(
                    "Failed to get or create NodeSettings at path: " + Arrays.toString(path), ex);
            }
            currentSettings.addNodeSettings(nextSettings);
            currentSettings = nextSettings;
        }
        currentSettings.addEntry(value);
    }

    /**
     * Turns a given JSON object into a {@link NodeSettings}-object. Since the JSON object lacks the complete type
     * information required for the settings, another {@link NodeSettings}-object (called 'schema') of the same
     * structure must be supplied where to get the type-infos from (i.e. ConfigEntries).
     *
     * @param schema node settings serving as a schema where only the settings-structure and types are taken into
     *            account but not the actual values
     * @param jsonSettings
     * @return
     */
    static NodeSettings toNodeSettings(final NodeSettings schema, final JsonNode jsonSettings) {
        final var extractedModelSettings = new NodeSettings("extracted model settings");
        BiConsumer<String[], AbstractConfigEntry> visitor = (path, entry) -> {
            var leaf = jsonSettings;
            final var pathWithReplacedDots = DotSubstitutionUtil.substituteDots(path);
            for (int i = 0; i < pathWithReplacedDots.length; i++) {
                leaf = leaf.get(pathWithReplacedDots[i]);
            }
            var type = entry.getType();
            var key = path[path.length - 1];
            AbstractConfigEntry newEntry = switch (type) {
                case xboolean -> new ConfigBooleanEntry(key, leaf.booleanValue());
                case xint -> new ConfigIntEntry(key, leaf.intValue());
                case xdouble -> new ConfigDoubleEntry(key, leaf.doubleValue());
                case xfloat -> new ConfigFloatEntry(key, leaf.floatValue());
                case xlong -> new ConfigLongEntry(key, leaf.longValue());
                case xshort -> new ConfigShortEntry(key, (short)leaf.intValue());
                case xbyte -> new ConfigByteEntry(key, (byte)leaf.intValue());
                case xstring -> new ConfigStringEntry(key, leaf == null ? null : leaf.asText());
                case xchar -> new ConfigCharEntry(key, leaf.asText());
                case xpassword -> new ConfigPasswordEntry(key, leaf.asText());
                case xtransientstring -> new ConfigTransientStringEntry(key, leaf.asText());
                case config -> throw new UnsupportedOperationException(String.format(
                    "Unsupported settings type \"%s\" for for path: %s", ConfigEntries.config, Arrays.toString(path)));
            };
            setAtPath(extractedModelSettings, path, newEntry);

        };
        traverseConfig(schema, new String[0], visitor);
        return extractedModelSettings;
    }

}
