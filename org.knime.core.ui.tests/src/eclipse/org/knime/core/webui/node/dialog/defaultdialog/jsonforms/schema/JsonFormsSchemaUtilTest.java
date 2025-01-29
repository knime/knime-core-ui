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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */

@SuppressWarnings({"unused", "java:S2698"}) // we accept assertions without messages
class JsonFormsSchemaUtilTest {

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    private static class PropertyNameOverrideTestSetting implements WidgetGroup {
        private static String SNAPSHOT = "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0}}";

        int m_test;
    }

    @Test
    void testPropertyNameOverride() throws JsonProcessingException {
        testSettings(PropertyNameOverrideTestSetting.class);
    }

    private static class TitleTestSetting implements WidgetGroup {
        private static String SNAPSHOT =
            "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"title\":\"some title\",\"default\":0}}";

        @Widget(title = "some title", description = "")
        int test;
    }

    @Test
    void testTitle() throws JsonProcessingException {
        testSettings(TitleTestSetting.class);
    }

    private static class DescriptionSetting implements WidgetGroup {
        private static String SNAPSHOT =
            "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"some description\"}}";

        @Widget(title = "", description = "some description")
        int test;
    }

    @Test
    void testDescription() throws JsonProcessingException {
        testSettings(DescriptionSetting.class);
    }

    static final class ChangeDescription implements WidgetGroup.Modifier {
        static final class FieldReference implements Modification.Reference {
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(FieldReference.class).modifyAnnotation(Widget.class)
                .withProperty("description", "modified description").modify();
        }
    }

    @Modification(ChangeDescription.class)
    private static class ModifiedDescriptionSettings implements WidgetGroup, PersistableSettings {
        /**
         * containing the modified description from {@link ChangeDescription}.
         */
        private static String SNAPSHOT =
            "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"modified description\"}}";

        @Widget(title = "", description = "some description")
        @Modification.WidgetReference(ChangeDescription.FieldReference.class)
        int test;
    }

    @Test
    void testModifiedDescription() throws JsonProcessingException {
        testSettings(ModifiedDescriptionSettings.class);
    }

    private static class ModifiedDescriptionWithinWidgetGroupSettings implements WidgetGroup {
        /**
         * containing the modified description from {@link ChangeDescription}.
         */
        private static String SNAPSHOT = "{\"widgetGroup\":{\"type\":\"object\",\"properties\":"
            + "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"modified description\"}}"
            + "}}";

        static final class FieldReference implements Reference<String> {

        }

        static final class WidgetGroupSettings implements WidgetGroup {

            @Widget(title = "", description = "some description")
            @Modification.WidgetReference(ChangeDescription.FieldReference.class)
            int test;
        }

        @Modification(ChangeDescription.class)
        WidgetGroupSettings m_widgetGroup;

    }

    @Test
    void testModifiedDescriptionWithinWidgetGroup() throws JsonProcessingException {
        testSettings(ModifiedDescriptionWithinWidgetGroupSettings.class);
    }

    private static class ModifiedDescriptionWithinArraySettings implements WidgetGroup {
        /**
         * containing the modified description from {@link ChangeDescription}.
         */
        private static String SNAPSHOT = "{\"widgetGroup\":{\"type\":\"array\",\"items\":" //
            + "{\"type\":\"object\",\"properties\":" //
            + "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"modified description\"}" //
            + "}}}}";

        static final class FieldReference implements Reference<String> {

        }

        static final class WidgetGroupSettings implements WidgetGroup {

            @Widget(title = "", description = "some description")
            @Modification.WidgetReference(ChangeDescription.FieldReference.class)
            int test;
        }

        @Modification(ChangeDescription.class)
        WidgetGroupSettings[] m_widgetGroup;

    }

    @Test
    void testModifiedDescriptionWithinArray() throws JsonProcessingException {
        testSettings(ModifiedDescriptionWithinArraySettings.class);
    }

    private static class ArrayWidgetWithConfigKeysTest implements WidgetGroup {
        /**
         * containing the modified description from {@link ChangeDescription}.
         */
        private static String SNAPSHOT = "{\"widgetGroup\":{\"type\":\"array\"," //
            + "\"configKeys\":[\"configKey\"]," //
            + "\"items\":" //
            + "{\"type\":\"object\",\"properties\":" //
            + "{\"test\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0,\"description\":\"modified description\"}" //
            + "}}}}";

        static final class FieldReference implements Reference<String> {

        }

        static final class WidgetGroupSettings implements WidgetGroup {

