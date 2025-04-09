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

import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;

/**
 * Utility class to provide a common implementation for the column name validation
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
public final class ColumnNameValidationUtils {

    private ColumnNameValidationUtils() {
    }

    /**
     * Checks whether the possible empty column name is valid, i.e. it is neither blank nor different when trimmed.
     *
     * @param columnName the column name to validate
     * @param invalidStateToMessage a function mapping from the different invalid column name states to a corresponding
     *            error message
     * @throws InvalidSettingsException if the given column name is invalid
     */
    public static void validatePossiblyEmptyColumnName(final String columnName,
        final Function<InvalidColumnNameState, String> invalidStateToMessage) throws InvalidSettingsException {
        if (columnName == null || columnName.isEmpty()) {
            return;
        }
        if (columnName.isBlank()) {
            throw new InvalidSettingsException(invalidStateToMessage.apply(InvalidColumnNameState.BLANK));
        }
        if (columnName.trim().length() != columnName.length()) {
            throw new InvalidSettingsException(invalidStateToMessage.apply(InvalidColumnNameState.NOT_TRIMMED));
        }
    }

    /**
     * Checks whether the column name is valid, i.e. it is neither empty, nor blank, nor different when trimmed.
     *
     * @param columnName the column name to validate
     * @param invalidStateToMessage a function mapping from the different invalid column name states to a corresponding
     *            error message
     * @throws InvalidSettingsException if the given column name is invalid
     */
    public static void validateColumnName(final String columnName,
        final Function<InvalidColumnNameState, String> invalidStateToMessage) throws InvalidSettingsException {
        if (columnName == null || columnName.isEmpty()) {
            throw new InvalidSettingsException(invalidStateToMessage.apply(InvalidColumnNameState.EMPTY));
        }
        validatePossiblyEmptyColumnName(columnName, invalidStateToMessage);
    }

    /**
     * Specifies the different states of an invalid column name.
     */
    public enum InvalidColumnNameState {
            /**
             * column name is null/empty
             */
            EMPTY,
            /**
             * column name is blank, but not null/empty
             */
            BLANK,
            /**
             * column name starts and/or ends with whitespace
             */
            NOT_TRIMMED
    }

}
