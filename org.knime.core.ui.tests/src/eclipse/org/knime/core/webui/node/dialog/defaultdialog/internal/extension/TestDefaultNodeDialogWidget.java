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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.JsonDataType;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Test implementation of {@link DefaultNodeDialogWidget} for testing the extension point registration.
 *
 * @author KNIME GmbH, Konstanz, Germany
 */
public final class TestDefaultNodeDialogWidget implements DefaultNodeDialogWidget {

    /**
     * Test annotation for the widget.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface TestWidgetAnnotation {
        /**
         * @return the test state provider class
         */
        Class<? extends StateProvider<?>> testStateProvider() default DefaultTestStateProvider.class;

        /**
         * @return a custom label to be used in the options
         */
        String customLabel() default "";
    }

    interface DefaultTestStateProvider extends StateProvider<String> {
    }

    static final String TEST_STATE_VALUE = "test-state-value";

    /**
     * Test state provider for the widget.
     */
    public static final class TestStateProvider implements StateProvider<String> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return TEST_STATE_VALUE;
        }
    }

    /**
     * A marker class for identifying which TreeNodes should be handled by this widget.
     */
    public static final class TestWidgetSettings implements WidgetGroup {
        // Marker class for testing
    }

    /**
     * Test RPC data service.
     */
    public static final class TestRpcDataService {
        /**
         * @return a test value
         */
        @SuppressWarnings("static-method")
        public String getTestValue() {
            return "test-rpc-value";
        }
    }

    static final String TEST_FORMAT = "testWidgetFormat";

    static final String TEST_RENDERER_CONTENT = "// Test renderer JavaScript content";

    static final String TEST_CUSTOM_OPTION_KEY = "customTestOption";

    static final String TEST_CUSTOM_OPTION_VALUE = "customTestValue";

    static final String TEST_CUSTOM_LABEL_KEY = "customLabel";

    static final String TEST_PROVIDED_OPTION_KEY = "testState";

    record TestOptions(String customTestOption, String customLabel) {
    }

    private final TestRpcDataService m_rpcDataService = new TestRpcDataService();

    @Override
    public boolean isApplicable(final TreeNode<WidgetGroup> node) {
        return node.getRawClass().equals(TestWidgetSettings.class);
    }

    @Override
    public CustomWidgetRenderer createRendererSpec(final TreeNode<WidgetGroup> node,
        final NodeParametersInput parametersInput) {
        final var testWidgetAnnotation = node.getAnnotation(TestWidgetAnnotation.class);
        final var customLabel = testWidgetAnnotation.map(TestWidgetAnnotation::customLabel).orElse("");

        var builder = new CustomWidgetRenderer.Builder(this.getClass()) //
            .withDataType(JsonDataType.OBJECT) //
            .withOption(TEST_CUSTOM_OPTION_KEY, TEST_CUSTOM_OPTION_VALUE) //
            .withOption(TEST_CUSTOM_LABEL_KEY, customLabel);
        if (testWidgetAnnotation.isPresent()) {
            builder =
                builder.withStateProvider(TEST_PROVIDED_OPTION_KEY, testWidgetAnnotation.get().testStateProvider());
        }
        return builder.build();
    }

    @Override
    public InputStream getRenderer() {
        return new ByteArrayInputStream(TEST_RENDERER_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<Object> getRpcDataService() {
        return Optional.of(m_rpcDataService);
    }

    @Override
    public AnnotationSyntax getSyntax() {
        return new AnnotationSyntax(List.of(TestWidgetAnnotation.class),
            List.of(new StateProviderSyntax<>(TEST_PROVIDED_OPTION_KEY, TestWidgetAnnotation.class,
                TestWidgetAnnotation::testStateProvider, DefaultTestStateProvider.class)));
    }
}
