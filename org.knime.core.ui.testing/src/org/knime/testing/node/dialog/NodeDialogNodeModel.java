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
 *   Sep 13, 2021 (hornm): created
 */
package org.knime.testing.node.dialog;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.NodeDialog;

/**
 * Dummy node model for tests around the {@link NodeDialog}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeDialogNodeModel extends NodeModel {

    /**
     * Value that is considered invalid for {@link #VALIDATED_MODEL_SETTING_CFG}.
     */
    public static final String INVALID_VALUE = "INVALID";

    /**
     * If validated settings contain this key and its value is {@Link #INVALID_VALUE}, the validation is expected to
     * fail.
     */
    public static final String VALIDATED_MODEL_SETTING_CFG = "model setting";

    /**
     * A constant to signal that the validation is expected to fail. Either because the settings contain the key "ERROR"
     * or the key "model setting" is set to {@link #INVALID_VALUE}.
     */
    public static final String VALIDATION_ERROR_MESSAGE = "validation expected to fail";

    private NodeSettingsRO m_loadNodeSettings;

    /**
     * Allows to check the latest load node settings via {@link #loadValidatedSettingsFrom(NodeSettingsRO)}.
     *
     * @return the node settings or <code>null</code> if not load, yet
     */
    public NodeSettingsRO getLoadNodeSettings() {
        return m_loadNodeSettings;
    }

    NodeDialogNodeModel(final int nrInDataPorts) {
        super(nrInDataPorts, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        var outSpecs = new DataTableSpec[getNrOutPorts()];
        Arrays.fill(outSpecs, new DataTableSpec());
        return outSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        var outData = new BufferedDataTable[getNrOutPorts()];
        var container = exec.createDataContainer(new DataTableSpec());
        container.close();
        Arrays.fill(outData, container.getTable());
        return outData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("a default model setting", "a default model setting value");
    }

    @Override
    protected void saveDefaultViewSettingsTo(final NodeSettingsWO settings) {
        settings.addString("a default view setting", "a default view setting value");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey("ERROR") //
            || hasInvalidStringField(settings, VALIDATED_MODEL_SETTING_CFG)//
        ) {
            throw new InvalidSettingsException(VALIDATION_ERROR_MESSAGE);
        }
    }

    private static boolean hasInvalidStringField(final NodeSettingsRO settings, final String key)
        throws InvalidSettingsException {
        return settings.containsKey(key) && INVALID_VALUE.equals(settings.getString(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loadNodeSettings = settings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

}
