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
 *   3 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.textView;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.view.NodeTableView;
import org.knime.core.webui.node.view.NodeView;
import org.knime.core.webui.node.view.textView.data.TextViewDataService;
import org.knime.core.webui.node.view.textView.data.TextViewDataServiceImpl;
import org.knime.core.webui.node.view.textView.data.TextViewInitialData;
import org.knime.core.webui.page.Page;

/**
 * A {@link NodeView} implementation for displaying tables.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class TextViewView implements NodeTableView {
    private TextViewViewSettings m_settings;
    private NodeContainer m_nc;

    /**
     * @param nc
     */
    public TextViewView(final NodeContainer nc) {
        m_nc = nc;
    }

    @Override
    public Page getPage() {
        return TextViewUtil.PAGE;
    }

    @Override
    public Optional<InitialDataService<TextViewInitialData>> createInitialDataService() {
        if (m_settings == null) {
            m_settings = new TextViewViewSettings();
        }
        return Optional.of(TextViewUtil.createInitialDataService(() -> m_settings, m_nc));
    }

    @Override
    public Optional<RpcDataService> createRpcDataService() {
        return Optional.of(createRpcDataService(new TextViewDataServiceImpl(m_settings, m_nc)));
    }

    /**
     * @param tableViewDataService
     * @param tableId
     * @return a new table view data service instance
     */
    public static RpcDataService createRpcDataService(final TextViewDataService textViewDataService) {
        return RpcDataService.builder(textViewDataService).build();
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) {
        try {
            m_settings = DefaultNodeSettings.loadSettings(settings, TextViewViewSettings.class);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("The settings should have been validated first.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <D> Optional<ApplyDataService<D>> createApplyDataService() {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

}
