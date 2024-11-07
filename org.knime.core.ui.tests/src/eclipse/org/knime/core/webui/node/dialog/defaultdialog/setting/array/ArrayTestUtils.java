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
 *   Nov 10, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.array;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.assertj.core.util.Arrays;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.mockito.Mockito;

/**
 *
 * @author Paul Bärnreuther
 */
public class ArrayTestUtils {

    private ArrayTestUtils() {
        // static utility class
    }

    /**
     * To be used to test the dynamic behavior of the array setting.
     *
     * @param n the index at which the associated input table would be connected
     * @param columnTypes the types of the columns in the table spec
     * @return a context with a table spec at that index. Column names are "column_i" where i is its index.
     */
    public static DefaultNodeSettingsContext createContextWithNthTableSpec(final int n, final DataType... columnTypes) {

        final var spec = createSpecWithDataValues(columnTypes);
        return createContextWithNthTableSpec(n, spec);
    }

    private static DataTableSpec createSpecWithDataValues(final DataType[] columnTypes) {
        return new DataTableSpecCreator().addColumns(//
            IntStream.range(0, columnTypes.length)
                .mapToObj(i -> new DataColumnSpecCreator("column_" + i, columnTypes[i]).createSpec())
                .toArray(DataColumnSpec[]::new) //
        ).createSpec();
    }

    /**
     * To be used to test the static behavior of the array setting.
     *
     * @param n the index at which the associated input table would be connected
     * @return a context without a table spec
     */
    public static DefaultNodeSettingsContext createContextWithoutNthTableSpec(final int n) {
        return createContextWithNthTableSpec(n, (DataTableSpec)null);
    }

    private static DefaultNodeSettingsContext createContextWithNthTableSpec(final int n, final DataTableSpec spec) {
        final var context = Mockito.mock(DefaultNodeSettingsContext.class);
        Mockito.when(context.getPortObjectSpec(n)).thenReturn(Optional.ofNullable(spec));
        Mockito.when(context.getDataTableSpec(n)).thenReturn(Optional.ofNullable(spec));
        return context;
    }

    /**
     * Setter for the values of an array setting.
     *
     * @param array
     * @param elementSettings
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends DefaultNodeSettings> void setValues(final Array<T> array, final T... elementSettings) {
        array.m_values = (List<T>)Arrays.asList(elementSettings);
    }

    /**
     * Setter for the columns of an array setting.
     *
     * @param array
     * @param columns
     */
    public static <T extends DefaultNodeSettings> void setColumns(final Array<T> array,
        final Map<String, String> columns) {
        array.m_columns = columns;
    }

}
