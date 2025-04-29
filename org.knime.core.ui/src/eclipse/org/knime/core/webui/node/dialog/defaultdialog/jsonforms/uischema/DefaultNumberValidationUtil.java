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
 *   11 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.util.List;
import java.util.Map;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec.TypeBounds;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.BuiltinValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MaxValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation;

/**
 * Utility class to generate default {@link MinValidation} and {@link MaxValidation} for specific numeric types.
 *
 * @author Robin Gerling
 */
public final class DefaultNumberValidationUtil {

    private DefaultNumberValidationUtil() {
    }

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    private record NumberValidation(Number value, String message) {
    }

    private record NumberValidationMinMax(NumberValidation min, NumberValidation max) {
    }

    /**
     * Record used to specify the interface a potential anonymous class implements.
     *
     * @param <T> the type of the validation
     * @param clazz the class of the implemented validation interface
     * @param instance a specific validation instance implementing the clazz interface
     */
    public record ValidationClassInstance<T extends BuiltinValidation>(Class<? extends T> clazz, T instance) {
    }

    private static final Map<TypeBounds, NumberValidationMinMax> //
    NUMERIC_TYPES_WITH_BUILTIN_BOUND_VALIDATION = Map.of( //
        TypeBounds.BYTE, new NumberValidationMinMax( //
            new NumberValidation(Byte.MIN_VALUE, null), //
            new NumberValidation(Byte.MAX_VALUE, null)), //
        TypeBounds.INTEGER, new NumberValidationMinMax( //
            new NumberValidation(Integer.MIN_VALUE, null), //
            new NumberValidation(Integer.MAX_VALUE, null)), //
        TypeBounds.LONG, new NumberValidationMinMax( //
            new NumberValidation(-MAX_SAFE_INTEGER, String.format( //
                "Value too small to process without risking precision loss (< %d).", -MAX_SAFE_INTEGER)), //
            new NumberValidation(MAX_SAFE_INTEGER, String.format( //
                "Value too large to process without risking precision loss (> %d).", MAX_SAFE_INTEGER))) //
    );

    /**
     * Creates {@link MinValidation} and {@link MaxValidation} for byte and int using the corresponding
     * MIN_VALUE/MAX_VALUE. For long, the MIN_SAFE_INTEGER/MAX_SAFE_INTEGER are used since using MIN_VALUE/MAX_VALUE
     * would result in precision loss.
     *
     * @param typeBounds for which to get the default validations
     * @return the default number validations
     */
    public static List<ValidationClassInstance<NumberInputWidgetValidation>>
        getDefaultNumberValidations(final TypeBounds typeBounds) {
        if (!NUMERIC_TYPES_WITH_BUILTIN_BOUND_VALIDATION.containsKey(typeBounds)) {
            return List.of();
        }
        final var minMax = NUMERIC_TYPES_WITH_BUILTIN_BOUND_VALIDATION.get(typeBounds);
        final var min = minMax.min();
        final var max = minMax.max();
        return List.of( //
            new ValidationClassInstance<>(MinValidation.class, //
                new MinValidation() {
                    @Override
                    protected double getMin() {
                        return min.value().doubleValue();
                    }

                    @Override
                    public String getErrorMessage() {
                        return min.message() == null ? super.getErrorMessage() : min.message();
                    }
                }),
            new ValidationClassInstance<>(MaxValidation.class, //
                new MaxValidation() {
                    @Override
                    protected double getMax() {
                        return max.value().doubleValue();
                    }

                    @Override
                    public String getErrorMessage() {
                        return max.message() == null ? super.getErrorMessage() : max.message();
                    }
                }));
    }
}
