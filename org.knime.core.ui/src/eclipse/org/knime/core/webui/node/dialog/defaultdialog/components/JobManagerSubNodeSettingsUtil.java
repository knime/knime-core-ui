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
 *   23 Jul 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.components;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toUiSchemaElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.PersistSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DropdownRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.IntegerRendererSpec;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.shared.workflow.storage.multidir.util.IOConst;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Robin Gerling
 */
public final class JobManagerSubNodeSettingsUtil {

    private JobManagerSubNodeSettingsUtil() {
    }

    static final NodeLogger LOGGER = NodeLogger.getLogger(JobManagerSubNodeSettingsUtil.class);

    /**
     * public for testing purposes
     */
    public static final String SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY =
        "org.knime.core.streaming.SimpleStreamerNodeExecutionJobManagerFactory";

    private static final String JOB_MANAGER_KEY = replaceDotsByDashes(IOConst.JOB_MANAGER_KEY.get());

    private static final String JOB_MANAGER_FACTORY_ID_KEY =
        replaceDotsByDashes(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get());

    private static final String JOB_MANAGER_SETTINGS_KEY = replaceDotsByDashes(IOConst.JOB_MANAGER_SETTINGS_KEY.get());

    private static final String STREAMING_MANAGER_CHUNK_SIZE_KEY = "chunk-size";

    private static String replaceDotsByDashes(final String str) {
        return str.replace(".", "-");
    }

    private static final DialogElementRendererSpec<ControlRendererSpec> CHUNK_SIZE_RENDERER =
        new IntegerRendererSpec() {

            @Override
            public String getTitle() {
                return "Chunk size";
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.of( //
                    "Determines the size of a batch that is collected at each node before it is handed off to the"
                        + " downstream node. Choosing larger values will reduce synchronization (and hence yield better"
                        + " runtime), whereas small values will make sure that less data is in transit/memory.<br/>"
                        + "For ordinary data (consisting only of strings and numbers) larger values are preferred.");
            }

        }.at(JOB_MANAGER_KEY, JOB_MANAGER_SETTINGS_KEY, STREAMING_MANAGER_CHUNK_SIZE_KEY);

    private static final PersistSchema.PersistLeafSchema PERSIST_LEAF_SCHEMA_WITH_EMPTY_CONFIG_PATHS =
        new PersistSchema.PersistLeafSchema() {

            @Override
            public Optional<String[][]> getConfigPaths() {
                return Optional.of(new String[0][]);
            }

        };

    // the persist tree schema for the job manager settings containing empty config paths for both settings as
    // those settings should not be controlled via flow variable
    private static final PersistSchema PERSIST_SCHEMA = new PersistSchema.PersistTreeSchema.PersistTreeSchemaRecord( //
        Map.of( //
            JOB_MANAGER_FACTORY_ID_KEY, //
            PERSIST_LEAF_SCHEMA_WITH_EMPTY_CONFIG_PATHS, //
            JOB_MANAGER_SETTINGS_KEY, //
            new PersistSchema.PersistTreeSchema.PersistTreeSchemaRecord(
                Map.of(STREAMING_MANAGER_CHUNK_SIZE_KEY, PERSIST_LEAF_SCHEMA_WITH_EMPTY_CONFIG_PATHS))));

    /**
     * Pseudo-factory that returns null - the job manager value that indicates that the parent component's or workflow's
     * job manager should be used if applicable or otherwise the appropriate standard job manager provided by
     * {@link NodeExecutionJobManagerPool#getDefaultJobManagerFactory(Class)}.
     */
    private static final NodeExecutionJobManagerFactory DEFAULT_FACTORY = new NodeExecutionJobManagerFactory() {
        @Override
        public String getID() {
            return getClass().getName();
        }

        @Override
        public String getLabel() {
            return "<<default>>";
        }

        @Override
        public NodeExecutionJobManager getInstance() {
            return null;
        }
    };

