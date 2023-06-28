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

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.OPTIONS_IS_ADVANCED;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ACTION_HANDLER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_DETAIL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ELEMENTS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.getApplicableDefaults;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.partitionWidgetAnnotationsByApplicability;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.SettingsCreationContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.DialogDataServiceHandler;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaDefaultNodeSettingsTraverser.JsonFormsControl;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.util.DefaultNodeSettingsFieldTraverser;
import org.knime.core.webui.node.dialog.defaultdialog.util.DefaultNodeSettingsFieldTraverser.Field;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.CancelableActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.DeclaringClass;
import org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.WidgetAnnotation;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 *
 * @author Paul Bärnreuther
 */
final class UiSchemaOptionsGenerator {

    private final PropertyWriter m_field;

    private final Class<?> m_fieldClass;

    private final String m_fieldName;

    private final ObjectMapper m_mapper;

    private final JavaType m_fieldType;

    private final SettingsCreationContext m_settingsCreationContext;

    private final Collection<JsonFormsControl> m_fields;

    /**
     *
     * @param mapper the object mapper used for the ui schema generation
     * @param field the field for which options are to be added from {@link Style} annotations
     */
    UiSchemaOptionsGenerator(final ObjectMapper mapper, final PropertyWriter field,
        final SettingsCreationContext context, final Collection<JsonFormsControl> fields) {
        m_mapper = mapper;
        m_field = field;
        m_fieldType = field.getType();
        m_fieldClass = field.getType().getRawClass();
        m_fieldName = field.getName();
        m_settingsCreationContext = context;
        m_fields = fields;
    }

    /**
     * This method applies the styles of the given field to the given control as described in {@link Style}.
     *
     * @param control
     */
    void addOptionsTo(final ObjectNode control) {
        final var defaultWidgets = getApplicableDefaults(m_fieldClass);
        final var annotatedWidgets = getAnnotatedWidgets();
        final var isArrayOfObjects =
            (m_fieldType.isArrayType() || m_fieldType.isCollectionLikeType()) && isObject(m_fieldType.getContentType());
        if (defaultWidgets.isEmpty() && annotatedWidgets.isEmpty() && !isArrayOfObjects) {
            return;
        }
        final var options = control.putObject(TAG_OPTIONS);

        for (var defaultWidget : defaultWidgets) {
            switch (defaultWidget.type()) {
                case CHECKBOX:
                    options.put(TAG_FORMAT, Format.CHECKBOX);
                    break;
                case COLUMN_FILTER:
                    options.put(TAG_FORMAT, Format.COLUMN_FILTER);
                    break;
                case COLUMN_SELECTION:
                    options.put(TAG_FORMAT, Format.COLUMN_SELECTION);
                    break;
            }
        }

        if (annotatedWidgets.contains(Widget.class)) {
            final var widget = m_field.getAnnotation(Widget.class);
            if (widget.advanced()) {
                options.put(OPTIONS_IS_ADVANCED, true);
            }
            if (widget.hideTitle()) {
                control.put(TAG_LABEL, "");
            }
        }

        if(annotatedWidgets.contains(DateTimeWidget.class)) {
            final var dateTimeWidget = m_field.getAnnotation(DateTimeWidget.class);
            options.put(TAG_FORMAT, Format.DATE_TIME);
            options.put("showTime", dateTimeWidget.showTime());
            options.put("showSeconds", dateTimeWidget.showSeconds());
            options.put("showMilliseconds", dateTimeWidget.showMilliseconds());
            if (!dateTimeWidget.timezone().isEmpty()) {
                options.put("timezone", dateTimeWidget.timezone());
            }
            if (!dateTimeWidget.minDate().isEmpty()) {
                options.put("minimum", dateTimeWidget.minDate());
            }
            if (!dateTimeWidget.timezone().isEmpty()) {
                options.put("maximum", dateTimeWidget.maxDate());
            }
            if (!dateTimeWidget.dateFormat().isEmpty()) {
                options.put("dateFormat", dateTimeWidget.dateFormat());
            }
        }

        if (annotatedWidgets.contains(ButtonWidget.class)) {
            final var buttonWidget = m_field.getAnnotation(ButtonWidget.class);
            options.put(TAG_ACTION_HANDLER, buttonWidget.actionHandler().getName());
            options.put(TAG_FORMAT, Format.BUTTON);
            options.putObject("buttonTexts").put("invoke", buttonWidget.invokeButtonText())
                .put("cancel", buttonWidget.cancelButtonText()).put("succeeded", buttonWidget.succeededButtonText());
            options.put("displayErrorMessage", buttonWidget.displayErrorMessage());
            options.put("isMultipleUse", buttonWidget.isMultipleUse());
            options.put("showTitleAndDescription", buttonWidget.showTitleAndDescription());
            var cancelable = CancelableActionHandler.class.isAssignableFrom(buttonWidget.actionHandler());
            options.put("isCancelable", cancelable);
            final var dependencies = options.putArray("dependencies");
            addDependencies(dependencies, buttonWidget.actionHandler());
        }

        if (annotatedWidgets.contains(ValueSwitchWidget.class)) {
            options.put(TAG_FORMAT, Format.VALUE_SWITCH);
        }

        if (annotatedWidgets.contains(RadioButtonsWidget.class)) {
            final var radioButtons = m_field.getAnnotation(RadioButtonsWidget.class);
            options.put(TAG_FORMAT, Format.RADIO);
            if (radioButtons.horizontal()) {
                options.put("radioLayout", "horizontal");
            } else {
                options.put("radioLayout", "vertical");
            }
        }

        if (annotatedWidgets.contains(ChoicesWidget.class)) {
            final var choicesWidget = m_field.getAnnotation(ChoicesWidget.class);
            final var possibleValuesGenerator = new ChoicesArrayNodeGenerator(m_mapper, m_settingsCreationContext);
            options.set("possibleValues", possibleValuesGenerator.createChoicesNode(choicesWidget.choices()));
            if (!m_fieldClass.equals(ColumnSelection.class) && !m_fieldClass.equals(ColumnFilter.class)) {
                String format = getChoicesComponentFormat();
                options.put(TAG_FORMAT, format);
            }
            options.put("showNoneColumn", choicesWidget.showNoneColumn());
            options.put("showRowKeys", choicesWidget.showRowKeys());
            options.put("showSearch", choicesWidget.showSearch());
            options.put("showMode", choicesWidget.showMode());
        }

        if (isArrayOfObjects) {
            applyArrayLayoutOptions(options, m_fieldType.getContentType().getRawClass());
        }
    }

