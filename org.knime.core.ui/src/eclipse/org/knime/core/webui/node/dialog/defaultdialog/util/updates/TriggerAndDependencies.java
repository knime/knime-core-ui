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

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil.getScopeFromLocation;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.ConvertValueUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A single trigger together with all dependencies of all of it triggered updates.
 *
 * @author Paul Bärnreuther
 */
public class TriggerAndDependencies {

    private final TriggerVertex m_triggerVertex;

    private final Collection<DependencyVertex> m_dependencyVertices;

    TriggerAndDependencies(final TriggerVertex triggerVertex, final Collection<DependencyVertex> dependencyVertices) {
        m_triggerVertex = triggerVertex;
        m_dependencyVertices = dependencyVertices;
    }

    private static List<Location> getDependencyPaths(final Collection<DependencyVertex> dependencyVertices) {
        return dependencyVertices.stream().map(DependencyVertex::getLocationAndType).map(LocationAndType::location)
            .sorted().toList();
    }

    /**
     * @return the dependencies
     */
    public List<Location> getDependencies() {
        return getDependencyPaths(m_dependencyVertices);
    }

    /**
     * @param settings
     * @param triggerIndices the indices indicating the triggers location if it triggering from within an array layout
     * @return a mapping to the values of the required dependencies
     */
    public Map<LocationAndType, List<IndexedValue<Integer>>>
        extractDependencyValues(final Map<SettingsType, WidgetGroup> settings, final int... triggerIndices) {
        final var mapper = JsonFormsDataUtil.getMapper();
        final Map<SettingsType, JsonNode> jsonNodes = getDependencySettingsTypes().stream().collect(
            Collectors.toMap(Function.identity(), settingsType -> mapper.valueToTree(settings.get(settingsType))));
        return createDependenciesValuesMap(jsonNodes, triggerIndices);
    }

    /**
     * This method can be used to extract dependencies from an already serialized data json.
     *
     * @param dataJson an object json node with top-level fields contained in ["model", "view"]
     * @param triggerIndices the indices indicating the triggers location if it triggering from within an array layout
     * @return a mapping to the values of the required dependencies
     */
    public Map<LocationAndType, List<IndexedValue<Integer>>> extractDependencyValues(final ObjectNode dataJson,
        final int... triggerIndices) {
        final Map<SettingsType, JsonNode> dataJsonPerSettingsType = new EnumMap<>(SettingsType.class);
        Stream.of(SettingsType.values()).forEach(settingsType -> {
            final var configKey = settingsType.getConfigKeyFrontend();
            if (dataJson.has(configKey)) {
                dataJsonPerSettingsType.put(settingsType, dataJson.get(configKey));
            }
        });

        return createDependenciesValuesMap(dataJsonPerSettingsType, triggerIndices);
    }

    private Map<LocationAndType, List<IndexedValue<Integer>>>
        createDependenciesValuesMap(final Map<SettingsType, JsonNode> jsonNodes, final int[] triggerIndices) {
        final Map<LocationAndType, List<IndexedValue<Integer>>> dependencyValues = new HashMap<>();
        for (var vertex : m_dependencyVertices) {
            dependencyValues.put(vertex.getLocationAndType(), extractValues(vertex, jsonNodes, triggerIndices));
        }
        return dependencyValues;
    }

    private static List<IndexedValue<Integer>> extractValues(final DependencyVertex vertex,
        final Map<SettingsType, JsonNode> jsonNodes, final int[] triggerIndices) {
        final var locationAndType = vertex.getLocationAndType();
        final var location = locationAndType.location();
        var groupJsonNode = jsonNodes.get(location.settingsType());

        final var paths = location.paths();
        var indexedFieldValues = getIndexedFieldValues(groupJsonNode, paths, triggerIndices);
        return indexedFieldValues.stream()
            .map(pair -> new IndexedValue<Integer>(pair.getFirst(),
                ConvertValueUtil.convertValue(pair.getSecond(), locationAndType.getType(), locationAndType.location(),
                    locationAndType.getSpecialDeserializer().orElse(null))))
            .toList();
    }

    private static List<Pair<List<Integer>, JsonNode>> getIndexedFieldValues(final JsonNode jsonNode,
        final List<List<String>> paths, final int... triggerIndices) {
        final var atFirstPath = jsonNode.at(toJsonPointer(paths.get(0)));
        if (paths.size() == 1) {
            return List.of(new Pair<>(List.of(), atFirstPath));
        }
        CheckUtils.checkState(atFirstPath.isArray(), "Json node at field with nested path should be an array.");
        final var restPaths = paths.subList(1, paths.size());
        final var nestLength = triggerIndices.length;
        if (nestLength > 0) {
            return getIndexedFieldValues(atFirstPath.get(triggerIndices[0]), restPaths,
                Arrays.copyOfRange(triggerIndices, 1, nestLength));
        }
        return IntStream.range(0, atFirstPath.size()).mapToObj(i -> i)
            .flatMap(i -> getIndexedFieldValues(atFirstPath.get(i), restPaths).stream().map(pair -> {
                final var indices = Stream.concat(Stream.of(i), pair.getFirst().stream()).toList();
                return new Pair<>(indices, pair.getSecond());
            })).toList();

    }

    private static String toJsonPointer(final List<String> path) {
        return "/" + String.join("/", path);
    }

    private Collection<SettingsType> getDependencySettingsTypes() {
        return getDependencies().stream().map(Location::settingsType).collect(Collectors.toSet());
    }

    /**
     * @return the id of the trigger
     */
    public Trigger getTrigger() {
        if (m_triggerVertex instanceof ValueTriggerVertex trigger) {
            return new Trigger.ValueTrigger(getScopeFromLocation(trigger.getLocation()));
        } else if (m_triggerVertex instanceof IdTriggerVertex trigger) {
            return new Trigger.IdTrigger(trigger.getId());
        }
        throw new IllegalStateException("Unknown trigger type: " + m_triggerVertex.getClass().getSimpleName());
    }

    /**
     * @return Whether the trigger is "Before the dialog is opened"
     */
    public boolean isBeforeOpenDialogTrigger() {
        return isIdTriggerWithId(IdTriggerVertex.BEFORE_OPEN_DIALOG_ID);
    }

    /**
     * @return Whether the trigger is "After the dialog is opened"
     */
    public boolean isAfterOpenDialogTrigger() {
        return isIdTriggerWithId(IdTriggerVertex.AFTER_OPEN_DIALOG_ID);
    }

    private boolean isIdTriggerWithId(final String id) {
        if (m_triggerVertex instanceof IdTriggerVertex globalTrigger) {
            return globalTrigger.getId().equals(id);
        }
        return false;

    }
}
