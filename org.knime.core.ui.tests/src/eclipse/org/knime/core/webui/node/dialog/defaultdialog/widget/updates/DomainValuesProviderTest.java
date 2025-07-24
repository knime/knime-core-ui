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
 *   4 Apr 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.updates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DomainValuesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.ErrorHandlingSingleton;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;

/**
 *
 * @author Robin Gerling
 */
class DomainValuesProviderTest {

    static final String testColumn = "colName";

    static class DomainValuesProviderTester implements DomainValuesProvider {

        @Override
        public String getSelectedColumn() {
            return testColumn;
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            // Do nothing in this test
        }
    }

    @Test
    void testDomainValuesProvider() throws WidgetHandlerException {

        final var rows = List.of("row1", "row2");
        final var domain = rows.stream().map(StringCell::new).collect(Collectors.toSet());
        final var colDomain = new DataColumnDomainCreator(domain).createDomain();

        final var colSpecCreator = new DataColumnSpecCreator(testColumn, StringCell.TYPE);
        colSpecCreator.setDomain(colDomain);
        final var colSpec = colSpecCreator.createSpec();

        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{new DataTableSpec(//
                new DataColumnSpec[]{colSpec} //
            )}, null, null);

        final var domainChoicesStateProviderTester = new DomainValuesProviderTester();

        assertThat(domainChoicesStateProviderTester.computeState(context)).hasSize(2);
    }

    @Test
    void testDomainValuesProviderThrowsException() {

        final var colSpecCreator = new DataColumnSpecCreator(testColumn, StringCell.TYPE);
        final var colSpec = colSpecCreator.createSpec();

        final var context = NodeParametersInputImpl.createDefaultNodeSettingsContext(
            new PortType[]{BufferedDataTable.TYPE}, new PortObjectSpec[]{new DataTableSpec(//
                new DataColumnSpec[]{colSpec} //
            )}, null, null);

        final var domainChoicesStateProviderTester = new DomainValuesProviderTester();

        assertThat(domainChoicesStateProviderTester.computeState(context)).isEmpty();
        ;
        final var messages = ErrorHandlingSingleton.getErrorMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains(
            "No column domain values present for column \"colName\". Consider using a Domain Calculator node.");
    }
}
