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
 *   Jul 10, 2025 (marcbux): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.data.util.InputPortUtil;

/**
 * A context that holds any available information that might be relevant for creating a new instance of
 * {@link NodeParameters}.
 */
public final class DefaultNodeSettingsContext {

    private final PortType[] m_inTypes;

    private final PortObjectSpec[] m_specs;

    private final FlowObjectStack m_stack;

    private final CredentialsProvider m_credentialsProvider;

    private final PortObject[] m_inputPortObjects;

    private final DialogNode m_dialogNode;

    DefaultNodeSettingsContext(final PortType[] inTypes, final PortObjectSpec[] specs, final FlowObjectStack stack,
        final CredentialsProvider credentialsProvider, final PortObject[] inputPortObjects,
        final DialogNode dialogNode) {
        m_inTypes = inTypes;
        m_specs = specs;
        m_stack = stack;
        m_credentialsProvider = credentialsProvider;
        m_inputPortObjects = inputPortObjects;
        m_dialogNode = dialogNode;
    }

    DefaultNodeSettingsContext(final PortType[] inTypes, final PortObjectSpec[] specs, final FlowObjectStack stack,
        final CredentialsProvider credentialsProvider, final PortObject[] inputPortObjects) {
        this(inTypes, specs, stack, credentialsProvider, inputPortObjects, null);
    }

    DefaultNodeSettingsContext(final PortType[] inTypes, final PortObjectSpec[] specs, final FlowObjectStack stack,
        final CredentialsProvider credentialsProvider) {
        this(inTypes, specs, stack, credentialsProvider, null, null);
    }

    /**
     * Widens scope of constructor of {@link DefaultNodeSettingsContext}. Only used in tests.
     */
    @SuppressWarnings("javadoc")
    public static DefaultNodeSettingsContext createDefaultNodeSettingsContext(final PortType[] inPortTypes,
        final PortObjectSpec[] specs, final FlowObjectStack stack, final CredentialsProvider credentialsProvider) {
        return new DefaultNodeSettingsContext(inPortTypes, specs, stack, credentialsProvider, null, null);
    }

    /**
     * Widens scope of constructor of {@link DefaultNodeSettingsContext}. Only used in tests.
     */
    @SuppressWarnings("javadoc")
    public static DefaultNodeSettingsContext createDefaultNodeSettingsContext(final PortType[] inPortTypes,
        final PortObjectSpec[] specs, final FlowObjectStack stack, final CredentialsProvider credentialsProvider,
        final PortObject[] inputPortObjects) {
        return new DefaultNodeSettingsContext(inPortTypes, specs, stack, credentialsProvider, inputPortObjects,
            null);
    }

    /**
     * The node's input types. Not null and not containing null.
     *
     * @return the inTypes
     */
    public PortType[] getInPortTypes() {
        return m_inTypes;
    }

    /**
     * @return the input {@link PortObjectSpec PortObjectSpecs} of the node; NOTE: array of specs can contain
     *         {@code null} values, e.g., if input port is not connected!
     */
    public PortObjectSpec[] getPortObjectSpecs() {
        return m_specs;
    }

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link PortObjectSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<PortObjectSpec> getPortObjectSpec(final int portIndex) {
        return Optional.ofNullable(m_specs[portIndex]);
    }

    /**
     * @return the input {@link DataTableSpec DataTableSpecs} of the node; NOTE: array of specs can contain
     *         {@code null} values, e.g., if input port is not connected or inactive!
     * @throws ClassCastException if any of the node's input ports does not hold a {@link DataTableSpec}
     */
    public DataTableSpec[] getDataTableSpecs() {
        return Arrays.stream(m_specs).map(spec -> spec instanceof DataTableSpec dts ? dts : null)
            .toArray(DataTableSpec[]::new);
    }

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link DataTableSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<DataTableSpec> getDataTableSpec(final int portIndex) {
        return getPortObjectSpec(portIndex).map(DataTableSpec.class::cast);
    }

    /**
     * @return the input {@link PortObject}s of the node
     */
    public PortObject[] getInputPortObjects() {
        return m_inputPortObjects;
    }

