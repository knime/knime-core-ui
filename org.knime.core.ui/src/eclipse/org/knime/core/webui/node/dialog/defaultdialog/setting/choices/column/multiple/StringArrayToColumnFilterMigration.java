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
package org.knime.core.webui.node.dialog.defaultdialog.setting.choices.column.multiple;

import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;

/**
 * The data structure of a TwinList changed from an array of strings to a more complex representation by a
 * {@link ColumnFilter}. For previous workflows to still execute (given that the setting is not overwritten by a flow
 * variable), we transform the stored string array to the correct representation.
 *
 * @author Paul Bärnreuther
 */
public abstract class StringArrayToColumnFilterMigration implements NodeSettingsMigration<ColumnFilter> {

    private final String m_legacyConfigKey;

    /**
     * The config key under which the string array has been persisted before has to be deprecated to not break flow
     * variables. I.e. either rename the field or add a {@link Persist#configKey} annotation on the field where this
     * class is attached.
     *
     * @param legacyConfigKey the config key under which the string array has been stored previously.
     */
    protected StringArrayToColumnFilterMigration(final String legacyConfigKey) {
        m_legacyConfigKey = legacyConfigKey;
    }

    private ColumnFilter loadLegacy(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var fieldSettingsArray = settings.getStringArray(m_legacyConfigKey);
        if (fieldSettingsArray != null) {
            return new ColumnFilter(fieldSettingsArray);
        } else {
            return loadSettings(ColumnFilter.class, settings.getNodeSettings(m_legacyConfigKey));
        }
    }

    @Override
    public final List<ConfigMigration<ColumnFilter>> getConfigMigrations() {
        return List
            .of(ConfigMigration.builder(this::loadLegacy).withDeprecatedConfigPath(m_legacyConfigKey).build());
    }

}