            @Widget(title = "", description = "some description")
            @Modification.WidgetReference(ChangeDescription.FieldReference.class)
            int test;
        }

        @Modification(ChangeDescription.class)
        WidgetGroupSettings[] m_configKey;

    }

    @Test
    void testArrayWidgetWithConfigKeys() throws JsonProcessingException {

    }

    @Test
    void testEnum() throws JsonProcessingException {
        class EnumTestSetting implements WidgetGroup {
            private static String SNAPSHOT = "{\"testEnum\":{\"oneOf\":["//
                + "{\"const\":\"SOME_CHOICE\",\"title\":\"Some choice\"},"//
                + "{\"const\":\"SOME_OTHER_CHOICE\",\"title\":\"second choice\"}"//
                + "]}}";

            enum TestEnum {
                    SOME_CHOICE, //
                    @Label("second choice")
                    SOME_OTHER_CHOICE
            }

            TestEnum testEnum;
        }
        testSettings(EnumTestSetting.class);
    }

    @Test
    void testEnumThrowsWhenUsingWidgetAnnotation() throws JsonProcessingException {

        class EnumTestSettingWidgetAnnotation implements WidgetGroup {
            enum TestEnum {
                    SOME_CHOICE, //
                    @Widget(title = "second choice", description = "")
                    SOME_OTHER_CHOICE
            }

            TestEnum testEnum;
        }
        assertThrows(IllegalStateException.class, () -> testSettings(EnumTestSettingWidgetAnnotation.class));
    }

    @Test
    void testEnumConstantDescriptionsAdded() throws JsonProcessingException {

        class EnumTestSettingDescription implements WidgetGroup {
            private static String SNAPSHOT =
                "{\"testEnum\":{" + "\"description\": \"An enum to test if the constants are added automatically."
                    + "\\n<ul>\\n<li><b>First choice</b>: The first choice.</li>"
                    + "\\n<li><b>Second choice</b>: The second choice.</li>\\n</ul>\"," + "\"oneOf\":["//
                    + "{\"const\":\"SOME_CHOICE\",\"title\":\"First choice\"},"//
                    + "{\"const\":\"SOME_OTHER_CHOICE\",\"title\":\"Second choice\"}"//
                    + "], \"title\":\"Test Enum\"}}";

            enum TestEnum {
                    @Label(value = "First choice", description = "The first choice.")
                    SOME_CHOICE, //
                    @Label(value = "Second choice", description = "The second choice.")
                    SOME_OTHER_CHOICE;
            }

            @Widget(title = "Test Enum", description = "An enum to test if the constants are added automatically.")
            TestEnum testEnum;
        }

        testSettings(EnumTestSettingDescription.class);
    }

    @Test
    void testEnumConstantDescriptionsOnlyAddedIfAtLeastOneIsDefined() throws JsonProcessingException {

        class EnumTestSettingDescription implements WidgetGroup {
            private static String SNAPSHOT = "{\"testEnum\":{"
                + "\"description\": \"An enum to test if the constants are added if any constant is described."
                + "\\n<ul>" + "\\n<li><b>First choice</b>: The first choice.</li>" + "\\n<li><b>Second choice</b></li>"
                + "\\n</ul>" + "\"," + "\"oneOf\":["//
                + "{\"const\":\"SOME_CHOICE\",\"title\":\"First choice\"},"//
                + "{\"const\":\"SOME_OTHER_CHOICE\",\"title\":\"Second choice\"}"//
                + "], \"title\":\"Test Enum\"}}";

            enum TestEnum {
                    @Label(value = "First choice", description = "The first choice.")
                    SOME_CHOICE, //
                    @Label("Second choice")
                    SOME_OTHER_CHOICE;
            }

            @Widget(title = "Test Enum",
                description = "An enum to test if the constants are added if any constant is described.")
            TestEnum testEnum;
        }

        testSettings(EnumTestSettingDescription.class);
    }

    private static class ValidatedStringSetting implements WidgetGroup {
        private static String SNAPSHOT = "{"//
            + "\"testMinLength\":{\"type\":\"string\",\"minLength\":0},"//
            + "\"testMaxLength\":{\"type\":\"string\",\"maxLength\":100},"//
            + "\"testPattern\":{\"type\":\"string\",\"pattern\":\"a.*\"},"//
            + "\"testAll\":{\"type\":\"string\",\"minLength\":0,\"maxLength\":100,\"pattern\":\"a.*\"}"//
            + "}";

        @TextInputWidget(minLength = 0)
        public String testMinLength;

