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
 *   Nov 11, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.array;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultFieldNodeSettingsPersistorFactory.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.util.DynamicArrayUtil;

/**
 * Default persistor for {@link Array}.
 *
 * @param <S> The elements type
 * @author Paul Bärnreuther
 */
public final class DynamicArrayPersistor<S extends DefaultNodeSettings> implements NodeSettingsPersistor<Array<S>> {
    private final ArrayPersistor<S> m_arrayFieldPersistor;

    private final Tree<PersistableSettings> m_elementTree;

    /**
     * @param elementTree
     */
    public DynamicArrayPersistor(final Tree<PersistableSettings> elementTree) {
        m_arrayFieldPersistor = new ArrayPersistor<>(elementTree);
        m_elementTree = elementTree;
    }

    @Override
    public Array<S> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        var staticArray = m_arrayFieldPersistor.load(settings);
        final var columnsMap = loadColumnsMapFromRootSettings(settings);
        return new Array<>(Arrays.asList(staticArray), columnsMap);
    }

    @Override
    public void save(final Array<S> array, final NodeSettingsWO settings) {
        m_arrayFieldPersistor.save(toArray(array.m_values), settings);
        saveColumnsMap(array.m_columns, settings.addNodeSettings("columns"));
    }

    private Map<String, String> loadColumnsMapFromRootSettings(final NodeSettingsRO nodeSettings)
        throws InvalidSettingsException {
        // We allow the "columns" config to be missing to be backwards compatible with respect to static arrays.
        if (nodeSettings.containsKey("columns")) {
            return loadColumnsMap(nodeSettings);
        } else {
            return Map.of();
        }
    }

    private Map<String, String> loadColumnsMap(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        final var columns = nodeSettings.getNodeSettings("columns");
        final Map<String, String> columnsMap = new HashMap<>();
        for (var key : m_elementTree.getWidgetNodes().map(DynamicArrayUtil::getColumnKey).filter(columns::containsKey)
            .toList()) {
            columnsMap.put(key, columns.getString(key));
        }
        return columnsMap;
    }

    private static void saveColumnsMap(final Map<String, String> columnsMap, final NodeSettingsWO columns) {
        columnsMap.forEach(columns::addString);
    }

    @Override
    public ConfigMappings getConfigMappings(final Array<S> array) {
        return m_arrayFieldPersistor.getConfigMappings(array.m_values);
    }

    private S[] toArray(final List<S> list) {
        @SuppressWarnings("unchecked")
        final var array = (S[])java.lang.reflect.Array.newInstance(m_elementTree.getType(), list.size());
        return list.toArray(array);

    }

}
