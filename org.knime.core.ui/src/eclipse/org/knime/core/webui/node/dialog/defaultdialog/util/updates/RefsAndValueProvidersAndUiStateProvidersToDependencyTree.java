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
 *   Feb 6, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.RendererSpecsToImperativeRefsAndStateProviders.ImperativeRefsAndStateProviders;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Vertex.VertexVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.WidgetTreesToRefsAndStateProviders.RefsAndStateProviders;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.StateProvider.TypeReference;

final class RefsAndValueProvidersAndUiStateProvidersToDependencyTree {

    private RefsAndValueProvidersAndUiStateProvidersToDependencyTree() {
        // Utility
    }

    /**
     * Converts collected valueRefs and state providers to a tree structure connecting these
     *
     * @param valueRefsAndStateProviders collected from settings classes
     * @return the trigger vertices of the resulting tree of vertices
     */
    static Collection<TriggerVertex> refsAndStateProvidersToDependencyTree(
        final RefsAndStateProviders valueRefsAndStateProviders, final NodeParametersInput context) {
        return new DependencyTreeCreator(valueRefsAndStateProviders, ImperativeRefsAndStateProviders.empty(), context)
            .getTriggerVertices();
    }

    /**
     * Converts collected imperatively defined valueRefs and state providers to a tree structure connecting these
     *
     * @param imperativeRefsAndStateProviders collected from renderer specs
     * @return the trigger vertices of the resulting tree of vertices
     */
    static Collection<TriggerVertex> imperativeRefsAndStateProvidersToDependencyTree(
        final ImperativeRefsAndStateProviders imperativeRefsAndStateProviders,
        final NodeParametersInput context) {
        return new DependencyTreeCreator(RefsAndStateProviders.empty(), imperativeRefsAndStateProviders, context)
            .getTriggerVertices();
    }

    private static final class DependencyTreeCreator {

        private final NodeParametersInput m_context;

        private final Set<Vertex> m_visited = new HashSet<>();

        private final Queue<Vertex> m_queue = new ArrayDeque<>();

        private final Map<ResolvedStateProvider, StateVertex> m_stateVertices = new HashMap<>();

        private final Collection<DependencyVertex> m_dependencyVertices = new HashSet<>();

        private final Collection<TriggerVertex> m_triggerVertices = new HashSet<>();

        private final RefsAndStateProviders m_refsAndStateProviders;

        private final ImperativeRefsAndStateProviders m_imperativeRefsAndStateProviders;

        DependencyTreeCreator(final RefsAndStateProviders refsAndStateProviders,
            final ImperativeRefsAndStateProviders imperativeRefsAndStateProviders,
            final NodeParametersInput context) {
            m_context = context;
            m_refsAndStateProviders = refsAndStateProviders;
            m_imperativeRefsAndStateProviders = imperativeRefsAndStateProviders;
        }

        TriggerVertex addTriggerVertex(final TriggerVertex triggerVertex) {
            return addToCollectionIfAbsent(triggerVertex, m_triggerVertices);
        }

        DependencyVertex addDependencyVertex(final DependencyVertex dependencyVertex) {
            return addToCollectionIfAbsent(dependencyVertex, m_dependencyVertices);
        }

        /**
         * Adds the element to the collection if it is not already present.
         *
         * @param <T> type of the element
         * @param element the element to add
         * @param collection the collection to add the element to
         * @return the element itself if it was added, or the existing element in the collection
         */
        static <T> T addToCollectionIfAbsent(final T element, final Collection<T> collection) {
            if (collection.contains(element)) {
                return collection.stream().filter(element::equals).findAny().orElseThrow(IllegalStateException::new);
            } else {
                collection.add(element);
                return element;
            }

        }

        Collection<TriggerVertex> getTriggerVertices() {
            collectVertices();
            return m_triggerVertices.stream().sorted().toList();
        }

        private void collectVertices() {
            m_refsAndStateProviders.valueProviders()
                .forEach(update -> addToQueue(new LocationUpdateVertex(update.fieldLocation(),
                    new ResolvedStateProvider(update.stateProviderClass()))));
            m_refsAndStateProviders.idUiStateProviders().forEach(update -> addToQueue(new IdUpdateVertex(update.id(),
                update.providedOptionName(), new ResolvedStateProvider(update.stateProviderClass()))));
            m_refsAndStateProviders.locationUiStateProviders()
                .forEach(update -> addToQueue(new LocationUpdateVertex(update.fieldLocation(),
                    update.providedOptionName(), new ResolvedStateProvider(update.stateProviderClass()))));
            m_imperativeRefsAndStateProviders.locationUiStateProviders()
                .forEach(update -> addToQueue(new LocationUpdateVertex(update.fieldLocation(),
                    update.providedOptionName(), new ResolvedStateProvider(update.stateProvider()))));
            while (!m_queue.isEmpty()) {
                addParentsForVertex(m_queue.poll());
            }
        }

        private void addParentsForVertex(final Vertex vertex) {
            m_visited.add(vertex);
            final var parentVertices = vertex.visit(new CollectParentVerticesVisitor());
            parentVertices.forEach(vertex::addParent);
            parentVertices.forEach(this::addToQueue);
        }

        private void addToQueue(final Vertex vertex) {
            if (!m_visited.contains(vertex)) {
                m_queue.add(vertex);
            }
        }

        private final class CollectParentVerticesVisitor implements VertexVisitor<Collection<Vertex>> {

            StateVertex getStateVertex(final ResolvedStateProvider resolvedStateProvider) {
                return m_stateVertices.computeIfAbsent(resolvedStateProvider, StateVertex::new);
            }

