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
package org.knime.core.webui.node.dialog.defaultdialog.jobmanager;

import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY_ID;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_FACTORY_ID_KEY_FE;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_KEY_FE;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_SETTINGS_KEY_FE;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.hasJobManagerSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_SCHEMA;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_CONDITION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EFFECT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_RULE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_SCOPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toSchemaConstructor;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toUiSchemaElement;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.SectionRendererSpec;
import org.knime.core.webui.node.dialog.internal.SettingsApplier;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.shared.workflow.storage.multidir.util.IOConst;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class to handle sub node (components) specific job manager functionality. Currently only two job managers are
 * supported, the default one and the job manager contained in the streaming extension, why those settings are
 * hard-coded.
 */
public final class JobManagerParametersSubNodeUtil {

    private JobManagerParametersSubNodeUtil() {
    }

    /**
     * Exported for testing purposes
     */
    public static final String SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY =
        "org.knime.core.streaming.SimpleStreamerNodeExecutionJobManagerFactory";

    /**
     * Exported for testing purposes
     */
    public static final String STREAMING_MANAGER_CHUNK_SIZE_KEY = "chunk-size";

    /**
     *
     * @param settings the settings of the node
     * @return true, if the streaming extension is installed or the node settings contain job manager settings
     */
    public static boolean showJobManagerSettings(final NodeSettingsRO settings) {
        return isStreamingExtensionInstalled() || hasJobManagerSettings(settings);
    }

    private static boolean isStreamingExtensionInstalled() {
        final var bundleContext = FrameworkUtil.getBundle(JobManagerParametersSubNodeUtil.class).getBundleContext();
        return Arrays.stream(bundleContext.getBundles()) //
            .anyMatch(bundle -> bundle.getSymbolicName().equals("org.knime.core.streaming"));
    }

    /**
     * Helper to connect a renderer and its uischema
     *
     * @param renderer the renderer for a specific settings
     * @param uiSchema the uischema of the renderer
     */
    public record RendererAndUiSchema(DialogElementRendererSpec<ControlRendererSpec> renderer, ObjectNode uiSchema) {
    }

    /**
     * Add a job manager section to the schema and the uiSchema elements.
     *
     * @param schema the schema to add the fields within the job manager section to
     * @param uiSchemaElements to append the sections ui schema to
     */
    public static void addJobManagerSection(final ObjectNode schema, final ArrayNode uiSchemaElements) {
        final var jobManagerSelectionRenderer =
            new JobManagerSelectionDropdownSubNode(getSimpleStreamingJobManagerFactory()) //
                .at(JOB_MANAGER_FACTORY_ID_KEY_FE);
        final var chunkSizeRenderer = new StreamingJobManagerChunkSizeRenderer() //
            .at(JOB_MANAGER_SETTINGS_KEY_FE, STREAMING_MANAGER_CHUNK_SIZE_KEY);
        final var jobManagerSection = new SectionRendererSpec() {

            @Override
            public Collection<DialogElementRendererSpec> getElements() {
                return List.of(jobManagerSelectionRenderer, chunkSizeRenderer);
            }

            @Override
            public String getTitle() {
                return "Job manager selection";
            }
        }.at(JOB_MANAGER_KEY_FE);

        toSchemaConstructor(jobManagerSection).apply(schema);
        // Rules are not yet supported by imperative renderers, so we need to add them manually
        final var uiSchema = toUiSchemaElement(jobManagerSection);
        ((ObjectNode)((ArrayNode)uiSchema.get(UiSchema.TAG_ELEMENTS)).get(1)).set(TAG_RULE, createShowRule(//
            JsonFormsScopeUtil.toScope(List.of(JOB_MANAGER_FACTORY_ID_KEY_FE), SettingsType.JOB_MANAGER),
            SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY//
        ));

        uiSchemaElements.add(uiSchema);

    }

