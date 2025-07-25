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
 *   Jan 20, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_DATA;
import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.TextToJsonUtil.textToJson;
import static org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil.rootJsonToVariableSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.map;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersNativeNodeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.JsonDataToDefaultNodeSettingsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.ToNodeSettingsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;
import org.knime.core.webui.node.dialog.internal.SettingsApplier.TextToNodeSettingsConverter;
import org.knime.node.parameters.NodeParameters;

/**
 * Default implementation for applying the JSON representation of settings to {@link NodeAndVariableSettingsWO}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultTextToNodeSettingsConverter implements TextToNodeSettingsConverter {

    private final Map<SettingsType, Class<? extends NodeParameters>> m_settingsClasses;

    DefaultTextToNodeSettingsConverter(final Map<SettingsType, Class<? extends NodeParameters>> settingsClasses) {
        m_settingsClasses = settingsClasses;
    }

    @Override
    public void toNodeSettings(final String textSettings,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {
        final var root = textToJson(textSettings);
        final var defaultNodeSettings =
            JsonDataToDefaultNodeSettingsUtil.toDefaultNodeSettings(m_settingsClasses, root.get(FIELD_NAME_DATA));
        final var extractedNodeSettings = ToNodeSettingsUtil.toNodeSettings(defaultNodeSettings);
        JobManagerParametersNativeNodeUtil.toNodeSettings(extractedNodeSettings);
        final var extractedVariableSettings = VariableSettingsUtil.extractVariableSettings(settings.keySet(), root);

        alignSettingsWithFlowVariables(//
            extractedNodeSettings,
            previousSettings.entrySet().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue)),
            extractedVariableSettings, defaultNodeSettings);
        copyLeftToRight(extractedNodeSettings, settings);
        rootJsonToVariableSettings(root, map(settings));

    }

    private void alignSettingsWithFlowVariables( //
        final Map<SettingsType, NodeSettings> settings, //
        final Map<SettingsType, NodeSettingsRO> previousSettings, //
        final Map<SettingsType, VariableSettingsRO> extractedVariableSettings, //
        final Map<SettingsType, NodeParameters> defaultNodeSettingsMap //
    ) {
        for (var key : settings.keySet()) { // NOSONAR
            final var configMappings =
                NodeParametersUtil.getConfigMappings(m_settingsClasses.get(key), defaultNodeSettingsMap.get(key));
            NodeSettingsCorrectionUtil.correctNodeSettingsRespectingFlowVariables(configMappings, settings.get(key),
                previousSettings.get(key), extractedVariableSettings.get(key));
        }
    }

    private static void copyLeftToRight(final Map<SettingsType, NodeSettings> extractedNodeSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        extractedNodeSettings.entrySet().forEach(entry -> entry.getValue().copyTo(settings.get(entry.getKey())));
    }

}
