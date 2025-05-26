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

import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ManualFilterRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;

/**
 * The sub-settings of the column filter which hold the information for filtering manually.
 *
 * @author Paul Bärnreuther
 */
public class ManualFilter implements PersistableSettings {

    /**
     * the manually selected values in case of m_mode = "MANUAL".
     *
     * public for tests
     */
    public String[] m_manuallySelected; //NOSONAR

    /**
     * the (last) deselected values. It is necessary to store these in order to know which values of a new input
     * {@link DataTableSpec} are unknown in case unknown values are included.
     *
     * public for tests
     */
    public String[] m_manuallyDeselected; //NOSONAR

    /**
     * A value is unknown if it is not present in the available choices depending on the current context. If this
     * setting is true, these values will be selected/included when the selected values are updated after a
     * reconfiguration.
     *
     * public for tests
     *
     * Not renamed to "includeUnknownValues" due to backwards compatibility
     */
    public boolean m_includeUnknownColumns; //NOSONAR

    /**
     * Creates a new manual filter with the given initial values.
     *
     * @param initialSelected the initially manually selected values
     */
    public ManualFilter(final String[] initialSelected) {
        m_manuallySelected = initialSelected;
        m_manuallyDeselected = new String[0];
    }

    /**
     * Used for the {@link ManualFilterRendererSpec} to create the underlying JSON object for the manual filter.
     *
     * @param manuallySelected initially included values
     * @param manuallyDeselected initially excluded values
     * @param includeUnknownValues the position of unknown values in the filter
     */
    public ManualFilter(final String[] manuallySelected, final String[] manuallyDeselected,
        final boolean includeUnknownValues) {
        m_manuallySelected = manuallySelected;
        m_manuallyDeselected = manuallyDeselected;
        m_includeUnknownColumns = includeUnknownValues;
    }

    ManualFilter() {
    }

    /**
     * @return a predicate which tests for a value being manually selected of not manually deselected
     */
    public Predicate<String> getFilterPredicate() {
        if (m_includeUnknownColumns) {
            final var manuallyDeselectedSet = Set.of(m_manuallyDeselected);
            return ((Predicate<String>)manuallyDeselectedSet::contains).negate();
        }
        final var manuallySelectedSet = Set.of(m_manuallySelected);
        return manuallySelectedSet::contains;
    }

    /**
     * Returns the manually selected (also missing ones, i.e. not present in the choices) columns plus any unknown
     * columns if these are included. Note that the manually selected and manually deselected do not get updated by this
     * method. The only place where these get altered is if the dialog gets opened and new settings get saved. This way,
     * excluded columns stay marked as excluded when a view is executed without opening the dialog.
     *
     * @param choices for selected values from which previously unknown ones are either selected or deselected.
     * @return the manually selected columns plus the new previously unknown ones if these are included.
     */
    public String[] getUpdatedMissingValues(final String[] choices) {
        if (m_includeUnknownColumns) {
            return new String[0];
        }
        return Arrays.asList(m_manuallySelected).stream().filter(not(Set.of(choices)::contains)).toArray(String[]::new);

    }

}
