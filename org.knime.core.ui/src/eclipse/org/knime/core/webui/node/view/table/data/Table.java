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
 *   Jul 15, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data;

import java.util.List;
import java.util.Map;

import org.knime.core.webui.node.view.table.data.render.DataValueImageRenderer.ImageDimension;

/**
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface Table {

    /**
     * @return the displayed columns which remain after missing columns are filtered out.
     */
    String[] getDisplayedColumns();

    /**
     * @return the content type per column (which depends on the selected renderer per column)
     */
    String[] getColumnContentTypes();

    /**
     * @return the data type ids per column; can be used to access the actual data type via
     *         {@link TableViewInitialData#getDataTypes()}
     */
    String[] getColumnDataTypeIds();

    /**
     * @return the description of the formatters attached to the columns or null where none is attached.
     */
    String[] getColumnFormatterDescriptions();

    /**
     * @return the color for each displayed column or an empty array when there is no color handler specified.
     */
    String[] getColumnNamesColors();

    /**
     * @return the requested rows; contains {@code String}s for existing values and can contain {@code null}s or
     *         {@code Cell}s in case of missing values
     */
    List<List<Object>> getRows();

    /**
     * @return the indices of the rows in {@link #getRows} within the original table (prior to windowing, filtering and
     *         sorting).
     */
    long[] getRowIndices();

    /**
     * @return the row count of the table in use
     */
    long getRowCount();

    /**
     * @return the number of valid selected columns of the table in use. These can be possibly more than the displayed
     *         ones if the columns are trimmed.
     */
    long getColumnCount();

    /**
     * @return the number of selected rows of the table in use
     */
    Long getTotalSelected();

    /**
     * @return the column sizes of image columns
     */
    Map<String, ImageDimension> getFirstRowImageDimensions();

}
