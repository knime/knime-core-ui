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
 */
package org.knime.core.webui.node.dialog.impl;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.persistence.NodeSettingsPersistorWithConfigKey;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Similar to {@link JsonBasedNodeSettingsPersistor} this field persistor uses JSON to persist.
 *
 * @author Paul Bärnreuther
 */
public class JsonBasedNodeSettingsFieldPersistor<S extends DefaultNodeSettings>
    extends NodeSettingsPersistorWithConfigKey<S> {

    private final Class<S> m_settingsClass;

    /**
     * Constructor.
     *
     * @param settingsClass the settings class to persist
     */
    public JsonBasedNodeSettingsFieldPersistor(final Class<S> settingsClass) {
        m_settingsClass = settingsClass;
    }

    @Override
    public S load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        if (nodeSettings.isLeaf() && m_settingsClass.getDeclaredFields().length > 0) {
            // unfortunately Jackson does not allow to fail if some field of the deserialized type is not provided
            // by the JSON
            throw new InvalidSettingsException("No settings available. Most likely an implementation error.");
        }
        final var fieldSettings = nodeSettings.getNodeSettings(getConfigKey());
        final var node = JsonFormsDataUtil.getMapper().createObjectNode();
        JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(fieldSettings, node);
        return JsonFormsDataUtil.toDefaultNodeSettings(node, m_settingsClass);
    }

    @Override
    public void save(final S obj, final NodeSettingsWO settings) {
        var objectNode = (ObjectNode)JsonFormsDataUtil.toJsonData(obj);
        var schemaNode = JsonFormsSchemaUtil.buildSchema(m_settingsClass);
        final var fieldSettings = settings.addNodeSettings(getConfigKey());
        JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings(objectNode, schemaNode, fieldSettings);
    }
}
