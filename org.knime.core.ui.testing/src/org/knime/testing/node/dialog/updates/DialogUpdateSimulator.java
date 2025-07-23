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
 *   Sep 26, 2024 (Paul Bärnreuther): created
 */
package org.knime.testing.node.dialog.updates;

import static org.knime.core.webui.node.dialog.defaultdialog.util.updates.WidgetTreesToDependencyTreeUtil.settingsToTriggersAndInvocationHandler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerAndDependencies;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler.TriggerResult;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ButtonReferenceIdInternal;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Use this simulator to simulate the updates that are defined by {@link StateProvider}s within
 * {@link NodeParameters}
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public class DialogUpdateSimulator implements UpdateSimulator {

    Map<SettingsType, WidgetGroup> m_settings;

    TriggerInvocationHandler<Integer> m_triggerInvocationHandler;

    List<TriggerAndDependencies> m_listOfTriggers;

    private NodeParametersInput m_context;

    /**
     * @param settings - The classes of these settigns are used to extract the dependency tree. The settings themselves
     *            are used to extract dependencies during invocation.
     * @param context - Used during invocation
     */
    public DialogUpdateSimulator(final Map<SettingsType, WidgetGroup> settings,
        final NodeParametersInput context) {
        final var pair = settingsToTriggersAndInvocationHandler(
            settings.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getClass())),
            context);
        m_listOfTriggers = pair.getFirst();
        m_triggerInvocationHandler = pair.getSecond();
        m_settings = settings;
        m_context = context;
    }

    /**
     * @param modelSettings - The class of these settigns are used to extract the dependency tree. The settings
     *            themselves are used to extract dependencies during invocation.
     * @param context - Used during invocation
     */
    public DialogUpdateSimulator(final WidgetGroup modelSettings, final NodeParametersInput context) {
        this(Map.of(SettingsType.MODEL, modelSettings), context);
    }

    private TriggerResult<Integer> getTriggerResult(final Trigger trigger, final int... indices) {
        final var triggerAndDependencies =
            m_listOfTriggers.stream().filter(t -> t.getTrigger().equals(trigger)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("No trigger %s found.", trigger)));
        final var dependencyValues = triggerAndDependencies.extractDependencyValues(m_settings, m_context, indices);
        return m_triggerInvocationHandler.invokeTrigger(trigger, dependencyValues::get, m_context);
    }

    private UpdateSimulatorResult simulateTrigger(final Trigger trigger, final int... indices) {
        final var triggerResult = getTriggerResult(trigger, indices);

        return new UpdateSimulatorResult() {

            @Override
            public List<IndexedValue<Integer>> getMultiValueUpdatesInArrayAt(final SettingsType settingsType,
                final List<List<String>> paths) {
                return triggerResult.valueUpdates().get(new Location(paths, settingsType));
            }

            @Override
            public List<IndexedValue<Integer>> getMultiUiStateUpdateAt(final SettingsType settingsType,
                final List<List<String>> paths, final String providedOptionName) {
                return triggerResult.locationUiStateUpdates().get(new Location(paths, settingsType))
                    .get(providedOptionName);
            }
        };
    }

    @Override
    public UpdateSimulatorResult simulateValueChange(final SettingsType settingsType, final List<List<String>> paths,
        final int... indices) {
        return simulateTrigger(
            new Trigger.ValueTrigger(JsonFormsScopeUtil.getScopeFromLocation(new Location(paths, settingsType))),
            indices);
    }

    @Override
    public UpdateSimulatorResult simulateButtonClick(final Class<? extends ButtonReference> trigger,
        final int... indices) {
        final var internalReferenceId = trigger.getAnnotation(ButtonReferenceIdInternal.class);
        if (internalReferenceId != null) {
            return simulateTrigger(new Trigger.IdTrigger(internalReferenceId.value()), indices);
        }
        return simulateTrigger(new Trigger.IdTrigger(trigger.getName()), indices);
    }

    @Override
    public UpdateSimulatorResult simulateBeforeOpenDialog() {
        return simulateTrigger(new Trigger.IdTrigger("before-open-dialog"));
    }

    @Override
    public UpdateSimulatorResult simulateAfterOpenDialog() {
        return simulateTrigger(new Trigger.IdTrigger("after-open-dialog"));
    }

}
