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
package org.knime.core.webui.node.dialog;

import java.util.Map;
import java.util.Optional;

/**
 * Sealed interface representing the PersistSchema structure.
 *
 * PersistSchema is used to describe the structure of persisted settings in a dialog node. It supports three types of
 * schemas: - PersistLeafSchema: Represents a leaf node. - PersistTreeSchema: Represents an object with nested
 * properties. - PersistArrayParentSchema: Represents an array with items.
 *
 * Config paths are assembled by traversing the schema depth-first, replacing encountered segments with config keys if
 * custom config keys are found in the schema. Traversal ends prematurely if entire custom config paths are found in the
 * segment's schema.
 *
 */
public sealed interface PersistSchema permits PersistSchema.PersistTreeSchema, PersistSchema.PersistLeafSchema {

    /**
     * Used in case the schema segment is renamed to a different config key.
     *
     * @return Optional config key for the schema segment.
     */
    default Optional<String> getConfigKey() {
        return Optional.empty();
    }

    /**
     *
     * Used to define the config paths for this schema segment (and stop the traversal).
     *
     * @return Optional config paths for the schema segment.
     */
    default Optional<String[][]> getConfigPaths() {
        return Optional.empty();
    }

    /**
     * PersistTreeSchema implementation.
     *
     * Represents an object schema with nested properties. Properties can have their own config paths and deprecated
     * config keys.
     */
    non-sealed interface PersistTreeSchema extends PersistSchema {
        /**
         *
         * In contrast to {@link #getConfigPaths()}, the provided paths are a replacement for the property keys nested
         * within this object and not the property key of this object.
         *
         * @return the paths that apply to all properties.
         */
        default Optional<String[][]> getPropertiesConfigPaths() {
            return Optional.empty();
        }

        /**
         * @return Map of property names to their corresponding PersistSchema.
         */
        Map<String, PersistSchema> getProperties();

        /**
         * PersistTreeSchema implementation.
         *
         * Represents an object schema with nested properties. Properties can have their own config paths and deprecated
         * config keys.
         */
        record PersistTreeSchemaRecord(Map<String, PersistSchema> properties) implements PersistTreeSchema {

            @Override
            public Map<String, PersistSchema> getProperties() {
                return properties;
            }

        }
    }

    /**
     * PersistLeafSchema implementation.
     *
     * Represents a leaf node in the schema.
     */
    non-sealed interface PersistLeafSchema extends PersistSchema {
    }

}
