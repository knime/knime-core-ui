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
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.NodeDialogTest.createNodeAndVariableSettingsRO;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeDialogTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonNodeSettingsMapperUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.migration.ParametersLoader;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests DefaultNodeSettingsService.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class DefaultNodeSettingsServiceTest {

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    @SuppressWarnings("unused")
    private static class TestSettings implements NodeParameters {
        String m_value;

        TestSettings() {
        }

        TestSettings(final String value) {
            m_value = value;
        }

        TestSettings(final NodeParametersInput context) {
            m_value = (context.getInTableSpecs()[0]).getColumnSpec(0).getName();
        }
    }

    @Test
    void testGetInitialDataFromEmptySettings() throws JsonProcessingException {
        final var nodeSettings = new NodeSettings("node_settings");
        var expectedViewData = (ObjectNode)JsonFormsDataUtil.toJsonData(new TestSettings("bar"));
        // the framework should create new settings using the TestSettings(NodeParametersInput) constructor
        obtainAndCheckInitialData(expectedViewData, nodeSettings,
            new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec())});
    }

    @Test
    void testGetInitialData() throws JsonProcessingException {
        final var viewData = (ObjectNode)JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var specs =
            new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec())};
        final var viewDataSchema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            NodeParametersUtil.createDefaultNodeSettingsContext(specs), MAPPER);
        final var nodeSettings = new NodeSettings("node_settings");
        JsonNodeSettingsMapperUtil.jsonObjectToNodeSettings(viewData, viewDataSchema, nodeSettings);

        // obtain initial data using "foo" node settings and "bar" specs and compare against "foo" view data
        obtainAndCheckInitialData(viewData, nodeSettings, specs);
    }

    private static void obtainAndCheckInitialData(final JsonNode viewData, final NodeSettings nodeSettings,
        final PortObjectSpec[] specs) throws JsonProcessingException {

        // create settings service and obtain initial data using node settings and specs
        final var settingsService =
            new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, TestSettings.class));
        final var initialData = MAPPER.readTree(settingsService.fromNodeSettings(
            Map.of(SettingsType.VIEW, NodeDialogTest.createNodeAndVariableSettingsRO(nodeSettings)), specs));

        // assert that returned data is equal to wrapped "foo" view data created via JsonFormsDataUtil
        final var wrappedViewData = MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData);
        assertThatJson(initialData.get("data")).isEqualTo(wrappedViewData);

        // assert that returned schema is equal to wrapped schema created via JsonFormsSchemaUtil
        final var schema = JsonFormsSchemaUtil.buildSchema(TestSettings.class,
            NodeParametersUtil.createDefaultNodeSettingsContext(specs), MAPPER);
        final var wrappedSchema = MAPPER.createObjectNode();
        wrappedSchema.put("type", "object").putObject("properties").set(SettingsType.VIEW.getConfigKey(), schema);
        assertThatJson(initialData.get("schema")).isEqualTo(wrappedSchema);

        // assert that returned ui schema is equal to json object created via JsonFormsUiSchemaUtil
        final Map<SettingsType, Class<? extends WidgetGroup>> testSettingsMap =
            Map.of(SettingsType.VIEW, TestSettings.class);
        final var uiSchema =
            JsonFormsUiSchemaUtil.buildUISchema(
                testSettingsMap.entrySet().stream()
                    .map(e -> new WidgetTreeFactory().createTree(e.getValue(), e.getKey())).toList(),
                NodeParametersUtil.createDefaultNodeSettingsContext(specs));
        assertThatJson(initialData.get("ui_schema").get("elements")).isEqualTo(uiSchema.get("elements"));
    }

    @Test
    void testApplyData() throws InvalidSettingsException {
        // create "foo" view data and empty node settings
        final var viewData = JsonFormsDataUtil.toJsonData(new TestSettings("foo"));
        final var nodeSettings = new NodeSettings("node_settings");

        // create settings service and apply wrapped "foo" view data into node settings
        final var settingsService =
            new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, TestSettings.class));
        final var wrappedViewData = MAPPER.createObjectNode().set("data",
            MAPPER.createObjectNode().set(SettingsType.VIEW.getConfigKey(), viewData));
        settingsService.toNodeSettings(wrappedViewData.toString(),
            Map.of(SettingsType.VIEW,
                NodeDialogTest.createNodeAndVariableSettingsRO(new NodeSettings("previousSettings"))),
            Map.of(SettingsType.VIEW, NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings)));

        // assert that node settings are no longer empty but equal to the "foo" view data
        final var node = JsonFormsDataUtil.getMapper().createObjectNode();
        JsonNodeSettingsMapperUtil.nodeSettingsToJsonObject(nodeSettings, node);
        assertThatJson(node).isEqualTo(viewData);
    }

    @Test
    void testSettingsOverwrittenByFlowVariablesAreSetToPrevious() throws InvalidSettingsException {
        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(TestSettings.class, new TestSettings("old"), previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService =
            new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, TestSettings.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("value")).isEqualTo("old");
    }

    static class LegacySettings implements NodeParameters {
        String m_valueLegacy1;

        String m_valueLegacy2;

        LegacySettings() {
        }

        LegacySettings(final String valueLegacy1, final String valueLegacy2) {
            m_valueLegacy1 = valueLegacy1;
            m_valueLegacy2 = valueLegacy2;
        }
    }

    private static final List<ConfigMigration<String>>
        createConfigsDeprecationsForMyLegacySettings(final ParametersLoader<String> loader) {
        return List.of(ConfigMigration.builder(loader).withDeprecatedConfigPath("valueLegacy1")
            .withDeprecatedConfigPath("valueLegacy2").build());
    }

    static class MigratedSettings implements NodeParameters {

        static class MyLegacyPersistor implements NodeParametersPersistor<String> {

            private static final String CFG_KEY = "value";

            @Override
            public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
                throw new IllegalStateException("This method should not be called within the current test");
            }

            @Override
            public void save(final String obj, final NodeSettingsWO settings) {
                settings.addString(CFG_KEY, obj);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_KEY}};
            }

        }

        static class MyLegacyMigrator implements NodeParametersMigration<String> {
            @Override
            public List<ConfigMigration<String>> getConfigMigrations() {
                return createConfigsDeprecationsForMyLegacySettings(settings -> {
                    throw new IllegalStateException("Should not be called within this test");
                });
            }

        }

        @Persistor(MyLegacyPersistor.class)
        @Migration(MyLegacyMigrator.class)
        String m_value;

        MigratedSettings() {
        }

        MigratedSettings(final String value) {
            m_value = value;
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"controlling", "exposed"})
    void testSettingsOverwrittenByDeprecatedFlowVariablesAreSetToPrevious(final String flowVarType)
        throws InvalidSettingsException {

        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService =
            new DefaultNodeSettingsService(Map.of(SettingsType.VIEW, MigratedSettings.class));
        final var textSettings = String.format("""
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.valueLegacy1": {
                            "%sFlowVariableName": "flowVar"
                        }
                    }
                }""", flowVarType);
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("valueLegacy1")).isEqualTo("old1");
        assertThat(nodeSettings.getString("valueLegacy2")).isEqualTo("old2");
        assertThat(nodeSettings.containsKey("value")).isFalse();
    }

    static class MigratedSettingsWithLoad implements NodeParameters {

        static class MyLegacyMigrator implements NodeParametersMigration<String> {
            @Override
            public List<ConfigMigration<String>> getConfigMigrations() {
                return createConfigsDeprecationsForMyLegacySettings(
                    settings -> settings.getString("valueLegacy1") + settings.getString("valueLegacy2"));
            }

        }

        @Migration(MyLegacyMigrator.class)
        String m_value;

        MigratedSettingsWithLoad() {
        }

        MigratedSettingsWithLoad(final String value) {
            m_value = value;
        }

    }

    @Test
    void testMapsPreviousSettingsToNewSettingsOnOverwrite() throws InvalidSettingsException {

        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, MigratedSettingsWithLoad.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.containsKey("valueLegacy1")).isFalse();
        assertThat(nodeSettings.containsKey("valueLegacy2")).isFalse();
        assertThat(nodeSettings.getString("value")).isEqualTo("old1old2");
    }

    static class MigratedSettingsWithFailingLoad implements NodeParameters {

        static class MyLegacyMigrator implements NodeParametersMigration<String> {
            @Override
            public List<ConfigMigration<String>> getConfigMigrations() {
                return createConfigsDeprecationsForMyLegacySettings(settings -> {
                    throw new InvalidSettingsException("Old configs");
                });
            }

        }

        @Migration(MyLegacyMigrator.class)
        String m_value;

        MigratedSettingsWithFailingLoad() {
        }

        MigratedSettingsWithFailingLoad(final String value) {
            m_value = value;
        }

    }

    @Test
    void testUsesNewSettingsInCaseLoadingTheOldSettingsFails() throws InvalidSettingsException {

        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, MigratedSettingsWithFailingLoad.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.containsKey("valueLegacy1")).isFalse();
        assertThat(nodeSettings.containsKey("valueLegacy2")).isFalse();
        assertThat(nodeSettings.getString("value")).isEqualTo("new");
    }

    @Test
    void testDoesNotMapPreviousSettingsToNewSettingsOnExpose() throws InvalidSettingsException {

        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, MigratedSettingsWithLoad.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "exposedFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("value")).isEqualTo("new");
    }

    static final class RootSettingsDeprecationsDefinition
        implements NodeParametersMigration<RootSettingsWithDeprecations> {

        @Override
        public List<ConfigMigration<RootSettingsWithDeprecations>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(settings -> new RootSettingsWithDeprecations())
                .withDeprecatedConfigPath("valueLegacy1").withDeprecatedConfigPath("valueLegacy2").build());

        }
    }

    @Migration(RootSettingsDeprecationsDefinition.class)
    static class RootSettingsWithDeprecations implements NodeParameters {

        String m_value = "the default";
    }

    @Test
    void testInferrsConfigKeysFromFieldBasedDefaultPersistor() throws InvalidSettingsException {
        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, RootSettingsWithDeprecations.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.valueLegacy1": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("valueLegacy1", "notPresent")).isEqualTo("old1");
        assertThat(nodeSettings.getString("valueLegacy2", "notPresent")).isEqualTo("old2");
        assertThat(nodeSettings.getString("value", "notPresent")).isEqualTo("notPresent");
    }

    @Test
    void testInferrsConfigKeysFromFieldBasedDefaultPersistorWhenANewVariableIsSet() throws InvalidSettingsException {
        final var previousNodeSettings = new NodeSettings("previousSettings");
        NodeParametersUtil.saveSettings(LegacySettings.class, new LegacySettings("old1", "old2"),
            previousNodeSettings);
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, RootSettingsWithDeprecations.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("valueLegacy1", "notPresent")).isEqualTo("notPresent");
        assertThat(nodeSettings.getString("valueLegacy2", "notPresent")).isEqualTo("notPresent");
        assertThat(nodeSettings.getString("value", "notPresent")).isEqualTo("the default");
    }

    static class SettingsWithOptionalSetting implements NodeParameters {

        @Migrate(loadDefaultIfAbsent = true)
        String m_value = "the default";
    }

    @Test
    void testAddsConfigForOptionalPersistor() throws InvalidSettingsException {
        final var previousNodeSettings = new NodeSettings("previousSettings");
        final var nodeSettings = new NodeSettings("newSettings");
        final var nodeAndVariableSettingsWO = NodeDialogTest.createNodeAndVariableSettingsWO(nodeSettings);
        final var settingsService = new DefaultNodeSettingsService(
            Map.of(SettingsType.VIEW, SettingsWithOptionalSetting.class));
        final var textSettings = """
                    {
                    "data": {"view": {"value": "new"}},
                    "flowVariableSettings": {
                        "view.value": {
                            "controllingFlowVariableName": "flowVar"
                        }
                    }
                }""";
        settingsService.toNodeSettings(textSettings,
            Map.of(SettingsType.VIEW, createNodeAndVariableSettingsRO(previousNodeSettings)),
            Map.of(SettingsType.VIEW, nodeAndVariableSettingsWO));

        assertThat(nodeSettings.getString("value", "notPresent")).isEqualTo("the default");

    }

}
