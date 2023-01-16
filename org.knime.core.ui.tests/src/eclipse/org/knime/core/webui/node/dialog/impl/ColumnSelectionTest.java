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
package org.knime.core.webui.node.dialog.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings.SettingsCreationContext;
import org.knime.testing.node.view.TableTestUtil;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class ColumnSelectionTest {

    private static final DataTableSpec TABLE_SPEC = TableTestUtil.getDefaultTestSpec();

    private static final DataColumnSpec COL_SPEC = TABLE_SPEC.getColumnSpec(0);

    private static final SettingsCreationContext CONTEXT =
        new SettingsCreationContext(new PortObjectSpec[]{TABLE_SPEC}, null);

    @Test
    void testGetByManual() {
        final var selection = new ColumnSelection(CONTEXT);
        assertThat(ColumnSelection.get(selection, null, TABLE_SPEC)).isEqualTo(new String[]{});

        selection.m_manuallySelected = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, null, TABLE_SPEC)).isEqualTo(selection.m_manuallySelected);
    }

    @Test
    void testGetByType() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.TYPE;
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});

        selection.m_selectedTypes = List.of(ColumnSelection.typeToString(COL_SPEC.getType()));
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);
    }

    @Test
    void testGetByRegex() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.REGEX;
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});

        selection.m_pattern = ".*";
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);
    }

    @Test
    void testGetByInvertedRegex() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.REGEX;
        selection.m_isInverted = true;
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);

        selection.m_pattern = ".*";
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});
    }

    @Test
    void testGetByWildcard() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.WILDCARD;
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});

        selection.m_pattern = "*";
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);
    }

    @Test
    void testGetByInvertedWildcard() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.WILDCARD;
        selection.m_isInverted = true;
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);

        selection.m_pattern = "*";
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});
    }

    @Test
    void testGetByCaseSensitiveWildcard() {
        final var selection = new ColumnSelection(CONTEXT);
        selection.m_mode = ColumnSelectionMode.WILDCARD;
        selection.m_pattern = COL_SPEC.getName().toUpperCase();
        final var choices = new String[]{COL_SPEC.getName()};
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(choices);

        selection.m_isCaseSensitive = true;
        assertThat(ColumnSelection.get(selection, choices, TABLE_SPEC)).isEqualTo(new String[]{});
    }

}
