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
 *   Aug 23, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a web resource used in ui-extensions (such as node view, port view and node dialog).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public interface Resource {

    /**
     * The resource content type.
     */
    enum ContentType {
            /**
             * If the resource is a html file.
             */
            HTML,
            /**
             * If the resource represents a vue component to be loaded dynamically.
             */
            VUE_COMPONENT_LIB,
            /**
             * The resource is a svg image.
             */
            SVG,
            /**
             * The resource is a png image.
             */
            PNG,
            /**
             * The resource just references a vue component which is already present on the frontend-side (i.e. it does
             * not provide the vue component itself).
             */
            VUE_COMPONENT_REFERENCE;

        private static final Map<String, ContentType> FILE_EXTENSION_TO_CONTENT_TYPE_MAP = Map.of( //
            ".js", VUE_COMPONENT_LIB, //
            ".html", HTML, //
            ".svg", SVG, //
            ".png", PNG, //
            "vue_component_reference", VUE_COMPONENT_REFERENCE);

        static ContentType determineType(final String path) {
            return FILE_EXTENSION_TO_CONTENT_TYPE_MAP.entrySet().stream() //
                .filter(e -> path.endsWith(e.getKey()))//
                .map(Entry::getValue)//
                .findFirst()//
                .orElseThrow(
                    () -> new IllegalArgumentException("Can't determine resource content type for path " + path));
        }
    }

    /**
     * @return the resource's relative path (including the resource name itself, too)
     */
    String getRelativePath();

    /**
     * @return the actual resource content as {@link InputStream}
     * @throws IOException
     */
    InputStream getInputStream() throws IOException;

    /**
     * A static resource will never change at runtime and thus potentially be 're-used' between, e.g., multiple node
     * instances. I.e. multiple node instances access a static resource at the very same URL which is independent from a
     * specific node instance.
     *
     * @return <code>true</code> if the resource is static and won't ever change at runtime, otherwise
     *         <code>false</code>
     */
    boolean isStatic();

    /**
     * @return the content type of the resource
     */
    ContentType getContentType();

}
