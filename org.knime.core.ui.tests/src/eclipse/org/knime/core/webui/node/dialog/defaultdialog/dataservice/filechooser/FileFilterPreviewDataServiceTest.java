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
 *   Apr 3, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewUtils.AdditionalFilterConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewUtils.PreviewResult;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.mockito.MockedConstruction;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class FileFilterPreviewDataServiceTest {

    FileSystem m_fs;

    Path m_tempDir;

    Path m_file1;

    Path m_file2;

    Path m_subDir;

    Path m_file3;

    Path m_file4;

    FileSystemConnector m_fsConnector;

    FileFilterPreviewDataService m_service;

    private MockedConstruction<LocalFileChooserBackend> m_fileChooserBackendMock;

    @BeforeEach
    public void setUp() throws IOException {
        m_fs = Jimfs.newFileSystem(Configuration.unix());
        final var root = m_fs.getRootDirectories().iterator().next();

        // Create a temporary directory for testing.
        m_tempDir = Files.createDirectory(root.resolve("tempDir"));

        // Create files in the root (tempDir).
        m_file1 = Files.createFile(m_tempDir.resolve("file1.txt"));
        m_file2 = Files.createFile(m_tempDir.resolve("file2.txt"));

        // Create a subdirectory with additional files.
        m_subDir = Files.createDirectory(m_tempDir.resolve("subdir"));
        m_file3 = Files.createFile(m_subDir.resolve("file3.csv"));
        m_file4 = Files.createFile(m_subDir.resolve("file4.txt"));

        m_fsConnector = new FileSystemConnector();
        m_service = new FileFilterPreviewDataService(m_fsConnector, () -> Set.of(testSettingsTree));

        m_fileChooserBackendMock =
            FileSystemTestMockingUtil.mockLocalFileChooserBackend(m_fs, false);
    }

    @AfterEach
    public void tearDown() throws IOException {
        m_fsConnector.clear();
        m_fileChooserBackendMock.close();
        m_fs.close();
    }

    @Test
    void testNonRecursiveList() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, false, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering).hasSize(2);
    }

    @Test
    void testRecursiveList() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering.size()).isEqualTo(4);
    }

    @Test
    void testNonRecursiveFolders() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FOLDERS, false, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering).hasSize(1);
        assertThat(successResult.m_itemsAfterFiltering.get(0)).isEqualTo("subdir");
    }

    @Test
    void testRecursiveFolders() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering).hasSize(1);
        assertThat(successResult.m_itemsAfterFiltering.get(0)).isEqualTo("subdir");
    }

    @Test
    void testNonRecursiveFilesAndFolders() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_AND_FOLDERS, false, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering).hasSize(3);
        assertThat(successResult.m_itemsAfterFiltering).containsExactlyInAnyOrder("file1.txt", "file2.txt", "subdir");
    }

    @Test
    void testRecursiveFilesAndFolders() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_AND_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering.size()).isEqualTo(5);
        assertThat(successResult.m_itemsAfterFiltering).containsExactly( //
            "file1.txt", "file2.txt", "subdir", "subdir/file3.csv", "subdir/file4.txt");
    }

    @Test
    void testPathsAreRelativeToProvidedFolder() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        for (var item : successResult.m_itemsAfterFiltering) {
            assertThat( //
                Path.of(item).startsWith(m_tempDir)).isFalse()
                    .as("Expected the root of the file to be the temp directory");

            assertThat( //
                ArrayUtils.contains(new int[]{1, 2}, Path.of(item).getNameCount())).isTrue()
                    .as("Expected either one or two levels of path");
        }
    }

    @Test
    void testPathsAreRelativeToProvidedSubfolder() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_subDir.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        for (var item : successResult.m_itemsAfterFiltering) {
            assertThat(Path.of(item).getNameCount()).isEqualTo(1).as("Expected plain filenames");
        }
    }

    @Test
    void testFiltersAreRespected() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(false)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Success.class);
        var successResult = (PreviewResult.Success)result;

        assertThat(successResult.m_itemsAfterFiltering).isEmpty();
    }

    @Test
    void testFailsWhenRootIsFile() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_file1.toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );
        assertThat(result).isInstanceOf(PreviewResult.Error.class);
        var errorResult = (PreviewResult.Error)result;

        assertThat(errorResult.m_errorMessage).isEqualTo("Root path is not a folder.");
    }

    @Test
    void testFailsWhenRootDoesNotExist() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.resolve("nonexistent").toString(), //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Error.class);
        var errorResult = (PreviewResult.Error)result;

        assertThat(errorResult.m_errorMessage).isEqualTo("Root path does not exist.");
    }

    @Test
    void testFailsWhenRootIsEmpty() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            "", //
            MultiFileSelectionMode.FILES_IN_FOLDERS, true, //
            constructFilterParameter(new FileFilters(true)) //
        );

        assertThat(result).isInstanceOf(PreviewResult.Error.class);
        var errorResult = (PreviewResult.Error)result;

        assertThat(errorResult.m_errorMessage).isEqualTo("Root path cannot be empty.");
    }

    void testThrowsIfNoMultiFileSelectionIsPresent() {
        final var wrongFileFilterParams = new AdditionalFilterConfiguration(
            JsonFormsDataUtil.getMapper().valueToTree(new FileFilters(true)), EmptySettings.class.getName());
        final var somePath = m_tempDir.toString();
        assertThrows(IllegalStateException.class, () -> m_service.listItemsForPreview("local", somePath,
            MultiFileSelectionMode.FILES_IN_FOLDERS, false, wrongFileFilterParams));
    }

    static final class EmptySettings implements NodeParameters {

    }

    static class FileFilters implements FileChooserFilters {

        boolean m_allowAll;

        FileFilters() {

        }

        FileFilters(final boolean allowAll) {
            this.m_allowAll = allowAll;
        }

        @Override
        public boolean passesFilter(final Path root, final Path path, final BasicFileAttributes attrs)
            throws IllegalStateException {
            return m_allowAll;
        }

        @Override
        public boolean followSymlinks() {
            return false;
        }
    }

    static private AdditionalFilterConfiguration constructFilterParameter(final FileFilters fileFilters) {
        final var json = JsonFormsDataUtil.getMapper().valueToTree(fileFilters);
        return new AdditionalFilterConfiguration(json, FileFilters.class.getName());
    }

    static final class TestSettings implements NodeParameters {

        @Widget(title = "", description = "")
        MultiFileSelection<FileFilters> m_testSetting;

    }

    static final Tree<WidgetGroup> testSettingsTree = new WidgetTreeFactory().createTree(TestSettings.class);
}
