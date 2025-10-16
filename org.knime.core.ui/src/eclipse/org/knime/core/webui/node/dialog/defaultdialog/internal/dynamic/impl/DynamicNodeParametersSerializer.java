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
 *   Aug 28, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl;

import static org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil.createInstance;

import java.io.IOException;

import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.util.JacksonSerializationUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Uses the default serializer of the value but inserts a "@class" field with a class identifier The class identifier is
 * provided by a {@link DynamicParametersProvider} that is either - set via the constructor if the update framework
 * requires to serialize a value manually - set via the
 * {@link ContextualSerializer#createContextual(SerializerProvider, BeanProperty)} in case jackson encounters the
 * dynamic parameters (annotated with {@link DynamicParameters}) inside settings parameters
 *
 * @author Paul Bärnreuther
 */
public class DynamicNodeParametersSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    static final String CLASS_ID_KEY = "@class";

    private DynamicParametersProvider<?> m_dynamicParametersProvider;

    /**
     * Default constructor for serialization by jackson. This is only called if createContextual is called with a field
     * value before serialization.
     */
    public DynamicNodeParametersSerializer() {
        // default constructor.
    }

    /**
     * Constructor used when a provider implementation is already available.
     *
     * @param dynamicParametersProvider the dynamic parameters provider
     */
    public DynamicNodeParametersSerializer(final DynamicParametersProvider<?> dynamicParametersProvider) {
        m_dynamicParametersProvider = dynamicParametersProvider;
    }

    /**
     * Constructor used by the update framework when value-updating dynamic parameters.
     *
     * @param dynamicParametersProviderClass the class of the dynamic parameters provider
     */
    public DynamicNodeParametersSerializer(
        final Class<? extends DynamicParametersProvider<?>> dynamicParametersProviderClass) {
        setDynamicParametersProviderClass(dynamicParametersProviderClass);
    }

    private void setDynamicParametersProviderClass(
        final Class<? extends DynamicParametersProvider<?>> dynamicParametersProviderClass) {
        m_dynamicParametersProvider = createInstance(dynamicParametersProviderClass);
    }

    @Override
    public JsonSerializer<?> createContextual(final SerializerProvider prov, final BeanProperty property)
        throws JsonMappingException {
        if (property != null) {
            setDynamicParametersProviderClass(property.getAnnotation(DynamicParameters.class).value());
        }
        return this;
    }

    @Override
    public void serialize(final Object value, final JsonGenerator gen, final SerializerProvider serializers)
        throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value instanceof FallbackDialogNodeParameters fallbackParameters) {
            gen.writeObject(fallbackParameters.toJson());
            return;
        }
        final var classIdentifier = getClassIdentifier(value);
        final var withoutClassField = getDefaultSerializedValue(value, serializers);
        gen.writeStartObject();
        gen.writeStringField(CLASS_ID_KEY, classIdentifier);
        for (var entry = withoutClassField.fields(); entry.hasNext();) {
            var e = entry.next();
            if (!CLASS_ID_KEY.equals(e.getKey())) {
                gen.writeFieldName(e.getKey());
                gen.writeTree(e.getValue());
            }
        }
        gen.writeEndObject();
    }

    private static JsonNode getDefaultSerializedValue(final Object value, final SerializerProvider serializers)
        throws JsonMappingException {
        final var defaultSerializer = serializers.findValueSerializer(value.getClass());
        return JacksonSerializationUtil.serialize(value, defaultSerializer);
    }

    private String getClassIdentifier(final Object value) throws IOException {
        if (m_dynamicParametersProvider == null) {
            throw new IOException(
                "DynamicNodeParametersSerializer used without setting the dynamicParametersProviderClass");
        }
        return m_dynamicParametersProvider.getClassIdStrategy().toIdentifier(value.getClass());
    }

}
