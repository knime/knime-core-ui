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
 *   Oct 8, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigrator;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests {@link PersistUtil}. Use {@link #getPersistSchema(Class)} to get the persist schema of a single settings class
 * in other tests.
 *
 * @author Paul Bärnreuther
 */
public class PersistUtilTest {

    /**
     * @param modelSettings
     * @return a persist schema with the given settings interpreted as model settings
     */
    public static ObjectNode getPersistSchema(final Class<? extends PersistableSettings> modelSettings) {
        final var persistTree = new PersistTreeFactory().createTree(modelSettings);
        final var uischema = new ObjectMapper().createObjectNode();
        PersistUtil.addPersist(uischema, Map.of(SettingsType.MODEL, persistTree));
        return (ObjectNode)uischema.get("persist");
    }

    private interface TestPersistor<S> extends NodeSettingsPersistor<S> {
        @Override
        default S load(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new UnsupportedOperationException("should not be called by this test");
        }

        @Override
        default void save(final S obj, final NodeSettingsWO settings) {
            throw new UnsupportedOperationException("should not be called by this test");
        }
    }

    private static class CustomPersistor implements TestPersistor<Integer> {

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"config_key_from_persistor_1"},
                {"config_key_from_persistor_2", "config_key_from_persistor_3_Internals"}};
        }

    }

    private static class SettingWithCustomPersistor implements PersistableSettings {

        @Persistor(CustomPersistor.class)
        public int test;
    }

    @Test
    void testConfigKeyFromCustomPersistor() throws JsonProcessingException {
        final var result = getPersistSchema(SettingWithCustomPersistor.class);
        assertThatJson(result).inPath("$.properties.model.properties.test.configPaths").isArray()
            .isEqualTo(new String[][]{{"config_key_from_persistor_1"}, {"config_key_from_persistor_2"}});

    }

    private static class CustomPersistorWithPaths implements TestPersistor<Integer> {

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"config_key_from_persistor_1", "config_key_from_persistor_2"},
                {"config_key_from_persistor_3"}};
        }

    }

    private static class SettingWithCustomPersistorWithPaths implements PersistableSettings {

        @Persistor(CustomPersistorWithPaths.class)
        public int test;
    }

    @Test
    void testConfigPathsFromCustomPersistor() throws JsonProcessingException {
        final var result = getPersistSchema(SettingWithCustomPersistorWithPaths.class);
        assertThatJson(result).inPath("$.properties.model.properties.test.configPaths").isArray()
            .isEqualTo(new String[][]{{"config_key_from_persistor_1", "config_key_from_persistor_2"},
                {"config_key_from_persistor_3"}});

    }

    private static class CustomPersistorWithKeysContainingDots implements TestPersistor<Integer> {

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"config.key.from.persistor.1", "config.key.from.persistor.2"}};
        }

    }

    private static class SettingWithCustomPersistorWithKeysContainingDots implements PersistableSettings {

        @Persistor(CustomPersistorWithKeysContainingDots.class)
        public int test;
    }

    @Test
    void testThrowsOnCustomPersistorWithConfigKeysContainingDots() throws JsonProcessingException {

        assertThrows(IllegalArgumentException.class,
            () -> getPersistSchema(SettingWithCustomPersistorWithKeysContainingDots.class));

    }

    private static class SettingsWithHiddenPersist implements PersistableSettings {

        static final class NestedSettings implements PersistableSettings {
            @SuppressWarnings("unused")
            public int nested;
        }

        @Persist(hidden = true)
        public NestedSettings nestedSettings;

        @Persist(configKey = "foo", hidden = true)
        public int test;

    }

    @Test
    void testAddsEmpytArraysOnHiddenPersist() throws JsonProcessingException {
        final var result = getPersistSchema(SettingsWithHiddenPersist.class);
        assertThatJson(result).inPath("$.properties.model.properties.nestedSettings.configPaths").isArray().isEmpty();
        assertThatJson(result).inPath("$.properties.model.properties.test.configPaths").isArray().isEmpty();
    }

    private static class SettingsWithConfigRename implements PersistableSettings {
        static final class NestedSettings implements PersistableSettings {
            @SuppressWarnings("unused")
            public int nested;
        }

        @Persist(configKey = "foo")
        public NestedSettings nestedSettings;

        @Persist(configKey = "bar")
        public int test;

    }

    @Test
    void testAddsSingleConfigKeyOnPersistWithConfigKey() {

        final var result = getPersistSchema(SettingsWithConfigRename.class);
        assertThatJson(result).inPath("$.properties.model.properties.nestedSettings.configKey").isString()
            .isEqualTo("foo");
        assertThatJson(result).inPath("$.properties.model.properties.test.configKey").isString().isEqualTo("bar");
    }

    private static class CustomModifierInteger implements NodeSettingsMigrator<Integer> {
        @Override
        public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
            return getDummyConfigsDeprecations();
        }

    }

    private static class SettingWithCustomFieldPersistorWithDeprecatedConfigs implements PersistableSettings {

        @Migration(CustomModifierInteger.class)
        public int test;
    }

    @Test
    void testConfigKeyFromCustomFieldPersistorWithDeprecatedConfigs() throws JsonProcessingException {
        final var result = getPersistSchema(SettingWithCustomFieldPersistorWithDeprecatedConfigs.class);
        assertThatJson(result).inPath("$.properties.model.properties.test.deprecatedConfigKeys").isArray().hasSize(3);
        assertThatJson(result).inPath("$.properties.model.properties.test.deprecatedConfigKeys[0].deprecated").isArray()
            .isEqualTo(new String[][]{{"A", "B"}, {"C"}});
        assertThatJson(result).inPath("$.properties.model.properties.test.deprecatedConfigKeys[1].deprecated").isArray()
            .isEqualTo(new String[][]{{"G", "H"}});
        assertThatJson(result).inPath("$.properties.model.properties.test.deprecatedConfigKeys[2].deprecated").isArray()
            .isEqualTo(new String[][]{{"K", "L"}});
    }

    private static class CustomClassPersistorWithDeprecatedConfigs
        implements TestPersistor<SettingWithCustomClassPersistorWithDeprecatedConfigs> {

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"X", "Y"}, {"Z"}};
        }

    }

    private static class CustomModifier
        implements NodeSettingsMigrator<SettingWithCustomClassPersistorWithDeprecatedConfigs> {
        @Override
        public List<ConfigsDeprecation<SettingWithCustomClassPersistorWithDeprecatedConfigs>> getConfigsDeprecations() {
            return getDummyConfigsDeprecations();
        }

    }

    @Persistor(CustomClassPersistorWithDeprecatedConfigs.class)
    @Migration(CustomModifier.class)
    private static class SettingWithCustomClassPersistorWithDeprecatedConfigs implements PersistableSettings {

        @Widget(title = "my_title", description = "")
        public int test;
    }

    @Test
    void testConfigKeyFromCustomClassPersistorWithDeprecatedConfigs() throws JsonProcessingException {
        final var result = getPersistSchema(SettingWithCustomClassPersistorWithDeprecatedConfigs.class);
        assertThatJson(result).inPath("$.properties.model.propertiesDeprecatedConfigKeys").isArray().hasSize(3);
        assertThatJson(result).inPath("$.properties.model.propertiesDeprecatedConfigKeys[0].deprecated").isArray()
            .isEqualTo(new String[][]{{"A", "B"}, {"C"}});
        assertThatJson(result).inPath("$.properties.model.propertiesDeprecatedConfigKeys[1].deprecated").isArray()
            .isEqualTo(new String[][]{{"G", "H"}});
        assertThatJson(result).inPath("$.properties.model.propertiesDeprecatedConfigKeys[2].deprecated").isArray()
            .isEqualTo(new String[][]{{"K", "L"}});

        assertThatJson(result).inPath("$.properties.model.propertiesConfigPaths").isArray().hasSize(2);
        assertThatJson(result).inPath("$.properties.model.propertiesConfigPaths[0]").isArray()
            .isEqualTo(new String[]{"X", "Y"});
        assertThatJson(result).inPath("$.properties.model.propertiesConfigPaths[1]").isArray()
            .isEqualTo(new String[]{"Z"});
    }

    private static class SettingsWithCustomFieldAndClassModifier implements PersistableSettings {
        @Migration(CustomModifier.class)
        public SettingWithCustomClassPersistorWithDeprecatedConfigs bothPersistors;
    }

    @Test
    void testConfigKeyFromCustomFieldAndClassPersistorWithDeprecatedConfigs() throws JsonProcessingException {
        final var result = getPersistSchema(SettingsWithCustomFieldAndClassModifier.class);
        assertThatJson(result).inPath("$.properties.model.properties.bothPersistors.propertiesDeprecatedConfigKeys")
            .isArray().hasSize(3);
        assertThatJson(result).inPath("$.properties.model.properties.bothPersistors.propertiesConfigPaths").isArray()
            .hasSize(2);
        assertThatJson(result).inPath("$.properties.model.properties.bothPersistors.deprecatedConfigKeys").isArray()
            .hasSize(3);
    }

    private static <T> List<ConfigsDeprecation<T>> getDummyConfigsDeprecations() {
        SettingsLoader<T> dummyLoader = settings -> {
            throw new IllegalStateException("Should not be called within this test");
        };
        return List.of(//
            new Builder<T>(dummyLoader)//
                .withDeprecatedConfigPath("A", "B")//
                .withDeprecatedConfigPath("C")//
                .build(), //
            new Builder<T>(dummyLoader)//
                .withDeprecatedConfigPath("G", "H")//
                .build(), //
            new Builder<T>(dummyLoader)//
                .withDeprecatedConfigPath("K", "L")//
                .build());
    }
}
