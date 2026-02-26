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
 *   Feb 27, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.FolderAndError;
import org.mockito.MockedConstruction;

/**
 * Tests for {@link FileChooserDataService} with multiple roots (e.g. on windows).
 *
 * @author Paul Bärnreuther
 */
class FileChooserDataServiceMultipleRootsTest {

    FileSystem m_fs;

    Path m_subDir;

    MockedConstruction<LocalFileChooserBackend> m_backendMock;

    FileSystemConnector m_fsConnector;

    FileChooserDataService m_service;

    @BeforeEach
    void setup() throws IOException {
        m_fs = JimfsTestUtil.newWindowsFileSystem();
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
        return m_service.listItems("local", path, nextFolder, FileChooserDataServiceListItemsTest.DEFAULT_CONFIG, null);
    }

    @Test
    void testNullPath_multipleRoots_showsRootOfRootsWithNullPath() throws IOException {
        final var result = listItems(null, null);

        assertThat(result.errorMessage()).isEmpty();
        assertThat(result.folder().isRootFolder()).isTrue();
        assertThat(result.folder().path()).isNull();
        assertThat(result.folder().items()).hasSize(2);
        final var roots = m_fs.getRootDirectories().iterator();
        final var backend = m_backendMock.constructed().get(0);
        verify(backend).directoryPathToObject(roots.next());
        verify(backend).directoryPathToObject(roots.next());
    }
}
