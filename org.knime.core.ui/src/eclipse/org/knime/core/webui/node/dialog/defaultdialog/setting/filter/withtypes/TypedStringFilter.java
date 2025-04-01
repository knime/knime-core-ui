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
 *   15 Dec 2022 Paul Bärnreuther: created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.DoNotPersistSelected;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.ManualFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.PatternFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.PatternFilter.PatternMode;

/**
 * A class used to filter names with associated types. I.e. the names can be determined using the same filter options as
 * {@link StringFilter} and an additional option that allows for filtering by a type.
 *
 * @author Paul Bärnreuther
 * @param <T> the type of the type filter
 */
public abstract class TypedStringFilter<T extends TypeFilter> implements PersistableSettings {

    /**
     * The setting representing the selected columns. This field is usually null and only needs to be set when
     * serializing the default node settings to be used within a view which expects the same json object as initial data
     * as it gets from the default node dialog. There, a field with key "selected" is added depending on the given
     * possible values whenever the configuration changes in the dialog. So setting this field has to be repeated when
     * creating these initial view data.
     */
    @Persistor(DoNotPersistSelected.class)
    String[] m_selected;

    /**
     * The way the selection is determined by
     */
    public TypedStringFilterMode m_mode; //NOSONAR

    /**
     * Settings regarding selection by pattern matching (regex or wildcard)
     */
    public PatternFilter m_patternFilter; //NOSONAR

    /**
     * Settings regarding manual selection
     */
    public ManualFilter m_manualFilter; //NOSONAR

    /**
     * Settings regarding selection per type
     */
    public T m_typeFilter; //NOSONAR

    /**
     * Initializes the column selection with an initial array of columns which are manually selected
     *
     * @param initialSelected the initial manually selected non-null columns
     * @param defaultTypeFilter the initial value of the type filter
     */
    protected TypedStringFilter(final String[] initialSelected, final T defaultTypeFilter) {
        m_mode = TypedStringFilterMode.MANUAL;
        m_manualFilter = new ManualFilter(Objects.requireNonNull(initialSelected));
        m_patternFilter = new PatternFilter();
        m_typeFilter = defaultTypeFilter;
    }

    /**
     * Initializes the column selection with an initial array of columns which are manually selected
     *
     * @param initialSelected the initial manually selected non-null columns
     * @param defaultTypeFilter the initial value of the type filter
     */
    protected TypedStringFilter(final List<String> initialSelected, final T defaultTypeFilter) {
        this(initialSelected.toArray(String[]::new), defaultTypeFilter);
    }

    /**
     * Set the filter to include unknown values while in manual mode.
     *
     * @param value
     *
     */
    protected void setIncludeUnknownValues(final boolean value) {
        m_manualFilter.m_includeUnknownColumns = value;
    }

    /**
     * In case of manual selection, the selected values can become missing by the choices dynamically changing. This
     * method returns the manually selected values that are missing in the list of choices.
     *
     * @see #getSelected
     * @param choices the non-null list of all possible names
     * @return the manually selected values that are selected but missing in the list of choices in case manual
     *         selection is chosen.
     */
    protected String[] getMissingSelected(final String[] choices) {
        Objects.requireNonNull(choices);
        return switch (m_mode) {
            case MANUAL -> m_manualFilter.getUpdatedMissingValues(choices);
            default -> new String[0];

        };
    }

    /**
     * Get selected columns, but only those that are available in the provided column list. This is likely the method
     * you want to use.
     *
     * @see #getMissingSelected
     * @param choices the non-null list of all possible column names
     * @param getSelectedFromTypeFilter the function to get the selected values when using the type filter
     * @return the subset of the choices that are selected by the filter.
     */
    protected String[] getSelected(final String[] choices, final Function<T, String[]> getSelectedFromTypeFilter) {
        Objects.requireNonNull(choices);
        if (m_mode == TypedStringFilterMode.TYPE) {
            return getSelectedFromTypeFilter.apply(m_typeFilter);
        }
        final var predicate = getStringFilterPredicate();
        return Arrays.asList(choices).stream().filter(predicate::test).toArray(String[]::new);
    }

    /**
     * If the order of manually selected values or missing manually selected values matter, use {@link #getSelected} or
     * {@link #getMissingSelected} instead.
     *
     * @throws IllegalStateException in case the mode is TYPE
     * @return a predicate on names
     */
    protected Predicate<String> getStringFilterPredicate() {
        CheckUtils.checkState(m_mode != TypedStringFilterMode.TYPE,
            "isSelected predicate on string is not possible for filtering by type.");
        return switch (m_mode) {
            case MANUAL -> m_manualFilter.getFilterPredicate();
            case REGEX -> m_patternFilter.getIsSelectedPredicate(PatternMode.REGEX);
            case WILDCARD -> m_patternFilter.getIsSelectedPredicate(PatternMode.WILDCARD);
            default -> throw new IllegalArgumentException("Unexpected value: " + m_mode);
        };

    }

    /**
     * Only to be called right before serialization to enable using this setting in UI Extensions that don't have access
     * to the choices nor the type information.
     *
     * @noreference
     *
     * @param selected
     */
    protected void setSelected(final String[] selected) {
        m_selected = selected;
    }

}
