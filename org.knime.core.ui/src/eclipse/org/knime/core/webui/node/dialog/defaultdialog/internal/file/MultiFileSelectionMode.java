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
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.file;

/**
 * Enumeration of selection modes for multi-file selection widgets.
 *
 * @author Paul Baernreuther
 */
public enum MultiFileSelectionMode {
        /** Select a single file */
        FILE("File", "Select a single file."),
        /** Select a single folder */
        FOLDER("Folder", "Select a single folder."),
        /** Select a folder and apply filters to files within it */
        FILES_IN_FOLDERS("Files in folders", "Select a folder and apply filters to select files within it.", true,
            false),
        /** Select a folder and apply filters to subfolders within it */
        FOLDERS("Folders", "Select a folder and apply filters to select subfolders within it.", false, true),
        /** Select a folder and apply filters to both files and subfolders within it */
        FILES_AND_FOLDERS("Files and folders",
            "Select a folder and apply filters to select both files and subfolders within it.", true, true),
        /** Select a single workflow */
        WORKFLOW("Workflow", "Select a single workflow.");

    private final boolean m_includeFiles;

    private final boolean m_includeFolders;

    private final boolean m_isSingleSelection;

    private final String m_title;

    private final String m_description;

    MultiFileSelectionMode(final String title, final String description, final boolean includeFiles,
        final boolean includeFolders) {
        m_includeFiles = includeFiles;
        m_includeFolders = includeFolders;
        m_isSingleSelection = false;
        m_title = title;
        m_description = description;
    }

    MultiFileSelectionMode(final String title, final String description) {
        m_includeFiles = false;
        m_includeFolders = false;
        m_isSingleSelection = true;
        m_title = title;
        m_description = description;
    }

    /**
     * Whether the selected files can include non-folder files.
     *
     * @return whether files are included
     */
    public boolean includesFiles() {
        return m_includeFiles;
    }

    /**
     * Whether the selected files can include folders.
     *
     * @return whether folders are included
     */
    public boolean includesFolders() {
        return m_includeFolders;
    }

    /**
     * Asserts that this selection mode supports multi-selection.
     */
    public void assertMultiSelection() {
        if (m_isSingleSelection) {
            throw new IllegalStateException("The selection mode " + this + " does not support multi-selection.");
        }
    }

    /**
     * Gets the description of this selection mode.
     *
     * @return the description
     */
    public String getDescription() {
        return m_description;

    }

    /**
     * Gets the title of this selection mode.
     *
     * @return the title
     */
    public String getTitle() {
        return m_title;
    }

}
