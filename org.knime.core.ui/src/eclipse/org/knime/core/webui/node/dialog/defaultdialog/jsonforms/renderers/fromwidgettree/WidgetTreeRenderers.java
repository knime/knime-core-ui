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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dirty.DirtyTracker;
import org.knime.core.webui.node.dialog.defaultdialog.internal.extension.DefaultNodeDialogWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.WidgetRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.legacy.LegacyMultiFileSelection;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.text.TextAreaWidget;

/**
 * Responsible for creating the renderer specs for the widget tree leafes.
 *
 * @author Paul Bärnreuther
 */
public class WidgetTreeRenderers {

    private interface IWidgetTreeNodeTester {

        /**
         * Whether the given node should be handled by this tester.
         *
         * @param node the node to test
         * @return {@code true} if supported, {@code false} otherwise
         */
        boolean test(TreeNode<WidgetGroup> node);

        /**
         * Create the renderer spec for the given node.
         *
         * @param node the node to create the renderer for
         * @param nodeParametersInput the node parameters input
         * @return the renderer spec
         */
        WidgetRendererSpec<?> create(TreeNode<WidgetGroup> node, NodeParametersInput nodeParametersInput);
    }

    record WidgetTreeNodeTester(//
        Function<TreeNode<WidgetGroup>, WidgetRendererSpec<?>> creator, //
        Predicate<TreeNode<WidgetGroup>> tester //
    ) implements IWidgetTreeNodeTester {

        @Override
        public boolean test(final TreeNode<WidgetGroup> node) {
            return tester().test(node);
        }

        @Override
        public WidgetRendererSpec<?> create(final TreeNode<WidgetGroup> node,
            final NodeParametersInput nodeParametersInput) {
            return creator().apply(node);
        }

    }

    record WidgetTreeNodeTesterWithInput(//
        BiFunction<TreeNode<WidgetGroup>, NodeParametersInput, ControlRendererSpec> creator, //
        Predicate<TreeNode<WidgetGroup>> tester //
    ) implements IWidgetTreeNodeTester {

        @Override
        public boolean test(final TreeNode<WidgetGroup> node) {
            return tester().test(node);
        }

        @Override
        public ControlRendererSpec create(final TreeNode<WidgetGroup> node,
            final NodeParametersInput nodeParametersInput) {
            return creator().apply(node, nodeParametersInput);
        }

    }

