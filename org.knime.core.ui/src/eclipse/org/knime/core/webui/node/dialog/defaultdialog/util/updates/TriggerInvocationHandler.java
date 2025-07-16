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
 *   Feb 13, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil.getLocationFromScope;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;

/**
 * @author Paul Bärnreuther
 * @param <I> the type of the keys of the dependencies and resulting values in case of nested scopes (either by index or
 *            by indexId)
 *
 */
public class TriggerInvocationHandler<I> {

    private final Collection<TriggerVertex> m_triggers;

    TriggerInvocationHandler(final Collection<TriggerVertex> triggers) {
        m_triggers = triggers;
    }

    /**
     * @param <T> the type of the keys of the dependencies and resulting values in case of nested scopes (either by
     *            index by indexId)
     * @param widgetTrees
     * @param context the current context
     * @return a invocation handler for updates within the supplied widget trees
     */
    public static <T> TriggerInvocationHandler<T> fromWidgetTrees(final Collection<Tree<WidgetGroup>> widgetTrees,
        final NodeParametersInput context) {
        return new TriggerInvocationHandler<>(
            WidgetTreesToDependencyTreeUtil.widgetTreesToDependencyTree(widgetTrees, context));
    }

    /**
     * @param <T> the type of the keys of the dependencies and resulting values in case of nested scopes (either by
     *            index by indexId)
     * @param rendererSpecs that comprise the current dialog.
     * @param context the current context
     * @return a invocation handler for updates within the supplied widget trees
     */
    public static <T> TriggerInvocationHandler<T> fromRendererSpecs(
        final Collection<DialogElementRendererSpec> rendererSpecs, final NodeParametersInput context) {
        return new TriggerInvocationHandler<>(
            RendererSpecsToDependencyTreeUtil.rendererSpecsToDependencyTree(rendererSpecs, context));
    }

    /**
     * The resulting updates of a trigger invocation
     *
     * @param valueUpdates keys here are the path locations of fields whose value is updated
     * @param locationUiStateUpdates keys here are the path locations of fields whose ui state is updated; inner keys
     *            define which ui state is updated
     * @param idUiStateUpdates keys here are the ids of components without a location whose ui state is updated; inner
     *            keys define which ui state is updated
     *
     * @param <I> the type of the keys of the resulting values in case of nested scopes (either by index or by indexId)
     *
     */
    public record TriggerResult<I>(Map<Location, List<IndexedValue<I>>> valueUpdates,
        Map<Location, Map<String, List<IndexedValue<I>>>> locationUiStateUpdates,
        Map<String, Map<String, List<IndexedValue<I>>>> idUiStateUpdates) {

    }

    /**
     *
     * @param trigger the to be invoked trigger
     * @param dependencyProvider providing values for dependencies of this trigger (see {@link TriggerAndDependencies})
     * @param context provided to the triggered state providers
     * @return a mapping from identifiers of fields to their updated value
     */
    public TriggerResult<I> invokeTrigger(final Trigger trigger,
        final Function<LocationAndType, List<IndexedValue<I>>> dependencyProvider,
        final NodeParametersInput context) {
        final var constructedTriggerVertex = toTriggerVertex(trigger);
        final var triggerVertex = m_triggers.stream().filter(constructedTriggerVertex::equals).findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String
                .format("Trigger %s not found in the list of triggers: %s", constructedTriggerVertex, m_triggers)));
        return invokeTrigger(triggerVertex, dependencyProvider, context);
    }

    private static TriggerVertex toTriggerVertex(final Trigger trigger) {
        if (trigger instanceof Trigger.ValueTrigger valueTrigger) {
            return new ValueTriggerVertex(getLocationFromScope(valueTrigger.scope()));
        } else if (trigger instanceof Trigger.IdTrigger idTrigger) {
            return new IdTriggerVertex(idTrigger.id());
        } else {
            throw new IllegalArgumentException("Unknown trigger type: " + trigger);
        }
    }

    private TriggerResult<I> invokeTrigger(final TriggerVertex triggerVertex,
        final Function<LocationAndType, List<IndexedValue<I>>> dependencyProvider,
        final NodeParametersInput context) {
        final var resultPerUpdateHandler =
            new InvokeTrigger<>(dependencyProvider, context).invokeTrigger(triggerVertex);

        final Map<Location, List<IndexedValue<I>>> valueUpdates = new HashMap<>();
        final Map<Location, Map<String, List<IndexedValue<I>>>> otherLocationUpdates = new HashMap<>();
        final Map<String, Map<String, List<IndexedValue<I>>>> otherUpdates = new HashMap<>();

        for (var entry : resultPerUpdateHandler.entrySet()) {
            final var updateVertex = entry.getKey();
            final var indexedValues = entry.getValue();
            if (indexedValues.isEmpty()) {
                /**
                 * Update of states inside an empty array. No need add those to the list.
                 */
                continue;
            }
            if (updateVertex instanceof LocationUpdateVertex locationUpdateVertex) {
                final var providedOptions = locationUpdateVertex.getProvidedOption();
                if (providedOptions.isPresent()) {
                    otherLocationUpdates.computeIfAbsent(locationUpdateVertex.getLocation(), k -> new HashMap<>())
                        .put(providedOptions.get(), indexedValues);
                } else {
                    valueUpdates.put(locationUpdateVertex.getLocation(), indexedValues);
                }
            } else if (updateVertex instanceof IdUpdateVertex idUpdateVertex) {
                otherUpdates.computeIfAbsent(idUpdateVertex.getId(), k -> new HashMap<>())
                    .put(idUpdateVertex.getProvidedOption(), indexedValues);
            }
        }

        return new TriggerResult<>(valueUpdates, otherLocationUpdates, otherUpdates);
    }

}
