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
 *   Apr 9, 2024 (hornm): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.testing.util.TableTestUtil;

/**
 * Tests for {@link NodeParametersInput}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeParametersInputTest {

    /**
     * Widens scope of constructor of {@link NodeParametersInput}. Only used in tests.
     */
    @SuppressWarnings("javadoc")
    public static final NodeParametersInput createDefaultNodeSettingsContext(final PortType[] inPortTypes,
        final PortObjectSpec[] specs, final FlowObjectStack stack, final CredentialsProvider credentialsProvider) {
        return new NodeParametersInputImpl(inPortTypes, specs, stack, credentialsProvider);
    }

    @Test
    void testGetDataTableSpecsWithInactivePortObjectSpec() {
        var context = new NodeParametersInputImpl(null,
            new PortObjectSpec[]{new DataTableSpec("test"), InactiveBranchPortObjectSpec.INSTANCE}, null, null);
        assertThrows(ClassCastException.class, () -> context.getInTableSpecs());
    }

    @Test
    void testGetDataTable() {
        var testTable = createTestTable();

        var context = new NodeParametersInputImpl(null, new PortObjectSpec[]{testTable.getDataTableSpec()}, null, null,
            new PortObject[]{testTable});
        var specs = context.getInTableSpecs();
        assertThat(specs).isEqualTo(new PortObjectSpec[]{testTable.getDataTableSpec()});
    }

    private static BufferedDataTable createTestTable() {

        var intCell = new IntCell(0);
        var testSpec = new TableTestUtil.SpecBuilder();
        testSpec.addColumn("testColumn", intCell.getType());

        final var builder = new TableTestUtil.TableBuilder(testSpec.build());

        var dataCells = new Object[]{intCell};
        builder.addRow(dataCells);

        return builder.build().get();
    }
}
