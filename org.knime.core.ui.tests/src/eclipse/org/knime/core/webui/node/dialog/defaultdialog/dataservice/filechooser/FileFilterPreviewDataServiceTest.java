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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewDataService.AdditionalFilterConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewDataService.PreviewResult;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class FileFilterPreviewDataServiceTest {

    @TempDir
    Path m_tempDir;

    Path m_file1;

    Path m_file2;

    Path m_subDir;

    Path m_file3;

    Path m_file4;

    FileSystemConnector m_fsConnector;

    FileFilterPreviewDataService m_service;

    @BeforeEach
    public void setUp() throws IOException {
        // Create files in the root (tempDir).
        m_file1 = m_tempDir.resolve("file1.txt");
        m_file2 = m_tempDir.resolve("file2.txt");
        Files.createFile(m_file1);
        Files.createFile(m_file2);

        // Create a subdirectory with additional files.
        m_subDir = m_tempDir.resolve("subdir");
        Files.createDirectory(m_subDir);
        m_file3 = m_subDir.resolve("file3.csv");
        m_file4 = m_subDir.resolve("file4.txt");
        Files.createFile(m_file3);
        Files.createFile(m_file4);

        m_fsConnector = new FileSystemConnector();
        m_service = new FileFilterPreviewDataService(m_fsConnector);
    }

    @AfterEach
    public void tearDown() throws IOException {
        m_fsConnector.clear();
    }

    @Test
    void testNonRecursiveList() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            List.of(), //
            false, //
            false, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        var successResult = (PreviewResult.Success)result;

        assertEquals( //
            2, //
            successResult.m_itemsAfterFiltering.size(), //
            "Expected 2 items in the list" //
        );
    }

    @Test
    void testRecursiveList() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        var successResult = (PreviewResult.Success)result;

        assertEquals( //
            4, //
            successResult.m_itemsAfterFiltering.size(), //
            "Expected 4 items in the list" //
        );
    }

    @Test
    void testPathsAreRelativeToProvidedFolder() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        var successResult = (PreviewResult.Success)result;

        for (var item : successResult.m_itemsAfterFiltering) {
            assertTrue( //
                Path.of(item).startsWith(m_tempDir), //
                "Expected the root of the file to be the temp directory" //
            );
        }

        // and check again using the subdir as root
        result = m_service.listItemsForPreview( //
            "local", //
            m_subDir.toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        successResult = (PreviewResult.Success)result;

        for (var item : successResult.m_itemsAfterFiltering) {
            assertTrue( //
                Path.of(item).startsWith(m_subDir), //
                "Expected the root of the file to be the subdir" //
            );
        }
    }

    @Test
    void testFiltersAreRespected() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(false), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        var successResult = (PreviewResult.Success)result;

        assertEquals( //
            0, //
            successResult.m_itemsAfterFiltering.size(), //
            "Expected 0 items in the list" //
        );
    }

    @Test
    void testExtensionsAreRespected() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.toString(), //
            List.of("csv"), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            fail("Failed to list files with error message: " + e.m_errorMessage);
        }

        var successResult = (PreviewResult.Success)result;

        assertEquals( //
            1, //
            successResult.m_itemsAfterFiltering.size(), //
            "Expected 1 item in the list" //
        );
    }

    @Test
    void testFailsWhenRootIsFile() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_file1.toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            assertEquals( //
                "Root path is not a folder", //
                e.m_errorMessage, //
                "Expected error message for file root" //
            );
        } else {
            fail("Expected an error when the root is a file");
        }
    }

    @Test
    void testFailsWhenRootDoesNotExist() throws IOException {
        var result = m_service.listItemsForPreview( //
            "local", //
            m_tempDir.resolve("nonexistent").toString(), //
            List.of(), //
            false, //
            true, //
            new AdditionalFilterConfiguration<>(new FileFilters(true), FileFilters.class) //
        );

        if (result instanceof PreviewResult.Error e) {
            assertEquals( //
                "Root path does not exist", //
                e.m_errorMessage, //
                "Expected error message for non-existent root" //
            );
        } else {
            fail("Expected an error when the root does not exist");
        }
    }

    static class FileFilters implements FileChooserFilters {

        private final boolean m_allowAll;

        FileFilters(final boolean allowAll) {
            this.m_allowAll = allowAll;
        }

        @Override
        public boolean passesFilter(final Path root, final Path path) throws IllegalStateException {
            return m_allowAll;
        }
    }
}
