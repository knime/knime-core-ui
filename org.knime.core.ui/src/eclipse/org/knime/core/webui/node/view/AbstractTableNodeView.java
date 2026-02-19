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
 *  when its Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 27, 2026 (gerling): created
 */
package org.knime.core.webui.node.view;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.NodeParameters;

/**
 * Abstract base class for {@link NodeTableView} implementations that visualize data from a {@link BufferedDataTable}.
 * Captures all state and behavior shared between table-based views (e.g. Table View and Tile View), leaving only the
 * page definition and service-creation specifics to concrete subclasses.
 *
 * @param <S> the concrete view-settings type
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractTableNodeView<S extends NodeParameters> implements NodeTableView {

    /** Table id derived from the node id – globally unique across all view instances. */
    protected final String m_tableId;

    /** Supplies the input table that this view visualizes. */
    protected final Supplier<BufferedDataTable> m_tableSupplier;

    /** Supplies the currently selected row keys, or {@code null} if selection is not shown. */
    protected final Supplier<Set<RowKey>> m_selectionSupplier;

    /**
     * The settings class used by {@link #loadValidatedSettingsFrom}. May be replaced by a subclass after construction
     * to support custom settings subclasses (see the {@code settingsClass} constructor variant).
     */
    protected Class<? extends S> m_settingsClass;

    /** Current view settings; {@code null} until first initialised from persisted state or from the table spec. */
    protected S m_settings;

    private final int m_inputPortIndex;

    /**
     * Cached supplier for the RPC data service. Set during {@link #createInitialDataService()} so that the initial-data
     * service and the RPC data service share the same underlying data-service instance (and its caches).
     */
    protected Supplier<RpcDataService> m_rpcDataServiceSupplier;

    /**
     * Canonical constructor.
     *
     * @param tableId globally unique table id, typically derived from the node id
     * @param tableSupplier supplier of the input table
     * @param selectionSupplier supplier of the currently selected row keys, or {@code null}
     * @param inputPortIndex the input port index (0-based, ignoring the flow-variable port)
     * @param defaultSettingsClass the settings class to instantiate when no persisted settings are available
     */
    protected AbstractTableNodeView(final String tableId, final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final int inputPortIndex, final Class<S> defaultSettingsClass) {
        m_tableId = tableId;
        m_tableSupplier = tableSupplier;
        m_selectionSupplier = selectionSupplier;
        m_inputPortIndex = inputPortIndex;
        m_settingsClass = defaultSettingsClass;
    }

    /**
     * Creates a default settings instance from the table spec, used when no persisted settings are available.
     *
     * @param spec the data table spec of the connected table
     * @return a new default settings instance
     */
    protected abstract S createDefaultSettings(DataTableSpec spec);

    /**
     * Builds the initial-data service and a supplier for the RPC data service, sharing a single underlying data-service
     * instance. This method is called from {@link #createInitialDataService()} after {@link #m_settings} has been
     * initialised.
     *
     * @return a pair of the initial-data service and the corresponding RPC data service supplier
     */
    protected abstract Pair<? extends InitialDataService<?>, Supplier<RpcDataService>>
        buildInitialDataServiceWithRpcDataService();

    /**
     * Builds the RPC data service when {@link #createInitialDataService()} has not been called first (i.e. when no
     * shared underlying data-service instance is cached yet).
     *
     * @return a freshly created, standalone RPC data service
     */
    protected abstract RpcDataService buildFallbackRpcDataService();

    @Override
    public boolean canBeUsedInReport() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <D> Optional<InitialDataService<D>> createInitialDataService() {
        if (m_settings == null) {
            m_settings = createDefaultSettings(m_tableSupplier.get().getSpec());
        }
        final var pair = buildInitialDataServiceWithRpcDataService();
        m_rpcDataServiceSupplier = pair.getSecond();
        return Optional.of((InitialDataService<D>)pair.getFirst());
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        if (m_rpcDataServiceSupplier != null) {
            return Optional.of(m_rpcDataServiceSupplier.get());
        }
        return Optional.of(buildFallbackRpcDataService());
    }

    @Override
    public <D> Optional<ApplyDataService<D>> createApplyDataService() {
        return Optional.empty();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var viewSettings = NodeParametersUtil.loadSettings(settings, m_settingsClass);
        viewSettings.validate();
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_settings = NodeParametersUtil.loadSettings(settings, m_settingsClass);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("The settings should have been validated first.", ex);
        }
    }

    @Override
    public int getPortIndex() {
        return m_inputPortIndex;
    }

}
