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
 *   Nov 16, 2023 (benjamin): created
 */
package org.knime.scripting.editor;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.workflow.NodeContext;

/**
 * A generic initial data service for the scripting editor, accessed on the frontend via the scripting editor's
 * initialDataService. This is a builder-style class that allows you to add data suppliers that provide data to the
 * frontend in a highly flexible composable way.
 *
 * Each supplier has an associated name, which is the key that it will have when it is sent to the frontend as JSON.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class GenericInitialDataBuilder {

    private Map<String, DataSupplier> m_dataSuppliers = new HashMap<>();

    /**
     * Add a new {@link DataSupplier} to this service.
     *
     * @param key the key under which the data will be sent to the frontend
     * @param supplier the supplier that provides the data
     * @return this, for builder-style chaining
     */
    public GenericInitialDataBuilder addDataSupplier(final String key, final DataSupplier supplier) {
        if (m_dataSuppliers.containsKey(key)) {
            throw new IllegalArgumentException("Data supplier with key '%s' already exists.".formatted(key));
        }

        m_dataSuppliers.put(key, supplier);
        return this;
    }

    /**
     * Build the initial data map using the given {@link DataSupplier}s.
     *
     * @return a map of the data that will be sent to the frontend
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, DataSupplier> entry : m_dataSuppliers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getData());
        }

        return result;
    }

    /**
     * Create a default initial data service for a scripting editor. Includes: settings, input port configs, and
     * K-AI-related configuration: hubId, isLoggedIntoHub, isKaiEnabled.
     *
     * @param context the node context, used for getting the port configs
     * @return a new initial data service, which can be extended to include additional data suppliers
     */
    public static GenericInitialDataBuilder createDefaultInitialDataBuilder(final NodeContext context) {

        return new GenericInitialDataBuilder() //
            .addDataSupplier("inputPortConfigs", createInputPortConfigsSupplier(context)) //
            .addDataSupplier("inputConnectionInfo", createInputConnectionInfoSupplier(context)) //
            .addDataSupplier("kAiConfig", createKAIConfigSupplier());
    }

    /*
     * ------------------------------------------------------------------------
     * Some general data suppliers that will be used by most/all editors
     * ------------------------------------------------------------------------
     */

    /**
     * Create a {@link DataSupplier} that provides a summary of the current status of incoming Ports. Each port is
     * represented by an entry in the returning array.
     *
     * @param context the node context for the current node (see {@link NodeContext#getContext()})
     * @return the data supplier
     */
    public static DataSupplier createInputConnectionInfoSupplier(final NodeContext context) {
        return () -> {
            return new WorkflowControl(context.getNodeContainer()).getInputConnectionInfo();
        };
    }

    /**
     * Create a {@link DataSupplier} that provides the input port configs.
     *
     * @param context the node context for the current node (see {@link NodeContext#getContext()})
     * @return the data supplier
     */
    public static DataSupplier createInputPortConfigsSupplier(final NodeContext context) {
        return () -> new PortConfigs(context.getNodeContainer());
    }

    /**
     * Create a {@link DataSupplier} that provides the KAI configuration.
     *
     * @return the data supplier
     */
    public static DataSupplier createKAIConfigSupplier() {
        return () -> {
            Map<String, Object> result = new HashMap<>();

            var kaiHandler = CodeKaiHandlerDependency.getCodeKaiHandler();
            var projectId = CodeKaiHandlerDependency.getProjectId();
            result.put("hubId", kaiHandler.map(h -> h.getHubId()).orElse(null));
            result.put("loggedIntoHub", kaiHandler.map(h -> h.isLoggedIn(projectId)).orElse(false));
            result.put("isKaiEnabled", kaiHandler.map(h -> h.isKaiEnabled()).orElse(false));

            return result;
        };
    }

    /**
     * Used by the {@link GenericInitialDataBuilder} to supply data to the editor in a composable way.
     */
    @FunctionalInterface
    public static interface DataSupplier {

        /**
         * Return the data for which this supplier is responsible. The settings and specs are provided, but the supplier
         * may ignore them.
         *
         * @param settings the settings for the node, which may be from the file or overridden by flow variables.
         * @param specs the specs for the ports connected to the node, including the flow variables.
         * @return the data whatever data the supplier is responsible for, which will be serialised to JSON and then
         *         sent to the editor along with the data from the other suppliers.
         */
        Object getData();

    }
}