        @TextInputWidget(maxLength = 100)
        public String testMaxLength;

        @TextInputWidget(pattern = "a.*")
        public String testPattern;

        @TextInputWidget(minLength = 0, maxLength = 100, pattern = "a.*")
        public String testAll;
    }

    @Test
    void testStringValidationSetting() throws JsonProcessingException {
        testSettings(ValidatedStringSetting.class);
    }

    private static class ContainerSetting implements WidgetGroup {
        private static String SNAPSHOT = "{\"testIntArray\":{"//
            + "\"type\":\"array\","//
            + "\"title\":\"foo\","//
            + "\"items\":{\"type\":\"integer\",\"format\":\"int32\"}"//
            + "}}";

        @Widget(title = "foo", description = "")
        public int[] testIntArray;
    }

    @Test
    void testNoAnnotationsInContainerItems() throws JsonProcessingException {
        testSettings(ContainerSetting.class);
    }

    private static class DefaultSetting implements WidgetGroup {
        private static String SNAPSHOT = "{"//
            + "\"testDouble\":{\"type\":\"number\",\"format\":\"double\",\"default\":0.0},"//
            + "\"testFloat\":{\"type\":\"number\",\"format\":\"float\",\"default\":0.0},"//
            + "\"testInt\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0},"//
            + "\"testLong\":{\"type\":\"integer\",\"format\":\"int64\",\"default\":0},"//
            + "\"testBoolean\":{\"type\":\"boolean\",\"default\":false},"//
            + "\"testNoDefault\":{\"type\":\"string\"},"//
            + "\"testString\":{\"type\":\"string\",\"default\":\"foo\"},"//
            + "\"testArray\":{\"type\":\"array\",\"default\":[{\"testInt\":0}],"//
            + "\"items\":{\"type\":\"object\",\"properties\":"
            + "{\"testInt\":{\"default\":0,\"type\":\"integer\",\"format\":\"int32\"}}}}"//
            + "}";

        public double testDouble;

        public float testFloat;

        public int testInt;

        public long testLong;

        public boolean testBoolean;

        public String testNoDefault;

        public String testString = "foo";

        public IntWithDefault[] testArray = {new IntWithDefault()};
    }

    private static class IntWithDefault {
        public int testInt;
    }

    @Test
    void testDefault() throws JsonProcessingException {
        testSettings(DefaultSetting.class);
    }

    private static class IgnoreSetting implements WidgetGroup {
        private static String SNAPSHOT = "{\"testInt\":{\"type\":\"integer\",\"format\":\"int32\",\"default\":0}}";

        public int testInt;

        public Boolean testBoxedBoolean;

        public Integer testBoxedInteger;

        public Long testBoxedlong;

        public short testShort;

        public Short testBoxedShort;

        public Double testBoxedDouble;

        public Float testBoxedFloat;
    }

    @Test
    void testIgnore() throws JsonProcessingException {
        testSettings(IgnoreSetting.class);
    }

    private record MyStringWrapper(String m_test) {

        @JsonCreator
        MyStringWrapper(final String m_test) {
            this.m_test = m_test;
        }

        @JsonValue
        String toJSON() {
            return m_test;
        }

    }

    private static class SettingWithCustomType implements WidgetGroup {

        private static String SNAPSHOT = "{\"test\":{" //
            + "\"type\":\"object\"," //
            + "\"default\":\"42\"" //
            + "}}";

        @Widget(title = "", description = "")
        public MyStringWrapper m_test = new MyStringWrapper("42");
    }

    /**
     * Tests behavior when providing a custom type via jackson annotations.
     *
     * @throws JsonProcessingException
     */
    @Test
    void testOverrideType() throws JsonProcessingException {
        testSettingsWithoutContext(SettingWithCustomType.class);
    }

    private static class SettingWithJavaTime implements WidgetGroup {

