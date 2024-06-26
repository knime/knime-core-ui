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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.ConvertValueUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

import com.fasterxml.jackson.databind.JsonNode;

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

    private static List<Dependency> getDependencyPaths(final Collection<DependencyVertex> dependencyVertices) {
        return dependencyVertices.stream().map(dep -> {
            final var valueRef = dep.getValueRef().getName();
            return new Dependency(dep.getFieldLocation(), valueRef);
        }).toList();
    }

    /**
     * @return Information on the field associated to the trigger, if such a field exists. Otherwise empty.
     */
    public Optional<PathsWithSettingsKey> getTriggerFieldLocation() {
        return m_triggerVertex.getFieldLocation();
    }

    /**
     * @param fieldLocation - information on the field associated to this dependency
     * @param valueRef - an id of the reference class
     */
    public record Dependency(PathsWithSettingsKey fieldLocation, String valueRef) {
    }

    /**
     * @return the dependencies
     */
    public List<Dependency> getDependencies() {
        return getDependencyPaths(m_dependencyVertices);
    }

    /**
     * @param settings
     * @param context the current {@link DefaultNodeSettingsContext}
     * @return a mapping to the values of the required dependencies
     */
    public Map<Class<? extends Reference>, Object> extractDependencyValues(final Map<String, WidgetGroup> settings,
        final DefaultNodeSettingsContext context) {
        final var mapper = JsonFormsDataUtil.getMapper();
        final Map<String, JsonNode> jsonNodes = getDependencySettingsKeys().stream().collect(
            Collectors.toMap(Function.identity(), settingsKey -> mapper.valueToTree(settings.get(settingsKey))));
        return createDependenciesValuesMap(context, jsonNodes);
    }

    private Map<Class<? extends Reference>, Object>
        createDependenciesValuesMap(final DefaultNodeSettingsContext context, final Map<String, JsonNode> jsonNodes) {
        final Map<Class<? extends Reference>, Object> dependencyValues = new HashMap<>();
        for (var vertex : m_dependencyVertices) {
            dependencyValues.put(vertex.getValueRef(), extractValue(vertex, jsonNodes, context));
        }
        return dependencyValues;
    }

    private static Object extractValue(final DependencyVertex vertex, final Map<String, JsonNode> jsonNodes,
        final DefaultNodeSettingsContext context) {
        var groupJsonNode = jsonNodes.get(vertex.getFieldLocation().settingsKey());
        var fieldJsonNode = groupJsonNode.at(toJsonPointer(vertex.getFieldLocation().paths()));
        return ConvertValueUtil.convertValueRef(fieldJsonNode, vertex.getValueRef(), context);
    }

    private static String toJsonPointer(final List<List<String>> paths) {

        /**
         * TODO: UIEXT-1841 remove this and treat the case of multiple paths here.
         */
        CheckUtils.check(paths.size() == 1, UiSchemaGenerationException::new,
            () -> String
                .format("There exists an initially triggered state provider with dependencies inside an array layout "
                    + "(with paths %s). This is not yet supported.", paths));
        return "/" + String.join("/", paths.get(0));
    }

    private Collection<String> getDependencySettingsKeys() {
        return getDependencies().stream().map(Dependency::fieldLocation).map(PathsWithSettingsKey::settingsKey)
            .collect(Collectors.toSet());
    }

    /**
     * @return the id of the trigger
     */
    public String getTriggerId() {
        return m_triggerVertex.getId();
    }

    /**
     * @return Whether the trigger is "Before the dialog is opened"
     */
    public boolean isBeforeOpenDialogTrigger() {
        return TriggerVertex.BEFORE_OPEN_DIALOG_ID.equals(getTriggerId());
    }

    /**
     * @return Whether the trigger is "After the dialog was opened"
     */
    public boolean isAfterOpenDialogTrigger() {
        return TriggerVertex.AFTER_OPEN_DIALOG_ID.equals(getTriggerId());
    }
}
