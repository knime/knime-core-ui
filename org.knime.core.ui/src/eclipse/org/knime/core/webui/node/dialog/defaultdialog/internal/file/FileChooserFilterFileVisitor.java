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
 *   Apr 15, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters.FilterResult;

/**
 *
 * @author david
 */
final class FileChooserFilterFileVisitor extends SimpleFileVisitor<Path> {

    private final BiPredicate<Path, BasicFileAttributes> m_pathFilter;

    private final int m_limitToAcceptedFiles;

    private final List<Path> m_acceptedFilePaths = new ArrayList<>();

    private final boolean m_includeSubfolders;

    private final boolean m_includeFiles;

    private final boolean m_includeFolders;

    /**
     * Counts files AND folders encountered.
     */
    private int m_numEncounteredFiles;

    private int m_numEncounteredFolders;

    private boolean m_wereAnySubtreesSkipped;

    FileChooserFilterFileVisitor(final MultiFileSelectionMode filterMode,
        final BiPredicate<Path, BasicFileAttributes> pathFilter, final int limitToAcceptedFiles,
        final boolean includeSubfolders) {
        m_includeFiles = filterMode.includesFiles();
        m_includeFolders = filterMode.includesFolders();
        m_pathFilter = pathFilter;
        m_limitToAcceptedFiles = limitToAcceptedFiles;
        m_includeSubfolders = includeSubfolders;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

        /**
         * also called for directories (if max depth is hit by Files.walkFileTree) for these directories
         * #preVisitDirectory is not being invoked.
         */
        if (attrs.isDirectory()) {
            /* Tested before files because for a Windows Junction attrs.isOther() and attrs.isDirectory() return true
             * but we want to treat them as directories */
            if (m_includeFolders) {
                testAndAddFile(file, attrs);
            }
        } else if (attrs.isRegularFile() || attrs.isOther()) {
            if (m_includeFiles) {
                testAndAddFile(file, attrs);
            }
        } else {
            // we only care for files and folders
        }
        return super.visitFile(file, attrs);
    }

    void testAndAddFile(final Path file, final BasicFileAttributes attrs) {
        testAndAddFile(file, m_pathFilter.test(file, attrs));
    }

    void testAndAddFile(final Path file, final boolean passesFilter) {
        ++m_numEncounteredFiles;
        boolean limitNotHit = m_acceptedFilePaths.size() < m_limitToAcceptedFiles;
        if (passesFilter && limitNotHit) {
            m_acceptedFilePaths.add(file);
        }
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

        final var isRootDir = m_numEncounteredFolders == 0;
        m_numEncounteredFolders++;
        if (isRootDir) {
            return super.preVisitDirectory(dir, attrs);
        }
        final var limitReached = m_acceptedFilePaths.size() >= m_limitToAcceptedFiles;
        final var shouldSkipFolder = limitReached || !m_pathFilter.test(dir, attrs);
        if (m_includeSubfolders && m_includeFolders) {
            testAndAddFile(dir, !shouldSkipFolder);
        }

        m_wereAnySubtreesSkipped |= shouldSkipFolder;

        return shouldSkipFolder //
            ? FileVisitResult.SKIP_SUBTREE //
            : super.preVisitDirectory(dir, attrs);
    }

    FilterResult getFilterResult() {
        return new FilterResult( //
            m_acceptedFilePaths, //
            m_numEncounteredFiles, //
            m_wereAnySubtreesSkipped, //
            m_acceptedFilePaths.size() >= m_limitToAcceptedFiles //
        );
    }

}