    /**
     * @param portIndex
     * @return the {@link PortObject} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<PortObject> getInputPortObject(final int portIndex) {
        return Optional.ofNullable(m_inputPortObjects[portIndex]);
    }

    /**
     * @param portIndex the port for which to retrieve the object
     * @return the {@link DataTable} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<DataTable> getDataTable(final int portIndex) {
        return getInputPortObject(portIndex).map(DataTable.class::cast);
    }

    /**
     * @param name the name of the variable
     * @param type the {@link VariableType} of the variable
     * @param <T> the simple value type of the variable
     * @return the simple non-null value of the top-most variable with the argument name and type, if present,
     *         otherwise an empty {@link Optional}
     * @throws NullPointerException if any argument is null
     * @see FlowObjectStack#peekFlowVariable(String, VariableType)
     */
    public <T> Optional<T> peekFlowVariable(final String name, final VariableType<T> type) {
        return m_stack.peekFlowVariable(name, type).map(flowVariable -> flowVariable.getValue(type));
    }

    /**
     * @param types the {@link VariableType VariableTypes} of the requested {@link FlowVariable FlowVariables}
     * @return the non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @throws NullPointerException if the argument is null
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     */
    public Map<String, FlowVariable> getAvailableInputFlowVariables(final VariableType<?>... types) {
        Objects.requireNonNull(types, () -> "Variable types must not be null.");
        return Collections.unmodifiableMap(Optional.ofNullable(m_stack)
            .map(stack -> stack.getAvailableFlowVariables(types)).orElse(Collections.emptyMap()));
    }

    /**
     * @return the names of the available flow variables or an empty array if there are no flow variables available
     */
    public String[] getAvailableFlowVariableNames() {
        return m_stack != null ? m_stack.getAllAvailableFlowVariables().keySet().toArray(String[]::new)
            : new String[0];
    }

    /**
     * @param name the name of a flow variable
     * @return the associated flow variable if it exists
     */
    public Optional<FlowVariable> getFlowVariableByName(final String name) {
        if (m_stack == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(m_stack.getAllAvailableFlowVariables().get(name));
    }

    /**
     * @return the {@link CredentialsProvider} associated with the node. Can be empty, e.g., if the node is a
     *         component
     */
    public Optional<CredentialsProvider> getCredentialsProvider() {
        return Optional.ofNullable(m_credentialsProvider);
    }

    /**
     * Getter for the {@link DialogNode} of a configuration node.
     *
     * @return the dialogNode
     */
    public DialogNode getDialogNode() {
        CheckUtils.checkNotNull(m_dialogNode, "No dialog node is available in this context. "
            + "This method should be called only for configurations.");
        return m_dialogNode;
    }

    /**
     * Method to create a new {@link DefaultNodeSettingsContext} from input {@link PortObjectSpec PortObjectSpecs}.
     *
     * @param specs the non-null specs with which to create the schema
     * @return the newly created context
     * @throws NullPointerException if the argument is null
     */
    public static DefaultNodeSettingsContext createDefaultNodeSettingsContext(final PortObjectSpec[] specs) {
        Objects.requireNonNull(specs, () -> "Port object specs must not be null.");
        final var nodeContext = NodeContext.getContext();
        if (nodeContext == null) {
            // can only happen during tests
            return new DefaultNodeSettingsContext(fallbackPortTypesFor(specs), specs, null, null, null, null);
        }
        final var nc = nodeContext.getNodeContainer();
        final CredentialsProvider credentialsProvider;
        final PortType[] inPortTypes;
        DialogNode dialogNode = null;
        if (nc instanceof NativeNodeContainer nnc) {
            credentialsProvider = nnc.getNode().getCredentialsProvider();
            // skip hidden flow variable input (mickey mouse ear) - not exposed to node implementation
            inPortTypes = IntStream.range(1, nnc.getNrInPorts()).mapToObj(nnc::getInPort).map(NodeInPort::getPortType)
                .toArray(PortType[]::new);
            if (nnc.getNode().getNodeModel() instanceof DialogNode model) {
                dialogNode = model;
            }
        } else {
            credentialsProvider = null;
            inPortTypes = fallbackPortTypesFor(specs);
        }

        final var inPortObjects = nc.getParent() == null // This function is used by tests that mock the container
            ? new PortObject[0] // When mocked the container is not a child of a workflow manager
            : InputPortUtil.getInputPortObjectsExcludingVariablePort(nc);

        return new DefaultNodeSettingsContext(inPortTypes, specs, nc.getFlowObjectStack(), credentialsProvider,
            inPortObjects, dialogNode);
    }

    private static PortType[] fallbackPortTypesFor(final PortObjectSpec[] specs) {
        return IntStream.range(0, specs.length).mapToObj(i -> PortObject.TYPE).toArray(PortType[]::new);
    }
}