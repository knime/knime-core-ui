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
 *   19 Oct 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.schema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.VERSION;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.ConfigKeyUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

/**
 *
 * Utility class for creating a JSON schema from a settings POJO class.
 *
 * The JSON Forms schema mimics the structure of the Json Forms data while providing the following information for the
 * respective data entries:
 * <ul>
 * <li>type</li>
 * <li>title</li>
 * <li>description</li>
 * <li>validity</li>
 * </ul>
 * The type is recognized automatically using the same mapper between POJO and json as in {@link JsonFormsDataUtil}.
 *
 * The other information can be controlled by using the {@link Widget @Widget} annotation and other field specific
 * widget annotations (e.g. {@link NumberInputWidget}) on the fields in the POJO class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class JsonSchemaExtractor {

    private static final Set<Class<?>> PROHIBITED_TYPES =
        Stream.of(Boolean.class, Integer.class, Long.class, short.class, Short.class, Double.class, Float.class)
            .collect(Collectors.toCollection(HashSet::new));

    private final Function<FieldScope, PropertyAccessor> m_propertyExtractorFactory;

    private final ObjectMapper m_mapper;

    /**
     * @param context of the node for which settings are created. Pass null if there is no context available.
     * @return a JsonSchemaExtractor that can extract the JsonSchema from classes that use the Parameter annotations
     */
    public static JsonSchemaExtractor createParameterSchemaExtractor(final NodeSettingsCreationContext context,
        final ObjectMapper mapper) {
        return new JsonSchemaExtractor(f -> new ParameterPropertyAccessor(f, context), mapper);
    }

    public static JsonSchemaExtractor createWidgetSchemaExtractor(final DefaultNodeSettingsContext context,
        final ObjectMapper mapper) {
        return new JsonSchemaExtractor(f -> new WidgetPropertyAccessor(f, context), mapper);
    }

    JsonSchemaExtractor(final Function<FieldScope, PropertyAccessor> propertyExtractorFactory,
        final ObjectMapper mapper) {
        m_propertyExtractorFactory = propertyExtractorFactory;
        m_mapper = mapper;
    }

    interface PropertyAccessor {

        Optional<String> getTitle();

        Optional<String> getDescription();

        Optional<BigDecimal> getMin();

        Optional<BigDecimal> getMax();

        Optional<Integer> getStringMinLength();

        Optional<Integer> getStringMaxLength();

        Optional<String> getStringPattern();

        Optional<Object> getDefault();

    }

    private Optional<PropertyAccessor> extractAccessor(final FieldScope field) {
        return field.isFakeContainerItemScope() ? Optional.empty()
            : Optional.of(m_propertyExtractorFactory.apply(field));
    }

    private <T> ConfigFunction<FieldScope, T>
        createExtractor(final Function<PropertyAccessor, Optional<T>> propertyAccess) {
        return field -> extractAccessor(field).flatMap(propertyAccess).orElse(null);
    }

    @SuppressWarnings("javadoc")
    public ObjectNode buildSchema(final Class<?> settingsClass) {
        final var builder = new SchemaGeneratorConfigBuilder(m_mapper, VERSION, new OptionPreset(//
            Option.ADDITIONAL_FIXED_TYPES, //
            Option.EXTRA_OPEN_API_FORMAT_VALUES, //
            Option.FLATTENED_ENUMS, //
            Option.EXTRA_OPEN_API_FORMAT_VALUES, //
            Option.PUBLIC_NONSTATIC_FIELDS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS, //
            Option.INLINE_ALL_SCHEMAS, //
            Option.ALLOF_CLEANUP_AT_THE_END));

        builder.forFields()
            .withIgnoreCheck(f -> f.isPrivate() || PROHIBITED_TYPES.contains(f.getType().getErasedType()));

        builder.forFields().withCustomDefinitionProvider(new EnumDefinitionProvider());

        builder.forFields().withDefaultResolver(createExtractor(PropertyAccessor::getDefault));

        builder.forFields().withTitleResolver(createExtractor(PropertyAccessor::getTitle));

        builder.forFields().withDescriptionResolver(createExtractor(PropertyAccessor::getDescription));

        builder.forFields().withNumberInclusiveMinimumResolver(createExtractor(PropertyAccessor::getMin));

        builder.forFields().withNumberInclusiveMaximumResolver(createExtractor(PropertyAccessor::getMax));

        builder.forFields().withStringMinLengthResolver(createExtractor(PropertyAccessor::getStringMinLength));

        builder.forFields().withStringMaxLengthResolver(createExtractor(PropertyAccessor::getStringMaxLength));

        builder.forFields().withStringPatternResolver(createExtractor(PropertyAccessor::getStringPattern));

        builder.forFields().withPropertyNameOverrideResolver(field -> StringUtils.removeStart(field.getName(), "m_"));

        builder.forFields().withInstanceAttributeOverride(JsonSchemaExtractor::addConfigKeys);

        builder.forFields().withTargetTypeOverridesResolver(JsonSchemaExtractor::overrideClass);

        return new SchemaGenerator(builder.build()).generateSchema(settingsClass);
    }

    /** Add a "configKeys" array to the field if a custom persistor is used */
    private static void addConfigKeys(final ObjectNode node, final FieldScope field,
        final SchemaGenerationContext context) {
        var configKeys = ConfigKeyUtil.getConfigKeysUsedByField(field.getRawMember());
        if (configKeys.length > 0) {
            var configKeysNode = context.getGeneratorConfig().createArrayNode();
            Arrays.stream(configKeys).forEach(configKeysNode::add);
            node.set("configKeys", configKeysNode);
        }
    }

    // TODO JSON Forms specific details should not live here
    private static List<ResolvedType> overrideClass(final FieldScope field) {
        // override class regardless of @Widget annotation
        if (field.isFakeContainerItemScope()) {
            return Collections.emptyList();
        }
        return javaTimeToNumeric(field);
    }

    // TODO this is a JSON Forms detail. We could either expose the builder
    // or inject the necessary logic similar to the propertyExtractorFactory
    private static List<ResolvedType> javaTimeToNumeric(final FieldScope field) {
        final var ctx = field.getContext();
        final var fieldClass = field.getDeclaredType().getErasedType();
        // Make java.time types that are not supported out-of-the-box by JSONForms map to supported fallback types
        // explicitly, otherwise they get mapped to "object" and result in "No applicable renderer found".
        if (Duration.class.equals(fieldClass) || Year.class.equals(fieldClass)) {
            // Make `{... "type":"object"}` become `{... "format":"int32","type":"integer"}`,
            // otherwise we get "No applicable renderer found", even if we overwrite the config for e.g. Duration
            // on the mapper in the *DataUtil:
            //  mapper.configOverride(Duration.class)
            //    .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)); // NOSONAR
            return List.of(ctx.resolve(int.class));
        }
        if (MonthDay.class.equals(fieldClass) || YearMonth.class.equals(fieldClass)
            || ZoneOffset.class.equals(fieldClass) || Period.class.equals(fieldClass)) {
            // make `{... "type":"object"}` become `{... "type":"string"}`
            return List.of(ctx.resolve(String.class));
        }
        return Collections.emptyList();
    }
}
