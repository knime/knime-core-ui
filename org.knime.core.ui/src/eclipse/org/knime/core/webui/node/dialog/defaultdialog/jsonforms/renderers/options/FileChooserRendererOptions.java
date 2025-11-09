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
 *   Nov 5, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;

/**
 * Shared option interfaces for file chooser renderers, mirroring the TypeScript type hierarchy.
 *
 * @author Paul Bärnreuther
 */
public final class FileChooserRendererOptions {

    private FileChooserRendererOptions() {
        // Utility class
    }

    /**
     * Options for connected file system
     */
    public interface ConnectedFSOptions {

        /**
         * @return the name of the connected file system
         */
        String getFileSystemType();

        /**
         * @return the fsSpecifier of the connected file system
         */
        String getFileSystemSpecifier();

        /**
         * @return true whenever there exists a portIndex but a connection could not be established
         */
        boolean isFileSystemConnectionMissing();

        /**
         * @return the index of the port this file chooser is connected to
         */
        int getPortIndex();
    }

    /**
     * Options for space file system
     */
    public interface SpaceFSOptions {

        /**
         * @return the mount ID to use for space file system access
         */
        String getMountId();

        /**
         * @return the path within the space to start browsing from
         */
        String getSpacePath();
    }

    /**
     * Reader-specific options (file extensions for filtering)
     */
    private interface ReaderOptions {

        /**
         * File extensions for FILE selection. Used to filter selectable files.
         *
         * @return array of file extensions
         */
        default Optional<String[]> getFileExtensions() {
            return Optional.empty();
        }
    }

    /**
     * Base options for all file choosers
     */
    private interface Base extends ReaderOptions {

        /**
         * If this option is false, local file system access is disabled even if the file system option LOCAL is set.
         *
         * @return whether the current execution environment is local (which enables local file system access)
         */
        default Optional<Boolean> getIsLocal() {
            return Optional.empty();
        }

        /**
         * @return options for connected file system. Required if file system option CONNECTED is used.
         */
        default Optional<ConnectedFSOptions> getConnectedFSOptions() {
            return Optional.empty();
        }

        /**
         * @return options for space file system. Required if file system option SPACE is used.
         */
        default Optional<SpaceFSOptions> getSpaceFSOptions() {
            return Optional.empty();
        }
    }

    /**
     * Writer-specific options
     */
    private interface WriterOptions {

        /**
         * @return whether this is a writer widget
         */
        default Optional<Boolean> getIsWriter() {
            return Optional.empty();
        }

        /**
         * File extension for FILE selection. Used to append the extension if not present in the user input.
         *
         * @return single file extension
         */
        default Optional<String> getFileExtension() {
            return Optional.empty();
        }
    }

    /**
     * Options for file systems selection
     */
    private interface FileSystemsOptions {

        /**
         * Available file systems. If not provided, all file systems are available.
         *
         * @return array of file system options
         */
        default Optional<FileSystemOption[]> getFileSystems() {
            return Optional.empty();
        }
    }

    /**
     * Options for String-based file selection
     */
    public interface StringFileChooserOptions extends SingleSelectionOptionsBase {

        /**
         * The single file system to use (required for String fields)
         *
         * @return the file system
         */
        FileSystemOption getFileSystem();
    }

    /**
     * Base options for single file/folder selection
     */
    private interface SingleSelectionOptionsBase extends Base, WriterOptions {

        /**
         * Whether a file or a folder is to be chosen
         *
         * @return the selection mode
         */
        default Optional<SingleFileSelectionMode> getSelectionMode() {
            return Optional.empty();
        }

        /**
         * Placeholder text to show in the file chooser input field
         *
         * @return the placeholder text
         */
        default Optional<String> getPlaceholder() {
            return Optional.empty();
        }

    }

    /**
     * Options for FileSelection-based file selection
     */
    public interface FileChooserOptions extends SingleSelectionOptionsBase, FileSystemsOptions {
    }

    /**
     * Filter configuration for multi-file selection
     */
    public interface Filters {

        /**
         * The name of the class defining the additional filter options
         *
         * @return the class identifier of the additional filter options
         */
        String getClassId();

        /**
         * The sub UI schema for the filter options
         *
         * @return the filter sub UI schema
         */
        Object getUiSchema();
    }

    /**
     * Options for MultiFileSelection-based file selection
     */
    public interface MultiFileChooserOptions extends Base, FileSystemsOptions {

        /**
         * The filter modes that are available to the user
         *
         * @return array of possible filter modes
         */
        MultiFileSelectionMode[] getPossibleFilterModes();

        /**
         * The filter configuration containing the UI schema and class identifier
         *
         * @return the filters configuration
         */
        Filters getFilters();

    }
}
