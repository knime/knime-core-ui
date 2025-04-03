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
 *   Jan 13, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.choices.column.multiple;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.PatternFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.choices.util.LegacyManualFilterPersistorUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.choices.util.LegacyPatternFilterPersistorUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.choices.util.PatternFilter;

/**
 * {@link NodeSettingsPersistor} for {@link ColumnFilter} that persists it in a way compatible to
 * {@link DataColumnSpecFilterConfiguration}.
 *
 * If only backwards compatible load is required but the settings should be saved as per default (i.e. also with the
 * default flow variables), use a {@link LegacyColumnFilterMigration} instead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class LegacyColumnFilterPersistor implements NodeSettingsPersistor<ColumnFilter> {

    private final String m_configKey;

    /**
     * @param configKey the root config key to save to and load from.
     */
    protected LegacyColumnFilterPersistor(final String configKey) {
        m_configKey = configKey;
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LegacyColumnFilterPersistor.class);

    /**
     * See NameFilterConfiguration.KEY_FILTER_TYPE
     */
    private static final String KEY_FILTER_TYPE = "filter-type";

    /**
     * See NameFilterConfiguration.TYPE.
     */
    private static final String KEY_FILTER_TYPE_MANUAL = "STANDARD";

    /**
     * See TypeFilterConfigurationImpl.TYPE
     */
    private static final String OLD_FILTER_TYPE_DATATYPE = "datatype";

    /**
     * See TypeFilterConfigurationImpl.CFG_TYPELIST
     */
    private static final String TYPELIST = "typelist";

    @SuppressWarnings("javadoc")
    public static ColumnFilter load(final NodeSettingsRO nodeSettings, final String configKey)
        throws InvalidSettingsException {
        var columnFilterSettings = nodeSettings.getNodeSettings(configKey);
        var columnFilter = new ColumnFilter();
        columnFilter.m_mode = loadMode(columnFilterSettings);
        columnFilter.m_manualFilter = LegacyManualFilterPersistorUtil.loadManualFilter(columnFilterSettings);
        if (columnFilterSettings.containsKey(PatternFilterConfiguration.TYPE)) {
            columnFilter.m_patternFilter = LegacyPatternFilterPersistorUtil
                .loadPatternMatching(columnFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE));
        } else {
            // in some very old workflows this field might not have been populated,
            // see knime://Testflows/Testflows%20(master)/knime-base/Flow%20Control/testGroupLoopStart
            columnFilter.m_patternFilter = new PatternFilter();
        }

        if (columnFilterSettings.containsKey(OLD_FILTER_TYPE_DATATYPE)) {
            columnFilter.m_typeFilter = loadTypeFilter(columnFilterSettings.getNodeSettings(OLD_FILTER_TYPE_DATATYPE));
        } else {
            // in some very old workflows this field might not have been populated,
            // see knime://Testflows/Testflows%20(master)/knime-base/Flow%20Control/testGroupLoopStart
            columnFilter.m_typeFilter = new TypeFilter();
        }

        return columnFilter;
    }

    private static ColumnFilterMode loadMode(final NodeSettingsRO columnFilterSettings)
        throws InvalidSettingsException {
        var filterType = columnFilterSettings.getString(KEY_FILTER_TYPE);
        if (KEY_FILTER_TYPE_MANUAL.equals(filterType)) {
            return ColumnFilterMode.MANUAL;
        } else if (PatternFilterConfiguration.TYPE.equals(filterType)) {
            var patternMatchingSettings = columnFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
            return ColumnFilterMode
                .toColumnFilterMode(LegacyPatternFilterPersistorUtil.loadPatternMode(patternMatchingSettings));
        } else if (OLD_FILTER_TYPE_DATATYPE.equals(filterType)) {
            return ColumnFilterMode.TYPE;
        } else {
            throw new InvalidSettingsException("Unsupported column filter type: " + filterType);
        }
    }

    private static TypeFilter loadTypeFilter(final NodeSettingsRO typeFilterSettings) throws InvalidSettingsException {
        var typeFilter = new TypeFilter();
        typeFilter.m_selectedTypes = loadSelectedTypes(typeFilterSettings);
        typeFilter.m_typeDisplays = getDisplays(typeFilter.m_selectedTypes);
        return typeFilter;
    }

    private static ColumnTypeDisplay[] getDisplays(final String[] selectedTypes) {
        return Stream.of(selectedTypes)//
            .map(ColumnTypeDisplay::fromPreferredValueClass)//
            .flatMap(Optional::stream)//
            .toArray(ColumnTypeDisplay[]::new);
    }

    private static String[] loadSelectedTypes(final NodeSettingsRO typeFilterSettings) throws InvalidSettingsException {
        var typeListSettings = typeFilterSettings.getNodeSettings(TYPELIST);
        var keys = typeListSettings.keySet();
        var selectedTypes = new ArrayList<String>(keys.size());
        for (var key : keys) {
            if (typeListSettings.getBoolean(key)) {
                selectedTypes.add(key);
            }
        }
        return selectedTypes.toArray(String[]::new);
    }

    @SuppressWarnings("javadoc")
    public static void save(ColumnFilter columnFilter, final NodeSettingsWO settings, final String configKey) {
        if (columnFilter == null) {
            LOGGER.coding(createFilterNullError(configKey));
            columnFilter = new ColumnFilter();
        }
        var columnFilterSettings = settings.addNodeSettings(configKey);
        columnFilterSettings.addString(KEY_FILTER_TYPE, toFilterType(columnFilter.m_mode));
        LegacyManualFilterPersistorUtil.saveManualFilter(columnFilter.m_manualFilter, columnFilterSettings);
        LegacyPatternFilterPersistorUtil.savePatternMatching(columnFilter.m_patternFilter,
            columnFilter.m_mode.toPatternMode(), columnFilterSettings.addNodeSettings(PatternFilterConfiguration.TYPE));
        saveTypeFilter(columnFilter.m_typeFilter, columnFilterSettings.addNodeSettings(OLD_FILTER_TYPE_DATATYPE));
    }

    private static String createFilterNullError(final String configKey) {
        var nodeContext = NodeContext.getContext();
        String prefix;
        if (nodeContext != null) {
            prefix = String.format("The ColumnFilter with key '%s' of the node '%s' is null.", configKey,
                nodeContext.getNodeContainer().getNameWithID());
        } else {
            prefix = String.format("The ColumnFilter with key '%s' is null. ", configKey);
        }
        return prefix
            + " It is replaced by a new ColumnFilter instance to prevent errors but please fix this issue anyway.";
    }

    private static String toFilterType(final ColumnFilterMode mode) {
        return switch (mode) {
            case MANUAL -> KEY_FILTER_TYPE_MANUAL;
            case REGEX, WILDCARD -> PatternFilterConfiguration.TYPE;
            case TYPE -> OLD_FILTER_TYPE_DATATYPE;
            default -> throw new IllegalArgumentException("Unsupported ColumnSelectionMode: " + mode);
        };
    }

    private static void saveTypeFilter(final TypeFilter typeFilter, final NodeSettingsWO typeFilterSettings) {
        var typeListSettings = typeFilterSettings.addNodeSettings(TYPELIST);
        if (typeFilter.m_selectedTypes != null) {
            Stream.of(typeFilter.m_selectedTypes).forEach(t -> typeListSettings.addBoolean(t, true));
        }
    }

    @Override
    public ColumnFilter load(final NodeSettingsRO settings) throws InvalidSettingsException {
        return load(settings, m_configKey);
    }

    @Override
    public void save(final ColumnFilter obj, final NodeSettingsWO settings) {
        save(obj, settings, m_configKey);
    }

    @Override
    public String[][] getConfigPaths() {
        return getConfigPaths(m_configKey);
    }

    static String[][] getConfigPaths(final String configKey) {
        return new String[][]{//
            {configKey, KEY_FILTER_TYPE}, //
            {configKey, LegacyManualFilterPersistorUtil.KEY_INCLUDED_NAMES}, //
            {configKey, LegacyManualFilterPersistorUtil.OLD_EXCLUDED_NAMES}, //
            {configKey, LegacyManualFilterPersistorUtil.KEY_ENFORCE_OPTION}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_PATTERN}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_TYPE}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_CASESENSITIVE}, //
            {configKey, PatternFilterConfiguration.TYPE,
                LegacyPatternFilterPersistorUtil.PATTERN_FILTER_EXCLUDEMATCHING} //
        };
    }
}
