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
import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;

/**
 * With 5.5 we removed the ColumnSelection setting and nodes that used it need to use a String instead. In some cases,
 * these ColumnSelections stored certain special choices (e.g. NONE) in special strings (e.g. "<none>" . This migration
 * helps to convert these ColumnSelections to Strings.
 *
 * It also handles cases where that ColumnSelection previously had been migrated from a string.
 *
 * @param <E> the enum defining the special choices
 * @author Paul Bärnreuther
 */
public abstract class ColumnSelectionToStringOrEnumMigration<E extends Enum<E>>
    implements NodeParametersMigration<StringOrEnum<E>>, ToStringOrEnumMigration<E> {

    private final String m_legacyColumnSelectionConfigKey;

    /**
     * Null if no such state existed
     */
    private final String m_legacyStringOrColumnSelectionConfigKey;

    /**
     * Use this constructor if the field has never been a String before it was a ColumnSelection.
     *
     * @param legacyConfigKey the key that was previously used to persist the ColumnSelection
     */
    protected ColumnSelectionToStringOrEnumMigration(final String legacyConfigKey) {
        this(null, legacyConfigKey);

    }

    /**
     *
     * Use this constructor if the to be migrated field had been migrated from a String to a ColumnSelection in the
     * past.
     *
     * @param legacyStringOrColumnSelectionConfigKey the key that was previously used to persist a String or a
     *            ColumnSelection
     * @param legacyColumnSelectionConfigKey the key that was previously used to persist the ColumnSelection
     */
    protected ColumnSelectionToStringOrEnumMigration(final String legacyStringOrColumnSelectionConfigKey,
        final String legacyColumnSelectionConfigKey) {
        m_legacyStringOrColumnSelectionConfigKey = legacyStringOrColumnSelectionConfigKey;
        m_legacyColumnSelectionConfigKey = legacyColumnSelectionConfigKey;
    }

    @Override
    public List<ConfigMigration<StringOrEnum<E>>> getConfigMigrations() {
        return ColumnSelectionToStringMigration.defineConfigMigrations(//
            m_legacyColumnSelectionConfigKey, this::loadFromColumnSelection, //
            m_legacyStringOrColumnSelectionConfigKey, this::loadFromStringOrColumnSelection//
        );
    }

    private StringOrEnum<E> loadFromStringOrColumnSelection(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        /**
         * Cannot be null since otherwise the resp. migration would not have been registered
         */
        Objects.requireNonNull(m_legacyStringOrColumnSelectionConfigKey);
        final var oldString = ColumnSelectionToStringMigration
            .loadFromStringOrColumnSelectionAtKey(m_legacyStringOrColumnSelectionConfigKey, settings);
        return loadFromLegacyString(oldString);
    }

    private StringOrEnum<E> loadFromColumnSelection(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var oldString =
            ColumnSelectionToStringMigration.loadFromColumnSelectionAtKey(m_legacyColumnSelectionConfigKey, settings);
        return loadFromLegacyString(oldString);
    }

}
