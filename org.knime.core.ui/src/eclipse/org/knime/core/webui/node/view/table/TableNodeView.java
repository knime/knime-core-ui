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
 *   3 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.table;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.util.NodeCleanUpCallback;
import org.knime.core.webui.node.view.NodeTableView;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.table.data.TableViewInitialData;
import org.knime.core.webui.page.Page;

/**
 * A {@link NodeView} implementation for displaying tables.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class TableNodeView implements NodeTableView {

    private final String m_tableId;

    private final Supplier<BufferedDataTable> m_tableSupplier;

    private final Supplier<Set<RowKey>> m_selectionSupplier;

    private Class<? extends TableViewViewSettings> m_settingsClass;

    private TableViewViewSettings m_settings;

    private final int m_inputPortIndex;

    private Supplier<RpcDataService> m_rpcDataServiceSupplier;

    /**
     * @param tableSupplier supplier of the table which this view visualizes
     * @param selectionSupplier supplier of currently selected rowKeys
     * @param nc the node this node view belongs to; used to determine the table id (derived from the node id) and to a
     *            {@link NodeCleanUpCallback} to free resources (caches) in case the node is, e.g., deleted
     * @param inputPortIndex the index of the input port this table node view is created for; index starts at '0'
     *            ignoring the flow variable port
     */
    public TableNodeView(final Supplier<BufferedDataTable> tableSupplier, final Supplier<Set<RowKey>> selectionSupplier,
        final NodeContainer nc, final int inputPortIndex) {
        this(TableViewUtil.toTableId(nc.getID()), tableSupplier, selectionSupplier, inputPortIndex);
    }

    /**
     * @param tableSupplier supplier of the table which this view visualizes
     * @param nc the node this node view belongs to; used to determine the table id (derived from the node id) and to a
     *            {@link NodeCleanUpCallback} to free resources (caches) in case the node is, e.g., deleted
     * @param inputPortIndex the index of the input port this table node view is created for; index starts at '0'
     *            ignoring the flow variable port
     */
    public TableNodeView(final Supplier<BufferedDataTable> tableSupplier, final NodeContainer nc,
        final int inputPortIndex) {
        this(tableSupplier, null, nc, inputPortIndex);
    }

    /**
     * @param tableSupplier supplier of the table which this view visualizes
     * @param nc the node this node view belongs to; used to determine the table id (derived from the node id) and to a
     *            {@link NodeCleanUpCallback} to free resources (caches) in case the node is, e.g., deleted
     * @param inputPortIndex the index of the input port this table node view is created for; index starts at '0'
     *            ignoring the flow variable port
     * @param settingsClass to be used instead of {@link TableViewViewSettings} for the view settings
     */
    public TableNodeView(final Supplier<BufferedDataTable> tableSupplier, final NodeContainer nc,
        final int inputPortIndex, final Class<? extends TableViewViewSettings> settingsClass) {
        this(tableSupplier, null, nc, inputPortIndex);
        m_settingsClass = settingsClass;
    }

    TableNodeView(final String tableId, final Supplier<BufferedDataTable> tableSupplier, final int inputPortIndex) {
        this(tableId, tableSupplier, null, inputPortIndex);
    }

    TableNodeView(final String tableId, final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final int inputPortIndex) {
        m_tableId = tableId;
        m_tableSupplier = tableSupplier;
        m_selectionSupplier = selectionSupplier;
        m_inputPortIndex = inputPortIndex;
        m_settingsClass = TableViewViewSettings.class;
    }

    @Override
    public Page getPage() {
        return TableViewUtil.PAGE;
    }

    @Override
    public boolean canBeUsedInReport() {
        return true;
    }

    @Override
    public Optional<InitialDataService<TableViewInitialData>> createInitialDataService() {
        if (m_settings == null) {
            m_settings = new TableViewViewSettings(m_tableSupplier.get().getSpec());
        }
        final var pair = TableViewUtil.createInitialDataServiceWithRPCDataService(() -> m_settings, m_tableSupplier,
            m_selectionSupplier, m_tableId, null, null);
        m_rpcDataServiceSupplier = pair.getSecond();
        return Optional.of(pair.getFirst());
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        if (m_rpcDataServiceSupplier != null) {
            return Optional.of(m_rpcDataServiceSupplier.get());
        }
        return Optional.of(TableViewUtil.createRpcDataService(
            TableViewUtil.createTableViewDataService(m_tableSupplier, m_selectionSupplier, m_tableId), m_tableId));
    }

    @Override
    public Optional<ApplyDataService<?>> createApplyDataService() {
        return Optional.empty();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_settings = DefaultNodeSettings.loadSettings(settings, m_settingsClass);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("The settings should have been validated first.", ex);
        }
    }

    @Override
    public int getPortIndex() {
        return m_inputPortIndex;
    }

}
