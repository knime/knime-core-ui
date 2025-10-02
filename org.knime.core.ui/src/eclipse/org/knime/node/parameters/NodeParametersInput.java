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

import java.util.Optional;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

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

    // input similar to ConfigureInput; difference: specs can be null / empty if node not configurable

    /**
     * @return the input {@link PortObjectSpec PortObjectSpecs} of the node; NOTE: array of specs can contain
     *         {@code null} values, e.g., if input port is not connected!
     */
    PortObjectSpec[] getInPortSpecs();

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link PortObjectSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    Optional<PortObjectSpec> getInPortSpec(final int portIndex);

    /**
     * @return the input {@link DataTableSpec DataTableSpecs} of the node; NOTE: array of specs can contain {@code null}
     *         values, e.g., if input port is not connected or inactive!
     * @throws ClassCastException if any of the node's input ports does not hold a {@link DataTableSpec}
     */
    DataTableSpec[] getInTableSpecs();

    /**
     * @param portIndex the port for which to retrieve the spec
     * @return the {@link DataTableSpec} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    Optional<DataTableSpec> getInTableSpec(final int portIndex);

    // input similar to ExecuteInput; difference: specs can be null / empty if node not configurable

    /**
     * @return the input {@link PortObject PortObjects} of the node; NOTE: array of port objects can contain
     *         {@code null} values, e.g., if upstream node is not executed
     */
    PortObject[] getInPortObjects();

    /**
     * @param portIndex
     * @return the {@link PortObject} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    Optional<PortObject> getInPortObject(final int portIndex);

    /**
     * @return the input {@link BufferedDataTable BufferedDataTables} of the node; NOTE: array of tables can contain
     *         {@code null} values, e.g., if upstream node is not executed
     * @throws ClassCastException if the requested port is not a table port
     */
    BufferedDataTable[] getInTables();

    /**
     * @param portIndex the port for which to retrieve the object
     * @return the {@link DataTable} at the given portIndex or {@link Optional#empty()} if it is not available
     * @throws ClassCastException if the requested port is not a table port
     * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
     */
    Optional<BufferedDataTable> getInTable(final int portIndex);

    // output ports (only types)

    /**
     * The node's output types. Not null and not containing null.
     *
     * @return the outTypes
     */
    PortType[] getOutPortTypes();

    /**
     * Getter for the {@link PortsConfiguration} of a configurable node, i.e. a node with dynamic in- or output ports.
     *
     * @return the ports configuration
     * @throws IllegalStateException if there does not exist a port configuration. Whenever a node has dynamic ports, it
     *             has a port configuration.
     */
    PortsConfiguration getPortsConfiguration();

}
