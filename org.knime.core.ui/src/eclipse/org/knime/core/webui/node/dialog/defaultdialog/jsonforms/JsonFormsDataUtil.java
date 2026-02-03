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
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.CredentialsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.DataTypeSerializationUtil;
import org.knime.node.parameters.NodeParameters;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class mainly for creating json-forms data content from a {@link NodeParameters} POJO and vice-versa.
 *
 * The following fields in a POJO are ignored:
 * <ul>
 * <li>Private fields without a getter method</li>
 * <li>Fields annotated with @JsonIgnore</li>
 * <li>Fields whose getters are annotated with. @JsonIgnore</li>
 * <li>Fields with {@code null} value</li>
 * </ul>
 * The translation furthermore follows these rules (from POJO to JSON; the inverse rules in the other direction):
 * <ul>
 * <li>"m_"-prefixes are removed.</li>
 * <li>Enums and BigDecimals are serialized as their string value.</li>
 * <li>Nested fields are translated to a nested JSON structure.</li>
 * </ul>
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class JsonFormsDataUtil {

    private static ObjectMapper MAPPER; // NOSONAR

    private JsonFormsDataUtil() {
        //utility class
    }

    private static ObjectMapper createMapper() {
        final var mapper = new ObjectMapper();

        mapper.registerModule(new Jdk8Module());

        mapper.registerModule(new JavaTimeModule());
        // If this serialization feature would be _enabled_, and we would not write timestamps as int,
        // this would lead to the date being written as an array in LocalDateSerializer.java
        // which is displayed by the text input as `2023,3,3`.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // By default periods/durations are always serialized as (numeric) timestamps, see
        // {@link SerializationFeature#WRITE_DURATIONS_AS_TIMESTAMPS}

        mapper.registerModule(createDialogModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NON_PRIVATE);
        mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);

        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public String nameForField(final MapperConfig<?> config, final AnnotatedField field,
                final String defaultName) {
                return StringUtils.removeStart(defaultName, "m_");
            }
        });
        return mapper;
    }

    private static SimpleModule createDialogModule() {
        final var module = new SimpleModule();

        module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        CredentialsUtil.addSerializerAndDeserializer(module);
        FSLocationJsonSerializationUtil.addSerializerAndDeserializer(module);
        DataTypeSerializationUtil.addSerializerAndDeserializer(module);

        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());

        return module;
    }

    /**
     * @return the configured mapper which is used to serialize {@link NodeParameters}-objects
     */
    public static ObjectMapper getMapper() {
        if (MAPPER == null) {
            MAPPER = createMapper();
        }
        return MAPPER;
    }

    /**
     * @param settings to convert to JSON
     * @return the JSON node representing settings
     */
    public static JsonNode toJsonData(final NodeParameters settings) {
        return getMapper().valueToTree(settings);
    }

    static JsonNode toCombinedJsonData(final Map<SettingsType, NodeParameters> settings) {
        final var root = getMapper().createObjectNode();
        settings.entrySet().stream() //
            .sorted(Comparator.comparing(Entry::getKey)) //
            .forEachOrdered(e -> {
                root.set(e.getKey().getConfigKeyFrontend(), toJsonData(e.getValue()));
            });
        return root;
    }

    /**
     * @param <T> the type of the default node settings
     * @param jsonFormsData a json representation of the default node settings
     * @param clazz the specific class of the default node settings
     * @return an instance of type <T> deserialized from the json representation.
     * @throws JsonProcessingException
     */
    public static <T extends NodeParameters> T toDefaultNodeSettings(final JsonNode jsonFormsData, final Class<T> clazz)
        throws JsonProcessingException {
        return getMapper().treeToValue(jsonFormsData, clazz);
    }

    private static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final BigDecimal value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            gen.writeNumber(value.toPlainString());
        }

    }

    private static class ZonedDateTimeSerializer extends JsonSerializer<ZonedDateTime> {

        @Override
        public void serialize(final ZonedDateTime value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {

            // extract wall time
            var wallTime = value.toLocalDateTime();

            gen.writeStartObject();
            gen.writeStringField("dateTime", wallTime.format(DateTimeFormatter.ISO_DATE_TIME));
            gen.writeStringField("timeZone", value.getZone().getId());
            gen.writeEndObject();
        }
    }

    private static class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

        @Override
        public ZonedDateTime deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {

            JsonNode node = p.getCodec().readTree(p);

            var dateTimeText = node.get("dateTime").asText();
            var timeZoneText = node.get("timeZone").asText();

            // ISO_DATE_TIME can deal with formats with or without timezone information,
            // then we extract the wall time and discard any redundant tz data.
            var dateTime = LocalDateTime.parse(dateTimeText, DateTimeFormatter.ISO_DATE_TIME);
            var timeZone = ZoneId.of(timeZoneText);

            return ZonedDateTime.of(dateTime, timeZone);
        }
    }

    /**
     * Utility method for modifying json data at specific location(s).
     *
     * The location is specified via a settings type followed by a list of paths and indices. The list of indices can be
     * shorter than paths.size() - 1. When indices run out, the traversal branches into every element of the current
     * array, setting the value at all matching locations.
     *
     * For example, given the json
     *
     * <pre>
     * {
     *   "model": {
     *     "arrayField": [
     *       {
     *         "nestedField": "foo"
     *       }, {
     *         "nestedField": "bar"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     *
     * and
     * <ul>
     * <li>settingsType = SettingsType.MODEL</li>
     * <li>paths = [ ["arrayField"], ["nestedField"] ]</li>
     * <li>indices = [1]</li>
     * <li>value = "baz" (as JsonNode)</li>
     * </ul>
     *
     * the resulting json will be
     *
     * <pre>
     * {
     *   "model": {
     *     "arrayField": [
     *       {
     *         "nestedField": "foo"
     *       }, {
     *         "nestedField": "baz"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     *
     * If indices were empty [], the value would be set at both nestedField locations.
     *
     * @param jsonData the json data to modify
     * @param settingsType the settings type
     * @param paths the paths to the location(s) to set the value at
     * @param indices the indices of the array elements to set the value at (can be shorter than paths.size() - 1)
     * @param value the value to set, already converted to a JsonNode
     */
    public static void setValueAtLocation(final ObjectNode jsonData, final SettingsType settingsType,
        final List<List<String>> paths, final List<Integer> indices, final JsonNode value) {
        final var rootNode = jsonData.get(settingsType.getConfigKeyFrontend());
        final var parentPathsAndLastSegment = toParentPathsAndLastSegment(paths);
        final var parentPaths = parentPathsAndLastSegment.getFirst();
        final var lastSegment = parentPathsAndLastSegment.getSecond();

        traverseWithIndices(rootNode, parentPaths, indices, (indicesUsed, node) -> {
            if (node instanceof ObjectNode obj) {
                obj.set(lastSegment, value);
            } else {
                throw new IllegalArgumentException("Cannot set value - parent is not an object node");
            }
        });
    }

    /**
     * Convert a list of strings to a json pointer. The strings must not contain '/' characters.
     *
     * @param path the path as list of strings
     * @return the json pointer
     */
    public static String toJsonPointer(final List<String> path) {
        if (path.isEmpty()) {
            return "";
        }
        return "/" + String.join("/", path);
    }

    /**
     * Traverses JSON data at a path with indices, handling branching when indices are exhausted. When indices run out
     * before paths do, the traversal branches into every element of the current array.
     *
     * @param jsonNode the JSON node to traverse
     * @param paths the paths to traverse (each path is a list of field names)
     * @param indices the indices for array elements (can be shorter than paths.size() - 1)
     * @return list of (indices used, final JsonNode) pairs
     */
    public static List<Pair<List<Integer>, JsonNode>> traverseWithIndices(final JsonNode jsonNode,
        final List<List<String>> paths, final List<Integer> indices) {
        return traverseWithIndicesInternal(jsonNode, paths, indices);
    }

    /**
     * Traverses JSON data at a path with indices, calling the consumer for each reached node.
     *
     * @param jsonNode the JSON node to traverse
     * @param paths the paths to traverse (each path is a list of field names)
     * @param indices the indices for array elements (can be shorter than paths.size() - 1)
     * @param consumer called for each (indices used, node) pair reached
     */
    public static void traverseWithIndices(final JsonNode jsonNode, final List<List<String>> paths,
        final List<Integer> indices, final BiConsumer<List<Integer>, JsonNode> consumer) {
        traverseWithIndicesInternal(jsonNode, paths, indices).forEach(pair -> consumer.accept(pair.getFirst(), pair.getSecond()));
    }

    private static List<Pair<List<Integer>, JsonNode>> traverseWithIndicesInternal(final JsonNode jsonNode,
        final List<List<String>> paths, final List<Integer> indices) {
        if (paths.isEmpty()) {
            return List.of(new Pair<>(List.of(), jsonNode));
        }

        final var firstPath = paths.get(0);
        final var atFirstPath = firstPath.isEmpty() ? jsonNode : jsonNode.at(toJsonPointer(firstPath));
        if (paths.size() == 1) {
            return List.of(new Pair<>(List.of(), atFirstPath));
        }

        CheckUtils.checkState(atFirstPath.isArray(), "Json node at field with nested path should be an array.");
        final var restPaths = paths.subList(1, paths.size());

        if (!indices.isEmpty()) {
            // Use the provided index
            final var restIndices = indices.subList(1, indices.size());
            return traverseWithIndicesInternal(atFirstPath.get(indices.get(0)), restPaths, restIndices);
        }

        // No more indices - branch into all array elements
        return IntStream.range(0, atFirstPath.size()).mapToObj(i -> i)
            .flatMap(i -> traverseWithIndicesInternal(atFirstPath.get(i), restPaths, List.of()).stream().map(pair -> {
                final var newIndices = Stream.concat(Stream.of(i), pair.getFirst().stream()).toList();
                return new Pair<>(newIndices, pair.getSecond());
            })).toList();
    }

    /**
     * Splits the paths into the parent paths and the last segment. For setValueAtLocation, we need to traverse to the
     * parent and then set the value using the last segment.
     *
     * @param paths the full paths
     * @return a pair of (parentPaths, lastSegment)
     */
    private static Pair<List<List<String>>, String> toParentPathsAndLastSegment(final List<List<String>> paths) {
        CheckUtils.checkArgument(!paths.isEmpty(), "Paths must not be empty");
        final var lastPath = paths.get(paths.size() - 1);
        CheckUtils.checkArgument(!lastPath.isEmpty(), "Last path must not be empty");

        final var lastSegment = lastPath.get(lastPath.size() - 1);
        final var parentLastPath = lastPath.subList(0, lastPath.size() - 1);

        if (parentLastPath.isEmpty() && paths.size() == 1) {
            // Single path with single segment - parent is the root
            return new Pair<>(List.of(List.of()), lastSegment);
        }

        final var parentPaths = Stream.concat(paths.subList(0, paths.size() - 1).stream(), Stream.of(parentLastPath))
            .toList();
        return new Pair<>(parentPaths, lastSegment);
    }

}
