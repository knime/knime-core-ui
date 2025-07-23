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
 *   Mar 4, 2025 (paulbaernreuther): created
 */
package org.knime.node.parameters.widget.choices.filter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnTypeToPossibleTypeValueUtil;
import org.knime.node.parameters.widget.choices.TypedStringChoice.PossibleTypeValue;

/**
 * The type filter used within a {@link ColumnFilter}.
 *
 * @author Paul Bärnreuther
 */
final class ColumnTypeFilter extends TypeFilter {

    /**
     * Default constructor.
     */
    ColumnTypeFilter() {
        super();
    }

    /**
     * @param selectedTypes
     */
    ColumnTypeFilter(final String[] selectedTypes) {
        super(selectedTypes);
    }

    @Override
    protected Optional<PossibleTypeValue> fromTypeId(final String typeId) {
        return ColumnTypeToPossibleTypeValueUtil.fromPreferredValueClassString(typeId);
    }

    /**
     * Filter the columns based on the selected types
     *
     * @param choices the list of all possible columns
     * @return the array of currently selected columns
     */
    String[] filter(final List<DataColumnSpec> choices) {
        return choices.stream().filter(getFilterPredicate()).map(DataColumnSpec::getName).toArray(String[]::new);
    }

    /**
     * Returns a predicate that evaluates to true for columns that match the selected types.
     *
     * @return the predicate for filtering columns by type
     */
    Predicate<DataColumnSpec> getFilterPredicate() {
        var selectedTypes = Set.of(m_selectedTypes);
        return col -> selectedTypes.contains(columnToTypeString(col));
    }

    /**
     * @param colSpec the column spec
     * @return the string representation of the data type
     */
    static String columnToTypeString(final DataColumnSpec colSpec) {
        return colSpec.getType().getPreferredValueClass().getName();
    }

}
