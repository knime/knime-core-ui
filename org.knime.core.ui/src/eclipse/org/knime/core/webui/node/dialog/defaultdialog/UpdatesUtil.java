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
 *   Jan 23, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil.UpdateResult;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerAndDependencies;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.WidgetTreesToDependencyTreeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Utility class to resolve updates given by {@link StateProvider}s
 *
 * @author Paul Bärnreuther
 */
public final class UpdatesUtil {

    private UpdatesUtil() {
        // Utility
    }

    private static ObjectMapper MAPPER; // NOSONAR

    /**
     * @return the configured mapper for ui-schema generation
     */
    static ObjectMapper getMapper() {
        if (MAPPER == null) {
            MAPPER = createMapper();
        }
        return MAPPER;
    }

    private static ObjectMapper createMapper() {
        final var mapper = new ObjectMapper();
        /**
         * Added for resolving jdk8 dependent state providers
         */
        mapper.registerModule(new Jdk8Module());
        return mapper;
    }

    /**
     * Adds an array with one element for each trigger of an update defined by {@link StateProviders} to the rootNode if
     * any are present.
     *
     * @param rootNode
     * @param widgetTrees
     * @param settings
     * @param context
     */
    static void addUpdates(final ObjectNode rootNode, final Collection<Tree<WidgetGroup>> widgetTrees,
        final Map<SettingsType, WidgetGroup> settings, final DefaultNodeSettingsContext context) {
        final var pair =
            WidgetTreesToDependencyTreeUtil.<Integer> widgetTreesToTriggersAndInvocationHandler(widgetTrees, context);
        final var triggersWithDependencies = pair.getFirst();
        final var invocationHandler = pair.getSecond();
        final var partitioned = triggersWithDependencies.stream()
            .collect(Collectors.partitioningBy(TriggerAndDependencies::isBeforeOpenDialogTrigger));

        addInitialUpdates(rootNode, invocationHandler, settings, partitioned.get(true), context);
        addGlobalUpdates(rootNode, partitioned.get(false));
    }

    /**
     * public and only used for tests
     *
     * @param rootNode
     * @param settings
     * @param context
     */
    public static void constructTreesAndAddUpdates(final ObjectNode rootNode,
        final Map<SettingsType, DefaultNodeSettings> settings, final DefaultNodeSettingsContext context) {
        final var widgetTreeFactory = new WidgetTreeFactory();
        final var widgetTrees =
            SettingsTypeMapUtil.map(settings, (type, s) -> widgetTreeFactory.createTree(s.getClass(), type));
        addUpdates(rootNode, widgetTrees.values(), SettingsTypeMapUtil.map(settings), context);
    }

    private static void addInitialUpdates(final ObjectNode rootNode,
        final TriggerInvocationHandler<Integer> invocationHandler, final Map<SettingsType, WidgetGroup> settings,
        final List<TriggerAndDependencies> initialTriggersWithDependencies, final DefaultNodeSettingsContext context) {
        if (!initialTriggersWithDependencies.isEmpty()) {
            CheckUtils.check(initialTriggersWithDependencies.size() == 1, IllegalStateException::new,
                () -> "There should not exist more than one initial trigger.");
            addInitialUpdates(rootNode, initialTriggersWithDependencies.get(0), invocationHandler, settings, context);
        }
    }

    private static void addInitialUpdates(final ObjectNode rootNode,
        final TriggerAndDependencies triggerWithDependencies, final TriggerInvocationHandler<Integer> invocationHandler,
        final Map<SettingsType, WidgetGroup> settings, final DefaultNodeSettingsContext context) {
        final var dependencyValues = triggerWithDependencies.extractDependencyValues(settings, context);
        final var triggerResult =
            invocationHandler.invokeTrigger(triggerWithDependencies.getTrigger(), dependencyValues::get, context);
        final var updateResults = UpdateResultsUtil.toUpdateResults(triggerResult);

        final var initialUpdates = rootNode.putArray("initialUpdates");
        updateResults.forEach(updateResult -> addInitialUpdate(updateResult, initialUpdates));
    }

    private static void addInitialUpdate(final UpdateResult<Integer> updateResult, final ArrayNode initialUpdates) {
        initialUpdates.add(getMapper().valueToTree(updateResult));
    }

    private static void addGlobalUpdates(final ObjectNode rootNode,
        final List<TriggerAndDependencies> triggersWithDependencies) {
        if (triggersWithDependencies.isEmpty()) {
            return;
        }
        final var globalUpdates = rootNode.putArray("globalUpdates");
        triggersWithDependencies
            .forEach(triggerWithDependencies -> addGlobalUpdate(globalUpdates, triggerWithDependencies));
    }

    private static void addGlobalUpdate(final ArrayNode globalUpdates,
        final TriggerAndDependencies triggerWithDependencies) {
        final var updateObjectNode = globalUpdates.addObject();
        addTrigger(triggerWithDependencies, updateObjectNode);
        addDependencies(triggerWithDependencies, updateObjectNode);
    }

    private static void addDependencies(final TriggerAndDependencies triggerWithDependencies,
        final ObjectNode updateObjectNode) {
        final var dependenciesArrayNode = updateObjectNode.putArray("dependencies");
        triggerWithDependencies.getDependencies().stream()//
            /**
             * Sorting is necessary for deterministic behavior in snapshot tests.
             */
            .sorted()//
            .map(JsonFormsScopeUtil::getScopeFromLocation)//
            .forEach(dependenciesArrayNode::add);
    }

    private static void addTrigger(final TriggerAndDependencies triggerWithDependencies,
        final ObjectNode updateObjectNode) {
        updateObjectNode.set("trigger", MAPPER.valueToTree(triggerWithDependencies.getTrigger()));
        if (triggerWithDependencies.isAfterOpenDialogTrigger()) {
            updateObjectNode.put("triggerInitially", true);
        }

    }

}
