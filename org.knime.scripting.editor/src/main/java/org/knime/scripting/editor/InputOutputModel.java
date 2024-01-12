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
 *   Oct 26, 2023 (benjamin): created
 */
package org.knime.scripting.editor;

import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.scripting.editor.InputOutputModel.InputOutputModelSubItem;

/**
 * An item that will be displayed in the input/output panel of the script editor
 *
 * @param name The name of the item
 * @param codeAlias The code alias needed to access this item in the code
 * @param subItemCodeAliasTemplate A Handlebars.js template that is used for code alias insertion. It should have a
 *            single input parameter { subItems: string[] } that can be used to fill in the subItems.
 * @param requiredImport The import statement that is needed to use this object or null if there is none.
 * @param multiSelection whether to enable multi selection in the frontend.
 * @param subItems A (possibly empty) list of sub items.
 */
public record InputOutputModel(String name, //
    String codeAlias, //
    String subItemCodeAliasTemplate, //
    String requiredImport, //
    boolean multiSelection, //
    InputOutputModelSubItem[] subItems) {

    /**
     * An item in an InputOutputModel, e.g. for table columns
     *
     * @param name The name of the sub item
     * @param type The display name of the type of the sub item
     */
    public static record InputOutputModelSubItem(String name, String type) {

    }

    /**
     * Helper method to convert a table spec into a {@link InputOutputModel}
     *
     * @param name
     * @param spec
     * @param codeAlias
     * @param subItemCodeAliasTemplate
     * @param requiredImport
     * @return the {@link InputOutputModel} with the subItems from the table columns
     */
    public static InputOutputModel createFromTableSpec( //
        final String name, //
        final DataTableSpec spec, //
        final String codeAlias, //
        final String subItemCodeAliasTemplate, //
        final String requiredImport //
    ) {
        final var columnNames = spec.getColumnNames();
        final var columnTypes = IntStream.range(0, spec.getNumColumns())
            .mapToObj(i -> spec.getColumnSpec(i).getType().getName()).toArray(String[]::new);
        final var subItems = IntStream.range(0, spec.getNumColumns())
            .mapToObj(i -> new InputOutputModelSubItem(columnNames[i], columnTypes[i]))
            .toArray(InputOutputModelSubItem[]::new);

        return new InputOutputModel(name, codeAlias, subItemCodeAliasTemplate, requiredImport, true, subItems);
    }
}
