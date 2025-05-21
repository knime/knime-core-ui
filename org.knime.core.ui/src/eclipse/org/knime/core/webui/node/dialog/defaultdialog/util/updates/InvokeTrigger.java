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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.DependencyInjector.DependencyProvider;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Vertex.VertexVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 * This class is used to convert a trigger to a map indexed by its triggered updates.
 *
 * @param <I> the type by which dependencies and results are indexed.
 * @author Paul Bärnreuther
 */
final class InvokeTrigger<I> {

    static final NodeLogger LOGGER = NodeLogger.getLogger(InvokeTrigger.class);

    private Map<Vertex, ValuesWithLevelOfNesting<I>> m_cache = new HashMap<>();

    private final Function<LocationAndType, List<IndexedValue<I>>> m_dependencyProvider;

    private final DefaultNodeSettingsContext m_context;

    /**
     *
     * @param dependencyProvider providing the values of all {@link Reference} dependencies that the triggered state
     *            providers will depend on.
     * @param context the context provided to triggered state providers
     */
    InvokeTrigger(final Function<LocationAndType, List<IndexedValue<I>>> dependencyProvider,
        final DefaultNodeSettingsContext context) {
        m_dependencyProvider = dependencyProvider;
        m_context = context;
    }

    /**
     * This class is used to convert a trigger to a map indexed by its triggered updates.
     *
     * @param trigger
     * @return a mapping from updated vertex to its associated state
     */
    public Map<UpdateVertex, List<IndexedValue<I>>> invokeTrigger(final TriggerVertex trigger) {
        return trigger.visit(new GetTriggeredUpdatesVisitor()).stream().collect(//
            Collectors.toMap(Function.identity(),
                updateVertex -> updateVertex.visit(new ComputeVisitor()).indexedValues)//
        );

    }

    private static final class GetTriggeredUpdatesVisitor implements VertexVisitor<Collection<UpdateVertex>> {

        @Override
        public Collection<UpdateVertex> accept(final UpdateVertex updateVertex) {
            return Set.of(updateVertex);
        }

        @Override
        public Collection<UpdateVertex> acceptDefault(final Vertex vertex) {
            return vertex.getChildren().stream().flatMap(c -> c.visit(this).stream())
                .collect(Collectors.toUnmodifiableSet());
        }

    }

    private static StateVertex getParentStateVertex(final Vertex vertex,
        final ResolvedStateProvider resolvedStateProvider) {
        final var getParentStateVertexVisitor = new VertexVisitor<StateVertex>() {
            @Override
            public StateVertex accept(final StateVertex stateVertex) {
                if (stateVertex.hasIdentifier(resolvedStateProvider.identifier())) {
                    return stateVertex;
                }
                return null;
            }
        };
        return vertex.getParents().stream().map(parent -> parent.visit(getParentStateVertexVisitor))
            .filter(Objects::nonNull).findAny().orElseThrow();
    }

    private static record ValuesWithLevelOfNesting<I>(List<IndexedValue<I>> indexedValues, int levelsOfNesting) {

        static <I> ValuesWithLevelOfNesting<I> from(final List<IndexedValue<I>> values) {
            if (values.isEmpty()) {
                return undefinedEmptyState();
            }
            final var levelsOfNesting = values.get(0).indices().size();
            return new ValuesWithLevelOfNesting<>(values, levelsOfNesting);
        }

        static <I> ValuesWithLevelOfNesting<I> undefinedEmptyState() {
            return new ValuesWithLevelOfNesting<>(List.of(), 0);
        }

        boolean isUndefined() {
            return indexedValues.isEmpty();
        }
    }

    private final class ComputeVisitor implements VertexVisitor<ValuesWithLevelOfNesting<I>> {

        /**
         * The initializer handed into a state provider for invocation
         *
         * @author Paul Bärnreuther
         */
        private final class StateProviderInvocationInitializer extends DefaultImperativeStateProviderInitializer {

            private final StateVertex m_stateVertex;

            private final Function<Vertex, Object> m_getParentValue;

            /**
             * @param stateVertex the vertex whose state provider is to be initialized
             * @param getParentValue a function to construct the suppliers from
             */
            private StateProviderInvocationInitializer(final StateVertex stateVertex,
                final Function<Vertex, Object> getParentValue) {
                super(new DependencyProvider<Class<? extends Reference<?>>>() {

                    @Override
                    public <V> Supplier<V> getValueSupplier(final Class<? extends Reference<?>> reference) {
                        return vertexToSupplier(stateVertex.getDependency(reference), getParentValue);
                    }

                }, new DependencyProvider<ControlValueReference<?>>() {

                    @Override
                    public <V> Supplier<V> getValueSupplier(final ControlValueReference<?> reference) {
                        return vertexToSupplier(stateVertex.getDependency(reference), getParentValue);
                    }

                });
                m_stateVertex = stateVertex;
                m_getParentValue = getParentValue;
            }

