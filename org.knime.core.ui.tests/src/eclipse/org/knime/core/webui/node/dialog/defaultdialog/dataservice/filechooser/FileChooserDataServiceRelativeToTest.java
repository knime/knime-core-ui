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
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.ListItemsConfig;
import org.mockito.MockedConstruction;

/**
 * Tests for the {@code relativeTo} parameter in {@link FileChooserDataService#listItems} and
 * {@link FileChooserDataService#getFilePath}, and for {@link FileChooserDataService#resolveRelativePath}.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class FileChooserDataServiceRelativeToTest {

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

    // ---- listItems: relativeTo ----

    @Test
    void testListItems_relativeParentFolders() throws IOException {
        final var dirA = Files.createDirectory(m_workDir.resolve("dirA"));
        final var dirB = Files.createDirectory(dirA.resolve("dirB"));

        final var result = m_service.listItems("local", dirB.toString(), null,
            new ListItemsConfig(false, null, false), m_workDir.toString());

        final var parents = result.folder().parentFolders();
        assertThat(parents.get(0).path()).isEqualTo(m_workDir.toString());
        assertThat(parents.get(0).name()).isNull();
        assertThat(parents.get(1).name()).isEqualTo(dirA.getFileName().toString());
        assertThat(parents.get(2).name()).isEqualTo(dirB.getFileName().toString());
    }

    // ---- getFilePath: relativeTo ----

    @Test
    void testGetFilePath_withRelativeTo_returnsRelativePath() throws IOException {
        final var dirA = Files.createDirectory(m_workDir.resolve("dirA"));
        final var dirB = Files.createDirectory(m_workDir.resolve("dirB"));
        Files.writeString(dirA.resolve("config.json"), "");

        final var result = m_service.getFilePath("local", dirA.toString(), "config.json", null, dirB.toString());

        assertThat(result.errorMessage()).isNull();
        assertThat(result.path()).isEqualTo(String.format("../%s/config.json", dirA.getFileName()));
    }

    // ---- resolveRelativePath ----

    @Test
    void testResolveRelativePath_withUpwardNavigation() throws IOException {
        final var resultsDir = Files.createDirectory(m_workDir.resolve("results"));
        final var preprocessingDir = Files.createDirectory(m_workDir.resolve("preprocessing"));
        final var relativePath = preprocessingDir
            .relativize(preprocessingDir.resolve("..").resolve(resultsDir.getFileName()).resolve("output.csv"));
        final var expectedPath = resultsDir.resolve("output.csv").normalize();

        final var resolved =
            m_service.resolveRelativePath("local", relativePath.toString(), preprocessingDir.toString());

        assertThat(resolved).isEqualTo(expectedPath.toString());
    }
}
