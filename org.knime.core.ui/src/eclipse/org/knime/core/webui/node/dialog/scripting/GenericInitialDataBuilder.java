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
package org.knime.core.webui.node.dialog.scripting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.node.workflow.NodeContext;

/**
 * A generic initial data service for the scripting editor, accessed on the frontend via the scripting editor's
 * initialDataService. This is a builder-style class that allows you to add data suppliers that provide data to the
 * frontend in a highly flexible composable way.
 *
 * Each supplier has an associated name, which is the key that it will have when it is sent to the frontend as JSON.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @noreference This class is not intended to be referenced by clients.
 */
public final class GenericInitialDataBuilder {

    /**
     * Represents a section in a multi-section script editor. A section can be either editable (linked to a settings
     * config key) or read-only (containing template text).
     *
     * @param isEditable whether this section is editable by the user
     * @param contentOrConfigKey if editable, the config key to load/save the content; if read-only, the template text
     *            to display
     * @since 5.4
     */
    public record ScriptSection(boolean isEditable, String contentOrConfigKey) {
    }

    private Map<String, Supplier<Object>> m_dataSuppliers = new HashMap<>();

    /**
     * Add a new {@link Supplier} to this service.
     *
     * @param key the key under which the data will be sent to the frontend
     * @param supplier the supplier that provides the data
     * @return this, for builder-style chaining
     */
    public GenericInitialDataBuilder addDataSupplier(final String key, final Supplier<Object> supplier) {
        if (m_dataSuppliers.containsKey(key)) {
            throw new IllegalArgumentException("Data supplier with key '%s' already exists.".formatted(key));
        }

        m_dataSuppliers.put(key, supplier);
        return this;
    }

    /**
     * Add script section data for multi-section script editors. This allows editors to have multiple editable
     * sections separated by read-only template text (e.g., Java class structure with editable imports, fields, and
     * body).
     *
     * @param sections list of script sections, alternating between editable and read-only sections
     * @return this, for builder-style chaining
     * @since 5.4
     */
    public GenericInitialDataBuilder addScriptSectionData(final List<ScriptSection> sections) {
        if (m_dataSuppliers.containsKey("scriptSections")) {
            throw new IllegalArgumentException("Script sections have already been added.");
        }
        
        m_dataSuppliers.put("scriptSections", () -> sections);
        return this;
    }

    /**
     * Build the initial data map using the given {@link Supplier}s.
     *
     * @return a map of the data that will be sent to the frontend
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Supplier<Object>> entry : m_dataSuppliers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
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
     * Create a {@link Supplier} that provides a summary of the current status of incoming Ports. Each port is
     * represented by an entry in the returning array.
     *
     * @param context the node context for the current node (see {@link NodeContext#getContext()})
     * @return the data supplier
     */
    public static Supplier<Object> createInputConnectionInfoSupplier(final NodeContext context) {
        return () -> new WorkflowControl(context.getNodeContainer()).getInputConnectionInfo();

    }

    /**
     * Create a {@link Supplier} that provides the input port configs.
     *
     * @param context the node context for the current node (see {@link NodeContext#getContext()})
     * @return the data supplier
     */
    public static Supplier<Object> createInputPortConfigsSupplier(final NodeContext context) {
        return () -> new PortConfigs(context.getNodeContainer());
    }

    /**
     * Create a {@link Supplier} that provides the KAI configuration.
     *
     * @return the data supplier
     */
    public static Supplier<Object> createKAIConfigSupplier() {
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
}
