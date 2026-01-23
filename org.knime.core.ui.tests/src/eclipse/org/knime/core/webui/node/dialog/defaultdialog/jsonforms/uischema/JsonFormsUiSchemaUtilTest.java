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
 *   Mar 21, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.layout.CheckboxesWithVennDiagram;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.layout.VerticalLayout;
import org.knime.node.parameters.updates.StateProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class JsonFormsUiSchemaUtilTest {

    static ObjectNode buildUiSchema(final Map<SettingsType, Class<? extends WidgetGroup>> settings) {
        return buildUiSchema(settings, null);
    }

    static ObjectNode buildUiSchema(final Map<SettingsType, Class<? extends WidgetGroup>> settings,
        final NodeParametersInput context) {
        return JsonFormsUiSchemaUtil.buildUISchema(settings.entrySet().stream()
            .map(e -> new WidgetTreeFactory().createTree(e.getValue(), e.getKey())).toList(), context);
    }

    @SuppressWarnings("javadoc")
    public static ObjectNode buildTestUiSchema(final Class<? extends NodeParameters> settingsClass) {
        return buildUiSchema(Map.of(SettingsType.MODEL, settingsClass));
    }

    @SuppressWarnings("javadoc")
    public static ObjectNode buildTestUiSchema(final Class<? extends NodeParameters> settingsClass,
        final NodeParametersInput context) {
        return buildUiSchema(Map.of(SettingsType.MODEL, settingsClass), context);
    }

    interface TestSettingsLayout {
        @Section(title = "Section1")
        interface Section1 {
        }

        @Section(title = "Test section title", description = "Test section description")
        @Advanced
        interface Section2 {
            @Section(title = "Nested section title")
            interface NestedSection {
            }
        }
    }

    @Layout(TestSettingsLayout.class)
    class DummySettings implements NodeParameters {

        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section1.class)
        String m_setting1;

        @Widget(title = "", description = "")
        @Layout(TestSettingsLayout.Section2.NestedSection.class)
        String m_setting2;
    }

    @Test
    void testSection() throws JsonProcessingException {
        final var response = buildTestUiSchema(DummySettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("Section1");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("description");
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[1].label").isString().isEqualTo("Test section title");
        assertThatJson(response).inPath("$.elements[1].description").isString().isEqualTo("Test section description");
        assertThatJson(response).inPath("$.elements[1].options.isAdvanced").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].elements[0].type").isString().isEqualTo("Section");
        assertThatJson(response).inPath("$.elements[1].elements[0].label").isString().isEqualTo("Nested section title");
    }

    class ClusterOfSettings implements WidgetGroup {
        @Widget(title = "", description = "")
        String m_sub1;

        @Widget(title = "", description = "")
        String m_sub2;
    }

    class ControlSetting {
        String m_sub1;

        String m_sub2;
    }

    class TestControlSettings implements NodeParameters {
        @Widget(title = "", description = "")
        String m_normalSetting;

        ClusterOfSettings m_settingWithNestedUiElements;

        @Widget(title = "", description = "")
        ControlSetting m_customSetting;
    }

    @Test
    void testAddsControlsWithCorrectScope() throws JsonProcessingException {
        final var response = buildTestUiSchema(TestControlSettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(4);
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/normalSetting");
        assertThatJson(response).inPath("$.elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/settingWithNestedUiElements/properties/sub1");
        assertThatJson(response).inPath("$.elements[2].scope").isString()
            .isEqualTo("#/properties/model/properties/settingWithNestedUiElements/properties/sub2");
        assertThatJson(response).inPath("$.elements[3].scope").isString()
            .isEqualTo("#/properties/model/properties/customSetting");
    }

    @Test
    void testHiddenSettings() throws JsonProcessingException {
        @SuppressWarnings("unused")
        class TestHiddenSettings implements NodeParameters {
            @Widget(title = "", description = "")
            String m_normalSetting;

            String m_hiddenSetting;

        }
        final var response = buildTestUiSchema(TestHiddenSettings.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/normalSetting");
    }

    @Section(title = "Side Drawer 1", sideDrawer = true)
    interface SideDrawer1 {

    }

    @Section(title = "Side Drawer 2", sideDrawer = true, sideDrawerSetText = SideDrawer2.SET_TEXT,
        description = SideDrawer2.DESCRIPTION)
    @After(SideDrawer1.class)
    interface SideDrawer2 {

        String DESCRIPTION = "Side drawer description";

        String SET_TEXT = "Set settings";

    }

    class SideDrawerSettings implements NodeParameters {

        @Layout(SideDrawer1.class)
        @Widget(title = "", description = "")
        String m_setting1;

        @Layout(SideDrawer2.class)
        @Widget(title = "", description = "")
        String m_setting2;

    }

    @Test
    void testSideDrawer() {

        final var response = buildTestUiSchema(SideDrawerSettings.class);

        // Test that we have 2 side drawer sections
        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);

        // Test first side drawer (basic configuration)
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("SettingsSubPanelLayout");
        assertThatJson(response).inPath("$.elements[0].label").isString().isEqualTo("Side Drawer 1");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("description");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("options");
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/setting1");

        // Test second side drawer (with setText and description)
        assertThatJson(response).inPath("$.elements[1].type").isString().isEqualTo("SettingsSubPanelLayout");
        assertThatJson(response).inPath("$.elements[1].label").isString().isEqualTo("Side Drawer 2");
        assertThatJson(response).inPath("$.elements[1].description").isString().isEqualTo(SideDrawer2.DESCRIPTION);
        assertThatJson(response).inPath("$.elements[1].options.setText").isString().isEqualTo(SideDrawer2.SET_TEXT);
        assertThatJson(response).inPath("$.elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/setting2");

    }

    @Test
    void testHorizontalLayout() {
        interface TestHorizontalLayout {

            @HorizontalLayout
            interface HorizontalGroup {
            }
        }

        class TestHorizontalLayoutSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @Layout(TestHorizontalLayout.HorizontalGroup.class)
            String m_setting1;

            @Widget(title = "", description = "")
            @Layout(TestHorizontalLayout.HorizontalGroup.class)
            String m_setting2;
        }

        final var response = buildTestUiSchema(TestHorizontalLayoutSettings.class);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("HorizontalLayout");
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/setting1");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/setting2");
    }

    @Test
    void testVerticalLayout() {

        class TestVerticalLayoutSettings implements NodeParameters {

            //   [ ] Use hour   [ ] Use minute
            //   Hours          Minutes
            //   12             45
            @HorizontalLayout
            interface HoursMinutes {

                @VerticalLayout
                interface Hours {
                }

                @After(Hours.class)
                @VerticalLayout
                interface Minutes {
                }
            }

            @Layout(HoursMinutes.Hours.class)
            @Widget(title = "Use hour", description = "")
            boolean m_useHour;

            @Layout(HoursMinutes.Hours.class)
            @Widget(title = "Hours", description = "")
            int m_hour = 0;

            @Layout(HoursMinutes.Minutes.class)
            @Widget(title = "Use minute", description = "")
            boolean m_useMinute;

            @Layout(HoursMinutes.Minutes.class)
            @Widget(title = "Minutes", description = "")
            int m_minute = 0;
        }

        final var response = buildTestUiSchema(TestVerticalLayoutSettings.class);

        // Assertions for the outer structure
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("HorizontalLayout");

        // Assertions for the first VerticalLayout
        assertThatJson(response).inPath("$.elements[0].elements[0].type").isString().isEqualTo("VerticalLayout");
        assertThatJson(response).inPath("$.elements[0].elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/useHour");
        assertThatJson(response).inPath("$.elements[0].elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/hour");

        // Assertions for the second VerticalLayout
        assertThatJson(response).inPath("$.elements[0].elements[1].type").isString().isEqualTo("VerticalLayout");
        assertThatJson(response).inPath("$.elements[0].elements[1].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/useMinute");
        assertThatJson(response).inPath("$.elements[0].elements[1].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/minute");
    }

    @Test
    void testVennDiagram() {
        interface TestVennDiagramLayout {

            @CheckboxesWithVennDiagram
            interface Venn {
            }
        }

        class TestVennDiagramSettings implements NodeParameters {
            @Widget(title = "", description = "")
            @Layout(TestVennDiagramLayout.Venn.class)
            String m_inner;

            @Widget(title = "", description = "")
            @Layout(TestVennDiagramLayout.Venn.class)
            String m_left;

            @Widget(title = "", description = "")
            @Layout(TestVennDiagramLayout.Venn.class)
            String m_right;
        }

        final var response = buildTestUiSchema(TestVennDiagramSettings.class);

        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("VennDiagram");
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/inner");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/left");
        assertThatJson(response).inPath("$.elements[0].elements[2].scope").isString()
            .isEqualTo("#/properties/model/properties/right");
    }

    @Test
    void testParametersWithSubParameters() {

        class TestParametersWithSubParametersSettigns implements NodeParameters {

            interface SubParamsRoot {

                interface InsideSubParamsRoot {
                }
            }

            @SubParameters(subLayoutRoot = SubParamsRoot.class)
            @Widget(title = "Setting with sub parameters always visible",
                description = "The sub parameters of this setting are always visible below this setting.")
            String m_withSubParamsAlways;

            @SubParameters(subLayoutRoot = SubParamsRoot.class, showSubParametersProvider = ShowSubParamsProvider.class)
            @Widget(title = "Setting with sub parameters sometimes visible",
                description = "The sub parameters of this setting are only visible when the checkbox is checked.")
            String m_withSubParamsSometimes;

            static final class ShowSubParamsProvider implements StateProvider<Boolean> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    throw new UnsupportedOperationException("should not be called in this test");
                }

                @Override
                public Boolean computeState(final NodeParametersInput parametersInput)
                    throws StateComputationFailureException {
                    throw new UnsupportedOperationException("should not be called in this test");
                }

            }

            @Layout(SubParamsRoot.class)
            @Widget(title = "Setting1", description = "")
            String m_subSetting1;

            @Layout(SubParamsRoot.InsideSubParamsRoot.class)
            @Widget(title = "Setting2", description = "")
            boolean m_subSetting2;

        }

        final var response = buildTestUiSchema(TestParametersWithSubParametersSettigns.class);
        assertThatJson(response).inPath("$.elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[0].type").isString().isEqualTo("ControlWithSubParameters");
        assertThatJson(response).inPath("$.elements[0].control.type").isString().isEqualTo("Control");
        assertThatJson(response).inPath("$.elements[0].control.scope").isString()
            .isEqualTo("#/properties/model/properties/withSubParamsAlways");
        assertThatJson(response).inPath("$.elements[0].control").isObject().doesNotContainKey("providedOptions");
        assertThatJson(response).inPath("$.elements[0].elements").isArray().hasSize(2);
        assertThatJson(response).inPath("$.elements[0].elements[0].scope").isString()
            .isEqualTo("#/properties/model/properties/subSetting1");
        assertThatJson(response).inPath("$.elements[0].elements[1].scope").isString()
            .isEqualTo("#/properties/model/properties/subSetting2");

        assertThatJson(response).inPath("$.elements[1].control.providedOptions").isArray()
            .isEqualTo(new String[]{"showSubParameters"});

    }

}