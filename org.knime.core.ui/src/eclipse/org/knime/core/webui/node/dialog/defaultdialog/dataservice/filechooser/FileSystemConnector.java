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
 *   Oct 26, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * An instance of this class manages the open file connections of the {@link FileChooserDataService} and provides the
 * respective functionality depending on a String id per file system.
 *
 * @author Paul Bärnreuther
 */
public final class FileSystemConnector {

    final Map<String, FileChooserBackend> m_fileChooserBackends = new ConcurrentHashMap<>();

    /**
     * Tracks which fileSystemId belongs to which scope and indices combination
     */
    private record ScopeAndIndices(String scope, List<?> indices) {
    }

    final Map<ScopeAndIndices, String> m_scopeIndicesToFileSystemId = new ConcurrentHashMap<>();

    interface FileChooserBackend {
        FileSystem getFileSystem();

        Item pathToObject(Path path);

        /**
         * For root directories we know that a path should be displayed as directory but the file system lists them as
         * files for unavailable ones. Overwrite this method to adjust how paths where it is known that they should be
         * displayed as directories are displayed.
         *
         * @param path the directory path
         * @return a representation given to the front-end
         *
         */
        default Item directoryPathToObject(final Path path) {
            return pathToObject(path);
        }

        default boolean isAbsoluteFileSystem() {
            return true;
        }

        void close() throws IOException;
    }


    FileChooserBackend getFileChooserBackend(final String fileSystemId) {
        return m_fileChooserBackends.computeIfAbsent(fileSystemId, FileSystemConnector::createFileChooserBackend);
    }

    /**
     * Registers a custom file system backend using an FSConnectionProvider and returns a unique UUID as the
     * fileSystemId.
     *
     * @param connectionProvider the provider for the FSConnection to use for this file system
     * @param scope the scope of the field
     * @param indices the indices for array elements
     * @return a unique UUID that can be used as fileSystemId
     */
    public String registerCustomFileSystem(final FSConnectionProvider connectionProvider, final String scope,
        final List<?> indices) {
        final var scopeAndIndices = new ScopeAndIndices(scope, indices);

        // Close and remove old connection for this scope/indices combination if it exists
        final var oldFileSystemId = m_scopeIndicesToFileSystemId.get(scopeAndIndices);
        if (oldFileSystemId != null) {
            final var oldBackend = m_fileChooserBackends.remove(oldFileSystemId);
            if (oldBackend != null) {
                try {
                    oldBackend.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to close old connection", ex);
                }
            }
        }

        final var fileSystemId = UUID.randomUUID().toString();
        m_fileChooserBackends.put(fileSystemId, new CustomConnectionFileChooserBackend(connectionProvider));

        m_scopeIndicesToFileSystemId.put(scopeAndIndices, fileSystemId);

        return fileSystemId;
    }

    private static final Pattern PORT_PATTERN = Pattern.compile("connected(\\d+)");

    private static FileChooserBackend createFileChooserBackend(final String fileSystemId) {
        if (fileSystemId.equals("local")) {
            return new LocalFileChooserBackend();
        }
        if (fileSystemId.equals("relativeToCurrentHubSpace")) {
            return new HubFileChooserBackend();
        }
        if (fileSystemId.equals("embedded")) {
            return new DataAreaFileChooserBackend();
        }
        final var matcher = PORT_PATTERN.matcher(fileSystemId);
        if (matcher.matches()) {
            final var portIndex = Integer.parseInt(matcher.group(1));
            final var portObjectSpec = (FileSystemPortObjectSpec)DataServiceContext.get().getInputSpecs()[portIndex];
            return new ConnectedFileChooserBackend(portObjectSpec);
        }

        throw new IllegalArgumentException(String.format("%s is not a valid file system id", fileSystemId));
    }

    /**
     * Closes all connections and clears the state.
     */
    public void clear() {
        for (final var backend : m_fileChooserBackends.values()) {
            try {
                backend.close();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        m_fileChooserBackends.clear();
        m_scopeIndicesToFileSystemId.clear();
    }
}
