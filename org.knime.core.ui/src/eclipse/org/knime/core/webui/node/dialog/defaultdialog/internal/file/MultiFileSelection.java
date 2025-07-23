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
 *   Mar 27, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.file;

import java.util.Objects;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.widget.choices.Label;

/**
 * A setting that represents a selection of a single or many files. This setting is used for multi file selection.
 *
 * Note that although this class implements {@link WidgetGroup} it has a custom renderer and thus is not just the sum of
 * its widgets in the dialog. It additionally shows a preview of the selected files with the applied filters.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @param <F> the type of the file chooser filters
 */
public final class MultiFileSelection<F extends FileChooserFilters> implements Persistable, WidgetGroup {

    /**
     * Constructor. An initial non-null filter must be provided.
     *
     * @param filters the filters to use initially, not null.
     * @param path the path to the file or folder to select, not null.
     */
    public MultiFileSelection(final F filters, final FSLocation path) {
        m_filters = Objects.requireNonNull(filters, "Filters must not be null");
        m_path = Objects.requireNonNull(path, "Path must not be null");
    }

    /**
     * Constructor. An initial non-null filter must be provided. The initial selected path is set to the default path.
     *
     * @param filters the filters to use initially, not null.
     */
    public MultiFileSelection(final F filters) {
        this(filters, new FSLocation(FSCategory.LOCAL, ""));
    }

    /**
     * Only used for deserialization.
     */
    MultiFileSelection() {
    }

    /**
     * The selection mode (FILE or FOLDER).
     */
    @Widget(title = "Type", description = "The selection mode.")
    public FileOrFolder m_fileOrFolder = FileOrFolder.FILE; // NOSONAR

    /**
     * The root location of the file selection (if the selection mode is FOLDER), or the file itself (if the selection
     * mode is FILE).
     */
    @Widget(title = "Source", description = "The path to the file or folder to select.")
    public FSLocation m_path; // NOSONAR

    /**
     * Whether to include subfolders when selecting files. Only relevant when the selection mode is FOLDER.
     */
    @Widget(title = "Include Subfolders",
        description = "Whether to include subfolders when selecting multiple files within a folder.")
    public boolean m_includeSubfolders; // NOSONAR

    /**
     * The filters to use when selecting files. Only relevant when the selection mode is FOLDER. If you set this to
     * null, lots of things will break, so don't do that.
     */
    public F m_filters; // NOSONAR

    /**
     * The possible selection modes.
     */
    public enum FileOrFolder {
            /**
             * Select a single file.
             */
            @Label(value = "File", description = "Select a single file.")
            FILE, //
            /**
             * Select all files in a folder (optionally recursively).
             */
            @Label(value = "Folder", description = "Select all files in a folder. Optionally, it is then also possible "
                + "to define filter options to select only specific files within the selected folder.")
            FOLDER;
    }
}
