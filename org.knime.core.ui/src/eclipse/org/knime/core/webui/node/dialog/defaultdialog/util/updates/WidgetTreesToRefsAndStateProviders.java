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
 *   Feb 6, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.data.DataType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DataAndDialog;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl.DynamicNodeParametersDeserializer;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl.DynamicNodeParametersSerializer;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.CredentialsRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.OptionalWidgetOptionsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.DefaultDataTypeChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.WidgetGroupTraverser.Configuration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopBooleanProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMaxLengthValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMaxValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMinLengthValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMinValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopPatternValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

final class WidgetTreesToRefsAndStateProviders {

    record ValueRefWrapper(Class<? extends ParameterReference> valueRef, Location fieldLocation,
        JsonDeserializer<?> specialDeserializer) {
    }

    record ValueProviderWrapper(Class<? extends StateProvider> stateProviderClass, Location fieldLocation,
        JsonSerializer<Object> specialSerializer) {

    }

    record LocationUiStateProviderWrapper(Class<? extends StateProvider> stateProviderClass, Location fieldLocation,
        String providedOptionName) {
    }

    record IdUiStateProviderWrapper(Class<? extends StateProvider> stateProviderClass, String id,
        String providedOptionName) {
    }

    record RefsAndStateProviders(Collection<ValueRefWrapper> valueRefs, Collection<ValueProviderWrapper> valueProviders,
        Collection<LocationUiStateProviderWrapper> locationUiStateProviders,
        Collection<IdUiStateProviderWrapper> idUiStateProviders) {

        static RefsAndStateProviders empty() {
            return new RefsAndStateProviders(List.of(), List.of(), List.of(), List.of());
        }
    }

    static final Configuration TRAVERSAL_CONFIG = new Configuration.Builder()//
        .includeWidgetGroupFields()//
        .build();

    private final Collection<ValueRefWrapper> m_valueRefs = new ArrayList<>();

    private final Collection<ValueProviderWrapper> m_valueProviders = new ArrayList<>();

    private final Collection<LocationUiStateProviderWrapper> m_locationUiStateProviders = new ArrayList<>();

    private final Collection<IdUiStateProviderWrapper> m_idUiStateProviders = new ArrayList<>();

    /**
     * @param widgetTrees a collection of widget trees derived from settings classes to collect annotated fields from
     * @return the parsed refs and updates from annotations
     */
    RefsAndStateProviders widgetTreesToRefsAndStateProviders(final Collection<Tree<WidgetGroup>> widgetTrees) {
        widgetTrees.forEach(this::traverseWidgetTree);
        return new RefsAndStateProviders(m_valueRefs, m_valueProviders, m_locationUiStateProviders,
            m_idUiStateProviders);

    }

    private void traverseWidgetTree(final Tree<WidgetGroup> tree) {
        tree.getChildren().forEach(this::traverseWidgetTreeNode);
    }

    private void traverseWidgetTreeNode(final TreeNode<WidgetGroup> node) {
        addWidgetValueAnnotationRefAndValueProviderForNode(node);
        addUiStateProviderForNode(node);
        if (node instanceof Tree<WidgetGroup> widgetTree) {
            traverseWidgetTree(widgetTree);
        } else if (node instanceof ArrayParentNode<WidgetGroup> arrayWidgetNode) {
            traverseWidgetTree(arrayWidgetNode.getElementTree());
        }
    }

    private record UiState(String name, Class<? extends StateProvider> stateProviderClass) {
    }

    interface UiStateProviderSpec {
        Optional<List<UiState>> getUiStateProviders(TreeNode<WidgetGroup> node);

    }

    /**
     * A class that defines a ui state which is defined for a specific field type.
     */
    private record UiStateProviderFieldTypeSpec( //
        /**
         * The identifier of this ui state within the updated control.
         */
        String providedOptionName,
        /**
         * The field type for which a ui state is defined
         */
        Class<?> fieldType,
        /**
         * The parameter of the provider that needs to be instantiated to retrieve the state
         */
        Class<? extends StateProvider> uiStateProvider,
        /**
         * The annotation that overrides the provided ui state for this field type. If null, no such override exists.
         */
        Class<? extends Annotation> ignoreIfThisAnnotationIsPresent//
    ) implements UiStateProviderSpec {

        @Override
        public Optional<List<UiState>> getUiStateProviders(final TreeNode<WidgetGroup> node) {
            if (node.getPossibleAnnotations().contains(Widget.class) && node.getAnnotation(Widget.class).isEmpty()) {
                return Optional.empty();
            }
            if (!fieldType.isAssignableFrom(node.getRawClass())) {
                return Optional.empty();
            }
            if (ignoreIfThisAnnotationIsPresent != null
                && node.getAnnotation(ignoreIfThisAnnotationIsPresent).isPresent()) {
                return Optional.empty();
            }
            return Optional.of(List.of(new UiState(providedOptionName, uiStateProvider)));
        }

    }