    /**
     * @return true if the streaming extension is installed
     */
    public static boolean isStreamingExtensionInstalled() {
        final var bundleContext = FrameworkUtil.getBundle(JobManagerSubNodeSettingsUtil.class).getBundleContext();
        return Stream.of(bundleContext.getBundles()) //
            .filter(bundle -> bundle.getSymbolicName().equals("org.knime.core.streaming")) //
            .findFirst() //
            .isPresent();
    }

    record RendererAndUiSchema(DialogElementRendererSpec<ControlRendererSpec> renderer, ObjectNode uiSchema) {
    }

    static RendererAndUiSchema[] getJobManagerSubNodeSettingsAndSetSelectedValues(final ObjectNode data,
        final NodeAndVariableSettingsRO settings) {
        final var streamingJobManagerFactory = getSimpleStreamingJobManagerFactory();
        final var jobManagerSelectionRenderer = createJobManagerSelectionRenderer(streamingJobManagerFactory);

        final var jobManagerSelectionUiSchema = toUiSchemaElement(jobManagerSelectionRenderer);

        final var chunkSizeUiSchema = toUiSchemaElement(CHUNK_SIZE_RENDERER);
        chunkSizeUiSchema.set("rule",
            createHideRule("#/properties/job-manager/properties/job-manager-factory-id", DEFAULT_FACTORY.getID()));

        setSelectedValues(data, settings, streamingJobManagerFactory);

        return new RendererAndUiSchema[]{
            new RendererAndUiSchema(jobManagerSelectionRenderer, jobManagerSelectionUiSchema),
            new RendererAndUiSchema(CHUNK_SIZE_RENDERER, chunkSizeUiSchema)};
    }

    static PersistSchema setPersistSchema(final HashMap<String, PersistSchema> persistSchemaMap) {
        return persistSchemaMap.put(JOB_MANAGER_KEY, PERSIST_SCHEMA);
    }

