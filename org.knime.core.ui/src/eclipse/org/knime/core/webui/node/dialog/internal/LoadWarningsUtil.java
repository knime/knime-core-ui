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
 *   Mar 8, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.internal;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.data.DataServiceContext;

/**
 * Utility for handling all cases where loading settings encounters an {@link InvalidSettingsException}
 *
 * @author paulbaernreuther
 */
public final class LoadWarningsUtil {

    private LoadWarningsUtil() {
        // Utility class
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoadWarningsUtil.class);

    private static void warnWithFormattedMessage(final String baseMessage, final InvalidSettingsException ex) {
        final var message = String.format("%s%nFirst error: %s.", baseMessage, ex.getMessage());
        setMessage(message, ex);
    }

    static void warnAboutVariableOverridesBeingIgnored(final InvalidSettingsException ex) {
        warnWithFormattedMessage("Unable to show previews for flow variable value in the dialog. "
            + "Errors occurred when trying to override the settings.", ex);
    }

    @SuppressWarnings("javadoc")
    public static void warnAboutDefaultSettingsBeingUsedInstead(final InvalidSettingsException ex) {
        warnWithFormattedMessage("The loaded settings are default settings. Loading the saved settings failed.", ex);
    }

    private static void setMessage(final String message, final InvalidSettingsException ex) {
        DataServiceContext.get().addWarningMessage(message);
        LOGGER.error(message, ex);
    }

}
