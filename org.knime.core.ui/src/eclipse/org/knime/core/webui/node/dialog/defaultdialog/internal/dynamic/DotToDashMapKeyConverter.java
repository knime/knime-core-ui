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
 *   Jun 30, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DotToDashMapKeyConverter.DotToDashMapKeyDeserializer;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DotToDashMapKeyConverter.DotToDashMapKeySerializer;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Adds custom JSON serializer and deserializer to dynamic settings.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = DotToDashMapKeySerializer.class)
@JsonDeserialize(using = DotToDashMapKeyDeserializer.class)
public @interface DotToDashMapKeyConverter {

    /**
     * Serializes a Map and replaces the dots (.) in the keys to dashes (-).
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    final class DotToDashMapKeySerializer extends JsonSerializer<Map<String, Object>> {

        @Override
        public void serialize(final Map<String, Object> value, final com.fasterxml.jackson.core.JsonGenerator gen,
            final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                serializers.defaultSerializeField(entry.getKey().replace('.', '-'), entry.getValue(), gen);
            }
            gen.writeEndObject();
        }

    }

    /**
     * Deserializes a Map and replaces the dashes (-) in the keys to dots (.).
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    final class DotToDashMapKeyDeserializer extends JsonDeserializer<Map<String, Object>> {

        @Override
        public Map<String, Object> deserialize(final JsonParser p, final DeserializationContext ctxt)
            throws IOException {
            final Map<String, Object> values =
                JsonFormsDataUtil.getMapper().readValue(p, new TypeReference<Map<String, Object>>() {
                });
            Map<String, Object> transformed = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String newKey = entry.getKey().replace("-", ".");
                transformed.put(newKey, entry.getValue());
            }
            return transformed;
        }

    }
}
