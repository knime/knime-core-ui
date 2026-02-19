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
 *   Feb 19, 2026 (rg): created
 */
package org.knime.core.webui.node.view.table.data;

import java.io.IOException;
import java.util.Objects;

/**
 * Wrapper that projects {@link TableViewDataService} responses to {@link TileTable}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public final class TileViewDataServiceImpl implements TileViewDataService {

    private final TableViewDataService m_delegate;

    public TileViewDataServiceImpl(final TableViewDataService delegate) {
        m_delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public TileTable getTable(final String[] columns, final long fromIndex, final int numRows,
        final String[] rendererIds, final boolean updateDisplayedColumns, final boolean forceClearImageDataCache,
        final boolean trimColumns, final boolean showOnlySelectedRows) {
        return toTileTable(m_delegate.getTable(columns, fromIndex, numRows, rendererIds, updateDisplayedColumns,
            forceClearImageDataCache, trimColumns, showOnlySelectedRows));
    }

    @Override
    @SuppressWarnings("java:S107")
    public TileTable getFilteredAndSortedTable(final String[] columns, final long fromIndex, final int numRows,
        final String sortColumn, final boolean sortAscending, final String globalSearchTerm,
        final String[][] columnFilterValue, final boolean filterRowKeys, final String[] rendererIds,
        final boolean updateDisplayedColumns, final boolean updateTotalSelected, final boolean forceClearImageDataCache,
        final boolean trimColumns, final boolean showOnlySelectedRows) {

        return toTileTable(m_delegate.getFilteredAndSortedTable(columns, fromIndex, numRows, sortColumn, sortAscending,
            globalSearchTerm, columnFilterValue, filterRowKeys, rendererIds, updateDisplayedColumns,
            updateTotalSelected, forceClearImageDataCache, trimColumns, showOnlySelectedRows));
    }

    @Override
    public TableViewDataService.HTMLAndCSV getCopyContent(final TableViewDataService.SpecialColumnConfig rowIndexConfig,
        final TableViewDataService.SpecialColumnConfig rowKeyConfig, final boolean withHeaders,
        final String[] dataColumns, final int fromIndex, final int toIndex) throws IOException {
        return m_delegate.getCopyContent(rowIndexConfig, rowKeyConfig, withHeaders, dataColumns, fromIndex, toIndex);
    }

    @Override
    public String[] getCurrentRowKeys() {
        return m_delegate.getCurrentRowKeys();
    }

    @Override
    public Long getTotalSelected() {
        return m_delegate.getTotalSelected();
    }

    @Override
    public void clearCache() {
        m_delegate.clearCache();
    }

    private static TileTable toTileTable(final Table table) {
        return new TileTable() {

            @Override
            public String[] getDisplayedColumns() {
                return table.getDisplayedColumns();
            }

            @Override
            public String[] getColumnContentTypes() {
                return table.getColumnContentTypes();
            }

            @Override
            public java.util.List<java.util.List<Object>> getRows() {
                return table.getRows();
            }

            @Override
            public long[] getRowIndices() {
                return table.getRowIndices();
            }

            @Override
            public long getRowCount() {
                return table.getRowCount();
            }

            @Override
            public long getColumnCount() {
                return table.getColumnCount();
            }

            @Override
            public Long getTotalSelected() {
                return table.getTotalSelected();
            }

        };
    }
}