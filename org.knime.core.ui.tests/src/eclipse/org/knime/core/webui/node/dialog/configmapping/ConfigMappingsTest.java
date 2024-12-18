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
 *   Oct 24, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.configmapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigMappingsFactory.getConfigMappings;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigrator;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;

@SuppressWarnings("java:S2698") // we accept assertions without messages
class ConfigMappingsTest {

    static final class Migrator implements NodeSettingsMigrator<Integer> {

        private static final String DEPRECATED = "deprecated";

        @Override
        public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
            return List.of(ConfigsDeprecation.builder(settings -> 1).withDeprecatedConfigPath(DEPRECATED).build());
        }

    }

    static final class PeristorWithKeys implements NodeSettingsPersistor<Integer> {
        @Override
        public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new UnsupportedOperationException("not used by tests");
        }

        @Override
        public void save(final Integer obj, final NodeSettingsWO settings) {
            throw new UnsupportedOperationException("not used by tests");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"key1"}, {"key2"}};
        }

    }

    static final class InferredConfigsFromConfigPathsTestSettings implements PersistableSettings {

        @Migration(Migrator.class)
        @Persistor(PeristorWithKeys.class)
        int m_field;

    }

    @Test
    void testInferredConfigsFromConfigPaths() {
        final var configMappings = getConfigMappings(InferredConfigsFromConfigPathsTestSettings.class,
            new InferredConfigsFromConfigPathsTestSettings());
        ConfigMappings firstNonEmptyConfigMappingsChild = getFirstNonEmptyConfigMappingsChild(configMappings);
        assertDeprecatedConfigPaths(firstNonEmptyConfigMappingsChild);
        assertThat(firstNonEmptyConfigMappingsChild.m_newConfigPaths)
            .isEqualTo(List.of(new ConfigPath(List.of("key1")), new ConfigPath(List.of("key2"))));
    }

    static final class InferredConfigsFromConfigKeyTestSettings implements PersistableSettings {

        private static final String CONFIG_KEY = "configKey";

        @Migration(Migrator.class)
        @Persist(configKey = CONFIG_KEY)
        int m_field;

    }

    @Test
    void testInferredConfigsFromConfigKey() {
        final var configMappings = getConfigMappings(InferredConfigsFromConfigKeyTestSettings.class,
            new InferredConfigsFromConfigKeyTestSettings());
        ConfigMappings firstNonEmptyConfigMappingsChild = getFirstNonEmptyConfigMappingsChild(configMappings);
        assertDeprecatedConfigPaths(firstNonEmptyConfigMappingsChild);
        assertThat(firstNonEmptyConfigMappingsChild.m_newConfigPaths)
            .isEqualTo(List.of(new ConfigPath(List.of(InferredConfigsFromConfigKeyTestSettings.CONFIG_KEY))));
    }

    static final class InferredConfigsFromFieldNameTestSettings implements PersistableSettings {

        @Migration(Migrator.class)
        int m_fieldName;

    }

    @Test
    void testInferredConfigsFromFieldName() {
        final var configMappings = getConfigMappings(InferredConfigsFromFieldNameTestSettings.class,
            new InferredConfigsFromFieldNameTestSettings());
        ConfigMappings firstNonEmptyConfigMappingsChild = getFirstNonEmptyConfigMappingsChild(configMappings);
        assertDeprecatedConfigPaths(firstNonEmptyConfigMappingsChild);
        assertThat(firstNonEmptyConfigMappingsChild.m_newConfigPaths)
            .isEqualTo(List.of(new ConfigPath(List.of("fieldName"))));
    }

    private static ConfigMappings getFirstNonEmptyConfigMappingsChild(final ConfigMappings configMappings) {
        return getNonEmptyConfigMappingsChildren(configMappings).findFirst()
            .orElseThrow(() -> new IllegalStateException("No config mappings present althoug they should."));
    }

    private static void assertDeprecatedConfigPaths(final ConfigMappings configMappings) {
        assertThat(configMappings.m_deprecatedConfigPaths)
            .isEqualTo(List.of(new ConfigPath(List.of(Migrator.DEPRECATED))));
    }

    private static Stream<ConfigMappings> getNonEmptyConfigMappingsChildren(final ConfigMappings configMappings) {
        if (configMappings.m_newConfigPaths != null) {
            return Stream.of(configMappings);
        }
        return configMappings.m_children.stream().flatMap(ConfigMappingsTest::getNonEmptyConfigMappingsChildren);

    }

    static final class OptionalFieldSettings implements PersistableSettings {

        @Migrate(loadDefaultIfAbsent = true)
        int m_fieldName;

    }

    @Test
    void testOptionalField() {
        final var configMappings = getConfigMappings(OptionalFieldSettings.class, new OptionalFieldSettings());
        ConfigMappings firstNonEmptyConfigMappingsChild = getFirstNonEmptyConfigMappingsChild(configMappings);
        assertThat(firstNonEmptyConfigMappingsChild.m_newConfigPaths)
            .isEqualTo(List.of(new ConfigPath(List.of("fieldName"))));
        assertThat(firstNonEmptyConfigMappingsChild.m_deprecatedConfigPaths).isEmpty();
    }

}
