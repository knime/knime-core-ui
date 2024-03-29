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
 *   Apr 11, 2023 (hornm): created
 */
package org.knime.core.webui.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.webui.node.port.PortContext;

/**
 * Tests for {@link DataServiceUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class DataServiceUtilTest {

    @Test
    void testGetNodeContainerFromContext() {
        var nc = DataServiceUtil.getNodeContainerFromContext();
        assertThat(nc).isNull();

        var snc = mock(SingleNodeContainer.class);
        NodeContext.pushContext(snc);
        try {
            nc = DataServiceUtil.getNodeContainerFromContext();
        } finally {
            NodeContext.removeLastContext();
        }
        assertThat(nc).isSameAs(snc);

        var portMock = mock(NodeOutPort.class);
        when(portMock.getConnectedNodeContainer()).thenReturn(snc);
        PortContext.pushContext(portMock);
        try {
            nc = DataServiceUtil.getNodeContainerFromContext();
        } finally {
            PortContext.removeLastContext();
        }
        assertThat(nc).isSameAs(snc);
        verify(portMock).getConnectedNodeContainer();

        var inPortMock = mock(NodeInPort.class);
        PortContext.pushContext(inPortMock);
        try {
            assertThrows(UnsupportedOperationException.class,
                () -> DataServiceUtil.getNodeContainerFromContext());
        } finally {
            PortContext.removeLastContext();
        }
    }

}
