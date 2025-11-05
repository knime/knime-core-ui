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
 *   Nov 5, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.hiddenfeaturesnode;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;

/**
 * Parameters class demonstrating all combinations of the new file selection API.
 *
 * @author Paul Bärnreuther
 */
public class HiddenFeatureNewFileSelectionParameters implements NodeParameters {

    static final class DummyFileChooserFilters implements FileChooserFilters {

        @Override
        public boolean passesFilter(final Path root, final Path path, final BasicFileAttributes attrs) {
            return true;
        }

        @Override
        public boolean followSymlinks() {
            return false;
        }

        @Widget(title = "Dummy file setting", description = "A dummy checkbox for file filtering")
        boolean m_dummyFileSetting ;

        @Widget(title = "Dummy folder setting", description = "A dummy checkbox for folder filtering")
        boolean m_dummyFolderSetting ;
    }

    static final class ToggleToGetFileSystemErrorRef implements BooleanReference {
    }

    static final class TestFileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private Supplier<Boolean> m_computeFromValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_computeFromValueSupplier = initializer.computeFromValueSupplier(ToggleToGetFileSystemErrorRef.class);
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (!m_computeFromValueSupplier.get().booleanValue()) {
                return DefaultFSConnectionFactory::createLocalFSConnection;
            }
            return () -> {
                throw new InvalidSettingsException("Test what happens");
            };
        }
    }

    @Section(title = "Single File Selection (FileSelection)")
    interface SingleFileSelectionSection {
    }

    @Section(title = "Multi File Selection (MultiFileSelection)")
    @After(SingleFileSelectionSection.class)
    interface MultiFileSelectionSection {

        @Section(title = "Two modes", sideDrawer = true)
        interface SideDrawer1 {

        }

        @Section(title = "More modes", sideDrawer = true)
        @After(SideDrawer1.class)
        interface SideDrawer2 {

        }
    }

    @Section(title = "File System Options")
    @After(MultiFileSelectionSection.class)
    interface FileSystemOptionsSection {
    }

    @Section(title = "Writer Widgets")
    @After(FileSystemOptionsSection.class)
    interface WriterWidgetsSection {
    }

    @Section(title = "String Fields")
    @After(WriterWidgetsSection.class)
    interface StringFieldsSection {
    }

    @Section(title = "Custom File System")
    @After(StringFieldsSection.class)
    interface CustomFileSystemSection {
    }

    // === Single File Selection ===

    @Widget(title = "Single File", description = "FILE mode with extension")
    @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
    @Layout(SingleFileSelectionSection.class)
    FileSelection m_singleFile = new FileSelection();

    @Widget(title = "Single Folder", description = "FOLDER mode")
    @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
    @Layout(SingleFileSelectionSection.class)
    FileSelection m_singleFolder = new FileSelection();

    // === Multi File Selection ===

    @Widget(title = "Multi: FILE + FILES_IN_FOLDERS", description = "FileReaderWidget as separate annotation")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    @FileReaderWidget(fileExtensions = {"csv", "parquet"})
    @Layout(MultiFileSelectionSection.SideDrawer1.class)
    MultiFileSelection<DummyFileChooserFilters> m_multiFilePlusFolders =
        new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());

    @Widget(title = "Multi: More folder modes",
        description = "FILES_IN_FOLDERS, FOLDERS, FILES_AND_FOLDERS (vertical radio buttons)")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILES_IN_FOLDERS, MultiFileSelectionMode.FOLDERS,
        MultiFileSelectionMode.FILES_AND_FOLDERS})
    @Layout(MultiFileSelectionSection.SideDrawer2.class)
    MultiFileSelection<DummyFileChooserFilters> m_multiAllFolderModes =
        new MultiFileSelection<>(MultiFileSelectionMode.FILES_IN_FOLDERS, new DummyFileChooserFilters());

    // === File System Options ===

    @Widget(title = "Local only", description = "Only local file system")
    @FileSelectionWidget(SingleFileSelectionMode.FILE)
    @WithFileSystem(FileSystemOption.LOCAL)
    @Layout(FileSystemOptionsSection.class)
    FileSelection m_localOnly = new FileSelection();

    @Widget(title = "Local + Space", description = "Local and Space file systems")
    @WithFileSystem({FileSystemOption.LOCAL, FileSystemOption.SPACE})
    @Layout(FileSystemOptionsSection.class)
    FileSelection m_localAndSpace = new FileSelection();

    @Widget(title = "Connected only", description = "Only connected FS (disabled when no port)")
    @WithFileSystem(FileSystemOption.CONNECTED)
    @Layout(FileSystemOptionsSection.class)
    FileSelection m_connectedOnly = new FileSelection();

    @Widget(title = "All file systems (default)", description = "No @FileSystem annotation means all file systems")
    @Layout(FileSystemOptionsSection.class)
    FileSelection m_allFileSystems = new FileSelection();

    // === Writer Widgets ===

    @Widget(title = "File Writer", description = "FILE mode with FileWriterWidget")
    @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
    @FileWriterWidget(fileExtension = "csv")
    @Layout(WriterWidgetsSection.class)
    FileSelection m_fileWriter = new FileSelection();

    @Widget(title = "Folder Writer", description = "FOLDER mode with FileWriterWidget")
    @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
    @FileWriterWidget
    @Layout(WriterWidgetsSection.class)
    FileSelection m_folderWriter = new FileSelection();

    @Widget(title = "Multi File Writer", description = "MultiFileSelection with FileWriterWidget")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    @FileWriterWidget(fileExtension = "csv")
    @Layout(WriterWidgetsSection.class)
    MultiFileSelection<DummyFileChooserFilters> m_multiFileWriter =
        new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());

    @Widget(title = "String Writer", description = "String field with FileWriterWidget")
    @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
    @WithFileSystem(FileSystemOption.LOCAL)
    @FileWriterWidget(fileExtension = "csv")
    @Layout(WriterWidgetsSection.class)
    String m_stringWriter;

    // === String Fields ===

    @Widget(title = "String with File Selection",
        description = "String field with file selection (must have exactly one FS)")
    @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
    @WithFileSystem(FileSystemOption.LOCAL)
    @Layout(StringFieldsSection.class)
    String m_stringWithFileSelection = "";

    @Widget(title = "String with Folder Selection", description = "String field with folder selection")
    @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
    @WithFileSystem(FileSystemOption.CONNECTED)
    @Layout(StringFieldsSection.class)
    String m_stringWithFolderSelection = "";

    // === Custom File System ===

    @Widget(title = "Toggle to break file system",
        description = "Toggle this to trigger an error in the custom file system connection provider")
    @ValueReference(ToggleToGetFileSystemErrorRef.class)
    @Layout(CustomFileSystemSection.class)
    boolean m_toggleToGetFileSystemError;

    @Widget(title = "Custom File System (String)", description = "String with custom file system provider")
    @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
    @WithCustomFileSystem(connectionProvider = TestFileSystemConnectionProvider.class)
    @Layout(CustomFileSystemSection.class)
    String m_customFileSystemString = "";
}