            TriggerVertex getButtonTriggerVertex(final Class<? extends ButtonReference> buttonReferenceClass) {
                return addTriggerVertex(new IdTriggerVertex(buttonReferenceClass));
            }

            TriggerVertex getBeforeOpenDialogVertex() {
                return addTriggerVertex(new IdTriggerVertex(IdTriggerVertex.BEFORE_OPEN_DIALOG_ID));
            }

            TriggerVertex getAfterOpenDialogVertex() {
                return addTriggerVertex(new IdTriggerVertex(IdTriggerVertex.AFTER_OPEN_DIALOG_ID));
            }

            /**
             * Receives or creates the {@link StateVertex} for the associated state provider and sets it as parent.
             */
            @Override
            public Collection<Vertex> accept(final UpdateVertex updateVertex) {
                final var stateProviderClass = updateVertex.getResolvedStateProvider();
                return Set.of(getStateVertex(stateProviderClass));
            }

            /**
             * Receives or creates vertices for the dependencies of the state provider and sets them as parent.
             */
            @Override
            public Collection<Vertex> accept(final StateVertex stateVertex) {
                final var stateProvider = stateVertex.createStateProvider();
                CheckUtils.checkNotNull(stateProvider, "Failed to instantiate state provider class %s.", stateVertex);

                final Collection<Pair<Object, DependencyVertex>> extractedDependencies = new HashSet<>();
                final Collection<TriggerVertex> extractedTriggers = new HashSet<>();

                final var dependencyCollector = new DeclarativeDependencyCollector(m_refsAndStateProviders.valueRefs(),
                    (key, dep) -> extractedDependencies.add(new Pair<>(key, dep)), extractedTriggers::add);
                final var imperativeDependencyCollector =
                    new ImperativeDependencyCollector(m_imperativeRefsAndStateProviders.valueRefs(),
                        (key, dep) -> extractedDependencies.add(new Pair<>(key, dep)), extractedTriggers::add);

                final var stateProviderDependencyReceiver =
                    new StateProviderDependencyReceiver(m_context, dependencyCollector, imperativeDependencyCollector);
                StateProviderInitializerUtil.initializeStateProvider(stateProvider, stateProviderDependencyReceiver);

                final Collection<Vertex> parentVertices = new HashSet<>();

                extractedTriggers.stream().map(trigger -> addTriggerVertex(trigger)).forEach(parentVertices::add);
                extractedDependencies.stream()
                    .map(pair -> new Pair<>(pair.getFirst(), addDependencyVertex(pair.getSecond()))).forEach(pair -> {
                        final var reference = pair.getFirst();
                        final var dependencyVertex = pair.getSecond();
                        stateVertex.addDependency(reference, dependencyVertex);
                        parentVertices.add(dependencyVertex);
                    });

                parentVertices.addAll(stateProviderDependencyReceiver.getButtonRefTriggers().stream()
                    .map(this::getButtonTriggerVertex).toList());
                parentVertices.addAll(stateProviderDependencyReceiver.getStateProviders().stream()
                    .map(ResolvedStateProvider::new).map(this::getStateVertex).toList());
                if (stateProviderDependencyReceiver.m_computeBeforeOpenDialog) {
                    parentVertices.add(getBeforeOpenDialogVertex());
                }
                if (stateProviderDependencyReceiver.m_computeAfterOpenDialog) {
                    parentVertices.add(getAfterOpenDialogVertex());
                }
                return parentVertices;
            }

            @Override
            public Collection<Vertex> acceptDefault(final Vertex vertex) {
                // No parents. A trigger or a dependency can't have a parent itself.
                return new HashSet<>();
            }

        }
    }

    /**
     * The state provider initializer used during constructions to document dependencies and triggers
     *
     * @author Paul Bärnreuther
     */
    private static final class StateProviderDependencyReceiver extends DefaultImperativeStateProviderInitializer {

        private NodeParametersInput m_context;

        private StateProviderDependencyReceiver(final NodeParametersInput context,
            final DependencyInjector<Class<? extends ParameterReference<?>>> dependencyCollector,
            final DependencyInjector<ControlValueReference<?>> imperativeDependencyCollector) {
            super(dependencyCollector, imperativeDependencyCollector);
            m_context = context;
        }

        private final Collection<Class<? extends ButtonReference>> m_buttonRefTriggers = new HashSet<>();

        private final Collection<Class<? extends StateProvider>> m_stateProviders = new HashSet<>();

        boolean m_computeBeforeOpenDialog;

        boolean m_computeAfterOpenDialog;

        @Override
        public <T> Supplier<T> computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
            getStateProviders().add(stateProviderClass);
            return null;
        }

        @Override
        public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
            getButtonRefTriggers().add(ref);
        }

        @Override
        public void computeBeforeOpenDialog() {
            m_computeBeforeOpenDialog = true;
        }

        @Override
        public void computeAfterOpenDialog() {
            m_computeAfterOpenDialog = true;
        }

        Collection<Class<? extends StateProvider>> getStateProviders() {
            return m_stateProviders;
        }

        Collection<Class<? extends ButtonReference>> getButtonRefTriggers() {
            return m_buttonRefTriggers;
        }

        @Override
        public NodeParametersInput getNodeParametersInput() {
            return m_context;
        }

    }

    static Type getSettingsType(final TypeReference<?> valueRef) {
        return GenericTypeFinderUtil.getFirstGenericType(valueRef.getClass(), TypeReference.class);
    }

}
