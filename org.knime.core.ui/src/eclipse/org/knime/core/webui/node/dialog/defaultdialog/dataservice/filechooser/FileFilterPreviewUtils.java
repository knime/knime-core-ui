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
 *   Apr 15, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.filechooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileChooserFilters.FilterResult;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Utility methods for {@link FileFilterPreviewDataService}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class FileFilterPreviewUtils {

    private FileFilterPreviewUtils() {
        // utility class
    }

    /**
     * Input parameter for {@link #listFilteredAndSortedItemsForPreview}
     */
    @JsonDeserialize(using = AdditionalFilterConfiguration.Deserializer.class)
    //    @JsonSerialize(using = AdditionalFilterConfiguration.Serializer.class)
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
        // TODO (UIEXT-2662): this approach to deserialization opens some security holes, as it allows
        // to deserialize any compatible class, which will cause its default constructor
        // to run. Find a better way to handle this.that
        static final class Deserializer<U extends FileChooserFilters>
            extends JsonDeserializer<AdditionalFilterConfiguration<U>> {

            @SuppressWarnings("unchecked")
            @Override
            public AdditionalFilterConfiguration<U> deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException {

                final JsonNode node = p.readValueAsTree();
                final var classId = node.get("additionalFilterOptionsClassIdentifier").asText();

                final Class<U> clazz;
                try {
                    clazz = (Class<U>)Class.forName(classId);
                } catch (ClassNotFoundException ex) {
                    throw new IOException("Cannot find class with name " + classId, ex);
                }

                if (!FileChooserFilters.class.isAssignableFrom(clazz)) {
                    throw new IOException("Class " + classId + " is not a subclass of FileChooserFilters");
                }

                final var mapper = JsonFormsDataUtil.getMapper();
                final var additionalFilterOptions = mapper.treeToValue(node.get("additionalFilterOptions"), clazz);

                return new AdditionalFilterConfiguration<>( //
                    additionalFilterOptions, //
                    clazz //
                );
            }
        }
    }

    static sealed class PreviewResult {

        @JsonProperty("resultType")
        final ResultType m_resultType;

        PreviewResult(final ResultType resultType) {
            this.m_resultType = resultType;
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

            Success(final FilterResult filterResult, final Path rootFolder) {
                super(ResultType.SUCCESS);
                this.m_itemsAfterFiltering =
                    filterResult.passingFiles().stream().map(rootFolder::relativize).map(Path::toString).toList();
                this.m_numItemsBeforeFiltering = filterResult.numFilesBeforeFiltering();
                this.m_numFilesBeforeFilteringIsOnlyLowerBound = filterResult.numFilesBeforeFilteringIsOnlyLowerBound();
                this.m_numFilesAfterFilteringIsOnlyLowerBound = filterResult.numFilesAfterFilteringIsOnlyLowerBound();
            }
        }

        static final class Error extends PreviewResult {

            @JsonProperty("errorMessage")
            String m_errorMessage;

            Error(final String errorMessage) {
                super(ResultType.ERROR);
                this.m_errorMessage = errorMessage;
            }
        }
    }
}
