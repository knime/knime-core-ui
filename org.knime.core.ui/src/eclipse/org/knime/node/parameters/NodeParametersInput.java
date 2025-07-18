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
 *   Jul 18, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters;

import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * An input that holds any available information that might be relevant for creating a new instance of
 * {@link NodeParameters}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface NodeParametersInput {

    /**
     * The node's input types. Not null and not containing null.
     *
     * @return the inTypes
     */
    PortType[] getInPortTypes();

    /**
     * @return the input {@link PortObjectSpec PortObjectSpecs} of the node; NOTE: array of specs can contain
     *         {@code null} values, e.g., if input port is not connected!
     */
    PortObjectSpec[] getPortObjectSpecs();

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link PortObjectSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<PortObjectSpec> getPortObjectSpec(final int portIndex);

    /**
     * @return the input {@link DataTableSpec DataTableSpecs} of the node; NOTE: array of specs can contain {@code null}
     *         values, e.g., if input port is not connected or inactive!
     * @throws ClassCastException if any of the node's input ports does not hold a {@link DataTableSpec}
     */
    public DataTableSpec[] getDataTableSpecs();

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link DataTableSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<DataTableSpec> getDataTableSpec(final int portIndex);

    /**
     * @return the input {@link PortObject}s of the node
     */
    public PortObject[] getInputPortObjects();

    /**
     * @param portIndex
     * @return the {@link PortObject} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<PortObject> getInputPortObject(final int portIndex);

    /**
     * @param portIndex the port for which to retrieve the object
     * @return the {@link DataTable} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    public Optional<DataTable> getDataTable(final int portIndex);

    /**
     * @param name the name of the variable
     * @param type the {@link VariableType} of the variable
     * @param <T> the simple value type of the variable
     * @return the simple non-null value of the top-most variable with the argument name and type, if present, otherwise
     *         an empty {@link Optional}
     * @throws NullPointerException if any argument is null
     * @see FlowObjectStack#peekFlowVariable(String, VariableType)
     */
    public <T> Optional<T> peekFlowVariable(final String name, final VariableType<T> type);

    /**
     * @param types the {@link VariableType VariableTypes} of the requested {@link FlowVariable FlowVariables}
     * @return the non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @throws NullPointerException if the argument is null
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     */
    public Map<String, FlowVariable> getAvailableInputFlowVariables(final VariableType<?>... types);

    /**
     * @return the names of the available flow variables or an empty array if there are no flow variables available
     */
    public String[] getAvailableFlowVariableNames();

    /**
     * @param name the name of a flow variable
     * @return the associated flow variable if it exists
     */
    public Optional<FlowVariable> getFlowVariableByName(final String name);

    /**
     * @return the {@link CredentialsProvider} associated with the node. Can be empty, e.g., if the node is a component
     */
    public Optional<CredentialsProvider> getCredentialsProvider();
}
