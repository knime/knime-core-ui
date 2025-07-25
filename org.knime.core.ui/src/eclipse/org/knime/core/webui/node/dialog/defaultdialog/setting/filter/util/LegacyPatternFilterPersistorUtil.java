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
 *   Jan 22, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil.ColumnFilterBuilder;
import org.knime.node.parameters.widget.choices.filter.LegacyFilterUtil.StringFilterBuilder;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("javadoc")
public final class LegacyPatternFilterPersistorUtil {

    private LegacyPatternFilterPersistorUtil() {
        // Utility
    }

    /**
     * See PatternFilterConfiguration.CFG_TYPE
     */
    public static final String PATTERN_FILTER_TYPE = "type";

    /**
     * See PatternFilterConfiguration.PatternFilterType.Regex
     */
    public static final String PATTERN_FILTER_REGEX = "Regex";

    /**
     * See PatternFilterConfiguration.PatternFilterType.Wildcard
     */
    public static final String PATTERN_FILTER_WILDCARD = "Wildcard";

    /**
     * See PatternFilterConfiguration.CFG_PATTERN
     */
    public static final String PATTERN_FILTER_PATTERN = "pattern";

    /**
     * See PatternFilterConfiguration.CFG_CASESENSITIVE
     */
    public static final String PATTERN_FILTER_CASESENSITIVE = "caseSensitive";

    /**
     * See PatternFilterConfiguration.CFG_EXCLUDEMATCHING
     */
    public static final String PATTERN_FILTER_EXCLUDEMATCHING = "excludeMatching";

    public enum PatternMode {
            WILDCARD, REGEX;

        public ColumnFilterBuilder.Mode toColumnFilterMode() {
            return switch (this) {
                case WILDCARD -> ColumnFilterBuilder.Mode.WILDCARD;
                case REGEX -> ColumnFilterBuilder.Mode.REGEX;
            };
        }

        public StringFilterBuilder.Mode toStringFilterMode() {
            return switch (this) {
                case WILDCARD -> StringFilterBuilder.Mode.WILDCARD;
                case REGEX -> StringFilterBuilder.Mode.REGEX;
            };
        }
    }

    public static PatternMode loadPatternMode(final ConfigBaseRO patternMatchingSettings)
        throws InvalidSettingsException {
        var patternMatchingType = patternMatchingSettings.getString(PATTERN_FILTER_TYPE);
        if (PATTERN_FILTER_WILDCARD.equals(patternMatchingType)) {
            return PatternMode.WILDCARD;
        } else if (PATTERN_FILTER_REGEX.equals(patternMatchingType)) {
            return PatternMode.REGEX;
        } else {
            throw new InvalidSettingsException("Unsupported name pattern type: " + patternMatchingType);
        }
    }

    public static PatternFilter loadPatternMatching(final ConfigBaseRO patternMatchingSettings)
        throws InvalidSettingsException {
        var patternFilter = new PatternFilter();
        patternFilter.m_pattern = patternMatchingSettings.getString(PATTERN_FILTER_PATTERN);
        patternFilter.m_isCaseSensitive = patternMatchingSettings.getBoolean(PATTERN_FILTER_CASESENSITIVE);
        // In some very old workflows this field might not have existed, yet, and we default to 'false'
        // see, e.g., knime://Testflows/Testflows%20(master)/knime-base/Loops/test_LoopEnd_Bug4029
        patternFilter.m_isInverted = patternMatchingSettings.getBoolean(PATTERN_FILTER_EXCLUDEMATCHING, false);
        return patternFilter;
    }

    public static void savePattern(final String pattern, final ConfigBaseWO patternMatchingSettings) {
        patternMatchingSettings.addString(PATTERN_FILTER_PATTERN, pattern);
    }

    public static void savePatternCaseSensitive(final boolean caseSensitive,
        final ConfigBaseWO patternMatchingSettings) {
        patternMatchingSettings.addBoolean(PATTERN_FILTER_CASESENSITIVE, caseSensitive);
    }

    public static void savePatternInverted(final boolean inverted, final ConfigBaseWO patternMatchingSettings) {
        patternMatchingSettings.addBoolean(PATTERN_FILTER_EXCLUDEMATCHING, inverted);
    }

    public static void savePatternMode(final PatternMode mode, final ConfigBaseWO patternMatchingSettings) {
        patternMatchingSettings.addString(PATTERN_FILTER_TYPE,
            mode == PatternMode.REGEX ? PATTERN_FILTER_REGEX : PATTERN_FILTER_WILDCARD);
    }

    public static String loadPattern(final ConfigBaseRO patternMatchingSettings) throws InvalidSettingsException {
        return patternMatchingSettings.getString(PATTERN_FILTER_PATTERN);
    }

    public static boolean loadPatternCaseSensitive(final ConfigBaseRO patternMatchingSettings)
        throws InvalidSettingsException {
        return patternMatchingSettings.getBoolean(PATTERN_FILTER_CASESENSITIVE);
    }

    public static boolean loadPatternInverted(final ConfigBaseRO patternMatchingSettings)
        throws InvalidSettingsException {
        // In some very old workflows this field might not have existed, yet, and we default to 'false'
        // see, e.g., knime://Testflows/Testflows%20(master)/knime-base/Loops/test_LoopEnd_Bug4029
        return patternMatchingSettings.getBoolean(PATTERN_FILTER_EXCLUDEMATCHING, false);
    }
}
