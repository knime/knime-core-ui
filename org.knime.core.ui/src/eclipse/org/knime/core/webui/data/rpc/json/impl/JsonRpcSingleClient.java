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
 *   Jul 14, 2020 (carlwitt): created
 */
package org.knime.core.webui.data.rpc.json.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.data.rpc.AbstractRpcSingleClient;
import org.knime.core.webui.data.rpc.RpcTransport;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JSON-RPC based implementation of a node data service client.
 *
 * TODO: somehow keep backward-compatibility when a node implementer switches from a single json-rpc server to a (multi)
 * json rpc server (the service name need to be provided in the later)
 *
 * @param <S> the node data service interface type; defines which methods are offered by the node model to retrieve
 *            data. The parameters and return type of a method defined in the interface are subjected to serialization
 *            with an {@link ObjectMapper}. Jackson serialization works out-of-the-box on simple bean-like classes. In
 *            more advanced cases, parameter types and result types have to use Jackson annotations like
 *            {@link JsonAutoDetect} to control the serialization behavior.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 */
public class JsonRpcSingleClient<S> extends AbstractRpcSingleClient<S> {

    private static final Supplier<ObjectMapper> OBJECT_MAPPER = () -> ObjectMapperUtil.getInstance().getObjectMapper();

    private final ObjectMapper m_mapper;

    /**
     * Used in the JSON-RPC call as id to correlate the request with the response.
     */
    long m_callId = 0;

    /**
     * @param serviceInterface the data retrieval interface offered to the node dialog/view by the node model. This
     *            interface is defined and implemented by the node developer. See {@link JsonRpcSingleClient} for
     *            details.
     */
    public JsonRpcSingleClient(final Class<S> serviceInterface) {
        this(serviceInterface, OBJECT_MAPPER.get());
    }

    /**
     * @param serviceInterface the data retrieval interface offered to the node dialog/view by the node model. This
     *            interface is defined and implemented by the node developer. See {@link JsonRpcSingleClient} for
     *            details.
     * @param mapper used to provide custom serialization for the parameters and results of a remote procedure call
     */
    public JsonRpcSingleClient(final Class<S> serviceInterface, final ObjectMapper mapper) {
        super(serviceInterface);
        m_mapper = CheckUtils.checkNotNull(mapper, "Object mapper passed to JSON-RPC client must not be null.");
    }

    /**
     * For testing only: Constructor to initialize a JSON-RPC client with a test rpc transport.
     */
    JsonRpcSingleClient(final Class<S> serviceInterface, final ObjectMapper mapper, final RpcTransport rpcTransport) {
        super(serviceInterface, rpcTransport);
        m_mapper = mapper == null ? OBJECT_MAPPER.get() : mapper;
    }

    @Override
    protected String convertCall(final Method method, final Object[] args) {
        String res = JsonRpcClient.convertCall(null, method, args, m_mapper, m_callId);
        m_callId++;
        return res;
    }

    @Override
    protected <R> R convertResult(final String response, final Type valueType) throws Exception {
        return JsonRpcClient.convertResult(response, valueType, m_mapper);
    }

}
