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
package org.knime.core.webui.node.dialog;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.LocalizedControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;

/**
 * A configuration node to test whether "modern" NodeDialogs of Components work.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class TestConfigurationNodeFactory extends NodeFactory<NodeModel> {

    static class TestConfigNodeModel extends NodeModel
        implements DialogNode<TestConfigNodeRepresentation, TestConfigNodeValue> {
        protected TestConfigNodeModel() {
            super(0, 0);
        }

        TestConfigNodeValue m_value;

        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
        }

        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        }

        @Override
        protected void reset() {
        }

        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        }

        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        }

        @Override
        public void saveCurrentValue(final NodeSettingsWO content) {
        }

        @Override
        public TestConfigNodeRepresentation getDialogRepresentation() {
            return new TestConfigNodeRepresentation();
        }

        @Override
        public TestConfigNodeValue createEmptyDialogValue() {
            return new TestConfigNodeValue();
        }

        @Override
        public void setDialogValue(final TestConfigNodeValue value) {
            m_value = value;
        }

        @Override
        public TestConfigNodeValue getDefaultValue() {
            return new TestConfigNodeValue("default from model");
        }

        @Override
        public TestConfigNodeValue getDialogValue() {
            return m_value;
        }

        @Override
        public void validateDialogValue(final TestConfigNodeValue value) throws InvalidSettingsException {
        }

        @Override
        public String getParameterName() {
            return null;
        }

        @Override
        public boolean isHideInDialog() {
            return false;
        }

        @Override
        public void setHideInDialog(final boolean hide) {
        }

        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
            return null;
        }

        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            return null;
        }
    }

    static final class TestConfigNodeValue implements WebDialogValue {

        String m_data;

        static final String CFG_KEY = "data_cfg";

        static final String JSON_KEY = "data";

        public TestConfigNodeValue(final String string) {
            m_data = string;
        }

        public TestConfigNodeValue() {
        }

        @Override
        public void saveToNodeSettings(final NodeSettingsWO settings) {
            settings.addString(CFG_KEY, m_data);
        }

        @Override
        public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_data = settings.getString(CFG_KEY);
        }

        @Override
        public void loadFromNodeSettingsInDialog(final NodeSettingsRO settings) {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public void loadFromString(final String fromCmdLine) throws UnsupportedOperationException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public void loadFromJson(final JsonValue json) throws JsonException {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public JsonValue toJson() {
            throw new NotImplementedException("Not implemented");
        }

        static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

        @Override
        public JsonNode toDialogJson() throws IOException {
            return FACTORY.objectNode().put(JSON_KEY, m_data);
        }

        @Override
        public void fromDialogJson(final JsonNode json) throws IOException {
            m_data = json.get(JSON_KEY).asText();
        }

    }

    private static final class TestConfigNodeRepresentation
        implements WebDialogNodeRepresentation<TestConfigNodeValue> {

        @Override
        public DialogNodePanel<TestConfigNodeValue> createDialogPanel() {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public LocalizedControlRendererSpec getWebUIDialogControlSpec() {
            return new TextRendererSpec() {

                @Override
                public String getTitle() {
                    return "Test Configuration Node";
                }

                @Override
                public Optional<String> getDescription() {
                    return Optional.empty();
                }
            }.at(TestConfigurationNodeFactory.TestConfigNodeValue.JSON_KEY);

        }

    }

    @Override
    public NodeModel createNodeModel() {
        return new TestConfigNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<NodeModel> createNodeView(final int viewIndex, final NodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

}
