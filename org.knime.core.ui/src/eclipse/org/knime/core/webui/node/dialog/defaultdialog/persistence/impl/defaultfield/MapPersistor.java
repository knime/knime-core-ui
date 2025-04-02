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
 *   Jun 18, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigStringEntry;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.DefaultFieldPersistor;

/**
 * A persistor that supports storing and loading a map of dynamic settings. Currently this map only supports the
 * following value types: String, Boolean, Integer, Double.
 *
 * @author Paul Bärnreuther
 */
final class MapPersistor implements DefaultFieldPersistor<Map<String, Object>> {

    private final String m_configKey;

    MapPersistor(final String configKey) {
        m_configKey = configKey;
    }

    @Override
    public Map<String, Object> load(final NodeSettingsRO settings) throws InvalidSettingsException {

        final var dynamicSettings = settings.getNodeSettings(m_configKey);
        final var map = new HashMap<String, Object>();
        for (var i = 0; i < dynamicSettings.getChildCount(); i++) {
            if (dynamicSettings.getChildAt(i) instanceof AbstractConfigEntry ace) {
                map.put(ace.getKey(), abstractConfigEntryToValue(ace));
            }
        }
        return map;
    }

    @Override
    public void save(final Map<String, Object> obj, final NodeSettingsWO settings) {
        final var dynamicSettings = settings.addNodeSettings(m_configKey);
        for (var entry : obj.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            saveEntry(dynamicSettings, key, value);
        }
    }

    /**
     * Currently supported: String, Boolean, Integer, Double
     *
     * @throws InvalidSettingsException if the type of the given {@link AbstractConfigEntry} is not supported
     */
    private static Object abstractConfigEntryToValue(final AbstractConfigEntry ace) throws InvalidSettingsException {
        if (ace instanceof ConfigStringEntry se) {
            return se.getString();
        } else if (ace instanceof org.knime.core.node.config.base.ConfigBooleanEntry be) {
            return be.getBoolean();
        } else if (ace instanceof org.knime.core.node.config.base.ConfigIntEntry ie) {
            return ie.getInt();
        } else if (ace instanceof org.knime.core.node.config.base.ConfigDoubleEntry de) {
            return de.getDouble();
        }
        throw new InvalidSettingsException("Unsupported type of AbstractConfigEntry: " + ace.getClass().getName()
            + ". Supported types are: " + "String, Boolean, Integer, Double.");
    }

    /**
     * Currently supported: String, Boolean, Integer, Double
     */
    private static void saveEntry(final NodeSettingsWO dynamicSettings, final String key, final Object value) {
        if (value instanceof String str) {
            dynamicSettings.addString(key, str);
        } else if (value instanceof Boolean bool) {
            dynamicSettings.addBoolean(key, bool);
        } else if (value instanceof Integer integer) {
            dynamicSettings.addInt(key, integer);
        } else if (value instanceof Double number) {
            dynamicSettings.addDouble(key, number);
        } else {
            throw new IllegalArgumentException("Unsupported type for key " + key + ": " + value.getClass());
        }
    }

}
