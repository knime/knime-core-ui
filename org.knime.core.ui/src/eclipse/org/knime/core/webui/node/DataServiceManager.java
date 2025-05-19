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
 *   Feb 24, 2022 (hornm): created
 */
package org.knime.core.webui.node;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.util.NodeCleanUpCallback;

/**
 * Manages data service instances created by {@link DataServiceProvider}-implementations.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <N> the node wrapper this manager operates on
 */
public final class DataServiceManager<N extends NodeWrapper> {

    private final Map<N, InitialDataService<Object>> m_initialDataServices = new WeakHashMap<>();

    private final Map<N, RpcDataService> m_dataServices = new WeakHashMap<>();

    private final Map<N, ApplyDataService<Object>> m_applyDataServices = new WeakHashMap<>();

    private final Function<N, DataServiceProvider> m_getDataServiceProvider;

    private final boolean m_shouldCleanUpDataServicesOnNodeStateChange;

    /**
     * @param getDataServiceProvider function that returns the {@link DataServiceProvider}-instance for the given node
     *            wrapper
     */
    public DataServiceManager(final Function<N, DataServiceProvider> getDataServiceProvider) {
        this(getDataServiceProvider, false);
    }

    /**
     * @param getDataServiceProvider function that returns the {@link DataServiceProvider}-instance for the given node
     *            wrapper
     * @param shouldCleanUpDataServicesOnNodeStateChange
     */
    public DataServiceManager(final Function<N, DataServiceProvider> getDataServiceProvider,
        final boolean shouldCleanUpDataServicesOnNodeStateChange) {
        m_getDataServiceProvider = getDataServiceProvider;
        m_shouldCleanUpDataServicesOnNodeStateChange = shouldCleanUpDataServicesOnNodeStateChange;
    }

    /**
     * Returns data service instance of the given type or an empty optional of no data service of that type is
     * associated with the node.
     *
     * @param <S> the data service type being returned
     * @param nodeWrapper node to get the data service for
     * @param dataServiceClass A type (or sub-type) of {@link InitialDataService}, {@link RpcDataService} or
     *            {@link ApplyDataService}.
     * @return the data service instance or an empty optional if no data service is available
     */
    public <S> Optional<S> getDataServiceOfType(final N nodeWrapper, final Class<S> dataServiceClass) {
        Object ds = null;
        if (InitialDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getInitialDataService(nodeWrapper).orElse(null);
        } else if (RpcDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getRpcDataService(nodeWrapper).orElse(null);
        } else if (ApplyDataService.class.isAssignableFrom(dataServiceClass)) {
            ds = getApplyDataService(nodeWrapper).orElse(null);
        }
        if (ds != null && !dataServiceClass.isAssignableFrom(ds.getClass())) {
            ds = null;
        }
        return Optional.ofNullable((S)ds);
    }

    /**
     * Helper to call the {@link InitialDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @return the initial data
     * @throws IllegalStateException if there is not initial data service available
     */
    public String callInitialDataService(final N nodeWrapper) {
        /**
         * TODO UIEXT-1525: Remove this
         */
        removeRpcDataService(nodeWrapper).flatMap(RpcDataService::deactivateRunnable).ifPresent(Runnable::run);
        return getInitialDataService(nodeWrapper) //
            .filter(InitialDataService.class::isInstance) //
            .orElseThrow(() -> new IllegalStateException("No initial data service available")) //
            .getInitialData();
    }

    private Optional<InitialDataService<Object>> getInitialDataService(final N nodeWrapper) {
        return getOrCreateDataService(nodeWrapper, m_initialDataServices,
            DataServiceProvider::createInitialDataService);
    }

    /**
     * Helper to call the {@link RpcDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @param request the data service request
     * @return the data service response
     * @throws IllegalStateException if there is no text data service
     */
    public String callRpcDataService(final N nodeWrapper, final String request) {
        var service = getRpcDataService(nodeWrapper).filter(RpcDataService.class::isInstance).orElse(null);
        if (service != null) {
            return service.handleRpcRequest(request);
        } else {
            throw new IllegalStateException("No rpc data service available");
        }
    }

    private Optional<RpcDataService> getRpcDataService(final N nodeWrapper) {
        return getOrCreateDataService(nodeWrapper, m_dataServices, DataServiceProvider::createRpcDataService);
    }

    private Optional<RpcDataService> removeRpcDataService(final N nodeWrapper) {
        return Optional.ofNullable(m_dataServices.remove(nodeWrapper));
    }

    /**
     * Helper to call the {@link ApplyDataService}.
     *
     * @param nodeWrapper the node to call the data service for
     * @param request the data service request representing the data to apply
     * @return the apply data service response. This does not entail a result from the data service but might be used to
     *         signal warnings or an error.
     * @throws IllegalStateException if there is no text apply data service
     */
    public String callApplyDataService(final N nodeWrapper, final String request) {
        var service = getApplyDataService(nodeWrapper).orElse(null);
        if (service == null) {
            throw new IllegalStateException("No apply data service available");
        }
        return service.applyData(request);
    }

    private Optional<ApplyDataService<Object>> getApplyDataService(final N nodeWrapper) {
        return getOrCreateDataService(nodeWrapper, m_applyDataServices, DataServiceProvider::createApplyDataService);
    }

    private <S extends DataService> Optional<S> getOrCreateDataService(final N nodeWrapper,
        final Map<N, S> dataServices, final Function<DataServiceProvider, Optional<S>> createNewService) {
        if (!dataServices.containsKey(nodeWrapper)) {
            final var optionalDataService =
                nodeWrapper.getWithContext(() -> createNewService.apply(m_getDataServiceProvider.apply(nodeWrapper)));
            optionalDataService.ifPresent(dataService -> {
                dataServices.put(nodeWrapper, dataService);
                NodeCleanUpCallback.builder(nodeWrapper.get(), () -> {
                    final var removedDataService = dataServices.remove(nodeWrapper);
                    if (removedDataService != null) {
                        removedDataService.disposeRunnable().ifPresent(Runnable::run);
                    }

                }).cleanUpOnNodeStateChange(m_shouldCleanUpDataServicesOnNodeStateChange).build();
            });
            return optionalDataService;
        } else {
            return Optional.of(dataServices.get(nodeWrapper));
        }
    }

    /**
     * Calls {@code deactivate} on a data service if there is a data service instance available for the given node
     * (wrapper).
     *
     * @param nodeWrapper
     */
    public void deactivateDataServices(final N nodeWrapper) {
        dataServices(nodeWrapper).forEach(ds -> ds.deactivateRunnable().ifPresent(Runnable::run));
    }

    /**
     * @param nodeWrapper
     * @return whether at least one data service has a deactivate-runnable registered
     */
    public boolean hasDeactivateRunnable(final N nodeWrapper) {
        return dataServices(nodeWrapper).anyMatch(ds -> ds.deactivateRunnable().isPresent());
    }

    private Stream<DataService> dataServices(final N nodeWrapper) {
        return Stream.<DataService> of( //
            getInitialDataService(nodeWrapper).orElse(null), //
            getRpcDataService(nodeWrapper).orElse(null), //
            getApplyDataService(nodeWrapper).orElse(null) //
        ).filter(Objects::nonNull);
    }

}
