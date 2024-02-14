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
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.PathWithSettingsKey;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueRef;

/**
 * Used to convert triggers to a list of resulting updates given a map of dependencies.
 *
 * @author Paul Bärnreuther
 */
public class DataServiceTriggerInvocationHandler {

    private TriggerInvocationHandler m_triggerInvocationHandler;

    DataServiceTriggerInvocationHandler(final Map<String, Class<? extends WidgetGroup>> settingsClasses) {
        m_triggerInvocationHandler = new TriggerInvocationHandler(settingsClasses);
    }

    record UpdateResult(String path, String id, Object value) {

        private static UpdateResult forPath(final String path, final Object value) {
            return new UpdateResult(path, null, value);
        }

        private static UpdateResult forId(final String id, final Object value) {
            return new UpdateResult(null, id, value);
        }
    }

    List<UpdateResult> trigger(final String triggerId, final Map<String, Object> rawDependencies,
        final DefaultNodeSettingsContext context) {
        final Function<Class<? extends ValueRef>, Object> dependencyProvider = valueRef -> {
            final var rawDependencyObject = rawDependencies.get(valueRef.getName());
            return ConvertValueUtil.convertValueRef(rawDependencyObject, valueRef, context);
        };
        final var triggerResult = m_triggerInvocationHandler.invokeTrigger(triggerId, dependencyProvider);

        final var valueUpdates = triggerResult.valueUpdates().entrySet().stream()
            .map(entry -> UpdateResult.forPath(toScope(entry.getKey()), entry.getValue()));
        final var otherUpdates = triggerResult.otherUpdates().entrySet().stream()
            .map(entry -> UpdateResult.forId(entry.getKey(), entry.getValue()));
        return Stream.concat(valueUpdates, otherUpdates).toList();

    }

    private static String toScope(final PathWithSettingsKey scope) {
        return JsonFormsScopeUtil.toScope(scope.path(), scope.settingsKey());
    }
}