    static final IWidgetTreeNodeTester[] TESTERS = new IWidgetTreeNodeTester[]{//
        new WidgetTreeNodeTester(//
            node -> new TextAreaRenderer(node, getPresentAnnotation(node, TextAreaWidget.class)), //
            hasAnnotationAssertingTypes(TextAreaWidget.class, String.class)), //
        new WidgetTreeNodeTester(IntegerRenderer::new,
            node -> List.of(Byte.class, Integer.class, Duration.class)
                .contains(ClassUtils.primitiveToWrapper(node.getRawClass()))), // bytes and integers
        new WidgetTreeNodeTester(NumberRenderer::new,
            node -> Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(node.getRawClass()))), // all other numbers
        new WidgetTreeNodeTester(CheckboxRenderer::new,
            node -> Boolean.class.equals(ClassUtils.primitiveToWrapper(node.getRawClass()))), //
        new WidgetTreeNodeTester(DateRenderer::new, //
            node -> LocalDate.class.equals(node.getRawClass())),
        new WidgetTreeNodeTester(TimeRenderer::new, //
            node -> LocalTime.class.equals(node.getRawClass())),
        new WidgetTreeNodeTester(LocalDateTimeRenderer::new, //
            node -> LocalDateTime.class.equals(node.getRawClass())),
        new WidgetTreeNodeTester(ZonedDateTimeRenderer::new, //
            node -> ZonedDateTime.class.equals(node.getRawClass())),
        new WidgetTreeNodeTester(CredentialsRenderer::new,
            node -> Credentials.class.isAssignableFrom(node.getRawClass())), //
        new WidgetTreeNodeTester(LegacyCredentialsRenderer::new,
            node -> LegacyCredentials.class.isAssignableFrom(node.getRawClass())), //
        new WidgetTreeNodeTesterWithInput(FileChooserRenderer::new,
            node -> FileSelection.class.isAssignableFrom(node.getRawClass())),
        new WidgetTreeNodeTesterWithInput(MultiFileChooserRenderer::new,
            hasAnnotationAssertingTypes(MultiFileSelectionWidget.class, MultiFileSelection.class,
                LegacyMultiFileSelection.class)),
        new WidgetTreeNodeTesterWithInput(StringFileChooserRenderer::new,
            hasOneOfAnnotations(FileSelectionWidget.class, FileReaderWidget.class,
                FileWriterWidget.class).and(node -> String.class.equals(node.getRawClass()))),
        new WidgetTreeNodeTester(TextRenderer::new, //
            node -> String.class.equals(node.getRawClass())
                || ClassUtils.primitiveToWrapper(node.getRawClass()).equals(Character.class)), //
        new WidgetTreeNodeTester(node -> new DirtyTrackerRenderer(getPresentAnnotation(node, DirtyTracker.class)),
            hasAnnotationAssertingTypes(DirtyTracker.class, Void.class)),
        new WidgetTreeNodeTester(node -> new TextMessageRenderer(getPresentAnnotation(node, TextMessage.class)),
            hasAnnotationAssertingTypes(TextMessage.class, Void.class))};

    private static Predicate<TreeNode<WidgetGroup>> hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return node -> node.getPossibleAnnotations().contains(annotationClass)
            && node.getAnnotation(annotationClass).isPresent();
    }

    @SafeVarargs
    private static Predicate<TreeNode<WidgetGroup>>
        hasOneOfAnnotations(final Class<? extends Annotation>... annotationClasses) {
        return node -> Stream.of(annotationClasses).anyMatch(ac -> hasAnnotation(ac).test(node));
    }

    private static Predicate<TreeNode<WidgetGroup>> hasAnnotationAssertingTypes(
        final Class<? extends Annotation> annotationClass, final Class<?>... expectedTypes) {
        return node -> {
            final var hasAnnotation = hasAnnotation(annotationClass).test(node);
            if (hasAnnotation) {
                throwIfWrongType(expectedTypes, node, annotationClass);
            }
            return hasAnnotation;
        };
    }

    private static void throwIfWrongType(final Class<?>[] expectedTypes, final TreeNode<WidgetGroup> node,
        final Class<? extends Annotation> annotationClass) {
        final var fieldClass = node.getRawClass();
        if (!ArrayUtils.contains(expectedTypes, fieldClass)) {
            throw new UiSchemaGenerationException(
                String.format("The annotation %s is not applicable for setting field %s with type %s",
                    annotationClass.getSimpleName(), String.join(".", node.getPath()), fieldClass));
        }
    }

    /**
     * We assume that {@link hasAnnotation} is already checked.
     */
    private static <T extends Annotation> T getPresentAnnotation(final TreeNode<WidgetGroup> node,
        final Class<T> annotationClass) {
        return node.getAnnotation(annotationClass).orElseThrow(IllegalStateException::new);
    }

    private static Stream<IWidgetTreeNodeTester> getAllTesters() {
        return Stream.concat(DefaultNodeDialog.getAdditionalWidgets().stream().map(WidgetTreeRenderers::toTester),
            Stream.of(TESTERS));
    }

    private static IWidgetTreeNodeTester toTester(final DefaultNodeDialogWidget widget) {
        return new IWidgetTreeNodeTester() {

            @Override
            public boolean test(final TreeNode<WidgetGroup> node) {
                return widget.isApplicable(node);
            }

            @Override
            public WidgetRendererSpec<?> create(final TreeNode<WidgetGroup> node,
                final NodeParametersInput nodeParametersInput) {
                return widget.createRendererSpec(node, nodeParametersInput);
            }
        };

    }

    /**
     * Get the renderer spec for the given node. If no renderer is supported for the given node, {@code null} is
     * returned.
     *
     * @param node the node to check
     * @param nodeParametersInput the node parameters input
     * @return the renderer spec for the given node or {@code null} if not supported
     */
    public static WidgetRendererSpec<?> getRendererSpec(final TreeNode<WidgetGroup> node,
        final NodeParametersInput nodeParametersInput) {
        return getAllTesters().filter(tester -> tester.test(node))//
            .findFirst()//
            .map(tester -> tester.create(node, nodeParametersInput))//
            .orElse(null);
    }

}
