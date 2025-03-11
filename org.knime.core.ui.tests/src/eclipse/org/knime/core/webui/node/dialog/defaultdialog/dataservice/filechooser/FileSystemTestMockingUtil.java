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
 *   Apr 22, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import static org.mockito.Mockito.when;

import java.nio.file.FileSystem;

import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector.FileChooserBackend;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * Since e.g. {@link DefaultFSConnectionFactory} throws an error in case no local file system is registered, we should
 * make tests independent from such constructions and mock the file system.
 *
 * @author Paul Bärnreuther
 */
final class FileSystemTestMockingUtil {

    private FileSystemTestMockingUtil() {
        // prevent instantiation
    }

    /**
     * Mocks the construction of a file chooser backend. Make sure to close this mock after usage.
     *
     * @param <T> the type of the backend
     * @param backendType the type of the backend
     * @param fileSystem the file system to be used
     * @param isAbsolute whether the file system is absolute or not
     * @return the mocked construction
     */
    static <T extends FileChooserBackend> MockedConstruction<T> mockLocalFileChooserBackend(final Class<T> backendType,
        final FileSystem fileSystem, final boolean isAbsolute) {
        return Mockito.mockConstruction(backendType, (mock, context) -> {
            when(mock.getFileSystem()).thenReturn(fileSystem);
            when(mock.pathToObject(ArgumentMatchers.any())).thenCallRealMethod();
            when(mock.isAbsoluteFileSystem()).thenReturn(isAbsolute);
        });

    }

}
