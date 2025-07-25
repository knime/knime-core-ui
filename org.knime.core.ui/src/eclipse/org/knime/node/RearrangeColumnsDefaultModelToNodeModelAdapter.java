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
 *   Jul 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.node;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.node.DefaultModel.RearrangeColumnsInput;
import org.knime.node.parameters.NodeParameters;

/**
 * Adapter that converts a {@link DefaultModel} to a {@link SimpleStreamableFunctionNodeModel}.
 *
 * @author Paul Bärnreuther, KNIME GmbH
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class RearrangeColumnsDefaultModelToNodeModelAdapter extends SimpleStreamableFunctionNodeModel
    implements DefaultModelToNodeModelAdapter {

    private final DefaultModel.RearrangeColumnsDefaultModel m_model;

    private final Class<? extends NodeParameters> m_viewParametersClass;

    // set in createColumnRearranger
    private PortObjectSpec[] m_specs;

    // set in setModelSettings, which is called in loadValidatedSettingsFrom or createColumnRearranger
    private NodeParameters m_modelParameters;

    RearrangeColumnsDefaultModelToNodeModelAdapter(final DefaultModel.RearrangeColumnsDefaultModel model,
        final Class<? extends NodeParameters> viewParametersClass) {

        super();
        m_model = model;
        m_viewParametersClass = viewParametersClass;
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
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

        final AtomicReference<ColumnRearranger> columnRearranger = new AtomicReference<>();
        final var parameters = setInitialSettingsUsingSpecsIfNecessary(new PortObjectSpec[]{spec});
        m_specs = new PortObjectSpec[]{spec};

        m_model.m_rearrangeColumns.accept(new RearrangeColumnsInput() {

            @SuppressWarnings("unchecked")
            @Override
            public <S extends NodeParameters> S getParameters() {
                return (S)parameters;
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return spec;
            }

        }, rearranger -> {
            CheckUtils.checkArgumentNotNull(rearranger, "Column rearranger must not be null.");
            columnRearranger.set(rearranger);
        });

        if (columnRearranger.get() == null) {
            throw new InvalidSettingsException("No column rearranger has been set.");
        }
        return columnRearranger.get();
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
    public void setModelParameters(final NodeParameters modelParameters) {
        m_modelParameters = modelParameters;
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
