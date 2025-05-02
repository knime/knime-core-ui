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
 *   May 2, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DefaultValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.OptionalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoice;

/**
 * Tests for {@link Optional} fields and the associated {@link OptionalWidget}.
 *
 * @author Paul Bärnreuther
 */
class OptionalWidgetOptionsTest {

    @Test
    void testOptionalSetting() {
        class HidableSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @TextInputWidget(placeholder = "I am still being picked up")
            Optional<String> m_string;

            @Widget(title = "", description = "")
            Optional<Integer> m_integer;

            @Widget(title = "", description = "")
            Optional<Double> m_double;

            @Widget(title = "", description = "")
            Optional<Byte> m_byte;

            @Widget(title = "", description = "")
            Optional<String[]> m_stringArray;

            @Widget(title = "", description = "")
            Optional<LocalDate> m_date;

            @Widget(title = "", description = "")
            Optional<LocalDateTime> m_dateTime;

            @Widget(title = "", description = "")
            Optional<LocalTime> m_time;

            @Widget(title = "", description = "")
            Optional<ZonedDateTime> m_zonedDateTime;

            @Widget(title = "", description = "")
            Optional<ZoneId> m_zoneId;
        }

        var response = buildTestUiSchema(HidableSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].options.default").isString().isEmpty();
        assertThatJson(response).inPath("$.elements[0].options.placeholder").isString()
            .contains("I am still being picked up");

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("integer");
        assertThatJson(response).inPath("$.elements[1].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].options.default").isNumber().isZero();

        assertThatJson(response).inPath("$.elements[2].scope").isString().contains("double");
        assertThatJson(response).inPath("$.elements[2].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[2].options.default").isNumber().isZero();

        assertThatJson(response).inPath("$.elements[3].scope").isString().contains("byte");
        assertThatJson(response).inPath("$.elements[3].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[3].options.default").isNumber().isZero();

        assertThatJson(response).inPath("$.elements[4].scope").isString().contains("stringArray");
        assertThatJson(response).inPath("$.elements[4].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[4].options.default").isArray().isEmpty();

        assertThatJson(response).inPath("$.elements[5].scope").isString().contains("date");
        assertThatJson(response).inPath("$.elements[5].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[5].options.default").isPresent();

        assertThatJson(response).inPath("$.elements[6].scope").isString().contains("dateTime");
        assertThatJson(response).inPath("$.elements[6].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[6].options.default").isPresent();

        assertThatJson(response).inPath("$.elements[7].scope").isString().contains("time");
        assertThatJson(response).inPath("$.elements[7].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[7].options.default").isPresent();

        assertThatJson(response).inPath("$.elements[8].scope").isString().contains("zonedDateTime");
        assertThatJson(response).inPath("$.elements[8].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[8].options.default").isPresent();

        assertThatJson(response).inPath("$.elements[9].scope").isString().contains("zoneId");
        assertThatJson(response).inPath("$.elements[9].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[9].options.default").isPresent();

    }

    static final class FirstChoice implements DefaultValueProvider<String> {

        private Supplier<List<StringChoice>> m_testChoices;

        @Override
        public void init(final StateProviderInitializer initializer) {
            DefaultValueProvider.super.init(initializer);
            m_testChoices = initializer.computeFromProvidedState(TestChoicesProvider.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) throws StateComputationFailureException {
            return m_testChoices.get().stream().findFirst().map(StringChoice::id).orElse("");
        }

    }

    static final class DateIntervalDefault implements DefaultValueProvider<DateInterval> {

        @Override
        public DateInterval computeState(final DefaultNodeSettingsContext context)
            throws StateComputationFailureException {
            return DateInterval.of(1, 2, 3, 4);
        }

    }

    @Test
    void testThrowsOnFieldTypeWithoutBuiltinDefaultAndWithoutOptionalWidget() {
        class HidableSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            Optional<DateInterval> m_interval;
        }

        assertThat(assertThrows(IllegalStateException.class, () -> buildTestUiSchema(HidableSettings.class)))
            .hasMessageContaining(OptionalWidget.class.getSimpleName());

    }

    @Test
    void testOptionalWidgetWithDefaultProvider() {
        class HidableSettings implements DefaultNodeSettings {

            @Widget(title = "", description = "")
            @ChoicesProvider(TestChoicesProvider.class)
            @OptionalWidget(defaultProvider = FirstChoice.class)
            Optional<String> m_string = Optional.of("TestString");

            @Widget(title = "", description = "")
            @OptionalWidget(defaultProvider = DateIntervalDefault.class)
            Optional<DateInterval> m_interval;
        }

        var response = buildTestUiSchema(HidableSettings.class);

        assertThatJson(response).inPath("$.elements[0].scope").isString().contains("string");
        assertThatJson(response).inPath("$.elements[0].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[0].options.default").isAbsent();
        assertThatJson(response).inPath("$.elements[0].options.defaultProvider").isEqualTo(FirstChoice.class.getName());

        assertThatJson(response).inPath("$.elements[1].scope").isString().contains("interval");
        assertThatJson(response).inPath("$.elements[1].options.hideOnNull").isBoolean().isTrue();
        assertThatJson(response).inPath("$.elements[1].options.default").isAbsent();
        assertThatJson(response).inPath("$.elements[1].options.defaultProvider")
            .isEqualTo(DateIntervalDefault.class.getName());

    }

}
