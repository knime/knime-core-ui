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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextAreaWidget;

/**
 * Responsible for creating the renderer specs for the widget tree leafes.
 *
 * @author Paul Bärnreuther
 */
public class WidgetTreeRenderers {

    record WidgetTreeNodeTester(//
        Function<TreeNode<WidgetGroup>, ControlRendererSpec> creator, //
        Predicate<TreeNode<WidgetGroup>> tester //
    ) {
    }

    static final WidgetTreeNodeTester[] TESTERS = new WidgetTreeNodeTester[]{//
        new WidgetTreeNodeTester(//
            node -> new TextAreaRenderer(node, getPresentAnnotation(node, TextAreaWidget.class)), //
            hasAnnotationAssertingType(TextAreaWidget.class, String.class)), //
        new WidgetTreeNodeTester(//
            node -> new LocalFileChooserRenderer(node, getPresentAnnotation(node, LocalFileReaderWidget.class)), //
            hasAnnotationAssertingTypeAndNoSecondInvalidAnnotation(LocalFileReaderWidget.class, String.class,
                LocalFileWriterWidget.class)), //
        new WidgetTreeNodeTester(TextRenderer::new, //
            node -> String.class.equals(node.getRawClass())
                || ClassUtils.primitiveToWrapper(node.getRawClass()).equals(Character.class)), //
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
            node -> LegacyCredentials.class.isAssignableFrom(node.getRawClass())) //

    };

    private static Predicate<TreeNode<WidgetGroup>> hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return node -> node.getAnnotation(annotationClass).isPresent();
    }

    private static Predicate<TreeNode<WidgetGroup>>
        hasAnnotationAssertingType(final Class<? extends Annotation> annotationClass, final Class<?> expectedType) {
        return node -> {
            final var hasAnnotation = hasAnnotation(annotationClass).test(node);
            if (hasAnnotation) {
                throwIfWrongType(expectedType, node, annotationClass);
            }
            return hasAnnotation;
        };
    }

    private static void throwIfWrongType(final Class<?> expectedType, final TreeNode<WidgetGroup> node,
        final Class<? extends Annotation> annotationClass) {
        final var fieldClass = node.getRawClass();
        if (!fieldClass.equals(expectedType)) {
            throw new UiSchemaGenerationException(
                String.format("The annotation %s is not applicable for setting field %s with type %s",
                    annotationClass.getSimpleName(), String.join(".", node.getPath()), fieldClass));
        }
    }

    private static void throwIfInvalidSecondAnnotation(final TreeNode<WidgetGroup> node,
        final Class<? extends Annotation> validAnnotationClass,
        final Class<? extends Annotation> invalidAnnotationClass) {
        final var hasInvalidAnnotation = hasAnnotation(invalidAnnotationClass).test(node);
        if (hasInvalidAnnotation) {
            throw new UiSchemaGenerationException(String.format("A widget cannot be both, a %s and a %s.",
                validAnnotationClass.getSimpleName(), invalidAnnotationClass.getSimpleName()));
        }
    }

    private static Predicate<TreeNode<WidgetGroup>> hasAnnotationAssertingTypeAndNoSecondInvalidAnnotation(
        final Class<? extends Annotation> annotationClass, final Class<?> expectedType,
        final Class<? extends Annotation> invalidAnnotationClass) {
        return node -> {
            final var hasAnnotation = hasAnnotation(annotationClass).test(node);
            if (!hasAnnotation) {
                return false;
            }
            throwIfWrongType(expectedType, node, annotationClass);
            throwIfInvalidSecondAnnotation(node, annotationClass, invalidAnnotationClass);
            return true;
        };
    }

    /**
     * We assume that {@link hasAnnotation} is already checked.
     */
    private static <T extends Annotation> T getPresentAnnotation(final TreeNode<WidgetGroup> node,
        final Class<T> annotationClass) {
        return node.getAnnotation(annotationClass).orElseThrow(IllegalStateException::new);
    }

    /**
     * @param node the node to check
     * @return the renderer spec for the given node or {@code null} if not supported
     */
    public static ControlRendererSpec getRendererSpec(final TreeNode<WidgetGroup> node) {
        if (!(node instanceof LeafNode<WidgetGroup>)) {
            return null;
        }
        return Stream.of(TESTERS).filter(tester -> tester.tester().test(node))//
            .findFirst()//
            .map(tester -> tester.creator().apply(node))//
            .orElse(null);
    }

}
