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
package org.knime.core.webui.node.dialog.defaultdialog.internal.file;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.updates.ValueReference;

/**
 * Additional filters for the file chooser dialog. Used when the selection mode is 'folder' to decide which files within
 * the chosen folder get chosen.
 *
 * To make the contained parameters dependable on the filter mode, it is possible to add a field of type
 * {@link MultiFileSelectionMode} called {@code m_filterMode} and depend on it using {@link ValueReference}, i.e. add
 * these lines to your implementation:
 *
 * <pre>
 * &#64;ValueReference(FilterModeRef.class)
 * &#64;Persistor(DoNotPersist.class)
 * MultiFileSelectionMode m_filterMode = MultiFileSelectionMode.FILE;
 *
 * public interface FilterModeRef extends ParameterReference<MultiFileSelectionMode> {
 * }
 *
 * </pre>
 *
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @author Paul Bärnreuther
 */
public interface FileChooserFilters extends NodeParameters {

    /**
     * Use this on a field of type {@link MultiFileSelectionMode} to avoid persisting it. See {@link FileChooserFilters}
     * for an example.
     */
    final class DoNotPersist implements NodeParametersPersistor<MultiFileSelectionMode> {
        @Override
        public MultiFileSelectionMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return MultiFileSelectionMode.FILE;
        }

        @Override
        public void save(final MultiFileSelectionMode param, final NodeSettingsWO settingsWO) {
            // Do nothing
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][];
        }
    }

    /**
     * Should accept any file or folder as argument. If a folder passes the filter, then its children will be evaluated.
     * If a folder does not pass, its children will not be considered at all.
     *
     * @param root the folder being searched
     * @param path the file or folder to check
     * @param attrs the file attributes of the file or folder to check
     * @return true if the file or folder passes the filter, false otherwise
     */
    boolean passesFilter(final Path root, final Path path, BasicFileAttributes attrs);

    /**
     * Whether to follow symlinks or not. If true, then the file visitor will follow symlinks and include files that are
     * pointed to by symlinks, or in directories that are pointed to by symlinks. If false, then the file visitor will
     * not do any of that, and will treat symlinks as though they're not there.
     *
     * @return true if symlinks should be followed, false otherwise
     */
    boolean followSymlinks();

    /**
     * Get all files in a folder that pass the filter. If includeSubFolders is true, then all files in all
     * subdirectories will be considered as well (i.e. check files recursively).
     *
     * @param filters the filters to use
     * @param root the folder to search within
     * @param filterMode the mode to use when filtering files
     * @param includeSubFolders whether to include files in subfolders, i.e. whether to search recursively
     * @param limit the maximum number of files to return.
     * @return a {@link FilterResult} containing the files that pass the filter and the total number of files before
     *         filtering
     * @throws IOException if the file visitor throws an IOException
     * @throws IllegalArgumentException if the provided filter mode is not a multi-file selection mode
     */
    static FilterResult getPassingFilesInFolder(final FileChooserFilters filters, final Path root,
        final MultiFileSelectionMode filterMode, final boolean includeSubFolders, final int limit) throws IOException {

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Root path must be a folder");
        }
        var options = EnumSet.noneOf(FileVisitOption.class);
        if (filters.followSymlinks()) {
            options.add(FileVisitOption.FOLLOW_LINKS);
        }

        var walker = new FileChooserFilterFileVisitor(filterMode,
            (path, attrs) -> filters.passesFilter(root, path, attrs), limit, includeSubFolders);
        Files.walkFileTree(root, options, includeSubFolders ? Integer.MAX_VALUE : 1, walker);
        return walker.getFilterResult();
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
    record FilterResult( //
        List<Path> passingFiles, //
        int numFilesBeforeFiltering, //
        boolean numFilesBeforeFilteringIsOnlyLowerBound, //
        boolean numFilesAfterFilteringIsOnlyLowerBound //
    ) {
    }

}
