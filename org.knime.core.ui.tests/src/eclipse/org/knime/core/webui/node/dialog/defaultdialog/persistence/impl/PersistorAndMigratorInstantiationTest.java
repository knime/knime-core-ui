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
 *   Feb 9, 2023 (benjamin): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigrator;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistorContext;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;

/**
 * Contains unit tests for the {@link NodeSettingsPersistor}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
class PersistorAndMigratorInstantiationTest {

    private interface TestPersistor<T> extends NodeSettingsPersistor<T> {

        @Override
        public default T load(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new UnsupportedOperationException("not used by tests");
        }

        @Override
        public default void save(final T obj, final NodeSettingsWO settings) {
            throw new UnsupportedOperationException("not used by tests");
        }

        @Override
        public default String[][] getConfigPaths() {
            throw new UnsupportedOperationException("not used by tests");
        }

    }

    static class CustomPersistorWithDefaultConstructor implements TestPersistor<Integer> {

        private final int m_loadedValue;

        CustomPersistorWithDefaultConstructor() {
            m_loadedValue = 42;
        }

        @Override
        public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return m_loadedValue;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"config_key_by_method"}};
        }
    }

    static final class TestSettingsWithCustomPersistorWithDefaultConstructor implements PersistableSettings {

        @Persistor(CustomPersistorWithDefaultConstructor.class)
        Integer m_value;

    }

    @Test
    void testCreateInstance() throws InvalidSettingsException {

        final var testSettings =
            SettingsLoaderFactory.loadSettings(TestSettingsWithCustomPersistorWithDefaultConstructor.class, null);

        assertThat(testSettings.m_value).isEqualTo(42)
            .as("should use custom persistor instance with default constructor.");
    }

    static class CustomPersistorWithContext implements TestPersistor<String> {

        private final NodeSettingsPersistorContext<String> m_context;

        CustomPersistorWithContext(final NodeSettingsPersistorContext<String> context) {
            m_context = context;
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return String.format("fieldName: %s, class: %s", m_context.getFieldName(),
                m_context.getPersistedObjectClass().getSimpleName());
        }
    }

    static final class TestSettingsWithCustomPersistorWithContext implements PersistableSettings {

        @Persistor(CustomPersistorWithContext.class)
        String m_value;

    }

    @Test
    void testCreateInstanceWithContext() throws InvalidSettingsException {
        final var testSettings =
            SettingsLoaderFactory.loadSettings(TestSettingsWithCustomPersistorWithContext.class, null);

        assertThat(testSettings.m_value).isEqualTo("fieldName: value, class: String")
            .as("should use custom persistor instance with context constructor.");
    }

    static final class CustomPersistorWithoutSuitableConstructor implements TestPersistor<String> {

        /**
         * @param arg not used
         */
        CustomPersistorWithoutSuitableConstructor(final String arg) {
            throw new UnsupportedOperationException("not used by tests");
        }

    }

    static final class TestSettingsWithConstructorWithoutSuitableConstructor implements PersistableSettings {

        @Persistor(CustomPersistorWithoutSuitableConstructor.class)
        String m_value;

    }

    @Test
    void throwsIfNoSuitableConstructor() {
        assertThat(assertThrows(IllegalStateException.class, () -> SettingsLoaderFactory
            .loadSettings(TestSettingsWithConstructorWithoutSuitableConstructor.class, null)))
                .hasMessageContaining("CustomPersistorWithoutSuitableConstructor", "constructor");
    }

    static abstract class AbstractCustomPersistor implements TestPersistor<String> {

        AbstractCustomPersistor() {
            throw new UnsupportedOperationException("not used by tests");
        }

    }

    static abstract class AbstractMigrator implements NodeSettingsMigrator<String> {

        AbstractMigrator() {
            throw new UnsupportedOperationException("not used by tests");
        }

        @Override
        public List<ConfigsDeprecation<String>> getConfigsDeprecations() {
            throw new UnsupportedOperationException("not used by tests");
        }
    }

    static final class TestSettingsWithAbstractPersistor implements PersistableSettings {

        @Persistor(AbstractCustomPersistor.class)
        String m_value;

    }

    static final class TestSettingsWithAbstractMigrator implements PersistableSettings {

        @Migration(AbstractMigrator.class)
        String m_value;

    }

    @Test
    void throwsIfAbstractClass() {
        assertThat(assertThrows(IllegalStateException.class,
            () -> SettingsLoaderFactory.loadSettings(TestSettingsWithAbstractPersistor.class, null)))
                .hasMessageContaining("AbstractCustomPersistor", "abstract");
        assertThat(assertThrows(IllegalStateException.class,
            () -> SettingsLoaderFactory.loadSettings(TestSettingsWithAbstractMigrator.class, null)))
                .hasMessageContaining("AbstractMigrator", "abstract");
    }

    static final class MigratorWithDefaultConstructor implements NodeSettingsMigrator<Integer> {
        final int m_loadedValue;

        MigratorWithDefaultConstructor() {
            m_loadedValue = 42;

        }

        @Override
        public List<ConfigsDeprecation<Integer>> getConfigsDeprecations() {
            return List.of(ConfigsDeprecation.builder(settings -> m_loadedValue).build());
        }

    }

    static final class TestSettingsWithMigratorWithDefaultConstructor implements PersistableSettings {

        @Migration(MigratorWithDefaultConstructor.class)
        int m_value;

    }

    @Test
    void testCreateMigratorInstance() throws InvalidSettingsException {
        final var testSettings = SettingsLoaderFactory
            .loadSettings(TestSettingsWithMigratorWithDefaultConstructor.class, new NodeSettings("root"));

        assertThat(testSettings.m_value).isEqualTo(42)
            .as("should use custom migrator instance with default constructor.");
    }

    static final class MigratorWithoutDefaultConstructor implements NodeSettingsMigrator<String> {
        final String m_loadedValue;

        MigratorWithoutDefaultConstructor(final String loadedValue) {
            m_loadedValue = loadedValue;
        }

        @Override
        public List<ConfigsDeprecation<String>> getConfigsDeprecations() {
            return List.of(ConfigsDeprecation.builder(settings -> m_loadedValue).build());
        }

    }

    static final class TestSettingsWithMigratorWithoutDefaultConstructor implements PersistableSettings {

        @Migration(MigratorWithoutDefaultConstructor.class)
        String m_value;

    }

    @Test
    void throwsIfNoDefaultConstructorForMigrator() {
        assertThat(assertThrows(IllegalStateException.class,
            () -> SettingsLoaderFactory.loadSettings(TestSettingsWithMigratorWithoutDefaultConstructor.class, null)))
                .hasMessageContaining("MigratorWithoutDefaultConstructor", "constructor");
    }

}
