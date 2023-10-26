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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;

/**
 * An instance of this class manages the open file connections of the {@link FileChooserDataService} and provides the
 * respective functionality depending on a String id per file system.
 *
 */
@SuppressWarnings("resource")
final class FileSystemConnector {

    final Map<String, FileChooserBackend> m_fileChooserBackends = new HashMap<String, FileChooserBackend>();

    final Set<FSConnection> m_connections = new HashSet<>();

    interface FileChooserBackend {
        FileSystem getFileSystem();

        Object pathToObject(Path path);
    }

    FileChooserBackend getFileChooserBackend(final String fileSystemId) {
        return m_fileChooserBackends.computeIfAbsent(fileSystemId, this::createFileChooserBackend);
    }

    private FileChooserBackend createFileChooserBackend(final String fileSystemId) {
        if (fileSystemId.equals("local")) {
            return new DefaultFileChooserBackend(createLocalFileSystem());
        }
        throw new IllegalArgumentException(String.format("%s is not a valid file system id", fileSystemId));
    }

    private FSFileSystem<?> createLocalFileSystem() {
        final var connection = DefaultFSConnectionFactory.createLocalFSConnection();
        m_connections.add(connection);
        return connection.getFileSystem();
    }

    /**
     * Closes all connections and clears the state.
     */
    public void clear() {
        for (final var conn : m_connections) {
            try {
                conn.close();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        m_connections.clear();
        m_fileChooserBackends.clear();
    }

}