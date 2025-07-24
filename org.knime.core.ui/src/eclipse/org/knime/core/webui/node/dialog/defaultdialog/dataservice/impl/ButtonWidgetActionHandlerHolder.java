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
 *   Jul 10, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl;

import java.util.Collection;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.WidgetGroupTraverser.TraversedField;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * The holder of all {@link ButtonWidget#actionHandler}s.
 *
 * @author Paul Bärnreuther
 */
class ButtonWidgetActionHandlerHolder extends HandlerHolder<ButtonActionHandler<?, ?, ?>> {

    /**
     * @param settingsClasses
     */
    ButtonWidgetActionHandlerHolder(final Collection<Class<? extends WidgetGroup>> settingsClasses) {
        super(settingsClasses);
    }

    @Override
    Optional<Class<? extends ButtonActionHandler<?, ?, ?>>> getHandlerClass(final TraversedField field) {
        final var buttonWidget = field.propertyWriter().getAnnotation(ButtonWidget.class);
        if (buttonWidget == null) {
            return Optional.empty();

        }
        final var actionHandlerClass = buttonWidget.actionHandler();
        validate(field.propertyWriter(), actionHandlerClass);
        return Optional.of(actionHandlerClass);

    }

    private static void validate(final PropertyWriter field,
        final Class<? extends ButtonActionHandler<?, ?, ?>> actionHandlerClass) {
        if (!isValidReturnType(field, actionHandlerClass)) {
            throw new IllegalArgumentException(
                String.format("Return type of action handler %s is not assignable to the type of the field %s.",
                    actionHandlerClass.getSimpleName(), field.getFullName()));
        }
    }

    private static boolean isValidReturnType(final PropertyWriter field,
        final Class<? extends ButtonActionHandler<?, ?, ?>> handlerClass) {
        final var returnType = GenericTypeFinderUtil.getFirstGenericType(handlerClass, ButtonActionHandler.class);
        final var fieldType = field.getType();
        if (returnType instanceof Class<?> clazz) {
            return fieldType.getRawClass().isAssignableFrom(clazz);
        }
        return JsonFormsDataUtil.getMapper().constructType(returnType).equals(fieldType);
    }

}
