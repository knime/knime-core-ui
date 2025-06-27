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
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ARRAY_LAYOUT_DETAIL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Test UI schema generation with arrays.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 */
@SuppressWarnings({"java:S2698", "unused"}) // we accept assertions without messages
class JsonFormsUiSchemaUtilArrayTest {

    @Test
    void testArrayLayout() {
        class TestArrayLayoutSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            ArrayElements[] m_arraySetting;

            @Widget(title = "", description = "")
            Collection<CollectionElements> m_collectionSetting;

            class ArrayElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerSetting1;

                @Widget(title = "", description = "")
                String m_innerSetting2;
            }

            class CollectionElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerCollectionSetting1;

                @Widget(title = "", description = "")
                String m_innerCollectionSetting2;
            }
        }

        final var response = buildTestUiSchema(TestArrayLayoutSettings.class);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/arraySetting");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("arrayElementTitle");
        assertThatJson(response).inPath("$.elements[0].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[0].options.detail[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].options.detail[0].scope").isString()
            .isEqualTo("#/properties/innerSetting1");
        assertThatJson(response).inPath("$.elements[0].options.detail[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].options.detail[1].scope").isString()
            .isEqualTo("#/properties/innerSetting2");

        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/collectionSetting");
        assertThatJson(response).inPath("$.elements[1].options").isObject().doesNotContainKey("arrayElementTitle");
        assertThatJson(response).inPath("$.elements[1].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[1].options.detail[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[1].options.detail[0].scope").isString()
            .isEqualTo("#/properties/innerCollectionSetting1");
        assertThatJson(response).inPath("$.elements[1].options.detail[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[1].options.detail[1].scope").isString()
            .isEqualTo("#/properties/innerCollectionSetting2");
    }

    @Test
    void wrapsElementDetailsInAHorizontalLayoutInCaseOfSingleLineLayoutMode() {

        class TestArrayWidgetSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @ArrayWidget(elementLayout = ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE)
            ArrayElements[] m_arraySetting;

            class ArrayElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerSetting1;

                @Widget(title = "", description = "")
                String m_innerSetting2;
            }
        }

        final var response = buildTestUiSchema(TestArrayWidgetSettings.class);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].options.detail").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].options.detail[0].type").isString()
            .isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[0].options.detail[0].elements").isArray().hasSize(2);
    }

    @Test
    void doesNotWrapInAnAdditionalHorizontalLayoutIfItIsAlreadyOneElement() {
        class TestArrayWidgetSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @ArrayWidget(elementLayout = ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE)
            ArrayElements[] m_arraySetting;

            class ArrayElements implements WidgetGroup {

                @HorizontalLayout
                interface InnerLayout {
                }

                @Widget(title = "", description = "")
                @Layout(InnerLayout.class)
                String m_innerSetting1;

                @Widget(title = "", description = "")
                @Layout(InnerLayout.class)
                String m_innerSetting2;
            }
        }

        final var response = buildTestUiSchema(TestArrayWidgetSettings.class);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].options.detail").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].options.detail[0].elements[0].type").isString()
            .isEqualTo("Control");
    }

    @Test
    void testArrayWidgetAnnotation() {
        class TestArrayWidgetSettings implements DefaultNodeSettings {

            private static final String EXPECTED_ADD_TEXT = "expected add text";

            private static final String EXPECTED_TITLE = "Expected Title";

            @Widget(title = "", description = "")
            @ArrayWidget(addButtonText = EXPECTED_ADD_TEXT)
            ArrayElements[] m_arraySetting1;

            @Widget(title = "", description = "")
            @ArrayWidget(elementTitle = EXPECTED_TITLE)
            ArrayElements[] m_arraySetting2;

            @Widget(title = "", description = "")
            @ArrayWidget(elementLayout = ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "")
            ArrayElements[] m_arraySetting3;

            @Widget(title = "", description = "")
            @ArrayWidget(addButtonText = EXPECTED_ADD_TEXT)
            Collection<ArrayElements> m_collectionSetting1;

            @Widget(title = "", description = "")
            @ArrayWidget(elementTitle = EXPECTED_TITLE)
            Collection<ArrayElements> m_collectionSetting2;

            @Widget(title = "", description = "")
            @ArrayWidget(addButtonText = "")
            Collection<ArrayElements> m_collectionSetting3;

            @Widget(title = "", description = "", advanced = true)
            ArrayElements[] m_arrayAdvancedSetting;

            class ArrayElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerSetting1;
            }
        }
        final var response = buildTestUiSchema(TestArrayWidgetSettings.class);

        assertThatJson(response).inPath("$.elements[0].options.addButtonText").isString()
            .isEqualTo(TestArrayWidgetSettings.EXPECTED_ADD_TEXT);
        assertThatJson(response).inPath("$.elements[1].options.arrayElementTitle").isString()
            .isEqualTo(TestArrayWidgetSettings.EXPECTED_TITLE);
        assertThatJson(response).inPath("$.elements[1].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[2].options").isObject().doesNotContainKey("arrayElementTitle");
        assertThatJson(response).inPath("$.elements[2].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[2].options.elementLayout").isString()
            .isEqualTo("HORIZONTAL_SINGLE_LINE");

        assertThatJson(response).inPath("$.elements[3].options.addButtonText").isString()
            .isEqualTo(TestArrayWidgetSettings.EXPECTED_ADD_TEXT);
        assertThatJson(response).inPath("$.elements[4].options.arrayElementTitle").isString()
            .isEqualTo(TestArrayWidgetSettings.EXPECTED_TITLE);
        assertThatJson(response).inPath("$.elements[4].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[5].options").isObject().doesNotContainKey("addButtonText");
        assertThatJson(response).inPath("$.elements[6].options.isAdvanced").isBoolean().isTrue();
    }

    @Test
    void testDoesNotApplyArrayLayoutOnPrimitiveOrBoxed() {
        enum TestEnum {
                A, B, C;
        }
        class TestPrimitiveOrBoxedArraySettings implements DefaultNodeSettings {

            double[] m_doubleArray;

            Double[] m_boxedDoubleArray;

            int[] m_intArray;

            Integer[] m_boxedIntegerArray;

            boolean[] m_booleanArray;

            Boolean[] m_boxedBooleanArray;

            short[] m_shortArray;

            Short[] m_boxedShortArray;

            long[] m_longArray;

            Long[] m_boxedArray;

            float[] m_floatArray;

            Float[] m_boxedFloatArray;

            char[] m_charArray;

            Character[] m_boxedCharacterArray;

            TestEnum[] m_enumArray;

            Collection<String> m_stringCollection;
        }

        final var response = buildTestUiSchema(TestPrimitiveOrBoxedArraySettings.class);

        for (var item : response.get("elements")) {
            assertThatJson(item).isObject().doesNotContainKey("options");
        }
    }

    @Test
    void testDoesNotApplyArrayLayoutOnStringArrays() {

        class TestStringArraySettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            String[] m_stringArray;
        }

        final var response = buildTestUiSchema(TestStringArraySettings.class);
        assertThatJson(response).inPath("elements[0].options").isObject().doesNotContainKey(TAG_ARRAY_LAYOUT_DETAIL);
    }

    @Test
    void testInternalArrayLayoutElementCheckboxWidget() {
        class TestArrayLayoutWithUpdateSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ArrayWidgetInternal(withElementCheckboxes = true)
            ArrayElements[] m_arraySetting;

            class ArrayElements implements WidgetGroup {

                @ArrayWidgetInternal.ElementCheckboxWidget
                @Widget(title = "inner setting", description = "inner setting description")
                boolean m_innerSetting;
            }
        }

        final var response = buildTestUiSchema(TestArrayLayoutWithUpdateSettings.class);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/arraySetting");
        assertThatJson(response).inPath("$.elements[0].options.detail").isArray().isEmpty();
        assertThatJson(response).inPath("$.elements[0].options.elementCheckboxScope").isString()
            .isEqualTo("#/properties/innerSetting");

    }

    /**
     * In case an element title is required (i.e. the element layout is VERTICAL_CARD) but not set, it should be derived
     * from the field name.
     */
    @Test
    void testFieldNameToElementTitle() {
        class TestArrayLayoutWithFieldNameElementTitle implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ArrayWidget(elementLayout = ArrayWidget.ElementLayout.VERTICAL_CARD)
            ArrayElements[] m_myElements;

            class ArrayElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerSetting1;
            }
        }

        final var response = buildTestUiSchema(TestArrayLayoutWithFieldNameElementTitle.class);

        assertThatJson(response).inPath("$.elements[0].options.arrayElementTitle").isString().isEqualTo("My Element");

    }

    @Test
    void testThrowsOnSetElementTitleWhenNotRequired() {
        class TestArrayLayoutWithFieldNameElementTitle implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ArrayWidget(elementTitle = "My Element Title",
                elementLayout = ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE)
            ArrayElements[] m_myElements;

            class ArrayElements implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_innerSetting1;

                @Widget(title = "", description = "")
                String m_innerSetting2;
            }
        }

        assertThrows(UiSchemaGenerationException.class,
            () -> buildTestUiSchema(TestArrayLayoutWithFieldNameElementTitle.class));
    }

}