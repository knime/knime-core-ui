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
 *   Sep 12, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil.mapValues;

import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.SettingsConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link NodeSettingsService} decorator that can be used to enable saving and loading flow variables of
 * {@link NodeDialog} using the {@link DefaultNodeDialog#getPage()} page.
 *
 * @author Paul Bärnreuther
 */
public final class DefaultNodeSettingsServiceWithVariables implements NodeSettingsService {

    private final NodeSettingsService m_delegate;

    /**
     * Constructor
     *
     * @param delegate that will be used after saving and before loading the flow variable settings.
     */
    public DefaultNodeSettingsServiceWithVariables(final NodeSettingsService delegate) {
        m_delegate = delegate;
    }

    @Override
    public void toNodeSettings(final String textSettings, final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        SettingsConverter.textSettingsToVariableSettings(textSettings, mapValues(settings, v -> v));
        m_delegate.toNodeSettings(textSettings, settings);
    }

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final var delegateResult = m_delegate.fromNodeSettings(settings, specs);
        try {
            final var mapper = JsonFormsDataUtil.getMapper();
            final var parsed = mapper.readTree(delegateResult);
            final var variablesObjectNode = getVariablesObjectNode(settings, specs);
            ((ObjectNode)parsed).setAll(variablesObjectNode);
            return mapper.writeValueAsString(parsed);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static ObjectNode getVariablesObjectNode(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        final Map<SettingsType, VariableSettingsRO> variableSettings = settings.entrySet().stream()//
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final var context = DefaultNodeSettings.createDefaultNodeSettingsContext(specs);
        final var variablesObjectNode = SettingsConverter.variableSettingsToJsonObject(variableSettings, context);
        return variablesObjectNode;
    }

    @Override
    public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings, final PortObjectSpec[] specs) {
        m_delegate.getDefaultNodeSettings(settings, specs);
    }
}