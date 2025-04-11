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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsScopeUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * Use this utility class to construct schema and ui schema elements from renderer specs.
 *
 * @author Paul Bärnreuther
 */
public final class RendererToJsonFormsUtil {

    static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    private static ObjectMapper MAPPER = createMapper(); // NOSONAR

    private RendererToJsonFormsUtil() {
        //utility class
    }

    private static ObjectMapper createMapper() {
        final var mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return mapper;
    }

    /**
     * Convert a renderer spec to a method that adds properties to the schema (in place; not a Consumer just for
     * convenience)
     *
     * @param spec the renderer spec
     * @return the generated schema modifier
     */
    public static UnaryOperator<ObjectNode> toSchemaPropertiesAdder(final DialogElementRendererSpec spec) {
        return schema -> addPropertiesToSchema(spec, schema, ActionOnMissingObject.FAIL,
            ActionOnPresentProperty.REPLACE);
    }

    /**
     * Similar to {@link #constructSchema(DialogElementRendererSpec)} but does require a schema to be passed in.
     *
     * Convert a renderer spec to a method that creates the schema (in place; not a Consumer just for convenience)
     *
     * @param spec the renderer spec
     * @return the generated schema constructor which adds a minimal schema for the given spec to a given schema
     */
    public static UnaryOperator<ObjectNode> toSchemaConstructor(final DialogElementRendererSpec spec) {
        return schema -> addPropertiesToSchema(spec, schema, ActionOnMissingObject.CREATE,
            ActionOnPresentProperty.VALIDATE);
    }

    /**
     * Similar to {@link #toSchemaConstructor(DialogElementRendererSpec)} but does not require a schema to be passed in.
     *
     * @param spec the renderer spec
     * @return the constructed minimal json schema for the given renderer spec
     */
    public static ObjectNode constructSchema(final DialogElementRendererSpec spec) {
        return toSchemaConstructor(spec).apply(FACTORY.objectNode());
    }

    private enum ActionOnMissingObject {
            CREATE, FAIL;
    }

    private enum ActionOnPresentProperty {
            VALIDATE, REPLACE;
    }

    private static ObjectNode addPropertiesToSchema(final DialogElementRendererSpec spec,
        final ObjectNode jsonSchemaWithoutProperties, final ActionOnMissingObject actionOnMissingObject,
        final ActionOnPresentProperty actionOnPresentProperty) {
        if (spec instanceof LocalizedControlRendererSpec control) {
            return addControlPropertiesToSchema(control, jsonSchemaWithoutProperties, actionOnMissingObject,
                actionOnPresentProperty);
        }
        if (spec instanceof LayoutRendererSpec layout) {
            layout.getElements().forEach(el -> addPropertiesToSchema(el, jsonSchemaWithoutProperties,
                actionOnMissingObject, actionOnPresentProperty));
            return jsonSchemaWithoutProperties;
        }
        throw new NotImplementedException("Cannot convert " + spec.getClass().getName() + " to schema element.");
    }

    private static ObjectNode addControlPropertiesToSchema(final LocalizedControlRendererSpec localizedControl,
        final ObjectNode jsonSchemaWithoutProperties, final ActionOnMissingObject actionOnMissingObject,
        final ActionOnPresentProperty actionOnPresentProperty) {
        final BiFunction<ObjectNode, List<String>, ObjectNode> getSubSchemaAt = switch (actionOnMissingObject) {
            case CREATE -> JsonFormsScopeUtil::getOrConstructSchemaAt;
            case FAIL -> JsonFormsScopeUtil::getSchemaAtOrFail;
        };
        final var control = localizedControl.getControlSpec();
        final var controlSchema =
            getSubSchemaAt.apply(jsonSchemaWithoutProperties, localizedControl.getPathWithinValueJsonObject());
        putStringProperty(controlSchema, Schema.TAG_TYPE, control.getDataType().getType(), actionOnPresentProperty);
        putStringProperty(controlSchema, Schema.TAG_TITLE, control.getTitle(), actionOnPresentProperty);
        final var description = control.getDescription().orElse(null);
        if (description != null) {
            putStringProperty(controlSchema, Schema.TAG_DESCRIPTION, description, actionOnPresentProperty);
        }
        return jsonSchemaWithoutProperties;
    }

