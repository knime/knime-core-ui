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
import java.util.function.Supplier;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to provide the data required to initialize an UI extension.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @param <D> the data type this initial data service returns
 *
 * @since 4.5
 */
public final class InitialDataService<D> extends AbstractDataService {

    private final Supplier<D> m_dataSupplier;

    private final ObjectMapper m_mapper = ObjectMapperUtil.getInstance().getObjectMapper();

    private Serializer<D> m_serializer;

    private final NodeContainer m_nc;

    /**
     * @param dataSupplier
     */
    private InitialDataService(final InitialDataServiceBuilder<D> builder) {
        super(builder);
        m_dataSupplier = builder.m_dataSupplier;
        if (builder.m_serializer == null) {
            m_serializer = obj -> {
                if (obj instanceof String s) {
                    return s;
                } else {
                    return m_mapper.writeValueAsString(obj);
                }
            };
        } else {
            m_serializer = builder.m_serializer;
        }
        m_nc = DataServiceUtil.getNodeContainerFromContext();
    }

    /**
     * @return the initial data serialized into a string
     */
    public String getInitialData() {
        if (m_nc != null) {
            NodeContext.pushContext(m_nc);
        }
        try {
            final var root = m_mapper.createObjectNode();
            // Since the DataServiceContext is public API, warning messages could have been wrongfully added to it.
            // We clear the context here to make sure there are no "stale" warning messages.
            DataServiceContext.init(m_nc);
            final var dataString = m_serializer.serialize(m_dataSupplier.get());
            try { // NOSONAR
                root.set("result", m_mapper.readTree(dataString));
            } catch (StreamConstraintsException ex) {
                NodeLogger.getLogger(getClass()).error(ex);
                return m_mapper.createObjectNode()
                    .set("internalError",
                        m_mapper.valueToTree(
                            new InitialDataInternalError(new IOException(
                                "The initial value for this view is too large to process. "
                                + "Please verify if the output is expected to be this large.",
                                ex))))
                    .toString();
            } catch (JsonProcessingException ex) { // NOSONAR
                // if it couldn't be parsed as a json, just return the string itself
                root.put("result", dataString);
            }
            // We have to get the DataServiceContext again here, since the context may have changed since (or as a
            // consequence of) clearing it
            final var warningMessages = DataServiceContext.get().getWarningMessages();
            if (warningMessages != null && warningMessages.length > 0) {
                root.set("warningMessages", m_mapper.valueToTree(warningMessages));
            }
            return root.toString();
        } catch (DataServiceException e) {
            return m_mapper.createObjectNode().set("userError", m_mapper.valueToTree(new InitialDataUserError(e)))
                .toString();
        } catch (Throwable t) { // NOSONAR
            final var errorMessage = m_mapper.createObjectNode()
                .set("internalError", m_mapper.valueToTree(new InitialDataInternalError(t))).toString();
            NodeLogger.getLogger(getClass()).error(errorMessage);
            return errorMessage;
        } finally {
            DataServiceContext.remove();
            if (m_nc != null) {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * @param <D>
     * @param dataSupplier supplies the initial data. A {@link DataServiceContext} is available whenever the supplier is
     *            being called.
     * @return the builder to create an {@link InitialDataService}-instance
     */
    public static <D> InitialDataServiceBuilder<D> builder(final Supplier<D> dataSupplier) {
        return new InitialDataServiceBuilder<>(dataSupplier);
    }

    /**
     * The builder.
     *
     * @param <D>
     */
    public static final class InitialDataServiceBuilder<D> extends AbstractDataServiceBuilder {

        private Supplier<D> m_dataSupplier;

        private Serializer<D> m_serializer;

        private InitialDataServiceBuilder(final Supplier<D> dataSupplier) {
            m_dataSupplier = dataSupplier;
        }

        @Override
        public InitialDataServiceBuilder<D> onDispose(final Runnable dispose) {
            super.onDispose(dispose);
            return this;
        }

        @Override
        public InitialDataServiceBuilder<D> onDeactivate(final Runnable deactivate) {
            super.onDeactivate(deactivate);
            return this;
        }

        /**
         * @param serializer a custom serializer to turn the data object into a string
         * @return this builder
         */
        public InitialDataServiceBuilder<D> serializer(final Serializer<D> serializer) {
            m_serializer = serializer;
            return this;
        }

        /**
         * @return a new instance
         */
        public InitialDataService<D> build() {
            return new InitialDataService<>(this);
        }

    }

    @SuppressWarnings("javadoc")
    @FunctionalInterface
    public interface Serializer<D> {
        String serialize(D obj) throws IOException;
    }

}
