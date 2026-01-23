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
 *   Jan 19, 2026 (KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.extension;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtilTest.buildUpdates;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.RpcDataService;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.NodeDialogManagerTest;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialogUIExtension;
import org.knime.core.webui.node.dialog.defaultdialog.internal.extension.TestDefaultNodeDialogWidget.TestStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.extension.TestDefaultNodeDialogWidget.TestWidgetAnnotation;
import org.knime.core.webui.node.dialog.defaultdialog.internal.extension.TestDefaultNodeDialogWidget.TestWidgetSettings;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Tests for {@link DefaultNodeDialogWidget} integration.
 *
 * @author KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"static-method", "unused"})
final class DefaultNodeDialogWidgetTest {

    private TestDefaultNodeDialogWidget m_widget;

    private Tree<WidgetGroup> m_testWidgetSettingsTree;

    private Tree<WidgetGroup> m_otherSettingsTree;

    private WorkflowManager m_wfm;

    /**
     * Settings class containing the test widget settings.
     */
    static final class SettingsWithTestWidget implements WidgetGroup {
        TestWidgetSettings m_testWidget = new TestWidgetSettings();
    }

    /**
     * Settings class not containing test widget settings.
     */
    static final class OtherSettings implements WidgetGroup {
        String m_someField = "value";
    }

    @BeforeEach
    void setUp() throws IOException {
        m_widget = new TestDefaultNodeDialogWidget();
        final var factory = new WidgetTreeFactory();
        m_testWidgetSettingsTree = factory.createTree(SettingsWithTestWidget.class);
        m_otherSettingsTree = factory.createTree(OtherSettings.class);
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    @AfterEach
    void tearDown() {
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

    /**
     * Tests that the widget is applied to matching node types by verifying the format in the generated UI schema.
     */
    @Test
    void testIsApplicableProducesCorrectFormat() {
        class TestSettings implements NodeParameters {
            TestWidgetSettings m_testWidget;
        }

        var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("testWidget");
        assertThatJson(response).inPath("$.elements[0].options.format").isString().isEqualTo("custom");
    }

    /**
     * Tests that custom options from the renderer spec are included in the generated UI schema.
     */
    @Test
    void testCustomOptionsInRendererSpec() {
        class TestSettings implements NodeParameters {
            TestWidgetSettings m_testWidget;
        }

        var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements[0].options." + TestDefaultNodeDialogWidget.TEST_CUSTOM_OPTION_KEY)
            .isString().isEqualTo(TestDefaultNodeDialogWidget.TEST_CUSTOM_OPTION_VALUE);
    }

    /**
     * Tests that annotation properties can be used to customize the options in the renderer spec.
     */
    @Test
    void testAnnotationPropertyCustomizesOptions() {
        final var expectedLabel = "My Custom Label";

        class TestSettings implements NodeParameters {
            @TestWidgetAnnotation(customLabel = expectedLabel)
            TestWidgetSettings m_testWidget;
        }

        var response = buildTestUiSchema(TestSettings.class);

        assertThatJson(response).inPath("$.elements[0].options." + TestDefaultNodeDialogWidget.TEST_CUSTOM_LABEL_KEY)
            .isString().isEqualTo(expectedLabel);
    }

    /**
     * Tests that regular fields without TestWidgetSettings do not get the test widget format.
     */
    @Test
    void testIsApplicableDoesNotApplyToOtherTypes() {
        class OtherTestSettings implements NodeParameters {
            @Widget(title = "", description = "")
            String m_someField;
        }

        var response = buildTestUiSchema(OtherTestSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("someField");
        assertThatJson(response).inPath("$.elements[0]").isObject().doesNotContainKey("options");
    }

    /**
     * Tests that the state provider from the widget annotation is picked up by the framework and is listed in the
     * providedOptions in the uischema.
     */
    @Test
    void testStateProviderProducesProvidedOptionsEntry() {
        class TestSettings implements NodeParameters {
            @TestWidgetAnnotation(testStateProvider = TestStateProvider.class)
            TestWidgetSettings m_testWidget;
        }

        var response = buildTestUiSchema(TestSettings.class);
        assertThatJson(response).inPath("$.elements[0].providedOptions").isArray().hasSize(1);
        assertThatJson(response).inPath("$.elements[0].providedOptions[0]").isString()
            .isEqualTo(TestDefaultNodeDialogWidget.TEST_PROVIDED_OPTION_KEY);

    }

    /**
     * Tests that the state provider from the widget annotation is picked up by the framework and produces initial
     * updates.
     */
    @Test
    void testStateProviderProducesInitialUpdates() {
        class TestSettings implements NodeParameters {
            @TestWidgetAnnotation(testStateProvider = TestStateProvider.class)
            TestWidgetSettings m_testWidget;
        }

        var response = buildUpdates(new TestSettings());

        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);
        assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
            .isEqualTo("#/properties/model/properties/testWidget");
        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString()
            .isEqualTo(TestDefaultNodeDialogWidget.TEST_PROVIDED_OPTION_KEY);
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString()
            .isEqualTo(TestDefaultNodeDialogWidget.TEST_STATE_VALUE);
    }

    /**
     * Settings class for the RPC test.
     */
    static final class RpcTestSettings implements NodeParameters {
        TestWidgetSettings m_testWidget;
    }

    /**
     * Tests that the extension widget's RPC data service can be called via the DefaultNodeDialog's RPC service.
     */
    @Test
    void testExtensionWidgetRpcServiceIntegration() {
        var nc = NodeDialogManagerTest.createNodeWithNodeDialog(m_wfm,
            () -> new DefaultNodeDialog(SettingsType.MODEL, RpcTestSettings.class));

        var nodeWrapper = NodeWrapper.of(nc);
        var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();

        // The service name is the fully qualified class name with dots removed
        var serviceName = TestDefaultNodeDialogWidget.class.getName().replace(".", "");
        var jsonRpcRequest = RpcDataService.jsonRpcRequest(serviceName + ".getTestValue");

        var response = dataServiceManager.callRpcDataService(nodeWrapper, jsonRpcRequest);

        assertThat(response).contains("\"result\":\"test-rpc-value\"");

        final var uischema = buildTestUiSchema(RpcTestSettings.class);
        assertThatJson(uischema).inPath("$.elements[0].options.rpcServiceName").isString().isEqualTo(serviceName);
    }

    /**
     * Tests that extension widget renderers are registered as additional resources in the page.
     */
    @Test
    void testExtensionWidgetRendererRegisteredAsPageResource() {
        var page = DefaultNodeDialogUIExtension.PAGE;

        // The resource path is "customWidget/" + fully qualified class name + ".js"
        var resourcePath = "customWidget/" + TestDefaultNodeDialogWidget.class.getName() + ".js";
        var resource = page.getResource(resourcePath);

        assertTrue(resource.isPresent(), "Custom widget renderer resource should be present in the page");

        // Verify the content matches what the widget provides
        try (var inputStream = resource.get().getInputStream()) {
            var content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestDefaultNodeDialogWidget.TEST_RENDERER_CONTENT, content,
                "Resource content should match the widget's renderer content");
        } catch (IOException e) {
            throw new AssertionError("Failed to read resource content", e);
        }
    }
}
