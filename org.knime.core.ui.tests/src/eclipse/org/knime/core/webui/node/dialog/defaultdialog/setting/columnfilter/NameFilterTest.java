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
package org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class NameFilterTest {

    @Test
    void testGetSelectedByManualWithIncludeUnknownColumns() {
        final var selection = new NameFilter(new String[]{"Old selected"});
        selection.m_manualFilter.m_manuallyDeselected = new String[]{"Old deselected"};
        selection.m_manualFilter.m_includeUnknownColumns = true;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{"Old selected", choices[0], choices[1]});
    }

    @Test
    void testGetSelectedByManualOnlyIncludeNewColumnsIfUnknown() {
        final var selection = new NameFilter(new String[]{"Old selected"});
        final var choices = new String[]{"Choice1", "Choice2"};
        selection.m_manualFilter.m_manuallyDeselected = new String[]{choices[0]};
        selection.m_manualFilter.m_includeUnknownColumns = true;
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{"Old selected", choices[1]});
        assertThat(selection.getNonMissingSelected(choices)).isEqualTo(new String[]{choices[1]});
    }

    @Test
    void testGetSelectedByManualWithExcludedUnknownColumns() {
        final var selection = new NameFilter(new String[]{"Old selected"});
        selection.m_manualFilter.m_manuallyDeselected = new String[]{"Old deselected"};
        selection.m_manualFilter.m_includeUnknownColumns = false;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{"Old selected"});
    }

    @Test
    void testGetSelectedByRegex() {
        final var selection = new NameFilter();
        selection.m_mode = NameFilterMode.REGEX;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{});

        selection.m_patternFilter.m_pattern = ".*";
        assertThat(selection.getSelected(choices)).isEqualTo(choices);
        assertThat(selection.getNonMissingSelected(choices)).isEqualTo(choices);
    }

    @Test
    void testGetSelectedByInvertedRegex() {
        final var selection = new NameFilter();
        selection.m_mode = NameFilterMode.REGEX;
        selection.m_patternFilter.m_isInverted = true;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(choices);

        selection.m_patternFilter.m_pattern = ".*";
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{});
    }

    @Test
    void testGetSelectedByWildcard() {
        final var selection = new NameFilter();
        selection.m_mode = NameFilterMode.WILDCARD;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{});

        selection.m_patternFilter.m_pattern = "*";
        assertThat(selection.getSelected(choices)).isEqualTo(choices);
    }

    @Test
    void testGetSelectedByInvertedWildcard() {
        final var selection = new NameFilter();
        selection.m_mode = NameFilterMode.WILDCARD;
        selection.m_patternFilter.m_isInverted = true;
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(choices);

        selection.m_patternFilter.m_pattern = "*";
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{});
    }

    @Test
    void testGetSelectedByCaseSensitiveWildcard() {
        final var selection = new NameFilter();
        selection.m_mode = NameFilterMode.WILDCARD;
        selection.m_patternFilter.m_pattern = "Choice*".toUpperCase();
        final var choices = new String[]{"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(choices);

        selection.m_patternFilter.m_isCaseSensitive = true;
        assertThat(selection.getSelected(choices)).isEqualTo(new String[]{});
    }

    /**
     * Tests that an initially empty selection works.
     */
    @Test
    void testInitialSelectedEmpty() {
        final var empty = new String[0];
        final var selection = new NameFilter(empty);
        final String[] choices = {"Choice1", "Choice2"};
        assertThat(selection.getSelected(choices)).isEqualTo(empty);
    }

    /**
     * Tests that an initially null selection does not work.
     */
    @Test
    void testInitialSelectedNullThrows() {
        final String[] manuallySelected = null; // avoid ambiguous constructor call
        assertThrows(NullPointerException.class, () -> new NameFilter(manuallySelected));
    }

}
