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
 *   Feb 16, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.holder;

import java.io.IOException;

import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Utility for serializing internal data that are not to be sent to the client. Note that when attaching serializers to
 * a field it is not possible to use a {@link ValueReference} or {@link ValueProvider} on that same field. One can work
 * around this by nesting the field in another field with those annotations attached.
 *
 * @author Paul Bärnreuther
 */
public class CustomObjectSerializationUtil {

    private CustomObjectSerializationUtil() {
        // Cannot be instantiated
    }

    /**
     * Serializes an arbitrary object to boolean {@code true} (non-null, object stored server-side in
     * {@link CustomObjectHolder}) or {@code false} (null).
     *
     * @param <T> the type of the object to serialize
     */
    public abstract static class CustomObjectSerializer<T> extends JsonSerializer<T> {

        @Override
        public void serialize(final T value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            if (value == null) {
                gen.writeBoolean(false);
                return;
            }
            final var fieldId = FieldLocationUtil.toFieldId(gen.getOutputContext());
            final var nodeId = FieldLocationUtil.getNodeId();
            CustomObjectHolder.addObject(nodeId, fieldId, value);
            gen.writeBoolean(true);
        }
    }

    /**
     * Deserializes a boolean back to an object. If {@code true}, retrieves the actual object from
     * {@link CustomObjectHolder}. If {@code false}, returns {@code null}.
     *
     * @param <T> the type of the object to deserialize
     */
    public abstract static class CustomObjectDeserializer<T> extends JsonDeserializer<T> {

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final boolean hasValue = p.getBooleanValue();
            if (!hasValue) {
                return null;
            }
            final var fieldId = FieldLocationUtil.toFieldId(p.getParsingContext());
            final var nodeId = FieldLocationUtil.getNodeId();
            return (T)CustomObjectHolder.get(nodeId, fieldId);
        }
    }

}