    /**
     * A class that defines a ui state which is given by an annotation.
     */
    private record UiStateProviderAnnotationSpec<T extends Annotation, S extends StateProvider>( //
        /**
         * The identifier of this ui state within the updated control.
         */
        String providedOptionName,
        /**
         * The annotation class that defines the ui state
         */
        Class<T> annotationClass,
        /**
         * The parameter of the provider that needs to be instantiated to retrieve the state
         */
        Function<T, Class<? extends S>> getProviderParameter,
        /**
         * The value that should be ignored if it is set, since it is the default. If null, no default is ignored.
         */
        Class<? extends S> ignoredDefaultParameter//
    ) implements UiStateProviderSpec {

        @Override
        public Optional<List<UiState>> getUiStateProviders(final TreeNode<WidgetGroup> node) {
            if (!node.getPossibleAnnotations().contains(annotationClass)) {
                return Optional.empty();
            }
            final var annotation = node.getAnnotation(annotationClass);
            if (annotation.isEmpty()) {
                return Optional.empty();
            }
            final var stateProvider = getProviderParameter.apply(annotation.get());
            if (ignoredDefaultParameter != null && stateProvider.equals(ignoredDefaultParameter)) {
                return Optional.empty();
            }
            return Optional.of(List.of(new UiState(providedOptionName, stateProvider)));

        }

    }

    private static List<UiStateProviderFieldTypeSpec> uiStateProviderFieldTypeSpecs = List.of( //
        new UiStateProviderFieldTypeSpec( //
            "possibleValues", //
            DataType.class, //
            DefaultDataTypeChoicesProvider.class, //
            ChoicesProvider.class//
        )//
    );

