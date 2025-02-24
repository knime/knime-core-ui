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
 *   Dec 4, 2024 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.interval;

import java.io.IOException;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A class representing an interval consisting of variable-length units, like days, months, weeks and years. This is
 * very similar to a {@link Period} except that it has a concept of weeks. See {@link Interval} for more details.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@JsonTypeName("dateInterval")
@JsonSerialize(using = DateInterval.Serializer.class)
@JsonDeserialize(using = DateInterval.Deserializer.class)
public final class DateInterval implements Interval {

    private final int m_years;

    private final int m_months;

    private final int m_weeks;

    private final int m_days;

    private DateInterval(final int years, final int months, final int weeks, final int days) {
        this.m_years = years;
        this.m_months = months;
        this.m_weeks = weeks;
        this.m_days = days;
    }

    DateInterval() {
        this(0, 0, 0, 0);
    }

    @Override
    public long get(final TemporalUnit unit) {
        var cunit = (ChronoUnit)unit;

        return switch (cunit) {
            case YEARS -> getYears();
            case MONTHS -> getMonths();
            case WEEKS -> getWeeks();
            case DAYS -> getDays();
            default -> throw new IllegalArgumentException("Unexpected unit: " + unit);
        };
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return List.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.WEEKS, ChronoUnit.DAYS);
    }

    @Override
    public Temporal addTo(final Temporal temporal) {
        return asPeriod().addTo(temporal);
    }

    @Override
    public Temporal subtractFrom(final Temporal temporal) {
        return asPeriod().subtractFrom(temporal);
    }

    @Override
    public String toISOString() {
        // if all fields are negative we can prepend the -. Otherwise, we have to prepend it to all fields individually
        var shouldPrependMinus = m_years <= 0 && m_months <= 0 && m_weeks <= 0 && m_days <= 0 && !isZero();

        return shouldPrependMinus //
            ? "-P%sY%sM%sW%sD".formatted(Math.abs(m_years), Math.abs(m_months), Math.abs(m_weeks), Math.abs(m_days)) //
            : "P%sY%sM%sW%sD".formatted(m_years, m_months, m_weeks, m_days);
    }

    @Override
    public String toShortHumanReadableString() {
        return toLongHumanReadableString() //
            .replaceAll("\\s*years?", "y") //
            .replaceAll("\\s*months?", "M") //
            .replaceAll("\\s*weeks?", "w") //
            .replaceAll("\\s*days?", "d");
    }

    @Override
    public String toLongHumanReadableString() {
        if (isZero()) {
            return "0 days";
        }

        var names = List.of("year", "month", "week", "day");
        var values = List.of(m_years, m_months, m_weeks, m_days);

        var shouldPrependMinus = values.stream().allMatch(i -> i <= 0) && !isZero();

        var outputTokens = IntStream.range(0, names.size()) //
            .filter(i -> values.get(i) != 0) //
            .mapToObj(i -> String.join( //
                "", //
                String.valueOf(conditionalAbs(values.get(i), shouldPrependMinus)), //
                " ", //
                pluralise(names.get(i), values.get(i)) //
            )) //
            .toList();

        if (shouldPrependMinus) {
            return outputTokens.size() == 1 //
                ? "-" + outputTokens.get(0) //
                : "-(" + String.join(" ", outputTokens) + ")";
        }

        return String.join(" ", outputTokens).trim();
    }

    /**
     * @return the days
     */
    public int getDays() {
        return m_days;
    }

    /**
     * @return the months
     */
    public int getMonths() {
        return m_months;
    }

    /**
     * @return the weeks
     */
    public int getWeeks() {
        return m_weeks;
    }

    /**
     * @return the years
     */
    public int getYears() {
        return m_years;
    }

    @Override
    public boolean isStrictlyNegative() {
        var effectiveDays = getDays() + 7 * getWeeks();

        // check all fields are <= 0 and at least one < 0
        return !isZero() && m_years <= 0 && m_months <= 0 && effectiveDays <= 0;
    }

    /**
     * This method checks if the sign of the {@link DateInterval} is ambiguous. This is the case if the interval
     * consists of a mix of positive and negative values. Note that since weeks are directly convertable to days, we can
     * count them as days when checking for ambiguity.
     *
     * @return true if the sign of the {@link DateInterval} is ambiguous, false otherwise.
     */
    public boolean isAmbiguousSign() {
        var effectiveDays = getDays() + 7 * getWeeks();

        var signs = Stream.of(m_years, m_months, effectiveDays) //
            .mapToInt(i -> Integer.signum(i)) //
            .filter(i -> i != 0) // zero is irrelevant for this check
            .distinct() //
            .count();

        return signs > 1;
    }

    @Override
    public boolean isZero() {
        return m_years == 0 && m_months == 0 && m_weeks == 0 && m_days == 0;
    }

    /**
     * Check if this interval is effectively zero. This is the case if the interval refers to an amount of time that is
     * zero, but the fields are not all zero (so e.g. 1 week and -7 days would be effectively zero).
     *
     * @return true if the interval is effectively zero, false otherwise
     */
    @Override
    public boolean isEffectivelyZero() {
        return m_years == 0 && m_months == 0 && (7 * m_weeks + m_days == 0);
    }

    /**
     * Convert this to a {@link Period}. Note that since a {@link Period} has no concept of weeks, this will be a
     * {@link Period} with the same number of years and months as this, but with a day count equal to:
     *
     * {@link DateInterval#getDays()} + 7*{@link DateInterval#getWeeks()}
     *
     * @return a {@link Period} with the same length as this.
     */
    public Period asPeriod() {
        return Period.of(getYears(), getMonths(), getDays()).plus(Period.ofWeeks(getWeeks()));
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DateInterval)) {
            return false;
        }

        var other = (DateInterval)obj;
        return m_years == other.m_years && m_months == other.m_months && m_weeks == other.m_weeks
            && m_days == other.m_days;
    }

    @Override
    public boolean hasSameLength(final Object obj) {
        if (!(obj instanceof DateInterval)) {
            return false;
        }

        var other = (DateInterval)obj;

        var effectiveDays = getDays() + 7 * getWeeks();
        var otherEffectiveDays = other.getDays() + 7 * other.getWeeks();

        return m_years == other.m_years && m_months == other.m_months && effectiveDays == otherEffectiveDays;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_years, m_months, m_weeks, m_days);
    }

    @Override
    public String toString() {
        return "DateInterval[%s]".formatted(toISOString());
    }

    /**
     * Create a {@link DateInterval} from the given fields.
     *
     * @param years
     * @param months
     * @param weeks
     * @param days
     * @return a {@link DateInterval} that keeps track of the values used to create it, but can be used like a normal
     *         {@link TemporalAmount} and easily converted to a {@link Period}.
     */
    public static DateInterval of(final int years, final int months, final int weeks, final int days) {
        return new DateInterval(years, months, weeks, days);
    }

    /**
     * Create a {@link DateInterval} from a {@link Period}.
     *
     * @param value the period to convert
     * @return a {@link DateInterval} that represents the same period as the given {@link Period}.
     */
    public static DateInterval fromPeriod(final Period value) {
        return of(value.getYears(), value.getMonths(), value.getDays() / 7, value.getDays() % 7);
    }

    /**
     * A deserialiser for {@link DateInterval}s. It will convert an ISO8601 string representing a {@link Period} to a
     * {@link DateInterval}.
     */
    static final class Deserializer extends JsonDeserializer<DateInterval> {

        @Override
        public DateInterval deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            Interval parsed;
            try {
                parsed = Interval.parseISO(p.getValueAsString());
            } catch (IllegalArgumentException e) {
                throw new IOException("Could not parse interval '%s'".formatted(p.getValueAsString()), e);
            }

            if (!(parsed instanceof DateInterval)) {
                throw new IOException(
                    "Expected a DateInterval, but got a %s".formatted(parsed.getClass().getSimpleName()));
            }

            return (DateInterval)parsed;
        }
    }

    /**
     * A serialiser for {@link DateInterval}s. It will convert a {@link DateInterval} to an ISO8601 string representing
     * a {@link Period}, e.g. P1Y2M3W4D.
     */
    static final class Serializer extends JsonSerializer<DateInterval> {

        @Override
        public void serialize(final DateInterval value, final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
            gen.writeString(value.toISOString());
        }
    }

    private static String pluralise(final String value, final int count) {
        return (Math.abs(count) == 1) ? value : value + "s";
    }

    private static int conditionalAbs(final int value, final boolean condition) {
        return condition ? Math.abs(value) : value;
    }
}
