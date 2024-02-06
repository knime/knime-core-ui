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
 *   Jan 24, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.util.DefaultNodeSettingsFieldTraverser;
import org.knime.core.webui.node.dialog.defaultdialog.util.DefaultNodeSettingsFieldTraverser.TraversedField;

/**
 * Takes care of accessing the fields in a given collection of {@link WidgetGroup}s. The implementer has to convert a
 * the traversed fields to a map of handlers of type <H> to make it accessible later via {@link #getHandler}.
 *
 * @author Paul Bärnreuther
 * @param <H> the type of the handler
 */
public abstract class FieldHandlerHolder<H> implements HandlerHolder<H> {

    private Map<String, H> m_handlers = new HashMap<>();

    FieldHandlerHolder(final Map<String, Class<? extends WidgetGroup>> settingsClasses) {
        final List<FieldWithDefaultNodeSettingsKey> traversedFields = settingsClasses.entrySet().stream()
            .flatMap(entry -> getTraversedFields(entry.getValue(), entry.getKey())).toList();
        m_handlers = toHandlers(traversedFields);
    }

    private static Stream<FieldWithDefaultNodeSettingsKey>
        getTraversedFields(final Class<? extends WidgetGroup> settingsClass, final String settingsKey) {
        return new DefaultNodeSettingsFieldTraverser(settingsClass).getAllFields().stream()
            .map(field -> new FieldWithDefaultNodeSettingsKey(field, settingsKey));
    }

    record FieldWithDefaultNodeSettingsKey(TraversedField field, String settingsKey) {
    }

    abstract Map<String, H> toHandlers(List<FieldWithDefaultNodeSettingsKey> fields);

    @Override
    public H getHandler(final String handlerClassName) {
        return m_handlers.get(handlerClassName);
    }

}
