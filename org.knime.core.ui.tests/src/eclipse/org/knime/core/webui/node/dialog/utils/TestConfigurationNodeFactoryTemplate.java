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
 *   18 Aug 2022 (Carsten Haubold): created
 */
package org.knime.core.webui.node.dialog.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.DialogNodePanel;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.node.dialog.DialogNodeValue;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;

/**
 * A configuration node to test whether "modern" NodeDialogs of Components work.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public abstract class TestConfigurationNodeFactoryTemplate extends NodeFactory<NodeModel> {

    @Override
    protected int getNrNodeViews() {
        return 0; // No NodeViews for this node
    }

    @Override
    public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected boolean hasDialog() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        throw new NotImplementedException("Not implemented");
    }

    @SuppressWarnings("javadoc")
    public static abstract class TestConfigNodeModel<REP extends DialogNodeRepresentation<VAL>, VAL extends TestConfigNodeValue>
        extends NodeModel implements DialogNode<REP, VAL> {
        protected TestConfigNodeModel() {
            super(0, 0);
        }

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected void reset() {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public void saveCurrentValue(final NodeSettingsWO content) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public void validateDialogValue(final VAL value) throws InvalidSettingsException {
            return; // no validation per default
        }

        @Override
        public boolean isHideInDialog() {
            return false;
        }

        @Override
        public void setHideInDialog(final boolean hide) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            throw new NotImplementedException("Not implemented");
        }
    }

    @SuppressWarnings("javadoc")
    public interface TestConfigNodeValue extends DialogNodeValue {

        @Override
        default void loadFromNodeSettingsInDialog(final NodeSettingsRO settings) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        default void loadFromString(final String fromCmdLine) throws UnsupportedOperationException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        default void loadFromJson(final JsonValue json) throws JsonException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        default JsonValue toJson() {
            throw new NotImplementedException("Not implemented");
        }

    }

    @SuppressWarnings("javadoc")
    public interface TestConfigNodeRepresentation<VAL extends DialogNodeValue> extends DialogNodeRepresentation<VAL> {

        @Override
        default DialogNodePanel<VAL> createDialogPanel() {
            throw new NotImplementedException("Not implemented");
        }

    }

}
