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
 *   Oct 10, 2025 (Paul Bärnreuther): created
 */
package org.knime.node.parameters.updates.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;
import org.knime.testing.util.TableTestUtil;

class ColumnNameAutoGuessValueProviderTest {

    static final class TestParameters implements NodeParameters {

        TestParameters() {
            // empty
        }

        TestParameters(final String initialColumName) {
            m_columnName = initialColumName;
        }

        interface ColumnNameSelfReference extends ParameterReference<String> {
        }

        @ValueProvider(StringColumnNameAutoGuessValueProvider.class)
        @ValueReference(ColumnNameSelfReference.class)
        String m_columnName;

        static final class StringColumnNameAutoGuessValueProvider extends ColumnNameAutoGuessValueProvider {

            StringColumnNameAutoGuessValueProvider() {
                super(ColumnNameSelfReference.class);
            }

            @Override
            protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
                return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, StringValue.class);
            }
        }

    }

    @Test
    void testColumnNameAutoGuessValueProvider() {
        final var inputSpec = TableTestUtil.createDefaultTestTable(0).get().getSpec();
        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{inputSpec}, null, null);

        final var simulator = new DialogUpdateSimulator(Map.of(SettingsType.MODEL, new TestParameters()), context);

        final var result = simulator.simulateAfterOpenDialog();

        final var valueUpdate = result.getValueUpdateAt("columnName");
        assertThat(valueUpdate).isEqualTo("string");

    }

    @Test
    void testColumnNameAutoGuessValueProviderDoesNotGuessIfPresent() {
        final var inputSpec = TableTestUtil.createDefaultTestTable(0).get().getSpec();
        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{inputSpec}, null, null);

        final var simulator =
            new DialogUpdateSimulator(Map.of(SettingsType.MODEL, new TestParameters("other_column")), context);

        final var result = simulator.simulateAfterOpenDialog();

        assertThrows(NullPointerException.class, () -> result.getValueUpdateAt("columnName"));
    }

}
