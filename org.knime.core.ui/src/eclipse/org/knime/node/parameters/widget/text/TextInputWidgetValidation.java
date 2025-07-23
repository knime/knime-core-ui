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
package org.knime.node.parameters.widget.text;

import org.knime.node.parameters.validation.BuiltinValidation;

/**
 * This interface specifies the different possible validations for the {@link TextInputWidget}.
 *
 * @author Robin Gerling
 */
public sealed interface TextInputWidgetValidation extends BuiltinValidation {

    /**
     * The parameters needed for the {@link PatternValidation}
     *
     * @param pattern to validate against
     */
    record PatternValidationParams(String pattern) {
    }

    /**
     * Implement this interface to validate the input against a given pattern.
     */
    abstract non-sealed class PatternValidation implements TextInputWidgetValidation {

        @Override
        public final PatternValidationParams getParameters() {
            return new PatternValidationParams(getPattern());
        }

        @Override
        public String getErrorMessage() {
            return String.format("The string must match the pattern: %s", getPattern());
        }

        /**
         * @return the pattern that the value of the input field must conform to
         */
        protected abstract String getPattern();

        /**
         * Validates whether the input is not blank, i.e. whether it contains at least one non-whitespace character.
         */
        public static final class IsNotBlankValidation extends PatternValidation {

            @Override
            public String getErrorMessage() {
                return "The field cannot be blank (it must contain at least one non-whitespace character).";
            }

            @Override
            public String getPattern() {
                return ".*\\S.*";
            }

        }

        /**
         * Validates whether the input is not empty, i.e. whether it contains at least one character.
         */
        public static final class IsNotEmptyValidation extends MinLengthValidation {

            @Override
            public String getErrorMessage() {
                return "The field cannot be empty (it must contain at least one character).";
            }

            @Override
            protected int getMinLength() {
                return 1;
            }

        }

        /**
         * Validates whether the input is a single character.
         */
        public static final class IsSingleCharacterValidation extends PatternValidation {

            @Override
            public String getErrorMessage() {
                return "The string must be a single character.";
            }

            @Override
            public String getPattern() {
                return "^.$";
            }
        }
    }

    /**
     * Implement this interface to validate the length of the input against a given minLength.
     */
    abstract non-sealed class MinLengthValidation implements TextInputWidgetValidation {

        /**
         * @param minLength the length to validate against
         */
        record MinLengthValidationParams(int minLength) {
        }

        @Override
        public final MinLengthValidationParams getParameters() {
            return new MinLengthValidationParams(getMinLength());
        }

        @Override
        public String getErrorMessage() {
            final var minLength = getMinLength();
            return String.format("The string must be at least %d character%s long.", minLength,
                minLength == 1 ? "" : "s");
        }

        protected abstract int getMinLength();

    }

    /**
     * Implement this interface to validate the length of the input against a given maxLength.
     */
    abstract non-sealed class MaxLengthValidation implements TextInputWidgetValidation {

        /**
         * @param maxLength the length to validate against
         */
        record MaxLengthValidationParams(int maxLength) {
        }

        @Override
        public final MaxLengthValidationParams getParameters() {
            return new MaxLengthValidationParams(getMaxLength());
        }

        @Override
        public String getErrorMessage() {
            final var maxLength = getMaxLength();
            return String.format("The string must not exceed %d character%s.", maxLength, maxLength == 1 ? "" : "s");
        }

        protected abstract int getMaxLength();

        /**
         * Validates whether the input has at max 1 character.
         */
        public static final class HasAtMaxOneCharValidation extends MaxLengthValidation {

            @Override
            public int getMaxLength() {
                return 1;
            }

        }

    }
}
