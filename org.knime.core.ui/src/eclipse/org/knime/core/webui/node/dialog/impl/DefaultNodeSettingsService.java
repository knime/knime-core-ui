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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog.impl;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link NodeSettingsService} that translates {@link DefaultNodeSettings}-implementations into
 * {@link NodeSettings}-objects (on data apply) and vice-versa (initial data).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class DefaultNodeSettingsService implements NodeSettingsService {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultNodeSettingsService.class);

    static final String FIELD_NAME_DATA = "data";

    static final String FIELD_NAME_SCHEMA = "schema";

    static final String FIELD_NAME_UI_SCHEMA = "ui_schema";

    private final Map<SettingsType, Class<? extends DefaultNodeSettings>> m_settingsClasses;

    /**
     * @param settingsClasses map that associates a {@link DefaultNodeSettings} class-with a {@link SettingsType}
     */
    public DefaultNodeSettingsService(final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses) {
        m_settingsClasses = settingsClasses;
    }

    @Override
    public void toNodeSettings(final String textSettings, final Map<SettingsType, NodeSettingsWO> settings) {
        try {
            final var root = (ObjectNode)JsonFormsDataUtil.getMapper().readTree(textSettings);
            textSettingsToNodeSettings(root, SettingsType.MODEL, settings);
            textSettingsToNodeSettings(root, SettingsType.VIEW, settings);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                String.format("Exception when writing data to node settings: %s", e.getMessage()), e);
        }
    }

    private void textSettingsToNodeSettings(final ObjectNode rootNode, final SettingsType settingsType,
        final Map<SettingsType, NodeSettingsWO> settings) {
        if (settings.containsKey(settingsType)) {
            final var node = rootNode.get(settingsType.getConfigKey());
            var settingsClass = m_settingsClasses.get(settingsType);
            var settingsObj = JsonFormsDataUtil.toDefaultNodeSettings(node, settingsClass);
            DefaultNodeSettings.saveSettings(settingsClass, settingsObj, settings.get(settingsType));
        }
    }

    @Override
    public String fromNodeSettings(final Map<SettingsType, NodeSettingsRO> settings,
        final PortObjectSpec[] specs) {
        var creationContext = DefaultNodeSettings.createSettingsCreationContext(specs);
        var loadedSettings = settings.entrySet().stream()//
            .collect(toMap(Map.Entry::getKey, e -> loadSettings(settings, e.getKey(), specs)));
        final var jsonFormsSettings = new JsonFormsSettingsImpl(loadedSettings, creationContext);
        final var root = JsonFormsDataUtil.getMapper().createObjectNode();
        root.set(FIELD_NAME_DATA, jsonFormsSettings.getData());
        root.set(FIELD_NAME_SCHEMA, jsonFormsSettings.getSchema());
        root.putRawValue(FIELD_NAME_UI_SCHEMA, jsonFormsSettings.getUiSchema());
        return root.toString();
    }

    private DefaultNodeSettings loadSettings(final Map<SettingsType, NodeSettingsRO> nodeSettings,
        final SettingsType settingsType, final PortObjectSpec[] specs) {
        var settingsClass = m_settingsClasses.get(settingsType);
        try {
            return DefaultNodeSettings.loadSettings(nodeSettings.get(settingsType),
                settingsClass);
        } catch (InvalidSettingsException ex) {
            LOGGER.error(String.format("Failed to load settings ('%s'). New settings are created for the dialog.",
                ex.getMessage()), ex);
            return DefaultNodeSettings.createSettings(settingsClass, specs);
        }
    }


    @Override
    public void getDefaultNodeSettings(final Map<SettingsType, NodeSettingsWO> settings, final PortObjectSpec[] specs) {
        saveDefaultSettings(settings, SettingsType.MODEL, specs);
        saveDefaultSettings(settings, SettingsType.VIEW, specs);
    }

    private void saveDefaultSettings(final Map<SettingsType, NodeSettingsWO> settingsMap,
        final SettingsType settingsType, final PortObjectSpec[] specs) {
        var nodeSettings = settingsMap.get(settingsType);
        if (nodeSettings != null) {
            saveDefaultSettings(m_settingsClasses.get(settingsType), nodeSettings, specs);
        }
    }

    private static <S extends DefaultNodeSettings> void saveDefaultSettings(final Class<S> settingsClass,
        final NodeSettingsWO nodeSettings, final PortObjectSpec[] specs) {
        var settingsObj = DefaultNodeSettings.createSettings(settingsClass, specs);
        DefaultNodeSettings.saveSettings(settingsClass, settingsObj, nodeSettings);
    }

}
