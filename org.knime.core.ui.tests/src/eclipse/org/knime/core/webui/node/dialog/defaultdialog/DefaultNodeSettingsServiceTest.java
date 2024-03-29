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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeDialogTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonNodeSettingsMapperUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.impl.AsyncChoicesHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests DefaultNodeSettingsService.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class DefaultNodeSettingsServiceTest {

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    @SuppressWarnings("unused")
    private static class TestSettings implements DefaultNodeSettings {
        String m_value;

        TestSettings() {
        }

        TestSettings(final String value) {
            m_value = value;
        }

        TestSettings(final DefaultNodeSettingsContext context) {
            m_value = (context.getDataTableSpecs()[0]).getColumnSpec(0).getName();
        }
    }

    @Test
    void testGetInitialDataFromEmptySettings() throws JsonProcessingException {
        final var nodeSettings = new NodeSettings("node_settings");
        var expectedViewData = (ObjectNode)JsonFormsDataUtil.toJsonData(new TestSettings("bar"));
        // the framework should create new settings using the TestSettings(DefaultNodeSettingsContext) constructor
        obtainAndCheckInitialData(expectedViewData, nodeSettings,
            new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec())});
    }

    @Test
    void testGetInitialData() throws JsonProcessingException {
        final var viewData = (ObjectNode)JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var specs =
            new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec())};
        final var viewDataSchema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            DefaultNodeSettings.createDefaultNodeSettingsContext(specs), MAPPER);
        final var nodeSettings = new NodeSettings("node_settings");
        JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings(viewData, viewDataSchema, nodeSettings);

        // obtain initial data using "foo" node settings and "bar" specs and compare against "foo" view data
        obtainAndCheckInitialData(viewData, nodeSettings, specs);
    }

    private static void obtainAndCheckInitialData(final JsonNode viewData, final NodeSettings nodeSettings,
        final PortObjectSpec[] specs) throws JsonProcessingException {

        // create settings service and obtain initial data using node settings and specs
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, TestSettings.class), new AsyncChoicesHolder());
        final var initialData = MAPPER.readTree(settingsService.fromNodeSettings(
            Map.of(SettingsType.VIEW, NodeDialogTest.createNodeAndVariableSettingsRO(nodeSettings)), specs));

        // assert that returned data is equal to wrapped "foo" view data created via JsonFormsDataUtil
        final var wrappedViewData = MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData);
        assertThatJson(initialData.get("data")).isEqualTo(wrappedViewData);

        // assert that returned schema is equal to wrapped schema created via JsonFormsSchemaUtil
        final var schema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            DefaultNodeSettings.createDefaultNodeSettingsContext(specs), MAPPER);
        final var wrappedSchema = MAPPER.createObjectNode();
        wrappedSchema.put("type", "object").putObject("properties").set(SettingsType.VIEW.getConfigKey(), schema);
        assertThatJson(initialData.get("schema")).isEqualTo(wrappedSchema);

        // assert that returned ui schema is equal to json object created via JsonFormsUiSchemaUtil
        final Map<String, Class<? extends WidgetGroup>> testSettingsMap =
            Map.of(SettingsType.VIEW.getConfigKey(), TestSettings.class);
        final var uiSchema = JsonFormsUiSchemaUtil.buildUISchema(testSettingsMap,
            DefaultNodeSettings.createDefaultNodeSettingsContext(specs), new AsyncChoicesHolder());
        assertThatJson(initialData.get("ui_schema")).isEqualTo(uiSchema);
    }

    @Test
    void testApplyData() throws InvalidSettingsException, JsonProcessingException {
        // create "foo" view data and empty node settings
        final var viewData = JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var nodeSettings = new NodeSettings("node_settings");

        // create settings service and apply wrapped "foo" view data into node settings
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, TestSettings.class), new AsyncChoicesHolder());
        final var wrappedViewData = MAPPER.createObjectNode().set("data",
            MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData));
        settingsService.toNodeSettings(wrappedViewData.toString(),
            Map.of(SettingsType.VIEW, NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings)));

        // assert that node settings are no longer empty but equal to the "foo" view data
        final var node = JsonFormsDataUtil.getMapper().createObjectNode();
        JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(nodeSettings, node);
        assertThatJson(node).isEqualTo(viewData);
    }

}
