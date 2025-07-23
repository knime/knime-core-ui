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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.node;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.FluentNodeAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.node.parameters.NodeParameters;

/**
 * Fluent API to create a node model - not to be created directly but via the {@link DefaultNode}.
 *
 * The model of a node defines the logic of a node's configuration and execution. It requires the model parameters, the
 * configuration logic, and the execution logic.<br>
 * Model parameters alter the output spec or data, i.e. whenever a model setting changes, the node needs to be
 * re-executed. The configuration step generates the expected output specs based on the given input specs and model
 * parameters. The execution step transforms the input data.<br>
 * As an alternative to specifying the configuration and execution logic, the node can also be made streamable by
 * providing the logic how the columns of its input data are to be {@link ColumnRearranger rearranged}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public abstract sealed class DefaultModel implements FluentNodeAPI {

    private final Class<? extends NodeParameters> m_parametersClass;

    private DefaultModel(final Class<? extends NodeParameters> parametersClass) {
        m_parametersClass = parametersClass;
    }

    static RequireModelParameters create() {

        return parametersClass -> new RequireConfigureOrRearrangeColumns() { // NOSONAR

            @Override
            public DefaultModel rearrangeColumns(
                final ConfigureOrRearrangerConsumer<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns) {
                return new RearrangeColumnsDefaultModel(parametersClass, rearrangeColumns);
            }

            @Override
            public RequireExecute
                configure(final ConfigureOrRearrangerConsumer<ConfigureInput, ConfigureOutput> configure) {
                return execute -> new StandardDefaultModel(parametersClass, configure, execute);
            }
        };
    }

    record ConfigureAndExecute(BiConsumer<ConfigureInput, ConfigureOutput> configure,
        BiConsumer<ExecuteInput, ExecuteOutput> execute) {
    }

    static final class StandardDefaultModel extends DefaultModel {

        final ExecuteConsumer m_execute;

        final ConfigureOrRearrangerConsumer<ConfigureInput, ConfigureOutput> m_configure;

        private StandardDefaultModel(final Class<? extends NodeParameters> paramatersClass,
            final ConfigureOrRearrangerConsumer<ConfigureInput, ConfigureOutput> configure,
            final ExecuteConsumer execute) {
            super(paramatersClass);
            m_configure = configure;
            m_execute = execute;
        }
    }

    static final class RearrangeColumnsDefaultModel extends DefaultModel {

        final ConfigureOrRearrangerConsumer<RearrangeColumnsInput, RearrangeColumnsOutput> m_rearrangeColumns;

        private RearrangeColumnsDefaultModel(final Class<? extends NodeParameters> parametersClass,
            final ConfigureOrRearrangerConsumer<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns) {
            super(parametersClass);
            m_rearrangeColumns = rearrangeColumns;
        }
    }

    Optional<Class<? extends NodeParameters>> getParametersClass() {
        return Optional.ofNullable(m_parametersClass);
    }

    /**
     * The build stage that requires the model parameters.
     */
    public interface RequireModelParameters {

        /**
         * Specifies the model parameters class of the node.
         *
         * @param parametersClass the model parameters of the node
         * @return the subsequent build stage
         */
        RequireConfigureOrRearrangeColumns parametersClass(Class<? extends NodeParameters> parametersClass);

        /**
         * Indicates that the model does not have model parameters.
         *
         * @return the subsequent build stage
         */
        default RequireConfigureOrRearrangeColumns withoutParameters() {
            return parametersClass(null);
        }
    }

    /**
     * The build stage for defining how the node modifies or rearranges input data. This can either be a general
     * configuration method or a column-rearranging operation.
     */
    public interface RequireConfigureOrRearrangeColumns {

        /**
         * Used when the node has a different port configuration than one input and one output table port, and performs
         * operations beyond simple column-based modifications. It should use the provided {@link ConfigureInput} to
         * inspect the input and populate the corresponding {@link ConfigureOutput} accordingly.
         *
         * @param configure a function receiving the {@link ConfigureInput} and {@link ConfigureOutput}
         * @return the subsequent build stage
         */
        RequireExecute configure(final ConfigureOrRearrangerConsumer<ConfigureInput, ConfigureOutput> configure);

        /**
         * Used when the node has one input and one output table port, and performs column-based operations—such as
         * deleting, replacing, or adding columns—to compute the output table. It should use the provided
         * {@link RearrangeColumnsInput} to inspect the input and populate the corresponding
         * {@link RearrangeColumnsInput} accordingly. Nodes implemented using this method are streamable.
         *
         * @param rearrangeColumns a function receiving the {@link RearrangeColumnsInput} and
         *            {@link RearrangeColumnsOutput}
         * @return the {@link DefaultModel}
         */
        DefaultModel rearrangeColumns(
            final ConfigureOrRearrangerConsumer<RearrangeColumnsInput, RearrangeColumnsOutput> rearrangeColumns);
    }

    /**
     * A consumer for the {@link RequireConfigureOrRearrangeColumns#configure(ConfigureOrRearrangerConsumer) configure}
     * or {@link RequireConfigureOrRearrangeColumns#rearrangeColumns(ConfigureOrRearrangerConsumer) rearrangeColumns}
     * step that can throw an {@link InvalidSettingsException}.
     *
     * @param <I> the type of the input argument
     * @param <O> the type of the output argument
     */
    public interface ConfigureOrRearrangerConsumer<I, O> {

        /**
         * The actual configuration or rearrangement logic of the node.
         *
         * @param input the input argument from which this consumer can read
         * @param output the output argument which this consumer can populate
         * @throws InvalidSettingsException if some parameters are invalid and the node cannot be configured or columns
         *             cannot be arranged
         */
        void accept(I input, O output) throws InvalidSettingsException;
    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#configure(ConfigureOrRearrangerConsumer)} phase of a node model and
     * provides access to the input specs and access parameters.
     */
    public interface ConfigureInput {

        /**
         * Returns the model parameters of the node.
         *
         * @param <S> the type of the model parameters
         * @return the model parameters of the node
         */
        <S extends NodeParameters> S getParameters();

        /**
         * Returns all input specifications.
         *
         * @return an array containing all input specifications
         */
        PortObjectSpec[] getInPortSpecs();

        /**
         * Returns the input specification at the specified index.
         *
         * @param index the index of the input port
         * @param <S> the type of the input specification
         * @return the input specification at the specified index
         */
        <S extends PortObjectSpec> S getInPortSpec(int index);

        /**
         * Returns all input table specifications.
         *
         * @return the input {@link DataTableSpec DataTableSpecs} of the node
         * @throws ClassCastException if any of the node's input ports does not hold a {@link DataTableSpec}
         */
        DataTableSpec[] getInTableSpecs();

        /**
         * Returns the table input specification at the specified index.
         *
         * @param portIndex the port for which to retrieve the spec
         * @return the {@link DataTableSpec} at the given portIndex or {@link Optional#empty()} if it is not available
         * @throws ClassCastException if the requested port is not a table port
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        DataTableSpec getInTableSpec(final int portIndex);

    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#configure(ConfigureOrRearrangerConsumer)} phase of a node model and
     * provides methods to define output specs.
     */
    public interface ConfigureOutput {

        /**
         *
         * Sets the output specification at the specified index.
         *
         * @param index the index at which to set the input specification
         * @param spec the output specification to set
         * @param <S> the type of the output specification
         */
        <S extends PortObjectSpec> void setOutSpec(int index, S spec);

        /**
         * Sets the output specifications for all output ports.
         *
         * @param specs an array containing all output specifications
         * @param <S> the type of the output specifications ({@link PortObjectSpec} in case of mixed types)
         */
        @SuppressWarnings("unchecked")
        <S extends PortObjectSpec> void setOutSpecs(S... specs);

        /**
         * Sets a warning message informing the user about an issue that occurred during configuration.
         *
         * @param message a warning message informing the user about an issue that occurred during configuration
         */
        void setWarningMessage(String message);
    }

    /**
     * This interface is used within the {@link RequireExecute#execute(ExecuteConsumer)} phase of a node model and
     * provides access to the pand access parameters, input data, and execution context.
     */
    public interface ExecuteInput {

        /**
         * Returns the model parameters of the node.
         *
         * @param <S> the type of the model parameters
         * @return the model parameters of the node
         */
        <S extends NodeParameters> S getParameters();

        /**
         * Returns the current execution context.
         *
         * @return the current execution context
         */
        ExecutionContext getExecutionContext();

        /**
         * Returns all input data.
         *
         * @return the input data of each port
         */
        PortObject[] getInPortObjects();

        /**
         * Returns the input data at the specified index.
         *
         * @param portIndex the index of the input port
         * @param <D> the type of the input data
         * @return the input data at the specified index
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        <D extends PortObject> D getInPortObject(int portIndex);

        /**
         * Returns all input data tables.
         *
         * @return the input {@link BufferedDataTable BufferedDataTables} of the node
         * @throws ClassCastException if the requested port is not a table port
         */
        BufferedDataTable[] getInTables();

        /**
         * Returns the input table at the specified index.
         *
         * @param portIndex the index of the input port
         * @return the input table at the specified index
         * @throws ClassCastException if the requested port is not a table port
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        BufferedDataTable getInTable(final int portIndex);
    }

    /**
     * This interface is used within the {@link RequireExecute#execute(ExecuteConsumer)} phase of a node model and
     * provides methods to define output data and to specify a warning message for issues that occurred during the
     * execution.
     */
    public interface ExecuteOutput {

        /**
         * Sets the output data at the specified index.
         *
         * @param index the index of the output port
         * @param data the output data to set
         * @param <D> the type of the output data
         */
        <D extends PortObject> void setOutData(int index, D data);

        /**
         * Sets the output data for all output ports.
         *
         * @param data the output data to set
         * @param <D> the type of the output data ({@link PortObject} in case of mixed types)
         */
        @SuppressWarnings("unchecked")
        <D extends PortObject> void setOutData(D... data);

        /**
         * Sets the internal data for the node, which can be used in a view.
         *
         * @param data the data to keep in the node for the use in a view (see
         *            {@link DefaultView.ViewInput#getInternalPortObjects()})
         * @param <D> the type of the internal data ({@link PortObject} in case of mixed types)
         */
        @SuppressWarnings("unchecked")
        <D extends PortObject> void setInternalData(D... data);

        /**
         * Sets a warning message informing the user about an issue that occurred during execution
         *
         * @param message a warning message informing the user about an issue that occurred during execution
         */
        void setWarningMessage(String message);
    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#rearrangeColumns(ConfigureOrRearrangerConsumer)} phase of a node model
     * and provides access to the current parameters, the input data table spec, and an {@link ColumnRearranger}.
     */
    public interface RearrangeColumnsInput {

        /**
         * Returns the model parameters of the node.
         *
         * @param <S> the type of the model parameters
         * @return the model parameters of the node
         */
        <S extends NodeParameters> S getParameters();

        /**
         * Returns the data table specification of the input.
         *
         * @return the data table specification of the input
         */
        DataTableSpec getDataTableSpec();
    }

    /**
     * This interface is used within the
     * {@link RequireConfigureOrRearrangeColumns#rearrangeColumns(ConfigureOrRearrangerConsumer)} phase of a node model
     * and provides methods to specify the column rearranger.
     */
    public interface RearrangeColumnsOutput {

        /**
         * Sets the populated {@link ColumnRearranger} that will be used to rearrange the columns of the input. Can be
         * created via {@link ColumnRearranger#ColumnRearranger(DataTableSpec)} using the
         * {@link RearrangeColumnsInput#getDataTableSpec() input spec}.
         *
         * @param rearranger set the populated {@link ColumnRearranger}
         */
        void setColumnRearranger(ColumnRearranger rearranger);
    }

    /**
     * The build stage requiring the execution logic of the node, i.e. it operates on the input data.
     */
    public interface RequireExecute {

        /**
         * Specifies the execution logic of the node. It should use the provided {@link ExecuteInput} to receive the
         * input data and populate the corresponding {@link ExecuteOutput} accordingly.
         *
         * @param execute a function receiving the {@link ExecuteInput} and {@link ExecuteOutput}.
         * @return the {@link DefaultModel}
         */
        DefaultModel execute(ExecuteConsumer execute);
    }

    /**
     * A consumer for the {@link RequireExecute#execute(ExecuteConsumer) execute} step that can throw a
     * {@link KNIMEException}.
     */
    public interface ExecuteConsumer {

        /**
         * The actual execute logic of the node.
         *
         * @param input the input argument from which this consumer can use
         * @param output the output argument which this consumer can populate
         * @throws CanceledExecutionException if the execution was canceled during execution
         * @throws KNIMEException if something goes wrong during execution
         */
        void accept(ExecuteInput input, ExecuteOutput output) throws CanceledExecutionException, KNIMEException;
    }
}
