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
 *   16 Jan 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util;

import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.core.util.Pair;
import org.knime.node.parameters.persistence.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The sub-settings of filter settings which hold the information for filtering by regex or wildcard pattern.
 *
 * @author Paul Bärnreuther
 */
public class PatternFilter implements Persistable {

    /**
     * the pattern to which names are matched in case of m_mode = "REGEX" or "WILDCARD"
     */
    public String m_pattern; //NOSONAR

    private Pair<Pattern, String> m_compiledPattern;

    /**
     * whether m_pattern is case sensitive
     */
    public boolean m_isCaseSensitive; //NOSONAR

    /**
     * whether the pattern determines the excluded values or the included ones
     */
    public boolean m_isInverted; //NOSONAR

    /**
     * Initialise to empty pattern matching anything
     */
    public PatternFilter() {
        m_isCaseSensitive = false;
        m_isInverted = false;
        m_pattern = "";
    }

    /**
     * The different kinds of patterns we support.
     */
    @SuppressWarnings("javadoc")
    public enum PatternMode {
            REGEX, WILDCARD;

    }

    /**
     * Filter the given choices by the current pattern and mode.
     *
     * @param mode of the filter (either REGEX or WILDCARD)
     * @param choices the list of all possible strings
     * @return the array of currently selected strings with respect to the mode
     */
    @JsonIgnore
    public String[] getSelected(final PatternMode mode, final String[] choices) {
        final var predicate = getIsSelectedPredicate(mode);
        return Arrays.asList(choices).stream().filter(predicate).toArray(String[]::new);
    }

    /**
     * Construct a predicate which tests whether a given string is selected by the current pattern and mode.
     *
     * @param mode of the filter (either REGEX or WILDCARD)
     * @return a predicate on names
     */
    public Predicate<String> getIsSelectedPredicate(final PatternMode mode) {
        final Predicate<String> predicate = constructRawPredicate(mode);
        return getAugmentedPredicate(predicate, m_isCaseSensitive, m_isInverted);
    }

    private static Predicate<String> getAugmentedPredicate(final Predicate<String> originalPredicate,
        final boolean isCaseSensitive, final boolean isInverted) {
        final var directedPredicate = isInverted ? originalPredicate.negate() : originalPredicate;
        return string -> directedPredicate.test(isCaseSensitive ? string : string.toLowerCase(Locale.getDefault()));
    }

    private Predicate<String> constructRawPredicate(final PatternMode mode) {
        final var casedPattern = m_isCaseSensitive ? m_pattern : m_pattern.toLowerCase(Locale.getDefault());
        return switch (mode) {
            case REGEX -> getBasePredicateRegex(casedPattern);
            case WILDCARD -> choice -> wildcardMatch(choice, casedPattern);
            default -> throw new IllegalArgumentException("Unexpected value: " + mode);
        };
    }

    private Predicate<String> getBasePredicateRegex(final String casedPattern) {
        final var completedPattern = String.format("^%s$", casedPattern);
        final Pattern pattern;
        if (m_compiledPattern != null && m_compiledPattern.getSecond().equals(completedPattern)) {
            pattern = m_compiledPattern.getFirst();
        } else {
            pattern = Pattern.compile(completedPattern);
            m_compiledPattern = new Pair<>(pattern, completedPattern);
        }
        return casedPattern.isEmpty() ? choice -> false : pattern.asPredicate();
    }

}