        private static final String SNAPSHOT = """
                {
                  "duration": {
                    "default": 42.0,
                    "format": "int32",
                    "type": "integer"
                  },
                  "year": {
                    "default": "2006",
                    "format": "int32",
                    "type": "integer"
                  },
                  "instant": {
                    "default": "2006-07-28T10:30:00Z",
                    "format": "date-time",
                    "type": "string"
                  },
                  "localDate": {
                    "default": "2006-07-28",
                    "format": "date",
                    "type": "string"
                  },
                  "localDateTime": {
                    "default": "2006-07-28T10:30:00",
                    "format": "date-time",
                    "type": "string"
                  },
                  "localTime": {
                    "default": "10:30:00",
                    "format": "date-time",
                    "type": "string"
                  },
                  "offsetDateTime": {
                    "default": "2006-07-28T10:30:00Z",
                    "format": "date-time",
                    "type": "string"
                  },
                  "offsetTime": {
                    "default": "10:30Z",
                    "format": "date-time",
                    "type": "string"
                  },
                  "zonedDateTime": {
                    "default": {
                      "dateTime": "2006-07-28T10:30:00",
                      "timeZone": "Z"
                    },
                    "format": "date-time",
                    "type": "string"
                  },
                  "yearMonth": {
                    "default": "2006-07",
                    "type": "string"
                  },
                  "zoneId": {
                    "default": "Europe/Berlin",
                    "type": "string"
                  },
                  "zoneOffset": {
                    "default": "+02:00",
                    "type": "string"
                  },
                  "monthDay": {
                    "default": "--07-28",
                    "type": "string"
                  },
                  "period": {
                    "default": "P16Y7M1D",
                    "type": "string"
                  }
                }
                """;

        // integer
        Duration m_duration = Duration.ofSeconds(42);

        Year m_year = Year.of(2006);

        // string w/ date/date-time format
        LocalDate m_localDate = LocalDate.of(2006, 7, 28);

        LocalTime m_localTime = LocalTime.of(10, 30);

        LocalDateTime m_localDateTime = LocalDateTime.of(2006, 7, 28, 10, 30);

        Instant m_instant = LocalDateTime.of(2006, 7, 28, 10, 30).toInstant(ZoneOffset.UTC);

        OffsetDateTime m_offsetDateTime = LocalDateTime.of(2006, 7, 28, 10, 30).atOffset(ZoneOffset.UTC);

        ZonedDateTime m_zonedDateTime = LocalDateTime.of(2006, 7, 28, 10, 30).atZone(ZoneOffset.UTC);

        OffsetTime m_offsetTime = OffsetTime.of(LocalTime.of(10, 30), ZoneOffset.UTC);

        // string w/o format
        MonthDay m_monthDay = MonthDay.of(7, 28);

        YearMonth m_yearMonth = YearMonth.of(2006, 7);

        ZoneId m_zoneId = ZoneId.of("Europe/Berlin");

        ZoneOffset m_zoneOffset = ZoneOffset.ofHours(2);

        Period m_period = Period.between(LocalDate.of(2006, 7, 28), LocalDate.of(2023, 3, 1));
    }

    /**
     * Tests serialization of built-in java.time types.
     *
     * @throws JsonProcessingException
     */
    @Test
    void testBuiltInJavaTime() throws JsonProcessingException {
        testSettingsWithoutContext(SettingWithJavaTime.class);
    }

    private static void testSettings(final Class<? extends WidgetGroup> settingsClass, final PortObjectSpec... specs)
        throws JsonMappingException, JsonProcessingException {
        assertJSONAgainstSnapshot(getProperties(settingsClass, specs), settingsClass);
    }

    private static void testSettingsWithoutContext(final Class<? extends WidgetGroup> settingsClass)
        throws JsonMappingException, JsonProcessingException {
        assertJSONAgainstSnapshot(getPropertiesWithoutContext(settingsClass), settingsClass);
    }

    private static void assertJSONAgainstSnapshot(final JsonNode content, final Class<?> settingsClass)
        throws JsonMappingException, JsonProcessingException {
        final var actual = MAPPER.writeValueAsString(content);
        try {
            final var expected = (String)settingsClass.getDeclaredField("SNAPSHOT").get(null);
            final var aTree = MAPPER.readTree(actual);
            final var eTree = MAPPER.readTree(expected);
            assertThatJson(aTree).isEqualTo(eTree);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            /**
             * NOSONAR
             */
            Assertions.fail("Problem accessing the SNAPSHOT of settings class " + settingsClass.getSimpleName()
                + " (most likely a problem of the test implementation itself)");
        }
    }

    private static JsonNode getProperties(final Class<? extends WidgetGroup> clazz, final PortObjectSpec... specs) {
        return JsonFormsSchemaUtil.buildSchema(clazz, DefaultNodeSettings.createDefaultNodeSettingsContext(specs),
            JsonFormsDataUtil.getMapper()).get("properties");
    }

    private static JsonNode getPropertiesWithoutContext(final Class<? extends WidgetGroup> clazz) {
        return JsonFormsSchemaUtil.buildIncompleteSchema(clazz, JsonFormsDataUtil.getMapper()).get("properties");
    }

}
