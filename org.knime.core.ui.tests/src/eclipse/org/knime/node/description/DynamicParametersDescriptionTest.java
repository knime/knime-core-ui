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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.node.testing.DefaultNodeTestUtil.complete;
import static org.knime.node.testing.DefaultNodeTestUtil.createStage;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.DefaultNode.RequireModel;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.testing.TestWithWorkflowManager;

class DynamicParametersDescriptionTest extends TestWithWorkflowManager {

    @Test
    void testParametersWithDynamicParameters() {
        final var nc = addNode(complete(createStage(RequireModel.class)//
            .model(m -> m//
                .parametersClass(ParametersWithDynamicParameters.class) //
                .configure((i, o) -> {
                    // No configure
                }) //
                .execute((i, o) -> {
                    // No execute
                }) //
            )));

        final var description = nc.getNode().invokeGetNodeDescription();

        assertThat(description.getDialogOptionGroups().get(0).getOptions()).hasSize(2);
        final var arrayOption = description.getDialogOptionGroups().get(0).getOptions().get(1);
        assertThat(arrayOption.getDescription()).contains("Outer description").contains("Element dynamic parameters");
    }

    interface MyDynamicParameters extends DynamicNodeParameters {

    }

    static final class TestDynamicParametersProvider
        implements DynamicParameters.DynamicParametersProvider<MyDynamicParameters> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public ClassIdStrategy<MyDynamicParameters> getClassIdStrategy() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public MyDynamicParameters computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            throw new UnsupportedOperationException("Not implemented");
        }

    }

    static final class ParametersWithDynamicParameters implements NodeParameters {

        @DynamicParameters(value = TestDynamicParametersProvider.class,
            widgetAppearingInNodeDescription = @Widget(title = "The dynamic parameters",
                description = "These parameters are dynamic!"))
        MyDynamicParameters m_dynamicParametersWithWidget;

        @DynamicParameters(TestDynamicParametersProvider.class)
        MyDynamicParameters m_dynamicParametersWithoutWidget;

        static final class ElementSettings implements NodeParameters {
            @DynamicParameters(value = TestDynamicParametersProvider.class,
                widgetAppearingInNodeDescription = @Widget(title = "Element dynamic parameters",
                    description = "These element parameters are dynamic!"))
            MyDynamicParameters m_elementDynamicParametersField;
        }

        @Widget(title = "Array containing dynamic parameters", description = "Outer description")
        ElementSettings[] m_arrayOfElementsWithDynamicParameters;

    }
}
