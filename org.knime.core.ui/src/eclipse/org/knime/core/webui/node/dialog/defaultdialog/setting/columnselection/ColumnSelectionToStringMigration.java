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
 *   Mar 13, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection;

import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;

/**
 * With 5.5 we removed the ColumnSelection setting and nodes that used it need to use a String instead. Using this
 * migration ensures backwards compatibility for those cases. If the field already had been migrated from a String to a
 * ColumnSelection, one can ensure backwards-compatibility with respect to that first String-version by reusing the same
 * config key that had been deprecated before. If that config key had not been deprecated but instead is the same as the
 * one used for the ColumnSelection, this migration ensures backwards-compatibility to that old state as well (i.e.,
 * this is really a "String or ColumnSelection to String migration")
 *
 * @author Paul Bärnreuther
 */
public abstract class ColumnSelectionToStringMigration implements NodeSettingsMigration<String> {

    private final String m_legacyColumnSelectionConfigKey;

    private final Optional<String> m_legacyStringOrColumnSelectionConfigKey;

    /**
     * Use this constructor if the field has never been a String before it was a ColumnSelection.
     *
     * @param legacyConfigKey the key that was previously used to persist the ColumnSelection
     */
    protected ColumnSelectionToStringMigration(final String legacyConfigKey) {
        this(null, legacyConfigKey);

    }

    /**
     *
     * Use this constructor if the to be migrated field had been migrated from a String to a ColumnSelection in the
     * past.
     *
     * @param legacyStringOrColumnSelectionConfigKey the key that was previously used to persist a String or a
     *            ColumnSelection
     * @param legacyColumnFilterConfigKey the key that was previously used to persist the ColumnSelection
     */
    protected ColumnSelectionToStringMigration(final String legacyStringOrColumnSelectionConfigKey,
        final String legacyColumnFilterConfigKey) {
        m_legacyStringOrColumnSelectionConfigKey = Optional.ofNullable(legacyStringOrColumnSelectionConfigKey);
        m_legacyColumnSelectionConfigKey = legacyColumnFilterConfigKey;

    }

    @Override
    public List<ConfigMigration<String>> getConfigMigrations() {
        final var builder = ConfigMigration.builder(this::loadFromStringOrColumnSelection)
            .withDeprecatedConfigPath(m_legacyColumnSelectionConfigKey);

        m_legacyStringOrColumnSelectionConfigKey.ifPresent(builder::withDeprecatedConfigPath);

        return List.of(builder.build());
    }

    private String loadFromStringOrColumnSelection(final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadFromStringOrColumnSelection(m_legacyColumnSelectionConfigKey,
            m_legacyStringOrColumnSelectionConfigKey, settings);
    }

    static String loadFromStringOrColumnSelection(final String legacyColumnSelectionConfigKey,
        final Optional<String> legacyStringOrColumnSelectionConfigKey, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        try {
            /**
             * Trying to load the current state where the setting is a ColumnSelection. ColumnSelection used to be a
             * POJO with two fields, "selected" and "compatibleTypes".
             */
            return loadFromColumnSelection(legacyColumnSelectionConfigKey, settings);
        } catch (InvalidSettingsException ex) { //NOSONAR
            if (legacyStringOrColumnSelectionConfigKey.isEmpty()) {
                throw ex;
            }
            return loadFromColumnSelectionOrString(legacyStringOrColumnSelectionConfigKey.get(), settings);

        }
    }

    private static String loadFromColumnSelectionOrString(final String key, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        try {
            return loadFromColumnSelection(key, settings);
        } catch (InvalidSettingsException ex) { //NOSONAR
            /**
             * Trying to load an even older state where the setting was a String directly.
             */
            return settings.getString(key);
        }
    }

    private static String loadFromColumnSelection(final String key, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return settings.getNodeSettings(key).getString("selected");
    }

}