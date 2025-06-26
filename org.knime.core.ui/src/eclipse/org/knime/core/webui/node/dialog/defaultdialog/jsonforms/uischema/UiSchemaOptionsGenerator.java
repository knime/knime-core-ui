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
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_SUB_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_TITLE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_SHOW_SORT_BUTTONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_WITH_EDIT_AND_RESET;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DATE_TIME_FORMATS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DEPENDENCIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DYNAMIC_SETTINGS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ELEMENTS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EMPTY_STATE_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EXTERNAL_VALIDATION_HANDLER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FILE_EXTENSION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FILE_EXTENSIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FILE_FILTER_CLASS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_FORMAT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_HIDE_CONTROL_HEADER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_INTERVAL_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_IS_WRITER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_MESSAGE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_PLACEHOLDER;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_POSSIBLE_VALUES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_PROVIDED_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_UNKNOWN_VALUES_TEXT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_USE_FLOW_VAR_TEMPLATES;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.getApplicableDefaults;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.partitionWidgetAnnotationsByApplicability;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.OverwriteDialogTitleInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.RichTextInputWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.DateTimeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.EnumUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.Format;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree.WidgetTreeRenderers;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.setting.dbtableselection.DBTableSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable.FlowVariableFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.MultiFileSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Advanced;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FolderSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RichTextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TwinlistWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonActionHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonState;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.NoopButtonUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.EnumChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnFilterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.variable.FlowVariableChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.variable.FlowVariableFilterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DependencyHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.util.WidgetImplementationUtil.WidgetAnnotation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ExternalBuiltInValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.WorkflowContextUtil;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class UiSchemaOptionsGenerator {

    private final TreeNode<WidgetGroup> m_node;

    private final JavaType m_fieldType;

    private final Class<?> m_fieldClass;

    private final DefaultNodeSettingsContext m_defaultNodeSettingsContext;

    private final String m_scope;

    private final Collection<Tree<WidgetGroup>> m_widgetTrees;

    private final ControlRendererSpec m_rendererSpec;

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
    UiSchemaOptionsGenerator(final TreeNode<WidgetGroup> node, final DefaultNodeSettingsContext context,
        final String scope, final Collection<Tree<WidgetGroup>> widgetTrees) {
        m_node = node;
        m_fieldType = node.getType();
        m_fieldClass = node.getRawClass();
        m_defaultNodeSettingsContext = context;
        m_scope = scope;
        m_widgetTrees = widgetTrees;
        m_rendererSpec = WidgetTreeRenderers.getRendererSpec(node);

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
                case FILE_CHOOSER:
                    options.put(TAG_FORMAT, Format.FILE_CHOOSER);
                    addWorkflowContextInfo(options);
                    break;
                case MULTI_FILE_CHOOSER:
                    options.put(TAG_FORMAT, Format.MULTI_FILE_CHOOSER);
                    addWorkflowContextInfo(options);
                    var filtersClass = MultiFileSelectionUtil.extractFileChooserFiltersClass(m_node)
                        .orElseThrow(IllegalStateException::new);
                    setFilterSchemaForMultiFileWidgetChooser(options, filtersClass);
                    options.put(TAG_FILE_FILTER_CLASS, filtersClass.getName());
                    break;
                case DB_TABLE_CHOOSER:
                    options.put(TAG_FORMAT, Format.DB_TABLE_CHOOSER);
                    setDbTableChooserOptions(options);
                    break;
                case DYNAMIC_VALUE:
                    options.put(TAG_FORMAT, Format.DYNAMIC_VALUE);
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
                } else {
                    throw new UiSchemaGenerationException(
                        "The WidgetInternal annotation must have hideControlHeader set to true.");
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

        if (annotatedWidgets.contains(TextMessage.class)) {
            options.put(TAG_FORMAT, Format.TEXT_MESSAGE);
            getOrCreateProvidedOptions(control).add(TAG_MESSAGE);
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

        if (annotatedWidgets.contains(FileReaderWidget.class))

        {
            final var fileReaderWidget = m_node.getAnnotation(FileReaderWidget.class).orElseThrow();
            resolveFileExtensions(options, fileReaderWidget.fileExtensions());
            addFileSystemInformation(options);
        }

        if (annotatedWidgets.contains(FileWriterWidget.class)) {
            options.put(TAG_IS_WRITER, true);
            final var fileWriterWidget = m_node.getAnnotation(FileWriterWidget.class).orElseThrow();
            resolveFileExtension(control, options, fileWriterWidget.fileExtension(),
                fileWriterWidget.fileExtensionProvider());
        }

        if (annotatedWidgets.contains(FolderSelectionWidget.class)) {
            options.put("selectionMode", "FOLDER");
            addFileSystemInformation(options);
        }

        if (annotatedWidgets.contains(LocalFileWriterWidget.class)) {
            options.put(TAG_FORMAT, Format.LOCAL_FILE_CHOOSER);
            final var localFileWriterWidget = m_node.getAnnotation(LocalFileWriterWidget.class).orElseThrow();
            options.put(TAG_PLACEHOLDER, localFileWriterWidget.placeholder());
            options.put(TAG_IS_WRITER, true);
            resolveFileExtension(control, options, localFileWriterWidget.fileExtension(),
                localFileWriterWidget.fileExtensionProvider());
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

        if (isValueSwitch || isRadioButtons) {
            /**
             * That fieldClass is an enum is ensured by the {@link WidgetImplementationUtil}
             */
            final var fieldClass = m_fieldClass;
            if (fieldClass.isEnum()) {
                final var disabledOptions = getDisabledEnumConstants();
                if (!disabledOptions.isEmpty()) {
                    final var disabledOptionsNode = options.putArray("disabledOptions");
                    disabledOptions.forEach(disabledOptionsNode::add);
                }
            }
        }

        final var hasChoices = annotatedWidgets.contains(ChoicesProvider.class);

        final var isFilter =
            m_fieldClass.equals(StringFilter.class) || TypedStringFilter.class.isAssignableFrom(m_fieldClass);

        final var isSingleSelection = m_fieldClass.equals(StringOrEnum.class);
        if (hasChoices) {
            final var choicesProvider = m_node.getAnnotation(ChoicesProvider.class).orElseThrow();
            getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
            assertCorrectChoicesProviderIfNecessary(choicesProvider);
            if (!isFilter && !isSingleSelection) {
                options.put(TAG_FORMAT, getChoicesComponentFormat());
            }
        }
        if (annotatedWidgets.contains(ColumnFilterWidget.class)
            || annotatedWidgets.contains(FlowVariableFilterWidget.class)) {
            getOrCreateProvidedOptions(control).add(TAG_POSSIBLE_VALUES);
        }

        if (annotatedWidgets.contains(SortListWidget.class)) {
            options.put(TAG_FORMAT, Format.SORT_LIST);
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

        if (m_node instanceof

        ArrayParentNode<WidgetGroup> arrayWidgetNode) {
            applyArrayLayoutOptions(control, options, arrayWidgetNode.getElementTree());
        }

        if (options.isEmpty()) {
            control.remove(TAG_OPTIONS);
        }
    }

    private static ArrayNode getOrCreateProvidedOptions(final ObjectNode uiSchema) {
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

    private void addFileSystemInformation(final ObjectNode options) {
        getFirstFileSystemPortIndex(m_defaultNodeSettingsContext)
            .ifPresent(portIndex -> addFileSystemInformation(options, portIndex));
    }

    private static OptionalInt getFirstFileSystemPortIndex(final DefaultNodeSettingsContext context) {
        final var inPortTypes = context.getInPortTypes();
        return IntStream.range(0, inPortTypes.length)
            .filter(i -> FileSystemPortObjectSpec.class.equals(inPortTypes[i].getPortObjectSpecClass())).findAny();

    }

    private void addFileSystemInformation(final ObjectNode options, final int portIndex) {
        options.put("portIndex", portIndex);
        final var portObjectSpec = m_defaultNodeSettingsContext.getPortObjectSpec(portIndex)
            .map(spec -> toFileSystemPortObjectSpec(spec, portIndex));
        portObjectSpec.ifPresent(spec -> {
            options.put("fileSystemType", spec.getFileSystemType());
            spec.getFSLocationSpec().getFileSystemSpecifier()
                .ifPresent(fileSystemSpecifier -> options.put("fileSystemSpecifier", fileSystemSpecifier));
        });
        if (portObjectSpec.flatMap(FileSystemPortObjectSpec::getFileSystemConnection).isEmpty()) {
            options.put("fileSystemConnectionMissing", true);
        }
    }

    private static FileSystemPortObjectSpec toFileSystemPortObjectSpec(final PortObjectSpec spec, final int portIndex) {
        if (spec instanceof FileSystemPortObjectSpec fsSpec) {
            return fsSpec;
        }
        throw new IllegalStateException(String.format("Port at index %s is not a file system port", portIndex));
    }

    private static void addWorkflowContextInfo(final ObjectNode options) {
        WorkflowContextUtil.getWorkflowContextV2Optional().ifPresent(context -> {
            addExecutorTypeInformation(options, context.getExecutorType());
            addLocationInfo(options, context.getLocationInfo());
        });
    }

    private static void addExecutorTypeInformation(final ObjectNode options, final ExecutorType executorType) {
        if (executorType == ExecutorType.ANALYTICS_PLATFORM) {
            addLocalExecutorInfo(options);
        }
    }

    private static void addLocalExecutorInfo(final ObjectNode options) {
        options.put("isLocal", true);
    }

    private static void addLocationInfo(final ObjectNode options, final LocationInfo locationInfo) {
        if (locationInfo instanceof LocalLocationInfo) {
            addLocalLocationInfo(options);
        } else if (locationInfo instanceof HubSpaceLocationInfo hubSpace) {
            addhubSpaceLocationInfo(options, hubSpace);
        } else if (locationInfo instanceof ServerLocationInfo server) {
            addServerLocationInfo(options, server);
        }
    }

    private static void addLocalLocationInfo(final ObjectNode options) {
        options.put("mountId", "Local space");
    }

    private static void addhubSpaceLocationInfo(final ObjectNode options, final HubSpaceLocationInfo hubSpace) {
        options.put("mountId", hubSpace.getDefaultMountId());
        options.put("spacePath", hubSpace.getSpacePath());
    }

    private static void addServerLocationInfo(final ObjectNode options, final ServerLocationInfo server) {
        options.put("mountId", server.getDefaultMountId());
    }

    private <E extends Enum<E>> List<String> getDisabledEnumConstants() {
        @SuppressWarnings("unchecked")
        var enumClass = (Class<E>)m_fieldClass;
        return Stream.of(enumClass.getEnumConstants())//
            .map(constant -> constNameIfDisabled(enumClass, constant)).filter(Optional::isPresent).map(Optional::get)
            .toList();
    }

    private static <E extends Enum<E>> Optional<String> constNameIfDisabled(final Class<E> enumClass,
        final E constant) {
        Field field;
        final var constantName = constant.name();
        try {
            field = enumClass.getField(constantName);
            final var label = field.getAnnotation(Label.class);
            if (label != null && label.disabled()) {
                return Optional.of(constantName);
            }
        } catch (NoSuchFieldException | SecurityException ex) {
            NodeLogger.getLogger(UiSchemaOptionsGenerator.class)
                .error(String.format("Exception when accessing field %s.", constantName), ex);
        }
        return Optional.empty();
    }

    private static void resolveFileExtensions(final ObjectNode options, final String[] extensions) {
        if (extensions.length > 0) {
            final var fileExtensionsNode = options.putArray(TAG_FILE_EXTENSIONS);
            Arrays.stream(extensions).forEach(fileExtensionsNode::add);
        }
    }

    private static void resolveFileExtension(final ObjectNode uiSchema, final ObjectNode options,
        final String fileExtension, final Class<? extends StateProvider<String>> fileExtensionProvider) {
        if (!fileExtension.isEmpty()) {
            options.put(TAG_FILE_EXTENSION, fileExtension);
        }
        if (!fileExtensionProvider.equals(NoopStringProvider.class)) {
            CheckUtils.check(fileExtension.isEmpty(), UiSchemaGenerationException::new,
                () -> "The parameter \"fileExtension\" and \"fileExtensionProvider\" "
                    + "cannot be used in combination.");
            getOrCreateProvidedOptions(uiSchema).add(TAG_FILE_EXTENSION);
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
        final Tree<WidgetGroup> elementTree) {
        var details = JsonFormsUiSchemaUtil
            .buildUISchema(List.of(elementTree), m_widgetTrees, m_defaultNodeSettingsContext).get(TAG_ELEMENTS);
        options.set(TAG_ARRAY_LAYOUT_DETAIL, details);

        m_node.getAnnotation(ArrayWidget.class)
            .ifPresent(arrayWidget -> addArrayLayoutOptions(arrayWidget, control, options));

        m_node.getAnnotation(ArrayWidgetInternal.class).ifPresent(
            internalArrayWidget -> addInternalArrayLayoutOptions(internalArrayWidget, control, options, elementTree));
    }

    private static void addArrayLayoutOptions(final ArrayWidget arrayWidget, final ObjectNode control,
        final ObjectNode options) {
        var addButtonText = arrayWidget.addButtonText();
        if (!addButtonText.isEmpty()) {
            options.put(TAG_ARRAY_LAYOUT_ADD_BUTTON_TEXT, addButtonText);
        }
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
        if (arrayWidget.hasFixedSize()) {
            options.put(TAG_ARRAY_LAYOUT_HAS_FIXED_SIZE, true);
        }
        var elementDefaultValueProvider = arrayWidget.elementDefaultValueProvider();
        if (!elementDefaultValueProvider.equals(StateProvider.class)) {
            getOrCreateProvidedOptions(control).add(TAG_ARRAY_LAYOUT_ELEMENT_DEFAULT_VALUE);
        }
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

    private void setFilterSchemaForMultiFileWidgetChooser(final ObjectNode options,
        final Class<? extends FileChooserFilters> cls) {
        var elementTree = new WidgetTreeFactory().createTree(cls, null);

        var filterSubUiSchema =
            JsonFormsUiSchemaUtil.buildUISchema(List.of(elementTree), m_widgetTrees, m_defaultNodeSettingsContext);

        options.set("filterSubUiSchema", filterSubUiSchema);
    }

    private void setDbTableChooserOptions(final ObjectNode options) {
        var rootNode = m_node.getRoot();

        var provider = Optional.ofNullable(rootNode.getRawClass().getAnnotation(DBTableAdapterProvider.class)) //
            .map(DBTableAdapterProvider::value);

        if (provider.isPresent()) {
            var adapterClass = provider.get();
            Supplier<PortObjectSpec[]> supplier = m_defaultNodeSettingsContext::getPortObjectSpecs;
            var adapter = DBTableAdapter.instantiate(adapterClass, supplier);
            try {
                var isDbConnected = adapter.isDbConnected();
                options.put("dbConnected", isDbConnected);
                if (isDbConnected) {
                    options.put("catalogsSupported", adapter.listCatalogs().isPresent());
                }
            } catch (SQLException ex) {
                throw new UiSchemaGenerationException("""
                        Could not check whether the underlying DBTableAdapter supports catalogues, \
                        because an SQL error occurred while trying to query the database.
                        """, ex);
            }
        } else {
            var dbTableAdapterClassName = DBTableAdapterProvider.class.getSimpleName();
            var widgetClassName = DBTableSelection.class.getSimpleName();
            throw new UiSchemaGenerationException("""
                    The %s widget requires a @%s annotation on the root node of the \
                    widget tree to determine whether catalogues are supported.
                    """.formatted(widgetClassName, dbTableAdapterClassName));
        }
    }
}
