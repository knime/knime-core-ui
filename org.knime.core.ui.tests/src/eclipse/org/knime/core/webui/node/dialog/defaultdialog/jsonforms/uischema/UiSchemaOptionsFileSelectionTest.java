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
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtilTest.buildTestUiSchema;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
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
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests for uischema options of the new file selection API annotations.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class UiSchemaOptionsFileSelectionTest {

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
        boolean m_someVeryImportantField;

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

    static NodeParametersInput setUpEmptyNodeParametersInput() {
        final var context = Mockito.mock(NodeParametersInput.class);
        Mockito.when(context.getInPortSpec(ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        Mockito.when(context.getInPortTypes()).thenReturn(new PortType[0]);
        return context;
    }

    @SuppressWarnings("resource")
    static NodeParametersInput setupFileSystemMocks(final String fileSystemType, final String fileSystemSpecifier) {
        final var context = Mockito.mock(NodeParametersInput.class);
        final var spec = Mockito.mock(FileSystemPortObjectSpec.class);
        final var location = Mockito.mock(FSLocation.class);
        Mockito.when(location.getFileSystemSpecifier()).thenReturn(Optional.of(fileSystemSpecifier));
        Mockito.when(spec.getFileSystemType()).thenReturn(fileSystemType);
        Mockito.when(spec.getFSLocationSpec()).thenReturn(location);
        Mockito.when(spec.getFileSystemConnection()).thenReturn(Optional.of(Mockito.mock(FSConnection.class)));
        Mockito.when(context.getInPortSpec(0)).thenReturn(Optional.of(spec));
        Mockito.when(context.getInPortTypes()).thenReturn(new PortType[]{FileSystemPortObject.TYPE});

        return context;
    }

    record SpaceContextSetup(NodeParametersInput nodeParametersInput, Runnable closeStaticMocks) {
    }

    @SuppressWarnings("resource")
    static SpaceContextSetup setupSpaceContext(final String mountId, final String spacePath) {
        final var workflowContextMock = Mockito.mockStatic(WorkflowContextUtil.class);
        final var contextV2 = Mockito.mock(WorkflowContextV2.class);
        final var hubSpaceLocationInfo = Mockito.mock(HubSpaceLocationInfo.class);

        Mockito.when(hubSpaceLocationInfo.getDefaultMountId()).thenReturn(mountId);
        Mockito.when(hubSpaceLocationInfo.getSpacePath()).thenReturn(spacePath);
        Mockito.when(contextV2.getLocationInfo()).thenReturn(hubSpaceLocationInfo);
        workflowContextMock.when(WorkflowContextUtil::getWorkflowContextV2Optional).thenReturn(Optional.of(contextV2));

        final var nodeParametersInput = setUpEmptyNodeParametersInput();

        return new SpaceContextSetup(nodeParametersInput, workflowContextMock::close);
    }

    record ExecutorContextSetup(NodeParametersInput nodeParametersInput, Runnable closeStaticMocks) {
    }

    @SuppressWarnings("resource")
    static ExecutorContextSetup setupExecutorContext(final ExecutorType executorType) {
        final var workflowContextMock = Mockito.mockStatic(WorkflowContextUtil.class);
        final var contextV2 = Mockito.mock(WorkflowContextV2.class);

        Mockito.when(contextV2.getExecutorType()).thenReturn(executorType);
        workflowContextMock.when(WorkflowContextUtil::getWorkflowContextV2Optional).thenReturn(Optional.of(contextV2));

        final var nodeParametersInput = setUpEmptyNodeParametersInput();

        return new ExecutorContextSetup(nodeParametersInput, workflowContextMock::close);
    }

    static ObjectNode buildUiSchema(final Class<? extends NodeParameters> settingsClass) {
        return buildTestUiSchema(settingsClass, setUpEmptyNodeParametersInput());
    }

    @MethodSource("getTestSettingsClasses")
    @ParameterizedTest
    void testConnectedOptions(final Class<? extends NodeParameters> settingsClass) {
        final var fileSystemType = "Connected File System";
        final var fileSystemSpecifier = "someSpecifier";
        final var context = setupFileSystemMocks(fileSystemType, fileSystemSpecifier);
        final var uiSchemaWithConnection = buildTestUiSchema(settingsClass, context);
        assertThatJson(uiSchemaWithConnection).inPath("$.elements[0].options").isObject()
            .doesNotContainKey("connectedFSOptions");
        assertThatJson(uiSchemaWithConnection).inPath("$.elements[1].options.connectedFSOptions").isObject()
            .containsEntry("fileSystemType", fileSystemType)//
            .containsEntry("fileSystemSpecifier", fileSystemSpecifier) //
            .containsEntry("fileSystemConnectionMissing", false)//
            .containsEntry("portIndex", 0);

        final var uiSchemaWithoutConnection = buildUiSchema(settingsClass);

        assertThatJson(uiSchemaWithoutConnection).inPath("$.elements[0].options").isObject()
            .doesNotContainKey("connectedFSOptions");
    }

    @MethodSource("getTestSettingsClasses")
    @ParameterizedTest
    void testSpaceOptions(final Class<? extends NodeParameters> settingsClass) {
        final var mountId = " my mount id";
        final var spacePath = "my space path";
        final var contextSetup = setupSpaceContext(mountId, spacePath);
        final var context = contextSetup.nodeParametersInput();
        try {
            var uiSchema = buildTestUiSchema(settingsClass, context);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject().doesNotContainKey("spaceFSOptions");
            assertThatJson(uiSchema).inPath("$.elements[2].options.spaceFSOptions").isObject()
                .containsEntry("mountId", mountId)//
                .containsEntry("spacePath", spacePath);
        } finally {
            contextSetup.closeStaticMocks().run();

        }
    }

    @MethodSource("getTestSettingsClasses")
    @ParameterizedTest
    void testIsLocalOptionAnalyticsPlatform(final Class<? extends NodeParameters> settingsClass) {
        final var contextSetup = setupExecutorContext(ExecutorType.ANALYTICS_PLATFORM);
        final var context = contextSetup.nodeParametersInput();
        try {
            var uiSchema = buildTestUiSchema(settingsClass, context);

            assertThatJson(uiSchema).inPath("$.elements[1].options").isObject().doesNotContainKey("isLocal");
            assertThatJson(uiSchema).inPath("$.elements[0].options.isLocal").isBoolean().isEqualTo(true);
        } finally {
            contextSetup.closeStaticMocks().run();
        }
    }

    @MethodSource("getTestSettingsClasses")
    @ParameterizedTest
    void testIsLocalOptionHubExecutor(final Class<? extends NodeParameters> settingsClass) {
        final var contextSetup = setupExecutorContext(ExecutorType.HUB_EXECUTOR);
        final var context = contextSetup.nodeParametersInput();
        try {
            var uiSchema = buildTestUiSchema(settingsClass, context);

            assertThatJson(uiSchema).inPath("$.elements[0].options.isLocal").isBoolean().isEqualTo(false);
        } finally {
            contextSetup.closeStaticMocks().run();
        }
    }

    static Stream<Arguments> getTestSettingsClasses() {
        return Stream.of( //
            Arguments.of(FileChooserSettings.class), //
            Arguments.of(MultiFileChooserSettings.class), //
            Arguments.of(StringFileChooserSettings.class)//
        );
    }

    static final class FileChooserSettings implements NodeParameters {
        @Widget(title = "", description = "")
        @WithFileSystem(FileSystemOption.LOCAL)
        FileSelection m_fileSelectionLocal = new FileSelection();

        @Widget(title = "", description = "")
        @WithFileSystem(FileSystemOption.CONNECTED)
        FileSelection m_fileSelectionConnected = new FileSelection();

        @Widget(title = "", description = "")
        @WithFileSystem(FileSystemOption.SPACE)
        FileSelection m_fileSelectionSpace = new FileSelection();

    }

    static final class MultiFileChooserSettings implements NodeParameters {
        @Widget(title = "", description = "")
        @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
        @WithFileSystem(FileSystemOption.LOCAL)
        MultiFileSelection<DummyFileChooserFilters> m_multiFileSelectionLocal =
            new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());

        @Widget(title = "", description = "")
        @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
        @WithFileSystem(FileSystemOption.CONNECTED)
        MultiFileSelection<DummyFileChooserFilters> m_multiFileSelectionConnected =
            new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());

        @Widget(title = "", description = "")
        @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
        @WithFileSystem(FileSystemOption.SPACE)
        MultiFileSelection<DummyFileChooserFilters> m_multiFileSelectionSpace =
            new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());
    }

    static final class StringFileChooserSettings implements NodeParameters {
        @Widget(title = "", description = "")
        @FileSelectionWidget(SingleFileSelectionMode.FILE)
        @WithFileSystem(FileSystemOption.LOCAL)
        String m_stringFileSelectionLocal = "";

        @Widget(title = "", description = "")
        @FileSelectionWidget(SingleFileSelectionMode.FILE)
        @WithFileSystem(FileSystemOption.CONNECTED)
        String m_stringFileSelectionConnected = "";

        @Widget(title = "", description = "")
        @FileSelectionWidget(SingleFileSelectionMode.FILE)
        @WithFileSystem(FileSystemOption.SPACE)
        String m_stringFileSelectionSpace = "";
    }

    @Nested
    class SingleFileSelectionTests {

        @Test
        void testSingleFileMode() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                FileSelection m_singleFile = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("format", "fileChooser")//
                .doesNotContainKey("selectionMode");
        }

        @Test
        void testSingleFolderMode() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
                FileSelection m_singleFolder = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("selectionMode", "FOLDER");
        }

        @Test
        void testSingleFileWithExtension() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileReaderWidget(fileExtensions = "csv")
                FileSelection m_singleFile = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options.fileExtensions").isArray()//
                .containsExactly("csv");
        }

    }

    @Nested
    class MultiFileSelectionTests {

        @Test
        void testMultiFileTwoModes() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
                MultiFileSelection<DummyFileChooserFilters> m_multiFilePlusFolders =
                    new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("format", "multiFileChooser");
            assertThatJson(uiSchema).inPath("$.elements[0].options.possibleFilterModes").isArray()//
                .containsExactly("FILE", "FILES_IN_FOLDERS");

            assertThatJson(uiSchema).inPath("$.elements[0].options.filters.uiSchema.elements[0].scope") //
                .isString() //
                .contains("someVeryImportantField"); // name of the field inside the filter class we're using for this tes
            assertThatJson(uiSchema).inPath("$.elements[0].options.filters.classId") //
                .isString() //
                .isEqualTo(DummyFileChooserFilters.class.getName());
        }

        @Test
        void testMultiFileAllFolderModes() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @MultiFileSelectionWidget({MultiFileSelectionMode.FILES_IN_FOLDERS, MultiFileSelectionMode.FOLDERS,
                    MultiFileSelectionMode.FILES_AND_FOLDERS})
                MultiFileSelection<DummyFileChooserFilters> m_multiAllFolderModes =
                    new MultiFileSelection<>(MultiFileSelectionMode.FILES_IN_FOLDERS, new DummyFileChooserFilters());
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options.possibleFilterModes").isArray()//
                .containsExactly("FILES_IN_FOLDERS", "FOLDERS", "FILES_AND_FOLDERS");
        }

        @Test
        void testMultiFileWithSeparateFileReaderWidget() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
                @FileReaderWidget(fileExtensions = {"csv", "parquet"})
                MultiFileSelection<DummyFileChooserFilters> m_multiFilePlusFolders =
                    new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options.fileExtensions").isArray()//
                .containsExactly("csv", "parquet");
        }

        @Test
        void testThrowsMultiFileWithoutMultiFileOption() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FOLDER})
                MultiFileSelection<DummyFileChooserFilters> m_multiFileOnly =
                    new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(Settings.class));
        }

    }

    @Nested
    class FileSystemOptionsTests {

        @Test
        void testConnectedFileSystemOnly() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                @WithFileSystem(FileSystemOption.CONNECTED)
                FileSelection m_localOnly = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options.fileSystems").isArray()//
                .containsExactly("CONNECTED");
        }

        @Test
        void testLocalAndSpaceFileSystems() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @WithFileSystem({FileSystemOption.LOCAL, FileSystemOption.SPACE})
                FileSelection m_localAndSpace = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options.fileSystems").isArray()//
                .containsExactly("LOCAL", "SPACE");
        }

        /**
         * Default (all file systems) is inferred by absence of the fileSystems option
         */
        @Test
        void testAllFileSystemsDefault() {
            class SettingsSingleFileSelection implements NodeParameters {
                @Widget(title = "", description = "")
                FileSelection m_allFileSystems = new FileSelection();
            }
            var uiSchema = buildUiSchema(SettingsSingleFileSelection.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .doesNotContainKey("fileSystems");

            class SettingsMultiFileSelection implements NodeParameters {
                @Widget(title = "", description = "")
                @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
                MultiFileSelection<DummyFileChooserFilters> m_allFileSystemsMulti =
                    new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DummyFileChooserFilters());
            }
            var uiSchemaMulti = buildUiSchema(SettingsMultiFileSelection.class);
            assertThatJson(uiSchemaMulti).inPath("$.elements[0].options").isObject()//
                .doesNotContainKey("fileSystems");
        }
    }

    @Nested
    class WriterWidgetsTests {

        @Test
        void testFileWriterWithFileSelection() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileWriterWidget(fileExtension = "csv")
                FileSelection m_fileWriter = new FileSelection();
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("fileExtension", "csv") //
                .containsEntry("isWriter", true);
        }

        @Test
        void testStringFieldWithFileWriter() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
                @FileWriterWidget(fileExtension = "csv")
                @WithFileSystem(FileSystemOption.LOCAL)
                String m_stringWriter;
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("fileSystem", "LOCAL") //
                .containsEntry("selectionMode", "FILE") //
                .containsEntry("fileExtension", "csv") //
                .containsEntry("isWriter", true);
        }
    }

    @Nested
    class StringFieldsTests {

        @Test
        void testStringWithFileSelectionLocalFS() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                @WithFileSystem(FileSystemOption.LOCAL)
                String m_stringWithFileSelection = "";
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject() //
                .containsEntry("format", "stringFileChooser")//
                .containsEntry("fileSystem", "LOCAL") //
                .containsEntry("selectionMode", "FILE");
        }

        @Test
        void testStringWithFolderSelectionConnectedFS() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(value = SingleFileSelectionMode.FOLDER)
                @WithFileSystem(FileSystemOption.CONNECTED)
                String m_stringWithFolderSelection = "";
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()//
                .containsEntry("fileSystem", "CONNECTED") //
                .containsEntry("selectionMode", "FOLDER");
        }

        @Test
        void testWorksAlsoWithOnlyFileWriterWidgetAnnotation() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileWriterWidget(fileExtension = "csv")
                @WithFileSystem(FileSystemOption.LOCAL)
                String m_stringWriter = "";
            }
            final var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()
                .containsEntry("format", "stringFileChooser")//
                .containsEntry("fileSystem", "LOCAL") //
                .containsEntry("fileExtension", "csv")//
                .containsEntry("isWriter", true);
        }

        @Test
        void testWorksAlsoWithOnlyFileReaderWidgetAnnotation() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileReaderWidget(fileExtensions = "csv")
                @WithFileSystem(FileSystemOption.LOCAL)
                String m_stringReader = "";
            }
            final var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject()
                .containsEntry("format", "stringFileChooser")//
                .containsEntry("fileSystem", "LOCAL") //
                .doesNotContainKey("isWriter");
            assertThatJson(uiSchema).inPath("$.elements[0].options.fileExtensions").isArray()//
                .containsExactly("csv");
        }

        @Test
        void testThrowsIfMultipleFileSystems() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                @WithFileSystem({FileSystemOption.LOCAL, FileSystemOption.CONNECTED})
                String m_stringMultipleFileSystems = "";
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(Settings.class));
        }

        @Test
        void testThrowsIfReaderAndWriter() {
            class Settings implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                @WithFileSystem(FileSystemOption.LOCAL)
                @FileReaderWidget(fileExtensions = "csv")
                @FileWriterWidget(fileExtension = "csv")
                String m_stringReaderAndWriter = "";
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(Settings.class));
        }

        @Test
        void testThrowsIfNoFileSystemAnnotation() {
            class SettingsUsingFileSelectionWidget implements NodeParameters {
                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                String m_stringNoFileSystem = "";
            }
            assertThrows(UiSchemaGenerationException.class,
                () -> buildUiSchema(SettingsUsingFileSelectionWidget.class));

            class SettingsUsingFileWriterWidget implements NodeParameters {
                @Widget(title = "", description = "")
                @FileWriterWidget(fileExtension = "csv")
                String m_stringNoFileSystem = "";
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(SettingsUsingFileWriterWidget.class));

            class SettingsUsingFileReaderWidget implements NodeParameters {
                @Widget(title = "", description = "")
                @FileReaderWidget(fileExtensions = "csv")
                String m_stringNoFileSystem = "";
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(SettingsUsingFileReaderWidget.class));

        }

    }

    @Nested
    class CustomFileSystemTests {

        @Test
        void testCustomFileSystemWithString() {
            class Settings implements NodeParameters {

                @Widget(title = "", description = "")
                @FileSelectionWidget(value = SingleFileSelectionMode.FILE)
                @WithCustomFileSystem(connectionProvider = TestFileSystemConnectionProvider.class)
                String m_customFileSystemString = "";

                static final class TestFileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        throw new UnsupportedOperationException("Not used in this test");
                    }

                    @Override
                    public FSConnectionProvider computeState(final NodeParametersInput parametersInput)
                        throws StateComputationFailureException {
                        throw new UnsupportedOperationException("Not used in this test");
                    }
                }
            }
            var uiSchema = buildUiSchema(Settings.class);
            assertThatJson(uiSchema).inPath("$.elements[0].options").isObject().containsEntry("selectionMode", "FILE");
            assertThatJson(uiSchema).inPath("$.elements[0].providedOptions").isArray().contains("fileSystemId");
        }

        @Test
        void testThrowsWithCustomAndNonCustomFileSystemAnnotation() {
            class Settings implements NodeParameters {

                @Widget(title = "", description = "")
                @FileSelectionWidget(SingleFileSelectionMode.FILE)
                @WithCustomFileSystem(connectionProvider = TestFileSystemConnectionProvider.class)
                @WithFileSystem(FileSystemOption.LOCAL)
                String m_invalidCustomAndNonCustom = "";

                static final class TestFileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

                    @Override
                    public void init(final StateProviderInitializer initializer) {
                        throw new UnsupportedOperationException("Not used in this test");
                    }

                    @Override
                    public FSConnectionProvider computeState(final NodeParametersInput parametersInput)
                        throws StateComputationFailureException {
                        throw new UnsupportedOperationException("Not used in this test");
                    }
                }
            }
            assertThrows(UiSchemaGenerationException.class, () -> buildUiSchema(Settings.class));

        }
    }

}
