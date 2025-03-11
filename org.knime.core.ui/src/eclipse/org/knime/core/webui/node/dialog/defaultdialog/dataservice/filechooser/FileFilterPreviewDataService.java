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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std;

/**
 *
 * @author david
 */
public final class FileFilterPreviewDataService {

    private final FileSystemConnector m_fsConnector;

    /**
     * This data service is used in the DefaultNodeDialog and can be accessed by the frontend using the name
     * "fileChooser".
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
     * @param listItemsConfig
     * @return the list of items together with the total number of items before filtering
     * @throws IOException
     */
    public PreviewResult listItemsForPreview(final String fileSystemId, final String path,
        final ListItemsConfigForPreview listItemsConfig) throws IOException {
        try (var fileSystem = m_fsConnector.getFileChooserBackend(fileSystemId).getFileSystem()) {
            return listFilteredAndSortedItemsForPreview(Path.of(path), fileSystem, listItemsConfig);
        }
    }

    static record AdditionalFilters( //
        /**
         * Either 'all' or 'custom'
         */
        String fileFormat, //
        /**
         * A list of allowed extensions, only used if fileFormat is 'custom'
         */
        List<String> fileExtensions, //
        /**
         * Either 'all_pass', 'regex', or 'wildcard'
         */
        String filenamePatternType, //
        /**
         * The pattern to be used for filtering the filenames, only used if filenamePatternType is 'regex' or 'wildcard'
         */
        String filenamePattern, //
        /**
         *
         */
        boolean filenamePatternCaseSensitive,
        /**
         * Whether to include hidden files or not
         */
        boolean includeHiddenFiles, //
        /**
         * Either 'all_pass', 'regex', or 'wildcard'
         */
        String folderNamePatternType, //
        /**
         * The pattern to be used for filtering the folder names, only used if folderNamePatternType is 'regex' or
         * 'wildcard'
         */
        String folderNamePattern, //
        /**
         *
         */
        boolean folderNamePatternCaseSensitive, //
        /**
         * Whether to include hidden folders or not
         */
        boolean includeHiddenFolders, //
        /**
         * Whether to follow symlinks or not
         */
        boolean followLinks, //
        /**
         * Whether to include subfolders recursively or not
         */
        boolean includeSubfolders //
    ) {
        private boolean folderMatches(final Path px) {
            if (!Files.isDirectory(px)) {
                throw new IllegalArgumentException("This method is only for directories");
            }

            if (!followLinks && Files.isSymbolicLink(px)) {
                return false;
            }

            try {
                if (!includeHiddenFolders && Files.isHidden(px)) {
                    return false;
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot check if folder is hidden", ex);
            }

            var namePattern = switch (folderNamePatternType) {
                case "all_pass" -> Pattern.compile(".*");
                case "regex" -> Pattern.compile(folderNamePattern,
                    folderNamePatternCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                case "wildcard" -> throw new UnsupportedOperationException("Wildcard is not supported yet");
                default -> throw new IllegalArgumentException("Invalid folder name pattern type");
            };

            return namePattern.matcher(px.toString()).matches();
        }

        private boolean fileMatches(final Path px) {
            if (Files.isDirectory(px)) {
                throw new IllegalArgumentException("This method is only for files");
            }

            try {
                if (!includeHiddenFiles && Files.isHidden(px)) {
                    return false;
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot check if file is hidden", ex);
            }

            var extension = getExtension(px);
            if ("custom".equals(fileFormat) && (extension.isEmpty() || !fileExtensions.contains(extension.get()))) {
                return false;
            }

            // the regex is only applied to the base filename, I guess
            var namePattern = switch (filenamePatternType) {
                case "all_pass" -> Pattern.compile(".*");
                case "regex" -> Pattern.compile(filenamePattern,
                    filenamePatternCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                case "wildcard" -> throw new UnsupportedOperationException("Wildcard is not supported yet");
                default -> throw new IllegalArgumentException("Invalid filename pattern type");
            };

            return namePattern.matcher(px.getFileName().toString()).matches();
        }

        List<Path> findAllMatchingChildrenOfDirectory(final Path px) throws IOException {
            if (!Files.isDirectory(px)) {
                throw new IllegalArgumentException("This method is only for directories");
            }

            List<Path> directSubfolders;
            try (var files = Files.list(px)) {
                directSubfolders = files //
                    .filter(Files::isDirectory) //
                    .collect(Collectors.toCollection(ArrayList::new));
            }

            List<Path> directChildFiles;
            try (var files = Files.list(px)) {
                directChildFiles = files //
                    .filter(Files::isRegularFile) //
                    .collect(Collectors.toCollection(ArrayList::new));
            }

            var allChildFiles = new ArrayList<Path>(directChildFiles);

            // let's first build a list of all files in the directory (recursively if necessary)
            // and then filter that list.
            if (includeSubfolders) {
                var recursiveSubfolders = directSubfolders.stream() //
                    .filter(Files::isDirectory) //
                    .filter(this::folderMatches) //
                    .toList();
                for (var subfolder : recursiveSubfolders) {
                    allChildFiles.addAll(findAllMatchingChildrenOfDirectory(subfolder));
                }
            }

            return allChildFiles.stream() //
                .filter(this::fileMatches) //
                .toList();
        }

        private static Optional<String> getExtension(final Path px) {
            var fileName = px.getFileName().toString();
            var dotIndex = fileName.lastIndexOf('.');
            return (dotIndex <= 0 || dotIndex == fileName.length() - 1) //
                ? Optional.empty() //
                : Optional.of(fileName.substring(dotIndex + 1));
        }
    }

    /**
     * Input parameter for {@link #listFilteredAndSortedItemsForPreview}
     */
    @JsonDeserialize(using = ListItemsConfigForPreview.Deserializer.class)
    @JsonSerialize(using = ListItemsConfigForPreview.Serializer.class)
    static record ListItemsConfigForPreview<T extends FileChooserFilters>( //
        /**
         * Setting this will impact whether non-readable or non-writable files are not displayed
         */
        boolean isWriter, //
        /**
         * the extensions with respect to which the files are filtered. If empty or null, no filters will be applied.
         */
        List<String> extensions, //
        /**
         * additional filter settings for the preview. It will actually be of type T, but we cannot use that here
         * because it breaks the serialization
         */
        T additionalFilterOptions, //
        /**
         * the class identifier of the additional filter. Required for deserialization
         */
        Class<T> additionalFilterOptionsClassIdentifier //
    ) {
        final static class Deserializer<U extends FileChooserFilters>
            extends JsonDeserializer<ListItemsConfigForPreview<U>> {

            @SuppressWarnings("unchecked")
            @Override
            public ListItemsConfigForPreview<U> deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException {

                var mapperWithPackageVisibility = new ObjectMapper();
                mapperWithPackageVisibility
                    .setVisibility(Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.NON_PRIVATE));

                JsonNode node = p.readValueAsTree();
                var classId = node.get("additionalFilterOptionsClassIdentifier").asText();

                Class<U> clazz;
                try {
                    clazz = (Class<U>)Class.forName(classId);
                } catch (ClassNotFoundException ex) {
                    throw new IOException("Cannot find class with name " + classId, ex);
                }

                U additionalFilterOptions;
                try (var traversal = node.get("additionalFilterOptions").traverse(mapperWithPackageVisibility)) {
                    additionalFilterOptions = traversal.readValueAs(clazz);
                }

                // TODO: for some reason this never gets sent to the backend, so we need to
                // write this rubbish here.
                var isWriter = Optional.ofNullable(node.get("isWriter")) //
                    .map(JsonNode::asBoolean) //
                    .orElse(false);

                var extensions = new ArrayList<String>();
                node.get("extensions").forEach(jn -> extensions.add(jn.asText()));

                return new ListItemsConfigForPreview<U>( //
                    isWriter, //
                    extensions, //
                    additionalFilterOptions, //
                    clazz //
                );
            }
        }

        final static class Serializer<U extends FileChooserFilters>
            extends JsonSerializer<ListItemsConfigForPreview<U>> {

            @Override
            public void serialize(final ListItemsConfigForPreview<U> value, final JsonGenerator gen,
                final SerializerProvider serializers) throws IOException {

                // most important field: the additional filter options class identifier
                gen.writeStringField("additionalFilterOptionsClassIdentifier",
                    value.additionalFilterOptionsClassIdentifier.getName());
                gen.writeBooleanField("isWriter", value.isWriter);
                gen.writeArray(value.extensions.toArray(String[]::new), 0, value.extensions.size());
                gen.writeObjectField("additionalFilterOptions", value.additionalFilterOptions);
            }
        }
    }

    record PreviewResult( //
        List<String> items, //
        int totalItemsBeforeFiltering //
    ) {
    }

    private static PreviewResult listFilteredAndSortedItemsForPreview(final Path folder, final FileSystem fileSystem,
        final ListItemsConfigForPreview<?> listItemConfig) throws IOException {

        var filterResult = listItemConfig //
            .additionalFilterOptions() //
            .getPassingFilesInFolderRecursively(folder);

        // still need to apply the general filters though
        var matchingFiles = filterResult.passingFiles().stream() //
            .filter(getFilterPredicate(fileSystem, listItemConfig)) //
            .toList();

        // and sort them
        var sortedFiles = matchingFiles.stream() //
            .sorted(Comparator.comparing(Path::getFileName)) //
            .toList();

        var fileNames = sortedFiles.stream() //
            .map(Path::getFileName) //
            .map(Path::toString) //
            .toList();

        return new PreviewResult(fileNames, filterResult.numFilesBeforeFiltering());
    }

    private static Predicate<Path> getFilterPredicate(final FileSystem fileSystem,
        final ListItemsConfigForPreview<?> listItemConfig) {
        final var extensionsPredicate = getExtensionPredicate(fileSystem, listItemConfig.extensions());
        final var readerOrWriterPredicate = getReaderOrWriterPredicate(listItemConfig.isWriter());
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
