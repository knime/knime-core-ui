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
 *   Oct 30, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.FolderAndError;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.ListItemsConfig;
import org.mockito.MockedConstruction;

/**
 * Tests for {@link FileChooserDataService#listItems}.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class FileChooserDataServiceListItemsTest {

    FileSystem m_fs;

    Path m_subDir;

    MockedConstruction<LocalFileChooserBackend> m_backendMock;

    FileSystemConnector m_fsConnector;

    FileChooserDataService m_service;

    static final ListItemsConfig DEFAULT_CONFIG = new ListItemsConfig(false, null, false);

    static final ListItemsConfig WRITER_CONFIG = new ListItemsConfig(true, null, false);

    @BeforeEach
    void setup() throws IOException {
        m_fs = JimfsTestUtil.newUnixFileSystem();
        final var root = m_fs.getRootDirectories().iterator().next();
        m_subDir = Files.createDirectory(root.resolve("subDir"));
        m_backendMock = FileSystemTestMockingUtil.mockLocalFileChooserBackend(m_fs, true);
        m_fsConnector = new FileSystemConnector();
        m_service = new FileChooserDataService(m_fsConnector);
    }

    @AfterEach
    void tearDown() throws IOException {
        m_backendMock.close();
        m_fsConnector.clear();
        m_fs.close();
    }

    private FolderAndError listItems(final String path, final String nextFolder) throws IOException {
        return m_service.listItems("local", path, nextFolder, DEFAULT_CONFIG, null);
    }

    // ---- navigation ----

    @Test
    void testNavigatesIntoSubfolder() throws IOException {
        final var deepDir = Files.createDirectory(m_subDir.resolve("deepDir"));

        final var result = listItems(m_subDir.toString(), deepDir.getFileName().toString());

        assertThat(result.errorMessage()).isEmpty();
        assertThat(result.folder().path()).isEqualTo(deepDir.toString());
        assertThat(result.folder().isRootFolder()).isFalse();
        final var parentFolders = result.folder().parentFolders();
        assertThat(parentFolders.get(parentFolders.size() - 2).name()).isEqualTo(m_subDir.getFileName().toString());
        assertThat(parentFolders.get(parentFolders.size() - 1).name()).isEqualTo(deepDir.getFileName().toString());
    }

    @Test
    void testWithPathAndNoFolder_navigatesIntoPath() throws IOException {
        final var nestedDir = Files.createDirectory(m_subDir.resolve("nested"));

        final var result = listItems(m_subDir.toString(), null);

        assertThat(result.errorMessage()).isEmpty();
        assertThat(result.folder().path()).isEqualTo(m_subDir.toString());
        verify(m_backendMock.constructed().get(0)).pathToObject(eq(nestedDir));
    }

    @Test
    void testNavigatesToParentWithDotDot() throws IOException {
        final var deepDir = Files.createDirectory(m_subDir.resolve("deepDir"));
        final var file = Files.writeString(m_subDir.resolve("aFile"), "");

        final var result = listItems(deepDir.toString(), "..");

        assertThat(result.errorMessage()).isEmpty();
        assertThat(result.folder().path()).isEqualTo(m_subDir.toString());
        final var backend = m_backendMock.constructed().get(0);
        verify(backend).pathToObject(eq(deepDir));
        verify(backend).pathToObject(eq(file));
    }

    // ---- error cases ----

    @Test
    void testMissingPath_fallsBackToExistingParent() throws IOException {
        final var missingDir = Files.createDirectory(m_subDir.resolve("missingDir"));
        Files.delete(missingDir);

        final var result = listItems(null, missingDir.toString());

        assertThat(result.errorMessage().get())
            .isEqualTo(String.format("The selected path %s does not exist", missingDir));
        assertThat(result.folder().path()).isEqualTo(m_subDir.toString());
    }

    @Test
    void testMissingPath_writerMode_noError() throws IOException {
        final var missingDir = Files.createDirectory(m_subDir.resolve("missingDir"));
        Files.delete(missingDir);

        final var result = m_service.listItems("local", null, missingDir.toString(), WRITER_CONFIG, null);

        assertThat(result.errorMessage()).isEmpty();
        assertThat(result.folder().path()).isEqualTo(m_subDir.toString());
        assertThat(result.filePathRelativeToFolder()).isEqualTo(missingDir.getFileName().toString());
    }

    @Test
    void testInvalidPath_showsError() throws IOException {
        final var invalidPath = "has\u0000null";

        final var result = listItems(null, invalidPath);

        assertThat(result.errorMessage().get())
            .isEqualTo(String.format("The selected path %s is not a valid path", invalidPath));
    }

    @Test
    void testNotAccessiblePath_showsError() throws IOException {
        final var inaccessiblePath = "../outsideRoot";

        final var result = listItems(null, inaccessiblePath);

        assertThat(result.errorMessage().get())
            .isEqualTo(String.format("The selected path %s is not accessible", inaccessiblePath));
    }

    /**
     * This test would work with a newer version of JIMFS, which we cannot install because of conflicting guava version
     */
    void testAccessDeniedPath_showsError() throws IOException {
        final var restrictedDir = Files.createDirectory(m_subDir.resolve("restricted"));
        Files.setPosixFilePermissions(restrictedDir, Set.<PosixFilePermission> of());

        final var result = listItems(null, restrictedDir.toString());

        assertThat(result.errorMessage().get())
            .isEqualTo(String.format("Access to the selected path %s is denied", restrictedDir));
    }

    @Test
    void testThrowsOnUnknownFileSystemId() {
        assertThrows(IllegalArgumentException.class,
            () -> m_service.listItems("notAValidFileSystemId", null, null, DEFAULT_CONFIG, null));
    }

    // ---- filters ----

    @Test
    void testFiltersByExtension() throws IOException {
        final var extensions = List.of("pdf", "png");
        Files.createDirectory(m_subDir.resolve("aSubDirectory"));
        Files.createFile(m_subDir.resolve("aFile.pdf"));
        Files.createFile(m_subDir.resolve("aFile.txt"));

        final var result = m_service.listItems("local", null, m_subDir.toString(),
            new ListItemsConfig(false, extensions, false), null);

        assertThat(result.folder().items()).hasSize(2); // one directory + one .pdf, no .txt
        assertThat(result.folder().items()).anyMatch(Item::isDirectory)
            .anyMatch(i -> !i.isDirectory() && i.name().endsWith(".pdf"));
    }

    @Test
    void testWorkflowFilterMode() throws IOException {
        Files.createDirectory(m_subDir.resolve("aSubDirectory"));
        Files.createFile(m_subDir.resolve("workflow.knwf"));
        Files.createFile(m_subDir.resolve("file.txt"));

        final var result =
            m_service.listItems("local", null, m_subDir.toString(), new ListItemsConfig(false, null, true), null);

        assertThat(result.folder().items()).hasSize(2); // one directory + one .knwf, no .txt
    }

    // ---- backend lifecycle ----

    @Test
    void testReusesBackendAcrossCalls() throws IOException {
        listItems(null, null);
        listItems(null, null);

        assertThat(m_backendMock.constructed()).hasSize(1);
    }

    @Test
    void testClearClosesBackendAndAllowsRebuild() throws IOException {
        listItems(null, null);
        m_fsConnector.clear();

        verify(m_backendMock.constructed().get(0)).close();

        listItems(null, null);
        assertThat(m_backendMock.constructed()).hasSize(2);
    }
}
