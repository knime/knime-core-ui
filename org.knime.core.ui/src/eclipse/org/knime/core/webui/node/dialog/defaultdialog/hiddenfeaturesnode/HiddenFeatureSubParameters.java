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
 *   Oct 16, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.hiddenfeaturesnode;

import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;

/**
 * Demo of {@link SubParameters}.
 *
 * @author Paul Bärnreuther
 */
class HiddenFeatureSubParameters implements NodeParameters {

    enum SomeEnum {
            OPTION_A, OPTION_B, OPTION_C
    }

    interface SomeSubParameters {
    }

    interface ShowSubParams extends ParameterReference<Boolean> {
    }

    static final class ShowSubParamsProvider implements StateProvider<Boolean> {

        private Supplier<Boolean> m_showSubParams;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_showSubParams = initializer.computeFromValueSupplier(ShowSubParams.class);

        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return m_showSubParams.get();
        }
    }

    @Widget(title = "Show sub parameters", description = "Check to show the sub parameters")
    @ValueReference(ShowSubParams.class)
    boolean m_showSubParams = true;

    @SubParameters(subLayoutRoot = SomeSubParameters.class, showSubParametersProvider = ShowSubParamsProvider.class)
    @Widget(title = "Parent field",
        description = "Click the button on the right to get to the sub parameters of this field.")
    SomeEnum m_enumParam = SomeEnum.OPTION_A;

    @Layout(SomeSubParameters.class)
    @Widget(title = "A String Parameter", description = "Just a string parameter")
    String m_stringParam = "default value";

    @Layout(SomeSubParameters.class)
    @Widget(title = "A Boolean Parameter", description = "Just a boolean parameter")
    boolean m_booleanParam = true;

}
