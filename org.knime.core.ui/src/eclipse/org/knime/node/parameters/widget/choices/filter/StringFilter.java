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
package org.knime.node.parameters.widget.choices.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilterMode;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.ManualFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.PatternFilter;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * A class used for filtering a list of strings. The included strings can be chosen manually or by pattern matching. Use
 * a {@link ChoicesProvider} annotation with a {@link StringChoicesProvider} to define the choices.
 *
 * @author Paul Bärnreuther
 */
public class StringFilter implements Persistable {

    /**
     * The way the selection is determined by
     *
     * @noreference
     */
    public StringFilterMode m_mode; //NOSONAR

    /**
     * Settings regarding selection by pattern matching (regex or wildcard)
     *
     * @noreference
     */
    public PatternFilter m_patternFilter; //NOSONAR

    /**
     * Settings regarding manual selection
     *
     * @noreference
     */
    public ManualFilter m_manualFilter; //NOSONAR

    /**
     * Use this to construct a string array filter with an initial array of columns which are manually selected
     *
     * @param initialSelected the initial manually selected non-null columns
     */
    public StringFilter(final String[] initialSelected) {
        m_mode = StringFilterMode.MANUAL;
        m_manualFilter = new ManualFilter(Objects.requireNonNull(initialSelected));
        m_patternFilter = new PatternFilter();
    }

    /**
     * Use this to construct a string array filter with an initial array of columns which are manually selected
     *
     * @param initialSelected the initial manually selected non-null columns
     */
    public StringFilter(final List<String> initialSelected) {
        this(initialSelected.toArray(String[]::new));
    }

    /**
     * Exclude the unknown columns while in manual mode
     */
    public void excludeUnknownColumn() {
        m_manualFilter.m_includeUnknownColumns = false;
    }

    /**
     * Initializes the column selection with no initially selected columns.
     */
    public StringFilter() {
        this(new String[0]);
    }

    /**
     * This method returns a non-empty array if manual selection is active, unknown values are excluded and there exist
     * manually selected values that no longer exist in the choices.
     *
     * @param choices the non-null list of all possible names
     * @return the array of currently selected names with respect to the mode
     */
    public String[] getMissingSelected(final String[] choices) {
        if (m_mode == StringFilterMode.MANUAL) {
            return m_manualFilter.getUpdatedMissingValues(Objects.requireNonNull(choices));
        }
        return filter(choices);
    }

    /**
     * Filters the choices based on the current selection mode.
     *
     * @param choices the non-null list of all possible names
     * @return the array of currently selected names with respect to the mode which are contained in the given array of
     *         choices
     */
    public String[] filter(final String[] choices) {
        if (m_mode == StringFilterMode.MANUAL) {
            final var predicate = m_manualFilter.getFilterPredicate();
            return Arrays.stream(choices).filter(predicate::test).toArray(String[]::new);
        } else {
            return m_patternFilter.getSelected(m_mode.toPatternMode(), choices);
        }
    }
}
