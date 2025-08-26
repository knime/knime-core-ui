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
 *   Feb 21, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil.getScopeFromLocation;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.util.JacksonSerializationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler.TriggerResult;
import org.knime.node.parameters.WidgetGroup;

/**
 * Containing a utility method for converting the result of a {@link TriggerInvocationHandler} to something
 * interpretable by the DefaultNodeDialog frontend
 *
 * @author Paul Bärnreuther
 */
public final class UpdateResultsUtil {

    static final NodeLogger LOGGER = NodeLogger.getLogger(UpdateResultsUtil.class);

    private UpdateResultsUtil() {
        // Utility
    }

    /**
     * Common interface for all update results
     *
     */
    public sealed interface UpdateResult extends Comparable<UpdateResult> {
        @Override
        default int compareTo(final UpdateResult other) {
            return idForComparison().compareTo(other.idForComparison());
        }

        /**
         * We need to sort update results to ensure that the order of updates is deterministic. This is important for
         * snapshot tests.
         *
         * @return an id that is used when comparing update results.
         */
        String idForComparison();

        /**
         * Updates the value of a component.
         *
         * @param scope the scope of the component
         * @param values the list of to be updated values. Usually this is a one-element list with an element with empty
         *            indices. Other cases only occur when this update yields different results in each element of an
         *            array widget.
         * @param <I> the type by which dependencies and results are indexed.
         *
         */
        record ValueUpdateResult<I>(String scope, List<IndexedValue<I>> values) implements UpdateResult {

            @Override
            public String idForComparison() {
                return "0" + scope;
            }
        }

        /**
         * Updates the ui state of a component that has a location.
         *
         *
         * @param scope the scope leading to the component
         * @param providedOptionName the name of the option to be updated within the component
         * @param values the list of to be updated values. Usually this is a one-element list with an element with empty
         *            indices. Other cases only occur when this update yields different results in each element of an
         *            array widget.
         * @param <I> the type by which dependencies and results are indexed.
         *
         */
        record LocationUiStateUpdateResult<I>(String scope, String providedOptionName, List<IndexedValue<I>> values)
            implements UpdateResult {

            @Override
            public String idForComparison() {
                return "0" + scope + ":" + providedOptionName;
            }

        }

        /**
         *
         * Updates the ui state of a component that does not have a location.
         *
         * @param id the id of the component
         * @param providedOptionName the name of the option that is updated within the component
         * @param values the list of to be updated values. Usually this is a one-element list with an element with empty
         *            indices. Other cases only occur when this update yields different results in each element of an
         *            array widget.
         * @param <I> the type by which dependencies and results are indexed.
         *
         */
        record IdUiStateUpdateResult<I>(String id, String providedOptionName, List<IndexedValue<I>> values)
            implements UpdateResult {

            @Override
            public String idForComparison() {
                return "1" + id + ":" + providedOptionName;
            }
        }

    }

    /**
     * value can be a record here, leading to a failure, since the JsonFormsDataUtil mapper does not serialize /
     * getters, leading to empty serialized records
     */
    static UnaryOperator<Object> serializeUiState =
        v -> v instanceof WidgetGroup ? JsonFormsDataUtil.getMapper().valueToTree(v) : v;

    static UnaryOperator<Object> serializeValue = v -> JsonFormsDataUtil.getMapper().valueToTree(v);

    private static <I> UpdateResult createValueUpdateResult(final String scope, final List<IndexedValue<I>> values) {
        return new UpdateResult.ValueUpdateResult<>(scope, sortByIndices(serializeValues(values, serializeValue)));
    }

    private static <I> UpdateResult createLocationUiStateUpdateResult(final String scope,
        final String providedOptionName, final List<IndexedValue<I>> values) {
        return new UpdateResult.LocationUiStateUpdateResult<>(scope, providedOptionName,
            sortByIndices(serializeValues(values, serializeUiState)));
    }

    private static <I> UpdateResult createIdUiStateUpdateResult(final String id, final String providedOptionName,
        final List<IndexedValue<I>> values) {
        return new UpdateResult.IdUiStateUpdateResult<>(id, providedOptionName,
            sortByIndices(serializeValues(values, serializeUiState)));
    }

    private static <I> List<IndexedValue<I>> sortByIndices(final List<IndexedValue<I>> serializedValues) {
        return serializedValues.stream().sorted((v1, v2) -> toIndicesString(v1).compareTo(toIndicesString(v2)))
            .toList();
    }

    private static <I> String toIndicesString(final IndexedValue<I> v1) {
        return String.join(",", v1.indices().stream().map(Object::toString).toArray(String[]::new));
    }

    private static <I> List<IndexedValue<I>> serializeValues(final List<IndexedValue<I>> values,
        final UnaryOperator<Object> serialize) {
        return values.stream().map(value -> serializeValue(serialize, value)).toList();
    }

    private static <I> IndexedValue<I> serializeValue(final UnaryOperator<Object> serialize,
        final IndexedValue<I> value) {
        if (value.specialSerializer() != null) {
            final var serializedValue = JacksonSerializationUtil.serialize(value, value.specialSerializer());
            return new IndexedValue<>(value.indices(), serializedValue);
        }
        return new IndexedValue<I>(value.indices(), serialize.apply(value.value()));
    }

    /**
     * @param triggerResult
     * @return the list of resulting instructions
     */
    public static <I> List<UpdateResult> toUpdateResults(final TriggerResult<I> triggerResult) {
        final var valueUpdates = triggerResult.valueUpdates().entrySet().stream()
            .map(entry -> createValueUpdateResult(getScopeFromLocation(entry.getKey()), entry.getValue())).sorted();
        final var locationUiStateUpdates = triggerResult.locationUiStateUpdates().entrySet().stream()
            .flatMap(entry -> entry.getValue().entrySet().stream()
                .map(e -> createLocationUiStateUpdateResult(getScopeFromLocation(entry.getKey()), e.getKey(),
                    e.getValue())));
        final var idUiStateUpdates =
            triggerResult.idUiStateUpdates().entrySet().stream().flatMap(entry -> entry.getValue().entrySet().stream()
                .map(e -> createIdUiStateUpdateResult(entry.getKey(), e.getKey(), e.getValue())));
        return Stream.of(valueUpdates, locationUiStateUpdates, idUiStateUpdates).flatMap(Function.identity()).sorted()
            .toList();

    }

}
