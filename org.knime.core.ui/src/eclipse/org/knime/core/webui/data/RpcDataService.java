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
 *   Sep 14, 2021 (hornm): created
 */
package org.knime.core.webui.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.function.FailableFunction;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService.WildcardHandler.RequestException;
import org.knime.core.webui.data.rpc.RpcServerManager;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcServer;
import org.knime.core.webui.data.rpc.json.impl.JsonRpcSingleServer;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link RpcDataService} where the requests result in actual method-calls of registered handler(s) (aka remote
 * procedure calls).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class RpcDataService extends AbstractDataService {

    private static final ObjectMapper MAPPER = ObjectMapperUtil.getInstance().getObjectMapper();

    private final FailableFunction<String, String, IOException> m_rpcServer;

    private final Function<String, Object> m_getHandler;

    private final NodeContainer m_nc;

    private RpcDataService(final RpcDataServiceBuilder builder) {
        super(builder);
        final var hasUnnamedHandler = builder.m_unnamedHandler != null;
        final var hasNamedHandlers = !builder.m_namedHandlers.isEmpty();
        if (hasUnnamedHandler) {
            if (hasNamedHandlers) {
                throw new IllegalStateException(
                    "Having named and unnamed handlers at the same time is not supported at the moment.");
            }
            if (builder.m_unnamedHandler instanceof WildcardHandler handler) {
                m_rpcServer = createWildcardRpcServer(handler);
                m_getHandler = name -> handler;
            } else {
                var rpcServer = new JsonRpcSingleServer<>(builder.m_unnamedHandler);
                m_rpcServer = request -> RpcServerManager.doRpc(rpcServer, request);
                m_getHandler = name -> rpcServer.getHandler();
            }
        } else if (hasNamedHandlers) {
            final var jsonRpcServer = new JsonRpcServer();
            builder.m_namedHandlers.forEach(jsonRpcServer::addService);
            var rpcServer = jsonRpcServer;
            m_rpcServer = request -> RpcServerManager.doRpc(rpcServer, request);
            m_getHandler = rpcServer::getHandler;
        } else {
            throw new IllegalStateException("No handler was supplied to this RPCDataService");
        }
        m_nc = DataServiceUtil.getNodeContainerFromContext();
    }

    private static FailableFunction<String, String, IOException>
        createWildcardRpcServer(final WildcardHandler handler) {
        return request -> {
            try {
                final var root = MAPPER.readValue(request, Map.class);
                final var method = root.get("method").toString();
                final var paramsObj = root.get("params");
                final var id = (int)root.get("id");
                Object result;
                try {
                    if (paramsObj instanceof List list) {
                        result = handler.handleRequest(method, list);
                    } else if (paramsObj instanceof Map map) {
                        result = handler.handleRequest(method, map);
                    } else {
                        throw new IllegalArgumentException("Invalid params type: " + paramsObj.getClass());
                    }
                } catch (RequestException ex) {
                    return createJsonRpcErrorResponse(ex.getErrorCode(), ex.getMessage(), null, id);
                }
                return createJsonRpcReponse(result, id);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static String createJsonRpcReponse(final Object result, final int id) {
        var jsonRpc = MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", id);
        jsonRpc.set("result", MAPPER.convertValue(result, JsonNode.class));
        return jsonRpc.toString();
    }

    private static String createJsonRpcErrorResponse(final int errorCode, final String message, final Object data,
        final int id) {
        ObjectNode jsonRpc = MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", id);
        var res = jsonRpc.putObject("error").put("code", errorCode).put("message", message);
        if (data != null) {
            res.set("data", MAPPER.convertValue(data, JsonNode.class));
        }
        return jsonRpc.toString();
    }

    /**
     * @param request the rpc request (e.g. encoded in json-rpc)
     * @return the rpc-response (e.g. a json-rpc response)
     */
    public String handleRpcRequest(final String request) {
        if (m_nc != null) {
            NodeContext.pushContext(m_nc);
        }
        try {
            DataServiceContext.init(m_nc);
            final var response = m_rpcServer.apply(request);
            // We have to get the DataServiceContext again here, since the context may have changed since (or as a
            // consequence of) clearing it
            final var warningMessages = DataServiceContext.get().getWarningMessages();
            if (warningMessages != null && warningMessages.length > 0) {
                final var root = (ObjectNode)MAPPER.readTree(response);
                if (root.has("result")) {
                    return root.set("warningMessages", MAPPER.valueToTree(warningMessages)).toString();
                }
            }
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException("A problem occurred while making a rpc call.", ex);
        } finally {
            DataServiceContext.remove();
            if (m_nc != null) {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * -
     *
     * @param name the handler name, {@code null} to get the unnamed handler
     * @return the handler, or {@code null} if none
     */
    public Object getHandler(final String name) {
        return m_getHandler.apply(name);
    }

    /**
     * Helper to create a json rpc request string.
     *
     * @param method
     * @param params
     * @return the json rpc request as json string
     */
    public static String jsonRpcRequest(final String method, final String... params) {
        var paramsArrayNode = MAPPER.createArrayNode();
        for (var param : params) {
            paramsArrayNode.add(param);
        }
        return MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", 1).put("method", method)
            .set("params", paramsArrayNode).toPrettyString();
    }

    /**
     * @param <S>
     * @param handler the handler whose methods are called. Whenever any of the methods are being called, a
     *            {@link DataServiceContext} is available within the method.
     * @return a new builder instance
     */
    public static <S> RpcDataServiceBuilder builder(final S handler) {
        return new RpcDataServiceBuilder(handler);
    }

    /**
     * @return a new builder instance
     */
    public static RpcDataServiceBuilder builder() {
        return new RpcDataServiceBuilder();
    }

    /**
     * The builder.
     */
    public static final class RpcDataServiceBuilder extends AbstractDataServiceBuilder {

        private final Object m_unnamedHandler;

        private Map<String, Object> m_namedHandlers = new HashMap<>();

        private RpcDataServiceBuilder(final Object handler) {
            m_unnamedHandler = handler;
        }

        private RpcDataServiceBuilder() {
            m_unnamedHandler = null;
        }

        /**
         * Add a named service which can be accessed via RPC of the form [name].[methodName] i.e. the method name of the
         * given handler prefixed by the name and a "dot".
         *
         * @param name
         * @param handler the handler whose methods are called for the respective requests. Whenever any of the methods
         *            are being called, a {@link DataServiceContext} is available within the method.
         * @return the builder
         */
        public RpcDataServiceBuilder addService(final String name, final Object handler) {
            m_namedHandlers.put(name, handler);
            return this;
        }

        @Override
        public RpcDataServiceBuilder onDispose(final Runnable dispose) {
            super.onDispose(dispose);
            return this;
        }

        @Override
        public RpcDataServiceBuilder onDeactivate(final Runnable deactivate) {
            super.onDeactivate(deactivate);
            return this;
        }

        /**
         * @return a new instance
         */
        public RpcDataService build() {
            return new RpcDataService(this);
        }

    }

    /**
     * Handler which can handle any rpc-request, independent from the method name and parameters.
     */
    public interface WildcardHandler {

        /**
         * Handles requests where the parameters are given as a list.
         *
         * @param method the rpc method name
         * @param params the parameters of the request, given as a list
         * @return the response
         * @throws RequestException if an error occurred while handling the request
         */
        Object handleRequest(String method, List<Object> params) throws RequestException;

        /**
         * Handles requests for named paramaters.
         *
         * @param method the rpc method name
         * @param params the parameters of the request, given as a map
         * @return the response
         * @throws RequestException if an error occurred while handling the request
         */
        Object handleRequest(String method, Map<String, Object> params) throws RequestException;

        @SuppressWarnings("javadoc")
        class RequestException extends Exception {

            private int m_errorCode;

            RequestException(final String message, final int errorCode) {
                super(message);
                m_errorCode = errorCode;
            }

            int getErrorCode() {
                return m_errorCode;
            }

        }

        @SuppressWarnings("javadoc")
        class MethodNotFoundException extends RequestException {

            MethodNotFoundException(final String message) {
                super(message, -32601);
            }

        }

        @SuppressWarnings("javadoc")
        class InvalidParamsException extends RequestException {

            InvalidParamsException(final String message) {
                super(message, -32602);
            }

        }

        @SuppressWarnings("javadoc")
        class InternalErrorException extends RequestException {

            InternalErrorException(final String message) {
                super(message, -32603);
            }

        }

    }


}
