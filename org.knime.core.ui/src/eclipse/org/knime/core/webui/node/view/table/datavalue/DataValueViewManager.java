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
 *   Sep 16, 2024 (hornm): created
 */
package org.knime.core.webui.node.view.table.datavalue;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.DataValueWrapper;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.node.PageResourceManager.PageType;

/**
 * TODO
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class DataValueViewManager {

    private static DataValueViewManager instance;

    private final Map<Class<? extends DataValue>, DataValueViewFactory<? extends DataValue>> m_dataValueViewFactories =
        new HashMap<>();

    private final Map<DataValueWrapper, DataValueView> m_dataValueViewMap = new WeakHashMap<>();

    private final PageResourceManager<DataValueWrapper> m_pageResourceManager =
        new PageResourceManager<>(PageType.DATA_VALUE, dvw -> getDataValueView(dvw).getPage(), null, null, true);

    private final DataServiceManager<DataValueWrapper> m_dataServiceManager =
            new DataServiceManager<>(dvw -> getDataValueView(dvw));


    public static synchronized DataValueViewManager getInstance() {
        if (instance == null) {
            instance = new DataValueViewManager();
        }
        return instance;
    }

    /**
     * TODO
     */
    public static <V extends DataValue> void registerDataValueViewFactory(final Class<V> dataValueClass,
        final DataValueViewFactory<V> factory) {
        getInstance().m_dataValueViewFactories.put(dataValueClass, factory);
    }

    private DataValueViewManager() {
        // singleton
    }

    public DataValueView getDataValueView(final DataValueWrapper wrapper) {
        var dataValueView = m_dataValueViewMap.get(wrapper); // NOSONAR
        if (dataValueView != null) {
            return dataValueView;
        }

        var nc = wrapper.get();
        var portIdx = wrapper.getPortIdx();
        if (portIdx < 0 || portIdx >= nc.getNrOutPorts()) {
            throw new NoSuchElementException("No port at index " + portIdx);
        }
        var outPort = nc.getOutPort(portIdx);
        if (!outPort.getPortType().equals(BufferedDataTable.TYPE)) {
            throw new NoSuchElementException("No data table at index " + portIdx);
        }
        var table = (BufferedDataTable)outPort.getPortObject();
        if (table == null) {
            throw new NoSuchElementException("No data table available at index " + portIdx);
        }
        var dataCell = getDataCellAt(wrapper.getRowIdx(), wrapper.getColIdx(), table);
        var chosenValue = findCompatibleValue(dataCell);
        if (!chosenValue.isEmpty()) {
            @SuppressWarnings("rawtypes")
            DataValueViewFactory factory = m_dataValueViewFactories.get(chosenValue);
            dataValueView = factory.createDataValueViews(dataCell)[wrapper.getViewIdx()];
            m_dataValueViewMap.put(wrapper, dataValueView);
            return dataValueView;
        } else {
            throw new NoSuchElementException("TODO");
        }
    }

    private java.util.Optional<Class<? extends DataValue>> findCompatibleValue(final DataCell cell) {
        return cell.getType().getValueClasses().stream().filter(value -> m_dataValueViewFactories.containsKey(value)).findFirst();

    }

    private static DataCell getDataCellAt(final int rowIdx, final int colIdx, final BufferedDataTable table) {
        DataRow row = null;
        try (var it = table.iterator()) {
            row = it.next();
            for (int i = 0; i < rowIdx; i++) {
                row = it.next();
            }
        }
        return row.getCell(colIdx);
    }

    /**
     * @return the {@link PageResourceManager} instance
     */
    public PageResourceManager<DataValueWrapper> getPageResourceManager() {
        return m_pageResourceManager;
    }

    /**
     * @return the {@link DataServiceManager} instance
     */
    public DataServiceManager<DataValueWrapper> getDataServiceManager() {
        return m_dataServiceManager;
    }

}