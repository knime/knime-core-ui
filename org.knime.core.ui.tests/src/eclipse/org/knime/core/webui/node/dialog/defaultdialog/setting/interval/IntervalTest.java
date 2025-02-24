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
 *   Nov 21, 2024 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test for {@link Interval}
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"static-method", "squid:S1144"}) // suppress "unused private method" warning
final class IntervalTest {

    private static record ParseTestCase(Interval expected, String iso, String... humanReadable) {
    }

    private static record FormatTestCase(String expected, String expectedLong, String expectedShort, Interval input) {
    }

    private static record AdditionTestCase(Temporal startPosition, Interval interval, Temporal expectedEndPosition) {
    }

    private static final List<ParseTestCase> TEST_CASES_PERIOD_PARSE = List.of( //
        new ParseTestCase(DateInterval.of(1, 0, 0, 0), "P1Y", "1 year", "1 years", "1y"),
        new ParseTestCase(DateInterval.of(0, 1, 0, 0), "P1M", "1 month", "1 months", "1mo"),
        new ParseTestCase(DateInterval.of(0, 0, 1, 0), "P1W", "1 week", "1 weeks", "1w"),
        new ParseTestCase(DateInterval.of(0, 0, 0, 1), "P1D", "1 day", "1 days", "1d"),
        new ParseTestCase(DateInterval.of(1, 2, 3, 4), "P1Y2M3W4D", "1 year 2 months 3 weeks 4 days", "1y2m3w4d",
            "1y 2m 3w 4d", "1 y 2 m 3 w 4 d", "1Y2M3W4D"),
        new ParseTestCase(DateInterval.of(-1, -2, -3, -4), "-P1Y2M3W4D") //
    );

    private static final List<ParseTestCase> TEST_CASES_DURATION_PARSE = List.of( //
        new ParseTestCase(TimeInterval.of(1, 0, 0, 0), "PT1H", "1 hour", "1 hours", "1h"),
        new ParseTestCase(TimeInterval.of(0, 1, 0, 0), "PT1M", "1 minute", "1 minutes", "1min"),
        new ParseTestCase(TimeInterval.of(0, 0, 1, 0), "PT1S", "1 second", "1seconds", "1sec", "1s"),
        new ParseTestCase(TimeInterval.of(0, 0, 0, 1), "PT0.001S", "0.001 second"),
        new ParseTestCase(TimeInterval.of(0, 0, 0, 1), "PT0,001S", "0,001 second"),
        new ParseTestCase(TimeInterval.of(1, 2, 3, 4), "PT1H2M3.004S", "1 hour 2 minutes 3.004 seconds", "1h2m3.004s",
            "1h 2m 3.004s", "1 h 2 m 3.004 s", "1H2M3.004S"),
        new ParseTestCase(TimeInterval.of(0, 0, 3, 400), "PT3.4S", "3.4 seconds", "3.4s", "3.4 sec", "3.4 secs",
            "3.4s"),
        new ParseTestCase(TimeInterval.of(0, 0, 0, 400), "PT0.4S", "0.4 seconds", "0.4s", "0.4 sec", "0.4 secs",
            "0.4s"),
        new ParseTestCase(TimeInterval.of(-1, -2, -3, -4), "-PT1H2M3.004S") //
    );

