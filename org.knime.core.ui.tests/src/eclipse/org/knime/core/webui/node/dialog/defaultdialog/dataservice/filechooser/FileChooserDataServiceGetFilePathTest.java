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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * Tests for {@link FileChooserDataService#getFilePath} and {@link FileChooserDataService#resolveRelativePath}.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class FileChooserDataServiceGetFilePathTest {

    FileSystem m_fs;

    Path m_workDir;

    MockedConstruction<LocalFileChooserBackend> m_backendMock;

    FileSystemConnector m_fsConnector;

    FileChooserDataService m_service;

    @BeforeEach
    void setup() throws IOException {
        m_fs = JimfsTestUtil.newUnixFileSystem();
        m_workDir = m_fs.getPath(JimfsTestUtil.WORKING_DIRECTORY);
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

    // ---- getFilePath ----

    @Test
    void testReturnsAbsolutePath() throws IOException {
        final var file = Files.writeString(m_workDir.resolve("file.csv"), "");

        final var result = m_service.getFilePath("local", m_workDir.toString(), "file.csv", null, null);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.path()).isEqualTo(file.toString());
    }

    @Test
    void testRelativeFS_returnsPathRelativeToWorkingDirectory() throws IOException {
        // For relative file systems, the path should not be absolutized
        m_backendMock.close();
        m_backendMock = FileSystemTestMockingUtil.mockLocalFileChooserBackend(m_fs, false);

        final var subDir = Files.createDirectory(m_workDir.resolve("subDir"));
        Files.writeString(subDir.resolve("file.csv"), "");
        // For a relative FS, paths are relative to the working directory
        final var result = m_service.getFilePath("local", m_workDir.relativize(subDir).toString(), "file.csv",
            null, null);

        assertThat(result.errorMessage()).isNull();
        assertThat(result.path()).doesNotStartWith(m_workDir.getFileSystem().getSeparator());
    }

    @Test
    void testInvalidFileName_returnsError() {
        final var result = m_service.getFilePath("local", m_workDir.toString(), "bad\0name", null, null);

        assertThat(result.path()).isNull();
        assertThat(result.errorMessage()).isEqualTo("bad\0name is not a valid file name.");
    }

    // ---- getFilePath: append extension ----

    @Test
    void testAppendsExtension_forNonExistentFile() {
        final var result = m_service.getFilePath("local", m_workDir.toString(), "newFile", "csv", null);

        assertThat(result.path()).endsWith(".csv");
    }

    @Test
    void testDoesNotAppendExtension_forExistingWritableFile() throws IOException {
        Files.writeString(m_workDir.resolve("existing.csv"), "");

        final var result = m_service.getFilePath("local", m_workDir.toString(), "existing.csv", "csv", null);

        assertThat(result.path()).doesNotEndWith(".csv.csv");
    }

    @Test
    void testDoesNotAppendExtension_whenAlreadyPresent() {
        final var result = m_service.getFilePath("local", m_workDir.toString(), "file.csv", "csv", null);

        assertThat(result.path()).doesNotEndWith(".csv.csv");
    }

    @Test
    void testAppendsExtension_forExistingDirectory() throws IOException {
        final var dir = Files.createDirectory(m_workDir.resolve("myDir"));

        final var result =
            m_service.getFilePath("local", m_workDir.toString(), dir.getFileName().toString(), "csv", null);

        assertThat(result.path()).endsWith(".csv");
    }

}
