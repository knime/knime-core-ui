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

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable.FlowVariableTypeFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;

/**
 * A class used to store several representation of column choices. I.e. the columns can be determined using one of the
 * modes of {@link TypedStringFilterMode}.
 *
 * Use a {@link FlowVariableFilterWidget} annotation to use this class in a dialog and provide the flow variable
 * choices.
 *
 * @author Paul Bärnreuther
 */
public final class FlowVariableFilter extends TypedStringFilter<FlowVariableTypeFilter> {

    /**
     * A filter with excluded unknown values and no included columns.
     */
    public FlowVariableFilter() {
        this(new String[0]);
    }

    /**
     * A filter with excluded unknown values and initially selected included flow variables.
     *
     * @param selected the initially selected flow variable names
     */
    public FlowVariableFilter(final String[] selected) {
        super(selected, new FlowVariableTypeFilter());
    }

    /**
     * A filter with excluded unknown values and initially selected included flow variables.
     *
     * @param selected the initially selected flow variable names
     */
    public FlowVariableFilter(final List<String> selected) {
        super(selected, new FlowVariableTypeFilter());
    }

    /**
     * Set the column filter to exclude unknown columns while in manual mode.
     *
     * @return the instance
     */
    public FlowVariableFilter withExcludedUnknowns() {
        setIncludeUnknownValues(false);
        return this;
    }

    /**
     * Set the column filter to include unknown columns while in manual mode.
     *
     * @return the instance
     */
    public FlowVariableFilter withIncludedUnknowns() {
        setIncludeUnknownValues(true);
        return this;
    }

    /**
     * @return a predicate on flow variables
     */
    public Predicate<FlowVariable> getFilterPredicate() {
        if (m_mode == TypedStringFilterMode.TYPE) {
            return m_typeFilter.getFilterPredicate();
        }
        final var namePredicate = super.getStringFilterPredicate();
        return flowVar -> namePredicate.test(flowVar.getName());
    }

    /**
     * Get selected flow variables
     *
     * @param flowVariables the flow variables to filter
     * @return the array of currently selected flow variables with respect to the mode which are contained in the given
     *         input
     */
    public List<FlowVariable> filter(final Collection<FlowVariable> flowVariables) {
        final var predicate = getFilterPredicate();
        return flowVariables.stream().filter(predicate::test).toList();
    }

    /**
     * Get the flow variables which are missing in the given input
     *
     * @param flowVariables the available flow variable choices
     * @return the array of currently manually selected flow variables which are not contained in the given input
     */
    public String[] getMissingSelected(final Collection<FlowVariable> flowVariables) {
        if (m_mode == TypedStringFilterMode.MANUAL) {
            return getMissingSelected(flowVariables.stream().map(FlowVariable::getName).toArray(String[]::new));
        } else {
            return new String[0];
        }
    }

}
