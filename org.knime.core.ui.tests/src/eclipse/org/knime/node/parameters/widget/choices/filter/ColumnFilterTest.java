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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.widget.choices.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.testing.util.TableTestUtil;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class ColumnFilterTest {

    private static final DataTableSpec TABLE_SPEC = TableTestUtil.getDefaultTestSpec();

    private static final DataColumnSpec COL_SPEC = TABLE_SPEC.getColumnSpec(0);

    private static final NodeParametersInput CONTEXT =
        NodeParametersInputImpl.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
            new PortObjectSpec[]{TABLE_SPEC}, null, null);

    @Test
    void testGetSelectedByManualWithIncludeUnknownColumns() {
        final var selection = new ColumnFilter(new String[]{"Old selected"});
        selection.m_manualFilter.m_manuallyDeselected = new String[]{"Old deselected"};
        selection.m_manualFilter.m_includeUnknownColumns = true;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(new String[]{COL_SPEC.getName()});
        assertThat(selection.getMissingSelected(choices)).isEqualTo(new String[0]);
    }

    @Test
    void testGetSelectedByManualOnlyIncludeNewColumnsIfUnknown() {
        final var selection = new ColumnFilter(new String[]{"Old selected"});
        final var choices = List.of(COL_SPEC);
        selection.m_manualFilter.m_manuallyDeselected = new String[]{COL_SPEC.getName()};
        selection.m_manualFilter.m_includeUnknownColumns = true;
        assertThat(selection.getMissingSelected(choices)).isEqualTo(new String[0]);
        assertThat(selection.filter(choices)).isEqualTo(new String[0]);
    }

    @Test
    void testGetSelectedByManualWithExcludedUnknownColumns() {
        final var selection = new ColumnFilter(new String[]{"Old selected"});
        selection.m_manualFilter.m_manuallyDeselected = new String[]{"Old deselected"};
        selection.m_manualFilter.m_includeUnknownColumns = false;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.getMissingSelected(choices)).isEqualTo(new String[]{"Old selected"});
        assertThat(selection.filter(choices)).isEqualTo(new String[0]);
    }

    @Test
    void testGetSelectedByType() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.TYPE;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});

        selection.m_typeFilter = new ColumnTypeFilter(new String[]{ColumnTypeFilter.columnToTypeString(COL_SPEC)});
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));
    }

    @Test
    void testGetSelectedByRegex() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.REGEX;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});

        selection.m_patternFilter.m_pattern = ".*";
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));
    }

    @Test
    void testGetSelectedByInvertedRegex() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.REGEX;
        selection.m_patternFilter.m_isInverted = true;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));

        selection.m_patternFilter.m_pattern = ".*";
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});
    }

    @Test
    void testGetSelectedByWildcard() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.WILDCARD;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});

        selection.m_patternFilter.m_pattern = "*";
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));
    }

    @Test
    void testGetSelectedByInvertedWildcard() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.WILDCARD;
        selection.m_patternFilter.m_isInverted = true;
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));

        selection.m_patternFilter.m_pattern = "*";
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});
    }

    @Test
    void testGetSelectedByCaseSensitiveWildcard() {
        final var selection = new ColumnFilter();
        selection.m_mode = TypedStringFilterMode.WILDCARD;
        selection.m_patternFilter.m_pattern = COL_SPEC.getName().toUpperCase();
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(getColumnNames(choices));

        selection.m_patternFilter.m_isCaseSensitive = true;
        assertThat(selection.filter(choices)).isEqualTo(new String[]{});
    }

    /**
     * Tests that an initially empty selection works.
     */
    @Test
    void testInitialSelectedEmpty() {
        final var empty = new String[0];
        final var selection = new ColumnFilter(empty);
        final var choices = List.of(COL_SPEC);
        assertThat(selection.filter(choices)).isEqualTo(empty);
    }

    /**
     * Tests that an initially null selection does not work.
     */
    @Test
    void testInitialSelectedNullThrows() {
        final String[] manuallySelected = null; // avoid ambiguous constructor call
        assertThrows(NullPointerException.class, () -> new ColumnFilter(manuallySelected));
    }

    static String[] getColumnNames(final List<DataColumnSpec> cols) {
        return cols.stream().map(DataColumnSpec::getName).toArray(String[]::new);
    }

}
