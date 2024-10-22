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

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Hours;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Minutes;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.HoursMinutesAndSeconds.Seconds;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Micro;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Milli;
import org.knime.core.webui.node.dialog.defaultdialog.setting.time.TimeParts.MilliMicroAndNanoSeconds.Nano;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author Tobias Kampmann
 */
public class TimeParts implements DefaultNodeSettings {

    @HorizontalLayout
    interface HoursMinutesAndSeconds {
        interface Hours {}
        interface Minutes {}
        interface Seconds {}
    }

    @Layout(Hours.class)
    @Widget(title = "Use hour",description = "")
    boolean m_useHour;

    @Layout(Hours.class)
    @Widget(title = "Hours",description = "")
    @NumberInputWidget(min = 0, max = 23)
    int m_hour = 0;


    @Layout(Minutes.class)
    @Widget(title = "Use minute",description = "")
    boolean m_useMinute;


    @Layout(Minutes.class)
    @Widget(title = "Minutes",description = "")
    @NumberInputWidget(min = 0, max = 59)
    int m_minute = 0;


    @Layout(Seconds.class)
    @Widget(title = "Use second",description = "")
    boolean m_useSecond;

    @Layout(Seconds.class)
    @Widget(title = "Seconds",description = "")
    @NumberInputWidget(min = 0, max = 59)
    int m_second = 0;


    interface MilliMicroAndNanoSeconds {
        interface Milli {
        }

        interface Micro {
        }

        interface Nano {

        }
    }

    @Layout(Milli.class)
    @Widget(title = "Use millisecond",description = "")
    boolean m_useMillisecond;

    @Layout(Milli.class)
    @Widget(title = "Milliseconds",description = "")
    @NumberInputWidget(min = 0, max = 999)
    int m_millisecond = 0;

    @Layout(Micro.class)
    @Widget(title = "Use microsecond",description = "")
    boolean m_useMicrosecond;

    @Layout(Micro.class)
    @Widget(title = "Microseconds",description = "")
    @NumberInputWidget(min = 0, max = 999)
    int m_microsecond = 0;

    @Layout(Nano.class)
    @Widget(title = "Use nanosecond",description = "")
    boolean m_useNanosecond;

    @Layout(Nano.class)
    @Widget(title = "Nanoseconds",description = "")
    @NumberInputWidget(min = 0, max = 999)
    int m_nanosecond = 0;

}
