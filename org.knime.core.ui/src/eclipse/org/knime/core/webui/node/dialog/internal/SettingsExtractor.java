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
 *   Jan 20, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.internal;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;

/**
 * Applies flow variables and extracts the settings of a node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsExtractor {

    private final NodeContainer m_nc;

    private final Set<SettingsType> m_settingsTypes;

    private final SettingsValidator m_validator;

    /**
     * Validates initial settings.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public interface SettingsValidator {
        /**
         * Validates the given settings.
         *
         * @param settings to validate
         * @throws InvalidSettingsException if the settings are invalid
         */
        void validate(final Map<SettingsType, NodeAndVariableSettingsRO> settings) throws InvalidSettingsException;
    }

    /**
     * Constructor.
     *
     * @param nodeContainer to extract the settings from
     * @param settingsTypes the node has
     * @param settingsValidator to check if the settings are valie
     */
    public SettingsExtractor(final NodeContainer nodeContainer, final Set<SettingsType> settingsTypes,
        final SettingsValidator settingsValidator) {
        m_nc = nodeContainer;
        m_settingsTypes = settingsTypes;
        m_validator = settingsValidator;
    }

    /**
     * @return the settings values after flow variable have been applied
     */
    public Map<SettingsType, NodeAndVariableSettingsRO> getSettingsOverwrittenByVariables() {
        Map<SettingsType, NodeAndVariableSettingsRO> resultSettings = new EnumMap<>(SettingsType.class);
        if (m_nc instanceof NativeNodeContainer nnc) {
            var overwrittenSettingsLoaded = false;
            for (var type : m_settingsTypes) {
                var settings = getSettingsOverwrittenByFlowVariables(type, nnc);

                final var settingsOrDefault =
                    settings.type == Result.Type.NONE ? getDefaultSettings(type) : settings.result;
                final var variableSettings = new VariableSettings(nnc.getNodeSettings(), type);
                if (settings.type == Result.Type.OVERWRITTEN && hasControllingVariables(variableSettings)) {
                    overwrittenSettingsLoaded = true;
                }
                resultSettings.put(type,
                    NodeAndVariableSettingsProxy.createROProxy(settingsOrDefault, variableSettings));
            }
            if (overwrittenSettingsLoaded) {
                revertOverridesIfInvalid(resultSettings, nnc);
            }
        }
        // else: SubNodeContainers (aka components) are ignored here since those retrieve the settings
        // from the contained configuration nodes and not from the component settings directly
        return resultSettings;
    }

    private record Result(Type type, NodeSettings result) {

        private enum Type {
                /**
                 * There exist controlling flow variables and overriding did work. In this case the {@link #result} are
                 * the overwritten settings.
                 */
                OVERWRITTEN,
                /**
                 * There either do not exist any flow variables, or the settings could not be overwritten. In this case,
                 * the {@link #result} are the non-overwritten settings.
                 */
                NON_OVERWRITTEN,
                /**
                 * No settings could be obtained. The {@link #result} is null.
                 */
                NONE;
        }

        static Result none() {
            return new Result(Type.NONE, null);
        }

        static Result overwritten(final NodeSettings settings) {
            return new Result(Type.OVERWRITTEN, settings);
        }

        static Result nonOverwritten(final NodeSettings settings) {
            return new Result(Type.NON_OVERWRITTEN, settings);
        }

    }

    /*
     * Returns Optional.empty if there aren't settings, yet.
     */
    private static Result getSettingsOverwrittenByFlowVariables(final SettingsType settingsType,
        final NativeNodeContainer nnc) {
        if (nnc.getFlowObjectStack() != null) {
            try {
                // a flow object stack is available (usually in case the node is connected)
                if (settingsType == SettingsType.VIEW) {
                    return nnc.getViewSettingsUsingFlowObjectStack().map(Result::overwritten).orElseGet(Result::none);
                } else {
                    return Result.overwritten(nnc.getModelSettingsUsingFlowObjectStack());
                }
            } catch (InvalidSettingsException ex) {
                LoadWarningsUtil.warnAboutVariableOverridesBeingIgnored(ex);
                return getSettingsWithoutFlowVariableOverrides(settingsType, nnc);
            }
        } else {
            final var settings = getSettingsWithoutFlowVariableOverrides(settingsType, nnc);
            if (settings.result != null && settings.result.getChildCount() == 0) {
                return Result.none();
            }
            return settings;

        }
    }

    private static Result getSettingsWithoutFlowVariableOverrides(final SettingsType settingsType,
        final NativeNodeContainer nnc) {
        try {
            return Result.nonOverwritten(nnc.getNodeSettings().getNodeSettings(settingsType.getConfigKey()));
        } catch (InvalidSettingsException ex) { //NOSONAR
            LoadWarningsUtil.warnAboutDefaultSettingsBeingUsedInstead(ex);
            return Result.none();
        }
    }

    /**
     * Important assumption here (which is given): We'll end up here when no (model or view) settings have been stored
     * with the node, yet. It's the case when no settings have been applied for the node, yet (via the dialog). And if
     * no settings have been applied, yet, there can also be no flow variables configured to overwrite a setting. Thus,
     * no need to merge the default settings with flow variable values (as done above).
     */
    private NodeSettings getDefaultSettings(final SettingsType type) {
        final var settings = new NodeSettings("default_settings");
        if (m_nc instanceof NativeNodeContainer nnc) {
            switch (type) {
                case MODEL -> nnc.getNode().saveModelSettingsTo(settings);
                case VIEW -> nnc.getNode().saveDefaultViewSettingsTo(settings);
            }
            return settings;
        } else {
            /**
             * Nothing to do, because we initialize the dialog from the workflow representation, not the settings
             * directly.
             */
            throw new UnsupportedOperationException(
                "Method not expected to be called by the framework (in case of components).");
        }
    }

    private void revertOverridesIfInvalid(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final NativeNodeContainer nnc) {
        if (settings.values().stream().noneMatch(SettingsExtractor::hasControllingVariables)) {
            return;
        }
        try {
            m_validator.validate(settings);
        } catch (InvalidSettingsException ex) {
            LoadWarningsUtil.warnAboutVariableOverridesBeingIgnored(ex);
            m_settingsTypes.forEach(type -> {
                final var nodeSettings = getSettingsWithoutFlowVariableOverrides(type, nnc);
                settings.put(type, NodeAndVariableSettingsProxy.createROProxy(nodeSettings.result,
                    new VariableSettings(nnc.getNodeSettings(), type)));
            });
        }
    }

    private static boolean hasControllingVariables(final VariableSettingsRO variableSettings) {
        try {
            for (var key : variableSettings.getVariableSettingsIterable()) {
                if (variableSettings.isVariableSetting(key)) {
                    if (variableSettings.getUsedVariable(key) != null) {
                        return true;
                    }
                } else if (hasControllingVariables(variableSettings.getVariableSettings(key))) {
                    return true;
                }
            }
        } catch (InvalidSettingsException ex) {
            // should never happen since we only call the throwing methods with keys from the iterable
            throw new IllegalStateException(ex);
        }
        return false;

    }

}
