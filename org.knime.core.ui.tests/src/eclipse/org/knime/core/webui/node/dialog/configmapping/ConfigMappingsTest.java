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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.FieldNodeSettingsPersistorWithInferredConfigs;

@SuppressWarnings("java:S2698") // we accept assertions without messages
class ConfigMappingsTest {

    private static final List<ConfigsDeprecation<Integer>> DEPRECATIONS =
        List.of(ConfigsDeprecation.builder(settings -> 1).withDeprecatedConfigPath("deprecated").build());

    @Test
    void testInferredConfigsFromConfigPaths() {
        final var persistor = new NodeSettingsPersistor<Integer>() {
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

            @Override
            public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
                return DEPRECATIONS;
            }
        };
        final var configMappings = persistor.getConfigMappings(0);
        assertThat(configMappings.m_children).hasSize(1);
        final var child = configMappings.m_children.iterator().next();
        assertThat(child.m_newAndDeprecatedConfigPaths.getNewConfigPaths())
            .isEqualTo(List.of(new ConfigPath(List.of("key1")), new ConfigPath(List.of("key2"))));
    }

    @Test
    void testInferredConfigsFromConfigKey() {
        final var configKey = "configKey";
        final var persistor = new FieldNodeSettingsPersistorWithInferredConfigs<Integer>() {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
                return DEPRECATIONS;
            }

            @Override
            public String getConfigKey() {
                return configKey;
            }

        };

        final var configMappings = persistor.getConfigMappings(0);
        assertThat(configMappings.m_children).hasSize(1);
        final var child = configMappings.m_children.iterator().next();
        assertThat(child.m_newAndDeprecatedConfigPaths.getNewConfigPaths())
            .isEqualTo(List.of(new ConfigPath(List.of(configKey))));
    }

    @Test
    void throwsIfNewConfigPathsCannotBeInferred() {
        final var persistor = new NodeSettingsPersistor<Integer>() {

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
                return null; // NOSONAR

            }

            @Override
            public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
                return DEPRECATIONS;
            }
        };

        assertThrows(IllegalStateException.class, () -> persistor.getConfigMappings(0));
    }

}