    private static void putStringProperty(final ObjectNode schema, final String propertyName, final String value,
        final ActionOnPresentProperty action) {
        if (schema.has(propertyName)) {
            if (action == ActionOnPresentProperty.REPLACE) {
                schema.put(propertyName, value);
            } else {
                validateStringProperty(schema, propertyName, value);
            }
        } else {
            schema.put(propertyName, value);
        }
    }

    private static void validateStringProperty(final ObjectNode schema, final String propertyName, final String value) {
        validateProperty(JsonNode::asText, schema, propertyName, value);
    }

    private static <T> void validateProperty(final Function<JsonNode, T> extractor, final ObjectNode schema,
        final String propertyName, final T value) {
        final var presentValue = extractor.apply(schema.get(propertyName));
        if (!value.equals(presentValue)) {
            throw new IllegalStateException(String.format(
                "Property %s is already present with different value. Present value is '%s'. New value is '%s'.",
                propertyName, schema.get(propertyName).asText(), value));
        }
    }

    /**
     * Convert a renderer spec to its resolved ui schema
     *
     * @param spec the renderer spec
     * @return the generated ui schema representation
     */
    public static ObjectNode toUiSchemaElement(final DialogElementRendererSpec spec) {
        if (spec instanceof LayoutRendererSpec layout) {
            return layoutToUiSchemaElement(layout);
        }
        if (spec instanceof LocalizedControlRendererSpec control) {
            return controlToUiSchemaElement(control);
        }
        throw new NotImplementedException("Cannot convert " + spec.getClass().getName() + " to ui schema element.");
    }

    private static ObjectNode layoutToUiSchemaElement(final LayoutRendererSpec layout) {
        final var uiSchemaElement = FACTORY.objectNode()//
            .put(UiSchema.TAG_TYPE, layout.getType());
        layout.getLabel().ifPresent(label -> uiSchemaElement.put(UiSchema.TAG_LABEL, label));
        layout.getDescription().ifPresent(description -> uiSchemaElement.put(UiSchema.TAG_DESCRIPTION, description));
        final var elementsArray = uiSchemaElement.withArray(UiSchema.TAG_ELEMENTS);
        layout.getElements().stream().map(RendererToJsonFormsUtil::toUiSchemaElement).forEach(elementsArray::add);
        return uiSchemaElement;
    }

    private static ObjectNode controlToUiSchemaElement(final LocalizedControlRendererSpec localizedControl) {
        final var control = localizedControl.getControlSpec();
        final var path = localizedControl.getPathWithinValueJsonObject();
        final var uiSchemaElement = FACTORY.objectNode()//
            .put(UiSchema.TAG_TYPE, UiSchema.TYPE_CONTROL)
            .put(UiSchema.TAG_SCOPE, JsonFormsScopeUtil.toScope(path, null));
        final var options = MAPPER.valueToTree(control.getOptions());
        if (options instanceof ObjectNode objectNode) {
            getOrCreateOptions(uiSchemaElement).setAll(objectNode);
        } else {
            CheckUtils.checkState(options instanceof NullNode, "Options need to be an object or null");
        }
        control.getFormat().ifPresent(format -> getOrCreateOptions(uiSchemaElement).put(UiSchema.TAG_FORMAT, format));
        return uiSchemaElement;
    }

    private static ObjectNode getOrCreateOptions(final ObjectNode uiSchemaElement) {
        if (!uiSchemaElement.has(UiSchema.TAG_OPTIONS)) {
            uiSchemaElement.set(UiSchema.TAG_OPTIONS, FACTORY.objectNode());
        }
        return (ObjectNode)uiSchemaElement.get(UiSchema.TAG_OPTIONS);
    }

}
