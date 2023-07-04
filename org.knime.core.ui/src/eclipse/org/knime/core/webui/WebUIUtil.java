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
 *   Nov 11, 2022 (hornm): created
 */
package org.knime.core.webui;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.swt.program.Program;
import org.knime.core.node.NodeLogger;

/**
 * Utily methods relevant for the web UI.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class WebUIUtil {

    /**
     * System property that controls whether external requests from within CEF browser instances are to be blocked or
     * not.
     */
    public static final String BLOCK_ALL_EXTERNAL_REQUESTS_SYS_PROP = "chromium.block_all_external_requests";

    private static final String DEV_MODE_SYSTEM_PROPERTY = "org.knime.ui.dev.mode";

    private WebUIUtil() {
        // utility class
    }

    /**
     * Return whether the dev mode is set or not.
     *
     * @return isDevMode
     */
    public static boolean isInDevMode() {
        return Boolean.getBoolean(DEV_MODE_SYSTEM_PROPERTY);
    }

    /**
     * Tries to open the given URL in the default application. This is most likely a web browser if a HTTP(S) URL is
     * given. If a file:// URL is given, this is the systems default application associated with the file.
     * 
     * @param url
     * @param classForLogging
     *
     */
    public static void openURLWithDefaultApplicationAndLogDebug(final String url, final Class<?> classForLogging) {
        if (url.startsWith("http")) {
            launch(url, classForLogging);
        } else if (url.startsWith("file")) {
            var path = Paths.get(url.substring("file://".length()));
            if (Files.exists(path)) { // may open file explorer at root or create new file otherwise
                launch(path.toString(), classForLogging);
            } else {
                NodeLogger.getLogger(classForLogging).debugWithFormat("File at '%s' does not exist.", url);
            }
        } else {
            // don't do anything with other url-types
            NodeLogger.getLogger(classForLogging).debugWithFormat("URL '%s' can't be opened (not allowed).", url);
        }
    }

    private static void launch(final String url, final Class<?> classForLogging) {
        if (Program.launch(url)) {
            NodeLogger.getLogger(classForLogging)
                .debugWithFormat("Opening URL '%s' externally in default application ...", url);
        } else {
            NodeLogger.getLogger(classForLogging)
                .error("Failed to open URL in default application. The URL is: " + url);
        }
    }

}