    static NodeSettings toNodeSettings(final JsonNode dataJson) {
        final var extractedJobManagerSettings = new NodeSettings("extracted job manager settings");

        if (dataJson.has(JOB_MANAGER_KEY)) {
            final var jobManagerSettingsJson = dataJson.get(JOB_MANAGER_KEY);
            if (jobManagerSettingsJson.has(JOB_MANAGER_FACTORY_ID_KEY)) {
                final var selectedJobManagerFactory = jobManagerSettingsJson.get(JOB_MANAGER_FACTORY_ID_KEY).asText();
                if (selectedJobManagerFactory.equals(DEFAULT_FACTORY.getID())) {
                    return extractedJobManagerSettings;
                }

                extractedJobManagerSettings.addString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(),
                    selectedJobManagerFactory);
                final var jobManagerSubSettings =
                    extractedJobManagerSettings.addNodeSettings(IOConst.JOB_MANAGER_SETTINGS_KEY.get());
                if (jobManagerSettingsJson.has(JOB_MANAGER_SETTINGS_KEY)) {
                    final var jobManagerSubSettingsJson = jobManagerSettingsJson.get(JOB_MANAGER_SETTINGS_KEY);
                    if (jobManagerSubSettingsJson.has(STREAMING_MANAGER_CHUNK_SIZE_KEY)) {
                        jobManagerSubSettings.addInt(STREAMING_MANAGER_CHUNK_SIZE_KEY,
                            jobManagerSubSettingsJson.get(STREAMING_MANAGER_CHUNK_SIZE_KEY).asInt());
                    }
                }
            }
        }
        return extractedJobManagerSettings;
    }

    private static NodeExecutionJobManagerFactory getSimpleStreamingJobManagerFactory() {
        return NodeExecutionJobManagerPool.getJobManagerFactory(SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY);
    }

    private static ObjectNode createHideRule(final String conditionScope, final String valueToMatch) {
        final var mapper = JsonFormsDataUtil.getMapper();
        final var rule = mapper.createObjectNode();
        rule.put("effect", "HIDE");

        final var condition = mapper.createObjectNode();
        condition.put("scope", conditionScope);

        final var schema = mapper.createObjectNode();
        schema.put("const", valueToMatch);

        condition.set("schema", schema);
        rule.set("condition", condition);
        return rule;
    }

    private static void setSelectedValues(final ObjectNode data, final NodeSettingsRO jobManagerSettings,
        final NodeExecutionJobManagerFactory streamingJobManagerFactory) {
        final var selectedJobManagerAndChunkSize = getSettingsValues(jobManagerSettings, streamingJobManagerFactory);
        final var selectedJobManager = selectedJobManagerAndChunkSize.getFirst();
        final var chunkSize = selectedJobManagerAndChunkSize.getSecond();

        final var mapper = new ObjectMapper();

        final var jobManagerSettingsJson = data.putObject(JOB_MANAGER_KEY);
        jobManagerSettingsJson.set(JOB_MANAGER_FACTORY_ID_KEY, mapper.valueToTree(selectedJobManager));

        final var jobManagerSubSettingsJson = jobManagerSettingsJson.putObject(JOB_MANAGER_SETTINGS_KEY);
        jobManagerSubSettingsJson.set(STREAMING_MANAGER_CHUNK_SIZE_KEY, mapper.valueToTree(chunkSize));
    }

    private static Pair<String, Integer> getSettingsValues(final NodeSettingsRO jobManagerSettings,
        final NodeExecutionJobManagerFactory streamingJobManagerFactory) {
        var chunkSize = 50;
        var selectedJobManager = DEFAULT_FACTORY.getID();

        final var factoryIdKey = IOConst.JOB_MANAGER_FACTORY_ID_KEY.get();
        final var settingsKey = IOConst.JOB_MANAGER_SETTINGS_KEY.get();

        try {
            if (jobManagerSettings.containsKey(factoryIdKey)
                && jobManagerSettings.getString(factoryIdKey) == streamingJobManagerFactory.getID()) {
                selectedJobManager = streamingJobManagerFactory.getID();
            }

            if (jobManagerSettings.containsKey(settingsKey)) {
                final var jobManagerSubSettings = jobManagerSettings.getNodeSettings(settingsKey);
                if (jobManagerSubSettings.containsKey(STREAMING_MANAGER_CHUNK_SIZE_KEY)) {
                    chunkSize = jobManagerSubSettings.getInt(STREAMING_MANAGER_CHUNK_SIZE_KEY);
                }
            }
        } catch (InvalidSettingsException ex) {
            // should not happen because we explicitly check whether the keys are available
            LOGGER.error(ex);
        }
        return new Pair<String, Integer>(selectedJobManager, chunkSize);
    }

    private static String escapeLtGt(final String string) {
        return string.replace("<", "&lt;").replace(">", "&gt;");
    }

    private static DialogElementRendererSpec<ControlRendererSpec>
        createJobManagerSelectionRenderer(final NodeExecutionJobManagerFactory streamingJobManagerFactory) {
        return new DropdownRendererSpec() {

            @Override
            public String getTitle() {
                return "Job manager selection";
            }

            @Override
            public Optional<String> getDescription() {
                return Optional.of(String.format("Select the execution mode of the node." //
                    + "<ul>" //
                    + "<li><b>%s:</b> The job manager which executes the nodes in the component node by node.</li>" //
                    + "<li><b>%s:</b> The job manager which executes the nodes in the component concurrently.</li>" //
                    + "</ul>", //
                    escapeLtGt(DEFAULT_FACTORY.getLabel()), streamingJobManagerFactory.getLabel()));
            }

            @Override
            public Optional<DropdownRendererOptions> getOptions() {
                return Optional.of(new DropdownRendererOptions() {

                    @Override
                    public Optional<StringChoice[]> getPossibleValues() {
                        return Optional.of(new StringChoice[]{ //
                            new StringChoice(DEFAULT_FACTORY.getID(), DEFAULT_FACTORY.getLabel()), //
                            new StringChoice(streamingJobManagerFactory.getID(), streamingJobManagerFactory.getLabel()) //
                        });
                    }

                });
            }

        }.at(JOB_MANAGER_KEY, JOB_MANAGER_FACTORY_ID_KEY);
    }

}
