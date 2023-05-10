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
 *   Mar 22, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.getApplicableDefaults;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.partitionWidgetAnnotationsByApplicability;

import java.util.Collection;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.WidgetAnnotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 *
 * @author Paul Bärnreuther
 */
final class UiSchemaOptionsGenerator {

    private final ObjectMapper m_mapper;

    private final PropertyWriter m_field;

    private final Class<?> m_fieldType;

    private final String m_fieldName;

    /**
     *
     * @param mapper the object mapper used for the ui schema generation
     * @param field the field for which options are to be added from {@link Style} annotations
     */
    UiSchemaOptionsGenerator(final ObjectMapper mapper, final PropertyWriter field) {
        m_mapper = mapper;
        m_field = field;
        m_fieldType = field.getType().getRawClass();
        m_fieldName = field.getName();
    }

    /**
     * This method applies the styles of the given field to the given control as described in {@link Style}.
     *
     * @param control
     */
    void addOptionsTo(final ObjectNode control) {

        final var defaultWidgets = getApplicableDefaults(m_fieldType);
        final var annotatedWidgets = getAnnotatedWidgets();
        if (defaultWidgets.isEmpty() && annotatedWidgets.isEmpty()) {
            return;
        }
        final var options = control.putObject(TAG_OPTIONS);
        defaultWidgets.forEach(defaultWidget -> {
            switch (defaultWidget.type()) {
                case CHECKBOX:
                    options.put(TAG_FORMAT, Format.CHECKBOX);
                    break;
                case COLUMN_FILTER:
                    options.put(TAG_FORMAT, Format.COLUMN_FILTER);
                    break;
                case VALUE_SWITCH:
                    options.put(TAG_FORMAT, Format.VALUE_SWITCH);
                    break;
            }
        });

        if (annotatedWidgets.contains(RadioButtonsWidget.class)) {
            final var radioButtons = m_field.getAnnotation(RadioButtonsWidget.class);
            options.put(TAG_FORMAT, Format.RADIO);
            if (radioButtons.horizontal()) {
                options.put("radioLayout", "horizontal");
            }
        }

        // Handle arrays and collections
        var type = m_field.getType();
        if ((type.isArrayType() || type.isCollectionLikeType()) && isObject(type.getContentType())) {
            applyArrayLayoutOptions(control, type.getContentType().getRawClass());
        }
    }

    private Collection<?> getAnnotatedWidgets() {
        final var partitionedWidgetAnnotations = partitionWidgetAnnotationsByApplicability(
            (widgetAnnotation) -> m_field.getAnnotation(widgetAnnotation) != null, m_fieldType);

        if (!partitionedWidgetAnnotations.get(false).isEmpty()) {
            throw new UiSchemaGenerationException(
                String.format("The annotation %s is not applicable for setting field %s with type %s",
                    partitionedWidgetAnnotations.get(false).get(0), m_fieldName, m_fieldType));
        }

        return partitionedWidgetAnnotations.get(true).stream().map(WidgetAnnotation::widgetAnnotation).toList();
    }

    private void applyArrayLayoutOptions(final ObjectNode control, final Class<?> componentType) {
        var options = control.putObject(OPTIONS_TAG);

        Map<String, Class<?>> arraySettings = new HashMap<>();
        arraySettings.put(null, componentType);
        var details = JsonFormsUiSchemaUtil.buildUISchema(arraySettings).get(ELEMENTS_TAG);
        options.set(ARRAY_LAYOUT_DETAIL_TAG, details);

        Optional.ofNullable(m_field.getAnnotation(ArrayWidget.class)).ifPresent(arrayWidget -> {
            var addButtonText = arrayWidget.addButtonText();
            if (!addButtonText.isEmpty()) {
                options.put(ARRAY_LAYOUT_ADD_BUTTON_TEXT_TAG, addButtonText);
            }
            var elementTitle = arrayWidget.elementTitle();
            if (!elementTitle.isEmpty()) {
                options.put(ARRAY_LAYOUT_ELEMENT_TITLE_TAG, elementTitle);
            }
        });
    }

    /** @return true if the type is from an POJO and not a primitive, String, Boxed type, or enum */
    private static boolean isObject(final JavaType contentType) {
        var boxedTypes = List.of(String.class, Double.class, Short.class, Boolean.class, Float.class, Integer.class,
            Long.class, Character.class);

        return !contentType.isPrimitive() && !contentType.isEnumType()
            && boxedTypes.stream().allMatch(type -> !type.equals(contentType.getRawClass()));
    }
}
