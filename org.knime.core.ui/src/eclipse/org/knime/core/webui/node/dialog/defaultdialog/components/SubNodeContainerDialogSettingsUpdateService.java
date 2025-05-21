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
 *   May 21, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.components;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DataServiceRequestHandler;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DialogSettingsUpdateService;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Result;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.ErrorHandlingSingleton;

/**
 * RPC service enabling settings updates in sub node container dialog.
 *
 * @author Paul Bärnreuther
 */
public class SubNodeContainerDialogSettingsUpdateService implements DialogSettingsUpdateService {
    private final Supplier<Collection<DialogElementRendererSpec>> m_settingsService;

    private final DataServiceRequestHandler m_requestHandler;

    private SubNodeContainerTriggerInvocationHandler m_triggerInvocationHandler;

    /**
     * @param rendererSupplier that is used to access the loaded dialog representations
     */
    public SubNodeContainerDialogSettingsUpdateService(
        final Supplier<Collection<DialogElementRendererSpec>> rendererSupplier) {
        m_settingsService = rendererSupplier;
        m_requestHandler = new DataServiceRequestHandler();
    }

    static DefaultNodeSettingsContext createContext() {
        return DefaultNodeSettings.createDefaultNodeSettingsContext(DataServiceContext.get().getInputSpecs());
    }

    SubNodeContainerTriggerInvocationHandler getTriggerInvocationHandler() {
        if (m_triggerInvocationHandler == null) {
            m_triggerInvocationHandler =
                new SubNodeContainerTriggerInvocationHandler(m_settingsService, createContext());
        }
        return m_triggerInvocationHandler;
    }

    @Override
    public Result<?> update2(final String widgetId, final Trigger trigger,
        final Map<String, List<IndexedValue<String>>> rawDependencies) throws InterruptedException, ExecutionException {
        ErrorHandlingSingleton.reset();
        final var triggerInvocationHandler = getTriggerInvocationHandler();
        return m_requestHandler.handleRequest(widgetId,
            () -> triggerInvocationHandler.trigger(trigger, rawDependencies));
    }

}
