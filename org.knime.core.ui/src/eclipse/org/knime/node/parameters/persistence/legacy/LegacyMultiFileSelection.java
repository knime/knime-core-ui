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
 *   9 October 2025 (chaubold): created
 */
package org.knime.node.parameters.persistence.legacy;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSLocationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FilterOptionsSettings;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * A setting that represents the selection of multiple files. It is a specialization of MultiFileSelection for nodes
 * migrated from the classic UI, and maintains backwards compatibility by reproduce the legacy
 * {@code SettingsModelReaderFileChooser} settings structure.
 *
 * Note that although this class implements {@link WidgetGroup} it has a custom renderer and thus is not just the sum of
 * its widgets in the dialog. It additionally shows a preview of the selected files with the applied filters.
 *
 * @since 5.9
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class LegacyMultiFileSelection implements Persistable, WidgetGroup {

    /**
     * Constructor with initially selected path.
     *
     * @param filterMode the initial filter mode. It must be listed in the associated MultiFileSelectionWidget.value()
     *
     * @param path the path to the file or folder to select, not null.
     */
    public LegacyMultiFileSelection(final MultiFileSelectionMode filterMode, final FSLocation path) {
        m_path = Objects.requireNonNull(path, "Path must not be null");
        m_filterMode = filterMode;
    }

    /**
     * Constructor. The initial selected path is set to the default path.
     *
     * @param filterMode the initial filter mode. It must be listed in the associated MultiFileSelectionWidget.value()
     */
    public LegacyMultiFileSelection(final MultiFileSelectionMode filterMode) {
        this(filterMode, FSLocationUtil.getDefaultFSLocation());
    }

    LegacyMultiFileSelection() {
        // for framework.
    }

    /**
     * The selection mode (FILE or FOLDER).
     */
    @Widget(title = "Type", description = "The selection mode.")
    @Persist(configKey = "filter_mode")
    @PersistWithin("filter_mode")
    public MultiFileSelectionMode m_filterMode = MultiFileSelectionMode.FILE; // NOSONAR

    /**
     * The root location of the file selection (if the selection mode is FOLDER), or the file itself (if the selection
     * mode is FILE).
     */
    @Widget(title = "Source", description = "The path to the file or folder to select.")
    public FSLocation m_path; // NOSONAR

    static final class IncludeSubfoldersPersistor implements NodeParametersPersistor<Boolean> {
        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String mode = settings.getString("filter_mode", "FILE");
            boolean includeSubfolders = settings.getBoolean("include_subfolders", false);

            if (mode.equals("FILE")) {
                return false;
            } else {
                return includeSubfolders;
            }
        }

        @Override
        public void save(final Boolean param, final NodeSettingsWO settingsWO) {
            // Note, that was fileOrFolder==FOLDER && includeSubfolders before migrating to the modernUI
            settingsWO.addBoolean("include_subfolders", param);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"include_subfolders"}};
        }
    }

    /**
     * Whether to include subfolders when selecting files. Only relevant when the selection mode is FOLDER.
     */
    @Widget(title = "Include subfolders",
        description = "Whether to include subfolders when selecting multiple files within a folder.")
    @Persistor(IncludeSubfoldersPersistor.class)
    @PersistWithin("filter_mode")
    public boolean m_includeSubfolders; // NOSONAR

    /**
     * Filters for selecting only Excel files when the selection mode is FOLDER. For simplicity we only allow filtering
     * by extension (hidden files are accepted as in the legacy default). Further filter options can be added later
     * without breaking compatibility.
     */
    public static final class MultiFileChooserFilters implements FileChooserFilters {

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

        // ---- Predicates & References for Effects -------------------------
        // Value reference marker interfaces (must extend ParameterReference<Boolean>)
        interface FileExtensionFilterRef extends ParameterReference<Boolean> {
        }

        interface FileNameFilterRef extends ParameterReference<Boolean> {
        }

        interface FolderNameFilterRef extends ParameterReference<Boolean> {
        }

        // Effect predicate providers
        static final class FileExtensionFilterEnabled implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getBoolean(FileExtensionFilterRef.class).isTrue();
            }
        }

        static final class FileNameFilterEnabled implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getBoolean(FileNameFilterRef.class).isTrue();
            }
        }

        static final class FolderNameFilterEnabled implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getBoolean(FolderNameFilterRef.class).isTrue();
            }
        }

        // ---- File filters -------------------------------------------------

        @Widget(title = "Filter by file extension",
            description = "Enable filtering files by their extension (e.g. 'xlsx;xlsm').")
        @ValueReference(FileExtensionFilterRef.class)
        @Layout(FileFilterSection.class)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_FILTER_BY_EXTENSION)
        boolean m_filterFilesExtension;

        @Widget(title = "File extensions", description = """
                Semicolon-separated list of file extensions to include (e.g. 'xlsx;xlsm;xls').
                Case-insensitive unless 'Case sensitive (extensions)' is enabled.
                """)
        @Layout(FileFilterSection.class)
        @Effect(predicate = FileExtensionFilterEnabled.class, type = EffectType.SHOW)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_EXTENSION_EXPRESSION)
        String m_filesExtensionExpression = "";

        @Widget(title = "Case sensitive (extensions)",
            description = "Treat the entered extensions as case sensitive when matching.")
        @Layout(FileFilterSection.class)
        @Effect(predicate = FileExtensionFilterEnabled.class, type = EffectType.SHOW)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_EXTENSION_CASE_SENSITIVE)
        boolean m_filesExtensionCaseSensitive;

        @Widget(title = "Filter by file name",
            description = "Enable filtering by file name pattern with wildcards or regular expression.")
        @ValueReference(FileNameFilterRef.class)
        @Layout(FileFilterSection.class)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_FILTER_BY_NAME)
        boolean m_filterFilesName;

        @Widget(title = "File name filter pattern", description = """
                Pattern for file name filtering. With type 'Wildcard', use '*' and '?'.
                With type 'Regex', enter a Java regular expression.
                """)
        @Layout(FileFilterSection.class)
        @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_NAME_EXPRESSION)
        String m_filesNameExpression = "*";

        enum NameFilterType {
                @Label("Wildcard")
                WILDCARD, @Label("Regular Expression")
                REGEX;

            FileAndFolderFilter.FilterType toLegacyType() {
                return this == WILDCARD ? FileAndFolderFilter.FilterType.WILDCARD
                    : FileAndFolderFilter.FilterType.REGEX;
            }
        }

        @Widget(title = "File name filter type",
            description = "Choose how to interpret the file name pattern: wildcard or regular expression.")
        @ValueSwitchWidget
        @Layout(FileFilterSection.class)
        @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_NAME_FILTER_TYPE)
        NameFilterType m_filesNameFilterType = NameFilterType.WILDCARD;

        @Widget(title = "Case sensitive (names)", description = "Make file name filtering case sensitive.")
        @Layout(FileFilterSection.class)
        @Effect(predicate = FileNameFilterEnabled.class, type = EffectType.SHOW)
        @Persist(configKey = FilterOptionsSettings.CFG_FILES_NAME_CASE_SENSITIVE)
        boolean m_filesNameCaseSensitive;

        @Widget(title = "Include hidden files", description = "Include hidden files in the selection.")
        @Layout(FileFilterSection.class)
        @Persist(configKey = FilterOptionsSettings.CFG_INCLUDE_HIDDEN_FILES)
        boolean m_includeHiddenFiles = true;

        @Widget(title = "Include special files", description = "Include special file types (workflows etc).")
        @Layout(FileFilterSection.class)
        @Migrate(loadDefaultIfAbsent = true) // Setting was added in 4.3.3, and true by default
        @Persist(configKey = FilterOptionsSettings.CFG_INCLUDE_SPECIAL_FILES)
        boolean m_includeSpecialFiles = true;

        // ---- Folder filters -----------------------------------------------

        @Widget(title = "Filter by folder name",
            description = "Enable filtering of folders by name pattern before descending into them.")
        @ValueReference(FolderNameFilterRef.class)
        @Layout(FolderFilterSection.class)
        @Persist(configKey = FilterOptionsSettings.CFG_FOLDERS_FILTER_BY_NAME)
        boolean m_filterFoldersName;

        @Widget(title = "Folder name pattern", description = """
                Pattern for folder name filtering. Use '*' and '?' with filter type 'Wildcard'.
                With type 'Regex', enter a Java regular expression.
                """)
        @Layout(FolderFilterSection.class)
        @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
        @Persist(configKey = FilterOptionsSettings.CFG_FOLDERS_NAME_EXPRESSION)
        String m_foldersNameExpression = "*";

        @Widget(title = "Folder name filter type",
            description = "Choose how to interpret the folder name pattern: wildcard or regular expression.")
        @ValueSwitchWidget
        @Layout(FolderFilterSection.class)
        @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
        @Persist(configKey = FilterOptionsSettings.CFG_FOLDERS_NAME_FILTER_TYPE)
        NameFilterType m_foldersNameFilterType = NameFilterType.WILDCARD;

        @Widget(title = "Case sensitive (folders)", description = "Make folder name filtering case sensitive.")
        @Layout(FolderFilterSection.class)
        @Effect(predicate = FolderNameFilterEnabled.class, type = EffectType.ENABLE)
        @Persist(configKey = FilterOptionsSettings.CFG_FOLDERS_NAME_CASE_SENSITIVE)
        boolean m_foldersNameCaseSensitive;

        @Widget(title = "Include hidden folders",
            description = "Descend into folders that are hidden (if they otherwise pass filters).")
        @Layout(FolderFilterSection.class)
        @Persist(configKey = FilterOptionsSettings.CFG_INCLUDE_HIDDEN_FOLDERS)
        boolean m_includeHiddenFolders = true;

        @Widget(title = "Follow symlinks",
            description = "Follow symbolic links while traversing folders (only relevant when selecting a folder).")
        @Layout(LinkOptionsSection.class)
        @Migrate(loadDefaultIfAbsent = true) // Setting was added in 4.3.3, and false by default
        @Persist(configKey = FilterOptionsSettings.CFG_FOLLOW_LINKS)
        boolean m_followSymlinks = false;

        @Override
        public boolean passesFilter(final Path root, final Path path, final BasicFileAttributes attrs) {
            // We're constructing a FileAndFolderFilter here, as that is what is also used
            // in the model and was used in the old dialog. This way we ensure the same
            // filtering behavior in the dialog preview as in the model.
            FileAndFolderFilter filter = new FileAndFolderFilter(root, toFilterOptionsSettings());
            if (!attrs.isDirectory() && root.equals(path.getParent())) {
                /**
                 * Legacy behavior: The old filter used to check for each file whether its parent satisfies the folder
                 * name filter option. With the new visitor, instead, we check for the correct folder name already when
                 * entering the folder with one exception: This method is never called for the root folder. So to
                 * replicate old behavior, we have to check the root folder here.
                 */
                if (!filter.testFolderName(root)) {
                    return false;
                }
            }
            return filter.test(path, attrs);
        }

        private FilterOptionsSettings toFilterOptionsSettings() {
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

    @PersistWithin("filter_mode")
    @Persist(configKey = "filter_options")
    MultiFileChooserFilters m_filters = new MultiFileChooserFilters();

    /**
     * We use a dummy with a custom persistor that saves internals of the classic-UI File System Chooser. These settings
     * were used to cache the UI state and restore it when the dialog is opened again later. Even though we are no
     * longer showing the classic UI, we have to save these settings because the model still uses the old logic, and
     * that validates whether these settings are present. They are not used, so we provide default values.
     *
     * See https://knime-com.atlassian.net/browse/UIEXT-2085
     */
    @Persistor(FileSystemChooserInternalsPersistor.class)
    Void m_dummy;

    static class FileSystemChooserInternalsPersistor implements NodeParametersPersistor<Void> {
        @Override
        public Void load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return null;
        }

        @Override
        public void save(final Void param, final NodeSettingsWO settings) {
            final var internals = settings.addNodeSettings("file_system_chooser__Internals");
            internals.addBoolean("has_fs_port", false); // updated automatically at runtime when ports present
            internals.addBoolean("overwritten_by_variable", false);
            internals.addString("convenience_fs_category", "RELATIVE");
            internals.addString("relative_to", "knime.workflow");
            internals.addString("mountpoint", "LOCAL");
            internals.addString("spaceId", "");
            internals.addString("spaceName", "");
            internals.addInt("custom_url_timeout", 1000);
            internals.addBoolean("connected_fs", true);
        }

        @Override
        public String[][] getConfigPaths() {
            // We don't want these to be controllable by flow variable
            return new String[0][];
        }
    }

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
