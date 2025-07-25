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
 *   Mar 24, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilterModeToTypedStringFilterModeUtil;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * Use this migration whenever a {@link StringFilter} should be converted to a {@link FlowVariableFilter}.
 *
 * @author Paul Bärnreuther
 */
public abstract class StringFilterToFlowVariableFilterMigration implements NodeParametersMigration<FlowVariableFilter> {

    private final String m_legacyConfigKey;

    /**
     * @param legacyConfigKey the config key by which the setting was stored as a string filter. It has to be different
     *            to the current config key (i.e. it is deprecated).
     */
    protected StringFilterToFlowVariableFilterMigration(final String legacyConfigKey) {
        m_legacyConfigKey = legacyConfigKey;
    }

    @Override
    public List<ConfigMigration<FlowVariableFilter>> getConfigMigrations() {
        return List.of(

            ConfigMigration.builder(this::stringFilterSettingsToFlowVariableFilter)
                .withDeprecatedConfigPath(m_legacyConfigKey).build());
    }

    FlowVariableFilter stringFilterSettingsToFlowVariableFilter(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final var stringFilterSettings = settings.getNodeSettings(m_legacyConfigKey);
        final var stringFilter = SettingsLoaderFactory.loadSettings(StringFilter.class, stringFilterSettings);
        return fromStringFilter(stringFilter);
    }

    static FlowVariableFilter fromStringFilter(final StringFilter stringFilter) {
        final var flowVariableFilter = new FlowVariableFilter();
        flowVariableFilter.m_mode = StringFilterModeToTypedStringFilterModeUtil.convert(stringFilter.m_mode);
        flowVariableFilter.m_manualFilter = stringFilter.m_manualFilter;
        flowVariableFilter.m_patternFilter = stringFilter.m_patternFilter;
        return flowVariableFilter;
    }

}