    private static List<UiStateProviderAnnotationSpec<? extends Annotation, ? extends StateProvider>> uiStateProviderAnnotationSpecs =
        List.of( //
            new UiStateProviderAnnotationSpec<>( //
                CredentialsRendererSpec.HAS_PASSWORD, //
                CredentialsWidgetInternal.class, //
                CredentialsWidgetInternal::hasPasswordProvider, //
                NoopBooleanProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                CredentialsRendererSpec.HAS_USERNAME, //
                CredentialsWidgetInternal.class, //
                CredentialsWidgetInternal::hasUsernameProvider, //
                NoopBooleanProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_INTERVAL_TYPE, //
                IntervalWidget.class, //
                IntervalWidget::typeProvider, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_POSSIBLE_VALUES, //
                ChoicesProvider.class, //
                ChoicesProvider::value, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>(//
                UiSchema.TAG_POSSIBLE_VALUES, //
                ColumnFilterWidget.class, //
                ColumnFilterWidget::choicesProvider, //
                null//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                UiSchema.TAG_POSSIBLE_VALUES, //
                FlowVariableFilterWidget.class, //
                FlowVariableFilterWidget::choicesProvider, //
                null//
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_DATE_TIME_FORMATS, //
                DateTimeFormatPickerWidget.class, //
                DateTimeFormatPickerWidget::formatProvider, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_PLACEHOLDER, //
                TextInputWidget.class, //
                TextInputWidget::placeholderProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>(//
                UiSchema.TAG_MESSAGE, //
                TextMessage.class, //
                TextMessage::value, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_TITLE, //
                ArrayWidgetInternal.class, //
                ArrayWidgetInternal::titleProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_SUB_TITLE, //
                ArrayWidgetInternal.class, //
                ArrayWidgetInternal::subTitleProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_ARRAY_LAYOUT_ELEMENT_DEFAULT_VALUE, //
                ArrayWidget.class, //
                ArrayWidget::elementDefaultValueProvider, //
                StateProvider.class //
            ), //
            new UiStateProviderAnnotationSpec<>(//
                OptionalWidgetOptionsUtil.TAG_DEFAULT, //
                OptionalWidget.class, //
                OptionalWidget::defaultProvider, //
                null//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                NumberRendererSpec.TAG_MIN_VALIDATION, //
                NumberInputWidget.class, //
                NumberInputWidget::minValidationProvider, //
                NoopMinValidationProvider.class//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                NumberRendererSpec.TAG_MAX_VALIDATION, //
                NumberInputWidget.class, //
                NumberInputWidget::maxValidationProvider, //
                NoopMaxValidationProvider.class//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                TextRendererSpec.TAG_MIN_LENGTH_VALIDATION, //
                TextInputWidget.class, //
                TextInputWidget::minLengthValidationProvider, //
                NoopMinLengthValidationProvider.class//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                TextRendererSpec.TAG_MAX_LENGTH_VALIDATION, //
                TextInputWidget.class, //
                TextInputWidget::maxLengthValidationProvider, //
                NoopMaxLengthValidationProvider.class//
            ), //
            new UiStateProviderAnnotationSpec<>(//
                TextRendererSpec.TAG_PATTERN_VALIDATION, //
                TextInputWidget.class, //
                TextInputWidget::patternValidationProvider, //
                NoopPatternValidationProvider.class//
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_DYNAMIC_SETTINGS, //
                DynamicSettingsWidget.class, //
                DynamicSettingsWidget::value, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_DYNAMIC_SETTINGS, //
                DynamicParameters.class, //
                DynamicParameters::value, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UpdateResultsUtil.FILE_SYSTEM_ID, //
                WithCustomFileSystem.class, //
                WithCustomFileSystem::connectionProvider, //
                null //
            ), //
            new UiStateProviderAnnotationSpec<>( //
                UpdateResultsUtil.VALIDATOR_ID, //
                CustomValidation.class, //
                CustomValidation::value, //
                null //
            ), new UiStateProviderAnnotationSpec<>( //
                UiSchema.TAG_SHOW_SUB_PARAMETERS, //
                SubParameters.class, //
                SubParameters::showSubParametersProvider, //
                NoopBooleanProvider.class //
            ));

    static final List<UiStateProviderSpec> uiStateProviderSpecs = Stream.of( //
        uiStateProviderFieldTypeSpecs, //
        uiStateProviderAnnotationSpecs //
    ).<UiStateProviderSpec> flatMap(specs -> specs.stream()).toList();

    private void addUiStateProviderForNode(final TreeNode<WidgetGroup> node) {
        uiStateProviderSpecs.stream()
            .forEach(spec -> spec.getUiStateProviders(node).ifPresent(states -> addUiStateProviders(node, states)));
    }

    private void addUiStateProviders(final TreeNode<WidgetGroup> node, final List<UiState> states) {
        final var location = Location.fromTreeNode(node);
        if (WidgetTreeUtil.hasScope(node)) {
            m_locationUiStateProviders.addAll(states.stream()
                .map(state -> new LocationUiStateProviderWrapper(state.stateProviderClass(), location, state.name()))
                .toList());
        } else {
            m_idUiStateProviders
                .addAll(states.stream().map(state -> new IdUiStateProviderWrapper(state.stateProviderClass(),
                    JsonFormsScopeUtil.getScopeFromLocation(location), state.name())).toList());
        }

    }

    private void addWidgetValueAnnotationRefAndValueProviderForNode(final TreeNode<WidgetGroup> node) {
        final var pathsWithSettingsKey = Location.fromTreeNode(node);
        final var type = node.isOptional() ? Optional.class : node.getRawClass();
        node.getAnnotation(ValueReference.class)
            .ifPresent(valueReference -> addValueRef(valueReference.value(), type, pathsWithSettingsKey, node));
        node.getAnnotation(ValueProvider.class)
            .ifPresent(valueProvider -> addValueProvider(valueProvider.value(), type, pathsWithSettingsKey, node));
        if (node instanceof Tree<WidgetGroup> tree && tree.isDynamic()) {
            /**
             * Dynamic nodes always have a dynamic parameter provider that serves simultaneously as ui state provider
             * and as value provider.
             */
            final var dynamicParametersAnnotation = node.getAnnotation(DynamicParameters.class).orElseThrow();
            final var dynamicParametersProviderClass = dynamicParametersAnnotation.value();
            addDynamicParametersValueProvider(dynamicParametersProviderClass, pathsWithSettingsKey);
        }
    }

    private void addValueRef(final Class<? extends ParameterReference> valueRef, final Class<?> type,
        final Location pathWithSettingsKey, final TreeNode<WidgetGroup> node) {
        if (!valueRef.equals(ParameterReference.class)) {
            validateAgainstType(type, valueRef, ParameterReference.class,
                (field, annotationValue) -> annotationValue.isAssignableFrom(field));
            DynamicNodeParametersDeserializer specialDeserializer = null;
            if (node instanceof Tree<WidgetGroup> tree && tree.isDynamic()) {
                specialDeserializer = new DynamicNodeParametersDeserializer();
                specialDeserializer.setDynamicParametersProviderClass(
                    node.getAnnotation(DynamicParameters.class).orElseThrow().value());

            }
            m_valueRefs.add(new ValueRefWrapper(valueRef, pathWithSettingsKey, specialDeserializer));
        }
    }

    private void addValueProvider(final Class<? extends StateProvider> valueProviderClass, final Class<?> fieldType,
        final Location pathWithSettingsKey, final TreeNode<WidgetGroup> node) {
        if (!valueProviderClass.equals(StateProvider.class)) {
            DynamicNodeParametersSerializer specialSerializer = null;
            if (node instanceof Tree<WidgetGroup> tree && tree.isDynamic()) {
                specialSerializer = new DynamicNodeParametersSerializer(
                    node.getAnnotation(DynamicParameters.class).orElseThrow().value());
            }
            validateAgainstType(fieldType, valueProviderClass, StateProvider.class,
                (field, annotationValue) -> field.isAssignableFrom(annotationValue));
            m_valueProviders.add(new ValueProviderWrapper(valueProviderClass, pathWithSettingsKey, specialSerializer));
        }
    }

    private void addDynamicParametersValueProvider(final Class<? extends DynamicParametersProvider> valueProviderClass,
        final Location pathWithSettingsKey) {
        JsonSerializer<Object> specialSerializer = new JsonSerializer<>() {

            @Override
            public void serialize(final Object value, final JsonGenerator gen, final SerializerProvider serializers)
                throws IOException {
                @SuppressWarnings("unchecked") // The dynamic parameters provider returns a DataAndDialog<JsonNode>
                final var json = (JsonNode)((DataAndDialog<Object>)value).getData();
                gen.writeTree(json);
            }
        };

        m_valueProviders.add(new ValueProviderWrapper(valueProviderClass, pathWithSettingsKey, specialSerializer));
    }

    private static <T> void validateAgainstType(final Class<?> fieldType, final Class<? extends T> implementingClass,
        final Class<T> genericInterface, final BiPredicate<Class<?>, Class<?>> testFieldAgainstAnnotationGeneric) {

        final var genericType = GenericTypeFinderUtil.getFirstGenericType(implementingClass, genericInterface);
        if (genericType instanceof Class<?> genericTypeClass) {
            validateAgainstClass(fieldType, implementingClass, genericInterface, genericTypeClass,
                testFieldAgainstAnnotationGeneric);
        } else if (genericType instanceof ParameterizedType parameterizedType) {
            validateAgainstClass(fieldType, implementingClass, genericInterface,
                (Class<?>)parameterizedType.getRawType(), testFieldAgainstAnnotationGeneric);
        }
        // No validation for more complex types
    }

    private static <T> void validateAgainstClass(final Class<?> fieldType, final Class<? extends T> implementingClass,
        final Class<T> genericInterface, final Class<?> genericTypeClass,
        final BiPredicate<Class<?>, Class<?>> testFieldAgainstAnnotationGeneric) {
        CheckUtils.check(
            testFieldAgainstAnnotationGeneric.test(ClassUtils.primitiveToWrapper(fieldType), genericTypeClass),
            UiSchemaGenerationException::new,
            () -> String.format(
                "The generic type \"%s\" of the %s \"%s\" does not match the type \"%s\" of the annotated field",
                genericTypeClass.getSimpleName(), genericInterface.getSimpleName(), implementingClass.getSimpleName(),
                fieldType.getSimpleName()));
    }

}
