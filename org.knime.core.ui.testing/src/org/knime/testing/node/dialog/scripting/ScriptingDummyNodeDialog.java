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
package org.knime.testing.node.dialog.scripting;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DynamicParametersTriggerInvocationHandlerContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.NodeDialogServiceRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.validation.CustomValidationContext;
import org.knime.core.webui.node.dialog.scripting.GenericInitialDataBuilder;
import org.knime.core.webui.node.dialog.scripting.InputOutputModel;
import org.knime.core.webui.node.dialog.scripting.ScriptingDefaultNodeSettingsService;
import org.knime.core.webui.node.dialog.scripting.ScriptingService;
import org.knime.core.webui.node.dialog.scripting.WorkflowControl;
import org.knime.core.webui.page.Page;

/**
 * The dialog for the Dummy Scripting Node using the WebUI framework.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // WebUI API is still restricted
public class ScriptingDummyNodeDialog implements NodeDialog {

    private final Class<ScriptingDummyNodeSettings> m_modelSettings = ScriptingDummyNodeSettings.class;

    private final ScriptingDummyNodeScriptingService m_scriptingService;

    private final NodeDialogServiceRegistry m_serviceRegistry = new NodeDialogServiceRegistry(new FileSystemConnector(),
        new CustomValidationContext(), new DynamicParametersTriggerInvocationHandlerContext());

    ScriptingDummyNodeDialog() {
        m_scriptingService = new ScriptingDummyNodeScriptingService();
    }

    @Override
    public Page getPage() {
        return Page.create() //
            .fromFile() //
            .bundleClass(ScriptingService.class) // TODO use own class when resources are available
            .basePath("js-src/dist/") //
            .relativeFilePath("index.html") //
            .addResourceDirectory("assets") //
            .addResourceDirectory("monacoeditorwork");
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Set.of(SettingsType.MODEL);
    }

    @Override
    public NodeSettingsService getNodeSettingsService() {
        var workflowControl = new WorkflowControl(NodeContext.getContext().getNodeContainer());

        // TODO make input output items configurable

        var initialData = GenericInitialDataBuilder.createDefaultInitialDataBuilder(NodeContext.getContext()) //
            .addDataSupplier("inputObjects", Collections::emptyList) //
            .addDataSupplier("flowVariables", () -> {
                var flowVariables = Optional.ofNullable(workflowControl.getFlowObjectStack()) //
                    .map(stack -> stack.getAllAvailableFlowVariables().values()) //
                    .orElseGet(List::of);
                return InputOutputModel.flowVariables() //
                    .subItems(flowVariables, varType -> true) //
                    .build();
            }) //
            .addDataSupplier("outputObjects", Collections::emptyList) //
            .addDataSupplier("language", () -> "java") //
            .addDataSupplier("fileName", () -> "script.java") //
        ;

        return new ScriptingDefaultNodeSettingsService(initialData, m_modelSettings, m_serviceRegistry);
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        var serviceBuilder = RpcDataService.builder();
        DefaultNodeDialog.addDefaultNodeDialogRpcServices(serviceBuilder, Map.of(SettingsType.MODEL, m_modelSettings),
            m_serviceRegistry);

        return Optional.of(serviceBuilder //
            .addService("ScriptingService", m_scriptingService.getJsonRpcService()) //
            .onDeactivate(m_scriptingService::onDeactivate) //
            .onDeactivate(m_serviceRegistry::onDeactivateRpc) //
            .build());
    }

    @Override
    public boolean canBeEnlarged() {
        return true;
    }
}
