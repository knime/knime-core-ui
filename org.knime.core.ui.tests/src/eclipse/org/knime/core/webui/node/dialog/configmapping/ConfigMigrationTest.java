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
 *   Oct 22, 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.configmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.ConfigMigration.Builder;

class ConfigMigrationTest {

    private static final Builder<Integer> createBuilder(final Optional<Predicate<NodeSettingsRO>> matcher) {
        final var builder = new ConfigMigration.Builder<Integer>(settings -> {
            throw new IllegalStateException("Should not be called within this test");
        }).withDeprecatedConfigPath("A", "B").withDeprecatedConfigPath("C");

        if (matcher.isPresent()) {
            builder.withMatcher(matcher.get());
        }
        return builder;
    }

    @Test
    void testBuilderUsesGivenMatcher() {
        final Predicate<NodeSettingsRO> matcher = settings -> {
            try {
                return settings.getNodeSettings("A").containsKey("B");
            } catch (InvalidSettingsException ex) {
                throw new IllegalStateException(ex);
            }
        };
        final var configsDeprecation = createBuilder(Optional.of(matcher)).build();

        assertEquals(matcher, configsDeprecation.getMatcher());
    }

    @Test
    void testBuilderCreatesMatcherByDeprecatedConfigs() throws InvalidSettingsException {
        final var builder = createBuilder(Optional.empty()).build();

        final var settings = new NodeSettings("root");
        final var settingsA = new NodeSettings("A");
        settingsA.addString("B", "root -> A -> B");
        settings.addNodeSettings(settingsA);
        assertFalse(builder.getMatcher().test(settings));

        settings.addString("C", "root -> C");
        assertTrue(builder.getMatcher().test(settings));
    }

}
