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
 *   Aug 27, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.data.DataServiceContextTest;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DataAndDialog;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;

/**
 * Tests for custom deserialization of dynamic parameters during update calls. This test verifies that the
 * DynamicNodeParametersDeserializer correctly handles JSON data with "@class" properties during update flows.
 *
 * @author Paul Bärnreuther
 */
class DynamicParametersSimulationTest {

    final static PortObjectSpec[] PORT_OBJECT_SPECS = new PortObjectSpec[0];

    @BeforeAll
    static void initDataServiceContext() {
        DataServiceContextTest.initDataServiceContext(null, () -> PORT_OBJECT_SPECS);
    }

    @AfterAll
    static void removeDataServiceContext() {
        DataServiceContextTest.removeDataServiceContext();
    }

    private static NodeParametersInput createNodeParametersInput() {
        return NodeParametersInputImpl.createDefaultNodeSettingsContext(null, null, null, null);
    }

    interface TestDynamicParams extends DynamicNodeParameters {
    }

    static class TestStringImpl implements TestDynamicParams {
        @Widget(title = "Test String", description = "A test string field")
        public String m_testString = "123";
    }

    static class TestIntegerImpl implements TestDynamicParams {
        @Widget(title = "Test Integer", description = "A test integer field")
        public int m_testInteger = 456;
    }

    static class TestDynamicParametersProvider implements DynamicParametersProvider<TestDynamicParams> {

        private Supplier<TestDynamicParams> m_valueSupplier;

        static final List<Class<? extends TestDynamicParams>> SUPPORTED_CLASSES =
            List.of(TestStringImpl.class, TestIntegerImpl.class);

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            initializer.computeOnButtonClick(TriggerButton.class);
            m_valueSupplier = initializer.getValueSupplier(SelfReference.class);
        }

        @Override
        public ClassIdStrategy<TestDynamicParams> getClassIdStrategy() {
            return new DefaultClassIdStrategy<>(SUPPORTED_CLASSES);
        }

        @Override
        public TestDynamicParams computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var current = m_valueSupplier.get();

            if (current instanceof TestStringImpl currentString) {
                final var newImpl = new TestIntegerImpl();
                try {
                    newImpl.m_testInteger = Integer.parseInt(currentString.m_testString);
                } catch (final NumberFormatException e) {
                    newImpl.m_testInteger = 999;
                }
                return newImpl;
            }

            if (current instanceof TestIntegerImpl currentInteger) {
                final var newImpl = new TestStringImpl();
                newImpl.m_testString = String.valueOf(currentInteger.m_testInteger);
                return newImpl;
            }

            throw new StateComputationFailureException("Unknown dynamic parameters class: " + current.getClass());
        }
    }

    static class SelfReference implements ParameterReference<TestDynamicParams> {
    }

    static class TriggerButton implements ButtonReference {
    }

    static class PlusOneButton implements ButtonReference {
    }

    static class TestDynamicSettings implements NodeParameters {

        @Widget(title = "Trigger Update", description = "Press to trigger dynamic parameters update")
        @SimpleButtonWidget(ref = TriggerButton.class)
        Void m_triggerButton;

        @Widget(title = "Click to add 1 or '1'",
            description = "Triggers a value update of the present dynamic settings without switching to other dynamic settings.")
        @SimpleButtonWidget(ref = PlusOneButton.class)
        Void m_plusOneButton;

        @DynamicParameters(TestDynamicParametersProvider.class)
        @ValueProvider(TestDynamicParametersValueProvider.class)
        @ValueReference(SelfReference.class)
        TestDynamicParams m_dynamicParameters = new TestStringImpl();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDynamicParametersDeserializationDuringUpdate() {
        final var settings = new TestDynamicSettings();
        final var context = createNodeParametersInput();
        final var simulator = new DialogUpdateSimulator(settings, context);

        final var initialResult = simulator.simulateAfterOpenDialog();

        final var initialUpdate = initialResult.getUiStateUpdateAt(List.of("dynamicParameters"), "dynamicSettings");
        assertThat(initialUpdate).isNotNull();
        assertThat(initialUpdate).isInstanceOf(DataAndDialog.class);
        assertThatJson(((DataAndDialog<Object>)initialUpdate).getData()).inPath("$.testInteger").isNumber()
            .isEqualTo(BigDecimal.valueOf(123));

        // Update the settings as if they were changed by the update above
        final var initialUpdateResult = new TestIntegerImpl();
        initialUpdateResult.m_testInteger = 123;
        settings.m_dynamicParameters = initialUpdateResult;

        final var buttonResult = simulator.simulateButtonClick(TriggerButton.class);

        final var buttonUpdate = buttonResult.getUiStateUpdateAt(List.of("dynamicParameters"), "dynamicSettings");
        assertThat(buttonUpdate).isNotNull();

        assertThat(buttonUpdate).isInstanceOf(DataAndDialog.class);
        assertThatJson(((DataAndDialog<Object>)buttonUpdate).getData()).inPath("$.testString").isString()
            .isEqualTo("123");

    }

    @SuppressWarnings("unchecked")
    @Test
    void testParsingFallbackToDefault() {
        final var settings = new TestDynamicSettings();
        settings.m_dynamicParameters = new TestStringImpl();
        ((TestStringImpl)settings.m_dynamicParameters).m_testString = "not_a_number";

        final var context = createNodeParametersInput();
        final var simulator = new DialogUpdateSimulator(settings, context);

        final var result = simulator.simulateAfterOpenDialog();

        final var update = result.getUiStateUpdateAt(List.of("dynamicParameters"), "dynamicSettings");
        assertThat(update).isInstanceOf(DataAndDialog.class);
        assertThatJson(((DataAndDialog<Object>)update).getData()).inPath("$.testInteger").isNumber()
            .isEqualTo(BigDecimal.valueOf(999));
    }

    static final class TestDynamicParametersValueProvider implements StateProvider<TestDynamicParams> {

        private Supplier<TestDynamicParams> m_valueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(PlusOneButton.class);
            m_valueSupplier = initializer.getValueSupplier(SelfReference.class);
        }

        @Override
        public TestDynamicParams computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var currentValue = m_valueSupplier.get();
            if (currentValue == null) {
                return null;
            }
            if (currentValue instanceof TestStringImpl str) {
                str.m_testString += "1";
                return str;
            }
            if (currentValue instanceof TestIntegerImpl integ) {
                integ.m_testInteger += 1;
                return integ;
            }
            throw new IllegalStateException(
                String.format("Illegal value class %s", currentValue.getClass().getSimpleName()));

        }

    }

    @Test
    void testValueProviderOnDynamicParameters() {
        final var settings = new TestDynamicSettings();
        settings.m_dynamicParameters = new TestStringImpl();
        ((TestStringImpl)settings.m_dynamicParameters).m_testString = "100";
        final var context = createNodeParametersInput();
        final var simulator = new DialogUpdateSimulator(settings, context);

        final var result = simulator.simulateButtonClick(PlusOneButton.class);

        final var update = (TestStringImpl)result.getValueUpdateAt("dynamicParameters");
        assertThat(update.m_testString).isEqualTo("1001");
    }
}