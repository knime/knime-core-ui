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
 *   Jun 16, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 *
 * This class is used to traverse the settings within {@link DefaultNodeSettings}. It is a depth-first (for nested
 * settings implementing {@link WidgetGroup}) traversal and it calls a given callback on every traversed leaf field
 * (every field which is not of type {@link WidgetGroup}).
 *
 * Additionally, optionally, one can specify a list of classes of annotations, which should be tracked during the
 * traversal. Each such annotation is checked for at every point of the traversal starting with the annotation on the
 * traversed class, then the first level settings, then, in case of nested settings for their classes, then for the 2nd
 * level settings and so on. For a nested setting which has an annotation both on its class and on the respective
 * enclosing field, an exception is thrown (see {@link FieldAnnotationsHolder}.
 *
 * @author Paul Bärnreuther
 */
public class DefaultNodeSettingsFieldTraverser {

    private static ObjectMapper mapper;

    private static ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = createMapper();
        }
        return mapper;
    }

    private static ObjectMapper createMapper() {
        var newMapper = new ObjectMapper();
        newMapper.setSerializationInclusion(Include.NON_NULL);
        newMapper.setVisibility(PropertyAccessor.ALL, Visibility.NON_PRIVATE);
        newMapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        newMapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
        newMapper.setPropertyNamingStrategy(new PropertyNamingStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public String nameForField(final MapperConfig<?> config, final AnnotatedField field,
                final String defaultName) {
                return StringUtils.removeStart(defaultName, "m_");
            }
        });
        return newMapper;
    }

    private final SerializerProvider m_serializerProvider;

    private final Class<?> m_settingsClass;

    /**
     * @param settingsClass the class to be traversed.
     */
    public DefaultNodeSettingsFieldTraverser(final Class<?> settingsClass) {
        m_serializerProvider = getMapper().getSerializerProviderInstance();
        m_settingsClass = settingsClass;
    }

    private DefaultNodeSettingsFieldTraverser(final SerializerProvider serializerProvider,
        final Class<?> settingsClass) {
        m_serializerProvider = serializerProvider;
        m_settingsClass = settingsClass;
    }

    /**
     * @param propertyWriter the property writer of the field holding name, class, annotations, etc.
     * @param path the list of the names of all enclosing field and the present one
     * @param trackedAnnotations the tracked annotations for the field and its enclosing classes and fields.
     * @author Paul Bärnreuther
     */
    public static record TraversedField(PropertyWriter propertyWriter, List<String> path,
        FieldAnnotationsHolder trackedAnnotations) {
        /**
         * @return a unique deterministic id.
         */
        public String getId() {
            return propertyWriter.getFullName().toString();
        }
    }

    /**
     * @param fieldCallback a callback called with every field in the traversed order.
     */
    public void traverse(final Consumer<TraversedField> fieldCallback) {
        traverse(fieldCallback, Collections.emptyList());
    }

    /**
     * @param fieldCallback a callback called with every field in the traversed order.
     * @param trackedAnnotations the classes of the annotations which should be tracked and given with the field
     *            parameter.
     */
    public void traverse(final Consumer<TraversedField> fieldCallback,
        final Collection<Class<? extends Annotation>> trackedAnnotations) {
        final var annotations = new FieldAnnotationsHolder(trackedAnnotations);
        traverseClass(m_settingsClass, fieldCallback, Collections.emptyList(), annotations);
    }

    private void traverseClass(final Class<?> clazz, final Consumer<TraversedField> fieldCallback,
        final List<String> path, final FieldAnnotationsHolder enclosingFieldAnnotations) {
        final var annotations = enclosingFieldAnnotations.toClassAnnotationsHolder(clazz);
        final var properties = getSerializableProperties(clazz);
        properties.forEachRemaining(field -> traverseField(fieldCallback, path, annotations, field));
    }

    private Iterator<PropertyWriter> getSerializableProperties(final Class<?> clazz) {
        try {
            final var settingsSerializer = m_serializerProvider.findValueSerializer(clazz);
            return settingsSerializer.properties();
        } catch (JsonMappingException ex) {
            throw new UiSchemaGenerationException(
                String.format("Error while obtaining serializer for class %s.", clazz.getSimpleName()), ex);
        }
    }

    private void traverseField(final Consumer<TraversedField> fieldCallback, final List<String> parentPath,
        final ClassAnnotationsHolder classAnnotations, final PropertyWriter field) {
        final var path = getPath(parentPath, field.getName());
        final var fieldType = field.getType().getRawClass();
        final var annotations = classAnnotations.toFieldAnnotationsHolder(field);
        if (WidgetGroup.class.isAssignableFrom(fieldType)) {
            this.traverseClass(fieldType, fieldCallback, path, annotations);
        } else {
            fieldCallback.accept(new TraversedField(field, path, annotations));
        }
    }

    private static List<String> getPath(final List<String> parentPath, final String next) {
        final var newPath = new ArrayList<String>(parentPath);
        newPath.add(next);
        return newPath;
    }

    /**
     * @return A list of all traversed fields including fields nested within array layout elements.
     */
    public List<TraversedField> getAllFields() {
        List<TraversedField> fields = new ArrayList<>();
        final Consumer<TraversedField> addActionHandlerClass = getAddTravesedFieldCallback(fields);
        traverse(addActionHandlerClass);
        return fields;
    }

    private Consumer<TraversedField> getAddTravesedFieldCallback(final List<TraversedField> fields) {
        return field -> {
            fields.addAll(getArrayLayoutElementFields(field));
            fields.add(field);
        };

    }

    private List<TraversedField> getArrayLayoutElementFields(final TraversedField field) {
        final var javaType = field.propertyWriter().getType();
        if (ArrayLayoutUtil.isArrayLayoutField(javaType)) {
            final var elementClass = javaType.getContentType().getRawClass();
            if (DefaultNodeSettings.class.isAssignableFrom(elementClass)) {
                return new DefaultNodeSettingsFieldTraverser(m_serializerProvider, elementClass).getAllFields();
            }
        }
        return new ArrayList<>(0);
    }

}
