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
 *   Nov 15, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import java.io.IOException;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.URIFSConnectionConfig;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

final class FSLocationJsonSerializationUtil {

    private FSLocationJsonSerializationUtil() {
        // utility
    }

    private static final String PATH_KEY = "path";

    private static final String TIMEOUT_KEY = "timeout";

    private static final int DEFAULT_TIMEOUT = URIFSConnectionConfig.DEFAULT_TIMEOUT_MILLIS;

    private static final String CATEGORY_KEY = "fsCategory";

    private static final String CONTEXT_KEY = "context";

    private static final String FS_SPECIFIER_KEY = "fsSpecifier";

    private static final String FS_TO_STRING_KEY = "fsToString";

    /**
     * Adds serialization logic for the {@link FSLocation} within a {@link FileSelection}
     *
     * @param module
     */
    public static void addSerializerAndDeserializer(final SimpleModule module) {
        module.addSerializer(FSLocation.class, new FSLocationSerializer());
        module.addDeserializer(FSLocation.class, new FSLocationDeserializer());
    }

    private static final String RELATIVE_TO_CURRENT_HUBSPACE = "relative-to-current-hubspace";

    private static final String RELATIVE_TO_EMBEDDED_DATA = "relative-to-embedded-data";

    static final class FSLocationSerializer extends JsonSerializer<FSLocation> {

        @Override
        public void serialize(final FSLocation fsLocation, final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            final var fsCategory = toFrontendFsCategory(fsLocation);
            gen.writeStringField(CATEGORY_KEY, fsCategory);
            gen.writeStringField(PATH_KEY, fsLocation.getPath());
            final var timeout = fsLocation.getFileSystemSpecifier() //
                .filter(specifier -> isCustomURL(fsCategory)) //
                .map(Integer::valueOf) //
                .orElse(DEFAULT_TIMEOUT);
            gen.writeNumberField(TIMEOUT_KEY, timeout);
            writeContext(gen, fsLocation, fsCategory);
            gen.writeEndObject();
        }

        private static String toFrontendFsCategory(final FSLocation fsLocation) {
            if (isCurrentHubSpace(fsLocation)) {
                return RELATIVE_TO_CURRENT_HUBSPACE;
            }
            if (isEmbeddedData(fsLocation)) {
                return RELATIVE_TO_EMBEDDED_DATA;
            }
            return fsLocation.getFileSystemCategory();
        }

        private static void writeContext(final JsonGenerator gen, final FSLocation fsLocation, final String fsCategory)
            throws IOException {
            gen.writeObjectFieldStart(CONTEXT_KEY);
            if (useFileSystemSpecifierIfPresent(fsCategory)) {
                final var fsSpecifier = fsLocation.getFileSystemSpecifier();
                if (fsSpecifier.isPresent()) {
                    gen.writeStringField(FS_SPECIFIER_KEY, fsSpecifier.get());
                }
            }
            gen.writeStringField(FS_TO_STRING_KEY, fsLocation.toString());
            gen.writeEndObject();
        }
    }

    static final class FSLocationDeserializer extends JsonDeserializer<FSLocation> {

        @Override
        public FSLocation deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final var node = (JsonNode)p.getCodec().readTree(p);
            final var fsCategory = extractString(node, CATEGORY_KEY);
            final var path = extractString(node, PATH_KEY);
            if (useFileSystemSpecifierIfPresent(fsCategory)) {
                final var fsSpecifier = extractString(node.get(CONTEXT_KEY), FS_SPECIFIER_KEY);
                return new FSLocation(fsCategory, fsSpecifier, path);
            }
            if (isCustomURL(fsCategory)) {
                final var timeout = extractString(node, TIMEOUT_KEY);
                return new FSLocation(fsCategory, timeout, path);
            }
            if (fsCategory.equals(RELATIVE_TO_CURRENT_HUBSPACE)) {
                return new FSLocation(FSCategory.RELATIVE, RelativeTo.SPACE.getSettingsValue(), path);
            }
            if (fsCategory.equals(RELATIVE_TO_EMBEDDED_DATA)) {
                return new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), path);
            }
            return new FSLocation(fsCategory, path);
        }

        private static String extractString(final JsonNode node, final String key) {
            final var value = node.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.asText();
        }
    }

    static boolean isLocal(final String fsCategory) {
        return fsCategory.equals(FSCategory.LOCAL.toString());
    }

    static boolean isCustomURL(final String fsCategory) {
        return fsCategory.equals(FSCategory.CUSTOM_URL.toString());
    }

    static boolean isConnected(final String fsCategory) {
        return fsCategory.equals(FSCategory.CONNECTED.toString());
    }

    static boolean isCurrentHubSpace(final FSLocation fsLocation) {
        return isRelativeTo(fsLocation, RelativeTo.SPACE);
    }

    static boolean isEmbeddedData(final FSLocation fsLocation) {
        return isRelativeTo(fsLocation, RelativeTo.WORKFLOW_DATA);
    }

    static boolean isRelativeTo(final FSLocation fsLocation, final RelativeTo relativeTo) {
        return fsLocation.getFSCategory() == FSCategory.RELATIVE && fsLocation.getFileSystemSpecifier()
            .filter(specifier -> specifier.equals(relativeTo.getSettingsValue())).isPresent();
    }

    static boolean useFileSystemSpecifierIfPresent(final String fsCategory) {
        return !isLocal(fsCategory) && !isCustomURL(fsCategory) && !fsCategory.equals(RELATIVE_TO_CURRENT_HUBSPACE)
            && !fsCategory.equals(RELATIVE_TO_EMBEDDED_DATA);
    }

}
