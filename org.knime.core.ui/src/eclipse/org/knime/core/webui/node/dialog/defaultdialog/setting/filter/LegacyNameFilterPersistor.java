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
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.PatternFilterConfiguration;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.LegacyManualFilterPersistorUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.LegacyPatternFilterPersistorUtil;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil.StringFilterBuilder;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * {@link NodeParametersPersistor} for {@link StringFilter} that persists it in a way compatible to
 * {@link NameFilterConfiguration}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class LegacyNameFilterPersistor implements NodeParametersPersistor<StringFilter> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LegacyNameFilterPersistor.class);

    /**
     * See NameFilterConfiguration.KEY_FILTER_TYPE
     */
    private static final String KEY_FILTER_TYPE = "filter-type";

    /**
     * See NameFilterConfiguration.TYPE.
     */
    private static final String KEY_FILTER_TYPE_MANUAL = "STANDARD";

    private final String m_configKey;

    /**
     * @param configKey the root config key to save to and load from. Do not extend this class but use it directly if
     *            the field name of the {@link StringFilter} field should be used as root config key.
     */
    protected LegacyNameFilterPersistor(final String configKey) {
        m_configKey = configKey;
    }

    @SuppressWarnings("javadoc")
    public static StringFilter load(final NodeSettingsRO nodeSettings, final String configKey)
        throws InvalidSettingsException {
        var nameFilterSettings = nodeSettings.getNodeSettings(configKey);
        var builder = new StringFilterBuilder();

        builder.withMode(loadMode(nameFilterSettings));

        builder.withManuallySelected(LegacyManualFilterPersistorUtil.loadManuallySelected(nameFilterSettings))
            .withManuallyDeselected(LegacyManualFilterPersistorUtil.loadManuallyDeselected(nameFilterSettings))
            .withIncludeUnknownColumns(LegacyManualFilterPersistorUtil.loadIncludeUnknownColumns(nameFilterSettings));

        var patternSettings = nameFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
        builder.withPattern(LegacyPatternFilterPersistorUtil.loadPattern(patternSettings))
            .withPatternCaseSensitive(LegacyPatternFilterPersistorUtil.loadPatternCaseSensitive(patternSettings))
            .withPatternInverted(LegacyPatternFilterPersistorUtil.loadPatternInverted(patternSettings));

        return builder.build();
    }

    private static StringFilterBuilder.Mode loadMode(final NodeSettingsRO nameFilterSettings)
        throws InvalidSettingsException {
        var filterType = nameFilterSettings.getString(KEY_FILTER_TYPE);
        if (KEY_FILTER_TYPE_MANUAL.equals(filterType)) {
            return StringFilterBuilder.Mode.MANUAL;
        } else if (PatternFilterConfiguration.TYPE.equals(filterType)) {
            var patternMatchingSettings = nameFilterSettings.getNodeSettings(PatternFilterConfiguration.TYPE);
            return LegacyPatternFilterPersistorUtil.loadPatternMode(patternMatchingSettings).toStringFilterMode();
        } else {
            throw new InvalidSettingsException("Unsupported name filter type: " + filterType);
        }
    }

    @SuppressWarnings("javadoc")
    public static void save(StringFilter nameFilter, final NodeSettingsWO settings, final String configKey) {
        if (nameFilter == null) {
            LOGGER.coding(createFilterNullError(configKey));
            nameFilter = new StringFilter();
        }
        var nameFilterSettings = settings.addNodeSettings(configKey);

        LegacyFilterUtil.saveStringFilter(nameFilter, mode -> saveMode(mode, nameFilterSettings),
            manuallySelected -> saveManuallySelected(manuallySelected, nameFilterSettings),
            manuallyDeselected -> saveManuallyDeselected(manuallyDeselected, nameFilterSettings),
            includeUnknownColumns -> saveIncludeUnknownColumns(includeUnknownColumns, nameFilterSettings),
            (pattern, caseSensitive, inverted, mode) -> savePatternData(pattern, caseSensitive, inverted, mode,
                nameFilterSettings));
    }

    private static String createFilterNullError(final String configKey) {
        var nodeContext = NodeContext.getContext();
        String prefix;
        if (nodeContext != null) {
            prefix = String.format("The NameFilter with key '%s' of the node '%s' is null.", configKey,
                nodeContext.getNodeContainer().getNameWithID());
        } else {
            prefix = String.format("The NameFilter with key '%s' is null. ", configKey);
        }
        return prefix
            + " It is replaced by a new NameFilter instance to prevent errors but please fix this issue anyway.";
    }

    private static void saveMode(final StringFilterBuilder.Mode mode, final NodeSettingsWO nameFilterSettings) {
        nameFilterSettings.addString(KEY_FILTER_TYPE, toFilterType(mode.toStringFilterMode()));
    }

    private static void saveManuallySelected(final String[] manuallySelected, final NodeSettingsWO nameFilterSettings) {
        LegacyManualFilterPersistorUtil.saveManuallySelected(manuallySelected, nameFilterSettings);
    }

    private static void saveManuallyDeselected(final String[] manuallyDeselected,
        final NodeSettingsWO nameFilterSettings) {
        LegacyManualFilterPersistorUtil.saveManuallyDeselected(manuallyDeselected, nameFilterSettings);
    }

    private static void saveIncludeUnknownColumns(final boolean includeUnknownColumns,
        final NodeSettingsWO nameFilterSettings) {
        LegacyManualFilterPersistorUtil.saveIncludeUnknownColumns(includeUnknownColumns, nameFilterSettings);
    }

    private static void savePatternData(final String pattern, final boolean caseSensitive, final boolean inverted,
        final LegacyFilterUtil.PatternMode mode, final NodeSettingsWO nameFilterSettings) {
        var patternFilterSettings = nameFilterSettings.addNodeSettings(PatternFilterConfiguration.TYPE);
        LegacyPatternFilterPersistorUtil.savePattern(pattern, patternFilterSettings);
        // Save the pattern mode based on the filter mode
        var patternMode =
            (mode == LegacyFilterUtil.PatternMode.REGEX) ? LegacyPatternFilterPersistorUtil.PatternMode.REGEX
                : LegacyPatternFilterPersistorUtil.PatternMode.WILDCARD;
        LegacyPatternFilterPersistorUtil.savePatternMode(patternMode, patternFilterSettings);
        LegacyPatternFilterPersistorUtil.savePatternCaseSensitive(caseSensitive, patternFilterSettings);
        LegacyPatternFilterPersistorUtil.savePatternInverted(inverted, patternFilterSettings);
    }

    private static String toFilterType(final StringFilterMode mode) {
        switch (mode) {
            case MANUAL:
                return KEY_FILTER_TYPE_MANUAL;
            case REGEX:
            case WILDCARD:
                return PatternFilterConfiguration.TYPE;
            default:
                throw new IllegalArgumentException("Unsupported NameSelectionMode: " + mode);
        }
    }

    @Override
    public StringFilter load(final NodeSettingsRO settings) throws InvalidSettingsException {
        return load(settings, m_configKey);
    }

    @Override
    public void save(final StringFilter obj, final NodeSettingsWO settings) {
        save(obj, settings, m_configKey);
    }

    @Override
    public String[][] getConfigPaths() {
        return getConfigPaths(m_configKey);
    }

    private static String[][] getConfigPaths(final String configKey) {
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
