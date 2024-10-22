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
 *   Oct 22, 2024 (Tobias Kampmann): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.time;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.VerticalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Hours;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Minutes;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Seconds;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Micro;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Milli;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Nano;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.BooleanReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 *
 * @author Tobias Kampmann
 */
@Persistor(TimeParts.TimePartsPersistor.class)
public class TimeParts implements DefaultNodeSettings {

    @HorizontalLayout
    public interface HoursMinutesAndSeconds {

        @VerticalLayout
        interface Hours {
        }

        @After(Hours.class)
        @VerticalLayout
        interface Minutes {
        }

        @After(Minutes.class)
        @VerticalLayout
        interface Seconds {
        }
    }

    static final class UseHours implements BooleanReference {

    }

    @Layout(Hours.class)
    @Widget(title = "Use hour", description = "")
    @ValueReference(UseHours.class)
    boolean m_useHour;

    @Layout(Hours.class)
    @Widget(title = "Hours", description = "")
    @NumberInputWidget(min = 0, max = 23)
    @Effect(predicate = UseHours.class, type = EffectType.ENABLE)
    int m_hour = 0;

    static final class UseMinutes implements BooleanReference {

    }

    @Layout(Minutes.class)
    @Widget(title = "Use minute", description = "")
    @ValueReference(UseMinutes.class)
    boolean m_useMinute;

    @Layout(Minutes.class)
    @Widget(title = "Minutes", description = "")
    @NumberInputWidget(min = 0, max = 59)
    @Effect(predicate = UseMinutes.class, type = EffectType.ENABLE)
    int m_minute = 0;

    static final class UseSeconds implements BooleanReference {

    }

    @Layout(Seconds.class)
    @Widget(title = "Use second", description = "")
    @ValueReference(UseSeconds.class)
    boolean m_useSecond;

    @Layout(Seconds.class)
    @Widget(title = "Seconds", description = "")
    @NumberInputWidget(min = 0, max = 59)
    @Effect(predicate = UseSeconds.class, type = EffectType.ENABLE)
    int m_second = 0;

    @After(HoursMinutesAndSeconds.class)
    @HorizontalLayout()
    public interface MilliMicroAndNanoSeconds {
        @VerticalLayout
        interface Milli {
        }

        @After(Milli.class)
        @VerticalLayout
        interface Micro {
        }

        @After(Micro.class)
        @VerticalLayout
        interface Nano {

        }
    }

    static final class UseMilli implements BooleanReference {

    }

    @Layout(Milli.class)
    @Widget(title = "Use millisecond", description = "", advanced = true)
    @ValueReference(UseMilli.class)
    boolean m_useMillisecond;

    @Layout(Milli.class)
    @Widget(title = "Milliseconds", description = "", advanced = true)
    @NumberInputWidget(min = 0, max = 999)
    @Effect(predicate = UseMilli.class, type = EffectType.ENABLE)
    int m_millisecond = 0;

    static final class UseMicro implements BooleanReference {

    }

    @Layout(Micro.class)
    @Widget(title = "Use microsecond", description = "", advanced = true)
    @ValueReference(UseMicro.class)
    boolean m_useMicrosecond;

    @Layout(Micro.class)
    @Widget(title = "Microseconds", description = "", advanced = true)
    @NumberInputWidget(min = 0, max = 999)
    @Effect(predicate = UseMicro.class, type = EffectType.ENABLE)
    int m_microsecond = 0;

    static final class UseNano implements BooleanReference {

    }

    @Layout(Nano.class)
    @Widget(title = "Use nanosecond", description = "", advanced = true)
    @ValueReference(UseNano.class)
    boolean m_useNanosecond;

    @Layout(Nano.class)
    @Widget(title = "Nanoseconds", description = "", advanced = true)
    @NumberInputWidget(min = 0, max = 999)
    @Effect(predicate = UseNano.class, type = EffectType.ENABLE)
    int m_nanosecond = 0;

    final static class TimePartsPersistor implements FieldNodeSettingsPersistor<TimeParts> {

        @Override
        public TimeParts load(final NodeSettingsRO settings) throws InvalidSettingsException {
            TimeParts loaded = new TimeParts();

            String horribleOldSettings = settings.getString("time");

            var parsedTime = LocalTime.parse(horribleOldSettings);

            loaded.m_hour = parsedTime.getHour();
            loaded.m_minute = parsedTime.getMinute();
            loaded.m_second = parsedTime.getSecond();

            loaded.m_millisecond = parsedTime.getNano() / 1000000;
            loaded.m_microsecond = (parsedTime.getNano() % 1000) / 1000;
            loaded.m_nanosecond = parsedTime.getNano() % 1000000;

            return loaded;
        }

        @Override
        public void save(final TimeParts obj, final NodeSettingsWO settings) {
            settings.addString("time",
                LocalTime
                    .of(obj.m_hour, obj.m_minute, obj.m_second,
                        obj.m_nanosecond + 1000 * obj.m_microsecond + 1000000 * obj.m_millisecond)
                    .format(DateTimeFormatter.ISO_LOCAL_TIME));

        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{"time"};
        }

    }

}