    /**
     * Transforms the job manager represented as json to node settings.<br/>
     *
     * {@link SettingsApplier#handleJobManagerSettings} further handles the job manager settings
     *
     * @param dataJson the job manager settings as json
     * @return from json extracted job manager settings
     * @throws InvalidSettingsException
     */
    public static NodeSettings toNodeSettings(final JsonNode dataJson) throws InvalidSettingsException {
        final var extractedJobManagerSettings = new NodeSettings("extracted job manager settings");

        if (!dataJson.has(JOB_MANAGER_KEY_FE)) {
            return extractedJobManagerSettings;
        }

        final var jobManagerSettingsJson = dataJson.get(JOB_MANAGER_KEY_FE);
        if (!jobManagerSettingsJson.has(JOB_MANAGER_FACTORY_ID_KEY_FE)) {
            return extractedJobManagerSettings;
        }

        final var selectedJobManagerFactory = jobManagerSettingsJson.get(JOB_MANAGER_FACTORY_ID_KEY_FE).asText();
        if (selectedJobManagerFactory.equals(DEFAULT_JOB_MANAGER_FACTORY_ID)) {
            return extractedJobManagerSettings;
        }

        if (!selectedJobManagerFactory.equals(SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY)) {
            throw new InvalidSettingsException(
                "Custom job managers for components are not supported. Please select a valid job manager.");
        }

        extractedJobManagerSettings.addString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(), selectedJobManagerFactory);
        final var jobManagerSubSettings =
            extractedJobManagerSettings.addNodeSettings(IOConst.JOB_MANAGER_SETTINGS_KEY.get());

        if (jobManagerSettingsJson.has(JOB_MANAGER_SETTINGS_KEY_FE)) {
            final var jobManagerSubSettingsJson = jobManagerSettingsJson.get(JOB_MANAGER_SETTINGS_KEY_FE);

            if (jobManagerSubSettingsJson.has(STREAMING_MANAGER_CHUNK_SIZE_KEY)) {
                jobManagerSubSettings.addInt(STREAMING_MANAGER_CHUNK_SIZE_KEY,
                    jobManagerSubSettingsJson.get(STREAMING_MANAGER_CHUNK_SIZE_KEY).asInt());
            }
        }
        return extractedJobManagerSettings;

    }

    /**
     * Exported for testing purposes
     *
     * @return the node factory of the simple streaming job manager
     */
    public static Optional<NodeExecutionJobManagerFactory> getSimpleStreamingJobManagerFactory() {
        return Optional
            .ofNullable(NodeExecutionJobManagerPool.getJobManagerFactory(SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY));
    }

    private static ObjectNode createShowRule(final String scope, final String valueToMatch) {
        final var mapper = JsonFormsDataUtil.getMapper();

        final var schema = mapper.createObjectNode() //
            .put("const", valueToMatch);

        final var conditionNode = mapper.createObjectNode() //
            .put(TAG_SCOPE, scope) //
            .set(FIELD_NAME_SCHEMA, schema);

        return mapper.createObjectNode() //
            .put(TAG_EFFECT, String.valueOf(EffectType.SHOW)) //
            .set(TAG_CONDITION, conditionNode);
    }

    /**
     * Transforms the job manager settings from NodeSettings to the ObjectNode representation
     *
     * @param data the objectnode to save the settings to
     * @param jobManagerSettings the NodeSettings to parse the settings from
     */
    public static void fromNodeSettings(final ObjectNode data, final NodeSettingsRO jobManagerSettings) {
        final var selectedJobManagerAndChunkSize = getSettingsValues(jobManagerSettings);
        final var selectedJobManager = selectedJobManagerAndChunkSize.getFirst();
        final var chunkSize = selectedJobManagerAndChunkSize.getSecond();

        data.putObject(JOB_MANAGER_KEY_FE)//
            .put(JOB_MANAGER_FACTORY_ID_KEY_FE, selectedJobManager)//
            .putObject(JOB_MANAGER_SETTINGS_KEY_FE).put(STREAMING_MANAGER_CHUNK_SIZE_KEY, chunkSize);
    }

    private static Pair<String, Integer> getSettingsValues(final NodeSettingsRO jobManagerSettings) {
        var chunkSize = 50;
        var selectedJobManager = DEFAULT_JOB_MANAGER_FACTORY_ID;

        try {
            if (jobManagerSettings.containsKey(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get())) {
                selectedJobManager = jobManagerSettings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get());
            }

            if (jobManagerSettings.containsKey(IOConst.JOB_MANAGER_SETTINGS_KEY.get())) {
                final var jobManagerSubSettings =
                    jobManagerSettings.getNodeSettings(IOConst.JOB_MANAGER_SETTINGS_KEY.get());
                if (jobManagerSubSettings.containsKey(STREAMING_MANAGER_CHUNK_SIZE_KEY)) {
                    chunkSize = jobManagerSubSettings.getInt(STREAMING_MANAGER_CHUNK_SIZE_KEY);
                }
            }
        } catch (InvalidSettingsException ex) {
            // should never happen because we explicitly check whether the settings exist (via containsKey)
            throw new IllegalStateException(ex);
        }
        return new Pair<>(selectedJobManager, chunkSize);
    }

}
