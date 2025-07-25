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
 *   Sep 17, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.text.TextInputWidget;

class UiSchemaWidgetModificationTest {

    @Test
    void testModifiesExistingAnnotation() {

        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class ModifyFieldTitle implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .modifyAnnotation(TextInputWidget.class).withProperty("placeholder", "modified placeholder")
                        .modify();
                }

            }

            @Modification(ModifyFieldTitle.class)
            WidgetGroupSettings m_widgetGroup;
        }

        final var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString()
            .isEqualTo("modified placeholder");
    }

    @Test
    void testWorksOnClassAnnotations() {

        final class TestSettings implements NodeParameters {

            @Modification(ModifyPlaceholder.class)
            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class ModifyPlaceholder implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .modifyAnnotation(TextInputWidget.class).withProperty("placeholder", "modified placeholder")
                        .modify();
                }

            }

            WidgetGroupSettings m_widgetGroup;
        }

        final var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString()
            .isEqualTo("modified placeholder");
    }

    @Test
    void testAddsNewAnnotation() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class AddPlaceholder implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .addAnnotation(TextInputWidget.class).withProperty("placeholder", "added placeholder").modify();
                }

            }

            @Modification(AddPlaceholder.class)
            WidgetGroupSettings m_widgetGroup;
        }

        final var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString().isEqualTo("added placeholder");
    }

    @Test
    void testRemoveAnnotation() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class RemoveTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .removeAnnotation(TextInputWidget.class);
                }

            }

            @Modification(RemoveTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }

        final var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("options");

    }

    @Test
    void throwsForRemoveAnnotationOnMissing() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class RemoveTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .removeAnnotation(TextInputWidget.class);
                }

            }

            @Modification(RemoveTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }
        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
            .isEqualTo("Annotation cannot be removed because it is not present: TextInputWidget");
    }

    @Test
    void throwsForModifyAnnotationOnMissing() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class ModifyTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .modifyAnnotation(TextInputWidget.class).withProperty("placeholder", "modified placeholder")
                        .modify();
                }

            }

            @Modification(ModifyTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }
        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
            .isEqualTo("Annotation cannot be modified because it is not present: TextInputWidget");

    }

    @Test
    void throwsForAddAnnotationIfAlreadyPresent() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class AddTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .addAnnotation(TextInputWidget.class).withProperty("placeholder", "added placeholder").modify();
                }

            }

            @Modification(AddTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }
        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
            .isEqualTo("Annotation cannot be added because it is already present: TextInputWidget");
    }

    @Test
    void throwsOnInvalidParameterName() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class ModifyTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .modifyAnnotation(TextInputWidget.class).withProperty("invalidParameter", "some value").modify();
                }

            }

            @Modification(ModifyTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }
        assertThat(
            assertThrows(IllegalArgumentException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("No method with name \"invalidParameter\" found in TextInputWidget");
    }

    @Test
    void throwsOnInvalidParameterType() {
        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @TextInputWidget(placeholder = "some placeholder")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class ModifyTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.FieldReference.class)
                        .modifyAnnotation(TextInputWidget.class).withProperty("placeholder", 42).modify();
                }

            }

            @Modification(ModifyTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;
        }
        assertThat(
            assertThrows(IllegalArgumentException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
                .isEqualTo("The value for method \"placeholder\" in TextInputWidget must be of type String");

    }

    @Test
    void throwsWhenAddingAnnotationWithoutSettingRequiredProperties() {
        final class NoEffectTypeTestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class AddTextInputWidget implements Modification.Modifier {

                static final class MyPredicateProvider implements EffectPredicateProvider {

                    @Override
                    public EffectPredicate init(final PredicateInitializer i) {
                        return i.never();
                    }

                }

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(NoEffectTypeTestSettings.WidgetGroupSettings.FieldReference.class)
                        .addAnnotation(Effect.class).withProperty("predicate", MyPredicateProvider.class)// No effectType
                        .modify();
                }

            }

            @Modification(AddTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;

        }

        assertThat(assertThrows(IllegalArgumentException.class, () -> buildTestUiSchema(NoEffectTypeTestSettings.class))
            .getMessage()).isEqualTo("The property \"type\" is required for Effect");

        final class NoValueReferenceTestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class FieldReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(FieldReference.class)
                String m_field;
            }

            static final class AddTextInputWidget implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(NoValueReferenceTestSettings.WidgetGroupSettings.FieldReference.class)
                        .addAnnotation(ValueReference.class).modify();
                }

            }

            @Modification(AddTextInputWidget.class)
            WidgetGroupSettings m_widgetGroup;

        }

        assertThat(
            assertThrows(IllegalArgumentException.class, () -> buildTestUiSchema(NoValueReferenceTestSettings.class))
                .getMessage()).isEqualTo("The property \"value\" is required for ValueReference");
    }

    @Test
    void throwsOnDuplicateReferences() {

        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class DuplicateMissingReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                @Modification.WidgetReference(DuplicateMissingReference.class)
                String m_field1;

                @Widget(title = "", description = "")
                @Modification.WidgetReference(DuplicateMissingReference.class)
                String m_field2;
            }

            static final class WidgetModificationWithDuplicateReferences implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.DuplicateMissingReference.class);
                }

            }

            @Modification(WidgetModificationWithDuplicateReferences.class)
            WidgetGroupSettings m_widgetGroup;

        }

        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
            .isEqualTo("Multiple nodes with the same reference found: widgetGroup.field1, widgetGroup.field2");

    }

    @Test
    void testThrowsOnMissingReferences() {

        final class TestSettings implements NodeParameters {

            final class WidgetGroupSettings implements WidgetGroup {

                static final class MissingReference implements Modification.Reference {

                }

                @Widget(title = "", description = "")
                // Reference is not bound to this field by @WidgetReference
                String m_field;
            }

            static final class WidgetModificationWithMissingReferences implements Modification.Modifier {

                @Override
                public void modify(final Modification.WidgetGroupModifier group) {
                    group.find(TestSettings.WidgetGroupSettings.MissingReference.class);
                }

            }

            @Modification(WidgetModificationWithMissingReferences.class)
            WidgetGroupSettings m_widgetGroup;

        }

        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(TestSettings.class)).getMessage())
            .isEqualTo("No node with the reference found: MissingReference");

    }

}
