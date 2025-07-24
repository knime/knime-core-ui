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
 *   10 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.impl;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.NodeParameters;

/**
 * The {@link NodeModel} for simple WebUI nodes, see {@link WebUINodeFactory}.
 *
 * @param <S> the type of model settings
 * @deprecated use org.knime.node.DefaultNode instead
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@Deprecated(since = "5.6")
public abstract class WebUINodeModel<S extends NodeParameters> extends NodeModel { // NOSONAR

    private S m_modelSettings;

    private final Class<S> m_modelSettingsClass;

    /**
     * Note: If the node factory is not a {@link ConfigurableNodeFactory}, use
     * {@link #WebUINodeModel(WebUINodeConfiguration, Class)} instead.
     *
     * @param configuration the {@link WebUINodeConfiguration} for this factory
     * @param modelSettingsClass the type of the model settings for this node
     */
    protected WebUINodeModel(final WebUINodeConfiguration configuration, final Class<S> modelSettingsClass) {
        this(configuration.getInputPortTypes(), configuration.getOutputPortTypes(), modelSettingsClass);
    }

    /**
     * Use this constructor if the node factory is a {@link ConfigurableNodeFactory}. In this case the port types have
     * to be extracted from the context within the method creating the node model.
     *
     * @param modelSettingsClass the type of the model settings for this node
     * @param inputPortTypes input port types
     * @param outputPortTypes output port types
     */
    protected WebUINodeModel(final PortType[] inputPortTypes, final PortType[] outputPortTypes,
        final Class<S> modelSettingsClass) {
        super(inputPortTypes, outputPortTypes);
        m_modelSettingsClass = modelSettingsClass;
    }

    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_modelSettings == null) {
            m_modelSettings = NodeParametersUtil.createSettings(m_modelSettingsClass, inSpecs);
        }
        return configure(inSpecs, m_modelSettings);
    }

    /**
     * @return the settings, an empty optional if the node hasn't been configured, yet
     */
    protected final Optional<S> getSettings() {
        return Optional.ofNullable(m_modelSettings);
    }

    @Override
    protected final DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_modelSettings == null) {
            m_modelSettings = NodeParametersUtil.createSettings(m_modelSettingsClass, inSpecs);
        }
        return configure(inSpecs, m_modelSettings);
    }

    /**
     * @param inSpecs the input {@link PortObjectSpec PortObjectSpecs}
     * @param modelSettings the current model settings
     * @return the output {@link PortObjectSpec PortObjectSpecs}
     * @throws InvalidSettingsException if the settings are inconsistent with the input specs
     * @see NodeModel#configure(PortObjectSpec[])
     */
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final S modelSettings)
        throws InvalidSettingsException {
        var tableSpecs = Stream.of(inSpecs).map(DataTableSpec.class::cast).toArray(DataTableSpec[]::new);
        return configure(tableSpecs, modelSettings);
    }

    /**
     * @param inSpecs the input {@link DataTableSpec DataTableSpecs}
     * @param modelSettings the current model settings
     * @return the output {@link DataTableSpec DataTableSpecs}
     * @throws InvalidSettingsException if the settings are inconsistent with the input specs
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final S modelSettings)
        throws InvalidSettingsException {
        throw new NotImplementedException("NodeModel.configure() implementation missing!");
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        return execute(inObjects, exec, m_modelSettings);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        return execute(inObjects, exec, m_modelSettings);
    }

    /**
     * @param inObjects the input {@link PortObject PortObjects}
     * @param exec the current {@link ExecutionContext}
     * @param modelSettings the current model settings
     * @return the output {@link PortObject PortObjects}
     * @throws Exception if anything goes wrong during the execution
     * @see NodeModel#execute(PortObject[], ExecutionContext)
     */
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec, final S modelSettings)
        throws Exception {
        var tables = Stream.of(inObjects).map(BufferedDataTable.class::cast).toArray(BufferedDataTable[]::new);
        return execute(tables, exec, modelSettings);
    }

    /**
     * @param inData the input {@link BufferedDataTable BufferedDataTables}
     * @param exec the current {@link ExecutionContext}
     * @param modelSettings the current model settings
     * @return the output {@link BufferedDataTable BufferedDataTables}
     * @throws Exception if anything goes wrong during the execution
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final S modelSettings) throws Exception {//NOSONAR
        throw new NotImplementedException("NodeModel.execute() implementation missing!");
    }

    /**
     * @noreference reference {@link #createStreamableOperator(PartitionInfo, PortObjectSpec[], S)} instead
     */
    @Override
    public final StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return createStreamableOperator(partitionInfo, inSpecs, m_modelSettings);
    }

    /**
     * @see NodeModel#createStreamableOperator(PartitionInfo, PortObjectSpec[])
     *
     * @param partitionInfo
     * @param inSpecs
     * @param modelSettings the current model settings
     * @return the {@link StreamableOperator}
     * @throws InvalidSettingsException
     */
    protected StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs, final S modelSettings) throws InvalidSettingsException {
        return super.createStreamableOperator(partitionInfo, inSpecs);
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        // We have use default settings since this method can be called before the node is configured.
        final var modelSettings = Optional.ofNullable(m_modelSettings)
            .orElseGet(() -> NodeParametersUtil.createSettings(m_modelSettingsClass));
        return getOutHiLiteHandler(outIndex, modelSettings);
    }

    /**
     * @see NodeModel#getOutHiLiteHandler(int)
     * @param outIndex
     * @param modelSettings the current model settings or default settings when called before settings have been loaded
     *            (e.g. from the constructor of {@link Node})
     * @return the {@link HiLiteHandler}
     */
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex, final S modelSettings) {
        return super.getOutHiLiteHandler(outIndex);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    /**
     * @param nodeInternDir directory where the node can persist information
     * @param exec for progress and cancellation
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        final var modelSettings =
            m_modelSettings == null ? NodeParametersUtil.createSettings(m_modelSettingsClass) : m_modelSettings;
        NodeParametersUtil.saveSettings(m_modelSettingsClass, modelSettings, settings);
    }

    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateSettings(NodeParametersUtil.loadSettings(settings, m_modelSettingsClass));
    }

    /**
     * Allows extending classes to validate the settings before they are loaded into the NodeModel.
     * <p>
     * This does not affect new nodes. Their settings are <b>not</b> passed through this method before being passed to
     * configure or execute.
     * </p>
     *
     * @param settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void validateSettings(final S settings) throws InvalidSettingsException {
        // hook that can be overwritten by extending classes
    }

    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_modelSettings = NodeParametersUtil.loadSettings(settings, m_modelSettingsClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

}
