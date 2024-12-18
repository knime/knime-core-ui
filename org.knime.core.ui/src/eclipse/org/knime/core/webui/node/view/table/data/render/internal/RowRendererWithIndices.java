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
 *   Aug 4, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.view.table.data.render.internal;

import java.util.LinkedList;
import java.util.stream.IntStream;

import org.knime.core.data.DataRow;

/**
 * Decorator row renderer which renders the index of the row as a first element.
 *
 * @author Paul Bärnreuther
 * @param <R> output type
 */
public final class RowRendererWithIndices<R> extends RowRendererDecorator<R> {

    private final IndexExtractor m_indexExtractor;

    /**
     * @param delegate to extend
     * @param indexExtractor that extracts the index from a row
     */
    public RowRendererWithIndices(final RowRenderer<R> delegate, final IndexExtractor indexExtractor) {
        super(delegate);
        m_indexExtractor = indexExtractor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LinkedList<R> renderRow(final DataRow row, final long rowIndex) {
        final var linkedList = m_delegate.renderRow(row, rowIndex);
        // + 1 because indices should start at 1 in the data
        linkedList.add(0, (R)Long.toString(m_indexExtractor.extractIndex(row, rowIndex) + 1));
        return linkedList;
    }

    @Override
    public int[] getMaterializedColumnIndices() {
        return IntStream.concat(IntStream.of(m_indexExtractor.getMaterializedColumnIndices()),
            IntStream.of(m_delegate.getMaterializedColumnIndices())).toArray();
    }

}
