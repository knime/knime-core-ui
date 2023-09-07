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
 *   Sep 7, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSFileSystem;

/**
 *
 * @author Paul Bärnreuther
 */
public class FileChooserDataService {
    private Iterable<Path> m_roots;

    private FSFileSystem<?> m_fileSystem;

    /**
     * This data service is used in the DefaultNodeDialog and can be accessed by the frontend using the name
     * "fileChooser".
     * @throws IOException
     */
    public FileChooserDataService(){
        //TODO: Workflow-aware final var workflowAware = m_fileSystem.getWorkflowAware().get();
    }

    <S> S withFileSystem(final Function<FSFileSystem, S> callback) throws IOException {
        S result;
        try (var fsConnection = DefaultFSConnectionFactory.createLocalFSConnection();
                var fileSystem = fsConnection.getFileSystem()) {
            result = callback.apply(fileSystem);
        }
        return result;
    }

    record Item(boolean isDirectory, String path) {
    }

    public List<Item> getRootItems() throws IOException {
        return withFileSystem(fs -> {
            final List<Item> out = new ArrayList<>();
            fs.getRootDirectories().forEach(p -> out.add(toItem(p)));
            return out;
        });
    }

    public List<Item> listItems(final String path) throws IOException {
        return withFileSystem(fs -> {
            try {
                return Files.list(fs.getPath(path)).map(this::toItem).toList();
            } catch (IOException ex) {
                return Collections.emptyList();
            }
        });
    }

    Item toItem(final Path path) {
        return new Item(Files.isDirectory(path), path.toString());
    }
}
