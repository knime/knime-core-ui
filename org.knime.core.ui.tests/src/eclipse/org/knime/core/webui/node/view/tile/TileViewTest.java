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
 *   Feb 27, 2026 (Robin Gerling): created
 */
package org.knime.core.webui.node.view.tile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.testing.util.TableTestUtil.createDefaultTestTable;
import static org.knime.testing.util.TableTestUtil.getExec;

import java.awt.Color;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.data.DataServiceContextTest;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.table.data.render.SwingBasedRendererFactory;
import org.knime.core.webui.node.view.tile.TileViewViewParameters.RowIDOrNoneChoice;
import org.knime.core.webui.node.view.tile.data.TileViewDataServiceImpl;
import org.knime.core.webui.node.view.tile.data.TileViewInitialDataImpl;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.TableTestUtil.ObjectColumn;

/**
 * Tests for the tile view backend, covering {@link TileViewDataServiceImpl} and {@link TileViewInitialDataImpl}.
 * General table functionality (sorting, filtering, etc.) is already covered by the TableViewTest and is not repeated
 * here.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
final class TileViewTest {

    private static final int NUM_DEFAULT_INCLUDED_COLUMNS = 2;

    @BeforeEach
    void initDataServiceContext() {
        DataServiceContextTest.initDataServiceContext(() -> getExec(), null);
    }

    @AfterEach
    void removeDataServiceContext() {
        DataServiceContextTest.removeDataServiceContext();
    }

    @Nested
    class TileViewDataServiceImplTest {

        @Test
        void testGetTableNoTitleNoColor() {
            final var table = createDefaultTestTable(3);
            final var dataService = createTileDataService(table);
            final var columns = new String[]{"string", "long"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>(RowIDOrNoneChoice.NONE), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, 3, false, false);

            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRowTitles()).isNull();
            assertThat(result.getRowColors()).isNull();
            assertThat(result.getRowCount()).isEqualTo(3);
            assertThat(result.getRows()).hasSize(3);
        }

        @Test
        void testGetTableRowTitlesFromRowId() {
            final var numRows = 2;
            final var table = createDefaultTestTable(numRows);
            final var dataService = createTileDataService(table);
            final var columns = new String[]{"string"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>(RowIDOrNoneChoice.ROW_ID), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, numRows, false, false);

            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowTitles()).containsExactly("rowkey 0", "rowkey 1");
        }

        @Test
        void testGetTableRowTitlesFromDisplayedColumn() {
            final var table = createDefaultTestTable(2);
            final var dataService = createTileDataService(table);
            // "string" column is already included in displayed columns
            final var columns = new String[]{"string", "long"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>("string"), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, 2, false, false);

            // "string" column remains in displayed columns (was not added extra)
            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowTitles()).containsExactly("0", "1");
        }

        @Test
        void testGetTableRowTitlesFromAdditionalColumn() {
            final var table = createDefaultTestTable(2);
            final var dataService = createTileDataService(table);
            // "string" is NOT in the displayed columns but used as title column
            final var columns = new String[]{"long"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>("string"), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, 2, false, false);

            // "string" must have been fetched for titles but not appear in displayed columns
            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowTitles()).containsExactly("0", "1");
        }

        @Test
        void testGetTableRowColorsFromDisplayedColumn() {
            final var colorColumnName = "colorCol";
            final var table = createTableWithColorColumn(colorColumnName);
            final var dataService = createTileDataService(table);
            // color column is already included in displayed columns
            final var columns = new String[]{"valueCol", colorColumnName};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>(RowIDOrNoneChoice.NONE), //
                new StringOrEnum<>(colorColumnName), //
                0, 2, false, false);

            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowColors()).hasSize(2);
            assertThat(result.getRowColors()[0]).isEqualTo("#00FF00");
            assertThat(result.getRowColors()[1]).isEqualTo("#FF0000");
        }

        @Test
        void testGetTableRowColorsFromAdditionalColumn() {
            final var colorColumnName = "colorCol";
            final var table = createTableWithColorColumn(colorColumnName);
            final var dataService = createTileDataService(table);
            // color column is NOT in displayed columns
            final var columns = new String[]{"valueCol"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>(RowIDOrNoneChoice.NONE), //
                new StringOrEnum<>(colorColumnName), //
                0, 2, false, false);

            // color column fetched for colors but not in displayed columns
            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowColors()).hasSize(2);
            assertThat(result.getRowColors()[0]).isEqualTo("#00FF00");
            assertThat(result.getRowColors()[1]).isEqualTo("#FF0000");
        }

        @Test
        void testGetTableNoColorsWhenNone() {
            final var table = createDefaultTestTable(2);
            final var dataService = createTileDataService(table);

            final var result = dataService.getTable(new String[]{"string"}, //
                new StringOrEnum<>(RowIDOrNoneChoice.NONE), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, 2, false, false);

            assertThat(result.getRowColors()).isNull();        }

        @Test
        void testGetTableBothTitleAndColorColumnsAdded() {
            final var colorColumnName = "colorCol";
            final var table = createTableWithColorColumn(colorColumnName);
            final var dataService = createTileDataService(table);
            // request only "valueCol"; both title AND color columns are extra
            final var columns = new String[]{"valueCol"};

            final var result = dataService.getTable(columns, //
                new StringOrEnum<>(colorColumnName), // title column = colorCol (also used as color → shares the extra col)
                new StringOrEnum<>(colorColumnName), //
                0, 2, false, false);

            // only one extra column was added (same column for both title and color)
            assertThat(result.getDisplayedColumns()).isEqualTo(columns);
            assertThat(result.getRows().get(0)).hasSize(columns.length + NUM_DEFAULT_INCLUDED_COLUMNS);
            assertThat(result.getRowTitles()).hasSize(2);
            assertThat(result.getRowColors()).hasSize(2);
        }

        @Test
        void testGetTableBothTitleAndColorColumnsAddedSeparately() {
            // title="string" and color="colorCol" are both NOT in the displayed columns → both are
            // fetched as extra columns, then removed from the displayed result
            final var colorColumnName = "colorCol";
            final var colorModel = new ColorModelNominal(//
                Map.<DataCell, ColorAttr> of(//
                    new StringCell("titleA"), ColorAttr.getInstance(new Color(0, 255, 0)), //
                    new StringCell("titleB"), ColorAttr.getInstance(new Color(255, 0, 0))), //
                new ColorAttr[0]);
            final var table = TableTestUtil.createTableFromColumns( //
                new ObjectColumn("value", DoubleCell.TYPE, new Double[]{1.0, 2.0}), //
                new ObjectColumn("titleCol", StringCell.TYPE, new String[]{"titleA", "titleB"}), //
                new ObjectColumn(colorColumnName, StringCell.TYPE, new ColorHandler(colorModel),
                    new String[]{"titleA", "titleB"}));
            final var dataService = createTileDataService(() -> table);

            final var result = dataService.getTable(new String[]{"value"}, //
                new StringOrEnum<>("titleCol"), //
                new StringOrEnum<>(colorColumnName), //
                0, 2, false, false);

            assertThat(result.getDisplayedColumns()).isEqualTo(new String[]{"value"});
            assertThat(result.getRowTitles()).containsExactly("titleA", "titleB");
            assertThat(result.getRowColors()).containsExactly("#00FF00", "#FF0000");
        }

        @Test
        void testGetTableRowCountIsPassedThrough() {
            final var numRows = 5;
            final var table = createDefaultTestTable(numRows);
            final var dataService = createTileDataService(table);

            final var result = dataService.getTable(new String[]{"string"}, //
                new StringOrEnum<>(RowIDOrNoneChoice.NONE), //
                new StringOrEnum<>(NoneChoice.NONE), //
                0, numRows, false, false);

            assertThat(result.getRowCount()).isEqualTo(numRows);
        }

        private static TileViewDataServiceImpl createTileDataService(final Supplier<BufferedDataTable> tableSupplier) {
            final var delegate = new TableViewDataServiceImpl(tableSupplier, "tableId", new SwingBasedRendererFactory(),
                new DataValueImageRendererRegistry(() -> "pageId"));
            return new TileViewDataServiceImpl(delegate);
        }

        /**
         * Creates a 2-row table with a plain "valueCol" and a "colorCol" that has a nominal color handler with "value0"
         * → green and "value1" → red.
         */
        private static Supplier<BufferedDataTable> createTableWithColorColumn(final String colorColumnName) {
            final var colorModel = new ColorModelNominal(//
                Map.<DataCell, ColorAttr> of(//
                    new StringCell("value0"), ColorAttr.getInstance(new Color(0, 255, 0)), //
                    new StringCell("value1"), ColorAttr.getInstance(new Color(255, 0, 0))), //
                new ColorAttr[0]);
            final var colorHandler = new ColorHandler(colorModel);

            return () -> TableTestUtil.createTableFromColumns( //
                new ObjectColumn("valueCol", DoubleCell.TYPE, new Double[]{1.0, 2.0}), //
                new ObjectColumn(colorColumnName, StringCell.TYPE, colorHandler, new String[]{"value0", "value1"}));
        }
    }

    // -- TileViewInitialDataImpl --

    @Nested
    class TileViewInitialDataImplTest {

        @Test
        void testGetTableUsesPageSize() {
            final var numRows = 10;
            final var table = createDefaultTestTable(numRows);
            final var spec = table.get().getDataTableSpec();
            final var settings = new TileViewViewParameters(spec);
            settings.m_pageSize = 3;
            settings.m_titleColumn = new StringOrEnum<>(RowIDOrNoneChoice.ROW_ID);
            settings.m_colorColumn = new StringOrEnum<>(NoneChoice.NONE);

            final var initialData = new TileViewInitialDataImpl(settings, table, createTileDataService(table));

            final var result = initialData.getTable();

            assertThat(result.getRows()).hasSize(3);
            assertThat(result.getRowCount()).isEqualTo(numRows);
        }

        @Test
        void testGetTableUsesDisplayedColumns() {
            final var table = createDefaultTestTable(2);
            final var spec = table.get().getDataTableSpec();
            final var settings = new TileViewViewParameters(spec);
            settings.m_titleColumn = new StringOrEnum<>(RowIDOrNoneChoice.NONE);
            settings.m_colorColumn = new StringOrEnum<>(NoneChoice.NONE);
            // by default all columns are selected; verify all columns appear in the result
            final var expectedColumns = settings.getDisplayedColumns(spec);

            final var initialData = new TileViewInitialDataImpl(settings, table, createTileDataService(table));

            final var result = initialData.getTable();

            assertThat(result.getDisplayedColumns()).isEqualTo(expectedColumns);
        }

        @Test
        void testGetTableTitlesFromRowId() {
            final var table = createDefaultTestTable(2);
            final var spec = table.get().getDataTableSpec();
            final var settings = new TileViewViewParameters(spec);
            settings.m_pageSize = 2;
            settings.m_titleColumn = new StringOrEnum<>(RowIDOrNoneChoice.ROW_ID);
            settings.m_colorColumn = new StringOrEnum<>(NoneChoice.NONE);

            final var initialData = new TileViewInitialDataImpl(settings, table, createTileDataService(table));

            final var result = initialData.getTable();

            assertThat(result.getRowTitles()).containsExactly("rowkey 0", "rowkey 1");
        }

        private static TileViewDataServiceImpl createTileDataService(final Supplier<BufferedDataTable> tableSupplier) {
            final var delegate = new TableViewDataServiceImpl(tableSupplier, "tableId", new SwingBasedRendererFactory(),
                new DataValueImageRendererRegistry(() -> "pageId"));
            return new TileViewDataServiceImpl(delegate);
        }
    }
}
