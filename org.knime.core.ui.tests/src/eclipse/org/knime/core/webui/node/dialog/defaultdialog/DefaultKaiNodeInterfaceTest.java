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
 *   14 Apr 2025 (Seray Arslan, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.Node;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Seray Arslan, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class DefaultKaiNodeInterfaceTest {

    private static final PortType[] EXPLICIT_PORT_TYPES = new PortType[] {
        BufferedDataTable.TYPE,
        BufferedDataTable.TYPE,
        FlowVariablePortObject.TYPE,

    };
    private static final PortObjectSpec[] PORT_SPECS = new PortObjectSpec[]{
        new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec()),
        new DataTableSpec(new DataColumnSpecCreator("baz", StringCell.TYPE).createSpec()),
        FlowVariablePortObjectSpec.INSTANCE,
    };


    @Test
    void testGetConfigurePrompt() throws Exception {
        final var kaiNodeInterface = new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, TestSettings.class));
        final var mockSettings = mock(NodeAndVariableSettingsRO.class);
        final var settingsMap = Map.of(SettingsType.MODEL, mockSettings);
        final var nodeContainer = mockNodeContainer(EXPLICIT_PORT_TYPES, new ArrayList<>());

        NodeContext.pushContext(nodeContainer);
        try {

            final var configurePrompt = kaiNodeInterface.getConfigurePrompt(settingsMap, PORT_SPECS);
            // this model parameter is coming from TestSettings class
            assertThat(configurePrompt.toString()).contains("someModelSetting").contains("\"title\":\"Some Model Setting\"",
                "\"description\":\"Some Description\"");
        } finally {
            NodeContext.removeLastContext();
        }
    }


    @Test
    void testConstructSystemMessage() throws Exception {
        final var nodeContainer = mockNodeContainer(EXPLICIT_PORT_TYPES, new ArrayList<>());
        NodeContext.pushContext(nodeContainer);
        try {
            final var testSettings = """
                    {
                      "foo": "bar"
                    }
                    """;

            final var result = DefaultKaiNodeInterface.constructSystemMessage(testSettings, PORT_SPECS);
            assertThat(result).as("Unexpected settings").contains("# Current settings",
                testSettings, "# Inputs", ", "
                    + "(bar, String)]\n"
                    + ", (baz, String)]\n"
                    + "Flow Variable",
                    "# Flow Variables");


        } finally {
            NodeContext.removeLastContext();
        }
    }


    @Test
    final void testWriteFlowVarsAsJson() throws Exception {
        final var credentials = CredentialsStore.newCredentialsFlowVariable(
            "credentials flow variable", "varUsername", "varPassword", "varSecondFactor");

        final var flowVariables = List.of(new FlowVariable("foo", 42), credentials);
        final var flowObjectStack = FlowObjectStack.createFromFlowVariableList(flowVariables, new NodeID(0));

        final var jsonNode = DefaultKaiNodeInterface.writeFlowVarsAsJson(flowObjectStack);
        assertInstanceOf(ObjectNode.class, jsonNode);
        assertThat(jsonNode.has("flow_variables"));
        ArrayNode flowVars = (ArrayNode)jsonNode.get("flow_variables");
        assertEquals(flowVariables.size() + 1, flowVars.size());
        final var credentialsFlowVar = flowVars.get(0);
        assertEquals("credentials flow variable", credentialsFlowVar.get("name").asText());
        final var fooFlowVar = flowVars.get(1);
        assertEquals("foo", fooFlowVar.get("name").asText());
        final var workspaceFlowVar = flowVars.get(2);
        assertEquals("knime.workspace", workspaceFlowVar.get("name").asText());

    }


    @Test
    void testWriteInputFlowVarsAsJson() throws Exception {
        final var flowVariables = List.of(new FlowVariable("inputVar", "inputValue"));
        final var nodeContainer = mockNodeContainer(EXPLICIT_PORT_TYPES, flowVariables);

        NodeContext.pushContext(nodeContainer);
        try {
            final var inputJson = DefaultKaiNodeInterface.writeInputFlowVarsAsJson(nodeContainer);
            assertInstanceOf(ObjectNode.class, inputJson);
            assertThat(inputJson.has("flow_variables")).isTrue();

            final ArrayNode flowVars = (ArrayNode) inputJson.get("flow_variables");
            assertThat(flowVars).isNotEmpty();
            final var inputVar = flowVars.get(0);
            assertEquals("inputVar", inputVar.get("name").asText());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    @Test
    void testWriteOutputFlowVarsAsJson() throws Exception {
        final var flowVariables = List.of(new FlowVariable("outputVar", "outputValue"));
        final var nodeContainer = mockNodeContainer(EXPLICIT_PORT_TYPES, flowVariables);

        NodeContext.pushContext(nodeContainer);
        try {
            final var outputJson = DefaultKaiNodeInterface.writeOutputFlowVarsAsJson(nodeContainer);
            assertInstanceOf(ObjectNode.class, outputJson);
            assertThat(outputJson.has("flow_variables")).isTrue();

            final ArrayNode flowVars = (ArrayNode) outputJson.get("flow_variables");
            assertThat(flowVars).isNotEmpty();
            final var outputVar = flowVars.get(0);
            assertEquals("outputVar", outputVar.get("name").asText());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private static NodeContainer mockNodeContainer(final PortType[] explicitPortTypes, final List<FlowVariable> flowVariables) {
        final var nodeContainer = Mockito.mock(NativeNodeContainer.class);
        final var port = Mockito.mock(NodeOutPort.class);

        // add ports
        when(nodeContainer.getNrInPorts()).thenReturn(explicitPortTypes.length + 1);
        when(nodeContainer.getInPort(0)).thenReturn(new NodeInPort(0, FlowVariablePortObject.TYPE));

        when(nodeContainer.getNrOutPorts()).thenReturn(explicitPortTypes.length + 2);
        when(nodeContainer.getOutPort(0)).thenReturn(port);


        for (int i = 0; i < explicitPortTypes.length; i++) {
            when(nodeContainer.getInPort(i + 1)).thenReturn(new NodeInPort(i + 1, explicitPortTypes[i]));
        }
        when(nodeContainer.getOutputType(0)).thenReturn(FlowVariablePortObject.TYPE);

        // set flow object stack
        final FlowObjectStack flowObjectStack = FlowObjectStack.createFromFlowVariableList(flowVariables, new NodeID(0));
        when(nodeContainer.getFlowObjectStack()).thenReturn(flowObjectStack);
        when(nodeContainer.getOutPort(0).getFlowObjectStack()).thenReturn(flowObjectStack);

        // mock node
        final Node mockNode = Mockito.mock(Node.class);
        when(nodeContainer.getNode()).thenReturn(mockNode);

        when(nodeContainer.getParent()).thenReturn(Mockito.mock(WorkflowManager.class));

        return nodeContainer;

    }

    static final class TestSettings implements DefaultNodeSettings {
        @Widget(title = "Some Model Setting", description = "Some Description")
        int m_someModelSetting;

    }
}
