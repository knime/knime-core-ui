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
 *   Sep 16, 2025 (benjaminwilhelm): created
 */
package org.knime.core.webui.node.dialog.scripting;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.RpcDataService.RpcDataServiceBuilder;
import org.knime.core.webui.page.Page;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.persistence.Persist;

/**
 * The default scripting editor dialog with a centered scripting editor and additional widgets on the right side.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @since 5.10
 */
public abstract class AbstractDefaultScriptingNodeDialog extends AbstractScriptingNodeDialog {

    /**
     * A static completion item for the autocompletion dialog in the editor
     * <p>
     * Items are displayed as either functions or constants based on whether arguments are provided:
     * <ul>
     * <li><b>Function</b>: Include <code>arguments</code> (comma-separated) and optionally <code>returnType</code>
     * <ul>
     * <li>Example: <code>name: "get_data"</code>, <code>arguments: "port, index"</code>,
     * <code>returnType: "DataFrame"</code></li>
     * <li>Displays as: <code>get_data(port, index) -&gt; DataFrame</code></li>
     * </ul>
     * </li>
     * <li><b>Constant/Variable</b>: Omit <code>arguments</code>, optionally include <code>returnType</code>
     * <ul>
     * <li>Example: <code>name: "knime_context"</code>, <code>returnType: "Context"</code></li>
     * <li>Displays as: <code>knime_context: Context</code></li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param name The name to display and insert (required)
     * @param arguments Comma-separated parameter names for functions (optional)
     * @param description Help text shown in completion details, supports HTML (required)
     * @param returnType Return type annotation displayed in the completion list (optional)
     */
    public record StaticCompletionItem(String name, String arguments, String description, String returnType) {
    }

    private final DefaultScriptingNodeScriptingService m_scriptingService;

    /**
     * Create a default scripting editor dialog with the given model settings. The settings class must have a script
     * field of type String. The field name must match the {@code mainScriptConfigKey} specified in the initial data
     * returned by {@link #getInitialData(NodeContext)}. This setting will be shown in the scripting editor. Refrain
     * from using the {@link Persist} annotation. Other settings should use widgets and they are displayed to the right
     * of the scripting editor.
     *
     * @param modelSettings the model settings class
     */
    protected AbstractDefaultScriptingNodeDialog(final Class<? extends NodeParameters> modelSettings) {
        super(modelSettings);
        m_scriptingService = new DefaultScriptingNodeScriptingService();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Additional to the common initial data the initial data for the default scripting dialog supports:
     * <ul>
     * <li>"language" (required): the scripting language for Monaco (e.g. "sql", "python", "r", ...)</li>
     * <li>"fileName" (required): the file name for the script with an appropriate file ending</li>
     * <li>"staticCompletionItems" (optional): completion items of the type {@link StaticCompletionItem}</li>
     * </ul>
     */
    @Override
    protected abstract GenericInitialDataBuilder getInitialData(NodeContext context);

    @Override
    public Page getPage() {
        return Page.create() //
            .fromFile() //
            .bundleClass(AbstractDefaultScriptingNodeDialog.class) //
            .basePath("js-src/dist-scripting-editor/") //
            .relativeFilePath("index.html") //
            .addResourceDirectory("assets");
    }

    @Override
    protected RpcDataServiceBuilder getDataServiceBuilder(final NodeContext context) {
        return RpcDataService.builder() //
            .addService("ScriptingService", m_scriptingService.getJsonRpcService()) //
            .onDeactivate(m_scriptingService::onDeactivate);
    }
}
