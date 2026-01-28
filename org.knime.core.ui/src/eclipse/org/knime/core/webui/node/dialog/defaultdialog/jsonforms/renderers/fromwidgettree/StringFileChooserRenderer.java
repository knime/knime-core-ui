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
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.ConnectedFSOptionsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.UpdateResultsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.StringFileChooserRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopSingleFileSelectionModeProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Renderer for String-based file selection using the new file selection API.
 *
 * @author Paul Bärnreuther
 */
final class StringFileChooserRenderer extends WidgetTreeControlRendererSpec implements StringFileChooserRendererSpec {

    private final Optional<FileSelectionWidget> m_fileSelectionAnnotation;

    private final Optional<FileWriterWidget> m_fileWriterAnnotation;

    private final Optional<WithFileSystem> m_withFileSystemAnnotation;

    private final Optional<WithCustomFileSystem> m_customFileSystemAnnotation;

    private final WorkflowContextInfoProvider m_contextInfoProvider;

    private Optional<FileReaderWidget> m_fileReaderAnnotation;

    StringFileChooserRenderer(final TreeNode<WidgetGroup> node, final NodeParametersInput nodeParametersInput) {
        super(node);
        m_fileSelectionAnnotation = node.getAnnotation(FileSelectionWidget.class);
        m_fileReaderAnnotation = node.getAnnotation(FileReaderWidget.class);
        m_fileWriterAnnotation = node.getAnnotation(FileWriterWidget.class);
        m_withFileSystemAnnotation = node.getAnnotation(WithFileSystem.class);
        m_customFileSystemAnnotation = node.getAnnotation(WithCustomFileSystem.class);
        m_contextInfoProvider = new WorkflowContextInfoProvider( //
            nodeParametersInput, //
            m_withFileSystemAnnotation.map(WithFileSystem::value).orElse(new FileSystemOption[0]), //
            m_withFileSystemAnnotation.map(WithFileSystem::connectionProvider).orElse(null) //
        );

        validateAnnotations();
    }

    private void validateAnnotations() {
        if (m_fileReaderAnnotation.isPresent() && m_fileWriterAnnotation.isPresent()) {
            throw new UiSchemaGenerationException("String fields with file selection cannot have both "
                + "@FileReaderWidget and @FileWriterWidget annotations");
        }
        // String fields must have exactly one file system
        if (m_withFileSystemAnnotation.isEmpty() && m_customFileSystemAnnotation.isEmpty()) {
            throw new UiSchemaGenerationException("String fields with file selection must have either "
                + "@WithFileSystem or @WithCustomFileSystem annotation");
        }

        if (m_withFileSystemAnnotation.isPresent() && m_customFileSystemAnnotation.isPresent()) {
            throw new UiSchemaGenerationException(
                "Cannot use both @WithFileSystem and @WithCustomFileSystem on the same field");
        }

        // If using @WithFileSystem, ensure exactly one file system is specified
        if (m_withFileSystemAnnotation.isPresent()) {
            final var fileSystems = m_withFileSystemAnnotation.get().value();
            if (fileSystems.length != 1) {
                throw new UiSchemaGenerationException(
                    "String fields with file selection must have exactly one file system in @WithFileSystem");
            }
        }

    }

    @Override
    public Optional<FileChooserRendererOptions.StringFileChooserOptions> getOptions() {
        return Optional.of(new FileChooserRendererOptions.StringFileChooserOptions() {

            @Override
            public FileSystemOption getFileSystem() {
                if (m_customFileSystemAnnotation.isPresent()) {
                    return null; // Custom file system handled by state provider
                }
                final var selectionOptions = m_withFileSystemAnnotation.get().value();
                return selectionOptions[0];
            }

            @Override
            public Optional<SingleFileSelectionMode> getSelectionMode() {
                return m_fileSelectionAnnotation.map(FileSelectionWidget::value);
            }

            @Override
            public Optional<String> getFileExtension() {
                return m_fileWriterAnnotation.map(FileWriterWidget::fileExtension).filter(s -> !s.isEmpty());
            }

            @Override
            public Optional<String[]> getFileExtensions() {
                return m_fileReaderAnnotation.map(FileReaderWidget::fileExtensions).filter(arr -> arr.length > 0);
            }

            @Override
            public Optional<Boolean> getIsWriter() {
                return m_fileWriterAnnotation.map(w -> true);
            }

            @Override
            public Optional<Boolean> getIsLocal() {
                return m_contextInfoProvider.getIsLocal();
            }

            @Override
            public Optional<FileChooserRendererOptions.ConnectedFSOptions> getConnectedFSOptions() {
                return m_contextInfoProvider.getConnectedFSOptions();
            }

            @Override
            public Optional<FileChooserRendererOptions.SpaceFSOptions> getSpaceFSOptions() {
                return m_contextInfoProvider.getSpaceFSOptions();
            }

            @Override
            public Optional<String> getPlaceholder() {
                return m_fileSelectionAnnotation.map(FileSelectionWidget::placeholder)
                    .filter(Predicate.not(String::isEmpty));
            }

        });
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        Map<String, Class<? extends StateProvider>> stateProviders = new HashMap<>();
        m_customFileSystemAnnotation
            .ifPresent(ann -> stateProviders.put(UpdateResultsUtil.FILE_SYSTEM_ID, ann.connectionProvider()));
        m_withFileSystemAnnotation.map(WithFileSystem::connectionProvider)
            .filter(Predicate.not(ConnectedFSOptionsProvider.class::equals))
            .ifPresent(provider -> stateProviders.put(UiSchema.TAG_CONNECTED_FS_OPTIONS, provider));
        m_fileWriterAnnotation.map(FileWriterWidget::fileExtensionProvider)
            .filter(Predicate.not(NoopStringProvider.class::equals))
            .ifPresent(fileExtensionProvider -> stateProviders.put(UiSchema.TAG_FILE_EXTENSION, fileExtensionProvider));
        m_fileSelectionAnnotation.map(FileSelectionWidget::selectionModeProvider)
            .filter(Predicate.not(NoopSingleFileSelectionModeProvider.class::equals))
            .ifPresent(singleFileSelectionModeProvider -> stateProviders.put(UiSchema.TAG_SELECTION_MODE,
                singleFileSelectionModeProvider));
        return stateProviders;

    }

}
