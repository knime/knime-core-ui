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
 *   Feb 21, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.datatype;

import java.io.IOException;
import java.io.StringReader;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * A utility class for transforming {@link DataType DataTypes} to and from string for use in JSON serialization.
 *
 * @author Paul Bärnreuther
 */
public final class DataTypeSerializationUtil {

    private DataTypeSerializationUtil() {
        // Util
    }

    private static final String TYPE_CFG_KEY = "dataType";

    /**
     * Adds custom serialization logic for {@link DataType} to the given module.
     *
     * @param module
     */
    public static void addSerializerAndDeserializer(final SimpleModule module) {
        module.addSerializer(DataType.class, new DataTypeSerializer());
        module.addDeserializer(DataType.class, new DataTypeDeserializer());
    }

    /**
     * Serializer for {@link DataType}. The corresponding deserializer is {@link DataTypeDeserializer}.
     *
     * @author Paul Bärnreuther
     */
    public static final class DataTypeSerializer extends JsonSerializer<DataType> {

        @Override
        public void serialize(final DataType value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            if (value == null) {
                throw new IOException("DataType must not be null");
            }
            gen.writeString(typeToString(value));
        }

        /**
         * Serializes a given {@link DataType} into a string. Use {@link DataTypeDeserializer#stringToType(String)} to
         * de-serialize it.
         *
         * @param type the to-be-serialized {@link DataType}
         * @return the serialized string
         */
        private static String typeToString(final DataType type) {
            final var settings = new NodeSettings("serialized_data_type_settings");
            settings.addDataType(TYPE_CFG_KEY, type);
            return JSONConfig.toJSONString(settings, WriterConfig.DEFAULT);
        }
    }

    /**
     * Deserializer for {@link DataType}. The corresponding serializer is {@link DataTypeSerializer}.
     *
     * @author Paul Bärnreuther
     */
    public static final class DataTypeDeserializer extends JsonDeserializer<DataType> {

        @Override
        public DataType deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final var node = (JsonNode)p.getCodec().readTree(p);
            final var serializedType = node.asText();
            try {
                return stringToType(serializedType);
            } catch (InvalidSettingsException ex) {
                throw new IOException("Could not de-serialize DataType", ex);
            }
        }

        /**
         * De-serializes a string that has been generated via {@link DataTypeSerializer#typeToString(DataType)}.
         *
         * @param string the previously serialized string
         * @return the de-serialized {@link DataType}
         * @throws IOException in case the provided string is not a valid JSON
         * @throws InvalidSettingsException if the JSON does not represent a valid {@link DataType}
         */
        private static DataType stringToType(final String string) throws IOException, InvalidSettingsException {
            final var settings = new NodeSettings("data_type_deserialization_settings");
            JSONConfig.readJSON(settings, new StringReader(string));
            return settings.getDataType(TYPE_CFG_KEY);
        }

    }
}
