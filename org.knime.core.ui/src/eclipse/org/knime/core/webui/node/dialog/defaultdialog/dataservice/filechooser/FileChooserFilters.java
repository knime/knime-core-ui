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
 *   Mar 26, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Additional filters for the file chooser dialog. Used when the selection mode is 'folder' to decide exactly what gets
 * displayed in the file chooser dialog.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
abstract class FileChooserFilters {

    /**
     * Should accept any file or folder as argument. If a folder passes the filter, then its children will be evaluated.
     * If a folder does not pass, its children will not be considered at all.
     *
     * @param path
     * @return true if the file or folder passes the filter, false otherwise
     */
    public abstract boolean passesFilter(final Path root, final Path path) throws IllegalStateException;

    public FilterResult getPassingFilesInFolderRecursively(final Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path must be a folder");
        }

        var nextFolders = List.of(root);
        var allFilesEncountered = new ArrayList<Path>(listFilesInFolder(root).toList());

        System.out.println("Initial folder: " + root.toString());
        try (var stream = Files.list(root)) {
            System.out.println("All entries in initial folder: " + stream.toList());
        }

        do {
            nextFolders = nextFolders.stream() //
                .flatMap(FileChooserFilters::listFoldersInFolder) //
                .filter(path -> !Files.isSymbolicLink(path)) //
                .map(Path::normalize) //
                .filter(path -> this.passesFilter(root, path)) //
                .toList();

            nextFolders.stream() //
                .flatMap(FileChooserFilters::listFilesInFolder) //
                .forEach(allFilesEncountered::add);
        } while (!nextFolders.isEmpty());

        var totalFilesBeforeFiltering = allFilesEncountered.size();

        var passingFiles = allFilesEncountered.stream() //
            .filter(path -> this.passesFilter(root, path)) //
            .toList();

        return new FilterResult(passingFiles, totalFilesBeforeFiltering);
    }

    public static record FilterResult(List<Path> passingFiles, int numFilesBeforeFiltering) {
    }

    private static Stream<Path> listFolder(final Path folder) {
        try {
            return Files.list(folder);
        } catch (IOException e) {
            throw new IllegalStateException("Exception was thrown while trying to list folder " + folder, e);
        }
    }

    private static Stream<Path> listFilesInFolder(final Path folder) {
        return listFolder(folder) //
            .filter(Files::isRegularFile);
    }

    private static Stream<Path> listFoldersInFolder(final Path folder) {
        return listFolder(folder) //
            .filter(Files::isDirectory);
    }

    public static class TestFileFilter extends FileChooserFilters {

        String fileFormat;

        String[] fileExtensions;

        String filenamePatternType;

        String filenamePattern;

        boolean filenamePatternCaseSensitive;

        boolean includeHiddenFiles;

        String folderNamePatternType;

        String folderNamePattern;

        boolean folderNamePatternCaseSensitive;

        boolean includeHiddenFolders;

        boolean followLinks;

        boolean includeSubFolders;

        @Override
        public boolean passesFilter(final Path root, final Path path) throws IllegalStateException {
            // find depth of path relative to root
            var depth = root.relativize(path).getNameCount();
            if (!includeSubFolders && depth > 1) {
                return false;
            }

            try {
                return Files.isDirectory(path) //
                    ? folderPassesFilter(path) //
                    : filePassesFilter(path);
            } catch (IOException e) {
                throw new IllegalStateException("Exception was thrown while trying to filter path " + path, e);
            }
        }

        boolean filePassesFilter(final Path file) throws IOException {
            if (fileFormat.equals("custom") && !extractExtension(file) //
                .map(this::isValidExtension) //
                .orElse(false)) {
                return false;
            }

            if (!includeHiddenFiles && Files.isHidden(file)) {
                return false;
            }

            // TODO: implement filename pattern matching

            return true;
        }

        boolean folderPassesFilter(final Path folder) throws IOException {
            if (!includeHiddenFolders && Files.isHidden(folder)) {
                return false;
            }

            // TODO: implement folder name pattern matching

            return true;
        }

        private static Optional<String> extractExtension(final Path px) {
            if (Files.isDirectory(px)) {
                throw new IllegalArgumentException("Path must be a file, not a folder");
            }

            var fileName = px.getFileName().toString();
            var lastDot = fileName.lastIndexOf('.');
            if (lastDot <= 0 || lastDot == fileName.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(fileName.substring(lastDot + 1));
        }

        private boolean isValidExtension(final String extension) {
            return Arrays.stream(fileExtensions).anyMatch(extension::equalsIgnoreCase);
        }

        @Override
        public String toString() {
            return "TestFileFilter [fileFormat=" + fileFormat + ", fileExtensions=" + Arrays.toString(fileExtensions)
                + ", filenamePatternType=" + filenamePatternType + ", filenamePattern=" + filenamePattern
                + ", filenamePatternCaseSensitive=" + filenamePatternCaseSensitive + ", includeHiddenFiles="
                + includeHiddenFiles + ", folderNamePatternType=" + folderNamePatternType + ", folderNamePattern="
                + folderNamePattern + ", folderNamePatternCaseSensitive=" + folderNamePatternCaseSensitive
                + ", includeHiddenFolders=" + includeHiddenFolders + ", followLinks=" + followLinks
                + ", includeSubFolders=" + includeSubFolders + "]";
        }
    }
}
