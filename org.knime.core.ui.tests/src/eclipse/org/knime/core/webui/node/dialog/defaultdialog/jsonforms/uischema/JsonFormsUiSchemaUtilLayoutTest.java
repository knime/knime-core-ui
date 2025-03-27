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
 *   Mar 27, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildUiSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilLayoutTest.SuperclassAnnotationTestLayout.AfterCenterLayout;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilLayoutTest.SuperclassAnnotationTestLayout.BeforeCenterLayout;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilLayoutTest.SuperclassAnnotationTestLayout.CenterLayout;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.ClusterOfSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.TestSettingsLayout;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.TestLayout.FirstSection;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.TestLayout.SecondSection;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Inside;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Tests for the various ways to define nested layouts
 *
 * The basic tests for the ui schema generation of each single layout part are contained in
 * {@link JsonFormsUiSchemaUtilTest}
 *
 * @author Paul Baernreuther
 */
class JsonFormsUiSchemaUtilLayoutTest {
    class TestLayoutViewSettings implements DefaultNodeSettings {
        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section1.class)
        String m_testViewSetting1;

        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section2.class)
        String m_testViewSetting2;
    }

    class TestLayoutModelSettings implements DefaultNodeSettings {
        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section1.class)
        String m_testModelSetting1;

        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section2.NestedSection.class)
        String m_nestedModelSetting;
    }

    /**
     * Tests that model and view settings can be combined to a layout and that nesting interfaces leads to nested
     * layouts
     */
    @Test
    void testLayout() throws JsonProcessingException {
        final var settings = new LinkedHashMap<SettingsType, Class<? extends WidgetGroup>>();
        settings.put(SettingsType.MODEL, TestLayoutModelSettings.class);
        settings.put(SettingsType.VIEW, TestLayoutViewSettings.class);
        final var response = buildUiSchema(settings);
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(2);
        //Section1
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/testModelSetting1");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/view/properties/testViewSetting1");
        //Section2
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/view/properties/testViewSetting2");
        //NestedSection
        assertThatJson(response).inPath("$.elements[1].elements[1].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[1].elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/nestedModelSetting");
    }

    interface TestDefaultParentLayout {
        @Section
        interface DefaultSection {
        }

        @Section
        interface Section1 {
        }
    }

    @Layout(TestDefaultParentLayout.DefaultSection.class)
    class TestDefaultParentSettings implements DefaultNodeSettings {
        @Widget(title = "", description = "")
        String m_defaultParentSetting;

        @Widget(title = "", description = "")
        @Layout(TestDefaultParentLayout.Section1.class)
        String m_sectionSetting;

        ClusterOfSettings m_clusterOfSettingsDefaultParent;

        @Layout(TestDefaultParentLayout.Section1.class)
        ClusterOfSettings m_clusterOfSettingsInSection;
    }

    /**
     * Tests that when a containing class is annotated with a layout annotation, this defines the layout for all of its
     * fields that do not set a layout themselves.
     */
    @Test
    void testDefaultParent() throws JsonProcessingException {
        final var response = buildTestUiSchema(TestDefaultParentSettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);
        //Default Section
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(3);
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/defaultParentSetting");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/clusterOfSettingsDefaultParent/properties/sub1");
        assertThatJson(response).inPath("$.elements[0].elements[2].scope").isString()
            .isEqualTo("#/properties/model/properties/clusterOfSettingsDefaultParent/properties/sub2");
        //Section1
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(3);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/sectionSetting");
        assertThatJson(response).inPath("$.elements[1].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/clusterOfSettingsInSection/properties/sub1");
        assertThatJson(response).inPath("$.elements[1].elements[2].scope").isString()
            .isEqualTo("#/properties/model/properties/clusterOfSettingsInSection/properties/sub2");
    }

    interface TestDefaultParentOnSuperClassLayout {

        @Section
        interface DefaultSection {
        }

        @Section
        @After(DefaultSection.class)
        interface FieldSection {
        }

        @Section
        @After(FieldSection.class)
        interface SuperClassDefaultSection {
        }

        @Section
        @After(SuperClassDefaultSection.class)
        interface SuperClassFieldSection {
        }
    }

    @Layout(TestDefaultParentOnSuperClassLayout.SuperClassDefaultSection.class)
    class SuperClassWithDefaultParent implements DefaultNodeSettings {

        @Widget(title = "", description = "")
        String m_defaultParentSuperClassSetting;

        @Widget(title = "", description = "")
        @Layout(TestDefaultParentOnSuperClassLayout.SuperClassFieldSection.class)
        String m_superClassSetting;

    }

    @Layout(TestDefaultParentOnSuperClassLayout.DefaultSection.class)
    class TestDefaultParentOnSuperClassSettings extends SuperClassWithDefaultParent {

        @Widget(title = "", description = "")
        String m_defaultParentSetting;

        @Widget(title = "", description = "")
        @Layout(TestDefaultParentOnSuperClassLayout.FieldSection.class)
        String m_setting;

    }

    /**
     * Tests that a layout annotation on a super-class only serves as default layout for the fields that are also
     * defined within that superclass.
     */
    @Test
    void testDefaultParentOnSuperclass() throws JsonProcessingException {
        final var response = buildTestUiSchema(TestDefaultParentOnSuperClassSettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(4);
        // DefaultSection
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/defaultParentSetting");
        // FieldSection
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/setting");
        // SuperClassDefaultSection
        assertThatJson(response).inPath("$.elements[2].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[2].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/defaultParentSuperClassSetting");
        // SuperClassFieldSection
        assertThatJson(response).inPath("$.elements[3].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[3].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/superClassSetting");
    }

    interface TestNoLayoutAnnotationLayout {

        @Section
        interface Section1 {
        }
    }

    class TestNoLayoutAnnotationSettings implements DefaultNodeSettings {

        @Widget(title = "", description = "")
        String m_rootSetting;

        @Widget(title = "", description = "")
        @Layout(TestNoLayoutAnnotationLayout.Section1.class)
        String m_sectionSetting;

    }

    /**
     * Test that no layout means "taking the class" as layout
     */
    @Test
    void testNoLayoutAnnotation() throws JsonProcessingException {
        final var response = buildTestUiSchema(TestNoLayoutAnnotationSettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/rootSetting");
        //Section1
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/sectionSetting");
    }

    static class TestLayoutWithinSettingsSettings implements DefaultNodeSettings {
        @Section(title = "first")
        static interface Section1 {
        }

        @Section(title = "second")
        static interface Section2 {
        }

        @Widget(title = "", description = "")
        @Layout(Section1.class)
        String m_foo;

        @Widget(title = "", description = "")
        @Layout(Section2.class)
        String m_bar;
    }

    /**
     * Test that using layout interfaces within the settings themselves works as expected.
     */
    @Test
    void testLayoutWithinSettings() {
        final var response = buildTestUiSchema(TestLayoutWithinSettingsSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("first");
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/foo");

        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[1].label").isString().isEqualTo("second");
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/bar");
    }

    static class NoRootForSectionSettings implements DefaultNodeSettings {
        @Widget(title = "", description = "")
        @Layout(SectionWithoutEnclosingClass.class)
        String m_foo;
    }

    /**
     * Test that using a layout interface that is not nested within any other class is working as expected
     */
    @Test
    void testSingleLayoutPartWithoutRoot() {
        var response = buildTestUiSchema(NoRootForSectionSettings.class);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/foo");
    }

    static class TestMultipleRootsOne implements DefaultNodeSettings {
        @Section
        static interface Section1 {
        }

        @Widget(title = "", description = "")
        @Layout(Section1.class)
        String m_foo;
    }

    static class TestMultipleRootsTwo implements DefaultNodeSettings {

        @Widget(title = "", description = "")
        @Layout(GeneralTestLayout.GeneralSection1.class)
        String m_bar;
    }

    /**
     * Test that if two layout parts are referenced that do not know how they are to behave relative to each other, an
     * error is thrown.
     */
    @Test
    void testThrowsIfMultipleLayoutRootsAreDetected() {
        final Map<SettingsType, Class<? extends WidgetGroup>> settings =
            Map.of(SettingsType.MODEL, TestMultipleRootsOne.class, SettingsType.VIEW, TestMultipleRootsTwo.class);
        assertThat(assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(settings))).message()
            .contains("Multiple root layout nodes detected").contains("Section1", "GeneralSection1");
    }

    /**
     * Test that a field annotation is never ignored even if the class defines a layout annotation.
     */
    @Test
    void testOverwritesClassAnnotationsWithFieldAnnotationsIfBothAreGiven() {
        class TestFieldWithTwoLayoutAnnotationsSettings implements DefaultNodeSettings {

            @Section(title = "Section1")
            interface Section1 {
            }

            @Section(title = "Section2")
            interface Section2 {
            }

            @Layout(Section2.class)
            class TwoAnnotationsFieldClass implements WidgetGroup {

                @Widget(title = "", description = "")
                String m_setting;

            }

            @Layout(Section1.class)
            TwoAnnotationsFieldClass m_settingWithTowAnnotations;
        }
        final var response = buildTestUiSchema(TestFieldWithTwoLayoutAnnotationsSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("Section1");
        assertThatJson(response).inPath("$.elements[0].elements[0].type").isString().isEqualTo("Control");
    }

    @Test
    void testVirtualSections() throws JsonProcessingException {

        interface TestVirtualSectionLayout {
            @Section(title = "Section1")
            interface Section1 {
            }

            interface Section2 { // A virtual section
            }

            @Section(title = "Section3")
            interface Section3 {
            }
        }

        class VirtualLayoutSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @Layout(TestVirtualSectionLayout.Section1.class)
            String m_setting1;

            @Widget(title = "", description = "")
            @Layout(TestVirtualSectionLayout.Section2.class)
            String m_setting2;

            @Widget(title = "", description = "")

            @Layout(TestVirtualSectionLayout.Section3.class)
            String m_setting3;
        }

        final var response = buildTestUiSchema(VirtualLayoutSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(3);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("Section1");
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[2].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[2].label").isString().isEqualTo("Section3");
    }

    /**
     * Tests that empty sections are not listed in the output.
     *
     * This is important since we might want to share a layout between nodes and some of them do not have settings for a
     * certain section.
     */
    @Test
    void testEmptySection() throws JsonProcessingException {

        interface TestEmptySectionLayout {
            @Section(title = "Section1")
            interface Section1 {
            }

            @Section(title = "Section2")
            interface Section2 {
            }
        }

        class TestEmptySectionSettings implements DefaultNodeSettings {
            @Widget(title = "", description = "")
            @Layout(TestEmptySectionLayout.Section1.class)
            String m_setting1;
        }

        final var response = buildTestUiSchema(TestEmptySectionSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("Section1");
    }

    static final class MySelfContainedWidgetGroup implements WidgetGroup {

        @HorizontalLayout
        interface MyHorizontalLayout {

        }

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field1;

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field2;
    }

    @Test
    void testWidgetGroupContainedLayout() {
        class TestSettings implements DefaultNodeSettings {

            MySelfContainedWidgetGroup m_myWidgetGroup = new MySelfContainedWidgetGroup();

            @Section(title = "My Section")
            interface MySection {

            }

            @Widget(title = "", description = "")
            String m_mySetting;

            @Widget(title = "", description = "")
            @Layout(MySection.class)
            String m_otherSetting;

            @Layout(MySection.class)
            MySelfContainedWidgetGroup m_myWidgetGroupWithLayout = new MySelfContainedWidgetGroup();
        }

        final var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(3);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("mySetting");
        assertThatJson(response).inPath("$.elements[2].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[2].label").isString().isEqualTo("My Section");
        assertThatJson(response).inPath("$.elements[2].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[2].elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[2].elements[0].scope").isString().contains("otherSetting");
        assertThatJson(response).inPath("$.elements[2].elements[1].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[2].elements[1].elements").isArray().hasSize(2);

    }

    static final class SelfContainedLayoutWithFieldsWithoutLayout implements WidgetGroup {

        @HorizontalLayout
        interface MyHorizontalLayout {

        }

        @Widget(title = "", description = "")
        String m_fieldWithoutLayout;

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field1;

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field2;

    }

    @Test
    void testWidgetGroupContainedLayoutWithFieldsWithoutLayout() {

        class TestSettings implements DefaultNodeSettings {

            SelfContainedLayoutWithFieldsWithoutLayout m_selfContainedLayout =
                new SelfContainedLayoutWithFieldsWithoutLayout();

            @Section(title = "My Section")
            interface MySection {

            }

            @Widget(title = "", description = "")
            String m_mySetting;

            @Widget(title = "", description = "")
            @Layout(MySection.class)
            String m_otherSetting;

            @Layout(MySection.class)
            SelfContainedLayoutWithFieldsWithoutLayout m_selfContainedLayoutWithLayout =
                new SelfContainedLayoutWithFieldsWithoutLayout();
        }

        final var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(4);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("selfContainedLayout",
            "fieldWithoutLayout");
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[1].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[2].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("mySetting");
        assertThatJson(response).inPath("$.elements[3].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[3].label").isString().isEqualTo("My Section");
        assertThatJson(response).inPath("$.elements[3].elements").isArray().hasSize(3);
        assertThatJson(response).inPath("$.elements[3].elements[0].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[3].elements[0].scope").isString().contains("otherSetting");
        assertThatJson(response).inPath("$.elements[3].elements[1].type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[3].elements[1].scope").isString()
            .contains("selfContainedLayoutWithLayout", "fieldWithoutLayout");
        assertThatJson(response).inPath("$.elements[3].elements[2].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[3].elements[2].elements").isArray().hasSize(2);
    }

    @Inside(FirstSection.class)
    static class NoLongerSelfContainedBecauseOfInside implements WidgetGroup {

        @HorizontalLayout
        interface MyHorizontalLayout {

        }

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field;

    }

    @Before(FirstSection.WithinFirstSection.class)
    static class NoLongerSelfContainedBecauseOfBefore implements WidgetGroup {

        @HorizontalLayout
        interface MyHorizontalLayout {

        }

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field;

    }

    @After(FirstSection.WithinFirstSection.class)
    static class NoLongerSelfContainedBecauseOfAfter implements WidgetGroup {

        @HorizontalLayout
        interface MyHorizontalLayout {

        }

        @Widget(title = "", description = "")
        @Layout(MyHorizontalLayout.class)
        String m_field;

    }

    abstract static class NoLongerSelfContainedTestSettings<T extends WidgetGroup> implements DefaultNodeSettings {

        T m_selfContainedLayout1;

        T m_selfContainedLayout2;
    }

    static class NoLongerSelfContainedTestSettingsWithInside
        extends NoLongerSelfContainedTestSettings<NoLongerSelfContainedBecauseOfInside> {
    }

    static class NoLongerSelfContainedTestSettingsWithAfter
        extends NoLongerSelfContainedTestSettings<NoLongerSelfContainedBecauseOfAfter> {
    }

    static class NoLongerSelfContainedTestSettingsWithBefore
        extends NoLongerSelfContainedTestSettings<NoLongerSelfContainedBecauseOfBefore> {
    }

    static Stream<Class<? extends NoLongerSelfContainedTestSettings<?>>> provideNonSelfContainedLayouts() {
        return Stream.of(//
            NoLongerSelfContainedTestSettingsWithInside.class, //
            NoLongerSelfContainedTestSettingsWithAfter.class, //
            NoLongerSelfContainedTestSettingsWithBefore.class //
        );
    }

    @ParameterizedTest
    @MethodSource("provideNonSelfContainedLayouts")
    void testTypeAnnotationBreaksSelfContaindnessOfWidgetGroups(
        final Class<? extends NoLongerSelfContainedTestSettings<?>> testSettings) {

        final var response = buildTestUiSchema(testSettings);

        assertThatJson(response).inPath("$.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        /**
         * If the layout would be treated as self-contained, there would be two horizontal layouts with one field each.
         * Instead there is one horizontal layout with two fields each
         */
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].elements[0].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[0].elements[0].elements").isArray().hasSize(2);

    }

    @Inside(FirstSection.class)
    interface SuperclassAnnotationTestLayout {

        @Before(CenterLayout.class)
        interface BeforeCenterLayout {
        }

        class CenterLayout implements PersistableSettings, WidgetGroup {
            @HorizontalLayout()
            interface CenterLayoutInnerLayout {
            }

            @Widget(title = "", description = "")
            @Layout(CenterLayoutInnerLayout.class)
            String centerLayoutElement1;

            @Widget(title = "", description = "")
            @Layout(CenterLayoutInnerLayout.class)
            String centerLayoutElement2;

            @Widget(title = "", description = "")
            @Layout(BeforeCenterLayout.class)
            String centerLayoutElement3;
        }

        @After(CenterLayout.class)
        interface AfterCenterLayout {
        }
    }

    /**
     * Tests that it is possible to define layouts and there relation to each other independent from the concrete
     * settings implementation. Without the @Inside annotation, the relation of these parts and the SecondSection used
     * in the settings would be unclear
     */
    @Test
    void testAbstractLayoutDefinitionWithInsideAnnotation() {

        class TestSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @Layout(BeforeCenterLayout.class)
            int intBeforeCenterLayout;

            CenterLayout secondSection = new CenterLayout();

            @Widget(title = "", description = "")
            @Layout(AfterCenterLayout.class)
            String stringAfterCenterLayout;

            @Widget(title = "", description = "")
            @Layout(SecondSection.class)
            String stringInSecondSection;
        }

        final var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("First");
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(4);
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString().contains("intBeforeCenterLayout");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString().contains("centerLayoutElement3");
        assertThatJson(response).inPath("$.elements[0].elements[2].type").isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[0].elements[2].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[0].elements[2].elements[0].scope").isString()
            .contains("centerLayoutElement1");
        assertThatJson(response).inPath("$.elements[0].elements[2].elements[1].scope").isString()
            .contains("centerLayoutElement2");
        assertThatJson(response).inPath("$.elements[0].elements[3].scope").isString()
            .contains("stringAfterCenterLayout");

        assertThatJson(response).inPath("$.elements[1].label").isString().isEqualTo("Second");
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString().contains("stringInSecondSection");

    }
}
