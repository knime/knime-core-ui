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
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.VERSION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PROPERTIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TYPE_OBJECT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.DescriptionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

/**
 *
 * Utility class for creating schema content from a settings POJO class.
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
 * widget annotations (e.g. {@link TextInputWidget}) on the fields in the POJO class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class JsonFormsSchemaUtil {

    private static final Set<Class<?>> PROHIBITED_TYPES =
        Stream.of(Boolean.class, Integer.class, Long.class, short.class, Short.class, Double.class, Float.class)
            .collect(Collectors.toCollection(HashSet::new));

    private JsonFormsSchemaUtil() {
        // utility class
    }

    /**
     * Build a schema from the combination of view and model settings (if both are used).
     *
     * @param settingsClasses the classes
     * @param widgetTrees from which annotations are taken into account
     * @param context the creation context with access to the input ports
     * @param mapper the object mapper to be used
     * @return a schema representation
     */
    public static ObjectNode buildCombinedSchema(
        final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses,
        final Map<SettingsType, Tree<WidgetGroup>> widgetTrees, final DefaultNodeSettingsContext context,
        final ObjectMapper mapper) {
        final var root = mapper.createObjectNode();
        root.put(TAG_TYPE, TYPE_OBJECT);
        final var properties = root.putObject(TAG_PROPERTIES);
        settingsClasses.entrySet().stream() //
            .sorted(Comparator.comparing(Entry::getKey)) //
            .forEachOrdered(e -> properties.set(e.getKey().getConfigKey(),
                buildSchema(e.getValue(), widgetTrees.get(e.getKey()), context, mapper)));
        return root;
    }

    /**
     * Build an incomplete schema from a provided POJO class. The settings are incomplete, since they might be missing
     * some default values and oneOf / anyOf choices, which can only be derived from port object specs.
     *
     * @param settingsClass
     * @param mapper to be used to map to json
     * @return a schema representation of settingsClass
     */
    static ObjectNode buildIncompleteSchema(final Class<? extends WidgetGroup> settingsClass,
        final ObjectMapper mapper) {
        return buildSchema(settingsClass, null, mapper);
    }

    @SuppressWarnings("javadoc") // public for test purposes
    public static ObjectNode buildSchema(final Class<? extends WidgetGroup> settingsClass,
        final DefaultNodeSettingsContext context, final ObjectMapper mapper) {
        final var widgetTree = new WidgetTreeFactory().createTree(settingsClass, SettingsType.MODEL);
        return buildSchema(settingsClass, widgetTree, context, mapper);
    }

    private static ObjectNode buildSchema(final Type settingsClass, final Tree<WidgetGroup> widgetTree,
        final DefaultNodeSettingsContext context, final ObjectMapper mapper) {
        final var builder = new SchemaGeneratorConfigBuilder(mapper, VERSION, new OptionPreset(//
            Option.ADDITIONAL_FIXED_TYPES, //
            Option.EXTRA_OPEN_API_FORMAT_VALUES, //
            Option.FLATTENED_ENUMS, //
            Option.EXTRA_OPEN_API_FORMAT_VALUES, //
            Option.PUBLIC_NONSTATIC_FIELDS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS, //
            Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS, //
            Option.INLINE_ALL_SCHEMAS, //
            Option.ALLOF_CLEANUP_AT_THE_END));

        /**
         * This custom definition provider ensures that the widgetTree parameter in this method, if present, is always
         * the widgetTree containing the next traversed fields.
         */
        builder.forFields().withCustomDefinitionProvider((fieldScope, generationContext) -> {
            if (widgetTree == null) {
                return null;
            }
            Function<Tree<WidgetGroup>, CustomPropertyDefinition> useWidgetTreeForNestedFields =
                wt -> new CustomPropertyDefinition(buildSchema(fieldScope.getType(), wt, context, mapper));

            final var fieldNode = widgetTree.getChildByName(fieldScope.getName());
            return getPropertyDefinition(fieldNode, fieldScope, generationContext, useWidgetTreeForNestedFields);
        });

        builder.forFields()
            .withIgnoreCheck(f -> f.isPrivate() || PROHIBITED_TYPES.contains(f.getType().getErasedType()));

        builder.forFields().withCustomDefinitionProvider(new EnumDefinitionProvider());
        builder.forFields().withDefaultResolver(new DefaultResolver(context));

        builder.forFields().withTitleResolver(field -> retrieveAnnotation(field, Widget.class, widgetTree)
            .map(Widget::title).filter(l -> !field.isFakeContainerItemScope() && !l.isEmpty()).orElse(null));

        builder.forFields().withDescriptionResolver(field -> resolveDescription(field, widgetTree));

        builder.forFields().withPropertyNameOverrideResolver(field -> StringUtils.removeStart(field.getName(), "m_"));

        builder.forFields().withTargetTypeOverridesResolver(JsonFormsSchemaUtil::overrideClass);

        return new SchemaGenerator(builder.build()).generateSchema(settingsClass);
    }

    private static CustomPropertyDefinition getPropertyDefinition(final TreeNode<WidgetGroup> fieldNode,
        final FieldScope fieldScope, final SchemaGenerationContext generationContext,
        final Function<Tree<WidgetGroup>, CustomPropertyDefinition> useWidgetTreeForNestedFields) {
        if (fieldNode instanceof Tree<WidgetGroup> wt) {
            /**
             * If the node is another widgetTree, we use it for the traversal of the nested fields.
             */
            return useWidgetTreeForNestedFields.apply(wt);
        } else if (fieldNode instanceof ArrayParentNode<WidgetGroup> awn) {
            /**
             * If the node is an array widget node, the next fields are those if the element widget tree.
             */
            return useWidgetTreeForNestedFields.apply(awn.getElementTree());
        } else {
            final var enumDefinition =
                new EnumDefinitionProvider().provideCustomSchemaDefinition(fieldScope, generationContext);
            if (enumDefinition != null) {
                return enumDefinition;
            }
            /**
             * Here we leave the widget tree to traverse nested fields of widget nodes further.
             */
            return useWidgetTreeForNestedFields.apply(null);
        }
    }

    /**
     * We use the widget tree to retrieve annotation whenever it is possible, since the annotations are pre-processed
     * (e.g. {@link Modification}s are resolved there).
     */
    private static <T extends Annotation> Optional<T> retrieveAnnotation(final FieldScope fieldScope,
        final Class<T> annotationClass, final Tree<WidgetGroup> widgetTree) {
        if (widgetTree != null) {
            final var widgetTreeNode = widgetTree.getChildByName(fieldScope.getName());
            if (widgetTreeNode != null) {
                if (widgetTreeNode.getPossibleAnnotations().contains(annotationClass)) {
                    return widgetTreeNode.getAnnotation(annotationClass);
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.ofNullable(fieldScope.getAnnotationConsideringFieldAndGetter(annotationClass));
    }

    private static String resolveDescription(final FieldScope fieldScope, final Tree<WidgetGroup> widgetTree) {
        var type = fieldScope.getType().getErasedType();
        return retrieveAnnotation(fieldScope, Widget.class, widgetTree)//
            .filter(w -> !fieldScope.isFakeContainerItemScope())//
            .flatMap(w -> resolveDescription(w, type))//
            .orElse(null);
    }

    /**
     * Resolves the description from a widget. In case of enums, the description of the enum constants are added as list
     * to the description of the enum setting.
     *
     * @param widget annotation of a setting
     * @param fieldType type of the setting
     * @return the description if it is present
     */
    public static Optional<String> resolveDescription(final Widget widget, final Class<?> fieldType) {
        var description = widget.description();
        if (description.isEmpty()) {
            return Optional.empty();
        }
        if (fieldType.isEnum()) {
            description += getConstantList(fieldType);
        }
        return Optional.of(description);
    }

    private static <E extends Enum<E>> String getConstantList(final Class<?> erasedEnumType) {
        @SuppressWarnings("unchecked") // the calling method checks that erasedEnumType is an enum
        var enumClass = (Class<E>)erasedEnumType;
        var constantEntries = EnumDefinitionProvider.getEnumConstantDescription(enumClass);
        return DescriptionUtil.getDescriptionsUlString(constantEntries);
    }

    private static List<ResolvedType> overrideClass(final FieldScope field) {
        // override class regardless of @Widget annotation
        if (field.isFakeContainerItemScope()) {
            return Collections.emptyList();
        }
        return resolveCustomTypes(field);
    }

    private static List<ResolvedType> resolveCustomTypes(final FieldScope field) {
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
            || ZoneOffset.class.equals(fieldClass) || Period.class.equals(fieldClass)
            || DataType.class.equals(fieldClass)) {
            // make `{... "type":"object"}` become `{... "type":"string"}`
            return List.of(ctx.resolve(String.class));
        }
        return Collections.emptyList();
    }
}
