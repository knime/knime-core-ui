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
 *   Feb 22, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DropdownRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.SectionRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.imperative.ImperativeStateProvider;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
public class UpdatesUtilImperativeUpdatesTest {

    private static NodeParametersInput createDefaultNodeSettingsContext() {
        return NodeParametersInputImpl.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
            new PortObjectSpec[]{null}, null, null);
    }

    /**
     * To be used only when not constructing initial updates (which will lead to a null pointer exception, since data
     * are not given)
     */
    static ObjectNode buildUpdates(final DialogElementRendererSpec rendererSpec) {
        return buildUpdates(List.of(rendererSpec));
    }

    /**
     * To be used only when not constructing initial updates (which will lead to a null pointer exception, since data
     * are not given)
     */
    static ObjectNode buildUpdates(final Collection<DialogElementRendererSpec> rendererSpecs) {
        return buildUpdates(rendererSpecs, null, createDefaultNodeSettingsContext());
    }

    static ObjectNode buildUpdates(final Collection<DialogElementRendererSpec> rendererSpecs,
        final ObjectNode dataJson) {
        return buildUpdates(rendererSpecs, dataJson, createDefaultNodeSettingsContext());
    }

    static ObjectNode buildUpdates(final Collection<DialogElementRendererSpec> rendererSpecs, final ObjectNode dataJson,
        final NodeParametersInput context) {
        final var objectNode = new ObjectMapper().createObjectNode();
        final var modelSettingsRendererSpec =
            rendererSpecs.stream().map(UpdatesUtilImperativeUpdatesTest::setModelSettingsType).toList();
        UpdatesUtil.addImperativeUpdates(objectNode, modelSettingsRendererSpec, dataJson, context);
        return objectNode;
    }

    /**
     * The dependency tree creation throws an illegal state exception when renderer paths don't start with "model".
     *
     * @param rendererSpec the renderer spec
     * @return the renderer spec with a model path
     */
    public static DialogElementRendererSpec setModelSettingsType(final DialogElementRendererSpec rendererSpec) {
        return rendererSpec.at("model");
    }

    interface TestImperativeStateProvider<T> extends ImperativeStateProvider<T> {

        @Override
        default T computeState(final NodeParametersInput context) throws StateComputationFailureException {
            throw new UnsupportedOperationException("Should not be called in this test");
        }

    }

    /**
     * Creates a test renderer spec which is a value dependency.
     *
     * @return a renderer that can be used as dependency in a test for imperative updates.
     */
    public static ControlValueReference<String> createTestDependencySpec() {
        return new DropdownRendererSpec() {

            @Override
            public String getTitle() {
                throw new UnsupportedOperationException("Should not be called in this test");
            }

            @Override
            public Optional<String> getDescription() {
                throw new UnsupportedOperationException("Should not be called in this test");
            }
        };

    }

    /**
     * Creates a control renderer spec with a state provider. The provided state is provided for the option name
     * "placeholder".
     *
     * @param stateProvider an imperative state provider
     * @return the control renderer spec
     */
    public static ControlRendererSpec createControlSpecWithStateProvider(final StateProvider<String> stateProvider) {
        return new TextRendererSpec() {

            @Override
            public String getTitle() {
                throw new UnsupportedOperationException("Should not be called in this test");
            }

            @Override
            public Optional<String> getDescription() {
                throw new UnsupportedOperationException("Should not be called in this test");
            }

            @Override
            public Map<String, StateProvider<?>> getStateProviders() {
                return Map.of("placeholder", stateProvider);
            }

        };
    }

    @Test
    void testUpdateAfterOpenDialog() {

        final var stateProvider = new TestImperativeStateProvider<String>() {

            @Override
            public void init(final ImperativeStateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
            }

        };

        final var updatedRendererSpec = createControlSpecWithStateProvider(stateProvider);
        final var response = buildUpdates(updatedRendererSpec);

        UpdatesUtilTest.assertAfterOpenDialogWithoutDependencies(response);
    }

    interface CombineToRenderers {
        Collection<DialogElementRendererSpec> getRendererSpecs(final DialogElementRendererSpec firstDependency,
            final DialogElementRendererSpec secondDependency, final DialogElementRendererSpec updatedRendererSpec);
    }

    static Stream<Arguments> combineToRenderers() {
        return Stream.of(//
            Arguments.of(new CombineToRenderers() {
                @Override
                public String toString() {
                    return "simple combination";
                }

                @Override
                public Collection<DialogElementRendererSpec> getRendererSpecs(
                    final DialogElementRendererSpec firstDependency, final DialogElementRendererSpec secondDependency,
                    final DialogElementRendererSpec updatedRendererSpec) {
                    return List.of(firstDependency, secondDependency, updatedRendererSpec);
                }
            }), Arguments.of(new CombineToRenderers() {
                @Override
                public String toString() {
                    return "section with updated renderer";
                }

                @Override
                public Collection<DialogElementRendererSpec> getRendererSpecs(
                    final DialogElementRendererSpec firstDependency, final DialogElementRendererSpec secondDependency,
                    final DialogElementRendererSpec updatedRendererSpec) {
                    return List.of(new SectionRendererSpec() {
                        @Override
                        public Collection<DialogElementRendererSpec> getElements() {
                            return List.of(firstDependency, updatedRendererSpec);
                        }

                        @Override
                        public String getTitle() {
                            return "Section";
                        }
                    }, secondDependency);
                }
            }));
    }

    @ParameterizedTest(name = "Global updates within {0}")
    @MethodSource("combineToRenderers")
    void testValueReferences(final CombineToRenderers combineToRenderers) {

        final var firstDropdown = createTestDependencySpec();
        final var secondDropdown = createTestDependencySpec();

        final var stateProvider = new TestImperativeStateProvider<String>() {

            @Override
            public void init(final ImperativeStateProviderInitializer initializer) {
                initializer.computeFromValueSupplier(firstDropdown);
                initializer.getValueSupplier(secondDropdown);
            }

        };

        final var updatedRendererSpec = createControlSpecWithStateProvider(stateProvider);
        final var renderers = combineToRenderers.getRendererSpecs(firstDropdown.at("first"),
            secondDropdown.at("second"), updatedRendererSpec.at("updated"));
        final var response = buildUpdates(renderers);

        assertThatJson(response).inPath("$").isObject().doesNotContainKey("initialUpdates");
        assertThatJson(response).inPath("$.globalUpdates").isArray().hasSize(1);

        assertThatJson(response).inPath("$.globalUpdates[0].trigger").isObject().doesNotContainKey("id");
        assertThatJson(response).inPath("$.globalUpdates[0].trigger.scope").isString()
            .isEqualTo("#/properties/model/properties/first");
        assertThatJson(response).inPath("$.globalUpdates[0]").isObject().doesNotContainKey("triggerInitially");
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies").isArray().hasSize(2);
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies[0]").isString()
            .isEqualTo("#/properties/model/properties/first");
        assertThatJson(response).inPath("$.globalUpdates[0].dependencies[1]").isString()
            .isEqualTo("#/properties/model/properties/second");
    }

    static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    @ParameterizedTest(name = "Initial updates within {0}")
    @MethodSource("combineToRenderers")
    void testInitialUpdates(final CombineToRenderers combineToRenderers) {

        final var firstDependency = createTestDependencySpec();
        final var secondDependency = createTestDependencySpec();

        final var stateProvider = new ImperativeStateProvider<String>() {

            private Supplier<String> m_firstDependency;

            private Supplier<String> m_secondDependency;

            @Override
            public void init(final ImperativeStateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_firstDependency = initializer.getValueSupplier(firstDependency);
                m_secondDependency = initializer.getValueSupplier(secondDependency);

            }

            @Override
            public String computeState(final NodeParametersInput context)
                throws StateComputationFailureException {
                return m_firstDependency.get() + "/" + m_secondDependency.get();
            }

        };

        final var firstDependencyKey = "first";
        final var secondDependencyKey = "second";
        final var updatedRendererKey = "updated";

        final var firstRendererSpec = firstDependency.at(firstDependencyKey);
        final var secondRendererSpec = secondDependency.at(secondDependencyKey);
        final var updatedRendererSpec = createControlSpecWithStateProvider(stateProvider).at(updatedRendererKey);

        final var jsonData = JsonNodeFactory.instance.objectNode();
        jsonData.putObject("model").put(firstDependencyKey, "first").put(secondDependencyKey, "second");

        final var renderers =
            combineToRenderers.getRendererSpecs(firstRendererSpec, secondRendererSpec, updatedRendererSpec);

        final var response = buildUpdates(renderers, jsonData);

        assertThatJson(response).inPath("$").isObject().doesNotContainKey("globalUpdates");
        assertThatJson(response).inPath("$.initialUpdates").isArray().hasSize(1);

        assertThatJson(response).inPath("$.initialUpdates[0].scope").isString()
            .isEqualTo("#/properties/model/properties/" + updatedRendererKey);
        assertThatJson(response).inPath("$.initialUpdates[0].providedOptionName").isString().isEqualTo("placeholder");
        assertThatJson(response).inPath("$.initialUpdates[0].values[0].value").isString().isEqualTo("first/second");
    }

}
