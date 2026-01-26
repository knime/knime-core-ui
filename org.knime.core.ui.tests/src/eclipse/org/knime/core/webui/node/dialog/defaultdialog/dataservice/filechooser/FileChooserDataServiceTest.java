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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.FolderAndError;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileChooserDataService.ListItemsConfig;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileSystemConnector.FileChooserBackend;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class FileChooserDataServiceTest {

    FileSystemConnector m_fsConnector;

    FileChooserDataService m_service;

    @BeforeEach
    void setup() {
        m_fsConnector = new FileSystemConnector();
        m_service = new FileChooserDataService(m_fsConnector);
    }

    @AfterEach
    void tearDown() {
        m_fsConnector.clear();
    }

    @Nested
    class AbsoluteFileSystemTest extends NestedFileChooserDataServiceTest {

        /**
         * Simulating the root folder of this test file system
         */
        @TempDir
        private Path m_tempRootFolder;

        @Override
        Path getDefaultDirectory() {
            return m_tempRootFolder;
        }

        @Override
        void verifyRootItemConstruction(final Path item, final FileChooserBackend localFileChooserBackend) {
            verify(localFileChooserBackend).directoryPathToObject(eq(item));
        }

        @Override
        FileSystem getFileSystem() {
            final var fileSystem = mock(FileSystem.class);
            when(fileSystem.getRootDirectories()).thenReturn(List.of(m_tempRootFolder));
            return fileSystem;
        }

        @Override
        boolean useAbsoluteFileSystem() {
            return true;
        }

        @Override
        List<Path> getItemsInInitialFolder() {
            return List.of(getDefaultDirectory());
        }

        @Test
        void testRelativeInputPath() throws IOException {
            when(super.m_fileSystem.getPath(eq(""))).thenReturn(m_tempRootFolder);
            final var deletedFolder = Files.createTempDirectory(m_subFolder, "aDirectory");
            Files.delete(deletedFolder);
            final var invalidPath = super.pathToString(m_tempRootFolder.relativize(deletedFolder));

            final var result =
                new PerformListItemsBuilder().asWriter().withFolder(invalidPath).build().performListItems();

            assertThat(result.errorMessage()).isEmpty();
            assertThat(result.folder().path()).isEqualTo(m_subFolder.toString());
            assertThat(result.filePathRelativeToFolder()).isEqualTo(m_subFolder.relativize(deletedFolder).toString());
        }

        @Test
        void testThrowsOnWrongFileSystemId() {
            final var performListItems =
                new PerformListItemsBuilder().withFileSystemId("notAValidFileSystemId").build();
            assertThrows(IllegalArgumentException.class, performListItems::performListItems);
        }

        @Test
        void testReusesFileSystem() throws IOException {
            final var performListItems = new PerformListItemsBuilder().fromDataService(m_service).build();
            performListItems.performListItems();
            performListItems.performListItems();
            assertThat(fileChooserBackendMock.constructed()).hasSize(1);
        }

        @Test
        void testClosesAndClearsFileSystemOnClear() throws IOException {
            final var performListItems = new PerformListItemsBuilder().fromDataService(m_service).build();
            performListItems.performListItems();
            m_fsConnector.clear();
            verify(fileChooserBackendMock.constructed().get(0)).close();
            performListItems.performListItems();
            assertThat(fileChooserBackendMock.constructed()).hasSize(2);
        }

        @Override
        void assertRootFolderPath(final String path) {
            assertThat(path).isNull();
        }

    }

    @Nested
    class RelativeFileSystemTest extends NestedFileChooserDataServiceTest {

        @TempDir
        private Path m_emptyPathDirectory;

        @Override
        Path getDefaultDirectory() {
            return m_emptyPathDirectory;
        }

        @Override
        void verifyRootItemConstruction(final Path item, final FileChooserBackend localFileChooserBackend) {
            verify(localFileChooserBackend).pathToObject(eq(item));
        }

        @Override
        FileSystem getFileSystem() {
            final var fileSystem = mock(FileSystem.class);
            when(fileSystem.getPath(eq(""))).thenReturn(getDefaultDirectory());
            return fileSystem;
        }

        @Override
        boolean useAbsoluteFileSystem() {
            return false;
        }

        @Override
        List<Path> getItemsInInitialFolder() throws IOException {
            return Files.list(getDefaultDirectory()).toList();
        }

        @Test
        void testAbsoluteInputPath() throws IOException {
            final var absolutePath = super.pathToString(m_subFolder);

            final var result =
                new PerformListItemsBuilder().asWriter().withFolder(absolutePath).build().performListItems();

            assertThat(result.errorMessage()).isEmpty();
            assertRootFolderPath(result.folder().path());
            assertThat(result.filePathRelativeToFolder()).isEqualTo(relativizeFromRoot(m_subFolder).toString());
        }

        static Path relativizeFromRoot(final Path path) {
            final var root = path.getRoot();
            return root.relativize(path);
        }

        @Override
        void assertRootFolderPath(final String path) {
            assertThat(path).isEqualTo(".");
        }

    }

    abstract class NestedFileChooserDataServiceTest {

        /**
         * A folder within the default folder
         */
        protected Path m_subFolder;

        protected MockedConstruction<LocalFileChooserBackend> fileChooserBackendMock;

        protected FileSystem m_fileSystem;

        abstract Path getDefaultDirectory();

        abstract void verifyRootItemConstruction(Path item, FileChooserBackend localFileChooserBackend);

        abstract FileSystem getFileSystem();

        abstract boolean useAbsoluteFileSystem();

        abstract List<Path> getItemsInInitialFolder() throws IOException;

        @BeforeEach
        void mockFileChooserBackend() throws IOException {
            m_subFolder = Files.createTempDirectory(getDefaultDirectory(), "directoryPath");
            m_fileSystem = getFileSystem();
            fileChooserBackendMock = FileSystemTestMockingUtil
                .mockLocalFileChooserBackend(LocalFileChooserBackend.class, m_fileSystem, useAbsoluteFileSystem());
        }

        @AfterEach
        void endMockingFileChooserBackend() {
            if (fileChooserBackendMock != null) {
                fileChooserBackendMock.close();
            }
        }

        @Test
        void testGetItemsWithoutParameters() throws IOException {
            final var result = new PerformListItemsBuilder().build().performListItems();

            getItemsInInitialFolder()
                .forEach(item -> verifyRootItemConstruction(item, fileChooserBackendMock.constructed().get(0)));
            assertThat(result.folder().items()).hasSize(1);
            assertRootFolderPath(result.folder().path());
            assertThat(result.folder().isRootFolder()).isTrue();
            assertThat(result.errorMessage()).isEmpty();
            final var rootItem = result.folder().items().get(0);
            assertThat(rootItem.isDirectory()).isTrue();
        }

        abstract void assertRootFolderPath(String path);

        @Test
        void testListItemsRelativeToPath() throws IOException {
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            final var subDirectory = Files.createTempDirectory(directory, "aSubDirectory");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            final var folderName = directory.getFileName().toString();
            when(m_fileSystem.getPath(eq(path), eq(folderName))).thenReturn(relativizeIfNecessary(directory));

            final var result =
                new PerformListItemsBuilder().withPath(path).withFolder(folderName).build().performListItems();

            final var fileChooserBackend = fileChooserBackendMock.constructed().get(0);
            verify(fileChooserBackend).pathToObject(eq(subDirectory));
            assertThat(result.errorMessage()).isEmpty();
            assertThat(result.folder().items()).hasSize(1);
            assertThat(result.folder().isRootFolder()).isFalse();
            final var parentFolders = result.folder().parentFolders();
            assertThat(parentFolders).hasSizeGreaterThan(1);
            assertThat(parentFolders.get(parentFolders.size() - 2).name())
                .isEqualTo(m_subFolder.getFileName().toString());
            assertThat(parentFolders.get(parentFolders.size() - 1).name())
                .isEqualTo(directory.getFileName().toString());
        }

        @Test
        void testGetItemsWithoutFolder() throws IOException {
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            final var subDirectory = Files.createTempDirectory(directory, "aSubDirectory");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            when(m_fileSystem.getPath(eq(path))).thenReturn(relativizeIfNecessary(directory));
            final var result = new PerformListItemsBuilder().withPath(path).build().performListItems();
            final var fileChooserBackend = fileChooserBackendMock.constructed().get(0);
            verify(fileChooserBackend).pathToObject(eq(subDirectory));
            assertThat(result.errorMessage()).isEmpty();
            assertThat(result.folder().items()).hasSize(1);
        }

        @Test
        void testListItemsWithFilteredFileExtensions() throws IOException {

            final var fileExtensions = List.of("pdf", "png");

            when(m_fileSystem.getPathMatcher(ArgumentMatchers.any())).thenReturn(new PathMatcher() {

                @Override
                public boolean matches(final Path path) {
                    return fileExtensions.stream().anyMatch(ext -> path.toString().endsWith(ext));
                }
            });
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            Files.createTempDirectory(directory, "aSubDirectory");
            Files.createTempFile(directory, "aFile", ".pdf");
            Files.createTempFile(directory, "aFile", ".txt");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            final var folderName = directory.getFileName().toString();
            when(m_fileSystem.getPath(eq(path), eq(folderName))).thenReturn(relativizeIfNecessary(directory));

            final var result = new PerformListItemsBuilder().withPath(path).withFolder(folderName)
                .withExtensions(fileExtensions).build().performListItems();
            verify(m_fileSystem).getPathMatcher(eq("glob:**.{pdf,png}"));
            /**
             * Two items expected: One folder (which is not checked against file extensions) and the pdf file but not
             * the txt file
             */
            assertThat(result.folder().items()).hasSize(2);
            assertThat(result.folder().items()).anyMatch(item -> item.isDirectory())
                .anyMatch(item -> !item.isDirectory() && item.name().endsWith(".pdf"));
        }

        @Test
        void testListItemsWithWorkflowFilterMode() throws IOException {
            when(m_fileSystem.getPathMatcher(ArgumentMatchers.any())).thenReturn(new PathMatcher() {

                @Override
                public boolean matches(final Path path) {
                    return path.toString().endsWith(".knwf");
                }
            });
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            Files.createTempDirectory(directory, "aSubDirectory");
            Files.createTempFile(directory, "aWorkflow", ".knwf");
            Files.createTempFile(directory, "aFile", ".txt");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            final var folderName = directory.getFileName().toString();
            when(m_fileSystem.getPath(eq(path), eq(folderName))).thenReturn(relativizeIfNecessary(directory));

            final var result = new PerformListItemsBuilder().withPath(path).withFolder(folderName)
                .withIsWorkflowFilterMode().build().performListItems();
            verify(m_fileSystem).getPathMatcher(eq("glob:**.knwf"));
            /**
             * Two items expected: One folder (which is not checked against file extensions) and the workflow file but
             * not the txt file
             */
            assertThat(result.folder().items()).hasSize(2);
            assertThat(result.folder().items()).anyMatch(item -> item.isDirectory())
                .anyMatch(item -> !item.isDirectory() && item.name().endsWith(".knwf"));
        }

        @Test
        void testListItemsWithoutPath() throws IOException {
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            final var file = Files.writeString(m_subFolder.resolve("aFile"), "");
            final var pathToDir = pathToString(relativizeIfNecessary(m_subFolder));

            final var result = new PerformListItemsBuilder().withFolder(pathToDir).build().performListItems();

            final var fileChooserBackend = fileChooserBackendMock.constructed().get(0);
            verify(fileChooserBackend).pathToObject(eq(directory));
            verify(fileChooserBackend).pathToObject(eq(file));
            assertThat(result.errorMessage()).isEmpty();
            final var items = result.folder().items();
            assertThat(items).hasSize(2);
            assertThat(items.stream().map(item -> item.isDirectory()).count()).isEqualTo(2);
        }

        @Test
        void testListItemsWithMissingPath() throws IOException {
            final var deletedFolder = Files.createTempDirectory(m_subFolder, "aDirectory");
            Files.delete(deletedFolder);
            final var missingPath = pathToString(relativizeIfNecessary(deletedFolder));

            final var result = new PerformListItemsBuilder().withFolder(missingPath).build().performListItems();

            assertThat(result.errorMessage().get())
                .isEqualTo(String.format("The selected path %s does not exist", deletedFolder));
            assertThat(result.folder().path()).isEqualTo(m_subFolder.toString());
        }

        @Test
        void testListItemsWithMissingPathWriter() throws IOException {
            final var deletedFolder = Files.createTempDirectory(m_subFolder, "aDirectory");
            Files.delete(deletedFolder);
            final var missingPath = pathToString(relativizeIfNecessary(deletedFolder));

            final var result =
                new PerformListItemsBuilder().asWriter().withFolder(missingPath).build().performListItems();

            assertThat(result.errorMessage()).isEmpty();
            assertThat(result.folder().path()).isEqualTo(m_subFolder.toString());
            assertThat(result.filePathRelativeToFolder()).isEqualTo(deletedFolder.getFileName().toString());
        }

        @Test
        void testListItemsWithInvalidPath() throws IOException {
            final var invalidPath = "an invalid path";
            when(m_fileSystem.getPath(eq(invalidPath))).thenThrow(InvalidPathException.class);
            when(m_fileSystem.getPath(eq(""))).thenReturn(getDefaultDirectory());

            final var result = new PerformListItemsBuilder().withFolder(invalidPath).build().performListItems();

            assertThat(result.errorMessage().get())
                .isEqualTo(String.format("The selected path %s is not a valid path", invalidPath));
        }

        @Test
        void testListItemsParentPath() throws IOException {
            final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
            final var file = Files.writeString(m_subFolder.resolve("aFile"), "");
            final var path = pathToString(relativizeIfNecessary(directory));

            final var rootItems =
                new PerformListItemsBuilder().withFolder("..").withPath(path).build().performListItems();

            final var fileChooserBackend = fileChooserBackendMock.constructed().get(0);
            verify(fileChooserBackend).pathToObject(eq(directory));
            verify(fileChooserBackend).pathToObject(eq(file));
            final var items = rootItems.folder().items();
            assertThat(items).hasSize(2);
            assertThat(items.stream().map(item -> item.isDirectory()).count()).isEqualTo(2);
        }

        @Test
        void testGetFilePath() throws IOException {
            final var file = Files.writeString(m_subFolder.resolve("aFile"), "");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            final var fileName = file.getFileName().toString();
            when(m_fileSystem.getPath(eq(path), eq(fileName))).thenReturn(file);
            final var filePath = m_service.getFilePath("local", path, fileName, null, null);
            assertThat(filePath.errorMessage()).isNull();
            assertThat(filePath.path()).isEqualTo(file.toString());
        }

        @Test
        void testGetFilePathForInvalidFileName() throws IOException {
            final var file = Files.writeString(m_subFolder.resolve("aFile"), "");
            final var path = relativizeIfNecessary(m_subFolder).toString();
            final var fileName = file.getFileName().toString();
            when(m_fileSystem.getPath(eq(path), eq(fileName))).thenThrow(InvalidPathException.class);
            final var filePath = m_service.getFilePath("local", path, fileName, null, null);
            assertThat(filePath.path()).isNull();
            assertThat(filePath.errorMessage()).isEqualTo("aFile is not a valid file name.");
        }

        @Nested
        class GetFilePathAppendExtensionTest {

            final static String APPENDED_EXTENSION = "ext";

            @Test
            void testGetFilePathDoesNotAppendExtensionForExistingFile() throws IOException {
                final var file = Files.writeString(m_subFolder.resolve("aFile"), "");
                final var path = relativizeIfNecessary(m_subFolder).toString();
                final var fileName = file.getFileName().toString();
                when(m_fileSystem.getPath(eq(path), eq(fileName))).thenReturn(file);
                final var filePath = m_service.getFilePath("local", path, fileName, APPENDED_EXTENSION, null);
                assertThat(filePath.path()).doesNotEndWith("." + APPENDED_EXTENSION);
            }

            @Test
            void testGetFilePathDoesAppendExtensionIfADirectoryOfThatNameExists() throws IOException {
                final var directory = Files.createTempDirectory(m_subFolder, "aDirectory");
                final var path = relativizeIfNecessary(m_subFolder).toString();
                final var fileName = directory.getFileName().toString();
                when(m_fileSystem.getPath(eq(path), eq(fileName))).thenReturn(directory);
                final var filePath = m_service.getFilePath("local", path, fileName, APPENDED_EXTENSION, null);
                assertThat(filePath.path()).endsWith("." + APPENDED_EXTENSION);
            }

            @Test
            void testGetFilePathDoesAppendExtensionTheFileDoesNotYetExists() throws IOException {
                final var file = m_subFolder.resolve("aFile");
                final var path = relativizeIfNecessary(m_subFolder).toString();
                final var fileName = file.getFileName().toString();
                when(m_fileSystem.getPath(eq(path), eq(fileName))).thenReturn(file);
                final var filePath = m_service.getFilePath("local", path, fileName, APPENDED_EXTENSION, null);
                assertThat(filePath.path()).endsWith("." + APPENDED_EXTENSION);
            }

            @Test
            void testGetFilePathDoesNotAppendExtensionIfExtensionAlreadyPresent() throws IOException {
                final var file = m_subFolder.resolve("aFile." + APPENDED_EXTENSION);
                final var path = relativizeIfNecessary(m_subFolder).toString();
                final var fileName = file.getFileName().toString();
                when(m_fileSystem.getPath(eq(path), eq(fileName))).thenReturn(file);
                final var filePath = m_service.getFilePath("local", path, fileName, APPENDED_EXTENSION, null);
                assertThat(filePath.path()).doesNotEndWith(APPENDED_EXTENSION + "." + APPENDED_EXTENSION);
            }
        }

        @Test
        void testGetFilePathWithRelativeToReturnsRelativePath() throws IOException {
            // Given: workflows/data-analysis/config.json
            final var dataAnalysisFolder = Files.createTempDirectory(m_subFolder, "data-analysis");
            final var preprocessingFolder = Files.createTempDirectory(dataAnalysisFolder, "preprocessing");
            final var file = Files.writeString(dataAnalysisFolder.resolve("config.json"), "");

            final var dataAnalysisPath = relativizeIfNecessary(dataAnalysisFolder).toString();
            final var fileName = file.getFileName().toString();
            final var relativeToPath = relativizeIfNecessary(preprocessingFolder).toString();

            when(m_fileSystem.getPath(eq(dataAnalysisPath), eq(fileName))).thenReturn(relativizeIfNecessary(file));
            when(m_fileSystem.getPath(eq(relativeToPath))).thenReturn(relativizeIfNecessary(preprocessingFolder));

            // When: getFilePath is called with relativeTo parameter
            final var filePath = m_service.getFilePath("local", dataAnalysisPath, fileName, null, relativeToPath);

            // Then: the returned path is "../config.json"
            assertThat(filePath.errorMessage()).isNull();
            final var separator = m_subFolder.getFileSystem().getSeparator();
            assertThat(filePath.path()).isEqualTo(".." + separator + "config.json");
        }

        @Test
        void testGetFilePathWithoutRelativeToReturnsAbsolutePath() throws IOException {
            // Given: workflows/data-analysis/output/results.csv
            final var dataAnalysisFolder = Files.createTempDirectory(m_subFolder, "data-analysis");
            final var outputFolder = Files.createTempDirectory(dataAnalysisFolder, "output");
            final var file = Files.writeString(outputFolder.resolve("results.csv"), "");

            final var outputPath = relativizeIfNecessary(outputFolder).toString();
            final var fileName = file.getFileName().toString();

            when(m_fileSystem.getPath(eq(outputPath), eq(fileName))).thenReturn(relativizeIfNecessary(file));

            // When: getFilePath is called without relativeTo parameter
            final var filePath = m_service.getFilePath("local", outputPath, fileName, null, null);

            // Then: the returned path is the full path
            assertThat(filePath.errorMessage()).isNull();
            assertThat(filePath.path()).isEqualTo(relativizeIfNecessary(file).toString());
        }

        @Test
        void testResolveRelativePathWithUpwardNavigation() throws IOException {
            // Given: relativeTo path is "workflows/data-analysis/preprocessing"
            // And: relative path is "../../results/output.csv"
            final var workflowsFolder = Files.createTempDirectory(m_subFolder, "workflows");
            final var dataAnalysisFolder = Files.createTempDirectory(workflowsFolder, "data-analysis");
            final var preprocessingFolder = Files.createTempDirectory(dataAnalysisFolder, "preprocessing");
            final var resultsFolder = Files.createTempDirectory(workflowsFolder, "results");

            final var relativeToPath = relativizeIfNecessary(preprocessingFolder);
            final var expectedResultPath = relativizeIfNecessary(resultsFolder.resolve("output.csv"));

            final var relativePath = relativeToPath.relativize(
                relativeToPath.resolve("..").resolve("..").resolve(resultsFolder.getFileName()).resolve("output.csv"));

            when(m_fileSystem.getPath(eq(relativeToPath.toString()))).thenReturn(relativeToPath);
            when(m_fileSystem.getPath(eq(relativePath.toString()))).thenReturn(relativePath);

            // When: resolveRelativePath is called
            final var resolved =
                m_service.resolveRelativePath("local", relativePath.toString(), relativeToPath.toString());

            // Then: the returned path is "workflows/results/output.csv"
            assertThat(resolved).isEqualTo(expectedResultPath.toString());
        }

        @Test
        void testListItemsGeneratesRelativeParentFolders() throws IOException {
            // Given: relativeTo path is "workflows/data-analysis/preprocessing"
            // And: current path is "workflows/data-analysis/results/output/final"
            final var workflowsFolder = Files.createTempDirectory(m_subFolder, "workflows");
            final var dataAnalysisFolder = Files.createTempDirectory(workflowsFolder, "data-analysis");
            final var preprocessingFolder = Files.createTempDirectory(dataAnalysisFolder, "preprocessing");
            final var resultsFolder = Files.createTempDirectory(dataAnalysisFolder, "results");
            final var outputFolder = Files.createTempDirectory(resultsFolder, "output");
            final var finalFolder = Files.createTempDirectory(outputFolder, "final");

            final var relativeToPath = preprocessingFolder;
            final var currentPath = relativizeIfNecessary(finalFolder);

            when(m_fileSystem.getPath(eq(relativeToPath.toString()))).thenReturn(relativeToPath);
            when(m_fileSystem.getPath(eq(currentPath.toString()))).thenReturn(currentPath);

            // When: listItems is called with relativeTo parameter
            final var result = new PerformListItemsBuilder().withFolder(currentPath.toString()).build()
                .performListItems(relativeToPath.toString());

            // Then: parent folders are relative to the relativeTo path
            final var parentFolders = result.folder().parentFolders();
            assertThat(parentFolders).isNotEmpty();
            assertThat(parentFolders.get(0).path()).isEqualTo(relativeToPath.toString());
            assertThat(parentFolders.get(0).name()).isNull();
            assertThat(parentFolders.get(1).name()).isEqualTo("..");
            assertThat(parentFolders.get(1).path()).isEqualTo(dataAnalysisFolder.toString());
            assertThat(parentFolders.get(2).name()).isEqualTo(resultsFolder.getFileName().toString());
            assertThat(parentFolders.get(2).path()).isEqualTo(resultsFolder.toString());
            assertThat(parentFolders.get(3).name()).isEqualTo(outputFolder.getFileName().toString());
            assertThat(parentFolders.get(3).path()).isEqualTo(outputFolder.toString());
            assertThat(parentFolders.get(4).name()).isEqualTo(finalFolder.getFileName().toString());
            assertThat(parentFolders.get(4).path()).isEqualTo(finalFolder.toString());
        }

        private Path relativizeIfNecessary(final Path absolutePath) {
            if (useAbsoluteFileSystem()) {
                return absolutePath;
            }
            return getDefaultDirectory().relativize(absolutePath);
        }

        private String pathToString(final Path path) {
            final var pathString = path.toString();
            when(m_fileSystem.getPath(eq(pathString))).thenReturn(path);
            return pathString;
        }

        interface PerformListItems {
            FolderAndError performListItems() throws IOException;

            FolderAndError performListItems(String relativeTo) throws IOException;
        }

        static final class PerformListItemsBuilder {

            private String m_folder;

            private String m_path;

            private boolean m_isWriter;

            private List<String> m_extensions;

            private boolean m_isWorkflowFilterMode;

            private FileChooserDataService m_dataService;

            private String m_fileSystemId;

            PerformListItems build() {
                if (m_dataService == null) {
                    m_dataService = new FileChooserDataService(new FileSystemConnector());
                }
                final var config = new ListItemsConfig(m_isWriter, m_extensions, m_isWorkflowFilterMode);
                return new PerformListItems() {

                    @Override
                    public FolderAndError performListItems() throws IOException {
                        return m_dataService.listItems(m_fileSystemId, m_path, m_folder, config, null);
                    }

                    @Override
                    public FolderAndError performListItems(final String relativeTo) throws IOException {
                        return m_dataService.listItems(m_fileSystemId, m_path, m_folder, config, relativeTo);
                    }

                };

            }

            PerformListItemsBuilder withIsWorkflowFilterMode() {
                m_isWorkflowFilterMode = true;
                return this;
            }

            PerformListItemsBuilder() {
                m_fileSystemId = "local";
            }

            PerformListItemsBuilder withPath(final String path) {
                m_path = path;
                return this;
            }

            PerformListItemsBuilder withFileSystemId(final String fileSystemId) {
                m_fileSystemId = fileSystemId;
                return this;
            }

            PerformListItemsBuilder withFolder(final String folder) {
                m_folder = folder;
                return this;
            }

            PerformListItemsBuilder asWriter() {
                m_isWriter = true;
                return this;
            }

            PerformListItemsBuilder withExtensions(final List<String> extensions) {
                m_extensions = extensions;
                return this;
            }

            PerformListItemsBuilder fromDataService(final FileChooserDataService dataService) {
                m_dataService = dataService;
                return this;
            }

        }
    }

    @Nested
    class CustomConnectionTest {

        private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

        @Test
        void testRegisterCustomFileSystemGeneratesUniqueUUIDs() {
            final var provider1 = mockConnectionProvider();
            final var provider2 = mockConnectionProvider();

            final var id1 = m_fsConnector.registerCustomFileSystem(provider1, "#/properties/field1", List.of());
            final var id2 = m_fsConnector.registerCustomFileSystem(provider2, "#/properties/field2", List.of());

            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1).matches(UUID_PATTERN);
            assertThat(id2).matches(UUID_PATTERN);
        }

        @Test
        void testLazyConnectionCreation() throws IOException, InvalidSettingsException {
            final var mockProvider = mockConnectionProvider();
            final var fileSystemId =
                m_fsConnector.registerCustomFileSystem(mockProvider, "#/properties/field", List.of());

            // Provider should not be called yet
            verify(mockProvider, Mockito.never()).getFileSystemConnection();

            // Now trigger backend creation
            m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();

            // Provider should have been called exactly once
            verify(mockProvider, org.mockito.Mockito.times(1)).getFileSystemConnection();
        }

        @Test
        void testConnectionReuse() throws IOException, InvalidSettingsException {
            final var mockProvider = mockConnectionProvider();
            final var fileSystemId =
                m_fsConnector.registerCustomFileSystem(mockProvider, "#/properties/field", List.of());

            // Get backend twice
            m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();
            m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem();

            // Provider should have been called only once (connection reused)
            verify(mockProvider, Mockito.times(1)).getFileSystemConnection();
        }

        @Test
        void testOldConnectionClosedOnReplacement() throws IOException, InvalidSettingsException {
            final var provider1 = mockConnectionProvider();
            final var mockConnection1 = mock(FSConnection.class);
            when(provider1.getFileSystemConnection()).thenReturn(mockConnection1);

            final var scope = "#/properties/field";
            final var indices = List.of();

            // Register and create first connection
            final var id1 = m_fsConnector.registerCustomFileSystem(provider1, scope, indices);
            m_fsConnector.getFileChooserBackend(id1).getFileSystem();

            // Register new connection with same scope/indices
            final var provider2 = mockConnectionProvider();
            final var id2 = m_fsConnector.registerCustomFileSystem(provider2, scope, indices);

            // Different IDs
            assertThat(id1).isNotEqualTo(id2);

            // Now trigger creation of second backend - this should close the first connection
            m_fsConnector.getFileChooserBackend(id2).getFileSystem();

            // First connection should have been closed
            verify(mockConnection1).close();
        }

        @Test
        void testThrowsUncheckedExceptionOnInvalidSettingsException() {
            String errorMessage = "Test invalid settings";
            final FSConnectionProvider provider = () -> {
                throw new InvalidSettingsException(errorMessage);
            };
            final var fileSystemId = m_fsConnector.registerCustomFileSystem(provider, "#/properties/field", List.of());
            final var fileChooserBackend = m_fsConnector.getFileChooserBackend(fileSystemId);
            final var exception = assertThrows(RuntimeException.class, () -> fileChooserBackend.getFileSystem());
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
        }

        @Test
        void testClearClosesCustomConnections() throws IOException, InvalidSettingsException {
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
    }

}