    private static final List<FormatTestCase> TEST_CASES_ISO_FORMAT = List.of( //
        new FormatTestCase("P1Y0M0W0D", "1 year", "1y", DateInterval.of(1, 0, 0, 0)),
        new FormatTestCase("P0Y1M0W0D", "1 month", "1M", DateInterval.of(0, 1, 0, 0)),
        new FormatTestCase("P0Y0M1W0D", "1 week", "1w", DateInterval.of(0, 0, 1, 0)),
        new FormatTestCase("P0Y0M0W1D", "1 day", "1d", DateInterval.of(0, 0, 0, 1)),
        new FormatTestCase("P1Y2M3W4D", "1 year 2 months 3 weeks 4 days", "1y 2M 3w 4d", DateInterval.of(1, 2, 3, 4)),
        new FormatTestCase("-P1Y2M3W4D", "-(1 year 2 months 3 weeks 4 days)", "-(1y 2M 3w 4d)",
            DateInterval.of(-1, -2, -3, -4)),
        new FormatTestCase("P-1Y2M-3W0D", "-1 year 2 months -3 weeks", "-1y 2M -3w", DateInterval.of(-1, 2, -3, 0)),
        new FormatTestCase("-P4Y0M0W0D", "-4 years", "-4y", DateInterval.of(-4, 0, 0, 0)),
        new FormatTestCase("PT1H0M0.000S", "1 hour", "1h", TimeInterval.of(1, 0, 0, 0)),
        new FormatTestCase("PT0H1M0.000S", "1 minute", "1m", TimeInterval.of(0, 1, 0, 0)),
        new FormatTestCase("PT0H0M1.000S", "1 second", "1s", TimeInterval.of(0, 0, 1, 0)),
        new FormatTestCase("PT0H0M0.001S", "0.001 seconds", "0.001s", TimeInterval.of(0, 0, 0, 1)),
        new FormatTestCase("PT1H2M3.004S", "1 hour 2 minutes 3.004 seconds", "1h 2m 3.004s",
            TimeInterval.of(1, 2, 3, 4)),
        new FormatTestCase("-PT1H2M3.004S", "-(1 hour 2 minutes 3.004 seconds)", "-(1h 2m 3.004s)",
            TimeInterval.of(-1, -2, -3, -4)), //
        new FormatTestCase("PT-1H2M-3.000S", "-1 hour 2 minutes -3 seconds", "-1h 2m -3s",
            TimeInterval.of(-1, 2, -3, 0)), //
        new FormatTestCase("-PT4H0M0.000S", "-4 hours", "-4h", TimeInterval.of(-4, 0, 0, 0)) //
    );

    private static final List<AdditionTestCase> TEST_CASES_ADDITION = List.of( //
        new AdditionTestCase(LocalDateTime.of(1970, 1, 1, 23, 59, 59), TimeInterval.of(0, 0, 1, 0),
            LocalDateTime.of(1970, 1, 2, 0, 0, 0)), //
        new AdditionTestCase(LocalTime.of(23, 59, 59), TimeInterval.of(0, 0, 1, 0), LocalTime.of(0, 0, 0)), //
        new AdditionTestCase(LocalDate.of(1969, 12, 31), DateInterval.of(0, 0, 0, 1), LocalDate.of(1970, 1, 1)), //
        new AdditionTestCase(LocalDateTime.of(1969, 1, 1, 23, 59, 59), DateInterval.of(0, 0, 0, 1),
            LocalDateTime.of(1969, 1, 2, 23, 59, 59)), //
        new AdditionTestCase(LocalTime.of(23, 59, 59), TimeInterval.of(0, 0, -1, 0), LocalTime.of(23, 59, 58)), //
        new AdditionTestCase(LocalDate.of(1996, 2, 28), DateInterval.of(0, 0, 1, 0), LocalDate.of(1996, 3, 6)), // leap year case
        new AdditionTestCase(LocalDate.of(1997, 2, 28), DateInterval.of(0, 0, 1, 0), LocalDate.of(1997, 3, 7)), // non-leap year case
        new AdditionTestCase(
            ZonedDateTime.of(2024, 10, 27, 2, 59, 59, 0, ZoneId.of("Europe/Berlin")).withEarlierOffsetAtOverlap(),
            TimeInterval.of(0, 0, 1, 0),
            ZonedDateTime.of(2024, 10, 27, 2, 0, 0, 0, ZoneId.of("Europe/Berlin")).withLaterOffsetAtOverlap()), // DST clocks back
        new AdditionTestCase(ZonedDateTime.of(2024, 03, 31, 1, 59, 59, 0, ZoneId.of("Europe/Berlin")),
            TimeInterval.of(0, 0, 1, 0), ZonedDateTime.of(2024, 03, 31, 3, 0, 0, 0, ZoneId.of("Europe/Berlin"))) // DST clocks forward
    );

    private static Stream<Arguments> provideArgumentsForIsoPeriodParse() {
        return TEST_CASES_PERIOD_PARSE.stream()
            .map(tc -> Arguments.of("Parsing '%s'".formatted(tc.iso), tc.expected, tc.iso));
    }

    private static Stream<Arguments> provideArgumentsForHumanReadablePeriodParse() {
        return TEST_CASES_PERIOD_PARSE.stream().map(tc -> Arrays.stream(tc.humanReadable)
            .map(hr -> Arguments.of("Parsing '%s'".formatted(hr), tc.expected, hr))).flatMap(Function.identity());
    }

