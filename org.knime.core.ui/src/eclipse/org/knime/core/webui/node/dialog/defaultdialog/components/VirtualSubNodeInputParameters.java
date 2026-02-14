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
package org.knime.core.webui.node.dialog.defaultdialog.components;

import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyNameFilterPersistor;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * WebUI dialog settings for the Component Input node (VirtualSubNodeInputNodeFactory). Uses {@link StringFilter} with
 * {@link LegacyNameFilterPersistor} for backward-compatible persistence of the flow variable filter, since the legacy
 * {@code FlowVariableFilterConfiguration} is a {@code NameFilterConfiguration} without type filtering.
 *
 * @author Paul Baernreuther
 */
public final class VirtualSubNodeInputParameters implements NodeParameters {

    @Widget(title = "Input variables",
        description = "Choose variables from the workflow to be visible inside the component.")
    @Persistor(VariableFilterPersistor.class)
    @ChoicesProvider(NonGlobalFlowVariableNamesProvider.class)
    StringFilter m_variableFilter = new StringFilter();

    @Widget(title = "Add prefix to all variables", description = "Set a prefix to add to all imported variables.")
    @Persistor(VariablePrefixPersistor.class)
    @OptionalWidget(defaultProvider = VariablePrefixDefaultProvider.class)
    Optional<String> m_variablePrefix = Optional.empty();

    // backward-compat fields (not shown in dialog but persisted for the model)
    @Persist(configKey = "port-names")
    String[] m_portNames = new String[0];

    @Persist(configKey = "port-descriptions")
    String[] m_portDescriptions = new String[0];

    @Persist(configKey = "sub-node-description")
    String m_subNodeDescription = "";

    static final class VariableFilterPersistor extends LegacyNameFilterPersistor {
        VariableFilterPersistor() {
            super("variable-filter");
        }
    }

    static final class NonGlobalFlowVariableNamesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return ((NodeParametersInputImpl)context)
                .getAvailableInputFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()) //
                .values().stream() //
                .filter(v -> !v.isGlobalConstant()) //
                .map(FlowVariable::getName) //
                .toList();
        }
    }

    /**
     * Persists (as in legacy dialog) an empty value as `null` in the config.
     *
     * @author paulbaernreuther
     */
    static final class VariablePrefixPersistor implements NodeParametersPersistor<Optional<String>> {
        static final String CONFIG_KEY = "variable-prefix";

        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String prefix = settings.getString(CONFIG_KEY, null);
            return Optional.ofNullable(prefix);
        }

        @Override
        public void save(final Optional<String> param, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, param.orElse(null));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }

    static final class VariablePrefixDefaultProvider implements DefaultValueProvider<String> {

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return "outer:";
        }

    }
}
