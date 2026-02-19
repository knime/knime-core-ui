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
 *   Feb 20, 2026 (gerling): created
 */
package org.knime.core.webui.node.view.tile.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.view.table.data.Cell;
import org.knime.core.webui.node.view.table.data.Table;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.node.view.tile.TileViewViewParameters.RowIDOrNoneChoice;

/**
 * Wrapper that projects {@link TableViewDataService} responses to {@link TileTable}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public final class TileViewDataServiceImpl implements TileViewDataService {

    private final TableViewDataService m_delegate;

    /**
     * Creates a new instance of {@link TileViewDataServiceImpl}.
     *
     * @param delegate the wrapped {@link TableViewDataService} to delegate calls to
     */
    public TileViewDataServiceImpl(final TableViewDataService delegate) {
        m_delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public TileTable getTable(final String[] columns, final StringOrEnum<RowIDOrNoneChoice> titleColumn,
        final StringOrEnum<NoneChoice> colorColumn, final long fromIndex, final int numRows,
        final boolean forceClearImageDataCache, final boolean showOnlySelectedRows) {

        final var requestedColumns = new ArrayList<>(Arrays.asList(columns));

        final var resolvedTitleColumn = resolveStringOrEnumColumn(titleColumn);
        final var addedTitleColumn =
            resolvedTitleColumn != null && shouldAddColumn(requestedColumns, resolvedTitleColumn);
        if (addedTitleColumn) {
            requestedColumns.add(resolvedTitleColumn);
        }

        final var resolvedColorColumn = resolveStringOrEnumColumn(colorColumn);
        final var addedColorColumn =
            resolvedColorColumn != null && shouldAddColumn(requestedColumns, resolvedColorColumn);
        if (addedColorColumn) {
            requestedColumns.add(resolvedColorColumn);
        }
        // We always update the displayed columns (aka filter) because the title or color column might be missing
        final var table = m_delegate.getTable(requestedColumns.toArray(String[]::new), fromIndex, numRows,
            new String[requestedColumns.size()], true, forceClearImageDataCache, false, showOnlySelectedRows);
        return toTileTable(table, addedTitleColumn, titleColumn, addedColorColumn, colorColumn);
    }

    @Override
    public void clearCache() {
        m_delegate.clearCache();
    }

    private static TileTable toTileTable(final Table table, final boolean addedTitleColumn,
        final StringOrEnum<RowIDOrNoneChoice> titleColumn, final boolean addedColorColumn,
        final StringOrEnum<NoneChoice> resolvedColorColumn) {
        var displayedColumns = table.getDisplayedColumns();
        var contentTypes = table.getColumnContentTypes();
        var rows = table.getRows();

        final String[] rowColors;
        final var rowColorColInd =
            extractedRowColorColumnIndexInDisplayedColumns(displayedColumns, resolvedColorColumn);
        if (rowColorColInd == -1) {
            rowColors = null;
        } else {
            rowColors = extractRowColors(rows, rowColorColInd + NUM_ROWS_BEFORE_DISPLAYED_COLS);
            if (addedColorColumn) {
                contentTypes = removeElementAt(contentTypes, rowColorColInd);
                displayedColumns = removeElementAt(displayedColumns, rowColorColInd);
                rows = removeElementAt(rows, rowColorColInd + NUM_ROWS_BEFORE_DISPLAYED_COLS);
            }
        }

        final Object[] rowTitles;
        final var rowTitleColInd = extractRowTitleColumnIndexInRow(displayedColumns, titleColumn);
        if (rowTitleColInd == -1) {
            rowTitles = null;
        } else {
            rowTitles = extractRowTitles(rows, rowTitleColInd);
            if (addedTitleColumn) {
                contentTypes = removeElementAt(contentTypes, rowTitleColInd - NUM_ROWS_BEFORE_DISPLAYED_COLS);
                displayedColumns = removeElementAt(displayedColumns, rowTitleColInd - NUM_ROWS_BEFORE_DISPLAYED_COLS);
                rows = removeElementAt(rows, rowTitleColInd);
            }
        }

        final var finalDisplayedColumns = displayedColumns;
        final var finalContentTypes = contentTypes;
        final var finalRows = rows;

        return new TileTable() {

            @Override
            public String[] getDisplayedColumns() {
                return finalDisplayedColumns;
            }

            @Override
            public String[] getColumnContentTypes() {
                return finalContentTypes;
            }

            @Override
            public List<List<Object>> getRows() {
                return finalRows;
            }

            @Override
            public Object[] getRowTitles() {
                return rowTitles;
            }

            @Override
            public String[] getRowColors() {
                return rowColors;
            }

            @Override
            public long getRowCount() {
                return table.getRowCount();
            }

            @Override
            public Long getTotalSelected() {
                return table.getTotalSelected();
            }

        };
    }

    private static boolean shouldAddColumn(final List<String> columns, final String column) {
        return column != null && !columns.contains(column);
    }

    private static <E extends Enum<E>> String resolveStringOrEnumColumn(final StringOrEnum<E> colorColumn) {
        if (colorColumn == null || colorColumn.getEnumChoice().isPresent()) {
            return null;
        }
        return colorColumn.getStringChoice();
    }

    private static String[] removeElementAt(final String[] values, final int index) {
        return IntStream.range(0, values.length) //
            .filter(i -> i != index) //
            .mapToObj(i -> values[i]) //
            .toArray(String[]::new);
    }

    private static List<List<Object>> removeElementAt(final List<List<Object>> rows, final int index) {
        return rows.stream() //
            .map(row -> IntStream.range(0, row.size()) //
                .filter(i -> i != index) //
                .mapToObj(row::get) //
                .toList())
            .toList();

    }

    private static final int ROW_ID_COLUMN_INDEX = 1;

    private static final int NUM_ROWS_BEFORE_DISPLAYED_COLS = 2;

    private static int extractedRowColorColumnIndexInDisplayedColumns(final String[] displayedColumns,
        final StringOrEnum<NoneChoice> colorColumn) {
        if (colorColumn.getEnumChoice().isPresent()) {
            return -1;
        }
        return Arrays.asList(displayedColumns).indexOf(colorColumn.getStringChoice());
    }

    private static int extractRowTitleColumnIndexInRow(final String[] displayedColumns,
        final StringOrEnum<RowIDOrNoneChoice> titleColumn) {
        final var titleColumnOpt = titleColumn.getEnumChoice();
        if (titleColumnOpt.isPresent()) {
            if (titleColumnOpt.get() == RowIDOrNoneChoice.NONE) {
                return -1;
            }
            return ROW_ID_COLUMN_INDEX;
        }
        final var titleColIndex = Arrays.asList(displayedColumns).indexOf(titleColumn.getStringChoice());
        if (titleColIndex == -1) {
            return -1;
        }
        return titleColIndex + NUM_ROWS_BEFORE_DISPLAYED_COLS;
    }

    private static Object[] extractRowTitles(final List<List<Object>> rows, final int titleColumnIndex) {
        return rows.stream().map(row -> row.get(titleColumnIndex)).toArray(Object[]::new);
    }

    private static String[] extractRowColors(final List<List<Object>> rows, final int colorColumnIndex) {
        return rows.stream().map(row -> {
            final Object colorCell = row.get(colorColumnIndex);
            return colorCell instanceof Cell cell ? cell.getColor() : null;
        }).toArray(String[]::new);
    }
}
