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
package org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileChooserFilters.FilterResult;

/**
 *
 * @author david
 */
final class FileChooserFilterFileVisitor extends SimpleFileVisitor<Path> {

    private final Predicate<Path> m_pathFilter;

    private final int m_limitToAcceptedFiles;

    private final List<Path> m_acceptedFilePaths;

    private int m_numEncounteredFiles;

    private boolean m_wereAnySubtreesSkipped;

    FileChooserFilterFileVisitor(final Predicate<Path> pathFilter, final int limitToAcceptedFiles) {
        m_pathFilter = pathFilter;
        m_limitToAcceptedFiles = limitToAcceptedFiles;

        m_acceptedFilePaths = new ArrayList<>();
        m_numEncounteredFiles = 0;
        m_wereAnySubtreesSkipped = false;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        boolean isFileLike = attrs.isRegularFile() || attrs.isOther();

        if (isFileLike) {
            ++m_numEncounteredFiles;
            boolean passesFilter = m_pathFilter.test(file);
            boolean limitNotHit = m_acceptedFilePaths.size() < m_limitToAcceptedFiles;
            if (passesFilter && limitNotHit) {
                m_acceptedFilePaths.add(file);
            }
        }

        return super.visitFile(file, attrs);
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        boolean shouldSkipSubtree = m_acceptedFilePaths.size() >= m_limitToAcceptedFiles || !m_pathFilter.test(dir);

        m_wereAnySubtreesSkipped |= shouldSkipSubtree;

        return shouldSkipSubtree //
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
