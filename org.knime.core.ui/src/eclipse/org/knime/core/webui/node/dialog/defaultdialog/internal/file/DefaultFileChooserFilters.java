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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.file;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FilterOptionsSettings;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Default implementation of {@link FileChooserFilters}. It can be used with any of the {@link MultiFileSelectionMode}
 * and shows file- and folder- filter options. In case only folders are selected, the file-fiter options are hidden.
 *
 * @author Paul Bärnreuther
 */
public class DefaultFileChooserFilters implements FileChooserFilters {

    /**
     * This value is automatically updated when the filter mode changes. It has to be called "filterMode" since the
     * frontend sets the value directly.
     */
    @ValueReference(FilterModeRef.class)
    @Persistor(DoNotPersist.class)
    MultiFileSelectionMode m_filterMode = MultiFileSelectionMode.FILE;

    /**
     * A ref to the current value of the filter mode. Use it within the filter options if those should depend on the
     * filter mode.
     */
    public interface FilterModeRef extends ParameterReference<MultiFileSelectionMode> {

    }

    static final class IsOnlyFoldersSelection implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FilterModeRef.class).isOneOf(MultiFileSelectionMode.FOLDERS);
        }
    }

    // ---- Sections ----------------------------------------------------
    @Section(title = "File filter options")
    @Effect(predicate = IsOnlyFoldersSelection.class, type = EffectType.HIDE)
    interface FileFilterSection {
    }

    @Section(title = "Folder filter options")
    interface FolderFilterSection {
    }

    @Section(title = "Link options")
    interface LinkOptionsSection {
    }

    // ---- File filters -------------------------------------------------

    @Widget(title = "Filter by file extension",
        description = "Enable filtering files by their extension (e.g. 'xlsx;xlsm').")
    @ValueReference(FileExtensionFilterEnabled.class)
    @Layout(FileFilterSection.class)
    boolean m_filterFilesExtension;

    static final class FileExtensionFilterEnabled implements BooleanReference {
    }

    @Widget(title = "File extensions", description = """
            Semicolon-separated list of file extensions to include (e.g. 'xlsx;xlsm;xls').
            Case-insensitive unless 'Case sensitive (extensions)' is enabled.
            """)
    @Layout(FileFilterSection.class)
    @Effect(predicate = FileExtensionFilterEnabled.class, type = EffectType.SHOW)
    String m_filesExtensionExpression = "";

    @Widget(title = "Case sensitive (extensions)",
        description = "Treat the entered extensions as case sensitive when matching.")
    @Layout(FileFilterSection.class)
    @Effect(predicate = FileExtensionFilterEnabled.class, type = EffectType.SHOW)
    boolean m_filesExtensionCaseSensitive;

    @Widget(title = "Filter by file name",
        description = "Enable filtering by file name pattern with wildcards or regular expression.")
    @ValueReference(FileNameFilterEnabled.class)
    @Layout(FileFilterSection.class)
    boolean m_filterFilesName;

    static final class FileNameFilterEnabled implements BooleanReference {
    }

    @Widget(title = "File name filter pattern", description = """
            Pattern for file name filtering. With type 'Wildcard', use '*' and '?'.
            With type 'Regex', enter a Java regular expression.
            """)
    @Layout(FileFilterSection.class)
    @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
    String m_filesNameExpression = "*";

    enum NameFilterType {
            @Label(value = "Wildcard", description = "Enable using '*' and '?' as wildcards.")
            WILDCARD, //
            @Label(value = "Regular Expression", description = "Enable using a Java regular expression.")
            REGEX;

        FileAndFolderFilter.FilterType toLegacyType() {
            return this == WILDCARD ? FileAndFolderFilter.FilterType.WILDCARD : FileAndFolderFilter.FilterType.REGEX;
        }
    }

    @Widget(title = "File name filter type", description = "Choose how to interpret the file name pattern.")
    @ValueSwitchWidget
    @Layout(FileFilterSection.class)
    @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
    NameFilterType m_filesNameFilterType = NameFilterType.WILDCARD;

    @Widget(title = "Case sensitive (names)", description = "Make file name filtering case sensitive.")
    @Layout(FileFilterSection.class)
    @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
    boolean m_filesNameCaseSensitive;

    @Widget(title = "Include hidden files", description = "Include hidden files in the selection.")
    @Layout(FileFilterSection.class)
    boolean m_includeHiddenFiles = true;

    @Widget(title = "Include special files", description = "Include special file types (workflows etc).")
    @Layout(FileFilterSection.class)
    boolean m_includeSpecialFiles = true;

    // ---- Folder filters -----------------------------------------------

    @Widget(title = "Filter by folder name",
        description = "Enable filtering of folders by name pattern before descending into them.")
    @ValueReference(FolderNameFilterEnabled.class)
    @Layout(FolderFilterSection.class)
    boolean m_filterFoldersName;

    static final class FolderNameFilterEnabled implements BooleanReference {
    }

    @Widget(title = "Folder name pattern", description = """
            Pattern for folder name filtering.
            Note that the pattern is applied to the path relative to the specified root folder.
            Use '*' and '?' with filter type 'Wildcard'.
            With type 'Regex', enter a Java regular expression.
            """)
    @Layout(FolderFilterSection.class)
    @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
    String m_foldersNameExpression = "*";

    @Widget(title = "Folder name filter type", description = "Choose how to interpret the folder name pattern.")
    @ValueSwitchWidget
    @Layout(FolderFilterSection.class)
    @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
    NameFilterType m_foldersNameFilterType = NameFilterType.WILDCARD;

    @Widget(title = "Case sensitive (folders)", description = "Make folder name filtering case sensitive.")
    @Layout(FolderFilterSection.class)
    @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
    boolean m_foldersNameCaseSensitive;

    @Widget(title = "Include hidden folders",
        description = "Descend into folders that are hidden (if they otherwise pass filters).")
    @Layout(FolderFilterSection.class)
    boolean m_includeHiddenFolders = true;

    @Widget(title = "Follow symlinks",
        description = "Follow symbolic links while traversing folders (only relevant when selecting a folder).")
    @Layout(LinkOptionsSection.class)
    boolean m_followSymlinks = false; // NOSONAR explicit initialization for clarity

    @Override
    public boolean passesFilter(final Path root, final Path path, final BasicFileAttributes attrs) {
        // We're constructing a FileAndFolderFilter here, as that is what is also used
        // in the model and was used in the old dialog. This way we ensure the same
        // filtering behavior in the dialog preview as in the model.
        FileAndFolderFilter filter = new FileAndFolderFilter(root, toFilterOptionsSettings());
        /**
         * Legacy behavior: The old filter used to check for each file whether its parent satisfies the folder name
         * filter option. With the new visitor, instead, we check for the correct folder name already when entering the
         * folder with one exception: This method is never called for the root folder. So to replicate old behavior, we
         * have to check the root folder here.
         */
        if (!attrs.isDirectory() && root.equals(path.getParent()) && !filter.testFolderName(root)) {
            return false;
        }
        return filter.test(path, attrs);
    }

    /**
     * Transforms this configuration to legacy {@link FilterOptionsSettings}.
     *
     * @return legacy {@link FilterOptionsSettings}
     */
    public FilterOptionsSettings toFilterOptionsSettings() {
        FilterOptionsSettings filterOptions = new FilterOptionsSettings();

        filterOptions.setFilterFilesByName(m_filterFilesName);
        filterOptions.setFilesNameCaseSensitive(m_filesNameCaseSensitive);
        filterOptions.setFilesNameExpression(m_filesNameExpression);
        filterOptions.setFilesNameFilterType(m_filesNameFilterType.toLegacyType());

        filterOptions.setFilterFilesByExtension(m_filterFilesExtension);
        filterOptions.setFilesExtensionCaseSensitive(m_filesExtensionCaseSensitive);
        filterOptions.setFilesExtensionExpression(m_filesExtensionExpression);

        filterOptions.setFilterFoldersByName(m_filterFoldersName);
        filterOptions.setFoldersNameCaseSensitive(m_foldersNameCaseSensitive);
        filterOptions.setFoldersNameExpression(m_foldersNameExpression);
        filterOptions.setFoldersNameFilterMode(m_foldersNameFilterType.toLegacyType());

        filterOptions.setIncludeSpecialFiles(m_includeSpecialFiles);
        filterOptions.setFollowLinks(m_followSymlinks);
        filterOptions.setIncludeHiddenFolders(m_includeHiddenFolders);
        filterOptions.setIncludeHiddenFiles(m_includeHiddenFiles);
        return filterOptions;
    }

    @Override
    public boolean followSymlinks() {
        return m_followSymlinks;
    }

}