    private void addDependencies(final ArrayNode dependencies,
        final Class<? extends DialogDataServiceHandler<?, ?>> actionHandler) {
        final var dependencyClass =
            GenericTypeFinderUtil.getNthGenericType(actionHandler, DialogDataServiceHandler.class, 1);
        final var traverser = new DefaultNodeSettingsFieldTraverser(m_mapper, dependencyClass);
        final Consumer<Field> addNewDependency = getAddNewDependencyCallback(dependencies);
        traverser.traverse(addNewDependency);
    }

    private Consumer<Field> getAddNewDependencyCallback(final ArrayNode dependencies) {
        return field -> {
            final var searchScope = UiSchemaDefaultNodeSettingsTraverser.toScope(field.path());
            final var declaringClass = field.propertyWriter().getAnnotation(DeclaringClass.class);
            final var targetScope = findTargetScope(searchScope, declaringClass);
            dependencies.add(targetScope);
        };
    }

    private String findTargetScope(final String searchScope, final DeclaringClass declaringClassAnnotation) {
        final var candidates = m_fields.stream().filter(control -> {
            if (declaringClassAnnotation != null
                && !control.field().getMember().getDeclaringClass().equals(declaringClassAnnotation.value())) {
                return false;
            }
            return control.scope().endsWith(searchScope);
        }).map(JsonFormsControl::scope).toList();
        if (candidates.size() > 1) {
            throw new UiSchemaGenerationException(
                String.format("Multiple settings found for path %s. Consider using @DeclaringClass to "
                    + "disambiguate the reference", searchScope));
        }
        final var targetScope = candidates.stream().findFirst().orElseThrow(
            () -> new UiSchemaGenerationException(String.format("No setting found for path %s", searchScope)));
        return targetScope;
    }

    /**
     * Note that for ColumnFilter and ColumnSelection, the format is set as part of the default formats. For String
     * arrays, we use the "twinList" format and otherwise we use "dropDown".
     *
     * @return
     */
    private String getChoicesComponentFormat() {
        String format = Format.DROP_DOWN;
        if (m_fieldType.isArrayType() && m_fieldType.getContentType().getRawClass().equals(String.class)) {
            format = Format.TWIN_LIST;
        }
        return format;
    }

    private Collection<?> getAnnotatedWidgets() {
        final var partitionedWidgetAnnotations = partitionWidgetAnnotationsByApplicability(
            widgetAnnotation -> m_field.getAnnotation(widgetAnnotation) != null, m_fieldClass);

        if (!partitionedWidgetAnnotations.get(false).isEmpty()) {
            throw new UiSchemaGenerationException(
                String.format("The annotation %s is not applicable for setting field %s with type %s",
                    partitionedWidgetAnnotations.get(false).get(0), m_fieldName, m_fieldClass));
        }

        return partitionedWidgetAnnotations.get(true).stream().map(WidgetAnnotation::widgetAnnotation).toList();
    }

    private void applyArrayLayoutOptions(final ObjectNode options, final Class<?> componentType) {

        Map<String, Class<?>> arraySettings = new HashMap<>();
        arraySettings.put(null, componentType);
        var details =
            JsonFormsUiSchemaUtil.buildUISchema(arraySettings, m_mapper, m_settingsCreationContext).get(TAG_ELEMENTS);
        options.set(TAG_ARRAY_LAYOUT_DETAIL, details);

        Optional.ofNullable(m_field.getAnnotation(ArrayWidget.class))
            .ifPresent(arrayWidget -> addArrayLayoutOptions(arrayWidget, options));
    }

    private static void addArrayLayoutOptions(final ArrayWidget arrayWidget, final ObjectNode options) {
        var addButtonText = arrayWidget.addButtonText();
        if (!addButtonText.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT, addButtonText);
        }
        var elementTitle = arrayWidget.elementTitle();
        if (!elementTitle.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ELEMENT_TITLE, elementTitle);
        }
        if (arrayWidget.showSortButtons()) {
            options.put(TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS, true);
        }
    }

    /** @return true if the type is from an POJO and not a primitive, String, Boxed type, or enum */
    private static boolean isObject(final JavaType contentType) {
        var boxedTypes = List.of(String.class, Double.class, Short.class, Boolean.class, Float.class, Integer.class,
            Long.class, Character.class);

        return !contentType.isPrimitive() && !contentType.isEnumType()
            && boxedTypes.stream().allMatch(type -> !type.equals(contentType.getRawClass()));
    }
}
