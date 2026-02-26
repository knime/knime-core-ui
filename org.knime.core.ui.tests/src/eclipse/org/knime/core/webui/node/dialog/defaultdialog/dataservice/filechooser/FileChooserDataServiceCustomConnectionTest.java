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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.mockito.Mockito;

/**
 * Tests for custom {@link FSConnectionProvider} registration in {@link FileSystemConnector}.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class FileChooserDataServiceCustomConnectionTest {

    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    FileSystemConnector m_fsConnector;

    @BeforeEach
    void setup() {
        m_fsConnector = new FileSystemConnector();
    }

    @AfterEach
    void tearDown() {
        m_fsConnector.clear();
    }

    private FSConnectionProvider mockConnectionProvider() {
        try {
            final var provider = mock(FSConnectionProvider.class);
            final var mockConnection = mock(FSConnection.class);
            when(provider.getFileSystemConnection()).thenReturn(mockConnection);
            return provider;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void testRegisterGeneratesUniqueUUIDs() {
        final var id1 = m_fsConnector.registerCustomFileSystem(mockConnectionProvider(), "#/properties/field1", List.of());
        final var id2 = m_fsConnector.registerCustomFileSystem(mockConnectionProvider(), "#/properties/field2", List.of());

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).matches(UUID_PATTERN);
        assertThat(id2).matches(UUID_PATTERN);
    }

    @Test
    void testConnectionCreationIsLazy() throws IOException, InvalidSettingsException {
        final var provider = mockConnectionProvider();
        final var fileSystemId = m_fsConnector.registerCustomFileSystem(provider, "#/properties/field", List.of());

        verify(provider, Mockito.never()).getFileSystemConnection();

        m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();

        verify(provider, Mockito.times(1)).getFileSystemConnection();
    }

    @Test
    void testConnectionIsReused() throws IOException, InvalidSettingsException {
        final var provider = mockConnectionProvider();
        final var fileSystemId = m_fsConnector.registerCustomFileSystem(provider, "#/properties/field", List.of());

        m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();
        m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();

        verify(provider, Mockito.times(1)).getFileSystemConnection();
    }

    @Test
    void testOldConnectionClosedOnReregistration() throws IOException, InvalidSettingsException {
        final var provider1 = mockConnectionProvider();
        final var mockConnection1 = mock(FSConnection.class);
        when(provider1.getFileSystemConnection()).thenReturn(mockConnection1);

        final var scope = "#/properties/field";
        final var id1 = m_fsConnector.registerCustomFileSystem(provider1, scope, List.of());
        m_fsConnector.getFileChooserBackend(id1).getFileSystem();

        final var id2 = m_fsConnector.registerCustomFileSystem(mockConnectionProvider(), scope, List.of());
        assertThat(id1).isNotEqualTo(id2);

        m_fsConnector.getFileChooserBackend(id2).getFileSystem();

        verify(mockConnection1).close();
    }

    @Test
    void testThrowsUncheckedExceptionOnInvalidSettings() {
        final String errorMessage = "Test invalid settings";
        final FSConnectionProvider provider = () -> {
            throw new InvalidSettingsException(errorMessage);
        };
        final var fileSystemId = m_fsConnector.registerCustomFileSystem(provider, "#/properties/field", List.of());
        final var backend = m_fsConnector.getFileChooserBackend(fileSystemId);

        final var exception = assertThrows(RuntimeException.class, () -> backend.getFileSystem());
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }

    @Test
    void testClearClosesAllConnections() throws IOException, InvalidSettingsException {
        final var provider = mockConnectionProvider();
        final var mockConnection = mock(FSConnection.class);
        when(provider.getFileSystemConnection()).thenReturn(mockConnection);
        final var mockFileSystem = mock(FSFileSystem.class);
        when(mockConnection.getFileSystem()).thenReturn(mockFileSystem);

        final var fileSystemId = m_fsConnector.registerCustomFileSystem(provider, "#/properties/field", List.of());
        m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();

        m_fsConnector.clear();

        verify(mockConnection).close();
    }
}