            @Override
            public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
                // Nothing to do here during invocation
            }

            @Override
            public void computeBeforeOpenDialog() {
                // Nothing to do here during invocation
            }

            @Override
            public void computeAfterOpenDialog() {
                // Nothing to do here during invocation
            }

            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                return vertexToSupplier(
                    getParentStateVertex(m_stateVertex, new ResolvedStateProvider(stateProviderClass)),
                    m_getParentValue);
            }

            @SuppressWarnings("unchecked")
            private static <T> Supplier<T> vertexToSupplier(final Vertex vertex,
                final Function<Vertex, Object> getParentValue) {
                return () -> (T)getParentValue.apply(vertex);
            }

            @Override
            public DefaultNodeSettingsContext getContext() {
                return m_context;
            }

        }

        @Override
        public ValuesWithLevelOfNesting<I> accept(final StateVertex stateVertex) {
            return cached(stateVertex, () -> computeStateVertexState(stateVertex));
        }

        private ValuesWithLevelOfNesting<I> computeStateVertexState(final StateVertex stateVertex) {
            final var parentValues = stateVertex.getParents().stream().map(v -> new Pair<>(v, v.visit(this)))
                .filter(pair -> pair.getSecond() != null).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            if (parentValues.values().stream().anyMatch(ValuesWithLevelOfNesting::isUndefined)) {
                return ValuesWithLevelOfNesting.undefinedEmptyState();
            }
            final var maximalParent =
                parentValues.values().stream().max(Comparator.comparing(ValuesWithLevelOfNesting::levelsOfNesting));
            final var indicesStream = maximalParent.map(ValuesWithLevelOfNesting::indexedValues)
                .map(values -> values.stream().map(IndexedValue::indices)).orElseGet(() -> Stream.of(List.of()));
            final var levelsOfNesting = maximalParent.map(ValuesWithLevelOfNesting::levelsOfNesting).orElse(0);
            return new ValuesWithLevelOfNesting<>(
                indicesStream.map(computeIndexedValue(stateVertex, parentValues)).flatMap(Optional::stream).toList(),
                levelsOfNesting);
        }

        private Function<List<I>, Optional<IndexedValue<I>>> computeIndexedValue(final StateVertex stateVertex,
            final Map<Vertex, ValuesWithLevelOfNesting<I>> parentValues) {
            return indices -> computeIndexedValue(indices, stateVertex, parentValues)
                .map(val -> new IndexedValue<>(indices, val));
        }

        private Optional<Object> computeIndexedValue(final List<I> indices, final StateVertex stateVertex,
            final Map<Vertex, ValuesWithLevelOfNesting<I>> parentValues) {
            final Function<Vertex, Object> getParentValue =
                vertex -> extractParentObject(parentValues.get(vertex), indices);
            final var initializer = new StateProviderInvocationInitializer(stateVertex, getParentValue);
            final var stateProvider = stateVertex.createStateProvider();
            StateProviderInitializerUtil.initializeStateProvider(stateProvider, initializer);
            try {
                return Optional.ofNullable(stateProvider.computeState(m_context));
            } catch (StateComputationFailureException e) {
                LOGGER.error(e);
                return Optional.empty();
            }
        }

        /**
         * Assumption: For every entry of the maximalParent, any parent contains an entry with the same indices but
         * reduced to the respective level of nesting.
         */
        private static <I> Object extractParentObject(final ValuesWithLevelOfNesting<I> valuesWithLevelOfNesting,
            final List<I> indices) {
            final var reducedIndices = indices.subList(0, valuesWithLevelOfNesting.levelsOfNesting);
            return valuesWithLevelOfNesting.indexedValues.stream()
                .filter(indexedValue -> indexedValue.indices().equals(reducedIndices)).map(IndexedValue::value).toList()
                .get(0);
        }

        @Override
        public ValuesWithLevelOfNesting<I> accept(final DependencyVertex dependencyVertex) {
            return cached(dependencyVertex,
                () -> ValuesWithLevelOfNesting.from(m_dependencyProvider.apply(dependencyVertex.getLocationAndType())));
        }

        @Override
        public ValuesWithLevelOfNesting<I> accept(final UpdateVertex updateVertex) {
            return getParentStateVertex(updateVertex, updateVertex.getResolvedStateProvider()).visit(this);
        }

        private ValuesWithLevelOfNesting<I> cached(final Vertex key,
            final Supplier<ValuesWithLevelOfNesting<I>> getValue) {
            if (!m_cache.containsKey(key)) {
                m_cache.put(key, getValue.get());
            }
            return m_cache.get(key);
        }

    }
}
