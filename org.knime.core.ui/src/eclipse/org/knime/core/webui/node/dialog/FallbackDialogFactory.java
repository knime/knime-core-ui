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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.ConfigEntries;
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
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                    return FallbackDialogUtils.toNodeSettings(schema, dataJson.get("model"));
                }

                @Override
                public JsonNode nodeSettingsToDataJson(final SettingsType type, final NodeSettingsRO nodeSettings,
                    final NodeParametersInput context) throws InvalidSettingsException {
                    return FallbackDialogUtils.toJson(nodeSettings);
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
            final var pair = FallbackDialogUtils.toJsonFormsSettingsForModelSettings(settings.get(MODEL));
            final var renderers = pair.getFirst();
            final var jsonFormsSettings = pair.getSecond();
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
            var extractedModelSettings = FallbackDialogUtils.toNodeSettings(schema, modelJson);

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

}
