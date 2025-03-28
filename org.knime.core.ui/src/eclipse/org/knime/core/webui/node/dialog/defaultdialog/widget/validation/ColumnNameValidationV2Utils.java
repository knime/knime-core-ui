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
 *   28 Mar 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;

/**
 * Utility class to provide a common implementation for the column name validation
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public final class ColumnNameValidationV2Utils {

    private ColumnNameValidationV2Utils() {
    }

    /**
     * A node settings persistor to load and save a utility boolean indicating the version of column name validation to
     * use. The original column validation is node specific. When used, the column name validation v2 does not allow
     * blank values or values that do not start and end with a non-whitespace character
     * {@link #validatePossiblyEmptyColumnName(String, String)} or empty values
     * {@link #validateColumnName(String, String)}. If the settings contain the boolean, the column name validation v2
     * should be used in the node model (when the corresponding validation methods are used), else, the original
     * validation should be used.
     *
     */
    public abstract static class AbstractIsColumnNameValidationV2Persistor implements NodeSettingsPersistor<Boolean> {

        private final String m_configKey;

        /**
         * @param configKey the root config key used by the settings model
         */
        protected AbstractIsColumnNameValidationV2Persistor(final String configKey) {
            m_configKey = configKey + SettingsModel.CFGKEY_INTERNAL;
        }

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.containsKey(m_configKey);
        }

        @Override
        public void save(final Boolean obj, final NodeSettingsWO settings) {
            settings.addBoolean(m_configKey, true);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][0];
        }
    }

    /**
     * Checks whether the possible empty column name is valid, i.e. it is neither blank nor different when trimmed. Use
     * the {@link AbstractIsColumnNameValidationV2Persistor} if the old validation differs from this one.
     *
     * @param columnName the column name to validate
     * @param settingTitle the title of the setting of the column name to validate
     * @throws InvalidSettingsException when column name is blank or does not start and end with a non-whitespace
     *             character
     */
    public static void validatePossiblyEmptyColumnName(final String columnName, final String settingTitle)
        throws InvalidSettingsException {
        if (columnName != null) {
            if (columnName.isBlank() && !columnName.isEmpty()) {
                throw new InvalidSettingsException(
                    String.format("The value of the setting \"%s\" is invalid because it is blank.", settingTitle));
            } else if (columnName.trim().length() != columnName.length()) {
                throw new InvalidSettingsException(
                    String.format("The value (\"%s\") of the setting (\"%s\") is invalid because it does not start and "
                        + "end with a non-whitespace character.", columnName, settingTitle));
            }
        }
    }

    /**
     * Checks whether the column name is valid, i.e. it is neither empty, nor blank, nor different when trimmed. Use the
     * {@link AbstractIsColumnNameValidationV2Persistor} if the old validation differs from this one.
     *
     * @param columnName the column name to validate
     * @param settingTitle the title of the setting of the column name to validate
     * @throws InvalidSettingsException when column name is empty, blank, or does not start and end with a
     *             non-whitespace character
     */
    public static void validateColumnName(final String columnName, final String settingTitle)
        throws InvalidSettingsException {
        if (columnName == null || columnName.isEmpty()) {
            throw new InvalidSettingsException(
                String.format("The value of the setting \"%s\" is invalid because it is empty.", settingTitle));
        }
        validatePossiblyEmptyColumnName(columnName, settingTitle);
    }

}
