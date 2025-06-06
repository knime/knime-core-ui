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
 *   Jun 10, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Map;

import org.knime.core.webui.node.dialog.PersistSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class to transform a {@link PersistSchema} into a JSON representation using Jackson's ObjectMapper.
 *
 * @author Paul Bärnreuther
 */
public final class PersistSchemaToJsonUtil {

    private PersistSchemaToJsonUtil() {
        // Utility class, no instantiation allowed.
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Transforms a PersistSchema into a Jackson ObjectNode.
     *
     * @param schema The PersistSchema to transform.
     * @return An ObjectNode representing the schema.
     */
    public static ObjectNode transformToJson(final PersistSchema schema) {
        ObjectNode node = MAPPER.createObjectNode();

        schema.getConfigKey().ifPresent(key -> node.put("configKey", key));
        schema.getConfigPaths().ifPresent(paths -> node.set("configPaths", createArrayNode(paths)));
        if (schema instanceof PersistSchema.PersistLeafSchema) {
            node.put("type", "leaf");
        } else if (schema instanceof PersistSchema.PersistTreeSchema treeSchema) {
            node.put("type", "object");
            node.set("properties", createPropertiesNode(treeSchema.getProperties()));
        }

        return node;
    }

    private static ArrayNode createArrayNode(final String[][] paths) {
        ArrayNode arrayNode = MAPPER.createArrayNode();
        for (String[] path : paths) {
            ArrayNode innerArray = MAPPER.createArrayNode();
            for (String segment : path) {
                innerArray.add(segment);
            }
            arrayNode.add(innerArray);
        }
        return arrayNode;
    }

    private static ObjectNode createPropertiesNode(final Map<String, PersistSchema> properties) {
        ObjectNode propertiesNode = MAPPER.createObjectNode();
        properties.forEach((key, value) -> propertiesNode.set(key, transformToJson(value)));
        return propertiesNode;
    }
}
