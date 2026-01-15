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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.data.RpcDataService.RpcDataServiceBuilder;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DynamicParametersTriggerInvocationHandlerContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.NodeDialogServiceRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.validation.CustomValidationContext;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.persistence.Persist;

/**
 * Abstract base class for scripting node dialogs that combine a code editor with {@link NodeParameters}-based settings.
 * Use this class when you need a custom frontend with custom components and logic.
 * <p>
 * Subclasses must implement {@link NodeDialog#getPage()} to provide their own frontend page. For a standard scripting
 * dialog that is fully configured via initial data without custom frontend code, use
 * {@link AbstractDefaultScriptingNodeDialog} instead.
 * <p>
 * <b>Requirements:</b>
 * <ul>
 * <li>Settings must have a String field for the script. Field name must match {@code mainScriptConfigKey} in initial
 * data. The script field should NOT use a {@link Persist} annotation.</li>
 * <li>{@link #getInitialData(NodeContext)} must provide: "mainScriptConfigKey", "inputObjects", "outputObjects",
 * "flowVariables"</li>
 * <li>{@link #getDataServiceBuilder(NodeContext)} must include a "ScriptingService" RPC service</li>
 * </ul>
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @since 5.10
 */
public abstract class AbstractScriptingNodeDialog implements NodeDialog {

    private final Class<? extends NodeParameters> m_modelSettings;

    private final NodeDialogServiceRegistry m_serviceRegistry = new NodeDialogServiceRegistry(new FileSystemConnector(),
        new CustomValidationContext(), new DynamicParametersTriggerInvocationHandlerContext());

    /**
     * Creates a scripting dialog with the given model settings class.
     *
     * @param modelSettings the model settings class defining the dialog configuration
     */
    protected AbstractScriptingNodeDialog(final Class<? extends NodeParameters> modelSettings) {
        m_modelSettings = modelSettings;
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Set.of(SettingsType.MODEL);
    }

    @Override
    public NodeSettingsService getNodeSettingsService() {
        return new DefaultScriptingNodeSettingsService(getInitialData(NodeContext.getContext()), m_modelSettings,
            m_serviceRegistry);
    }

    /**
     * Provides initial data for the scripting editor. Required entries:
     * <ul>
     * <li>"mainScriptConfigKey": Field name of the script setting (e.g., "script", "expression")</li>
     * <li>"inputObjects": Model describing available input objects</li>
     * <li>"outputObjects": Model describing expected output objects</li>
     * <li>"flowVariables": Model describing available flow variables</li>
     * </ul>
     *
     * @param context the node context
     * @return builder with initial data entries
     */
    protected abstract GenericInitialDataBuilder getInitialData(NodeContext context);

    /**
     * Provides RPC data services for frontend-backend communication. Must include a {@link ScriptingService.RpcService}
     * registered as "ScriptingService". Additional services can be added for node-specific functionality.
     *
     * @param context the node context
     * @return builder configured with RPC services
     */
    protected abstract RpcDataServiceBuilder getDataServiceBuilder(NodeContext context);

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        var serviceBuilder = getDataServiceBuilder(NodeContext.getContext());
        DefaultNodeDialog.addDefaultNodeDialogRpcServices(serviceBuilder, Map.of(SettingsType.MODEL, m_modelSettings),
            m_serviceRegistry);
        serviceBuilder.onDeactivate(m_serviceRegistry::onDeactivateRpc);
        return Optional.of(serviceBuilder.build());
    }

    @Override
    public boolean canBeEnlarged() {
        return true;
    }
}
