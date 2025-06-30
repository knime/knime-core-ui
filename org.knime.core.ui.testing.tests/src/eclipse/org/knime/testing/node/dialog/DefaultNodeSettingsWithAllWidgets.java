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
 *   Oct 9, 2024 (Paul Bärnreuther): created
 */
package org.knime.testing.node.dialog;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable.FlowVariableFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;

/**
 * A class with all possible widgets for testing backwards compatibility of their snapshots.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
class DefaultNodeSettingsWithAllWidgets implements DefaultNodeSettings {

    @Widget(title = "File Chooser", description = "")
    FileSelection m_fileSelection = new FileSelection();

    @Widget(title = "Credentials", description = "")
    Credentials m_credentials = new Credentials();

    @Widget(title = "Dynamic Values Input", description = "")
    DynamicValuesInput m_dynamicValuesInput = DynamicValuesInput.emptySingle();

    static final class MyColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }
    }

    @Widget(title = "Column Selection", description = "")
    @ChoicesProvider(MyColumnChoicesProvider.class)
    String m_columnSelection;

    @Widget(title = "Column Filter", description = "")
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Flow Variable Filter", description = "")
    FlowVariableFilter m_flowVariableFilter = new FlowVariableFilter();

    interface SimpleButtonWidgetRef extends ButtonReference {
    }

    @Widget(title = "Simple Button", description = "")
    @SimpleButtonWidget(ref = SimpleButtonWidgetRef.class)
    Void m_simpleButton;

    static final class MyValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(SimpleButtonWidgetRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            throw new IllegalAccessError("Should not be called within this test");
        }

    }

    @Widget(title = "String Setting", description = "")
    @ValueProvider(MyValueProvider.class)
    String m_string;

    static final class NestedSettings implements DefaultNodeSettings {
        @Widget(title = "Nested String Setting", description = "")
        String m_nestedString;
    }

    NestedSettings m_nested = new NestedSettings();

    @Persist(configKey = "myNestedSettings")
    NestedSettings m_nestedSettingsRenamed = new NestedSettings();

    static final class CustomPersistor implements NodeSettingsPersistor<NestedSettings> {

        @Override
        public NestedSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public void save(final NestedSettings obj, final NodeSettingsWO settings) {
            settings.addString("customConfigKeyForNestedSettings", "fromCustomPersistor");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"customConfigKeyForNestedSettings"}};
        }

    }

    static final class Migrator implements NodeSettingsMigration<NestedSettings> {
        @Override
        public List<ConfigMigration<NestedSettings>> getConfigMigrations() {
            return List.of(ConfigMigration.<NestedSettings> builder(settings -> {
                throw new IllegalStateException("Should not be called within this test");
            }).withDeprecatedConfigPath("deprecatedConfigKey").build());

        }
    }

    @Persistor(CustomPersistor.class)
    @Migration(Migrator.class)
    NestedSettings m_nestedSettingsWithCustomPersistor;

}
