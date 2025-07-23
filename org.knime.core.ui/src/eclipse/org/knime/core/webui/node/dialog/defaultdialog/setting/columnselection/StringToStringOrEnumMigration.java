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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;

/**
 * With 5.5. we introduced {@link StringOrEnum} to enable special choices in a dropdown. In case a string node setting
 * needs to be migrated to this, since the encoding of special choices (e.g. row ids) as a string (e.g. "<row-keys>")
 * has been removed with that change, use one an implementation of this class.
 *
 * @param <E> the enum of the special choices
 * @author Paul Bärnreuther
 */
public abstract class StringToStringOrEnumMigration<E extends Enum<E>>
    implements NodeParametersMigration<StringOrEnum<E>>, ToStringOrEnumMigration<E> {

    private final String m_legacyStringKey;

    /**
     * @param legacyStringKey the key that was previously used to persist the String
     */
    protected StringToStringOrEnumMigration(final String legacyStringKey) {
        m_legacyStringKey = legacyStringKey;
    }

    @Override
    public List<ConfigMigration<StringOrEnum<E>>> getConfigMigrations() {
        return List.of(
            ConfigMigration.builder(this::loadLegacyFromString).withDeprecatedConfigPath(m_legacyStringKey).build());
    }

    private StringOrEnum<E> loadLegacyFromString(final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadFromLegacyString(settings.getString(m_legacyStringKey));
    }

}
