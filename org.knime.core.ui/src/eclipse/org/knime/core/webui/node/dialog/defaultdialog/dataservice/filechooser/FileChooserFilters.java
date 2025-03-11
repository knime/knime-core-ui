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
import java.util.List;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Additional filters for the file chooser dialog. Used when the selection mode is 'folder' to decide exactly what gets
 * displayed in the file chooser dialog.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public abstract class FileChooserFilters implements DefaultNodeSettings {

    /**
     * Should accept any file or folder as argument. If a folder passes the filter, then its children will be evaluated.
     * If a folder does not pass, its children will not be considered at all.
     *
     * @param root the folder being searched
     * @param path the file or folder to check
     * @return true if the file or folder passes the filter, false otherwise
     * @throws IllegalStateException if an IO exception is thrown while trying to filter the file or folder.
     */
    public abstract boolean passesFilter(final Path root, final Path path) throws IllegalStateException;

    /**
     * Get all files in a folder that pass the filter. If includeSubFolders is true, then all files in all
     * subdirectories will be considered as well (i.e. check files recursively).
     *
     * @param root the folder to search within
     * @param includeSubFolders whether to include files in subfolders, i.e. whether to search recursively
     * @param limit the maximum number of files to return.
     * @return a {@link FilterResult} containing the files that pass the filter and the total number of files before
     *         filtering
     * @throws IOException
     */
    public FilterResult getPassingFilesInFolder(final Path root, final boolean includeSubFolders, final int limit)
        throws IOException {

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path must be a folder");
        }

        var nextFolders = List.of(root);

        var numFilesEncountered = 0;
        var allFilesAccepted = new ArrayList<Path>();

        boolean isConsideredFilesOnlyLowerBound = false;

        do {
            var filesInNextFolders = nextFolders.stream() //
                .flatMap(FileChooserFilters::listFilesInFolder) //
                .toList();

            numFilesEncountered += filesInNextFolders.size();

            filesInNextFolders.stream() //
                .filter(path -> this.passesFilter(root, path)) //
                .limit(limit - allFilesAccepted.size()) //
                .forEach(allFilesAccepted::add);

            var unfilteredNextFolders = nextFolders.stream() //
                .flatMap(FileChooserFilters::listFoldersInFolder) //
                .map(Path::normalize) //
                .toList();

            var foldersCountBeforeFiltering = unfilteredNextFolders.size();

            nextFolders = unfilteredNextFolders.stream() //
                // TODO(UIEXT-2661): we need smarter handling of symlinks, since they can create loops.
                // For now, we just ignore them.
                .filter(path -> !Files.isSymbolicLink(path)) //
                .filter(path -> this.passesFilter(root, path)) //
                .toList();

            var foldersCountAfterFiltering = nextFolders.size();

            if (foldersCountAfterFiltering != foldersCountBeforeFiltering) {
                // we've filtered a folder out, so our files are only a lower bound.
                isConsideredFilesOnlyLowerBound = true;
            }
        } while (!nextFolders.isEmpty() && includeSubFolders && allFilesAccepted.size() < limit);

        // if we hit the limit then we don't know how many files are accepted, so we can only
        // say we have a lower bound.
        boolean isAcceptedFilesOnlyLowerBound = allFilesAccepted.size() == limit;

        return new FilterResult( //
            allFilesAccepted, //
            numFilesEncountered, //
            isConsideredFilesOnlyLowerBound, //
            isAcceptedFilesOnlyLowerBound //
        );
    }

    /**
     * Result of a filter operation. Contains the files that passed the filter and the total number of files before
     * filtering, which can be displayed to the user.
     *
     * @param passingFiles the files that passed the filter
     * @param numFilesBeforeFiltering the total number of files before filtering. Note that files in excluded folders
     *            aren't counted, so if there are folder filters, then this is only a lower bound.
     * @param numFilesBeforeFilteringIsOnlyLowerBound if true, then the number of files before filtering is only a lower
     *            bound. This usually means that there are files in excluded folders that are not counted.
     * @param numFilesAfterFilteringIsOnlyLowerBound if true, then the number of files after filtering is only a lower
     *            bound. This usually means that the limit was hit and there are more files that will be included when
     *            the node runs.
     */
    public static record FilterResult( //
        List<Path> passingFiles, //
        int numFilesBeforeFiltering, //
        boolean numFilesBeforeFilteringIsOnlyLowerBound, //
        boolean numFilesAfterFilteringIsOnlyLowerBound //
    ) {
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

    /**
     * Implementation of the {@link FileChooserFilters} for testing purposes. Could be deleted later.
     */
    public static class TestFileFilter extends FileChooserFilters {

        @Widget(title = "Do filtering?", description = "If checked, no files pass. If unchecked, all files pass.")
        boolean doFiltering;

        @Override
        public boolean passesFilter(final Path root, final Path path) throws IllegalStateException {
            return Files.isDirectory(path) || !doFiltering;
        }
    }
}
