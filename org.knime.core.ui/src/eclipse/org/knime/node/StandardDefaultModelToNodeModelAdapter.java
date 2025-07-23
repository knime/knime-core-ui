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
 *   Jun 27, 2025 (Paul Bärnreuther): created
 */
package org.knime.node;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectHolder;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.parameters.NodeParameters;

/**
 * Adapter that converts a {@link DefaultModel} to a {@link NodeModel}.
 *
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class StandardDefaultModelToNodeModelAdapter extends NodeModel
    implements DefaultModelToNodeModelAdapter, PortObjectHolder {

    private final DefaultModel.StandardDefaultModel m_model;

    private final int m_numOutputPorts;

    private final Class<? extends NodeParameters> m_viewParametersClass;

    // set in configure
    private PortObjectSpec[] m_specs;

    // set in setModelSettings, which is called in loadValidatedSettingsFrom or configure
    private NodeParameters m_modelParameters;

    // set in setInternalPortObjects
    private PortObject[] m_portObjects;

    StandardDefaultModelToNodeModelAdapter(final DefaultModel.StandardDefaultModel model, final PortType[] inputPorts,
        final PortType[] outputPorts, final Class<? extends NodeParameters> viewParametersClass) {
        super(inputPorts, outputPorts);
        m_model = model;
        m_numOutputPorts = outputPorts.length;
        m_viewParametersClass = viewParametersClass;
    }

    @Override
    public Optional<Class<? extends NodeParameters>> getModelParametersClass() {
        return m_model.getParametersClass();
    }

    @Override
    public NodeParameters getModelParameters() {
        return m_modelParameters;
    }

    @Override
    public Optional<Class<? extends NodeParameters>> getViewParametersClass() {
        return Optional.ofNullable(m_viewParametersClass);
    }

    @Override
    public Optional<PortObjectSpec[]> getSpecs() {
        return Optional.ofNullable(m_specs);
    }

    @Override
    public void setModelParameters(final NodeParameters modelParameters) {
        m_modelParameters = modelParameters;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        m_specs = inSpecs;
        var outSpecs = new PortObjectSpec[m_numOutputPorts];
        final var params = setInitialSettingsUsingSpecsIfNecessary(inSpecs);

        m_model.m_configure.accept(new ConfigureInput() {

            @SuppressWarnings("unchecked")
            @Override
            public <S extends NodeParameters> S getParameters() {
                return (S)params;
            }

            @Override
            public PortObjectSpec[] getInPortSpecs() {
                return inSpecs;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <S extends PortObjectSpec> S getInPortSpec(final int index) {
                return (S)inSpecs[index];
            }

            @Override
            public DataTableSpec[] getInTableSpecs() {
                return Arrays.stream(getInPortSpecs()).map(DataTableSpec.class::cast).toArray(DataTableSpec[]::new);
            }

            @Override
            public DataTableSpec getInTableSpec(final int portIndex) {
                return (DataTableSpec)getInPortSpec(portIndex);
            }

        }, new ConfigureOutput() {

            @Override
            public <S extends PortObjectSpec> void setOutSpec(final int index, final S spec) {
                outSpecs[index] = spec;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <S extends PortObjectSpec> void setOutSpecs(final S... specs) {
                System.arraycopy(specs, 0, outSpecs, 0, specs.length);
            }

            @Override
            public void setWarningMessage(final String message) {
                StandardDefaultModelToNodeModelAdapter.this.setWarningMessage(message);
            }
        });

        return outSpecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        var outObjects = new PortObject[m_numOutputPorts];
        m_model.m_execute.accept(new ExecuteInput() { // NOSONAR

            @SuppressWarnings("unchecked")
            @Override
            public <S extends NodeParameters> S getParameters() {
                return (S)m_modelParameters;
            }

            @Override
            public PortObject[] getInPortObjects() {
                return inObjects;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <D extends PortObject> D getInPortObject(final int portIndex) {
                return (D)inObjects[portIndex];
            }

            @Override
            public BufferedDataTable[] getInTables() {
                return Arrays.stream(getInPortObjects()).map(BufferedDataTable.class::cast)
                    .toArray(BufferedDataTable[]::new);
            }

            @Override
            public BufferedDataTable getInTable(final int portIndex) {
                return (BufferedDataTable)getInPortObject(portIndex);
            }

            @Override
            public ExecutionContext getExecutionContext() {
                return exec;
            }

        }, new ExecuteOutput() {

            @Override
            public void setOutData(final int index, final PortObject data) {
                outObjects[index] = data;
            }

            @Override
            public void setOutData(final PortObject... data) {
                System.arraycopy(data, 0, outObjects, 0, data.length);
            }

            @Override
            public void setInternalData(final PortObject... data) {
                StandardDefaultModelToNodeModelAdapter.this.setInternalPortObjects(data);
            }

            @Override
            public void setWarningMessage(final String message) {
                StandardDefaultModelToNodeModelAdapter.this.setWarningMessage(message);
            }
        });

        return outObjects;
    }

    @Override
    protected void reset() {
        m_portObjects = null;
    }

    @Override
    public PortObject[] getInternalPortObjects() {
        return m_portObjects;
    }

    @Override
    public void setInternalPortObjects(final PortObject[] portObjects) {
        m_portObjects = portObjects;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        defaultSaveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        defaultValidateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        defaultLoadValidatedSettingsFrom(settings);
    }

    @Override
    protected void validateViewSettings(final NodeSettingsRO viewSettings) throws InvalidSettingsException {
        defaultValidateViewSettings(viewSettings);
    }

    @Override
    protected void saveDefaultViewSettingsTo(final NodeSettingsWO viewSettings) {
        defaultSaveDefaultViewSettingsTo(viewSettings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not implemented
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Not implemented
    }
}
