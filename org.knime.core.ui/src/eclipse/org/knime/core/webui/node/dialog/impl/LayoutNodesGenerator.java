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
 *   Mar 22, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.impl;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
final class LayoutNodesGenerator {

    private final ObjectMapper m_mapper;

    private final Map<Class<?>, ArrayNode> m_content;

    private final Class<?> m_rootClass;

    /**
     * @param mapper the object mapper used for the ui schema generation
     * @param content the mapping between layout parts and their contained settings controls
     * @param rootClass the root class of the layout. This can be null but only if no nested layout parts exist.
     */
    LayoutNodesGenerator(final ObjectMapper mapper, final Map<Class<?>, ArrayNode> content, final Class<?> rootClass) {
        m_mapper = mapper;
        m_content = content;
        m_rootClass = rootClass;
    }

    ObjectNode build() {
        final var root = m_mapper.createObjectNode();
        final var rootElements = addLayoutNode(root, m_rootClass);
        if (m_rootClass != null) {
            buildLayout(m_rootClass, rootElements);
        }
        return root;
    }

    private void buildLayout(final Class<?> parentClass, final ArrayNode parentNode) {
        Arrays.asList(parentClass.getDeclaredClasses()).forEach(childClass -> {
            final var childNode = addLayoutNode(parentNode.addObject(), childClass);
            buildLayout(childClass, childNode);
        });

    }

    private ArrayNode addLayoutNode(final ObjectNode root, final Class<?> layoutElement) {
        final var layoutPart = LayoutPart.determineFromClassAnnotation(layoutElement);
        final var contentForClass = m_content.getOrDefault(layoutElement, m_mapper.createArrayNode());
        return layoutPart.create(root, layoutElement, contentForClass);
    }
}
