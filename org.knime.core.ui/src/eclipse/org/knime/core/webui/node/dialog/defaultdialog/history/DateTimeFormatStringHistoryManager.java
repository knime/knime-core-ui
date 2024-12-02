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
 *   Dec 16, 2024 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.history;

import java.util.List;

import org.knime.core.node.util.StringHistory;

/**
 * Class to manage the history of date time formats used in the date time format dialog. Basically a wrapper around a
 * specific {@link StringHistory} instance.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 *
 * @see StringHistory
 */
public final class DateTimeFormatStringHistoryManager {

    private DateTimeFormatStringHistoryManager() {
        // prevent instantiation
    }

    /**
     * The key for the format history in the {@link StringHistory}.
     *
     * @see StringHistory#getInstance(String, int)
     * @see StringHistory#getHistory()
     */
    public static final String FORMAT_HISTORY_KEY = "string_to_date_formats";

    /**
     * Number of recent formats to store/retrieve in/from the history
     */
    public static final int FORMAT_HISTORY_SIZE = 256;

    /**
     * Get the most recent formats used. Stores up to {@link #FORMAT_HISTORY_SIZE} formats. The most recent format is
     * the first element in the list.
     *
     * @return the most recently used formats, ordered from most to least recent
     */
    public static List<String> getRecentFormats() {
        return List.of(StringHistory.getInstance(FORMAT_HISTORY_KEY, FORMAT_HISTORY_SIZE).getHistory());
    }

    /**
     * Helper function to add a format to the recent formats history if it is not already present in the recent formats.
     * Only checks the last {@link #FORMAT_HISTORY_SIZE} formats. If the format is already in the recent formats, it
     * will be moved to the top of the list.
     *
     * @param format the format to add
     */
    public static void addFormatToStringHistoryIfNotPresent(final String format) {
        StringHistory.getInstance(FORMAT_HISTORY_KEY, FORMAT_HISTORY_SIZE).add(format);
    }

}
