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
 *   Mar 26, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import static org.knime.core.webui.node.dialog.defaultdialog.util.MultiFileSelectionUtil.extractFileChooserFiltersClass;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewUtils.AdditionalFilterConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser.FileFilterPreviewUtils.PreviewResult;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.WidgetGroup;

/**
 * A service that can provide a preview of the files within a folder on a file system, while applying additional filter
 * rules to decide what qualifies for the preview.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class FileFilterPreviewDataService {

    private final FileSystemConnector m_fsConnector;

    private final Supplier<Collection<Tree<WidgetGroup>>> m_widgetTrees;

    /**
     * This data service is used in the DefaultNodeDialog and can be accessed by the frontend using the name
     * "fileFilterPreview".
     *
     * @param fsConnector the file system connector to use. It is the responsibility of the caller to ensure that the
     *            connector is closed once it is no longer needed.
     * @param widgetTrees the widget trees to use for the file chooser filters
     */
    public FileFilterPreviewDataService(final FileSystemConnector fsConnector,
        final Supplier<Collection<Tree<WidgetGroup>>> widgetTrees) {
        m_fsConnector = fsConnector;
        m_widgetTrees = widgetTrees;
    }

    private List<Class<? extends FileChooserFilters>> m_filterClasses;

    private List<Class<? extends FileChooserFilters>> getFilterClasses() {
        if (m_filterClasses == null) {
            m_filterClasses =
                m_widgetTrees.get().stream().flatMap(FileFilterPreviewDataService::extractFilterClasses).toList();
        }
        return m_filterClasses;
    }

    private static Stream<Class<? extends FileChooserFilters>> extractFilterClasses(final Tree<WidgetGroup> tree) {
        return tree.getWidgetAndWidgetTreeNodes().flatMap(FileFilterPreviewDataService::extractFilterClassesFromWidget);
    }

    private static Stream<Class<? extends FileChooserFilters>>
        extractFilterClassesFromWidget(final TreeNode<WidgetGroup> node) {
        if (node instanceof ArrayParentNode<WidgetGroup> arrayParentNode) {
            return extractFilterClasses(arrayParentNode.getElementTree());
        }
        return extractFileChooserFiltersClass(node).stream();
    }

    /**
     * Get the items of the specified file system at the specified path, while applying the additional filters for the
     * preview.
     *
     * @param fileSystemId specifying the file system. Supported ids are:
     *            <ul>
     *            <li>"local": For the local file system</li>
     *            <li>"relativeToCurrentHubSpace": For the current space</li>
     *            <li>"embedded": For the current workflow data area</li>
     *            <li>"connected${portIndex}": For the file system connected at portIndex.</li>
     *            </ul>
     * @param path the current path or null to reference the root level.
     * @param filterMode the filter mode (one of the multi file selection modes)
     * @param includeSubfolders if true, display files recursively in child directories
     * @param additionalFilterConfiguration the additional filter configuration to filter the files by
     * @return the list of items together with the total number of items before filtering
     * @throws IOException in case the file system could not be opened
     */
    public PreviewResult listItemsForPreview( //
        final String fileSystemId, //
        final String path, //
        final MultiFileSelectionMode filterMode, //
        final boolean includeSubfolders, //
        final AdditionalFilterConfiguration additionalFilterConfiguration//
    ) throws IOException {
        final var fileChooserFilters =
            additionalFilterConfiguration.toFileChooserFilters(getFilterClasses(), JsonFormsDataUtil.getMapper());

        try (var fileSystem = m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem()) {
            return listFilteredAndSortedItemsForPreview(fileSystem, path, filterMode, includeSubfolders,
                fileChooserFilters);
        }
    }

    private static final int LIMIT_FILES_FOR_PREVIEW = 1000;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileFilterPreviewDataService.class);

    private static PreviewResult listFilteredAndSortedItemsForPreview(final FileSystem fileSystem,
        final String folderInput, final MultiFileSelectionMode filterMode, final boolean includeSubfolders,
        final FileChooserFilters listItemConfig) throws IOException {

        filterMode.assertMultiSelection();

        final Path folder = fileSystem.getPath(folderInput);

        if (!Files.exists(folder)) {
            return new PreviewResult.Error("Root path does not exist.");
        } else if (!Files.isDirectory(folder)) {
            return new PreviewResult.Error("Root path is not a folder.");
        }
        try {
            var filterResult = FileChooserFilters.getPassingFilesInFolder( //
                listItemConfig, //
                folder, //
                filterMode, //
                includeSubfolders, //
                LIMIT_FILES_FOR_PREVIEW //
            );
            return new PreviewResult.Success(filterResult, folder);
        } catch (AccessDeniedException e) { // NOSONAR logged in the frontend
            final var file = e.getFile();
            return new PreviewResult.Error(String.format("Access denied to file '%s'.", file));
        } catch (Exception e) { // NOSONAR
            LOGGER.warn("Error while filtering files", e);
            return new PreviewResult.Error("Error while filtering files: " + e.getMessage());
        }
    }
}
