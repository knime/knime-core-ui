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
 *   Feb 23, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Utility for extracting from given settings classes those state providers that produce value-based updates triggered
 * at dialog open time (i.e., before-open-dialog or after-open-dialog). These are the updates that can cause the dialog
 * to appear dirty immediately after opening.
 *
 * @author Paul Bärnreuther
 */
public final class WidgetTreesToInitialValueUpdates {

    private WidgetTreesToInitialValueUpdates() {
        // Utility
    }

    /**
     * Information about a single value-based update triggered at dialog open time.
     *
     * @param stateProviderIdentifier identifies the state provider responsible for the update (its class name for
     *            class-based providers)
     * @param targetScope the JSON Forms scope of the field whose value is updated (e.g.
     *            {@code #/properties/model/properties/someField})
     * @param transitivelyTriggeredByScope if this update is triggered transitively by a value change on another field,
     *            this is the scope of that field; absent for direct (dialog-open) updates
     */
    @JsonInclude(Include.NON_NULL)
    public record ValueUpdateInfo(//
        @JsonProperty("stateProviderIdentifier") String stateProviderIdentifier, //
        @JsonProperty("targetScope") String targetScope, //
        @JsonProperty("transitivelyTriggeredByScope") String transitivelyTriggeredByScope) {
    }

    /**
     * Information about value-based state provider updates triggered at dialog open time.
     *
     * @param beforeOpenDialog updates triggered synchronously before the dialog is opened (these will always cause a
     *            dirty state)
     * @param afterOpenDialog updates triggered asynchronously after the dialog is opened (these may cause a dirty state)
     */
    public record InitialValueUpdates(//
        @JsonProperty("beforeOpenDialog") List<ValueUpdateInfo> beforeOpenDialog,//
        @JsonProperty("afterOpenDialog") List<ValueUpdateInfo> afterOpenDialog) {

        /**
         * @return true if there are any value-based state providers triggered at dialog open time
         */
        public boolean hasAny() {
            return !beforeOpenDialog().isEmpty() || !afterOpenDialog().isEmpty();
        }
    }

    /**
     * Builds the dependency tree from the given settings classes and extracts all value-based state provider updates
     * that are triggered when the dialog is opened (before-open-dialog and after-open-dialog).
     * <p>
     * Note: {@code null} is passed as context when building the dependency tree. This is sufficient for most state
     * providers since their {@code init()} method typically only declares triggers and dependencies without using
     * port/flow-variable context.
     *
     * @param settingsClasses map from settings type to settings class
     * @return the initial value updates
     */
    public static InitialValueUpdates getInitialValueUpdates(
        final Map<SettingsType, Class<? extends WidgetGroup>> settingsClasses) {
        final var factory = new WidgetTreeFactory();
        final var trees = settingsClasses.entrySet().stream()
            .map(e -> factory.createTree(e.getValue(), e.getKey())).toList();
        final var triggerVertices = WidgetTreesToDependencyTreeUtil.widgetTreesToDependencyTree(trees, null);
        return extractFromTriggers(triggerVertices);
    }

    private static InitialValueUpdates extractFromTriggers(final Collection<TriggerVertex> triggerVertices) {
        final var infos = List.of(IdTriggerVertex.BEFORE_OPEN_DIALOG_ID, IdTriggerVertex.AFTER_OPEN_DIALOG_ID).stream()
            .map(id -> triggerVertices.stream()
                .filter(t -> t instanceof IdTriggerVertex idTrigger && id.equals(idTrigger.getId()))
                .findFirst()
                .map(trigger -> collectValueUpdateInfos(trigger, triggerVertices))
                .orElse(List.of()))
            .toList();
        return new InitialValueUpdates(infos.get(0), infos.get(1));
    }

    /**
     * Collects direct and transitive value updates starting from an initial trigger. Transitive updates are those
     * triggered by value-change triggers on the fields updated by the initial (or a prior transitive) update. Each
     * scope is used as a trigger source at most once to handle cycles.
     */
    private static List<ValueUpdateInfo> collectValueUpdateInfos(final TriggerVertex initialTrigger,
        final Collection<TriggerVertex> allTriggers) {
        final List<ValueUpdateInfo> result = new ArrayList<>();
        final Set<String> processedTriggerScopes = new HashSet<>();
        final Queue<String> pendingScopes = new ArrayDeque<>();

        for (var update : collectDirectValueUpdatesFromTrigger(initialTrigger, null)) {
            result.add(update);
            pendingScopes.add(update.targetScope());
        }

        while (!pendingScopes.isEmpty()) {
            final var triggeringScope = pendingScopes.poll();
            if (!processedTriggerScopes.add(triggeringScope)) {
                continue;
            }
            allTriggers.stream()
                .filter(t -> t instanceof ValueTriggerVertex vtv
                    && triggeringScope.equals(JsonFormsScopeUtil.getScopeFromLocation(vtv.getLocation())))
                .findFirst()
                .ifPresent(valueTrigger -> {
                    for (var update : collectDirectValueUpdatesFromTrigger(valueTrigger, triggeringScope)) {
                        result.add(update);
                        if (!processedTriggerScopes.contains(update.targetScope())) {
                            pendingScopes.add(update.targetScope());
                        }
                    }
                });
        }

        return result;
    }

    /**
     * Traverses the graph from a single trigger vertex, collecting all {@link LocationUpdateVertex} instances with no
     * provided option (value updates, not UI-state updates).
     */
    private static List<ValueUpdateInfo> collectDirectValueUpdatesFromTrigger(final TriggerVertex trigger,
        final String transitivelyTriggeredByScope) {
        final List<ValueUpdateInfo> result = new ArrayList<>();
        final Set<Vertex> visited = new HashSet<>();
        final Queue<Vertex> queue = new ArrayDeque<>();
        queue.add(trigger);
        while (!queue.isEmpty()) {
            final var current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            for (final var child : current.getChildren()) {
                if (child instanceof LocationUpdateVertex luv && luv.getProvidedOption().isEmpty()) {
                    result.add(new ValueUpdateInfo(luv.getResolvedStateProvider().identifier().toString(),
                        JsonFormsScopeUtil.getScopeFromLocation(luv.getLocation()), transitivelyTriggeredByScope));
                } else if (!(child instanceof UpdateVertex)) {
                    queue.add(child);
                }
            }
        }
        return result;
    }
}
