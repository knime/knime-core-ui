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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.ConnectedFSOptionsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.MultiFileChooserRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.options.FileChooserRendererOptions;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.MultiFileSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Renderer for MultiFileSelection-based file selection using the new file selection API.
 *
 * @author Paul Bärnreuther
 */
final class MultiFileChooserRenderer extends WidgetTreeControlRendererSpec implements MultiFileChooserRendererSpec {

    private final MultiFileSelectionWidget m_multiFileSelectionAnnotation;

    private final Optional<FileReaderWidget> m_fileReaderAnnotation;

    private final Optional<WithFileSystem> m_withFileSystemAnnotation;

    private final WorkflowContextInfoProvider m_contextInfoProvider;

    private final NodeParametersInput m_nodeParametersInput;

    private Class<? extends FileChooserFilters> m_filtersClass;

    MultiFileChooserRenderer(final TreeNode<WidgetGroup> node, final NodeParametersInput nodeParametersInput) {
        super(node);
        m_multiFileSelectionAnnotation =
            node.getAnnotation(MultiFileSelectionWidget.class).orElseThrow(IllegalStateException::new);
        m_fileReaderAnnotation = node.getAnnotation(FileReaderWidget.class);
        m_withFileSystemAnnotation = node.getAnnotation(WithFileSystem.class);
        m_contextInfoProvider = new WorkflowContextInfoProvider( //
            nodeParametersInput, //
            m_withFileSystemAnnotation.map(WithFileSystem::value).orElse(null), //
            m_withFileSystemAnnotation.map(WithFileSystem::connectionProvider).orElse(null) //
        );
        m_nodeParametersInput = nodeParametersInput;
        m_filtersClass =
            MultiFileSelectionUtil.extractFileChooserFiltersClass(m_node).orElseThrow(IllegalStateException::new);

        validateAnnotations();
    }

    static final List<MultiFileSelectionMode> SINGLE_SELECTION_MODES =
        List.of(MultiFileSelectionMode.FILE, MultiFileSelectionMode.FOLDER, MultiFileSelectionMode.WORKFLOW);

    void validateAnnotations() {
        final var values = List.of(m_multiFileSelectionAnnotation.value());
        if (SINGLE_SELECTION_MODES.containsAll(values)) {
            throw new UiSchemaGenerationException(
                "MultiFileSelectionWidget must not be configured with only single selection modes");
        }
    }

    @Override
    public Optional<FileChooserRendererOptions.MultiFileChooserOptions> getOptions() {
        return Optional.of(new FileChooserRendererOptions.MultiFileChooserOptions() {

            @Override
            public Optional<FileSystemOption[]> getFileSystems() {
                return m_withFileSystemAnnotation.map(WithFileSystem::value);
            }

            @Override
            public MultiFileSelectionMode[] getPossibleFilterModes() {
                return m_multiFileSelectionAnnotation.value();
            }

            @Override
            public Optional<String[]> getFileExtensions() {
                return m_fileReaderAnnotation.map(FileReaderWidget::fileExtensions).filter(exts -> exts.length > 0);
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
            public FileChooserRendererOptions.Filters getFilters() {
                return new FileChooserRendererOptions.Filters() {

                    @Override
                    public String getClassId() {
                        return m_filtersClass.getName();
                    }

                    @Override
                    public Object getUiSchema() {
                        var elementTree = new WidgetTreeFactory().createTree(m_filtersClass, null);

                        /**
                         * Parent widget trees are not supported for filter sub-UI schemas. I.e. the legacy button will
                         * not work in there. TODO: UIEXT-1673: Remove this comment
                         */
                        return JsonFormsUiSchemaUtil.buildUISchema(List.of(elementTree), List.of(),
                            m_nodeParametersInput);
                    }
                };
            }
        });
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        Map<String, Class<? extends StateProvider>> stateProviders = new HashMap<>();
        m_withFileSystemAnnotation.map(WithFileSystem::connectionProvider)
            .filter(Predicate.not(ConnectedFSOptionsProvider.class::equals))
            .ifPresent(provider -> stateProviders.put(UiSchema.TAG_CONNECTED_FS_OPTIONS, provider));
        return stateProviders;
    }
}
