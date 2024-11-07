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
 *   31 Oct 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.table;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettingsSerializer;
import org.knime.core.webui.node.view.table.data.TableViewDataService;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;
import org.knime.core.webui.node.view.table.data.TableViewInitialData;
import org.knime.core.webui.node.view.table.data.TableViewInitialDataImpl;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.table.data.render.SwingBasedRendererFactory;
import org.knime.core.webui.page.Page;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class TableViewUtil {

    // Note on the 'static' page id: the entire TableView-page can be considered 'completely static'
    // because the page, represented by a vue component, is just a file (won't change at runtime)
    // And the image resources associated with a page of an individual table view instance are
    // served with a globally unique 'table id' in the path.
    private static final String TABLEVIEW_PAGE_ID = "tableview";

    /**
     * The page representing the table view.
     */
    public static final Page PAGE = Page.builder(TableViewUtil.class, "js-src/dist", "TableView.js") //
        .markAsReusable(TABLEVIEW_PAGE_ID)
        .addResources(createTableCellImageResourceSupplier(),
            DataValueImageRendererRegistry.RENDERED_CELL_IMAGES_PATH_PREFIX, true) //
        .build();

    // This is workaround/hack for the lack of proper random-access functionality for a (BufferedData)Table.
    // For more details see the class' javadoc.
    // It's static because it's registered and kept with the page which in turn is assumed to be static
    // (i.e. doesn't change between node instances and, hence, won't be re-created for each node instance).
    static final DataValueImageRendererRegistry RENDERER_REGISTRY =
        new DataValueImageRendererRegistry(() -> TABLEVIEW_PAGE_ID);

    private TableViewUtil() {
        // utility class
    }

    private static Function<String, InputStream> createTableCellImageResourceSupplier() {
        return relativePath -> {
            var bytes = RENDERER_REGISTRY.renderImage(relativePath);
            return new ByteArrayInputStream(bytes);
        };
    }

    /**
     * @param tableViewDataService
     * @param tableId
     * @return a new table view data service instance
     */
    public static RpcDataService createRpcDataService(final TableViewDataService tableViewDataService,
        final String tableId) {

        Runnable clearCache = () -> TableViewUtil.deactivateTableViewDataService(tableViewDataService, tableId);
        return RpcDataService.builder(tableViewDataService).onDeactivate(clearCache).onDispose(clearCache).build();
    }

    /**
     * Deactivate the table view data service and clear the cache.
     *
     * @param tableViewDataService the table view data service to deactivate
     * @param tableId the table id
     */
    public static void deactivateTableViewDataService(final TableViewDataService tableViewDataService,
        final String tableId) {
        tableViewDataService.clearCache();
        TableViewUtil.RENDERER_REGISTRY.clearImageDataCache(tableId);
    }

    /**
     * @param tableSupplier supplying the input table
     * @param selectionSupplier supplying the currently selected row keys (for update of totalSelected when filtering);
     *            or {@code null} if no selecting is shown
     * @param tableId
     * @return the {@link TableViewDataService} associated to the node
     */
    public static TableViewDataService createTableViewDataService(final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId) {
        return new TableViewDataServiceImpl(tableSupplier, selectionSupplier, tableId, new SwingBasedRendererFactory(),
            RENDERER_REGISTRY);
    }


    /**
     * @param tableSupplier supplying the input table
     * @param selectionSupplier supplying the currently selected row keys (for update of totalSelected when filtering);
     *            or {@code null} if no selecting is shown
     * @param tableId
     * @return the {@link TableViewDataService} associated to the node
     */
    public static TableViewDataService createTableViewDataService(final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId, final Consumer<String> projectIdConsumer) {
        return new TableViewDataServiceImpl(tableSupplier, selectionSupplier, tableId, new SwingBasedRendererFactory(),
            RENDERER_REGISTRY, projectIdConsumer);
    }

    /**
     * @param settings table view view settings
     * @param table the table to create the initial data for
     * @param tableId a globally unique id to be able to uniquely identify the images belonging to the table used here
     * @return the table view's initial data object
     */
    static TableViewInitialData createInitialData(final TableViewViewSettings settings, final BufferedDataTable table,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId) {
        return new TableViewInitialDataImpl(settings, () -> table, selectionSupplier, tableId,
            new SwingBasedRendererFactory(), RENDERER_REGISTRY);
    }

    static TableViewInitialData createInitialData(final TableViewViewSettings settings, final BufferedDataTable table,
        final TableViewDataService dataService) {
        return new TableViewInitialDataImpl(settings, () -> table, dataService);
    }

    /**
     * @param settingsSupplier
     * @param tableSupplier
     * @param selectionSupplier
     * @param tableId
     * @return the table view initial data service
     */
    public static InitialDataService<TableViewInitialData> createInitialDataService(
        final Supplier<TableViewViewSettings> settingsSupplier, final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId) {
        return createInitialDataService(settingsSupplier, tableSupplier, selectionSupplier, tableId, null, null);
    }

    /**
     * @param settingsSupplier
     * @param tableSupplier
     * @param selectionSupplier
     * @param tableId
     * @param onDeactivate
     * @param onDispose
     * @return the table view initial data service
     */
    public static InitialDataService<TableViewInitialData> createInitialDataService(
        final Supplier<TableViewViewSettings> settingsSupplier, final Supplier<BufferedDataTable> tableSupplier,
        final Supplier<Set<RowKey>> selectionSupplier, final String tableId, final Runnable onDeactivate,
        final Runnable onDispose) {
        final Supplier<TableViewInitialData> initialDataSupplier =
            () -> createInitialData(settingsSupplier.get(), tableSupplier.get(), selectionSupplier, tableId);
        return createInitialDataService(initialDataSupplier, tableId, onDeactivate, onDispose);
    }

    /**
     *
     * This method must be used instead of constructing the initial data service and the RPC data service individually
     * if there are settings which influence what tables are cached within the {@link TableViewDataService}. E.g. the
     * first of such settings is "showSelectedRowsOnly". The returned pair uses the same instance of
     * {@link TableViewDataServiceImpl} so that the RPC data service caches stay in sync with the initial data supplied
     * to the front-end. Otherwise methods which rely on these caches like {@link TableViewDataService#getCopyContent}
     * and {@link TableViewDataService#getCurrentRowKeys} can return wrong data.
     *
     * @param settingsSupplier
     * @param tableSupplier
     * @param selectionSupplier
     * @param tableId
     * @param onDeactivate
     * @param onDispose
     * @return the table view initial data service
     */
    public static Pair<InitialDataService<TableViewInitialData>, Supplier<RpcDataService>>
        createInitialDataServiceWithRPCDataService(final Supplier<TableViewViewSettings> settingsSupplier,
            final Supplier<BufferedDataTable> tableSupplier, final Supplier<Set<RowKey>> selectionSupplier,
            final String tableId, final Runnable onDeactivate, final Runnable onDispose) {

        DataServiceCache dataServiceCache = new DataServiceCache() {
            @Override
            protected TableViewDataService initialize() {
                return createTableViewDataService(tableSupplier, selectionSupplier, tableId);
            }
        };

        final Supplier<RpcDataService> rpcDataServiceSupplier =
            () -> createRpcDataService(dataServiceCache.get(), tableId);
        final Supplier<TableViewInitialData> initialDataSupplier =
            () -> createInitialData(settingsSupplier.get(), tableSupplier.get(), dataServiceCache.get());
        final var initialDataService = createInitialDataService(initialDataSupplier, tableId, onDeactivate, onDispose);
        return new Pair<>(initialDataService, rpcDataServiceSupplier);
    }

    private abstract static class DataServiceCache {

        TableViewDataService m_dataService;

        abstract TableViewDataService initialize();

        TableViewDataService get() {
            if (m_dataService == null) {
                m_dataService = initialize();
            }
            return m_dataService;
        }

    }

    private static InitialDataService<TableViewInitialData> createInitialDataService(
        final Supplier<TableViewInitialData> initialDataSupplier, final String tableId, final Runnable onDeactivate,
        final Runnable onDispose) {
        Runnable clearImageData = () -> TableViewUtil.RENDERER_REGISTRY.clearImageDataCache(tableId);
        return InitialDataService.builder(initialDataSupplier::get) //
            .onDeactivate(() -> {
                clearImageData.run();
                if (onDeactivate != null) {
                    onDeactivate.run();
                }
            }) //
            .onDispose(() -> {
                clearImageData.run();
                if (onDispose != null) {
                    onDispose.run();
                }
            }) //
            .serializer(new DefaultNodeSettingsSerializer<>()) //
            .build();
    }

    /**
     * Helper to return a proper table id from a node id.
     *
     * @param nodeID
     * @return a table id (which is globally unique, because the node id is)
     */
    public static String toTableId(final NodeID nodeID) {
        return nodeID.toString().replace(":", "_");
    }

}
