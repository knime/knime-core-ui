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
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.example;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
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
import org.knime.node.parameters.updates.ValueReference;

/**
 *
 * Use {@link DynamicParameters} whenver parts of a dialog need to dynamically depend on either the context or other
 * parameters in a way that cannot even be described statically by effects.
 *
 * This is particularly useful when showing parts of a dialog that are defined within an extension point (e.g. the
 * parameters for creating a data cell for a certain data type which depend on another parameter that let's the user
 * choose a column).
 *
 * The following example shows an implementation where two kinds of concrete parameters are dynamically switched
 * whenever the user clicks on a button.
 */
final class NodeParametersWithDynamicParameters implements NodeParameters {

    /**
     * Define an interface and define common behavior. This way the node model does not need to know about each concrete
     * implementation but can access the methods defined in this interface.
     */
    interface DynamicParametersWithCommonBehavior extends DynamicNodeParameters {

        String getOutput();

    }

    /**
     * These settings might be defined somewhere entirely else (e.g. in an extension).
     */
    static final class StringBasedConcreteSettings implements DynamicParametersWithCommonBehavior {

        @Widget(title = "Input string", description = "...")
        String m_textSetting;

        @Override
        public String getOutput() {
            return "string: " + m_textSetting;
        }

    }

    /**
     * These settings might be defined somewhere entirely else (e.g. in an extension).
     */
    static final class NumberBasedConcreteSettings implements DynamicParametersWithCommonBehavior {

        @Widget(title = "Input number", description = "...")
        int m_numberSetting;

        @Override
        public String getOutput() {
            return "integer: " + m_numberSetting;
        }

    }

    static class TriggerButton implements ButtonReference {

    }

    @Widget(title = "Click to fetch new dynamic settings", description = "...")
    @SimpleButtonWidget(ref = TriggerButton.class)
    Void m_triggerButton;

    static class SelfReference implements ParameterReference<DynamicParametersWithCommonBehavior> {

    }

    @DynamicParameters(MyDynamicParametersProvider.class)
    @ValueReference(SelfReference.class)
    DynamicParametersWithCommonBehavior m_dynamicParameters;

    static final class MyDynamicParametersProvider
        implements DynamicParametersProvider<DynamicParametersWithCommonBehavior> {

        private Supplier<DynamicParametersWithCommonBehavior> m_valueSupplier;

        static final List<Class<? extends DynamicParametersWithCommonBehavior>> POSSIBLE_CLASSES =
            List.of(StringBasedConcreteSettings.class, NumberBasedConcreteSettings.class);

        @Override
        public ClassIdStrategy<DynamicParametersWithCommonBehavior> getClassIdStrategy() {
            return new DefaultClassIdStrategy<>(POSSIBLE_CLASSES);
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnButtonClick(TriggerButton.class);
            m_valueSupplier = initializer.getValueSupplier(SelfReference.class);

            // Also possible to further depend on other parameters here.

        }

        /**
         * Switch back and fort between the two concrete parameter groups falling back to 999 if the string cannot be
         * parsed to an integer.
         */
        @Override
        public DynamicParametersWithCommonBehavior computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            DynamicParametersWithCommonBehavior current = m_valueSupplier.get();
            if (current == null) {
                // Initial case: start with string based settings.
                return new StringBasedConcreteSettings();
            }
            if (current instanceof StringBasedConcreteSettings stringBasedCurrent) {
                final var next = new NumberBasedConcreteSettings();
                try {
                    next.m_numberSetting = Integer.valueOf(stringBasedCurrent.m_textSetting);
                } catch (NumberFormatException ex) {
                    next.m_numberSetting = 999;
                }
                return next;
            }
            if (current instanceof NumberBasedConcreteSettings numberBasedCurrent) {
                final var next = new StringBasedConcreteSettings();
                next.m_textSetting = String.valueOf(numberBasedCurrent.m_numberSetting);
                return next;
            }
            return null;

        }

    }

}
