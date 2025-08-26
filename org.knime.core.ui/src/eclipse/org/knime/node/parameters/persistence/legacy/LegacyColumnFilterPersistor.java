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
package org.knime.node.parameters.persistence.legacy;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.util.filter.PatternFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.LegacyManualFilterPersistorUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.LegacyPatternFilterPersistorUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil.ColumnFilterBuilder;
import org.knime.node.parameters.widget.choices.filter.TypedStringFilter;

/**
 * {@link NodeParametersPersistor} for {@link TypedStringFilter} that persists it in a way compatible to
 * {@link DataColumnSpecFilterConfiguration} as it is used within the {@link SettingsModelColumnFilter2}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class LegacyColumnFilterPersistor implements NodeParametersPersistor<ColumnFilter> {

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
        var builder = new ColumnFilterBuilder();

        builder.withMode(loadMode(columnFilterSettings));

        builder.withManuallySelected(LegacyManualFilterPersistorUtil.loadManuallySelected(columnFilterSettings))
            .withManuallyDeselected(LegacyManualFilterPersistorUtil.loadManuallyDeselected(columnFilterSettings))
            .withIncludeUnknownColumns(LegacyManualFilterPersistorUtil.loadIncludeUnknownColumns(columnFilterSettings));

        if (columnFilterSettings.containsKey(PatternFilterConfiguration.TYPE)) {
            var patternSettings = columnFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
            builder.withPattern(LegacyPatternFilterPersistorUtil.loadPattern(patternSettings))
                .withPatternCaseSensitive(LegacyPatternFilterPersistorUtil.loadPatternCaseSensitive(patternSettings))
                .withPatternInverted(LegacyPatternFilterPersistorUtil.loadPatternInverted(patternSettings));
        } else {
            // in some very old workflows this field might not have been populated,
            // see knime://Testflows/Testflows%20(master)/knime-base/Flow%20Control/testGroupLoopStart
            builder.withPattern("").withPatternCaseSensitive(false).withPatternInverted(false);
        }

        if (columnFilterSettings.containsKey(OLD_FILTER_TYPE_DATATYPE)) {
            var selectedTypes = loadTypeFilter(columnFilterSettings.getNodeSettings(OLD_FILTER_TYPE_DATATYPE));
            builder.withSelectedTypes(selectedTypes);
        } else {
            // in some very old workflows this field might not have been populated,
            // see knime://Testflows/Testflows%20(master)/knime-base/Flow%20Control/testGroupLoopStart
            builder.withSelectedTypes(new String[0]);
        }

        return builder.build();
    }

    private static ColumnFilterBuilder.Mode loadMode(final NodeSettingsRO columnFilterSettings)
        throws InvalidSettingsException {
        var filterType = columnFilterSettings.getString(KEY_FILTER_TYPE);
        if (KEY_FILTER_TYPE_MANUAL.equals(filterType)) {
            return ColumnFilterBuilder.Mode.MANUAL;
        } else if (PatternFilterConfiguration.TYPE.equals(filterType)) {
            var patternMatchingSettings = columnFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
            return LegacyPatternFilterPersistorUtil.loadPatternMode(patternMatchingSettings).toColumnFilterMode();
        } else if (OLD_FILTER_TYPE_DATATYPE.equals(filterType)) {
            return ColumnFilterBuilder.Mode.TYPE;
        } else {
            throw new InvalidSettingsException("Unsupported column filter type: " + filterType);
        }
    }

    private static String[] loadTypeFilter(final NodeSettingsRO typeFilterSettings) throws InvalidSettingsException {
        return loadSelectedTypes(typeFilterSettings);
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

        LegacyFilterUtil.saveColumnFilter(columnFilter, mode -> saveMode(mode, columnFilterSettings),
            manuallySelected -> saveManuallySelected(manuallySelected, columnFilterSettings),
            manuallyDeselected -> saveManuallyDeselected(manuallyDeselected, columnFilterSettings),
            includeUnknownColumns -> saveIncludeUnknownColumns(includeUnknownColumns, columnFilterSettings),
            (pattern, caseSensitive, inverted, mode) -> savePatternData(pattern, caseSensitive, inverted, mode,
                columnFilterSettings),
            selectedTypes -> saveSelectedTypes(selectedTypes, columnFilterSettings));
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

    private static void saveMode(final ColumnFilterBuilder.Mode mode, final NodeSettingsWO columnFilterSettings) {
        columnFilterSettings.addString(KEY_FILTER_TYPE, toFilterType(mode.toTypedStringFilterMode()));
    }

    private static void saveManuallySelected(final String[] manuallySelected,
        final NodeSettingsWO columnFilterSettings) {
        LegacyManualFilterPersistorUtil.saveManuallySelected(manuallySelected, columnFilterSettings);
    }

    private static void saveManuallyDeselected(final String[] manuallyDeselected,
        final NodeSettingsWO columnFilterSettings) {
        LegacyManualFilterPersistorUtil.saveManuallyDeselected(manuallyDeselected, columnFilterSettings);
    }

    private static void saveIncludeUnknownColumns(final boolean includeUnknownColumns,
        final NodeSettingsWO columnFilterSettings) {
        LegacyManualFilterPersistorUtil.saveIncludeUnknownColumns(includeUnknownColumns, columnFilterSettings);
    }

    private static void savePatternData(final String pattern, final boolean caseSensitive, final boolean inverted,
        final LegacyFilterUtil.PatternMode mode, final NodeSettingsWO columnFilterSettings) {
        var patternFilterSettings = columnFilterSettings.addNodeSettings(PatternFilterConfiguration.TYPE);
        LegacyPatternFilterPersistorUtil.savePattern(pattern, patternFilterSettings);
        LegacyPatternFilterPersistorUtil.savePatternCaseSensitive(caseSensitive, patternFilterSettings);
        LegacyPatternFilterPersistorUtil.savePatternInverted(inverted, patternFilterSettings);
        // Save the pattern mode based on the filter mode
        var patternMode =
            (mode == LegacyFilterUtil.PatternMode.REGEX) ? LegacyPatternFilterPersistorUtil.PatternMode.REGEX
                : LegacyPatternFilterPersistorUtil.PatternMode.WILDCARD;
        LegacyPatternFilterPersistorUtil.savePatternMode(patternMode, patternFilterSettings);
    }

    private static void saveSelectedTypes(final String[] selectedTypes, final NodeSettingsWO columnFilterSettings) {
        saveTypeFilter(selectedTypes, columnFilterSettings.addNodeSettings(OLD_FILTER_TYPE_DATATYPE));
    }

    private static String toFilterType(final TypedStringFilterMode mode) {
        return switch (mode) {
            case MANUAL -> KEY_FILTER_TYPE_MANUAL;
            case REGEX, WILDCARD -> PatternFilterConfiguration.TYPE;
            case TYPE -> OLD_FILTER_TYPE_DATATYPE;
            default -> throw new IllegalArgumentException("Unsupported ColumnSelectionMode: " + mode);
        };
    }

    private static void saveTypeFilter(final String[] selectedTypes, final NodeSettingsWO typeFilterSettings) {
        var typeListSettings = typeFilterSettings.addNodeSettings(TYPELIST);
        if (selectedTypes != null) {
            Stream.of(selectedTypes).forEach(t -> typeListSettings.addBoolean(t, true));
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

    @SuppressWarnings("javadoc")
    public static String[][] getConfigPaths(final String configKey) {
        return new String[][]{//
            {configKey, KEY_FILTER_TYPE}, //
            {configKey, LegacyManualFilterPersistorUtil.KEY_INCLUDED_NAMES}, //
            {configKey, LegacyManualFilterPersistorUtil.OLD_EXCLUDED_NAMES}, //
            {configKey, LegacyManualFilterPersistorUtil.KEY_ENFORCE_OPTION}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_PATTERN}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_TYPE}, //
            {configKey, PatternFilterConfiguration.TYPE, LegacyPatternFilterPersistorUtil.PATTERN_FILTER_CASESENSITIVE},
            {configKey, PatternFilterConfiguration.TYPE,
                LegacyPatternFilterPersistorUtil.PATTERN_FILTER_EXCLUDEMATCHING} //
        };
    }
}
