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

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.WebDialogNodeRepresentation.DefaultWebDialogNodeRepresentation;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.utils.TestConfigurationNodeFactoryTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * A configuration node to test whether "modern" NodeDialogs of Components work.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class TestConfigurationNodeFactory extends TestConfigurationNodeFactoryTemplate {

    @Override
    public NodeModel createNodeModel() {
        return new TestConfigNodeModel();
    }

    static class TestConfigNodeModel extends
        TestConfigurationNodeFactoryTemplate.TestConfigNodeModel<TestConfigNodeRepresentation, TestConfigNodeValue> {
        protected TestConfigNodeModel() {
            super();
        }

        TestConfigNodeValue m_value;

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
        public String getParameterName() {
            return null; // this leads to the index being used as key
        }

    }

    static final class TestConfigNodeValue
        implements TestConfigurationNodeFactoryTemplate.TestConfigNodeValue, WebDialogValue {

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
        implements TestConfigurationNodeFactoryTemplate.TestConfigNodeRepresentation<TestConfigNodeValue>,
        DefaultWebDialogNodeRepresentation<TestConfigNodeValue> {

        @Override
        public DialogElementRendererSpec getWebUIDialogElementRendererSpec() {
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

}
