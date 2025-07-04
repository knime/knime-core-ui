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
 *   Jun 17, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data.render;

import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;

/**
 * Describes how {@link DataCell} are supposed to be rendered in the table on the frontend side.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public enum DataCellContentType {

        /**
         * If a {@link DataCell} shall be represented as plain text.
         */
        TXT,

        /**
         * Multiline text from newline characters.
         */
        MULTI_LINE_TXT,

        /**
         * Markup content.
         */
        HTML,

        /**
         * If a {@link DataCell} shall be represented as an image which is given by a relative path.
         */
        IMG_PATH,

        /**
         * If a {@link DataCell} shall be represented as formatted and highlighted JSON.
         */
        JSON,

        /**
         * If a {@link DataCell} shall be represented as formatted and highlighted XML.
         */
        XML;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Checks whether a list of column matches at least one of the given content types as their default content type.
     *
     * @param tableSpec the table spec whose columns to check
     * @param contentTypes the content types to check for
     * @return {@code true} if at least one column matches one of the given content types as default
     */
    public static boolean anyMatchAsDefault(final DataTableSpec tableSpec, final DataCellContentType... contentTypes) {
        final var targetTypes = Set.of(contentTypes);
        return tableSpec.stream() //
            .map(SwingBasedRendererFactory::createDefaultRenderer) //
            .map(DataValueRenderer::getContentType) //
            .anyMatch(targetTypes::contains);
    }

}
