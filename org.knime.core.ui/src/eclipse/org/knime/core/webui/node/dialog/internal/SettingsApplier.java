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
 *   Mar 10, 2023 (hornm): created
 */
package org.knime.core.webui.node.dialog.internal;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeDialog.OnApplyNodeModifier;
import org.knime.core.webui.node.dialog.NodeSettingsService;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.internal.SettingsTreeTraversalUtil.VariableSettingsTree;
import org.knime.core.webui.node.dialog.internal.SettingsTreeTraversalUtil.VariableSettingsTreeNode;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.view.NodeViewManager;

import com.google.common.base.Objects;

/**
 * Helper to parse and apply data representing node settings to a node.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S3553") // accept Optionals as parameters as it is much more readable this way
public final class SettingsApplier {

    static final NodeLogger LOGGER = NodeLogger.getLogger(SettingsApplier.class);

    private final NodeContainer m_nc;

    private final Set<SettingsType> m_settingsTypes;

    private final TextToNodeSettingsConverter m_textToNodeSettingsConverter;

    private final OnApplyNodeModiferWrapper m_onApplyModifierWrapper;

    /**
     * Interprets the textual settings representation and writes it into the settings object. Common denominator of
     * {@link KaiNodeInterface#applyConfigureResponse(String, Map, Map)} and
     * {@link NodeSettingsService#toNodeSettings(String, Map, Map)}.
     *
     */
    @FunctionalInterface
    public interface TextToNodeSettingsConverter {
        /**
         * @param textSettings to convert
         * @param previousSettings of the node
         * @param settings to be written
         * @throws InvalidSettingsException in case the settings cannot be transformed to node settings
         */
        void toNodeSettings(final String textSettings, Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
            Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException;
    }

    /**
     * @param nodeContainer of the node
     * @param settingsTypes of the node
     * @param textToNodeSettingsConverter that writes the text representation of the settings into the settings object
     * @param onApplyModifier to apply when the data is applied
     */
    public SettingsApplier(final NodeContainer nodeContainer, final Set<SettingsType> settingsTypes,
        final TextToNodeSettingsConverter textToNodeSettingsConverter, final OnApplyNodeModifier onApplyModifier) {
        m_nc = nodeContainer;
        m_settingsTypes = settingsTypes;
        m_textToNodeSettingsConverter = textToNodeSettingsConverter;
        m_onApplyModifierWrapper = (onApplyModifier != null && nodeContainer instanceof NativeNodeContainer nnc)
            ? new OnApplyNodeModiferWrapper(onApplyModifier, nnc) : null;
    }

    /**
     * Applies the given data.
     *
     * @param settings string representation of the settings to apply
     * @throws IOException if an {@link InvalidSettingsException} is raised
     */
    public void applySettings(final String settings) throws IOException {
        try {
            applySettingsOrThrow(settings);
        } catch (InvalidSettingsException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private void applySettingsOrThrow(final String settings) throws InvalidSettingsException {
        var wfm = m_nc.getParent();
        var nodeID = m_nc.getID();
        var previousNodeSettings = getExistingNodeSettings(wfm, nodeID);
        var nodeSettings = getToBeAppliedNodeSettings(settings, previousNodeSettings);

        final Optional<ApplyDataSettings> modelApplyDataSettings =
            getApplyDataSettings(nodeSettings, previousNodeSettings, SettingsType.MODEL);
        final var viewApplyDataSettings = getApplyDataSettings(nodeSettings, previousNodeSettings, SettingsType.VIEW);

        final var changedModelSettings = modelApplyDataSettings.filter(ApplyDataSettings::hasChanged);
        final var changedViewSettings = viewApplyDataSettings.filter(ApplyDataSettings::hasChanged);

        // count before `applyChange` to count before settings are validated
        if (changedViewSettings.isPresent() || changedModelSettings.isPresent()) {
            // count either change as settings change, but not twice if both changed
            NodeTimer.GLOBAL_TIMER.incNodeSettingsChanged(m_nc);
        }
        applyChange(wfm, nodeID, nodeSettings, changedModelSettings, changedViewSettings);
        callOnChangeModifyer(modelApplyDataSettings, viewApplyDataSettings);
    }

    private NodeSettings getExistingNodeSettings(final WorkflowManager wfm, final NodeID nodeID) {
        var nodeSettings = new NodeSettings("node_settings");
        wfm.saveNodeSettings(nodeID, nodeSettings);
        if (!m_settingsTypes.contains(SettingsType.MODEL)) {
            nodeSettings.addNodeSettings(SettingsType.MODEL.getConfigKey());
        }
        return nodeSettings;
    }

    private NodeSettings getToBeAppliedNodeSettings(final String data, final NodeSettings previousNodeSettings)
        throws InvalidSettingsException {
        var nodeSettings = cloneSettings(previousNodeSettings, "to_be_applied_settings");
        populateNewSettings(data, nodeSettings, previousNodeSettings);
        return nodeSettings;
    }

    private static NodeSettings cloneSettings(final NodeSettings s, final String key) {
        var res = new NodeSettings(key);
        s.copyTo(res);
        return res;
    }

    /**
     * Transfer data into settings, i.e., apply the data to the settings
     *
     * @param previousNodeSettings
     */
    private void populateNewSettings(final String data, final NodeSettings nodeSettings,
        final NodeSettings previousNodeSettings) throws InvalidSettingsException {
        var settingsMap = new EnumMap<SettingsType, NodeAndVariableSettingsWO>(SettingsType.class);
        var previousSettingsMap = new EnumMap<SettingsType, NodeAndVariableSettingsRO>(SettingsType.class);
        for (var settingsType : m_settingsTypes) {
            nodeSettings.addNodeSettings(new NodeSettings(settingsType.getConfigKey()));
            nodeSettings.addNodeSettings(new NodeSettings(settingsType.getVariablesConfigKey()));
            settingsMap.put(settingsType, NodeAndVariableSettingsProxy.createWOProxy(//
                ApplyDataSettings.getOrCreateSubSettings(nodeSettings, settingsType), //
                new VariableSettings(nodeSettings, settingsType)//
            ));
            previousSettingsMap.put(settingsType, NodeAndVariableSettingsProxy.createROProxy( //
                ApplyDataSettings.getOrCreateSubSettings(previousNodeSettings, settingsType),
                new VariableSettings(previousNodeSettings, settingsType) //
            ));
        }
        m_textToNodeSettingsConverter.toNodeSettings(data, previousSettingsMap, settingsMap);
    }

    private Optional<ApplyDataSettings> getApplyDataSettings(final NodeSettings nodeSettings,
        final NodeSettings previousNodeSettings, final SettingsType type) throws InvalidSettingsException {
        return m_settingsTypes.contains(type)
            ? Optional.of(new ApplyDataSettings(nodeSettings, previousNodeSettings, type)) : Optional.empty();
    }

    private void applyChange(final WorkflowManager wfm, final NodeID nodeID, final NodeSettings nodeSettings,
        final Optional<ApplyDataSettings> changedModelSettings, final Optional<ApplyDataSettings> changedViewSettings)
        throws InvalidSettingsException {

        if (changedViewSettings.isPresent()) {
            final var changedView = changedViewSettings.get();
            try {
                validateViewSettings(m_nc, changedView.getSettings());
                setWarningOnInvalidFlowVariables(SettingsType.VIEW, changedViewSettings.get());
            } catch (InvalidSettingsException ex) {
                if (m_nc instanceof NativeNodeContainer nnc) {
                    overwriteSettingsToMakeThemValid(SettingsType.VIEW, nnc, nodeSettings, changedView, ex);
                } else {
                    throw ex;
                }
            }

        }

        if (changedModelSettings.isPresent()) {
            final var changedModel = changedModelSettings.get();
            try {
                validateAndPersistNodeSettings(wfm, nodeID, nodeSettings);
                setWarningOnInvalidFlowVariables(SettingsType.MODEL, changedModel);
            } catch (InvalidSettingsException ex) {
                if (m_nc instanceof NativeNodeContainer nnc) {
                    overwriteSettingsToMakeThemValid(SettingsType.MODEL, nnc, nodeSettings, changedModel, ex);
                    validateAndPersistNodeSettings(wfm, nodeID, nodeSettings);
                } else {
                    throw ex;
                }
            }
        } else if (changedViewSettings.isPresent()) {
            loadViewSettingsIntoNode(wfm, nodeID, changedViewSettings.get(), nodeSettings);
        }
    }

    private static void overwriteSettingsToMakeThemValid(final SettingsType settingsType, final NativeNodeContainer nnc,
        final NodeSettings nodeSettings, final ApplyDataSettings changedModel, final InvalidSettingsException ex)
        throws InvalidSettingsException {
        if (!hasControllingVariables(changedModel)) {
            throw ex;
        }
        final var variables = changedModel.getVariables().orElseThrow(IllegalStateException::new);
        final var underlyingSettings = changedModel.getSettings();
        final var settingsDescription = getSettingsDescription(settingsType);
        final var message = String.format("Could not apply settings for node %s due to invalid %s. "
            + "Trying to apply the settings with the flow variables instead.", settingsDescription, nnc.getID());
        LOGGER.warn(() -> message, ex);
        try {
            overwriteSettings(settingsType, nodeSettings, nnc, underlyingSettings, variables);
        } catch (InvalidSettingsException exForFlowVariables) {
            LOGGER.error(() -> String.format("Could not apply %s for node %s with overwritten settings either.",
                settingsDescription, nnc.getID()), exForFlowVariables);
            throw new InvalidSettingsException(String.format(
                "The %s could not be applied since both underlying manual and overwritten flow variable settings are invalid: %s",
                settingsDescription, ex.getMessage()), ex);
        }
        DataServiceContext.get()
            .addWarningMessage("The settings overridden by flow variables have been saved as underlying manual settings"
                + " to prevent an invalid settings exception if flow variables are unset.");
    }

    private static Object getSettingsDescription(final SettingsType settingsType) {
        return settingsType == SettingsType.MODEL ? "settings" : "view settings";
    }

    private void setWarningOnInvalidFlowVariables(final SettingsType settingsType, final ApplyDataSettings applyData) {
        final var variables = applyData.getVariables();
        if (variables.isEmpty()) {
            return;
        }
        try {
            final var overwrittenSettings = getOverwrittenSettings(m_nc, applyData.getSettings(), variables.get());
            if (m_nc instanceof NativeNodeContainer nnc) {
                validateSettings(settingsType, nnc, overwrittenSettings);
            }
        } catch (InvalidSettingsException ex) {
            final var message = "Settings overwritten with flow variables are invalid";
            LOGGER.warn(() -> message, ex);
            DataServiceContext.get().addWarningMessage(String.format("%s: %s", message, ex.getMessage()));
        }

    }

    private static void overwriteSettings(final SettingsType settingsType, final NodeSettings nodeSettings,
        final NativeNodeContainer nnc, final NodeSettings underlyingModelSettings, final NodeSettings modelVariables)
        throws InvalidSettingsException {
        final var overwrittenSettings = getOverwrittenSettings(nnc, underlyingModelSettings, modelVariables);
        validateSettings(settingsType, nnc, overwrittenSettings);
        setAsNewSettings(settingsType, nodeSettings, overwrittenSettings);
    }

    private static void validateSettings(final SettingsType settingsType, final NativeNodeContainer nnc,
        final NodeSettings settings) throws InvalidSettingsException {
        if (settingsType == SettingsType.MODEL) {
            validateModelSettings(nnc, settings);
        } else {
            validateViewSettings(nnc, settings);
        }
    }

    private static void validateModelSettings(final NativeNodeContainer nnc, final NodeSettings overwrittenSettings)
        throws InvalidSettingsException {
        nnc.getNode().validateModelSettings(overwrittenSettings);
    }

    private static NodeSettings getOverwrittenSettings(final NodeContainer nc, final NodeSettings underlyingSettings,
        final NodeSettings variables) throws InvalidSettingsException {
        final var configEdit = ConfigEditTreeModel.create(underlyingSettings, variables);
        final var flowVariables =
            nc.getFlowObjectStack().getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes());
        final var overwrittenSettings = new NodeSettings("overwritten");
        underlyingSettings.copyTo(overwrittenSettings);
        configEdit.overwriteSettings(overwrittenSettings, flowVariables);
        return overwrittenSettings;
    }

    private static void setAsNewSettings(final SettingsType settingsType, final NodeSettings nodeSettings,
        final NodeSettings newSettings) throws InvalidSettingsException {
        nodeSettings.addNodeSettings(settingsType.getConfigKey());
        newSettings.copyTo(nodeSettings.getNodeSettings(settingsType.getConfigKey()));
    }

    private static void validateAndPersistNodeSettings(final WorkflowManager wfm, final NodeID nodeID,
        final NodeSettings nodeSettings) throws InvalidSettingsException {
        wfm.loadNodeSettings(nodeID, nodeSettings);
    }

    private static void validateViewSettings(final NodeContainer nc, final NodeSettings viewSettings)
        throws InvalidSettingsException {
        NodeViewManager.getInstance().validateSettings(nc, viewSettings);
    }

    private void loadViewSettingsIntoNode(final WorkflowManager wfm, final NodeID nodeID, final ApplyDataSettings view,
        final NodeSettings nodeSettings) throws InvalidSettingsException {
        /**
         * We reset the node and trigger configure when
         * <ul>
         * <li>There are any flawed controlling view variables set in the dialog</li>
         * <li>The value of an exposed view setting changed</li>
         * <li>The node is idle. Here we need to trigger the configure again in case configure errors were resolved by
         * changing view variables.
         * </ul>
         */
        if (m_nc.getNodeContainerState().isIdle() || variablesInduceReset(view)) {
            validateAndPersistNodeSettings(wfm, nodeID, nodeSettings);
        } else {
            /** 'persist' view settings only (without resetting the node) */
            wfm.loadNodeViewSettings(nodeID, nodeSettings);
        }
    }

    private static boolean variablesInduceReset(final ApplyDataSettings view) throws InvalidSettingsException {
        return exposedVariablesChanged(view) || variableSettingsFlawed(view);

    }

    /**
     * Helper method to recursively determine whether there is any setting that has changed and that is exposed as a
     * variable
     */
    private static boolean exposedVariablesChanged(final ApplyDataSettings view) {
        return SettingsTreeTraversalUtil.traverseSettingsTrees(new VariableSettingsTree(view),
            SettingsApplier::exposedVariableChanged);
    }

    private static boolean exposedVariableChanged(final VariableSettingsTreeNode leaf) {
        final var exposedVariableName = getExposedVariableName(leaf.variables());
        final var previousExposedVariableName =
            leaf.previousVariables() == null ? null : getExposedVariableName(leaf.previousVariables());

        if (!Objects.equal(exposedVariableName, previousExposedVariableName)) {
            return true;
        }
        final var isExposed = exposedVariableName != null;
        final var isNotControlled = getUsedVariable(leaf.variables()) == null;
        return isExposed && isNotControlled && !leaf.settings().isIdentical(leaf.previousSettings());
    }

    private static boolean hasControllingVariables(final ApplyDataSettings applyDataSettings) {
        return SettingsTreeTraversalUtil.traverseSettingsTrees(new VariableSettingsTree(applyDataSettings),
            SettingsApplier::hasControllingVariable);
    }

    private static boolean hasControllingVariable(final VariableSettingsTreeNode leaf) {
        return getUsedVariable(leaf.variables()) != null;
    }

    private static String getUsedVariable(final NodeSettingsRO variable) {
        return variable.getString(VariableSettings.USED_VARIABLE_CFG_KEY, null);
    }

    private static String getExposedVariableName(final NodeSettingsRO variable) {
        return variable.getString(VariableSettings.EXPOSED_VARIABLE_CFG_KEY, null);
    }

    private static final String IS_FLAWED_CFG_KEY = VariableSettings.USED_VARIABLE_FLAWED_CFG_KEY;

    private static boolean variableSettingsFlawed(final ApplyDataSettings view) throws InvalidSettingsException {
        final var varSettings = view.getVariables();
        if (varSettings.isPresent()) {
            return variableSettingsFlawed(varSettings.get());
        }
        return false;

    }

    private static boolean variableSettingsFlawed(final NodeSettingsRO varSettings) throws InvalidSettingsException {
        if (varSettings.getBoolean(IS_FLAWED_CFG_KEY, false)) {
            return true;
        }
        for (var children = varSettings.children(); children.hasMoreElements();) {
            final var child = children.nextElement();

            if (child instanceof NodeSettingsRO nodeSettingsRO && variableSettingsFlawed(nodeSettingsRO)) {
                return true;
            }
        }
        return false;

    }

    /**
     * Note that if model settings passed to the close modifier have been overridden by flow variables, these settings
     * have been replaced with their value from previous settings (see above). This means that (i) the close modifier
     * cannot be controlled by flow variables and (ii) any changes applied to settings controlled by flow variables are
     * ignored by the close modifier. In other words, if a setting is controlled by a flow variable, its previous and
     * updated values, as passed to the close modifier, are always identical.
     */
    private void callOnChangeModifyer(final Optional<ApplyDataSettings> modelApplyDataSettings,
        final Optional<ApplyDataSettings> viewApplyDataSettings) {
        if (m_onApplyModifierWrapper != null) {
            m_onApplyModifierWrapper.setAppliedSettings( //
                modelApplyDataSettings.map(m -> m.getSettings()).orElse(new NodeSettings("empty")), //
                viewApplyDataSettings.map(v -> v.getSettings()).orElse(null), //
                modelApplyDataSettings.map(m -> m.getPreviousSettings()).orElse(new NodeSettings("empty")), //
                viewApplyDataSettings.map(v -> v.getPreviousSettings()).orElse(null) //
            );
            if (calledForEmbeddedDialog()) {
                // directly apply the settings and don't defer it to the 'cleanUp'
                m_onApplyModifierWrapper.onApply();
            }
        }
    }

    /**
     * Clean up
     */
    public void cleanUp() {
        if (m_onApplyModifierWrapper != null) {
            m_onApplyModifierWrapper.onApply();
        }
    }

    /*
     * Replaceable for testing purposes.
     */
    public static BooleanSupplier calledForEmbeddedDialogsPredicate = () -> {
        var thread = Thread.currentThread();
        return Stream.of(thread.getStackTrace())
            .noneMatch(e -> e.getClassName().equals("org.knime.core.wizard.rpc.DefaultNodeService")
                && e.getMethodName().equals("callNodeDataService"));
    };

    /*
     * Returns whether a call is made for the embedded node dialog and not for an detached node dialog.
     */
    private static boolean calledForEmbeddedDialog() {
        return calledForEmbeddedDialogsPredicate.getAsBoolean();
    }

    /**
     * Wraps an {@link OnApplyNodeModifier}, stores / defers settings updates on apply and finally delegates the applied
     * updates to the underlying {@link OnApplyNodeModifier} on dialog close.
     *
     * The 'deferred' onApply-call is only required for 'detached' dialogs since apply is also called when clicking on
     * "Save and Execute" when having a dialog and a view opened.
     */
    private static final class OnApplyNodeModiferWrapper {

        private final OnApplyNodeModifier m_modifier;

        private final NativeNodeContainer m_nnc;

        private NodeSettingsRO m_initialModelSettings;

        private NodeSettingsRO m_initialViewSettings;

        private NodeSettingsRO m_updatedModelSettings;

        private NodeSettingsRO m_updatedViewSettings;

        private OnApplyNodeModiferWrapper(final OnApplyNodeModifier modifier, final NativeNodeContainer nnc) {
            m_modifier = modifier;
            m_nnc = nnc;
        }

        private void setAppliedSettings(final NodeSettings modelSettings, final NodeSettings viewSettings,
            final NodeSettings previousModelSettings, final NodeSettings previousViewSettings) {
            if (modelSettings != null) {
                if (m_initialModelSettings == null) {
                    m_initialModelSettings = previousModelSettings;
                }
                m_updatedModelSettings = modelSettings;
            }
            if (viewSettings != null) {
                if (m_initialViewSettings == null) {
                    m_initialViewSettings = previousViewSettings;
                }
                m_updatedViewSettings = viewSettings;
            }
        }

        /**
         * Delegates the onApply-call to the wrapped {@link OnApplyNodeModifier}.But only if there are (still) settings
         * to be applied (i.e. can be called multiple times in a row where subsequent calls won't have an effect).
         */
        private void onApply() {
            if (m_updatedModelSettings == null && m_updatedViewSettings == null) {
                return;
            }
            final var currentInitialModelSettings = m_initialModelSettings;
            final var currentInitialViewSettings = m_initialViewSettings;
            final var currentUpdatedModelSettings = m_updatedModelSettings;
            final var currentUpdatedViewSettings = m_updatedViewSettings;
            m_initialModelSettings = m_initialViewSettings = m_updatedModelSettings = m_updatedViewSettings = null;
            m_modifier.onApply(m_nnc, currentInitialModelSettings, currentUpdatedModelSettings,
                currentInitialViewSettings, currentUpdatedViewSettings);
        }
    }

}
