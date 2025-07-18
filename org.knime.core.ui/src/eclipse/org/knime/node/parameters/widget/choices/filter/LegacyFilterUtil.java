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
 *   Jul 18, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.widget.choices.filter;

import java.util.function.Consumer;

import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilterMode;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.ManualFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.PatternFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class LegacyFilterUtil {

    /**
     * Default constructor.
     */
    private LegacyFilterUtil() {
    }

    /**
     * Interface for saving pattern data components.
     */
    public interface PatternDataSaver {
        /**
         * Save pattern data components.
         *
         * @param pattern the pattern string
         * @param caseSensitive whether pattern matching is case sensitive
         * @param inverted whether pattern matching is inverted
         * @param mode the filter mode
         */
        void save(String pattern, boolean caseSensitive, boolean inverted, PatternMode mode);
    }

    @SuppressWarnings("javadoc")
    public enum PatternMode {
        WILDCARD,
        REGEX;

        static PatternMode fromStringFilterMode(final StringFilterBuilder.Mode mode) {
            return mode == StringFilterBuilder.Mode.REGEX ? REGEX : WILDCARD;
        }

        static PatternMode fromColumnFilterMode(final ColumnFilterBuilder.Mode mode) {
            return mode == ColumnFilterBuilder.Mode.REGEX ? REGEX : WILDCARD;
        }
    }

    /**
     * Save a StringFilter by extracting its internal data and passing it to the provided consumer methods.
     *
     * @param stringFilter the filter to save
     * @param modeSaver consumer for the filter mode
     * @param manuallySelectedSaver consumer for manually selected strings
     * @param manuallyDeselectedSaver consumer for manually deselected strings
     * @param includeUnknownColumnsSaver consumer for the include unknown columns flag
     * @param patternDataSaver consumer for pattern data (pattern, case sensitive, inverted, mode)
     */
    public static void saveStringFilter(final StringFilter stringFilter,
        final Consumer<StringFilterBuilder.Mode> modeSaver, final Consumer<String[]> manuallySelectedSaver,
        final Consumer<String[]> manuallyDeselectedSaver, final Consumer<Boolean> includeUnknownColumnsSaver,
        final PatternDataSaver patternDataSaver) {
        // Extract mode and convert to public enum
        var mode = StringFilterBuilder.Mode.fromStringFilterMode(stringFilter.m_mode);
        modeSaver.accept(mode);

        // Extract manual filter data
        manuallySelectedSaver.accept(stringFilter.m_manualFilter.m_manuallySelected);
        manuallyDeselectedSaver.accept(stringFilter.m_manualFilter.m_manuallyDeselected);
        includeUnknownColumnsSaver.accept(stringFilter.m_manualFilter.m_includeUnknownColumns);

        // Extract pattern filter data
        patternDataSaver.save(stringFilter.m_patternFilter.m_pattern, stringFilter.m_patternFilter.m_isCaseSensitive,
            stringFilter.m_patternFilter.m_isInverted, PatternMode.fromStringFilterMode(mode));
    }

    /**
     * Builder for {@link StringFilter} that only uses trivial parameters (String, String[], boolean) to avoid exposing
     * internal object structures.
     */
    public static final class StringFilterBuilder {

        /**
         * Public enum for string filter modes that hides the internal {@link StringFilterMode}.
         */
        public enum Mode {
                /**
                 * Manual selection of strings.
                 */
                MANUAL,

                /**
                 * Pattern matching using regular expressions.
                 */
                REGEX,

                /**
                 * Pattern matching using wildcards.
                 */
                WILDCARD;

            /**
             * Convert to internal StringFilterMode.
             *
             * @return the corresponding StringFilterMode
             */
            public StringFilterMode toStringFilterMode() {
                return switch (this) {
                    case MANUAL -> StringFilterMode.MANUAL;
                    case REGEX -> StringFilterMode.REGEX;
                    case WILDCARD -> StringFilterMode.WILDCARD;
                };
            }

            /**
             * Convert from internal StringFilterMode.
             *
             * @param mode the internal StringFilterMode
             * @return the corresponding public Mode
             */
            public static Mode fromStringFilterMode(final StringFilterMode mode) {
                return switch (mode) {
                    case MANUAL -> MANUAL;
                    case REGEX -> REGEX;
                    case WILDCARD -> WILDCARD;
                };
            }

        }

        private Mode m_mode = Mode.MANUAL;

        private String[] m_manuallySelected = new String[0];

        private String[] m_manuallyDeselected = new String[0];

        private boolean m_includeUnknownColumns = false;

        private String m_pattern = "";

        private boolean m_patternCaseSensitive = false;

        private boolean m_patternInverted = false;

        /**
         * Creates a new builder with default values.
         */
        public StringFilterBuilder() {
        }

        /**
         * Set the filter mode.
         *
         * @param mode the filter mode
         * @return this builder
         */
        public StringFilterBuilder withMode(final Mode mode) {
            m_mode = mode;
            return this;
        }

        /**
         * Set the manually selected strings.
         *
         * @param manuallySelected the manually selected strings
         * @return this builder
         */
        public StringFilterBuilder withManuallySelected(final String[] manuallySelected) {
            m_manuallySelected = manuallySelected;
            return this;
        }

        /**
         * Set the manually deselected strings.
         *
         * @param manuallyDeselected the manually deselected strings
         * @return this builder
         */
        public StringFilterBuilder withManuallyDeselected(final String[] manuallyDeselected) {
            m_manuallyDeselected = manuallyDeselected;
            return this;
        }

        /**
         * Set whether to include unknown strings.
         *
         * @param includeUnknownColumns whether to include unknown strings
         * @return this builder
         */
        public StringFilterBuilder withIncludeUnknownColumns(final boolean includeUnknownColumns) {
            m_includeUnknownColumns = includeUnknownColumns;
            return this;
        }

        /**
         * Set the pattern for pattern matching.
         *
         * @param pattern the pattern
         * @return this builder
         */
        public StringFilterBuilder withPattern(final String pattern) {
            m_pattern = pattern;
            return this;
        }

        /**
         * Set whether pattern matching is case sensitive.
         *
         * @param caseSensitive whether pattern matching is case sensitive
         * @return this builder
         */
        public StringFilterBuilder withPatternCaseSensitive(final boolean caseSensitive) {
            m_patternCaseSensitive = caseSensitive;
            return this;
        }

        /**
         * Set whether pattern matching is inverted (exclude matching).
         *
         * @param inverted whether pattern matching is inverted
         * @return this builder
         */
        public StringFilterBuilder withPatternInverted(final boolean inverted) {
            m_patternInverted = inverted;
            return this;
        }

        /**
         * Build the StringFilter with the configured parameters.
         *
         * @return the configured StringFilter
         */
        public StringFilter build() {
            var stringFilter = new StringFilter();
            stringFilter.m_mode = m_mode.toStringFilterMode();

            stringFilter.m_manualFilter =
                new ManualFilter(m_manuallySelected, m_manuallyDeselected, m_includeUnknownColumns);

            stringFilter.m_patternFilter = new PatternFilter();
            stringFilter.m_patternFilter.m_pattern = m_pattern;
            stringFilter.m_patternFilter.m_isCaseSensitive = m_patternCaseSensitive;
            stringFilter.m_patternFilter.m_isInverted = m_patternInverted;

            return stringFilter;
        }
    }

    /**
     * Save a ColumnFilter by extracting its internal data and passing it to the provided consumer methods.
     *
     * @param columnFilter the filter to save
     * @param modeSaver consumer for the filter mode
     * @param manuallySelectedSaver consumer for manually selected columns
     * @param manuallyDeselectedSaver consumer for manually deselected columns
     * @param includeUnknownColumnsSaver consumer for the include unknown columns flag
     * @param patternDataSaver consumer for pattern data (pattern, case sensitive, inverted, mode)
     * @param selectedTypesSaver consumer for the selected types array
     */
    public static void saveColumnFilter(
        final ColumnFilter columnFilter,
        final Consumer<ColumnFilterBuilder.Mode> modeSaver,
        final Consumer<String[]> manuallySelectedSaver,
        final Consumer<String[]> manuallyDeselectedSaver,
        final Consumer<Boolean> includeUnknownColumnsSaver,
        final PatternDataSaver patternDataSaver,
        final Consumer<String[]> selectedTypesSaver
    ) {
        // Extract mode and convert to public enum
        var mode = ColumnFilterBuilder.Mode.fromTypedStringFilterMode(columnFilter.m_mode);
        modeSaver.accept(mode);

        // Extract manual filter data
        manuallySelectedSaver.accept(columnFilter.m_manualFilter.m_manuallySelected);
        manuallyDeselectedSaver.accept(columnFilter.m_manualFilter.m_manuallyDeselected);
        includeUnknownColumnsSaver.accept(columnFilter.m_manualFilter.m_includeUnknownColumns);

        // Extract pattern filter data
        patternDataSaver.save(
            columnFilter.m_patternFilter.m_pattern,
            columnFilter.m_patternFilter.m_isCaseSensitive,
            columnFilter.m_patternFilter.m_isInverted,
            PatternMode.fromColumnFilterMode(mode)
        );

        // Extract type filter data
        selectedTypesSaver.accept(columnFilter.m_typeFilter.m_selectedTypes);
    }

    /**
     * Builder for {@link ColumnFilter} that only uses trivial parameters (String, String[], boolean) to avoid exposing
     * internal object structures.
     */
    public static final class ColumnFilterBuilder {

        /**
         * Public enum for column filter modes that hides the internal {@link TypedStringFilterMode}.
         */
        public enum Mode {
                /**
                 * Manual selection of columns.
                 */
                MANUAL,

                /**
                 * Pattern matching using regular expressions.
                 */
                REGEX,

                /**
                 * Pattern matching using wildcards.
                 */
                WILDCARD,

                /**
                 * Filter by column type.
                 */
                TYPE;

            /**
             * Convert to internal TypedStringFilterMode.
             *
             * @return the corresponding TypedStringFilterMode
             */
            public TypedStringFilterMode toTypedStringFilterMode() {
                return switch (this) {
                    case MANUAL -> TypedStringFilterMode.MANUAL;
                    case REGEX -> TypedStringFilterMode.REGEX;
                    case WILDCARD -> TypedStringFilterMode.WILDCARD;
                    case TYPE -> TypedStringFilterMode.TYPE;
                };
            }

            /**
             * Convert from internal TypedStringFilterMode to public Mode enum.
             *
             * @param mode the TypedStringFilterMode to convert
             * @return the corresponding Mode
             */
            public static Mode fromTypedStringFilterMode(final TypedStringFilterMode mode) {
                return switch (mode) {
                    case MANUAL -> Mode.MANUAL;
                    case REGEX -> Mode.REGEX;
                    case WILDCARD -> Mode.WILDCARD;
                    case TYPE -> Mode.TYPE;
                };
            }

        }

        private Mode m_mode = Mode.MANUAL;

        private String[] m_manuallySelected = new String[0];

        private String[] m_manuallyDeselected = new String[0];

        private boolean m_includeUnknownColumns = false;

        private String m_pattern = "";

        private boolean m_patternCaseSensitive = false;

        private boolean m_patternInverted = false;

        private String[] m_selectedTypes = new String[0];

        /**
         * Creates a new builder with default values.
         */
        public ColumnFilterBuilder() {
        }

        /**
         * Set the filter mode.
         *
         * @param mode the filter mode
         * @return this builder
         */
        public ColumnFilterBuilder withMode(final Mode mode) {
            m_mode = mode;
            return this;
        }

        /**
         * Set the manually selected columns.
         *
         * @param manuallySelected the manually selected columns
         * @return this builder
         */
        public ColumnFilterBuilder withManuallySelected(final String[] manuallySelected) {
            m_manuallySelected = manuallySelected;
            return this;
        }

        /**
         * Set the manually deselected columns.
         *
         * @param manuallyDeselected the manually deselected columns
         * @return this builder
         */
        public ColumnFilterBuilder withManuallyDeselected(final String[] manuallyDeselected) {
            m_manuallyDeselected = manuallyDeselected;
            return this;
        }

        /**
         * Set whether to include unknown columns.
         *
         * @param includeUnknownColumns whether to include unknown columns
         * @return this builder
         */
        public ColumnFilterBuilder withIncludeUnknownColumns(final boolean includeUnknownColumns) {
            m_includeUnknownColumns = includeUnknownColumns;
            return this;
        }

        /**
         * Set the pattern for pattern matching.
         *
         * @param pattern the pattern
         * @return this builder
         */
        public ColumnFilterBuilder withPattern(final String pattern) {
            m_pattern = pattern;
            return this;
        }

        /**
         * Set whether pattern matching is case sensitive.
         *
         * @param caseSensitive whether pattern matching is case sensitive
         * @return this builder
         */
        public ColumnFilterBuilder withPatternCaseSensitive(final boolean caseSensitive) {
            m_patternCaseSensitive = caseSensitive;
            return this;
        }

        /**
         * Set whether pattern matching is inverted (exclude matching).
         *
         * @param inverted whether pattern matching is inverted
         * @return this builder
         */
        public ColumnFilterBuilder withPatternInverted(final boolean inverted) {
            m_patternInverted = inverted;
            return this;
        }

        /**
         * Set the selected types for type filtering.
         *
         * @param selectedTypes the selected types
         * @return this builder
         */
        public ColumnFilterBuilder withSelectedTypes(final String[] selectedTypes) {
            m_selectedTypes = selectedTypes;
            return this;
        }

        /**
         * Build the ColumnFilter with the configured parameters.
         *
         * @return the configured ColumnFilter
         */
        public ColumnFilter build() {
            var columnFilter = new ColumnFilter();
            columnFilter.m_mode = m_mode.toTypedStringFilterMode();

            columnFilter.m_manualFilter =
                new ManualFilter(m_manuallySelected, m_manuallyDeselected, m_includeUnknownColumns);

            columnFilter.m_patternFilter = new PatternFilter();
            columnFilter.m_patternFilter.m_pattern = m_pattern;
            columnFilter.m_patternFilter.m_isCaseSensitive = m_patternCaseSensitive;
            columnFilter.m_patternFilter.m_isInverted = m_patternInverted;

            columnFilter.m_typeFilter = new ColumnTypeFilter(m_selectedTypes);

            return columnFilter;
        }
    }
}
