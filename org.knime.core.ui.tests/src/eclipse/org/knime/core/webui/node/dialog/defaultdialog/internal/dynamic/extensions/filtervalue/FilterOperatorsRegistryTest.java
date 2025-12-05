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
 *   Dec 8, 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.TestFilterOperators.TestEqOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;

/**
 * Tests for {@link FilterOperatorsRegistry} extension point registration.
 *
 * <p>
 * <b>Note on ID scoping:</b> The registry applies different ID transformation rules based on the extension's namespace:
 * </p>
 * <ul>
 * <li><b>KNIME internal extensions</b> (namespace starts with "org.knime." or "com.knime."): Operator IDs are used
 * as-is without transformation</li>
 * <li><b>Third-party extensions</b> (any other namespace): Operator IDs are transformed by converting to
 * uppercase and prefixing with "EXT_". For example, "myOperator" becomes "EXT_MYOPERATOR"</li>
 * </ul>
 * <p>
 * Since this test is in the org.knime.core.ui.tests bundle (KNIME internal), all operators registered here will be
 * treated as KNIME internal and will NOT receive the "EXT_" prefix. To test external operator behavior, we provide
 * the registry with custom lookup logic.
 * </p>
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class FilterOperatorsRegistryTest {

    @SuppressWarnings("serial")
    static class TestCell extends DataCell {
        static final DataType TYPE = DataType.getType(TestCell.class);

        @Override
        public String toString() {
            return "TestCell";
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            throw new UnsupportedOperationException("Not expected to be called in this test");
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Not expected to be called in this test");
        }

    }

    /**
     * Tests that the test operator registered via the extension point is properly loaded and has the expected ID
     * for a KNIME internal extension.
     *
     * <p>
     * KNIME internal extensions (org.knime.* or com.knime.*) should have their operator IDs preserved without the
     * "EXT_" prefix transformation.
     * </p>
     */
    @SuppressWarnings("static-method")
    @Test
    void testInternalExtensionPointRegistration() {
        final var registry = FilterOperatorsRegistry.getInstance();
        assertNotNull(registry, "Registry instance should not be null");

        final List<FilterOperator<FilterValueParameters>> operators = registry.getOperators(TestCell.TYPE);
        assertNotNull(operators, "Operators list should not be null");
        assertFalse(operators.isEmpty(), "Should have at least one operator registered for TestCell type");

        final var testOperatorOpt = operators.stream()
            .filter(op -> TestFilterOperators.TestOperator.OPERATOR_ID.equals(op.getId())).findFirst();

        assertTrue(testOperatorOpt.isPresent(),
            "Test operator with ID '" + TestFilterOperators.TestOperator.OPERATOR_ID + "' should be registered");

        final var testOperator = testOperatorOpt.get();
        assertEquals(TestFilterOperators.TestOperator.OPERATOR_ID, testOperator.getId(),
            "Operator ID should match expected value (without EXT_ prefix for KNIME internal extensions)");
        assertEquals(TestFilterOperators.TestParameters.class, testOperator.getNodeParametersClass(),
            "Operator should use the expected parameters class");

        assertEquals(TestFilterOperators.TestOperator.class, testOperator.getClass(),
            "Implementation class should match (wrapped for external operators)");
    }

    /**
     * Tests that the registry's parameter classes collection includes our test parameters.
     */
    @SuppressWarnings("static-method")
    @Test
    void testParameterClassesIncludesTestParameters() {
        final var registry = FilterOperatorsRegistry.getInstance();
        final var parameterClasses = registry.getAllParameterClasses();

        assertNotNull(parameterClasses, "Parameter classes list should not be null");
        assertTrue(parameterClasses.contains(TestFilterOperators.TestParameters.class),
            "Parameter classes should include TestParameters");
    }

    /**
     * Tests that the test operator registered via the extension point has a scoped ID and is treated as external,
     * since the registry in this test method uses custom lookup logic.
     */
    @SuppressWarnings("static-method")
    @Test
    void testExternalExtensionPointRegistration() {
        // treat test operator/extension as external for this test
        final Predicate<String> isInternal = identifier -> !identifier.contains("ui")
            && (identifier.startsWith("org.knime.") || identifier.startsWith("com.knime."));

        // mock the type namespace lookup to return the plugin id for the data type
        final Function<DataType, Optional<String>> dataTypeToNamespace =
            dt -> Optional.ofNullable(TestCell.TYPE.equals(dt) ? "org.knime.core.ui" : null);

        // test without setting the feature flag
        final var registryOne = new FilterOperatorsRegistry(false, isInternal, dataTypeToNamespace);
        final var testOperatorsOne = registryOne.getOperators(TestCell.TYPE);
        assertTrue(testOperatorsOne.isEmpty(), "No external operators registered, since feature flag is not enabled");

        // now enable feature flag and proceed with test
        final var registry = new FilterOperatorsRegistry(true, isInternal, dataTypeToNamespace);
        final var testOperators = registry.getOperators(TestCell.TYPE);
        assertFalse(testOperators.isEmpty(), "Should have at least one test operator registered for TestCell type");

        final var testOperatorOpt = testOperators.stream()
            // third-party operators are wrapped
            .filter(op -> op.getId().toUpperCase(Locale.US)
                .endsWith(TestFilterOperators.TestOperator.OPERATOR_ID.toUpperCase(Locale.US)))
            .findFirst();
        assertTrue(testOperatorOpt.isPresent(),
            "Test operator with ID '" + TestFilterOperators.TestOperator.OPERATOR_ID + "' should be registered");
        assertNotEquals(TestFilterOperators.TestOperator.OPERATOR_ID, testOperatorOpt.get().getId(),
            "External operator ID should be transformed");
    }

    /**
     * Tests that an external operator that overrides a built-in operator is not wrapped in order to not lose the
     * built-in's interface.
     */
    @SuppressWarnings("static-method")
    @Test
    void testExternalBuiltInOverride() {
        // setup needed to emulate external extension
        final Predicate<String> isInternal = identifier -> !identifier.contains("ui")
            && (identifier.startsWith("org.knime.") || identifier.startsWith("com.knime."));
        final Function<DataType, Optional<String>> dataTypeToNamespace =
            dt -> Optional.ofNullable(TestCell.TYPE.equals(dt) ? "org.knime.core.ui" : null);
        final var registry = new FilterOperatorsRegistry(true, isInternal, dataTypeToNamespace);

        final var testOperators = registry.getOperators(TestCell.TYPE);
        assertFalse(testOperators.isEmpty(), "Should have at least one test operator registered for TestCell type");

        final var overrideOperatorOpt = testOperators.stream() //
            // ID for override is not wrapped
            .filter(op -> EqualsOperator.ID.equals(op.getId())) //
            .findFirst();
        assertTrue(overrideOperatorOpt.isPresent(), "TestEqOperator should be registered");
        assertEquals(TestEqOperator.class, overrideOperatorOpt.get().getClass(),
            "Override operator should not be wrapped and retain its original class");
    }
}
