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
 *   24 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeStringFormatValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.DateTimeTemporalFormatValidation;

/**
 * Utility class to retrieve the builtin external validation class name for the given annotation and field class, e.g.
 * of a {@link DateTimeFormatPickerWidget} or to retrieve the validation instance for a given validation class name.
 *
 * @author Robin Gerling
 */
public final class ExternalBuiltInValidationUtil {

    private ExternalBuiltInValidationUtil() {
    }

    private static final Map<Class<?>, Class<? extends ExternalValidation<?>>> //
    DATE_TIME_FORMAT_FIELD_TO_VALIDATION_CLASS = Map.of( //
        String.class, DateTimeStringFormatValidation.class, //
        TemporalFormat.class, DateTimeTemporalFormatValidation.class //
    );

    private static final Map<Class<? extends Annotation>, Map<Class<?>, Class<? extends ExternalValidation<?>>>> //
    WIDGET_TO_FIELD_TO_VALIDATION_CLASS =
        Map.of(DateTimeFormatPickerWidget.class, DATE_TIME_FORMAT_FIELD_TO_VALIDATION_CLASS);

    /**
     * Retrieve the validation class based on the annotation and field class of a setting.
     *
     * @param annotationClass the class of the annotation
     * @param fieldClass the class of the field in the settings
     * @return the validation class for the field
     */
    public static Class<? extends ExternalValidation<?>> getValidationHandlerClass(final Class<?> annotationClass,
        final Class<?> fieldClass) {
        return WIDGET_TO_FIELD_TO_VALIDATION_CLASS.get(annotationClass).get(fieldClass);
    }

    private static final List<Class<? extends ExternalValidation<?>>> BUILT_IN_VALIDATION_CLASSES =
        WIDGET_TO_FIELD_TO_VALIDATION_CLASS.values().stream()
            .flatMap(fieldToValidationMap -> fieldToValidationMap.values().stream()).toList();

    /**
     * Retrieve the validation handler instance based on the validation handler class.
     *
     * @param validationHandlerClassName the class name of the validation handler
     * @return the validation instance for the class
     */
    public static Optional<? extends ExternalValidation<?>>
        getValidationHandlerInstance(final String validationHandlerClassName) {
        return BUILT_IN_VALIDATION_CLASSES.stream() //
            .filter(clazz -> clazz.getName().equals(validationHandlerClassName)) //
            .map(InstantiationUtil::createInstance).findFirst();
    }

}
