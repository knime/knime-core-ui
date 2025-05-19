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
 *   May 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 *
 * Expects a JSON object with the following keys:
 * <ul>
 * <li>"scope" (optional): the scope of the control where the trigger</li>
 * <li>"id" (optional): the id of the trigger.</li>
 * </ul>
 * At least one of these keys must be present.
 * <ul>
 * <li>Not providing the id means that the trigger is a value trigger.</li>
 * <li>Not providing the scope means that the trigger is a global trigger issued by the framework.</li>
 * </ul>
 */
final class TriggerDeserializer extends JsonDeserializer<Trigger> {

    /**
     * Same as the field name in {@link Trigger.IdTrigger}
     */
    private static final String ID = "id";

    /**
     * Same as the field name in {@link Trigger.ValueTrigger}
     */
    private static final String SCOPE = "scope";

    @Override
    public Trigger deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = p.readValueAs(Map.class);
        final var scope = getParameter(map, SCOPE);
        if (scope.isPresent()) {
            return new Trigger.ValueTrigger(scope.get());
        }
        final var id = getParameter(map, ID);
        if (id.isPresent()) {
            return new Trigger.IdTrigger(id.get());
        } else {
            throw new IOException("Could not deserialize trigger, since no scope or id was provided.");
        }
    }

    private static Optional<String> getParameter(final Map<String, Object> map, final String key) {
        return Optional.ofNullable(map.get(key)).filter(String.class::isInstance).map(String.class::cast);
    }

}
