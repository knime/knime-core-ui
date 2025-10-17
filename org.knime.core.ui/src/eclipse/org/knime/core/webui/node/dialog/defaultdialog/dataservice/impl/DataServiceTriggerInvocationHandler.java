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
 *   Feb 7, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.impl;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil.getScopeFromLocation;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.NodeDialogServiceRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.Trigger;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.ConvertValueUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil.UpdateResult;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.LocationAndType;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Used to convert triggers to a list of resulting updates given a map of dependencies.
 *
 * @author Paul Bärnreuther
 */
final class DataServiceTriggerInvocationHandler {

    private final TriggerInvocationHandler<String> m_triggerInvocationHandler;

    private final NodeParametersInput m_context;

    private final NodeDialogServiceRegistry m_serviceRegistry;

    DataServiceTriggerInvocationHandler(final Map<SettingsType, Class<? extends WidgetGroup>> settingsClasses,
        final NodeParametersInput context, final NodeDialogServiceRegistry serviceRegistry) {
        final var widgetTreeFactory = new WidgetTreeFactory();
        final var widgetTrees = settingsClasses.entrySet().stream()
            .map(entry -> widgetTreeFactory.createTree(entry.getValue(), entry.getKey())).toList();
        m_context = context;
        m_serviceRegistry = serviceRegistry;
        m_triggerInvocationHandler = TriggerInvocationHandler.fromWidgetTrees(widgetTrees, m_context);
    }

    List<UpdateResult> trigger(final Trigger trigger, final Map<String, List<IndexedValue<String>>> rawDependencies) {
        final Function<LocationAndType, List<IndexedValue<String>>> dependencyProvider =
            locationAndType -> rawDependencies.get(getScopeFromLocation(locationAndType.location())).stream()
                .map(raw -> new IndexedValue<>(raw.indices(), parseValue(raw.value(), locationAndType.getType(),
                    locationAndType.location(), locationAndType.getSpecialDeserializer().orElse(null), m_context)))
                .toList();

        final var triggerResult = m_triggerInvocationHandler.invokeTrigger(trigger, dependencyProvider, m_context);
        return UpdateResultsUtil.toUpdateResults(triggerResult, m_serviceRegistry);
    }

    private static Object parseValue(final Object rawDependencyObject, final Type type, final Location location,
        final JsonDeserializer<?> specialDeserializer, final NodeParametersInput context) {

        return ConvertValueUtil.convertValue(rawDependencyObject, type, location, specialDeserializer, context);
    }

}
