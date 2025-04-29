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
 *   Dec 5, 2024 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType.DATE;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType.DATE_TIME;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType.TIME;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType.ZONED_DATE_TIME;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatCategory.AMERICAN;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatCategory.EUROPEAN;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatCategory.STANDARD;
import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.DateTimeFormatValidationUtil.isTypeCompatibleWithPattern;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.node.util.StringHistory;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatCategory;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget.FormatWithExample;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 * <p>
 * A provider for a list of date/time formats - all the ones that our UX designers decided were in common use. If this
 * class is used directly as a provider in a {@link DateTimeFormatPickerWidget}, it will provide a list of recently used
 * formats from the {@link StringHistory} under the key "string_to_date_formats", as well as a list of default formats.
 * The default time to use in the examples is the current time and the default locale is {@link Locale#ENGLISH}.
 * </p>
 * <p>
 * (Note that the string history in this class is managed by the {@link DateTimeFormatStringHistoryManager}. See that
 * class to add formats to the history.)
 * </p>
 *
 * To customize the defaults, it is expected that the developer using this class will extend it and provide
 * <ul>
 * <li>a {@link Supplier} for the current time and a supplier the desired {@link Locale}, which will be used to generate
 * an example for each format</li>
 * <li>a set of recently used formats</li>
 * </ul>
 * </p>
 * <p>
 * The {@link FormatTemporalType}s will be inferred for each of the recent formats. Most formats will have multiple
 * types inferred, so e.g. the format 'yyyy' will match all three of {@link FormatTemporalType#DATE},
 * {@link FormatTemporalType#DATE_TIME}, and {@link FormatTemporalType#ZONED_DATE_TIME}, because any of the three
 * corresponding java date/time types can be formatted with this pattern.
 * </p>
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("squid:S1192") // suppress sonar's "too many literals" warning
public class ComprehensiveDateTimeFormatProvider implements StateProvider<FormatWithExample[]> {

    private Collection<String> m_recentFormats;

    /**
     * Constructor for providing a list of recently used formats. Most probably useful when this class is used as a base
     * class for a more specific format provider.
     *
     * @param recentFormats the formats recently used by the user. It may be empty, but not null.
     * @throws NullPointerException if recentFormats is null
     */
    public ComprehensiveDateTimeFormatProvider(final List<String> recentFormats) {
        m_recentFormats = new ArrayList<>((Objects.requireNonNull(recentFormats, "recentFormats must not be null")));
    }

    /**
     * Default constructor for providing a list of recently used formats from the {@link StringHistory}. When this class
     * is used as provider directly in a {@link DateTimeFormatPickerWidget}, this constructor will be called.
     */
    public ComprehensiveDateTimeFormatProvider() {
        this(DateTimeFormatStringHistoryManager.getRecentFormats());
    }

    /**
     * Get the time to use for the examples. Note that this method is called only once per call to
     * {@link #computeState}. Override this method to provide a custom time.
     *
     * @return a ZonedDateTime that will be formatted to produce the examples.
     */
    protected ZonedDateTime getTimeForExamples() {
        // Provide a default value for subclasses to override if needed
        return ZonedDateTime.now();
    }

    /**
     * Get the locale to use for the examples. Note that this method is called only once per call to
     * {@link #computeState}. Override this method to provide a custom locale.
     *
     * @return the locale to use for the examples.
     */
    protected Locale getLocaleForExamples() {
        // Provide a default value for subclasses to override if needed
        return Locale.ENGLISH;
    }

    @Override
    public void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();
    }

    /**
     * Compute the default formats with examples.
     *
     * @param exampleTime
     * @param exampleLocale
     * @return the formats with examples.
     */
    protected static final Collection<FormatWithExample> computeDefaultFormats(final ZonedDateTime exampleTime,
        final Locale exampleLocale) {
        return ALL_DEFAULT_FORMATS //
            .stream() //
            .map(formatWithoutExample -> new FormatWithExample( //
                formatWithoutExample.format, //
                formatWithoutExample.temporalType, //
                formatWithoutExample.category, //
                DateTimeFormatter.ofPattern(formatWithoutExample.format, exampleLocale).format(exampleTime) //
            )) //
            .toList();
    }

    /**
     * Compute the recently used formats with examples.
     *
     * @param exampleTime
     * @param exampleLocale
     * @param recentFormats
     * @return the formats with examples.
     */
    protected static final List<FormatWithExample> computeRecentFormats(final ZonedDateTime exampleTime,
        final Locale exampleLocale, final Collection<String> recentFormats) {

        return computeRecentFormatsWithoutExamples(recentFormats).stream() //
            .map(formatWithoutExample -> formatWithoutExample.withExample(exampleTime, exampleLocale)) //
            .toList();
    }

    /**
     * Compute the recently used formats without examples.
     *
     * @param recentFormats
     * @return the formats without examples.
     */
    protected static final List<FormatWithoutExample>
        computeRecentFormatsWithoutExamples(final Collection<String> recentFormats) {

        return recentFormats.stream() //
            .map(format -> { //
                var compatibleTypes = inferCompatibleTypesFromPattern(format);

                return compatibleTypes.stream() //
                    .map(type -> new FormatWithoutExample( //
                        format, //
                        type, //
                        FormatCategory.RECENT //
                ));
            }).flatMap(Function.identity()).toList();
    }

    @Override
    public FormatWithExample[] computeState(final DefaultNodeSettingsContext context) {
        var time = getTimeForExamples();
        var locale = getLocaleForExamples();

        var recentFormats = computeRecentFormats(time, locale, m_recentFormats);
        var defaultFormats = computeDefaultFormats(time, locale);

        return Stream.concat(recentFormats.stream(), defaultFormats.stream()).toArray(FormatWithExample[]::new);
    }

    /**
     * Which {@link TemporalFormatType}s are compatible with a given pattern?
     *
     * @param pattern the date-time pattern like 'yyyy-MM-dd'.
     * @return the compatible types.
     */
    private static Collection<FormatTemporalType> inferCompatibleTypesFromPattern(final String pattern) {
        return Arrays.stream(FormatTemporalType.values()) //
            .filter(type -> isTypeCompatibleWithPattern(pattern, type)) //
            .toList();
    }

    /**
     * A {@link FormatWithExample} without the example.
     *
     * @param format
     * @param temporalType
     * @param category
     */
    public record FormatWithoutExample(String format, FormatTemporalType temporalType, FormatCategory category) {

        FormatWithExample withExample(final String example) {
            return new FormatWithExample(format, temporalType, category, example);
        }

        FormatWithExample withExample(final ZonedDateTime exampleTime, final Locale exampleLocale) {
            return withExample(DateTimeFormatter.ofPattern(format, exampleLocale).format(exampleTime));
        }
    }

    /**
     * All the date formats.
     */
    public static final Collection<FormatWithoutExample> DATE_FORMATS = List.of( //
        new FormatWithoutExample("yyyy-MM-dd", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-MM", DATE, STANDARD), //
        new FormatWithoutExample("yyyyMMdd", DATE, STANDARD), //
        new FormatWithoutExample("YYYY-MM", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-M-d", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-MMMM-dd", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-ww", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-D", DATE, STANDARD), //
        new FormatWithoutExample("YYYYwwee", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-QQ", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-QQQ", DATE, STANDARD), //
        new FormatWithoutExample("yyyy-'W'ww", DATE, STANDARD), //
        new FormatWithoutExample("dd/MM/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd.MM.yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd/MM/yy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd.MM.yy", DATE, EUROPEAN), //
        new FormatWithoutExample("d. MMMM yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd MMM yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("d. MMMM YYYY", DATE, EUROPEAN), //
        new FormatWithoutExample("d/M/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd MMMM yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("yyyy/MM/dd", DATE, EUROPEAN), //
        new FormatWithoutExample("yyyy.MM.dd", DATE, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd", DATE, EUROPEAN), //
        new FormatWithoutExample("MMM yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("MMMM yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("dd/M/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("MM-yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("Q/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("MMM. yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("d-MMM-yy", DATE, EUROPEAN), //
        new FormatWithoutExample("d.M.yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("d/MM/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("d. MMM. yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("yyyy MMM dd", DATE, EUROPEAN), //
        new FormatWithoutExample("QQQ/yyyy", DATE, EUROPEAN), //
        new FormatWithoutExample("yyyy/QQQ", DATE, EUROPEAN), //
        new FormatWithoutExample("MM/dd/yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MM/dd/yy", DATE, AMERICAN), //
        new FormatWithoutExample("MMM d, yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MMMM yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MM/yy", DATE, AMERICAN), //
        new FormatWithoutExample("MM/dd/YYYY", DATE, AMERICAN), //
        new FormatWithoutExample("yyyy/MM/dd", DATE, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd", DATE, AMERICAN), //
        new FormatWithoutExample("yyyyMMdd", DATE, AMERICAN), //
        new FormatWithoutExample("M/d/yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("M/d/yy", DATE, AMERICAN), //
        new FormatWithoutExample("MM-dd-yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MMM. yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MMM yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MM/yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("MMM dd, yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("Q/yyyy", DATE, AMERICAN), //
        new FormatWithoutExample("QQ/yyyy", DATE, AMERICAN) //
    );

    /**
     * All the time formats.
     */
    public static final Collection<FormatWithoutExample> TIME_FORMATS = List.of( //
        new FormatWithoutExample("HH:mm", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm:ss", TIME, STANDARD), //
        new FormatWithoutExample("H:mm", TIME, STANDARD), //
        new FormatWithoutExample("H:mm:ss", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm:ss.SSS", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm:ss[.SSS]", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm:ss.nnn", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm:ss[.n]", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm[:ss[.SSS]]", TIME, STANDARD), //
        new FormatWithoutExample("hh:mma", TIME, STANDARD), //
        new FormatWithoutExample("h:mm a", TIME, STANDARD), //
        new FormatWithoutExample("h:mm:ss a", TIME, STANDARD), //
        new FormatWithoutExample("HH:mm", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm:ss", TIME, EUROPEAN), //
        new FormatWithoutExample("H:mm", TIME, EUROPEAN), //
        new FormatWithoutExample("H:mm:ss", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm:ss.SSS", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm:ss[.SSS]", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm:ss.nnn", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm:ss[.n]", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm[:ss[.SSS]]", TIME, EUROPEAN), //
        new FormatWithoutExample("hh:mma", TIME, EUROPEAN), //
        new FormatWithoutExample("h:mm a", TIME, EUROPEAN), //
        new FormatWithoutExample("h:mm:ss a", TIME, EUROPEAN), //
        new FormatWithoutExample("HH:mm", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm:ss", TIME, AMERICAN), //
        new FormatWithoutExample("H:mm", TIME, AMERICAN), //
        new FormatWithoutExample("H:mm:ss", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm:ss.SSS", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm:ss[.SSS]", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm:ss.nnn", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm:ss[.n]", TIME, AMERICAN), //
        new FormatWithoutExample("HH:mm[:ss[.SSS]]", TIME, AMERICAN), //
        new FormatWithoutExample("hh:mma", TIME, AMERICAN), //
        new FormatWithoutExample("h:mm a", TIME, AMERICAN), //
        new FormatWithoutExample("h:mm:ss a", TIME, AMERICAN) //
    );

    /**
     * All the date-time formats.
     */
    public static final Collection<FormatWithoutExample> DATE_TIME_FORMATS = List.of( //
        new FormatWithoutExample("yyyy-MM-dd HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd;HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss.S", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd;HH:mm:ss.S", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd;HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm[:ss[.SSS]]", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm[:ss]", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss[.SSS]", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm[:ss]", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd'T'HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd'T'HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd;HH:mm", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MMM-dd;HH:mm:ss", DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss.SSS", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd'T'HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd'T'HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd;HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MMM-dd;HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd/MM/yyyy HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd/MM/yyyy HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd/MM/yyyy hh:mm a", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd.MM.yyyy HH:mm", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd.MM.yyyy HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd.MM.yyyy hh:mm a", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd MMM yyyy HH:mm ", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("dd MMM yyyy HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("MM/dd/yyyy HH:mm", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MM/dd/yyyy HH:mm:ss", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MM/dd/yyyy HH:mm:ss.SSS", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss.SSS", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MM/dd/yyyy h:mm a", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MM/dd/yyyy h:mm:ss a", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyyMMdd HH:mm:ss", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyyMMdd HH:mm", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MMM dd, yyyy h:mm a", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MMM dd, yyyy HH:mm:ss", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("MMM dd, yyyy HH:mm", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm", DATE_TIME, AMERICAN) //
    );

    /**
     * All the zoned date-time formats.
     */
    public static final Collection<FormatWithoutExample> ZONED_DATE_TIME_FORMATS = List.of( //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'Z'", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSZ", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[Z]", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssXXX", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'['VV']'", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[ZZZZ]", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssVV", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss VV", ZONED_DATE_TIME, STANDARD), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'Z'", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSZ", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[Z]", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssXXX", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'['VV']'", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[ZZZZ]", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssVV", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss VV", ZONED_DATE_TIME, EUROPEAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'Z'", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSZ", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[Z]", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss'['VV']'", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS[ZZZZ]", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssVV", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd HH:mm:ss VV", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ss.SSS", ZONED_DATE_TIME, AMERICAN), //
        new FormatWithoutExample("yyyy-MM-dd'T'HH:mm:ssXXX", ZONED_DATE_TIME, AMERICAN) //
    );

    /**
     * All the formats combined together.
     *
     * <p>
     * (Note to the reader: we don't actually need to use {@link Collections#unmodifiableCollection(Collection)} here,
     * since {@link Stream#toList()} returns an unmodifiable list. However, if we don't, SonarLint will complain about
     * it being mutable).
     * </p>
     */
    public static final Collection<FormatWithoutExample> ALL_DEFAULT_FORMATS = Collections.unmodifiableCollection( //
        Stream.of( //
            DATE_FORMATS, //
            TIME_FORMATS, //
            DATE_TIME_FORMATS, //
            ZONED_DATE_TIME_FORMATS //
        ) //
            .flatMap(Collection::stream) //
            .toList() //
    );

    /**
     * Link to the JavaDoc for {@link DateTimeFormatter}, which includes a list of all the format characters that can be
     * used.
     */
    public static final String LINK_TO_FORMAT_JAVADOC =
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html";

    /**
     * Convenient list of allowed format strings in the date/time format picker, intended to be used verbatim in widget
     * documentation. Based on the format strings allowed by {@link DateTimeFormatter}.
     */
    public static final String DATE_FORMAT_LIST_FOR_DOCS = """
            <ul>
                <li>G: era</li>
                <li>u: year</li>
                <li>y: year of era</li>
                <li>D: day of year</li>
                <li>M: month in year (context sensitive)</li>
                <li>L: month in year (standalone form)</li>
                <li>d: day of month</li>
                <li>Q/q: quarter of year</li>
                <li>Y: week based year (you probably want to use y instead)</li>
                <li>w: week of week based year</li>
                <li>W: week of month</li>
                <li>E: day of week</li>
                <li>e: localized day of week
                    <ul>
                        <li>e: 4</li>
                        <li>ee: 04</li>
                        <li>eee: Wed</li>
                        <li>eeee: Wednesday</li>
                        <li>eeeee: W</li>
                    </ul>
                </li>
                <li>c: day of week</li>
                <li>F: day-of-week in month</li>
                <li>a: am/pm of day</li>
                <li>h: clock hour of am/pm (1-12)</li>
                <li>K: hour of am/pm (0-11)</li>
                <li>k: clock hour of am/pm (1-24)</li>
                <li>H: hour of day (0-23)</li>
                <li>m: minute of hour</li>
                <li>s: second of minute</li>
                <li>S: fraction of second</li>
                <li>A: milli of day</li>
                <li>n: nano of second</li>
                <li>N: nano of day</li>
                <li>V: time zone ID</li>
                <li>z: time zone name</li>
                <li>O: localized zone offset</li>
                <li>x: zone offset (ISO8601)
                      <ul>
                          <li>X: +08 or +0830</li>
                          <li>XX: +0800 or +0830 (no colons)</li>
                          <li>XXX: +08:00 or +08:30 (with colons)</li>
                          <li>XXXX: +0800 or +083015 (i.e. including offset seconds, no colons)</li>
                          <li>XXXXX: +08:00 or +08:30:15 (i.e. including offset seconds, with colons)</li>
                      </ul>
                </li>
                <li>X: same as x, but outputs Z when offset is 0</li>
                <li>Z: zone offset (RFC822)
                    <ul>
                        <li>Z, ZZ, ZZZ: +0800 or +0830</li>
                        <li>ZZZZ: GMT+08:00 or GMT+08:30</li>
                        <li>ZZZZZ: +08:00 or +08:30:15</li>
                    </ul>
                </li>
                <li>p: pad next</li>
                <li>' : escape for text</li>
                <li>'': single quote</li>
                <li>[: optional section start</li>
                <li>]: optional section end</li>
            </ul>
            """;

    /**
     * Return the first format that matches all of the provided dateStrings, or empty if none of the formats match all
     * of the dateStrings. Note that this can never return a format like 'yyyy/QQ' because that format doesn't fully
     * specify any of the {@link FormatTemporalType four supported temporal types}.
     *
     * @param dateStrings a collection of date/time strings (e.g. 2020-01-01 or 13:23:45)
     * @param temporalType the temporal type to use for parsing the date strings
     * @param recentFormats the formats that the user has recently used.
     * @return optional containing the first format that matches all of the dateStrings, or empty if none of the formats
     *         match all of the dateStrings
     */
    public static Optional<FormatWithoutExample> bestFormatGuess(final Collection<String> dateStrings,
        final FormatTemporalType temporalType, final Collection<String> recentFormats) {
        if (dateStrings.isEmpty()) {
            throw new IllegalArgumentException("dateStrings must not be empty");
        }

        return Stream.concat(ALL_DEFAULT_FORMATS.stream(), //
            computeRecentFormatsWithoutExamples(recentFormats).stream()) //
            .filter(fmt -> temporalType == null || temporalType == fmt.temporalType) //
            .filter(fmt -> matchesAllDateStrings(fmt, dateStrings, temporalType)) //
            .findFirst();
    }

    /**
     * Does the given format match all of the given date strings? Note that this is for PARSING only. So for example,
     * this can never return true for a format like 'yyyy/QQ' because it doesn't fully specify any of the
     * {@link FormatTemporalType four supported temporal types}.
     *
     * @param format a date-time format, from the list of valid formats. It must be valid or this method will throw.
     * @param dateStrings a collection of date strings (e.g. '2020-01-01')
     * @param desiredType the temporal type to use for parsing the date strings. Not only must the types match, but the
     *            query associated with the type will be used when parsing. This has the advantage of eliminating some
     *            parsing edge cases, like when the format string 'Q' might accept a value like '31' even though it's
     *            not a valid quarter.
     * @return true if the format matches all of the date strings and the provided format type, else false
     */
    static boolean matchesAllDateStrings(final FormatWithoutExample format, final Collection<String> dateStrings,
        final FormatTemporalType desiredType) {

        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(dateStrings, "dateStrings must not be null");

        if (dateStrings.isEmpty()) {
            throw new IllegalArgumentException("dateStrings must not be empty");
        }

        if (format.temporalType != desiredType && desiredType != null) {
            return false;
        }

        // should not throw since pattern is from our big list of valid formats
        var pattern = DateTimeFormatter.ofPattern(format.format);

        if (desiredType == null) {
            var allQueries = Arrays.stream(FormatTemporalType.values()) //
                .map(FormatTemporalType::associatedQuery) //
                .toArray(TemporalQuery[]::new);

            return dateStrings.stream().allMatch(dateString -> {
                try {
                    pattern.parseBest(dateString, allQueries);
                    return true;
                } catch (DateTimeParseException ex) {
                    return false;
                }
            });
        } else {
            return dateStrings.stream().allMatch(dateString -> {
                try {
                    pattern.parse(dateString, desiredType.associatedQuery());
                    return true;
                } catch (DateTimeParseException ex) {
                    return false;
                }
            });
        }
    }
}
