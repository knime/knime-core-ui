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
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector.FileChooserBackend;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("resource")
public final class FileChooserDataService {

    private final FileSystemConnector m_fsConnector;

    /**
     * This data service is used in the DefaultNodeDialog and can be accessed by the frontend using the name
     * "fileChooser".
     */
    public FileChooserDataService() {
        m_fsConnector = new FileSystemConnector();
    }

    /**
     * Closes all current file connections. To be called on deactivation of the service.
     */
    public void clear() {
        m_fsConnector.clear();
    }

    record Folder(List<Object> items, String path) {
        static FolderAndError asRootFolder(final List<Object> items) {
            return new FolderAndError(new Folder(items, null), Optional.empty());
        }

        static FolderAndError asNonRootFolder(final Path path, final List<Object> items, final String errorMessage) {
            final var folder = new Folder(items, path.toAbsolutePath().toString());
            return new FolderAndError(folder, Optional.ofNullable(errorMessage));
        }
    }

    /**
     * @param folder a representation of the path and the to be displayed items
     * @param errorMessage which if present explains why the folder is not the requested one (e.g. when the requested
     *            one does not exist)
     */
    record FolderAndError(Folder folder, Optional<String> errorMessage) {
    }

    /**
     * Get the items of the specified file system at the specified pair of path and folder name. If the requested path
     * is not accessible or does not exist, the next valid parent folder up to the root directory is used instead
     * together and returned with an appropriate error message.
     *
     * @param fileSystemId specifying the file system.
     * @param path the current path or null to reference the root level.
     * @param nextFolder - the name of the to be accessed folder relative to the path or ".." if the parent folder
     *            should be accessed. Set to null in order to access the path directly.
     * @return A list of items in the next folder possibly together with an error message explaining why the returned
     *         folder is not the requested one.
     *
     *
     * @throws IOException
     */
    public FolderAndError listItems(final String fileSystemId, final String path, final String nextFolder)
        throws IOException {
        final var fileChooserBackend = m_fsConnector.getFileChooserBackend(fileSystemId);
        final Path nextPath = getNextPath(path, nextFolder, fileChooserBackend.getFileSystem());
        if (nextPath == null) {
            return Folder.asRootFolder(getRootItems(fileChooserBackend));
        }
        final Deque<Path> pathStack = toFragments(fileChooserBackend, nextPath);
        return getItemsInFolder(fileChooserBackend, pathStack);
    }

    private static Deque<Path> toFragments(final FileChooserBackend fileChooserBackend, final Path nextPath) {
        final Deque<Path> subPaths = new ArrayDeque<>();
        final var pathFragments = StreamSupport.stream(nextPath.spliterator(), false).collect(Collectors.toList());
        final var emptyPath = fileChooserBackend.getFileSystem().getPath("");
        if (emptyPath != null) {
            subPaths.push(emptyPath);
        }
        final var rootPath = nextPath.getRoot();
        if (rootPath != null) {
            subPaths.push(rootPath);
        }
        for (Path pathFragent : pathFragments) {
            var lastPath = subPaths.peek();
            subPaths.push(lastPath.resolve(pathFragent));
        }
        return subPaths;
    }

    /**
     * Get the path of the file at the specified path
     *
     * @param fileSystemId specifying the file system.
     * @param path of the folder containing the file
     * @param fileName the name of the to be accessed file relative to the path.
     * @return the full path of the file
     */
    public String getFilePath(final String fileSystemId, final String path, final String fileName) {
        final var fileChooserBackend = m_fsConnector.getFileChooserBackend(fileSystemId);
        final var fileSystem = fileChooserBackend.getFileSystem();
        final Path nextPath = path == null ? fileSystem.getPath(fileName) : fileSystem.getPath(path, fileName);
        return nextPath.toString();
    }

    private static Path getNextPath(final String path, final String nextFolder, final FileSystem fileSystem) {
        if (path == null) {
            return nextFolder == null ? null : fileSystem.getPath(nextFolder);
        }
        if (nextFolder.equals("..")) {
            return fileSystem.getPath(path).toAbsolutePath().getParent();
        }
        return fileSystem.getPath(path, nextFolder);
    }

    private static List<Object> getRootItems(final FileChooserBackend fileChooserBackend) {
        final List<Object> out = new ArrayList<>();
        fileChooserBackend.getFileSystem().getRootDirectories()
            .forEach(p -> out.add(fileChooserBackend.pathToObject(p)));
        return out;
    }

    private static FolderAndError getItemsInFolder(final FileChooserBackend fileChooserBackend,
        final Deque<Path> pathStack) throws IOException {
        String errorMessage = null;
        while (!pathStack.isEmpty()) {
            final var folder = pathStack.pop();
            try {
                final var folderContent = listFilteredAndSortedItems(folder) //
                        .stream().map(fileChooserBackend::pathToObject).toList();
                return Folder.asNonRootFolder(folder, folderContent, errorMessage);
            } catch (NotDirectoryException ex) { //NOSONAR
                /**
                 * Do not set an error message in this case, since we intentionally get here when opening the file
                 * chooser with a preselected file path which we pass as folder path to get to the containing folder
                 * instead
                 */
            } catch (NoSuchFileException ex) { //NOSONAR
                if (errorMessage == null) {
                    errorMessage = String.format("The selected path %s does not exist", ex.getMessage());
                }
            } catch (AccessDeniedException ex) { //NOSONAR
                if (errorMessage == null) {
                    errorMessage = String.format("Access to the selected path %s is denied", ex.getMessage());
                }
            }
        }
        throw new IllegalStateException(
            "Something went wrong. There should be at least one valid path in the given stack.");
    }

    private static List<Path> listFilteredAndSortedItems(final Path folder) throws IOException {
        return Files.list(folder) //
            .filter(Files::isReadable) //
            .filter(t -> {
                try {
                    return !Files.isHidden(t);
                } catch (IOException ex) { // NOSONAR
                    return true;
                }
            }) //
            .sorted(
                Comparator.comparingInt(FileChooserDataService::getFileTypeOrdinal).thenComparing(Path::getFileName))
            .toList();
    }

    private static int getFileTypeOrdinal(final Path file) {
        return Files.isDirectory(file) ? 0 : 1;
    }

}
