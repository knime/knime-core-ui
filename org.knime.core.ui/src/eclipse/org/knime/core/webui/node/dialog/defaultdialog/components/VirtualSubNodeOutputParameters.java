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
 */
package org.knime.core.webui.node.dialog.defaultdialog.components;

import org.knime.core.webui.node.dialog.defaultdialog.components.VirtualSubNodeInputParameters.NonGlobalFlowVariableNamesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.components.VirtualSubNodeInputParameters.VariableFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * WebUI dialog settings for the Component Output node (VirtualSubNodeOutputNodeFactory). Similar to
 * {@link VirtualSubNodeInputParameters} but without the variable prefix option (which is not shown in the legacy output
 * dialog either).
 *
 * @author Paul Baernreuther
 */
public final class VirtualSubNodeOutputParameters implements NodeParameters {

    @Widget(title = "Output variables",
        description = "Choose variables from workflow to be visible outside the Component. "
            + "Note that the selection is applied to all output ports.")
    @Persistor(VariableFilterPersistor.class)
    @ChoicesProvider(NonGlobalFlowVariableNamesProvider.class)
    StringFilter m_variableFilter = new StringFilter();

    // The output dialog does not show a variable prefix UI (commented out in legacy dialog),
    // but the model still reads/writes the key, so we persist it as hidden.
    @Persist(configKey = "variable-prefix")
    String m_variablePrefix;

    // backward-compat fields (not shown in dialog but persisted for the model)
    @Persist(configKey = "port-names")
    String[] m_portNames = new String[0];

    @Persist(configKey = "port-descriptions")
    String[] m_portDescriptions = new String[0];

    @Persist(configKey = "allowOutputNodeToCompleteBeforeContent")
    boolean m_allowOutputNodeToCompleteBeforeContent;
}
