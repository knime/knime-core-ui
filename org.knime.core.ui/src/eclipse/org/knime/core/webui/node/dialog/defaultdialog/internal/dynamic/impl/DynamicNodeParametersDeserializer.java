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
 *   Aug 25, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl;

import static org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy.fromIdentifierConsistent;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl.DynamicNodeParametersSerializer.CLASS_ID_KEY;
import static org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil.createInstance;

import java.io.IOException;

import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This deserializer is used (only) for fields annotated with {@link DynamicParameters}. {@link DynamicNodeParameters}
 * are serialized with the {@link DynamicNodeParametersSerializer} and store a class identifier defined by
 * {@link ClassIdStrategy#toIdentifier(Class)}.
 *
 * For deserialization, we access the counterpart method {@link DynamicParametersProvider#getClass(String)}
 *
 * Implementation note: This deserializer must not be attached to {@link DynamicNodeParameters} directly. Otherwise the
 * delegating call at the end of the deserialization method would cause an infinite loop. It is automatically attached
 * to fields annotated with {@link DynamicParameters} via the {@link JacksonAnnotationsInside} and
 * {@link JsonDeserialize} annotations on that annotation.
 *
 * @author Paul Bärnreuther
 */
public class DynamicNodeParametersDeserializer extends JsonDeserializer<DynamicNodeParameters>
    implements ContextualDeserializer {

    private Class<? extends DynamicParametersProvider<?>> m_dynamicParametersProviderClass;

    /**
     * Default constructor for serialization by jackson. This is only called if createContextual is called with a field
     * value before deserialization.
     */
    public DynamicNodeParametersDeserializer() {
        // default constructor.
    }

    /**
     * Constructor used when deserializing in a detached context, i.e. for dependency resolution during updates
     *
     * @param dynamicParametersProviderClass the class of the dynamic parameters provider
     */
    public DynamicNodeParametersDeserializer(
        final Class<? extends DynamicParametersProvider<?>> dynamicParametersProviderClass) {
        m_dynamicParametersProviderClass = dynamicParametersProviderClass;
    }

    /**
     * Sets the class of the dynamic parameters provider. This method is called during contextualization of this
     * deserializer if it is used by the jackson framework or manually if this deserializer is used explicitly in the
     * code.
     *
     * @param dynamicParametersProviderClass the class of the dynamic parameters provider
     */
    public void setDynamicParametersProviderClass(
        final Class<? extends DynamicParametersProvider<?>> dynamicParametersProviderClass) {
        m_dynamicParametersProviderClass = dynamicParametersProviderClass;
    }

    @Override
    public JsonDeserializer<?> createContextual(final DeserializationContext ctxt, final BeanProperty property)
        throws JsonMappingException {
        if (property != null) {
            setDynamicParametersProviderClass(property.getAnnotation(DynamicParameters.class).value());
        }
        return this;
    }

    @Override
    public DynamicNodeParameters deserialize(final JsonParser jsonParser, final DeserializationContext context)
        throws IOException {
        final var codec = jsonParser.getCodec();
        final var root = codec.readTree(jsonParser);
        if (root instanceof NullNode) {
            return null;
        }
        final var obj = (ObjectNode)root;
        final var classNode = obj.get(CLASS_ID_KEY);
        if (classNode == null || !classNode.isTextual()) {
            throw new JsonMappingException(jsonParser, "Missing or non-textual '@class' property.");
        }
        final var requested = classNode.asText();
        final var dynamicParametersProvider = createInstance(m_dynamicParametersProviderClass);
        final var targetClass = fromIdentifierConsistent(dynamicParametersProvider.getClassIdStrategy(), requested);
        if (targetClass == null) {
            throw new JsonMappingException(jsonParser,
                "Class '" + requested + "' is not allowed for dynamic deserialization.");
        }
        obj.remove(CLASS_ID_KEY);
        return codec.treeToValue(obj, targetClass);

    }
}
