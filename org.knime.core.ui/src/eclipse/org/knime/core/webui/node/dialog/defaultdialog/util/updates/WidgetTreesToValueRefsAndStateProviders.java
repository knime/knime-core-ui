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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.WidgetGroupTraverser.Configuration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget.DoubleProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.InternalArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopBooleanProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

final class WidgetTreesToValueRefsAndStateProviders {

    record ValueRefWrapper(Class<? extends Reference> valueRef, PathsWithSettingsType fieldLocation) {
    }

    record ValueProviderWrapper(Class<? extends StateProvider> stateProviderClass,
        PathsWithSettingsType fieldLocation) {
    }

    record ValueRefsAndStateProviders(Collection<ValueRefWrapper> valueRefs,
        Collection<ValueProviderWrapper> valueProviders, Collection<Class<? extends StateProvider>> uiStateProviders) {
    }

    static final Configuration TRAVERSAL_CONFIG = new Configuration.Builder()//
        .includeWidgetGroupFields()//
        .build();

    private final Collection<ValueRefWrapper> m_valueRefs = new ArrayList<>();

    private final Collection<ValueProviderWrapper> m_valueProviders = new ArrayList<>();

    private final Collection<Class<? extends StateProvider>> m_uiStateProviders = new ArrayList<>();

    /**
     * @param widgetTrees a collection of widget trees derived from settings classes to collect annotated fields from
     * @return the valueRef and updates annotations
     */
    ValueRefsAndStateProviders
        widgetTreesToValueRefsAndStateProviders(final Collection<Tree<WidgetGroup>> widgetTrees) {
        widgetTrees.forEach(this::traverseWidgetTree);
        return new ValueRefsAndStateProviders(m_valueRefs, m_valueProviders, m_uiStateProviders);

    }

    private void traverseWidgetTree(final Tree<WidgetGroup> tree) {
        tree.getChildren().forEach(this::traverseWidgetTreeNode);
    }

    private void traverseWidgetTreeNode(final TreeNode<WidgetGroup> node) {
        addWidgetValueAnnotationValueRefAndValueProviderForNode(node);
        addUiStateProviderForNode(node);
        if (node instanceof Tree<WidgetGroup> widgetTree) {
            traverseWidgetTree(widgetTree);
        } else if (node instanceof ArrayParentNode<WidgetGroup> arrayWidgetNode) {
            traverseWidgetTree(arrayWidgetNode.getElementTree());
        }
    }

    private record UiStateProviderSpec<T extends Annotation, S extends StateProvider>( //
        Class<T> annotationClass, //
        Function<T, Class<? extends S>> getProviderParameter, //
        Class<? extends S> ignoredDefaultParameter //
    ) {
    }

    private static List<UiStateProviderSpec<? extends Annotation, ? extends StateProvider>> uiStateProviderSpecs =
        List.of( //
            new UiStateProviderSpec<>(//
                FileWriterWidget.class, //
                FileWriterWidget::fileExtensionProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                LocalFileWriterWidget.class, //
                LocalFileWriterWidget::fileExtensionProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                CredentialsWidget.class, //
                CredentialsWidget::hasPasswordProvider, //
                NoopBooleanProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                CredentialsWidget.class, //
                CredentialsWidget::hasUsernameProvider, //
                NoopBooleanProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                IntervalWidget.class, //
                IntervalWidget::typeProvider, //
                null //
            ), //
            new UiStateProviderSpec<>( //
                ChoicesWidget.class, //
                ChoicesWidget::choicesProvider, //
                ChoicesStateProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                DateTimeFormatPickerWidget.class, //
                DateTimeFormatPickerWidget::formatProvider, //
                null //
            ), //
            new UiStateProviderSpec<>( //
                TextInputWidget.class, //
                TextInputWidget::placeholderProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderSpec<>(//
                TextMessage.class, //
                TextMessage::value, //
                null //
            ), //
            new UiStateProviderSpec<>( //
                InternalArrayWidget.class, //
                InternalArrayWidget::titleProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                InternalArrayWidget.class, //
                InternalArrayWidget::subTitleProvider, //
                NoopStringProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                ArrayWidget.class, //
                ArrayWidget::elementDefaultValueProvider, //
                StateProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                NumberInputWidget.class, //
                NumberInputWidget::minProvider, //
                DoubleProvider.class //
            ), //
            new UiStateProviderSpec<>( //
                NumberInputWidget.class, //
                NumberInputWidget::maxProvider, //
                DoubleProvider.class //
            ));

    private void addUiStateProviderForNode(final TreeNode<WidgetGroup> node) {
        uiStateProviderSpecs.stream().forEach(
            spec -> getUiStateProviderForNodeAndSpecificAnnotation(node, spec).ifPresent(m_uiStateProviders::add));
    }

    private static <T extends Annotation, S extends StateProvider> Optional<Class<? extends S>>
        getUiStateProviderForNodeAndSpecificAnnotation(final TreeNode<WidgetGroup> node,
            final UiStateProviderSpec<T, S> spec) {
        if (!node.getPossibleAnnotations().contains(spec.annotationClass())) {
            return Optional.empty();
        }
        final var annotation = node.getAnnotation(spec.annotationClass());
        if (annotation.isEmpty()) {
            return Optional.empty();
        }
        final var stateProvider = spec.getProviderParameter().apply(annotation.get());
        if (spec.ignoredDefaultParameter != null && stateProvider.equals(spec.ignoredDefaultParameter())) {
            return Optional.empty();
        }
        return Optional.of(stateProvider);

    }

    private void addWidgetValueAnnotationValueRefAndValueProviderForNode(final TreeNode<WidgetGroup> node) {
        final var pathsWithSettingsKey = PathsWithSettingsType.fromTreeNode(node);
        final var type = node.getType();
        node.getAnnotation(ValueReference.class)
            .ifPresent(valueReference -> addValueRef(valueReference.value(), type, pathsWithSettingsKey));
        node.getAnnotation(ValueProvider.class)
            .ifPresent(valueProvider -> addValueProvider(valueProvider.value(), type, pathsWithSettingsKey));
    }

    private void addValueRef(final Class<? extends Reference> valueRef, final Class<?> type,
        final PathsWithSettingsType pathWithSettingsKey) {
        if (!valueRef.equals(Reference.class)) {
            validateAgainstType(type, valueRef, Reference.class,
                (field, annotationValue) -> annotationValue.isAssignableFrom(field));
            m_valueRefs.add(new ValueRefWrapper(valueRef, pathWithSettingsKey));
        }
    }

    private void addValueProvider(final Class<? extends StateProvider> valueProviderClass, final Class<?> fieldType,
        final PathsWithSettingsType pathWithSettingsKey) {
        if (!valueProviderClass.equals(StateProvider.class)) {
            validateAgainstType(fieldType, valueProviderClass, StateProvider.class,
                (field, annotationValue) -> field.isAssignableFrom(annotationValue));
            m_valueProviders.add(new ValueProviderWrapper(valueProviderClass, pathWithSettingsKey));
        }
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
