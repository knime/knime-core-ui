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
 *   30 Jul 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jobmanager;

import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY_ID;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_FACTORY_ID_KEY_FE;
import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_KEY_FE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toSchemaConstructor;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil.toUiSchemaElement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.SectionRendererSpec;
import org.knime.core.webui.node.dialog.internal.SettingsApplier;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class to handle native node specific job manager functionality. Currently only the default job managers is
 * supported. Selected non-default job managers must be removed to be able to apply the dialog.
 *
 * @author Robin Gerling
 */
@SuppressWarnings("rawtypes")
public class JobManagerParametersNativeNodeUtil {

    private static final DialogElementRendererSpec onlyDefaultJobManagerSelection =
        new JobManagerSelectionDropdown(new StringChoice[]{new StringChoice(//
            DEFAULT_JOB_MANAGER_FACTORY_ID, //
            DEFAULT_JOB_MANAGER_FACTORY_LABEL //
        )}) {
            @Override
            public Optional<String> getDescription() {
                return Optional
                    .of("The selection of a custom job manager for nodes is deprecated, why previously selected job"
                        + " managers appear as missing even though they are installed. The node can still be executed as long as"
                        + " the node settings do not change. Changing the node settings requires a valid job manager to be set.");
            }

        }.at(JOB_MANAGER_KEY_FE, JOB_MANAGER_FACTORY_ID_KEY_FE);

    private static final DialogElementRendererSpec jobManagerSection = new SectionRendererSpec() {
        @Override
        public Collection<DialogElementRendererSpec> getElements() {
            return List.of(onlyDefaultJobManagerSelection);
        }

        @Override
        public String getTitle() {
            return "Job manager selection";
        }
    };

    /**
     * NodeSettings -> job manager id
     *
     * @param jobManagerSettings
     *
     * @return the job manager id if it is not the default job manager, otherwise an empty optional
     */
    public static Optional<String> getNonDefaultJobManagerId(final NodeSettingsRO jobManagerSettings) {
        return Optional.ofNullable(jobManagerSettings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(), null));
    }

    /**
     * Adjusts the initial data of the dialog to include the job manager selection section.<br/>
     *
     * @param jfs the json forms settings to adjust
     * @param jobManagerId the job manager id to set in the data section of the dialog
     * @return the adjusted json forms settings with the job manager selection section added
     */
    public static JsonFormsSettings addJobManagerSelection(final JsonFormsSettings jfs, final String jobManagerId) {
        return new JsonFormsSettings() {

            @Override
            public ObjectNode getUiSchema() {
                final var uiSchema = jfs.getUiSchema();
                ((ArrayNode)uiSchema.get(UiSchema.TAG_ELEMENTS)).add(toUiSchemaElement(jobManagerSection));
                return uiSchema;
            }

            @Override
            public ObjectNode getSchema() {
                final var schema = jfs.getSchema();
                toSchemaConstructor(jobManagerSection).apply(schema);
                return schema;
            }

            @Override
            public JsonNode getData() {
                final var data = (ObjectNode)jfs.getData();
                data.putObject(JOB_MANAGER_KEY_FE)//
                    .put(JOB_MANAGER_FACTORY_ID_KEY_FE, jobManagerId);
                return data;

            }
        };

    }

    /**
     * Transforms the job manager represented as json to node settings.<br/>
     *
     * {@link SettingsApplier#handleJobManagerSettings} further handles the job manager settings (removing the whole
     * config if it is empty)
     *
     * @param dataJson the job manager settings as json
     * @return from json extracted job manager settings
     * @throws InvalidSettingsException if a non-default job manager is still selected
     */
    public static NodeSettings toNodeSettings(final JsonNode dataJson) throws InvalidSettingsException {
        final var emptySettings = new NodeSettings("extracted job manager settings");

        if (!dataJson.has(JOB_MANAGER_KEY_FE)) {
            return emptySettings;
        }

        final var jobManagerSettingsJson = dataJson.get(JOB_MANAGER_KEY_FE);
        if (!jobManagerSettingsJson.has(JOB_MANAGER_FACTORY_ID_KEY_FE)) {
            return emptySettings;
        }

        final var selectedJobManagerFactory = jobManagerSettingsJson.get(JOB_MANAGER_FACTORY_ID_KEY_FE).asText();
        if (selectedJobManagerFactory.equals(DEFAULT_JOB_MANAGER_FACTORY_ID)) {
            return emptySettings;
        }
        throw new InvalidSettingsException(
            "Custom job managers for nodes are not supported. Please select the default job manager.");
    }

}
