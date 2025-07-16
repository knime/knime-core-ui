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
 *   Jun 6, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.components;

import static org.knime.core.webui.node.dialog.defaultdialog.components.SubNodeContainerSettingsService.FACTORY;
import static org.knime.core.webui.node.dialog.defaultdialog.components.SubNodeContainerSettingsService.extractJsonFromWebDialogValueAndDialogRepresentation;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.WebDialogNodeRepresentation;
import org.knime.core.webui.node.dialog.defaultdialog.components.SubNodeContainerSettingsService.DialogSubNode;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DefaultDialogDataConverter;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Data converter for the {@link SubNodeContainerNodeDialog} used within the flow variable service.
 *
 * @author Paul Baernreuther
 */
final class SubNodeContainerDialogDataConverter implements DefaultDialogDataConverter {

    private final Supplier<List<DialogSubNode>> m_orderedDialogNodes;

    SubNodeContainerDialogDataConverter(final Supplier<List<DialogSubNode>> orderedDialogNodes) {
        m_orderedDialogNodes = orderedDialogNodes;
    }

    @Override
    public NodeSettings dataJsonToNodeSettings(final JsonNode dataJson, final SettingsType type)
        throws InvalidSettingsException {
        final var typeSettings = new NodeSettings(type.getConfigKey());
        final var jsonForType = getJsonNodeForType(dataJson, type);
        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            SubNodeContainerSettingsService.saveSettingsForSubNode(null, typeSettings, jsonForType, dialogSubNode);
        }
        return typeSettings;
    }

    private static JsonNode getJsonNodeForType(final JsonNode dataJson, final SettingsType type) {
        return dataJson.get(type.getConfigKey());
    }

    @Override
    public JsonNode nodeSettingsToDataJson(final SettingsType type, final NodeSettingsRO nodeSettings,
        final NodeParametersInput context) throws InvalidSettingsException {
        final var data = FACTORY.objectNode();
        final var jsonForType = data.putObject(type.getConfigKey());
        for (var dialogSubNode : m_orderedDialogNodes.get()) {
            final var dialogNode = dialogSubNode.dialogNode();
            final var value = dialogNode.getDefaultValue();
            final var paramName = dialogSubNode.paramName();
            if (nodeSettings.containsKey(paramName)) {
                value.loadFromNodeSettings(nodeSettings.getNodeSettings(paramName));
            }
            final var representation = dialogNode.getDialogRepresentation();
            if (!(representation instanceof WebDialogNodeRepresentation)) {
                continue;
            }
            final var jsonValue = extractJsonFromWebDialogValueAndDialogRepresentation(value, representation);
            jsonForType.set(paramName, jsonValue);
        }
        return data;
    }

}
