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
 *   Sep 04, 2022 (Paul Bärnreuther): created
 */

package org.knime.core.webui.node.view.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.node.dialog.defaultdialog.setting.choices.column.multiple.ColumnFilter;
import org.knime.core.webui.node.view.table.data.Renderer;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;
import org.knime.testing.util.TableTestUtil;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class TableViewInitialDataTest {

    private final int numRows = 10;

    private final String nodeId = "NodeID";

    private final BufferedDataTable table = TableTestUtil.createDefaultTestTable(numRows).get();

    private final Supplier<Set<RowKey>> selectionSupplier = () -> Collections.emptySet();

    private final String[] initiallySelectedColumns = new String[]{"double", "string", "date"};

    private final String[] initiallySelectedColumnsInCorrectOrder = new String[]{"date", "string", "double"};

    private final ColumnFilter displayedColumns = new ColumnFilter(initiallySelectedColumns);

    private MockedConstruction<TableViewDataServiceImpl> dataServiceMock;

    @BeforeEach
    public void beginTest() {
        dataServiceMock = Mockito.mockConstruction(TableViewDataServiceImpl.class);
    }

    @AfterEach
    public void endTest() {
        dataServiceMock.close();
    }

    @Test
    void testGetTableWithPagination() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_enablePagination = true;
        settings.m_pageSize = 8;
        final var initialData = TableViewUtil.createInitialData(settings, table, selectionSupplier, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(initiallySelectedColumnsInCorrectOrder), eq(0L),
            eq(settings.m_pageSize), any(String[].class), eq(true), eq(true), eq(false), eq(false));
    }

    @Test
    void testGetTableWithoutPagination() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_enablePagination = false;
        final var initialData = TableViewUtil.createInitialData(settings, table, selectionSupplier, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(initiallySelectedColumnsInCorrectOrder), eq(0L),
            eq(0), any(String[].class), eq(true), eq(true), eq(false), eq(false));
    }

    @Test
    void testGetTableWithSkipRemainingColumns() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_skipRemainingColumns = true;
        final var initialData = TableViewUtil.createInitialData(settings, table, selectionSupplier, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(initiallySelectedColumnsInCorrectOrder), eq(0L),
            any(Integer.class), any(String[].class), eq(true), eq(true), eq(true), eq(false));
    }

    @Test
    void testGetTableWithShowOnlySelectedRows() {
        final var settings = new TableViewViewSettings(table.getSpec());
        settings.m_displayedColumns = displayedColumns;
        settings.m_showOnlySelectedRows = true;
        final var initialData = TableViewUtil.createInitialData(settings, table, selectionSupplier, nodeId);
        initialData.getTable();
        verify(dataServiceMock.constructed().get(0)).getTable(aryEq(initiallySelectedColumnsInCorrectOrder), eq(0L),
            any(Integer.class), any(String[].class), eq(true), eq(true), eq(false), eq(true));
    }

    @Test
    void testGetSettings() {
        final var settings = new TableViewViewSettings(table.getSpec());
        final var initialData = TableViewUtil.createInitialData(settings, table, selectionSupplier, nodeId);
        assertThat(initialData.getSettings()).isEqualTo(initialData.getSettings());
    }

    @Test
    void testInitialDataGetDataTypes() {
        final var initData = TableViewUtil.createInitialData(new TableViewViewSettings(table.getSpec()), table,
            selectionSupplier, nodeId);
        var dataTypes = initData.getDataTypes();

        var stringType = dataTypes.get(String.valueOf(System.identityHashCode(StringCell.TYPE)));
        assertThat(stringType.getName()).isEqualTo(StringCell.TYPE.getName());
        assertRendererNames(stringType.getRenderers(), "Multi-line String", "String");
        assertThat(stringType.getHasDataValueView()).isTrue();

        var doubleType = dataTypes.get(String.valueOf(System.identityHashCode(DoubleCell.TYPE)));
        assertThat(doubleType.getName()).isEqualTo(DoubleCell.TYPE.getName());
        assertRendererNames(doubleType.getRenderers(), "Standard Double", "Percentage", "Full Precision", "Gray Scale",
            "Bars", "Default");
        assertThat(doubleType.getHasDataValueView()).isFalse();

        var booleanType = dataTypes.get(String.valueOf(System.identityHashCode(BooleanCell.TYPE)));
        assertThat(booleanType.getName()).isEqualTo(BooleanCell.TYPE.getName());
        assertRendererNames(booleanType.getRenderers(), "Boolean", "Integer", "Standard Double", "Percentage",
            "Full Precision", "Gray Scale", "Bars", "Default");
        assertThat(booleanType.getHasDataValueView()).isFalse();
    }

    private static void assertRendererNames(final Renderer[] renderers, final String... expectedRendererNames) {
        assertThat(Arrays.stream(renderers).map(Renderer::getName).toArray(String[]::new))
            .isEqualTo(expectedRendererNames);
        assertThat(renderers[0].getId()).isNotNull();
    }

}
