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
 *   Jul 13, 2020 (hornm): created
 */
package org.knime.core.webui.data.rpc;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Convenience specialization of {@link RpcClient} for the case where there is only node data service interface, which
 * the user then doesn't have to specify on every call.
 *
 * @param <S> The node data service interface, i.e., the methods offered by the node model to the node dialog/view to
 *            retrieve data.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 4.3
 * @deprecated can be removed since we don't need a java-based RpcClient anymore once the Remote Workflow Editor becomes
 *             obsolete
 */
@Deprecated(forRemoval = true)
public interface RpcSingleClient<S> extends RpcClient {

    /**
     * Used to call a method on the node model's data service interface and retrieve the result. Example usage:
     *
     * <pre>
     * RpcClient m_rpcClient = ...
     * Future&lt;List&lt;SomeType>> future =
     *      m_rpcClient.callServiceWithRes(nodeDataService -> nodeDataService.getSomeData(someParameter));
     * try {
     *     List&lt;SomeType> results = future.get(3, TimeUnit.SECONDS);
     * } catch (TimeoutException timeoutException) {
     *     ...
     * </pre>
     *
     * @param serviceEvaluator the service evaluator is given an implementation of the node model's data service. It
     *            then calls one of the methods on the node model's service interface and returns the result.
     *
     * @param <R> the result type of the invoked method.
     * @return a {@link Future} containing the result of the invoked method.
     */
    <R> Future<R> callServiceWithRes(Function<S, R> serviceEvaluator);

    @SuppressWarnings("unchecked")
    @Override
    default <S2, R> java.util.concurrent.Future<R> callServiceWithRes(final Class<S2> serviceInterface,
        final String serviceName, final Function<S2, R> serviceEvaluator) {
        return callServiceWithRes((Function<S, R>)serviceEvaluator);
    }

    /**
     * Similar to {@link #callServiceWithRes(Function)} but for void methods. For instance:
     * {@code Future<Void> future = m_rpcClient.callService(nodeDataService -> nodeDataService.sendSomeData(someData));}
     *
     * @param serviceConsumer used to invoke the method on the service interface
     * @return an empty future
     */
    Future<Void> callService(Consumer<S> serviceConsumer);

    @SuppressWarnings("unchecked")
    @Override
    default <S2> Future<Void> callService(final Class<S2> serviceInterface, final String serviceName,
        final Consumer<S2> serviceConsumer) {
        return callService((Consumer<S>)serviceConsumer);
    }

    /**
     * Gives direct access to the service implementation. Please note that a call to the service might block for a while
     * (in case the rpc request is send to the server). It is always advisable to run service calls in an extra thread
     * to not block the ui!
     *
     * @param serviceInterface the interface to get the service implementation for
     * @return the service implementation
     */
    S getService();

    @SuppressWarnings("unchecked")
    @Override
    default <S2> S2 getService(final Class<S2> serviceInterface, final String serviceName) {
        return (S2)getService();
    }


}
