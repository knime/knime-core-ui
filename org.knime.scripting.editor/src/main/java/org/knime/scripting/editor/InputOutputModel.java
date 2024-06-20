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

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;

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
 * @param portType The type of this item, e.g. flow variable, input table, etc.
 * @param portIconColor The color of the port icon.
 */
public record InputOutputModel(String name, //
    String codeAlias, //
    String subItemCodeAliasTemplate, //
    String requiredImport, //
    boolean multiSelection, //
    InputOutputModelSubItem[] subItems, //
    String portType, //
    String portIconColor //
) {

    public static final String FLOW_VAR_PORT_TYPE_NAME = "flowVariable";

    public static final String FLOW_VAR_PORT_ICON_COLOR = null;

    public static final String TABLE_PORT_TYPE_NAME = "table";

    public static final String TABLE_PORT_ICON_COLOR = null;

    public static final String OBJECT_PORT_TYPE_NAME = "object";

    public static final String VIEW_PORT_TYPE_NAME = "view";

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
        return InputOutputModel.createFromTableSpec(name, spec, codeAlias, subItemCodeAliasTemplate, true,
            requiredImport);
    }

    /**
     * Helper method to convert a table spec into a {@link InputOutputModel}
     *
     * @param name
     * @param spec
     * @param codeAlias
     * @param subItemCodeAliasTemplate
     * @param requiredImport
     * @param multipleSelection
     * @return the {@link InputOutputModel} with the subItems from the table columns
     */
    public static InputOutputModel createFromTableSpec( //
        final String name, //
        final DataTableSpec spec, //
        final String codeAlias, //
        final String subItemCodeAliasTemplate, //
        final boolean multipleSelection, //
        final String requiredImport //
    ) {
        return createFromTableSpec(name, spec, codeAlias, subItemCodeAliasTemplate, multipleSelection, requiredImport,
            type -> true);
    }

    /**
     * Helper method to convert a table spec into a {@link InputOutputModel}
     *
     * @param name
     * @param spec
     * @param codeAlias
     * @param subItemCodeAliasTemplate
     * @param requiredImport
     * @param multipleSelection
     * @param dataTypeFilter predicate to filter out columns of unsupported types
     * @return the {@link InputOutputModel} with the subItems from the table columns
     */
    public static InputOutputModel createFromTableSpec( //
        final String name, //
        final DataTableSpec spec, //
        final String codeAlias, //
        final String subItemCodeAliasTemplate, //
        final boolean multipleSelection, //
        final String requiredImport, //
        final Predicate<DataType> dataTypeFilter //
    ) {
        final var subItems = StreamSupport.stream(spec.spliterator(), false) //
            .filter(colSpec -> dataTypeFilter.test(colSpec.getType())) //
            .map(colSpec -> new InputOutputModelSubItem(colSpec.getName(), colSpec.getType().getName())) //
            .toArray(InputOutputModelSubItem[]::new);
        return new InputOutputModel(name, codeAlias, subItemCodeAliasTemplate, requiredImport, multipleSelection,
            subItems, TABLE_PORT_TYPE_NAME, TABLE_PORT_ICON_COLOR);
    }

    /**
     * Helper method to create a table {@link InputOutputModel} for a table that is not connected
     *
     * @param name
     * @param codeAlias
     * @param subItemCodeAliasTemplate
     * @param requiredImport
     * @param multiSelection
     * @return the {@link InputOutputModel} with no subItems
     */
    public static InputOutputModel createForNonAvailableTable( //
        final String name, //
        final String codeAlias, //
        final String subItemCodeAliasTemplate, //
        final String requiredImport, //
        final boolean multiSelection //
    ) {
        return new InputOutputModel(name, codeAlias, subItemCodeAliasTemplate, requiredImport, multiSelection, null,
            TABLE_PORT_TYPE_NAME, TABLE_PORT_ICON_COLOR);
    }

    /**
     * Helper method to create a flow variables {@link InputOutputModel}.
     *
     * @param flowVariables
     * @param codeAlias
     * @param subItemCodeAliasTemplate
     * @param requiredImport
     * @param multiSelection
     *
     * @return the {@link InputOutputModel} with subItems from the collection of flow variables
     */
    public static InputOutputModel createFromFlowVariables( //
        final Collection<FlowVariable> flowVariables, //
        final String codeAlias, //
        final String subItemCodeAliasTemplate, //
        final String requiredImport, //
        final boolean multiSelection //
    ) {
        var subItems = flowVariables.stream() //
            .map(f -> new InputOutputModelSubItem(f.getName(), f.getVariableType().toString())) //
            .toArray(InputOutputModelSubItem[]::new);

        return new InputOutputModel("Flow variables", codeAlias, subItemCodeAliasTemplate, requiredImport,
            multiSelection, subItems, FLOW_VAR_PORT_TYPE_NAME, FLOW_VAR_PORT_ICON_COLOR);
    }
}
