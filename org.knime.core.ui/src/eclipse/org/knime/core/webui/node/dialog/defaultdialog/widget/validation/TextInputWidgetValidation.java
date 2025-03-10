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
 *   7 Mar 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;

/**
 * This interface specifies the different validations for the {@link TextInputWidget}.
 *
 * @author Robin Gerling
 * @param <T>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface TextInputWidgetValidation<T> extends BasicValidation<T> {

    /**
     * Implement this interface to validate the input against a given regex.
     */
    interface RegExValidation extends TextInputWidgetValidation<String> {

        /**
         * @return the pattern that the value of the input field must conform to
         */
        String getPattern();

        @Override
        default String getErrorMessage(final String value) {
            return String.format("The string must match the pattern: %s", value);
        }

        /**
         * Validates whether the input is not blank, i.e. whether it contains at least one non-space character.
         */
        final class IsNotBlankValidation implements RegExValidation {

            @Override
            public String getErrorMessage(final String value) {
                return "The field cannot be blank (it must contain at least one non-space character).";
            }

            @Override
            public String getPattern() {
                return ".*\\S+.*";
            }

        }

        /**
         * Validates whether the input is not empty, i.e. whether it contains at least one character.
         */
        final class IsNotEmptyValidation implements RegExValidation {

            @Override
            public String getErrorMessage(final String value) {
                return "The field cannot be empty (it must contain at least one character).";
            }

            @Override
            public String getPattern() {
                return ".+";
            }
        }
    }

    /**
     * Implement this interface to validate the length of the input against a given minLength.
     */
    interface MinLengthValidation extends TextInputWidgetValidation<Integer> {

        int getMinLength();

        @Override
        default String getErrorMessage(final Integer value) {
            return String.format("The string must be at least %d characters long.", value);
        }

    }

    /**
     * Implement this interface to validate the length of the input against a given maxLength.
     */
    interface MaxLengthValidation extends TextInputWidgetValidation<Integer> {

        int getMaxLength();

        @Override
        default String getErrorMessage(final Integer value) {
            return String.format("The string must not exceed %d characters.", value);
        }

    }
}
