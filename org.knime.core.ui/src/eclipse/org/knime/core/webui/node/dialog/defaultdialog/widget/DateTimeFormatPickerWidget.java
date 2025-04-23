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
 *   Dec 2, 2024 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.format.DateTimeFormatter;

import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 * A widget to pick a date/time/date-time format.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface DateTimeFormatPickerWidget {

    /**
     * Provider for the formats that will be selectable in the frontend. {@link ComprehensiveDateTimeFormatProvider}
     * will be used as default. To customize the list of recent used formats or use a different locale or time for
     * creating the examples it is recommended to use ComprehensiveDateTimeFormatProvider as base class to extend from.
     *
     * @return the provider
     */
    Class<? extends StateProvider<FormatWithExample[]>> formatProvider() //
    default ComprehensiveDateTimeFormatProvider.class;

    /**
     * The category of the format.
     */
    public enum FormatCategory {
            /**
             * Standard formats are those that are commonly used and are not specific to any region. All the ISO formats
             * are standard.
             */
            STANDARD,
            /**
             * European formats include things like "dd.MM.yyyy" and "dd/MM/yyyy".
             */
            EUROPEAN,
            /**
             * American formats include things like "MM/dd/yyyy"..
             */
            AMERICAN,
            /**
             * Recent formats are those that have been used recently by the user.
             */
            RECENT;
    }

    /**
     * A record to represent a format with an example and some categorisations that will affect how it is displayed.
     *
     * @param format the format string, something that can be accepted by {@link DateTimeFormatter} like e.g.
     *            "yyyy-MM-dd".
     * @param temporalType the type of temporal that the format represents.
     * @param category the category of the format.
     * @param example an example of a date/time/date-time formatted with the format string, e.g. "2024-12-02" for the
     *            format "yyyy-MM-dd".
     */
    public static record FormatWithExample( //
        String format, //
        FormatTemporalType temporalType, //
        FormatCategory category, //
        String example //
    ) {
    }
}
