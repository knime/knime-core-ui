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
 *   Apr 7, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.DefaultNumberValidationUtil.getDefaultMaxValidations;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.DefaultNumberValidationUtil.getDefaultMinValidations;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.ValidationOptions;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Either integer or floating point number renderer.
 *
 * @author Paul Bärnreuther
 */
public interface NumberRendererSpec extends ControlRendererSpec {

    /**
     * @noimplement implement {@link #getCustomOptions()} and {@link #getTypeBounds()} instead.
     */
    @Override
    default Optional<NumberRendererOptions> getOptions() {
        final var customOptions = getCustomOptions();
        final var maxValidation = customOptions.flatMap(NumberRendererOptions::getValidation)
            .flatMap(NumberRendererValidationOptions::getMax).or(() -> getDefaultMaxValidations(getTypeBounds()));
        final var minValidation = customOptions.flatMap(NumberRendererOptions::getValidation)
            .flatMap(NumberRendererValidationOptions::getMin).or(() -> getDefaultMinValidations(getTypeBounds()));
        final var stepSize = customOptions.flatMap(NumberRendererOptions::getStepSize);

        if (minValidation.isEmpty() && maxValidation.isEmpty() && stepSize.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NumberRendererOptions() {

            @Override
            public Optional<Double> getStepSize() {
                return stepSize;
            }

            @Override
            public Optional<NumberRendererValidationOptions> getValidation() {
                return Optional.of(new NumberRendererValidationOptions() {

                    @Override
                    public Optional<MaxValidation> getMax() {
                        return maxValidation;
                    }

                    @Override
                    public Optional<MinValidation> getMin() {
                        return minValidation;
                    }

                });
            }

        });

    }

    /**
     * Type bound validations are to be defined via {@link #getTypeBounds()} and are added automatically to the options.
     *
     * @return the options of the number renderer.
     */
    default Optional<NumberRendererOptions> getCustomOptions() {
        return Optional.empty();
    }

    /**
     * Returns the validations that should be applied to the number renderer due to the type of the underlying.
     *
     * @author Paul Bärnreuther
     */
    enum TypeBounds {
            /**
             * Bound using min/max save integer value to avoid precision loss.
             */
            LONG, //
            /**
             * Limit to {@link Integer} values.
             */
            INTEGER, //
            /**
             * Limit to {@link Byte} values.
             */
            BYTE, //
            /**
             * No bounds.
             */
            NONE;
    }

    /**
     * @return the type bounds given by the type of the data of the number renderer.
     */
    default TypeBounds getTypeBounds() {
        return TypeBounds.NONE;
    }

    /**
     * Options for validating the number input field.
     */
    interface NumberRendererValidationOptions extends ValidationOptions<NumberInputWidgetValidation> {

        /**
         *
         * Use this validation if the number input has an upper bound.
         *
         * @return the maximum validation of the number input
         */
        Optional<NumberInputWidgetValidation.MaxValidation> getMax();

        /**
         * Use this validation if the number input has a lower bound.
         *
         * @return the minimum validation of the number input
         */
        Optional<NumberInputWidgetValidation.MinValidation> getMin();

    }

    /**
     * Use this tag to provide the minimum validation dynamically.
     */
    String TAG_MIN_VALIDATION = "validation.min";

    /**
     * Use this tag to provide the maximum validation dynamically.
     */
    String TAG_MAX_VALIDATION = "validation.max";

    /**
     * Options for rendering a text input field.
     */
    interface NumberRendererOptions extends ValidationOptions<NumberRendererValidationOptions> {

        /**
         * @return the step size for the number input spinner buttons
         */
        default Optional<Double> getStepSize() {
            return Optional.empty();
        }
    }

    @Override
    default JsonDataType getDataType() {
        return JsonDataType.NUMBER;
    }

}