    private static Stream<Arguments> provideArgumentsForIsoDurationParse() {
        return TEST_CASES_DURATION_PARSE.stream()
            .map(tc -> Arguments.of("Parsing '%s'".formatted(tc.iso), tc.expected, tc.iso));
    }

    private static Stream<Arguments> provideArgumentsForHumanReadableDurationParse() {
        return TEST_CASES_DURATION_PARSE.stream().map(tc -> Arrays.stream(tc.humanReadable)
            .map(hr -> Arguments.of("Parsing '%s'".formatted(hr), tc.expected, hr))).flatMap(Function.identity());
    }

    private static Stream<Arguments> provideArgumentsForFormat() {
        return TEST_CASES_ISO_FORMAT.stream().map(tc -> Arguments.of("Formatting '%s'".formatted(tc.input), tc.expected,
            tc.expectedLong, tc.expectedShort, tc.input));
    }

    private static Stream<Arguments> provideArgumentsForAddition() {
        return TEST_CASES_ADDITION.stream()
            .map(tc -> Arguments.of("Adding '%s' to '%s'".formatted(tc.interval, tc.startPosition), tc.startPosition,
                tc.interval, tc.expectedEndPosition));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"provideArgumentsForIsoPeriodParse", "provideArgumentsForIsoDurationParse"})
    void testIsoParse(@SuppressWarnings("unused") final String testName, final Interval expected, final String iso) {
        assertEquals(expected, Interval.parseISO(iso));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"provideArgumentsForHumanReadablePeriodParse", "provideArgumentsForHumanReadableDurationParse"})
    void testHumanReadableParse(@SuppressWarnings("unused") final String testName, final Interval expected,
        final String humanReadable) {
        assertEquals(expected, Interval.parseHumanReadable(humanReadable));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({ //
        "provideArgumentsForIsoPeriodParse", //
        "provideArgumentsForHumanReadablePeriodParse", //
        "provideArgumentsForIsoDurationParse", //
        "provideArgumentsForHumanReadableDurationParse" //
    })
    void testCombinedParse(@SuppressWarnings("unused") final String testName, final Interval expected,
        final String input) {
        assertEquals(expected, Interval.parseHumanReadableOrIso(input));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideArgumentsForFormat")
    void testFormat(@SuppressWarnings("unused") final String testName, final String expected, final String expectedLong,
        final String expectedShort, final Interval input) {
        assertEquals(expected, input.toISOString());
        assertEquals(expectedLong, input.toLongHumanReadableString());
        assertEquals(expectedShort, input.toShortHumanReadableString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideArgumentsForAddition")
    void testAddition(@SuppressWarnings("unused") final String testName, final Temporal startPosition,
        final Interval interval, final Temporal expectedEndPosition) {
        final var added = interval.addTo(startPosition);
        assertEquals(expectedEndPosition, interval.addTo(startPosition));
        final var addedAndSubstracted = interval.subtractFrom(added);
        assertEquals(startPosition, addedAndSubstracted);
    }

    @Test
    void testDateIntervalEquality() {
        assertEquals(DateInterval.of(1, 2, 3, 4), DateInterval.of(1, 2, 3, 4));
        assertEquals(DateInterval.of(1, 2, 3, 4), Interval.parseISO("P1Y2M3W4D"));
        assertEquals(DateInterval.of(-1, -2, -3, -4), Interval.parseISO("-P1Y2M3W4D"));

        assertNotEquals(DateInterval.of(1, 2, 3, 4), DateInterval.of(1, 2, 3, 5));
        assertNotEquals(DateInterval.of(-1, -2, -3, -4), DateInterval.of(1, 2, 3, 4));
        assertNotEquals(DateInterval.of(1, 2, 3, 4), TimeInterval.of(1, 2, 3, 4));
    }

    @Test
    void testTimeIntervalEquality() {
        assertEquals(TimeInterval.of(1, 2, 3, 4), TimeInterval.of(1, 2, 3, 4));
        assertEquals(TimeInterval.of(1, 2, 3, 4), Interval.parseISO("PT1H2M3.004S"));
        assertEquals(TimeInterval.of(-1, -2, -3, -4), Interval.parseISO("-PT1H2M3.004S"));

        assertNotEquals(TimeInterval.of(1, 2, 3, 4), TimeInterval.of(1, 2, 3, 5));
        assertNotEquals(TimeInterval.of(-1, -2, -3, -4), TimeInterval.of(1, 2, 3, 4));
        assertNotEquals(TimeInterval.of(1, 2, 3, 4), DateInterval.of(1, 2, 3, 4));
    }

    @Test
    void testDateIntervalJacksonSerialisation() throws JsonProcessingException {
        DateInterval interval = DateInterval.of(1, 2, 3, 4);
        var serialised = new ObjectMapper().writeValueAsString(interval);
        assertEquals("\"" + interval.toISOString() + "\"", serialised);
        var deserialised = new ObjectMapper().readValue(serialised, DateInterval.class);
        assertEquals(interval, deserialised);
    }

    @Test
    void testTimeIntervalJacksonSerialisation() throws JsonProcessingException {
        TimeInterval interval = TimeInterval.of(1, 2, 3, 4);
        var serialised = new ObjectMapper().writeValueAsString(interval);
        assertEquals("\"" + interval.toISOString() + "\"", serialised);
        var deserialised = new ObjectMapper().readValue(serialised, TimeInterval.class);
        assertEquals(interval, deserialised);
    }

    @Test
    void testIntervalJacksonSerialisation() throws JsonProcessingException {
        Interval interval = Interval.parseISO("PT1S");
        var serialised = new ObjectMapper().writeValueAsString(interval);
        assertEquals("\"" + interval.toISOString() + "\"", serialised);
        var deserialised = new ObjectMapper().readValue(serialised, Interval.class);
        assertEquals(interval, deserialised);
    }

    @Test
    void testAttemptingToDeserialiseTimeIntervalAsDateInterval() throws JsonProcessingException {
        TimeInterval interval = TimeInterval.of(1, 2, 3, 4);
        var serialised = new ObjectMapper().writeValueAsString(interval);

        assertThrows(JsonMappingException.class, () -> new ObjectMapper().readValue(serialised, DateInterval.class),
            "Expected failure when trying to deserialise time interval to date interval");
    }

    @Test
    void testAttemptingToDeserialiseDateIntervalAsTimeInterval() throws JsonProcessingException {
        DateInterval interval = DateInterval.of(1, 2, 3, 4);
        var serialised = new ObjectMapper().writeValueAsString(interval);

        assertThrows(JsonMappingException.class, () -> new ObjectMapper().readValue(serialised, TimeInterval.class),
            "Expected failure when trying to deserialise date interval to time interval");
    }

    @Test
    void testDateIntervalGetTemporalUnits() {
        final var dateInterval = DateInterval.of(1, 2, 3, 4);
        assertEquals(List.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.WEEKS, ChronoUnit.DAYS),
            dateInterval.getUnits());
        assertEquals(1, dateInterval.get(ChronoUnit.YEARS));
        assertEquals(2, dateInterval.get(ChronoUnit.MONTHS));
        assertEquals(3, dateInterval.get(ChronoUnit.WEEKS));
        assertEquals(4, dateInterval.get(ChronoUnit.DAYS));
    }

    @Test
    void testTimeIntervalGetTemporalUnits() {
        final var timeInterval = TimeInterval.of(1, 2, 3, 4);
        assertEquals(List.of(ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS, ChronoUnit.MILLIS),
            timeInterval.getUnits());
        assertEquals(1, timeInterval.get(ChronoUnit.HOURS));
        assertEquals(2, timeInterval.get(ChronoUnit.MINUTES));
        assertEquals(3, timeInterval.get(ChronoUnit.SECONDS));
        assertEquals(4, timeInterval.get(ChronoUnit.MILLIS));
    }

    @Test
    void testIsAmbiguousSignForDateIntervals() {
        assertFalse(DateInterval.of(1, 2, 3, 4).isAmbiguousSign());
        assertFalse(DateInterval.of(0, 0, 0, 0).isAmbiguousSign());
        assertFalse(DateInterval.of(1, 0, 0, 4).isAmbiguousSign());
        assertFalse(DateInterval.of(0, -1, -10, 10).isAmbiguousSign(),
            "not ambiguous because weeks are convertible to days");

        assertTrue(DateInterval.of(-1, 1, 0, 0).isAmbiguousSign());
    }
}
