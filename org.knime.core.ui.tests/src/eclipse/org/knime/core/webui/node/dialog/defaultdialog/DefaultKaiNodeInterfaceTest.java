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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface.ConfigurePrompt;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Seray Arslan, KNIME GmbH, Konstanz, Germany
 */
final class DefaultKaiNodeInterfaceTest {
    // TODO retrieve or populate?
    final String currentSettings = "{}";
    final String outputSchema = "{}";

    final PortObjectSpec[] specs = new PortObjectSpec[]{
        new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec()),
        new DataTableSpec(new DataColumnSpecCreator("baz", StringCell.TYPE).createSpec())
    };


    private final FlowVariable credentialsFlowVariable = CredentialsStore.newCredentialsFlowVariable(
        "credentials flow variable", "varUsername", "varPassword", "varSecondFactor");

    private final List<FlowVariable> flowVariables = new ArrayList<>();

    private final NodeInPort flowVarPort = new NodeInPort(0, FlowVariablePortObject.TYPE);
    private final NodeInPort tablePort = new NodeInPort(1, BufferedDataTable.TYPE);

    private final PortType[] explicitPortTypes = new PortType[] {
        flowVarPort.getPortType(),
        tablePort.getPortType()
    };

    @Test
    void testGetConfigurePrompt() throws Exception {
        var kaiNodeInterface = new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, TestSettings.class));
        flowVariables.add(credentialsFlowVariable);

        NodeContainer nc = mockNodeContainer(explicitPortTypes, flowVariables);

        NodeContext.pushContext(nc);
        try {
            var systemMessage = DefaultKaiNodeInterface.constructSystemMessage(currentSettings, specs);

            var configurePrompt = new ConfigurePrompt(systemMessage, JsonFormsDataUtil.getMapper().writeValueAsString(outputSchema));
            assertTrue(configurePrompt.toString().contains("outputSchema"));
        } finally {
            NodeContext.removeLastContext();
        }
    }


    @Test
    void testConstructSystemMessage() throws Exception {

        flowVariables.add(credentialsFlowVariable);

        NodeContainer nc = mockNodeContainer(explicitPortTypes, flowVariables);

        NodeContext.pushContext(nc);
        try {
            String result = DefaultKaiNodeInterface.constructSystemMessage(currentSettings, specs);

            // TODO check the column name and types and flow var name and types
            assertTrue(result.contains("# Current settings"));
            assertTrue(result.contains(currentSettings));
            assertTrue(result.contains("# Inputs"));
            assertTrue(result.contains("# Flow Variables"));


        } finally {
            NodeContext.removeLastContext();
        }
    }


    @Test
    void testGetFlowVariables() throws Exception {

        final FlowVariable credentialsFlowVariable = CredentialsStore.newCredentialsFlowVariable("credentials flow variable",
            "varUsername", "varPassword", "varSecondFactor");
        List<FlowVariable> flowVariables = new ArrayList<>();
        flowVariables.add(new FlowVariable("foo", 42));
        flowVariables.add(credentialsFlowVariable);
        var flowObjectStack = FlowObjectStack.createFromFlowVariableList(flowVariables, new NodeID(0));

        var jsonNode = DefaultKaiNodeInterface.writeFlowVarsAsJson(flowObjectStack);
        assertInstanceOf(ObjectNode.class, jsonNode);
        assertTrue(jsonNode.has("flow_variables"));
        ArrayNode flowVars = (ArrayNode)jsonNode.get("flow_variables");
        assertEquals(flowVariables.size() + 1, flowVars.size());
        var credentialsFlowVar = flowVars.get(0);
        assertEquals("credentials flow variable", credentialsFlowVar.get("name").asText());
        assertEquals("varUsername", credentialsFlowVar.get("value").get("username").asText());
        assertEquals("***", credentialsFlowVar.get("value").get("password").asText());
        var fooFlowVar = flowVars.get(1);
        assertEquals("foo", fooFlowVar.get("name").asText());
        assertEquals(42, fooFlowVar.get("value").asInt());
        // don't check value of workspace flow var because it is different depending on where the test is run
        var workspaceFlowVar = flowVars.get(2);
        assertEquals("knime.workspace", workspaceFlowVar.get("name").asText());

    }

    private static NodeContainer mockNodeContainer(final PortType[] explicitPortTypes, final List<FlowVariable> flowVariables) {
        var nc = Mockito.mock(NativeNodeContainer.class);
        when(nc.getNrInPorts()).thenReturn(explicitPortTypes.length + 1);
        when(nc.getInPort(0)).thenReturn(new NodeInPort(0, FlowVariablePortObject.TYPE));
        for (int i = 0; i < explicitPortTypes.length; i++) {
            when(nc.getInPort(i + 1)).thenReturn(new NodeInPort(i + 1, explicitPortTypes[i]));
        }
        when(nc.getOutputType(0)).thenReturn(FlowVariablePortObject.TYPE);
        FlowObjectStack flowObjectStack = FlowObjectStack.createFromFlowVariableList(flowVariables, new NodeID(0));
        when(nc.getFlowObjectStack()).thenReturn(flowObjectStack);
        return nc;
    }

    static final class TestSettings implements DefaultNodeSettings {
        // TODO add some test settings
    }


}
