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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.ValidationOptions;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.DefaultNumberValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.DefaultNumberValidationUtil.ValidationClassInstance;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation;

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
        final var defaultValidations = DefaultNumberValidationUtil.getDefaultNumberValidations(getTypeBounds());
        final var customOptions = getCustomOptions();
        if (customOptions.isEmpty() && defaultValidations.isEmpty()) {
            return Optional.empty();
        }
        final var validations =
            customOptions.flatMap(NumberRendererOptions::getValidations).orElse(new NumberInputWidgetValidation[0]);

        final var combinedValidations = getCombinedValidations(validations, defaultValidations);
        return Optional.of(new NumberRendererOptions() {

            @Override
            public Optional<NumberInputWidgetValidation[]> getValidations() {
                return Optional.of(combinedValidations);
            }

            @Override
            public Optional<String[]> getValidationProviders() {
                return customOptions.flatMap(NumberRendererOptions::getValidationProviders);
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
     * Options for rendering a text input field.
     */
    interface NumberRendererOptions extends ValidationOptions<NumberInputWidgetValidation> {

    }

    @Override
    default JsonDataType getDataType() {
        return JsonDataType.NUMBER;
    }

    private static NumberInputWidgetValidation[] getCombinedValidations(final NumberInputWidgetValidation[] validations,
        final List<ValidationClassInstance<NumberInputWidgetValidation>> defaultValidations) {
        final var filteredDefaultValidationInstances = defaultValidations.stream() //
            .filter(defaultValidation -> Arrays.stream(validations).noneMatch(defaultValidation.clazz()::isInstance))
            .map(ValidationClassInstance::instance);
        return Stream.concat(Arrays.stream(validations), filteredDefaultValidationInstances)
            .toArray(NumberInputWidgetValidation[]::new);
    }

}
