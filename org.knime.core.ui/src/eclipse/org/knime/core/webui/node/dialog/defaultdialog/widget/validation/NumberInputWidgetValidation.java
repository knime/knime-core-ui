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

import java.math.BigDecimal;

import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;

/**
 * This interface specifies the different validations for the {@link NumberInputWidget}.
 *
 * @author Robin Gerling
 */
public sealed interface NumberInputWidgetValidation extends BuiltinValidation {

    /**
     * @return true when the validation should use an exclusive bound, else an inclusive bound is used
     */
    default boolean isExclusive() {
        return false;
    }

    /**
     * Implement this interface to validate a numeric input against a given minimum.
     */
    abstract non-sealed class MinValidation implements NumberInputWidgetValidation {

        @Override
        public final String getId() {
            return "min";
        }

        /**
         * @param min the minimum to validate against
         * @param isExclusive whether to use an inclusive or an exclusive bound
         */
        record MinValidationParams(double min, boolean isExclusive) {
        }

        @Override
        public final MinValidationParams getParameters() {
            return new MinValidationParams(getMin(), isExclusive());
        }

        @Override
        public String getErrorMessage() {
            return String.format(
                isExclusive() ? "The value must be greater than %s." : "The value must be at least %s.",
                BigDecimal.valueOf(getMin()).stripTrailingZeros().toPlainString());
        }

        /**
         * @return the min value to validate against
         */
        protected abstract double getMin();

        /**
         * Validates whether the input is at least 0.
         */
        public static final class IsNonNegativeValidation extends MinValidation {

            @Override
            public double getMin() {
                return 0;
            }

        }

        /**
         * Validates whether the input is at least 1.
         */
        public static final class IsPositiveIntegerValidation extends MinValidation {

            @Override
            public double getMin() {
                return 1;
            }

        }

        /**
         * Validates whether the input is greater than 0.
         */
        public static final class IsPositiveDoubleValidation extends MinValidation {

            @Override
            public double getMin() {
                return 0;
            }

            @Override
            public boolean isExclusive() {
                return true;
            }

        }

    }

    /**
     * Implement this interface to validate a numeric input against a given maximum.
     */
    abstract non-sealed class MaxValidation implements NumberInputWidgetValidation {

        @Override
        public final String getId() {
            return "max";
        }

        /**
         * @param max the maximum to validate against
         * @param isExclusive whether to use an inclusive or an exclusive bound
         */
        record MaxValidationParams(Double max, boolean isExclusive) {
        }

        @Override
        public final MaxValidationParams getParameters() {
            return new MaxValidationParams(getMax(), isExclusive());
        }

        @Override
        public String getErrorMessage() {
            return String.format(isExclusive() ? "The value must not exceed %s." : "The value must be less than %s.",
                BigDecimal.valueOf(getMax()).stripTrailingZeros().toPlainString());
        }

        /**
         * @return the max value to validate against
         */
        protected abstract double getMax();

    }

}
