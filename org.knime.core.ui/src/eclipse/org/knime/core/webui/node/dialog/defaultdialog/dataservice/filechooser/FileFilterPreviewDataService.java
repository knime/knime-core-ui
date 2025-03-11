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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A service that can provide a preview of the files within a folder on a file system, while applying additional filter
 * rules to decide what qualifies for the preview.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class FileFilterPreviewDataService {

    private final FileSystemConnector m_fsConnector;

    /**
     * This data service is used in the DefaultNodeDialog and can be accessed by the frontend using the name
     * "fileFilterPreview".
     */
    public FileFilterPreviewDataService() {
        m_fsConnector = new FileSystemConnector();
    }

    /**
     * Closes all current file connections. To be called on deactivation of the service.
     */
    public void clear() {
        m_fsConnector.clear();
    }

    /**
     * Get the items of the specified file system at the specified path, while applying the additional filters for the
     * preview.
     *
     * @param fileSystemId
     * @param path
     * @param extensions
     * @param isWriter
     * @param includeSubfolders
     * @param listItemsConfig
     * @return the list of items together with the total number of items before filtering
     * @throws IOException
     */
    public PreviewResult listItemsForPreview( //
        final String fileSystemId, //
        final String path, //
        final List<String> extensions, //
        final boolean isWriter, //
        final boolean includeSubfolders, //
        final AdditionalFilterConfiguration<?> listItemsConfig //
    ) throws IOException {
        try (var fileSystem = m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem()) {
            return listFilteredAndSortedItemsForPreview(Path.of(path), fileSystem, extensions, isWriter,
                includeSubfolders, listItemsConfig);
        }
    }

    /**
     * Input parameter for {@link #listFilteredAndSortedItemsForPreview}
     */
    @JsonDeserialize(using = AdditionalFilterConfiguration.Deserializer.class)
    @JsonSerialize(using = AdditionalFilterConfiguration.Serializer.class)
    static record AdditionalFilterConfiguration<T extends FileChooserFilters>( //
        /**
         * additional filter settings for the preview.
         */
        T additionalFilterOptions, //
        /**
         * the class identifier of the additional filter. Required for deserialization
         */
        Class<T> additionalFilterOptionsClassIdentifier //
    ) {
        final static class Deserializer<U extends FileChooserFilters>

            // TODO(UIEXT-2662): this approach to deserialization opens some security holes, as it allows
            // to deserialize any compatible class, which will cause its default constructor
            // to run. Find a better way to handle this.
            extends JsonDeserializer<AdditionalFilterConfiguration<U>> {

            @SuppressWarnings("unchecked")
            @Override
            public AdditionalFilterConfiguration<U> deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException {

                var mapper = JsonFormsDataUtil.getMapper();

                JsonNode node = p.readValueAsTree();
                var classId = node.get("additionalFilterOptionsClassIdentifier").asText();

                Class<U> clazz;
                try {
                    clazz = (Class<U>)Class.forName(classId);
                } catch (ClassNotFoundException ex) {
                    throw new IOException("Cannot find class with name " + classId, ex);
                }

                if (!FileChooserFilters.class.isAssignableFrom(clazz)) {
                    throw new IOException("Class " + classId + " is not a subclass of FileChooserFilters");
                }

                U additionalFilterOptions;
                try (var traversal = node.get("additionalFilterOptions").traverse(mapper)) {
                    additionalFilterOptions = traversal.readValueAs(clazz);
                }

                return new AdditionalFilterConfiguration<U>( //
                    additionalFilterOptions, //
                    clazz //
                );
            }
        }

        final static class Serializer<U extends FileChooserFilters>
            extends JsonSerializer<AdditionalFilterConfiguration<U>> {

            @Override
            public void serialize(final AdditionalFilterConfiguration<U> value, final JsonGenerator gen,
                final SerializerProvider serializers) throws IOException {

                // most important field: the additional filter options class identifier
                gen.writeStringField("additionalFilterOptionsClassIdentifier",
                    value.additionalFilterOptionsClassIdentifier.getName());
                gen.writeObjectField("additionalFilterOptions", value.additionalFilterOptions);
            }
        }
    }

    static abstract sealed class PreviewResult {

        @JsonProperty("resultType")
        final ResultType m_resultType;

        PreviewResult(final ResultType resultType) {
            this.m_resultType = resultType;
        }

        /**
         * This constructor only for deserialization. Don't use.
         */
        @SuppressWarnings("unused")
        private PreviewResult() {
            this(ResultType.SUCCESS);
        }

        enum ResultType {
                SUCCESS, ERROR
        }

        static final class Success extends PreviewResult {

            /**
             * Items to show in the preview UI.
             */
            @JsonProperty("itemsAfterFiltering")
            List<String> m_itemsAfterFiltering;

            /**
             * Total number of items before filtering. This is just to give the user an idea of how many items were
             * considered.
             */
            @JsonProperty("numItemsBeforeFiltering")
            int m_numItemsBeforeFiltering;

            /**
             * If we don't see all files in the system, we can only say that the number of files is a lower bound. For
             * example, if we have a filter that excludes certain folders, we don't know how many files have been
             * excluded.
             */
            @JsonProperty("numFilesBeforeFilteringIsOnlyLowerBound")
            boolean m_numFilesBeforeFilteringIsOnlyLowerBound;

            /**
             * If we reach the limit, we don't know how many files will be accepted at runtime, only that we have at
             * least as many as the limit.
             */
            @JsonProperty("numFilesAfterFilteringIsOnlyLowerBound")
            boolean m_numFilesAfterFilteringIsOnlyLowerBound;

            Success( //
                final List<String> itemsAfterFiltering, //
                final int numItemsBeforeFiltering, //
                final boolean isTotalItemsBeforeFilteringOnlyLowerBound, //
                final boolean isTotalItemsAfterFilteringOnlyLowerBound //
            ) {
                super(ResultType.SUCCESS);
                this.m_itemsAfterFiltering = itemsAfterFiltering;
                this.m_numItemsBeforeFiltering = numItemsBeforeFiltering;
                this.m_numFilesBeforeFilteringIsOnlyLowerBound = isTotalItemsBeforeFilteringOnlyLowerBound;
                this.m_numFilesAfterFilteringIsOnlyLowerBound = isTotalItemsAfterFilteringOnlyLowerBound;
            }

            /**
             * This constructor only for deserialization. Don't use.
             */
            @SuppressWarnings("unused")
            private Success() {
                super(ResultType.SUCCESS);
            }
        }

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NON_PRIVATE)
        static final class Error extends PreviewResult {

            @JsonProperty("errorMessage")
            String m_errorMessage;

            Error(final String errorMessage) {
                super(ResultType.ERROR);
                this.m_errorMessage = errorMessage;
            }

            /**
             * This constructor only for deserialization. Don't use.
             */
            @SuppressWarnings("unused")
            private Error() {
                super(ResultType.ERROR);
            }
        }
    }

    private static final int LIMIT_FILES_FOR_PREVIEW = 1000;

    private static PreviewResult listFilteredAndSortedItemsForPreview(final Path folder, final FileSystem fileSystem,
        final List<String> extensions, final boolean isWriter, final boolean includeSubfolders,
        final AdditionalFilterConfiguration<?> listItemConfig) throws IOException {

        if (!Files.exists(folder)) {
            return new PreviewResult.Error("Root path does not exist");
        } else if (!Files.isDirectory(folder)) {
            return new PreviewResult.Error("Root path is not a folder");
        }

        try {
            var filterResult = listItemConfig //
                .additionalFilterOptions() //
                .getPassingFilesInFolder(folder, includeSubfolders, LIMIT_FILES_FOR_PREVIEW);

            // still need to apply the general filters though
            var matchingFiles = filterResult.passingFiles().stream() //
                .filter(getFilterPredicate(fileSystem, extensions, isWriter)) //
                .toList();

            // and sort them
            var sortedFiles = matchingFiles.stream() //
                .sorted( //
                    Comparator.comparing(Path::getNameCount) //
                        .thenComparing(Function.identity()) //
                ) //
                .toList();

            var fileNames = sortedFiles.stream() //
                .map(Path::toString) //
                .toList();

            return new PreviewResult.Success( //
                fileNames, //
                filterResult.numFilesBeforeFiltering(), //
                filterResult.numFilesBeforeFilteringIsOnlyLowerBound(), //
                filterResult.numFilesAfterFilteringIsOnlyLowerBound() //
            );
        } catch (Exception e) {
            return new PreviewResult.Error(e.getMessage());
        }
    }

    private static Predicate<Path> getFilterPredicate(final FileSystem fileSystem, final List<String> extensions,
        final boolean isWriter) {
        final var extensionsPredicate = getExtensionPredicate(fileSystem, extensions);
        final var readerOrWriterPredicate = getReaderOrWriterPredicate(isWriter);
        return extensionsPredicate.and(readerOrWriterPredicate);
    }

    private static Predicate<Path> getReaderOrWriterPredicate(final boolean isWriter) {
        return isWriter ? Files::isWritable : Files::isReadable;
    }

    private static Predicate<Path> getExtensionPredicate(final FileSystem fileSystem, final List<String> extensions) {
        if (extensions != null && !extensions.isEmpty()) {
            final var endingsMatcher =
                fileSystem.getPathMatcher(String.format("glob:**.{%s}", String.join(",", extensions)));
            return endingsMatcher::matches;
        }
        return path -> true;
    }
}
