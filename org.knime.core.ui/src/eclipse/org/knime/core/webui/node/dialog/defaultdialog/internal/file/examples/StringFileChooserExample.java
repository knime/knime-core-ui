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
 *   Nov 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.file.examples;

import java.io.IOException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Example demonstrating usage of String fields with file chooser widgets.
 *
 * <p>
 * String fields can be configured with file chooser widgets using the same annotations as {@link FileSelection} fields.
 * The difference is that String fields store only the path as a string, without the file system information.
 * </p>
 *
 * <p>
 * String file choosers support two file system configuration approaches:
 * </p>
 * <ul>
 * <li>{@link WithFileSystem} - to restrict which file systems are available to the user</li>
 * <li>{@link WithCustomFileSystem} - to provide a dynamic custom file system connection via a StateProvider</li>
 * </ul>
 *
 * <p>
 * Example settings structure:
 * </p>
 *
 * <pre>
 * inputPath: "/Users/username/data.csv"
 * outputPath: "/output"
 * customFsPath: "s3://bucket/path/to/file.csv"
 * </pre>
 *
 * @author Paul Bärnreuther
 */
public final class StringFileChooserExample {

    private StringFileChooserExample() {
        // Utility class
    }

    /**
     * Settings class demonstrating String field usage with file chooser widgets.
     */
    public static final class Settings implements NodeParameters {

        /**
         * A string-based file reader that accepts CSV and TXT files from all available file systems. When no
         * {@link WithFileSystem} annotation is present, all file systems are available (LOCAL, SPACE, CONNECTED,
         * EMBEDDED, CUSTOM_URL).
         */
        @FileReaderWidget(fileExtensions = {"csv", "txt"})
        public String m_inputPath = "";

        /**
         * A string-based folder writer that only allows LOCAL and SPACE file systems. The tabs will be displayed in
         * standard order (LOCAL, SPACE). Other file systems (CONNECTED, EMBEDDED, CUSTOM_URL) will not be available.
         */
        @FileWriterWidget
        @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
        @WithFileSystem({FileSystemOption.LOCAL, FileSystemOption.SPACE})
        public String m_outputPath = "";

        /**
         * A string-based file chooser with a custom file system connection provided dynamically via a StateProvider.
         * This allows the file system to depend on node input and other settings values. Cannot be combined with
         * {@link WithFileSystem}.
         */
        @FileReaderWidget(fileExtensions = {"csv"})
        @WithCustomFileSystem(connectionProvider = CustomConnectionProvider.class)
        public String m_customFsPath = "";
    }

    /**
     * Example StateProvider that dynamically provides a custom file system connection. The connection can be based on
     * node inputs, other settings values, or external state.
     */
    static final class CustomConnectionProvider implements StateProvider<FSConnectionProvider> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            // Initialize with dependencies if needed (e.g., other settings, input specs)
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput context) {
            // In a real implementation, you would create and return an FSConnectionProvider
            // based on the context, input specs, or other settings values.
            return new FSConnectionProvider() {

                @Override
                public FSConnection getFileSystemConnection() throws InvalidSettingsException, IOException {
                    // Return the actual file system connection
                    // For example, an S3 connection, Azure Blob storage, etc.
                    throw new UnsupportedOperationException("Example only - implement actual connection logic");
                }
            };
        }
    }
}
