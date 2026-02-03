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
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_CHECKBOX_SCOPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_DEFAULT_VALUE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_LAYOUT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_SUB_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_USE_SECTION_LAYOUT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_WITH_EDIT_AND_RESET;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DATE_TIME_FORMATS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DEPENDENCIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DYNAMIC_SETTINGS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ELEMENTS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EMPTY_STATE_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EXTERNAL_VALIDATION_HANDLER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_HIDE_CONTROL_HEADER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_INTERVAL_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_POSSIBLE_VALUES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_PROVIDED_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_UNKNOWN_VALUES_TEXT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_USE_FLOW_VAR_TEMPLATES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_HORIZONTAL_LAYOUT;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.getApplicableDefaults;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.partitionWidgetAnnotationsByApplicability;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonState;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.IncrementAndApplyOnClick;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.NoopButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.OverwriteDialogTitleInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.RichTextInputWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.DateTimeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.EnumUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.WidgetRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree.WidgetTreeRenderers;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DependencyHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.WidgetAnnotation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ExternalBuiltInValidationUtil;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.SuggestionsProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.choices.filter.StringFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.filter.TypedStringFilter;
import org.knime.node.parameters.widget.text.RichTextInputWidget;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class UiSchemaOptionsGenerator {

    private final TreeNode<WidgetGroup> m_node;

    private final JavaType m_fieldType;

    private final Class<?> m_fieldClass;

    private final NodeParametersInput m_nodeParametersInput;

    private final String m_scope;

    private final Collection<Tree<WidgetGroup>> m_widgetTrees;

    private final WidgetRendererSpec<?> m_rendererSpec;

    /**
     *
     * @param node the node for which options are to be added from {@link Widget} annotations
     * @param context the current context of the default node settings
     * @param fields all traversed fields
     * @param scope of the current field
     * @param asyncChoicesProvider to be used to store results of asynchronously computed choices of
     *            {@link ChoicesProvider}s.
     * @param widgetTrees the widgetTrees to resolve dependencies from. With UIEXT-1673 This can be removed again
     */
    UiSchemaOptionsGenerator(final TreeNode<WidgetGroup> node, final NodeParametersInput context, final String scope,
        final Collection<Tree<WidgetGroup>> widgetTrees) {
        m_node = node;
        m_fieldType = node.getType();
        m_fieldClass = node.getRawClass();
        m_nodeParametersInput = context;
        m_scope = scope;
        m_widgetTrees = widgetTrees;
        m_rendererSpec = WidgetTreeRenderers.getRendererSpec(node, context);

    }

    /*
     * This method applies the styles of the given field to the given control as described in {@link Widget}
     *
     * @param control
     */
    void addOptionsTo(final ObjectNode control) {

        if (m_rendererSpec != null) {
            control.setAll(RendererToJsonFormsUtil.toUiSchemaElement(m_rendererSpec));
        }

        final var defaultWidgets = getApplicableDefaults(m_fieldClass);
        final var annotatedWidgets = getAnnotatedWidgets();
        if (defaultWidgets.isEmpty() && annotatedWidgets.isEmpty()
            && !(m_node instanceof ArrayParentNode<WidgetGroup>)) {
            return;
        }
        final var options = control.get(TAG_OPTIONS) instanceof ObjectNode op ? op : control.putObject(TAG_OPTIONS);

        for (var defaultWidget : defaultWidgets) {
            switch (defaultWidget.type()) {
                case COLUMN_FILTER:
                    options.put(TAG_FORMAT, Format.TYPED_STRING_FILTER);
                    addTypedStringFilterOptions(options, "column");
                    break;
                case FLOW_VARIABLE_FILTER:
                    options.put(TAG_FORMAT, Format.TYPED_STRING_FILTER);
                    addTypedStringFilterOptions(options, "variable");
                    break;
                case NAME_FILTER:
                    options.put(TAG_FORMAT, Format.NAME_FILTER);
                    break;
                case SINGLE_SELECTION:
                    options.put(TAG_FORMAT, Format.SINGLE_SELECTION);
                    addSingleSelectionChoicesParams(options);
                    break;
                case COLUMN_SELECTION:
                    options.put(TAG_FORMAT, Format.COLUMN_SELECTION);
                    break;
                case DATA_TYPE:
                    options.put(TAG_FORMAT, Format.DROP_DOWN);
                    getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
                    break;
                case ZONE_ID:
                    options.put(TAG_FORMAT, Format.DROP_DOWN);
                    options.set(TAG_POSSIBLE_VALUES,
                        JsonFormsUiSchemaUtil.getMapper().valueToTree(DateTimeUtil.getPossibleZoneIdValues()));
                    break;
                case DATE_INTERVAL:
                    options.put(TAG_FORMAT, Format.INTERVAL);
                    options.put(TAG_INTERVAL_TYPE, IntervalWidget.IntervalType.DATE.name());
                    break;
                case TIME_INTERVAL:
                    options.put(TAG_FORMAT, Format.INTERVAL);
                    options.put(TAG_INTERVAL_TYPE, IntervalWidget.IntervalType.TIME.name());
                    break;
                case INTERVAL:
                    options.put(TAG_FORMAT, Format.INTERVAL);
                    options.put(TAG_INTERVAL_TYPE, IntervalWidget.IntervalType.DATE_OR_TIME.name());
                    break;
                case STRING_ARRAY:
                    options.put(TAG_FORMAT, Format.COMBO_BOX);
                    break;
            }
        }

        if (annotatedWidgets.contains(Widget.class)) {
            final var widget = m_node.getAnnotation(Widget.class).orElseThrow();
            if (widget.advanced()) {
                options.put(OPTIONS_IS_ADVANCED, true);
            }
            if (annotatedWidgets.contains(WidgetInternal.class)) {
                final var widgetInternal = m_node.getAnnotation(WidgetInternal.class).orElseThrow();
                if (widgetInternal.hideControlHeader()) {
                    options.put(TAG_HIDE_CONTROL_HEADER, true);
                }
            }
        }

        if (annotatedWidgets.contains(Advanced.class)) {
            options.put(OPTIONS_IS_ADVANCED, true);
        }

        if (annotatedWidgets.contains(OverwriteDialogTitleInternal.class)) {
            final var widget = m_node.getAnnotation(OverwriteDialogTitleInternal.class).orElseThrow();
            control.put(TAG_LABEL, widget.value());
        }

        if (annotatedWidgets.contains(IntervalWidget.class)) {
            getOrCreateProvidedOptions(control).add(TAG_INTERVAL_TYPE);
        }

        if (annotatedWidgets.contains(DateTimeFormatPickerWidget.class)) {
            options.put(TAG_FORMAT, getDateTimeFormatFormat());

            getOrCreateProvidedOptions(control).add(TAG_DATE_TIME_FORMATS);

            options.put(TAG_EXTERNAL_VALIDATION_HANDLER, ExternalBuiltInValidationUtil
                .getValidationHandlerClass(DateTimeFormatPickerWidget.class, m_fieldClass).getName());
        }

        if (annotatedWidgets.contains(RichTextInputWidget.class)) {
            options.put(TAG_FORMAT, Format.RICH_TEXT_INPUT);
            if (annotatedWidgets.contains(RichTextInputWidgetInternal.class)) {
                final var richTextInputWidgetInternal =
                    m_node.getAnnotation(RichTextInputWidgetInternal.class).orElseThrow();
                if (richTextInputWidgetInternal.useFlowVarTemplates()) {
                    options.put(TAG_USE_FLOW_VAR_TEMPLATES, true);
                } else {
                    throw new UiSchemaGenerationException(
                        "The RichTextInputWidgetInternal annotation must have useFlowVarTemplates set to true.");
                }
            }
        }

        if (annotatedWidgets.contains(ButtonWidget.class)) {
            final var buttonWidget = m_node.getAnnotation(ButtonWidget.class).orElseThrow();
            final var handler = buttonWidget.actionHandler();
            options.put(TAG_ACTION_HANDLER, handler.getName());
            options.put(TAG_FORMAT, Format.BUTTON);

            final var states = options.putArray("states");
            final var handlerInstance = InstantiationUtil.createInstance(handler);
            generateStates(states, handlerInstance);
            options.put("displayErrorMessage", buttonWidget.displayErrorMessage());
            options.put("showTitleAndDescription", buttonWidget.showTitleAndDescription());
            final var dependencies = options.putArray(TAG_DEPENDENCIES);
            addDependencies(dependencies, buttonWidget.actionHandler());
            final var updateHandlerClass = buttonWidget.updateHandler();
            if (updateHandlerClass != NoopButtonUpdateHandler.class) {
                final var updateOptions = options.putObject("updateOptions");
                updateOptions.put("updateHandler", updateHandlerClass.getName());
                final var updateDependencies = updateOptions.putArray(TAG_DEPENDENCIES);
                addDependencies(updateDependencies, updateHandlerClass);
            }

            if (annotatedWidgets.contains(IncrementAndApplyOnClick.class)) {
                options.put("incrementAndApplyOnClick", true);
            }
        }
        if (annotatedWidgets.contains(SimpleButtonWidget.class)) {
            options.put(TAG_FORMAT, Format.SIMPLE_BUTTON);
            final var simpleButtonWidget = m_node.getAnnotation(SimpleButtonWidget.class).orElseThrow();
            options.put("triggerId", simpleButtonWidget.ref().getName());
            if (simpleButtonWidget.icon() != Icon.NONE) {
                options.put("icon", switch (simpleButtonWidget.icon()) {
                    case RELOAD -> "reload";
                    default -> throw new IllegalArgumentException("Unexpected value: " + simpleButtonWidget.icon());
                });
            }
            if (!NoopStringProvider.class.equals(simpleButtonWidget.runFinishedProvider())) {
                getOrCreateProvidedOptions(control).add(UiSchema.TAG_RUN_FINISHED);
            }

        }

        final var isValueSwitch = annotatedWidgets.contains(ValueSwitchWidget.class);

        final var isRadioButtons = annotatedWidgets.contains(RadioButtonsWidget.class);

        if (isValueSwitch) {
            options.put(TAG_FORMAT, Format.VALUE_SWITCH);
        }

        if (isRadioButtons) {
            final var radioButtons = m_node.getAnnotation(RadioButtonsWidget.class).orElseThrow();
            options.put(TAG_FORMAT, Format.RADIO);
            options.put("radioLayout", radioButtons.horizontal() ? "horizontal" : "vertical");
        }

        final var hasChoicesProvider = annotatedWidgets.contains(ChoicesProvider.class);

        final var isFilter =
            m_fieldClass.equals(StringFilter.class) || TypedStringFilter.class.isAssignableFrom(m_fieldClass);

        final var isSingleSelection = m_fieldClass.equals(StringOrEnum.class);
        if (hasChoicesProvider) {
            final var choicesProvider = m_node.getAnnotation(ChoicesProvider.class).orElseThrow();
            getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
            assertCorrectChoicesProviderIfNecessary(choicesProvider);
            if (!isFilter && !isSingleSelection && !isValueSwitch && !isRadioButtons) {
                options.put(TAG_FORMAT, getChoicesComponentFormat());
            }
        }
        final var hasSuggestionsProvider = annotatedWidgets.contains(SuggestionsProvider.class);
        if (hasSuggestionsProvider) {
            if (hasChoicesProvider) {
                throw new UiSchemaGenerationException(
                    "Only one of @ChoicesProvider and @SuggestionsProvider can be applied to a field.");
            }
            options.put(TAG_FORMAT, Format.DROP_DOWN);
            options.put("allowNewValue", true);
            getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
        }
        if (annotatedWidgets.contains(ColumnFilterWidget.class)
            || annotatedWidgets.contains(FlowVariableFilterWidget.class)) {
            getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
        }
        if (annotatedWidgets.contains(TypedStringFilterWidgetInternal.class)) {
            final var typedStringFilterWidget =
                m_node.getAnnotation(TypedStringFilterWidgetInternal.class).orElseThrow();
            if (typedStringFilterWidget.hideTypeFilter()) {
                options.put("hideTypeFilter", true);
            }
        }

        if (annotatedWidgets.contains(SortListWidget.class)) {
            options.put(TAG_FORMAT, Format.SORT_LIST);
            final var sortListWidget = m_node.getAnnotation(SortListWidget.class).orElseThrow();
            if (!sortListWidget.unknownElementId().isEmpty()) {
                options.put("unknownElementId", sortListWidget.unknownElementId());
            }
            if (!sortListWidget.unknownElementLabel().isEmpty()) {
                options.put("unknownElementLabel", sortListWidget.unknownElementLabel());
            }
            if (!sortListWidget.resetSortButtonLabel().isEmpty()) {
                options.put("resetSortButtonLabel", sortListWidget.resetSortButtonLabel());
            }
        }

        if (annotatedWidgets.contains(TwinlistWidget.class)) {
            if (!isFilter) {
                options.put(TAG_FORMAT, Format.TWIN_LIST);
            }
            final var twinlistWidget = m_node.getAnnotation(TwinlistWidget.class).orElseThrow();
            if (!twinlistWidget.includedLabel().isEmpty()) {
                options.put("includedLabel", twinlistWidget.includedLabel());
            }
            if (!twinlistWidget.excludedLabel().isEmpty()) {
                options.put("excludedLabel", twinlistWidget.excludedLabel());
            }

        }

        if (m_node.isOptional()) {
            OptionalWidgetOptionsUtil.addOptionalWidgetOptions(m_node, options,
                () -> getOrCreateProvidedOptions(control));
        }

        if (annotatedWidgets.contains(DynamicSettingsWidget.class)) {
            options.put(TAG_FORMAT, Format.DYNAMIC_INPUT_TYPE);
            getOrCreateProvidedOptions(control).add(TAG_DYNAMIC_SETTINGS);
        } else if (m_node.getRawClass().equals(Map.class)) {
            throw new UiSchemaGenerationException(String.format("Map fields are only supported with the %s annotation.",
                DynamicSettingsWidget.class.getSimpleName()));
        }

        if (annotatedWidgets.contains(DynamicParameters.class)) {
            options.put(TAG_FORMAT, Format.DYNAMIC_INPUT_TYPE);
            getOrCreateProvidedOptions(control).add(TAG_DYNAMIC_SETTINGS);
        } else if (m_node.getRawClass().isInterface()
            && DynamicNodeParameters.class.isAssignableFrom(m_node.getRawClass())) {
            throw new UiSchemaGenerationException(
                String.format("Interface or abstract fields are only supported with the %s annotation.",
                    DynamicParameters.class.getSimpleName()));
        }

        if (m_node instanceof ArrayParentNode<WidgetGroup> arrayWidgetNode) {
            applyArrayLayoutOptions(control, options, arrayWidgetNode.getElementTree(),
                m_node.getAnnotation(ArrayWidget.class).map(ArrayWidget::elementLayout)
                    .orElse(ArrayWidget.ElementLayout.VERTICAL_CARD));
        }

        if (options.isEmpty()) {
            control.remove(TAG_OPTIONS);
        }
    }

    static ArrayNode getOrCreateProvidedOptions(final ObjectNode uiSchema) {
        if (uiSchema.has(TAG_PROVIDED_OPTIONS)) {
            return (ArrayNode)uiSchema.get(TAG_PROVIDED_OPTIONS);
        }
        return uiSchema.putArray(TAG_PROVIDED_OPTIONS);
    }

    private static void addTypedStringFilterOptions(final ObjectNode options, final String filteredObject) {
        options.put(TAG_UNKNOWN_VALUES_TEXT, String.format("Any unknown %s", filteredObject));
        options.put(TAG_EMPTY_STATE_LABEL, String.format("No %ss in this list.", filteredObject));
    }

    /**
     * Enforces that the {@link ChoicesProvider} annotation on a {@link ColumnFilter} or a {@link FlowVariableFilter}
     * matches the expected type.
     */
    private void assertCorrectChoicesProviderIfNecessary(final ChoicesProvider choicesProvider) {
        assertChoicesProviderIfNecessary(ColumnFilter.class, ColumnChoicesProvider.class, ColumnFilterWidget.class,
            choicesProvider);
        assertChoicesProviderIfNecessary(FlowVariableFilter.class, FlowVariableChoicesProvider.class,
            FlowVariableFilterWidget.class, choicesProvider);
        assertEnumChoicesProviderIfNecessary(choicesProvider);
    }

    /**
     * We allow setting a {@link ChoicesProvider @ChoicesProvider} annotation for discoverability, but we need to
     * enforce the same type as in the type-safe variant.
     */
    private void assertChoicesProviderIfNecessary(final Class<?> expectedClass, final Class<?> expectedProviderClass,
        final Class<?> widgetClass, final ChoicesProvider choicesProvider) {

        if (m_node.getRawClass().equals(expectedClass)
            && !expectedProviderClass.isAssignableFrom(choicesProvider.value())) {
            throw new UiSchemaGenerationException(String.format(
                "The field is a %s and the provided choicesProvider '%s' is not a %s. "
                    + "To prevent this from happening in a type-safe way, use the @%s annotation instead",
                expectedClass.getSimpleName(), choicesProvider.value().getSimpleName(),
                expectedProviderClass.getSimpleName(), widgetClass.getSimpleName()));
        }
    }

    @SuppressWarnings("rawtypes")
    private void assertEnumChoicesProviderIfNecessary(final ChoicesProvider choicesProvider) {
        if (!m_fieldClass.isEnum()) {
            return;
        }
        if (!EnumChoicesProvider.class.isAssignableFrom(choicesProvider.value())) {
            throw new UiSchemaGenerationException(
                "The field is an enum and the provided choicesProvider is not an EnumChoicesProvider.");
        }
        @SuppressWarnings({"unchecked"}) // Checked above
        final var choicesProviderClass = (Class<? extends EnumChoicesProvider>)choicesProvider.value();
        @SuppressWarnings("unchecked") // Ensured by the interface
        final var enumType = (Class<? extends Enum>)GenericTypeFinderUtil.getFirstGenericType(choicesProviderClass,
            EnumChoicesProvider.class);
        if (!enumType.equals(m_fieldClass)) {
            throw new UiSchemaGenerationException(
                String.format("The field is an enum of type %s but the choicesProvider %s is for type %s.",
                    m_fieldClass.getSimpleName(), choicesProviderClass.getSimpleName(), enumType.getSimpleName()));
        }

    }

    private void addSingleSelectionChoicesParams(final ObjectNode options) {
        final var specialChoices = options.putArray("specialChoices");
        @SuppressWarnings({"unchecked", "rawtypes"})
        // The first generic
        final var specialChoicesEnumType = (Class<? extends Enum>)m_fieldType.containedType(0).getRawClass();
        for (var enumConstant : specialChoicesEnumType.getEnumConstants()) {
            final var titleAndDescription = EnumUtil.createConstantEntry(enumConstant);
            specialChoices.addObject().put("id", enumConstant.name()).put("text", titleAndDescription.title());
        }
    }

    private static <M extends Enum<M>> void generateStates(final ArrayNode states,
        final ButtonActionHandler<?, ?, M> handler) {
        final var stateMachine = handler.getStateMachine();
        getEnumFields(stateMachine).forEach(pair -> addState(pair.getFirst(), pair.getSecond(), states, handler));
    }

    private static <M extends Enum<M>> List<Pair<Field, M>> getEnumFields(final Class<M> enumClass) {
        return Arrays.asList(enumClass.getFields()).stream().filter(Field::isEnumConstant)
            .map(f -> new Pair<>(f, getEnumConstant(f.getName(), enumClass.getEnumConstants()))).toList();
    }

    private static <M extends Enum<M>> M getEnumConstant(final String fieldName, final M[] enumConstants) {
        return Stream.of(enumConstants).filter(m -> m.name().equals(fieldName)).findFirst().orElseThrow();
    }

    private static <M extends Enum<M>> void addState(final Field field, final M enumConst, final ArrayNode states,
        final ButtonActionHandler<?, ?, M> handler) {
        final var state = states.addObject();
        state.put("id", field.getName());
        var buttonState = field.getAnnotation(ButtonState.class);
        buttonState = handler.overrideButtonState(enumConst, buttonState);
        state.put("disabled", buttonState.disabled());
        state.put("primary", buttonState.primary());
        final var nextState = buttonState.nextState();
        if (!nextState.isEmpty()) {
            state.put("nextState", nextState);
        }
        state.put("text", buttonState.text());
    }

    private void addDependencies(final ArrayNode dependencies,
        final Class<? extends DependencyHandler<?>> actionHandler) {
        new DependencyResolver(m_node, m_widgetTrees, m_scope).addDependencyScopes(actionHandler, dependencies::add);
    }

    private String getChoicesComponentFormat() {
        if (m_node instanceof LeafNode<WidgetGroup> leafNode && String.class.equals(leafNode.getContentType())) {
            return Format.COMBO_BOX;
        }
        return Format.DROP_DOWN;
    }

    /**
     * No, that's not a typo in the method name. We want the jsonforms format for picking a date-time format string,
     * which depends on the type of the field targeted by the annotation.
     *
     * @return
     */
    private String getDateTimeFormatFormat() {
        if (String.class.equals(m_node.getRawClass())) {
            return Format.TEMPORAL_FORMAT;
        } else if (TemporalFormat.class.equals(m_node.getRawClass())) {
            return Format.TEMPORAL_FORMAT_WITH_TYPE;
        } else {
            throw new UiSchemaGenerationException("The annotation %s is not applicable for type %s"
                .formatted(DateTimeFormatPickerWidget.class, m_node.getType()));
        }
    }

    private Collection<?> getAnnotatedWidgets() {
        final var partitionedWidgetAnnotations = partitionWidgetAnnotationsByApplicability(
            widgetAnnotation -> m_node.getPossibleAnnotations().contains(widgetAnnotation)
                && m_node.getAnnotation(widgetAnnotation).isPresent(),
            m_fieldClass);

        if (!partitionedWidgetAnnotations.get(false).isEmpty()) {
            throw new UiSchemaGenerationException(
                String.format("The annotation %s is not applicable for setting field %s with type %s",
                    partitionedWidgetAnnotations.get(false).get(0), String.join(".", m_node.getPath()), m_fieldClass));
        }

        return partitionedWidgetAnnotations.get(true).stream().map(WidgetAnnotation::widgetAnnotation).toList();
    }

    private void applyArrayLayoutOptions(final ObjectNode control, final ObjectNode options,
        final Tree<WidgetGroup> elementTree, final ArrayWidget.ElementLayout elementLayout) {
        var details = JsonFormsUiSchemaUtil.buildUISchema(List.of(elementTree), m_widgetTrees, m_nodeParametersInput)
            .get(TAG_ELEMENTS);
        if (elementLayout == ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE && details.isArray()
            && StreamSupport.stream(details.spliterator(), false).count() > 1) {
            options.putArray(TAG_ARRAY_LAYOUT_DETAIL)//
                .addObject()//
                .put(TAG_TYPE, TYPE_HORIZONTAL_LAYOUT)//
                .set(TAG_ELEMENTS, details);
        } else {
            options.set(TAG_ARRAY_LAYOUT_DETAIL, details);
        }

        m_node.getAnnotation(ArrayWidget.class)
            .ifPresent(arrayWidget -> addArrayLayoutOptions(arrayWidget, control, options));

        m_node.getAnnotation(ArrayWidgetInternal.class).ifPresent(
            internalArrayWidget -> addInternalArrayLayoutOptions(internalArrayWidget, control, options, elementTree));
        validateArrayLayoutOptions(options);
    }

    private void addArrayLayoutOptions(final ArrayWidget arrayWidget, final ObjectNode control,
        final ObjectNode options) {
        var addButtonText = arrayWidget.addButtonText();
        if (!addButtonText.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT, addButtonText);
        }
        if (!addButtonText.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT, addButtonText);
        }
        addArrayElementLayoutAndTitleOptions(arrayWidget, options);
        if (arrayWidget.showSortButtons()) {
            options.put(TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS, true);
        }
        if (arrayWidget.hasFixedSize()) {
            options.put(TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE, true);
        }
        var elementDefaultValueProvider = arrayWidget.elementDefaultValueProvider();
        if (!elementDefaultValueProvider.equals(StateProvider.class)) {
            getOrCreateProvidedOptions(control).add(TAG_ARRAY_LAYOUT_ELEMENT_DEFAULT_VALUE);
        }
    }

    private void addArrayElementLayoutAndTitleOptions(final ArrayWidget arrayWidget, final ObjectNode options) {
        final var elementLayout = arrayWidget.elementLayout();
        if (elementLayout != ArrayWidget.ElementLayout.VERTICAL_CARD) {
            options.put(TAG_ARRAY_LAYOUT_ELEMENT_LAYOUT, elementLayout.name());
        }
        var elementTitle = arrayWidget.elementTitle();
        if (!elementTitle.isEmpty() && elementLayout == ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE) {
            throw new UiSchemaGenerationException(
                "In an ArrayWidget, do not set the elementTitle in case elementLayout is HORIZONTAL_SINGLE_LINE.");
        }
        if (elementTitle.isEmpty() && elementLayout == ArrayWidget.ElementLayout.VERTICAL_CARD) {
            elementTitle = autoGuessElementTitle(m_node);
        }
        if (!elementTitle.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ELEMENT_TITLE, elementTitle);
        }
    }

    private static String autoGuessElementTitle(final TreeNode<WidgetGroup> node) {
        final var nodeName = node.getName();
        if (nodeName.isEmpty()) {
            throw new IllegalStateException("Cannot guess element title for array layout without a name.");

        }
        return fieldNameToElementTitle(nodeName.get());
    }

    private static void validateArrayLayoutOptions(final ObjectNode options) {
        final var useSectionLayout = options.has(TAG_ARRAY_LAYOUT_USE_SECTION_LAYOUT);
        final var showSortButtons = options.has(TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS);
        final var hasFixedSize = options.has(TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE);
        final var withEditAndReset = options.has(TAG_ARRAY_LAYOUT_WITH_EDIT_AND_RESET);
        final var withElementCheckboxes = options.has(TAG_ARRAY_LAYOUT_ELEMENT_CHECKBOX_SCOPE);
        if (useSectionLayout && (showSortButtons || !hasFixedSize || withEditAndReset || withElementCheckboxes)) {
            throw new UiSchemaGenerationException("The section layout for array widgets must be used with hasFixedSize"
                + " and cannot be used with showSortButtons, withEditAndReset or withElementCheckboxes options.");
        }
    }

    private static String fieldNameToElementTitle(final String nodeName) {
        return makeSingular(upperCase(resolveCamelCase(nodeName)));
    }

    private static String makeSingular(final String upperCaseFirstCharacter) {
        return upperCaseFirstCharacter.endsWith("s")
            ? upperCaseFirstCharacter.substring(0, upperCaseFirstCharacter.length() - 1) : upperCaseFirstCharacter;
    }

    private static String upperCase(final String withResolvedCamelCase) {
        return withResolvedCamelCase.substring(0, 1).toUpperCase(Locale.getDefault())
            + withResolvedCamelCase.substring(1);
    }

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([\\p{Ll}])([\\p{Lu}])");

    private static String resolveCamelCase(final String nodeName) {
        return CAMEL_CASE_PATTERN.matcher(nodeName).replaceAll("$1 $2");
    }

    private static void addInternalArrayLayoutOptions(final ArrayWidgetInternal arrayWidgetInternal,
        final ObjectNode control, final ObjectNode options, final Tree<WidgetGroup> elementWidgetTree) {

        if (arrayWidgetInternal.withEditAndReset()) {
            options.put(TAG_ARRAY_LAYOUT_WITH_EDIT_AND_RESET, true);
        }

        if (arrayWidgetInternal.withElementCheckboxes()) {
            final var elementCheckboxScope = findElementCheckboxScope(elementWidgetTree);
            options.put(TAG_ARRAY_LAYOUT_ELEMENT_CHECKBOX_SCOPE, elementCheckboxScope);
        }

        if (!NoopStringProvider.class.equals(arrayWidgetInternal.titleProvider())) {
            getOrCreateProvidedOptions(control).add(TAG_ARRAY_LAYOUT_ELEMENT_TITLE);
        }

        if (!NoopStringProvider.class.equals(arrayWidgetInternal.subTitleProvider())) {
            getOrCreateProvidedOptions(control).add(TAG_ARRAY_LAYOUT_ELEMENT_SUB_TITLE);
        }

        if (arrayWidgetInternal.isSectionLayout()) {
            options.put(TAG_ARRAY_LAYOUT_USE_SECTION_LAYOUT, true);
        }
    }

    private static String findElementCheckboxScope(final Tree<WidgetGroup> elementWidgetTree) {
        final var elementCheckboxField = elementWidgetTree.getWidgetNodes()
            .filter(UiSchemaOptionsGenerator::hasElementCheckboxWidgetAnnotation).findFirst().orElseThrow(
                () -> new UiSchemaGenerationException("No field with a @ElementCheckboxWidget annotation found "
                    + "within an array layout with @InternalArrayLayout#withElementCheckboxes."));
        return JsonFormsScopeUtil.toScope(elementCheckboxField);
    }

    static boolean hasElementCheckboxWidgetAnnotation(final TreeNode<WidgetGroup> field) {
        return field.getPossibleAnnotations().contains(ArrayWidgetInternal.ElementCheckboxWidget.class)
            && field.getAnnotation(ArrayWidgetInternal.ElementCheckboxWidget.class).isPresent();
    }

}
