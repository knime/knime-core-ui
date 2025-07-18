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
package org.knime.node.parameters.widget.choices.filter;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;

/**
 * A class used to store several representation of column choices. I.e. the columns can be determined using one of the
 * modes of {@link TypedStringFilterMode}.
 *
 * Use a {@link ColumnFilterWidget} annotation to use this class in a dialog and provide the column choices.
 *
 * @author Paul Bärnreuther
 */
public final class ColumnFilter extends TypedStringFilter<ColumnTypeFilter> {

    /**
     * A column filter with excluded unknown values and no included columns.
     */
    public ColumnFilter() {
        this(new String[0]);
    }

    /**
     * A column filter with excluded unknown values and initially selected included columns.
     *
     * @param selected the initially selected columns
     */
    public ColumnFilter(final String[] selected) {
        super(selected, new ColumnTypeFilter());
    }

    /**
     * A column filter with excluded unknown values and initially selected included columns.
     *
     * @param selectedColumns the initially selected columns
     */
    public ColumnFilter(final List<DataColumnSpec> selectedColumns) {
        super(selectedColumns.stream().map(DataColumnSpec::getName).toList(), new ColumnTypeFilter());
    }

    /**
     * Set the column filter to exclude unknown columns while in manual mode.
     *
     * @return the instance
     */
    public ColumnFilter withExcludeUnknownColumns() {
        setIncludeUnknownValues(false);
        return this;
    }

    /**
     * Set the column filter to include unknown columns while in manual mode.
     *
     * @return the instance
     */
    public ColumnFilter withIncludeUnknownColumns() {
        setIncludeUnknownValues(true);
        return this;
    }

    /**
     * In case of manual selection, the selected values can become missing by the columns dynamically changing. This
     * method returns the manually selected columns which are not contained in the provided column list. You likely want
     * to use {@link #filter} instead.
     *
     * @see #filter
     * @param choices the non-null list of all possible column names
     * @return the subset of the choices that are selected by the filter plus the ones that are selected but missing in
     *         the list of choices in case manual selection is chosen.
     */
    public String[] getMissingSelected(final List<DataColumnSpec> choices) {
        Objects.requireNonNull(choices);
        return super.getMissingSelected(extractColumnNames(choices));
    }

    /**
     * Get selected columns, but only those that are available in the provided column list. This is likely the method
     * you want to use.
     *
     * @see #getMissingSelected
     * @param choices the non-null list of all possible column names
     * @return the subset of the choices that are selected by the filter.
     */
    // UIEXT-2657 refactor to return list of pairs of indices and column specs
    public String[] filter(final List<DataColumnSpec> choices) {
        Objects.requireNonNull(choices);
        final var choicesColumnNames = extractColumnNames(choices);
        return super.getSelected(choicesColumnNames, t -> t.filter(choices));
    }

    /**
     * Get the filter predicate for the column filter. The predicate will return true for all columns that are selected
     * by the current configuration.
     *
     * @return the filter predicate
     */
    public Predicate<DataColumnSpec> getFilterPredicate() {
        if (m_mode == TypedStringFilterMode.TYPE) {
            return m_typeFilter.getFilterPredicate();
        }
        final var namePredicate = super.getStringFilterPredicate();
        return col -> namePredicate.test(col.getName());
    }

    /**
     * Get selected columns, but only those that are available in the provided data table spec. This is likely the
     * method you want to use.
     *
     * @param spec to take all columns from
     * @return the array of currently selected columns with respect to the mode which are contained in the given array
     */
    public String[] filterFromFullSpec(final DataTableSpec spec) {
        return filter(spec.stream().toList());
    }

    /**
     * In case of manual selection, the selected values can become missing by the columns dynamically changing. This
     * method returns the manually selected columns that are missing in the provided data table spec. You likely want to
     * use {@link #filterFromFullSpec} instead.
     *
     * @param spec to take all columns from
     * @return the array of currently selected columns with respect to the mode which are contained in the given array
     */
    public String[] getMissingSelectedFromFullSpec(final DataTableSpec spec) {
        return getMissingSelected(spec.stream().toList());
    }

    private static String[] extractColumnNames(final List<DataColumnSpec> choices) {
        return choices.stream().map(DataColumnSpec::getName).toArray(String[]::new);
    }

    /**
     * If this filter is used to convey a column selection in its serialized form (i.e. an object containing a parameter
     * which is a string array; without access to the above methods), use this method to first set the selected values.
     *
     * Setting the selected values this way will not change the state of the filter itself, i.e. it will not have an
     * impact on the returned values of the above methods.
     *
     * @param choices from which the selected columns are taken
     * @return the selected choices as they will be serialized
     *
     * @noreference
     */
    public String[] updateSelectedColumnsBeforeSerialization(final List<DataColumnSpec> choices) {
        final var missingSelected = getMissingSelected(choices);
        final var nonMissingSelected = filter(choices);
        final var newSelected = Stream.concat(//
            Stream.of(nonMissingSelected), //
            Stream.of(missingSelected)//
        ).toArray(String[]::new);
        setSelectedBeforeSerialization(newSelected);
        return newSelected;
    }

    /**
     * Only for serialization purposes. Do not use this method. Use
     * {@link #updateSelectedColumnsBeforeSerialization(List)} if possible instead. This method is only added for the
     * TableView whose displayed columns is controlled by non-ColumnFilter settings in the extending statistics view.
     *
     * @param selected the selected columns
     *
     * @noreference
     */
    public void setSelectedBeforeSerialization(final String[] selected) {
        super.setSelected(selected);
    }

}
