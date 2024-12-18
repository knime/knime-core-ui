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
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.saveSettings;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;

class StringArrayToColumnFilterMigratorTest {

    private static final String ROOT_KEY = "Test";

    private static final class StringArrayToColumnFilterMigratorSettings implements DefaultNodeSettings {

        static final class FooMigrator extends StringArrayToColumnSelectionMigrator {

            protected FooMigrator() {
                super("foo");
            }

        }

        @Migration(FooMigrator.class)
        ColumnFilter m_fooV2;
    }

    @Test
    void testLoadsColumnFilterFromOldStringArray() throws InvalidSettingsException {
        final var array = new String[]{"bar", "baz"};

        final var savedSettings = new NodeSettings(ROOT_KEY);
        savedSettings.addStringArray("foo", array);
        final var loaded = loadSettings(StringArrayToColumnFilterMigratorSettings.class, savedSettings);

        final var expected = new StringArrayToColumnFilterMigratorSettings();
        expected.m_fooV2 = new ColumnFilter(array);
        assertResults(expected, loaded);
    }

    /**
     * The first iteration of this migrator was a persistor that saved again to the same setting. We changed that but
     * for the saved settings in the meantime we also have to be able to load from that state.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testLoadsColumnFilterFromOldKey() throws InvalidSettingsException {
        final var array = new String[]{"bar", "baz"};
        final var columnFilter = new ColumnFilter(array);
        final var savedSettings = new NodeSettings(ROOT_KEY);
        final var oldFooSettings = savedSettings.addNodeSettings("foo");
        saveSettings(columnFilter, oldFooSettings);
        final var loaded = loadSettings(StringArrayToColumnFilterMigratorSettings.class, savedSettings);

        final var expected = new StringArrayToColumnFilterMigratorSettings();
        expected.m_fooV2 = columnFilter;
        assertResults(expected, loaded);
    }

    @Test
    void testSaveAndLoad() throws InvalidSettingsException {
        final var array = new String[]{"bar", "baz"};

        final var expected = new StringArrayToColumnFilterMigratorSettings();
        expected.m_fooV2 = new ColumnFilter(array);

        final var savedSettings = new NodeSettings(ROOT_KEY);
        saveSettings(expected, savedSettings);
        var loaded = loadSettings(StringArrayToColumnFilterMigratorSettings.class, savedSettings);
        assertResults(expected, loaded);
    }

    private static void assertResults(final StringArrayToColumnFilterMigratorSettings expected,
        final StringArrayToColumnFilterMigratorSettings loaded) {
        assertArrayEquals(expected.m_fooV2.m_manualFilter.m_manuallySelected,
            loaded.m_fooV2.m_manualFilter.m_manuallySelected, "The loaded settings are not as expected");
    }

}
