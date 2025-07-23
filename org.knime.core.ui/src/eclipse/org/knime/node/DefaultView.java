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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.port.PortObject;
import org.knime.core.util.Pair;
import org.knime.core.webui.data.DataService.DataServiceBuilder;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.InitialDataService.Serializer;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.RpcDataService.RpcDataServiceBuilder;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Page.RequireFromFileOrString;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.parameters.NodeParameters;

/**
 * Fluent API to create a node view - not to be created directly but via the {@link DefaultNode}.
 *
 * The view of the node defines how the data is visualized. It requires the view parameters, a location of the view
 * page, and a view description. Optionally, an initial data service or data services can be added to pass the data of
 * the node to the view.<br>
 * Unlike model parameters, view parameters do not alter the data, but can alter the visualization
 * (cf. @{@link DefaultModel}). Therefore, the node is not re-executed when a view parameters changes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class DefaultView implements FluentNodeAPI {

    private final Class<? extends NodeParameters> m_parametersClass;

    final String m_description;

    final Page m_page;

    Function<ViewInput, InitialDataService<?>> m_initialDataServiceFct;

    Function<ViewInput, RpcDataService> m_rpcDataServiceFct;

    static RequireViewParameters create() {
        return parametersClass -> description -> pageFct -> new DefaultView(parametersClass, description,
            pageFct.apply(Page.create()));
    }

    private DefaultView(final Class<? extends NodeParameters> parametersClass, final String description,
        final Page page) {
        m_parametersClass = parametersClass;
        m_description = description;
        m_page = page;
    }

    Optional<Class<? extends NodeParameters>> getParametersClass() {
        return Optional.ofNullable(m_parametersClass);
    }

    /* REQUIRED PROPERTIES */

    /**
     * The build stage that requires the view parameters.
     */
    public interface RequireViewParameters {

        /**
         * Specifies the view parameters class of the node.
         *
         * @param parametersClass the view parameters of the node
         * @return the subsequent build stage
         */
        RequireDescription parametersClass(Class<? extends NodeParameters> parametersClass);

        /**
         * Indicates that the model does not have view parameters.
         *
         * @return the subsequent build stage
         */
        default RequireDescription withoutParameters() {
            return parametersClass(null);
        }
    }

    /**
     * The build stage that requires the view description, which is shown next to the ports' and options' descriptions.
     */
    public interface RequireDescription {

        /**
         * Specifies the description of the view.
         *
         * @param description a description of the provided view
         * @return the subsequent build stage
         */
        RequirePage description(String description);
    }

    /**
     * The build stage that requires the page of the view.
     */
    public interface RequirePage {

        /**
         * Specifies the page of the view.
         *
         * @param page the page at which the view is located
         * @return the {@link DefaultView}
         */
        DefaultView page(Function<RequireFromFileOrString, Page> page);
    }

    /* OPTIONAL PROPERTIES */

    /**
     * Specify the initial data passed to a view.
     *
     * @param <D> the type of the initial data
     * @param initialData a function receiving the stage to create the {@link DefaultInitialData} and returning the
     *            {@link DefaultInitialData}
     * @return this {@link DefaultView}
     */
    public <D> DefaultView initialData(final Function<RequireInitialData, DefaultInitialData<D>> initialData) {
        m_initialDataServiceFct = vi -> initialData.apply(DefaultInitialData.create()).toInitialDataService(vi);
        return this;
    }

    /**
     * Specify the data service passed to a view.
     *
     * @param dataService a function receiving the stage to create the {@link DefaultDataService} and returning the
     *            {@link DefaultDataService}
     * @return this {@link DefaultView}
     */
    public DefaultView dataService(final Function<RequireDataService, DefaultDataService> dataService) {
        m_rpcDataServiceFct = vi -> dataService.apply(DefaultDataService.create()).toRpcDataService(vi);
        return this;
    }

    abstract static class AbstractDataService<T extends DataServiceBuilder> implements DataServiceBuilder {

        protected Runnable m_dispose;

        protected Runnable m_deactivate;

        @Override
        public T onDispose(final Runnable dispose) {
            if (m_dispose != null) {
                throw new IllegalStateException("onDispose already set");
            }
            m_dispose = dispose;
            return thisAsT();
        }

        @Override
        public T onDeactivate(final Runnable deactivate) {
            if (m_deactivate != null) {
                throw new IllegalStateException("onDeactivate already set");
            }
            m_deactivate = deactivate;
            return thisAsT();
        }

        @SuppressWarnings("unchecked")
        T thisAsT() {
            return (T)this;
        }
    }

    /**
     * Fluent API to create the default {@link RpcDataService} - not to be created directly but via the
     * {@link DefaultView}.
     */
    public abstract static class DefaultDataService extends AbstractDataService<DefaultDataService>
        implements FluentNodeAPI {

        abstract RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi);

        RpcDataService toRpcDataService(final ViewInput vi) {
            final var builder = toRpcDataServiceBuilder(vi);
            if (m_dispose != null) {
                builder.onDispose(m_dispose);
            }
            if (m_deactivate != null) {
                builder.onDeactivate(m_deactivate);
            }
            return builder.build();
        }

        static RequireDataService create() {
            return new RequireDataService() {
                @Override
                public SingleHandlerDataService handler(final Function<ViewInput, Object> service) {
                    return new SingleHandlerDataService(service);
                }

                @Override
                public MultipleNamedHandlersDataService addHandler(final String name,
                    final Function<ViewInput, Object> service) {
                    return new MultipleNamedHandlersDataService(name, service);
                }
            };
        }
    }

    /**
     * Fluent API to create the {@link InitialDataService} - not to be created directly but via the {@link DefaultView}.
     *
     * @param <D> the type of the initial data
     */
    public static final class DefaultInitialData<D> extends AbstractDataService<DefaultInitialData<D>>
        implements FluentNodeAPI {

        private final Function<ViewInput, D> m_dataSupplier;

        private Serializer<D> m_serializer;

        DefaultInitialData(final Function<ViewInput, D> dataSupplier) {
            m_dataSupplier = dataSupplier;
        }

        InitialDataService<D> toInitialDataService(final ViewInput vi) {
            final var builder = InitialDataService.builder(() -> m_dataSupplier.apply(vi));
            if (m_dispose != null) {
                builder.onDispose(m_dispose);
            }
            if (m_deactivate != null) {
                builder.onDeactivate(m_deactivate);
            }
            if (m_serializer != null) {
                builder.serializer(m_serializer);
            }
            return builder.build();
        }

        static RequireInitialData create() {
            return new RequireInitialData() {
                @Override
                public <D> DefaultInitialData<D> data(final Function<ViewInput, D> dataSupplier) {
                    return new DefaultInitialData<>(dataSupplier);
                }
            };
        }

        /**
         * Specifies a custom serializer to turn the data object into a string.
         *
         * @param serializer a custom serializer to turn the data object into a string
         * @return this {@link DefaultInitialData}
         */
        public DefaultInitialData<D> serializer(final Serializer<D> serializer) {
            if (m_serializer != null) {
                throw new IllegalStateException("Serializer already set.");
            }
            m_serializer = serializer;
            return this;
        }
    }

    /**
     * Fluent API to create a {@link DefaultDataService} with a single (unnamed) request handler - not to be created
     * directly but via the {@link DefaultView}.
     */
    public static final class SingleHandlerDataService extends DefaultDataService {

        private final Function<ViewInput, Object> m_handler;

        SingleHandlerDataService(final Function<ViewInput, Object> handler) {
            m_handler = handler;
        }

        @Override
        RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi) {
            return RpcDataService.builder(m_handler.apply(vi));
        }
    }

    /**
     * Fluent API to create a {@link DefaultDataService} with multiple named request handlers - not to be created
     * directly but via the {@link DefaultView}.
     */
    public static final class MultipleNamedHandlersDataService extends DefaultDataService
        implements AllowAddingNamedHandler {

        List<Pair<String, Function<ViewInput, Object>>> m_handlers = new ArrayList<>();

        MultipleNamedHandlersDataService(final String firstHandlerName,
            final Function<ViewInput, Object> firstHandler) {
            m_handlers.add(new Pair<>(firstHandlerName, firstHandler));
        }

        @Override
        public MultipleNamedHandlersDataService addHandler(final String name,
            final Function<ViewInput, Object> handler) {
            m_handlers.add(new Pair<>(name, handler));
            return this;
        }

        @Override
        RpcDataServiceBuilder toRpcDataServiceBuilder(final ViewInput vi) {
            final RpcDataServiceBuilder builder = RpcDataService.builder();
            for (final Pair<String, Function<ViewInput, Object>> handler : m_handlers) {
                builder.addService(handler.getFirst(), handler.getSecond().apply(vi));
            }
            return builder;
        }
    }

    /**
     * The build stage which requires the initial data.
     */
    public interface RequireInitialData {

        /**
         * Specifies the initial data of the node.
         *
         * @param data a function receiving the ViewInput and returning the initial data
         * @param <D> the type of the initial data
         * @return the default initial data service created from the supplier
         */
        <D> DefaultInitialData<D> data(Function<ViewInput, D> data);
    }

    /**
     * The build stage which requires the rpc data services .
     */
    public interface RequireDataService extends AllowAddingNamedHandler {

        /**
         * Specifies the (unnamed) handler of the data service. Note that handlers need to be public. The handler is a
         * class that specifies methods which can be called in the frontend to retrieve data. Data can be retrieved by
         * the frontend via RPC of the form [methodName].
         *
         * @param handler a function receiving the ViewInput and returning the handler object
         * @return a {@link SingleHandlerDataService} created from the given data service
         */
        SingleHandlerDataService handler(Function<ViewInput, Object> handler);
    }

    /**
     * The build stage which allows adding further named data services.
     */
    public interface AllowAddingNamedHandler {

        /**
         * Adds one of multiple named handlers to the data service. Note that handlers need to be public. The handler is
         * a class that specifies methods which can be called in the frontend to retrieve data. Data can be retrieved by
         * the frontend via RPC of the form [handlerName].[methodName].
         *
         * @see RpcDataServiceBuilder#addService(String, Object) for more information on named handlers
         * @param name the name of the handler
         * @param handler a function receiving the ViewInput and returning the handler object
         * @return a new {@link MultipleNamedHandlersDataService} created from the given data services
         */
        MultipleNamedHandlersDataService addHandler(String name, Function<ViewInput, Object> handler);
    }

    /**
     * This interface is used within {@link DefaultView#initialData(Function)} and
     * {@link DefaultView#dataService(Function)} and provides access to the parameters and internal data.
     */
    public interface ViewInput {

        /**
         * Returns the parameters of the node view.
         *
         * @param <S> the type of the view parameters
         * @return the view parameters
         */
        <S extends NodeParameters> S getParameters();

        /**
         * Return the internal tables of a node, which can be set during execution in the {@link DefaultModel} (see
         * {@link ExecuteOutput#setInternalData(PortObject...)}.
         *
         * @return the internal port objects of the node cast to {@link BufferedDataTable BufferedDataTables}
         */
        default BufferedDataTable[] getInternalTables() {
            return Arrays.stream(getInternalPortObjects()).map(BufferedDataTable.class::cast)
                .toArray(BufferedDataTable[]::new);
        }

        /**
         * Returns the internal port objects of a node, which can be set during execution in the {@link DefaultModel}
         * (see {@link ExecuteOutput#setInternalData(PortObject...)}.
         *
         * @return the internal port objects of the node
         */
        PortObject[] getInternalPortObjects();
    }
}
