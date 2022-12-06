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
package org.knime.core.webui.data.rpc.json;

import java.io.IOException;

import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.data.rpc.RpcDataService;
import org.knime.core.webui.data.rpc.RpcServerManager;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;
import org.knime.core.webui.data.text.TextDataService;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link RpcDataService} where the rpc requests and responses are encoded using the json-rpc standard
 * (https://www.jsonrpc.org/specification).
 * <p>
 * Data service implementations can obtain a {@link DataServiceContext}. This context can be used to add warning
 * messages, which will then be added to the data service response.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public interface JsonRpcDataService extends RpcDataService, TextDataService {

    /**
     * {@inheritDoc}
     */
    @Override
    default String handleRequest(final String request) {
        try {
            DataServiceContext.getContext().clear();
            final var response = RpcServerManager.doRpc(getRpcServer(), request);
            // We have to get the DataServiceContext again here, since the context may have changed since (or as a
            // consequence of) clearing it
            final var warningMessages = DataServiceContext.getContext().getWarningMessages();
            if (warningMessages != null && warningMessages.length > 0) {
                final var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
                final var root = (ObjectNode)mapper.readTree(response);
                if (root.has("result")) {
                    return root.set("warningMessages", mapper.valueToTree(warningMessages)).toString();
                }
            }
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException("A problem occurred while making a rpc call.", ex);
        } finally {
            DataServiceContext.getContext().clear();
        }
    }

    /**
     * Helper to create a json rpc request string.
     *
     * @param method
     * @param params
     * @return the json rpc request as json string
     */
    public static String jsonRpcRequest(final String method, final String... params) {
        var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
        var paramsArrayNode = mapper.createArrayNode();
        for (var param : params) {
            paramsArrayNode.add(param);
        }
        return mapper.createObjectNode().put("jsonrpc", "2.0").put("id", 1).put("method", method)
            .set("params", paramsArrayNode).toPrettyString();
    }

}
