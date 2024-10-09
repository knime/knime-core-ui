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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * An item that will be displayed in the input/output panel of the script editor. Use {@link InputOutputModel#table()},
 * {@link InputOutputModel#flowVariables()}, {@link InputOutputModel#portObject(String)}, or
 * {@link InputOutputModel#view()}, to create builders for input/output models.
 *
 * @param name The name of the item
 * @param codeAlias The code alias needed to access this item in the code
 * @param subItemCodeAliasTemplate A Handlebars.js template that is used for code alias insertion of one or multiple sub
 *            items. It should have a single input parameter { subItems: { name: string, insertionText: string | null
 *            }[] } that can be used to fill in the subItems.
 * @param requiredImport The import statement that is needed to use this object or null if there is none.
 * @param multiSelection Whether to enable simultaneous selection of multiple subitems in the frontend
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

    private static final String UNSUPPORTED_TYPE = "not supported";

    /**
     * An item in an InputOutputModel, e.g. for table columns
     *
     * @param name The name of the sub item
     * @param type The display name of the type of the sub item
     * @param supported Whether this sub item is supported by the current editor
     * @param insertionText A text that is provided to the template when this sub item is inserted into the code. Note
     *            that this is optional. The template also has access to the name which can be enough.
     */
    public static record InputOutputModelSubItem(String name, String type, boolean supported, String insertionText) {
    }

    private static RequiresNameBuilder builder(final String portType, final String portIconColor) {
        return name -> new Builder(portType, portIconColor, name);
    }

    /**
     * @return A builder for an {@link InputOutputModel} that represents a table port
     */
    public static RequiresNameBuilder table() {
        return builder(TABLE_PORT_TYPE_NAME, TABLE_PORT_ICON_COLOR);
    }

    /**
     * @return A builder for an {@link InputOutputModel} that represents flow variables
     */
    public static Builder flowVariables() {
        return builder(FLOW_VAR_PORT_TYPE_NAME, FLOW_VAR_PORT_ICON_COLOR).name("Flow variables");
    }

    /**
     * @param portIconColor The color of the port icon
     * @return A builder for an {@link InputOutputModel} that represents an object port
     */
    public static RequiresNameBuilder portObject(final String portIconColor) {
        return builder(OBJECT_PORT_TYPE_NAME, portIconColor);
    }

    /**
     * @return A builder for an {@link InputOutputModel} that represents a view
     */
    public static RequiresNameBuilder view() {
        return builder(VIEW_PORT_TYPE_NAME, null);
    }

    /** A builder for an {@link InputOutputModel} that requires a name to be set. */
    public interface RequiresNameBuilder {

        /**
         * @param name The name of the item
         * @return A builder for all optional parameters
         */
        Builder name(String name);
    }

    /**
     * Builder for an {@link InputOutputModel}. Only contains the optional stages. Note that the required stage
     * {@link RequiresNameBuilder} is not included. Use {@link InputOutputModel#table()},
     * {@link InputOutputModel#flowVariables()}, {@link InputOutputModel#portObject(String)}, or
     * {@link InputOutputModel#view()}, to create builders for input/output models.
     */
    public static class Builder {

        private final String m_portType;

        private final String m_portIconColor;

        private final String m_name;

        private String m_codeAlias;

        private String m_subItemCodeAliasTemplate;

        private String m_requiredImport;

        private boolean m_multiSelection;

        private List<InputOutputModelSubItem> m_subItems;

        private Builder(final String portType, final String portIconColor, final String name) {
            this.m_portType = portType;
            this.m_portIconColor = portIconColor;
            this.m_name = name;
        }

        /**
         * @param codeAlias The code alias needed to access this item in the code. Can be null in which case code
         *            insertion is disabled (default).
         * @return this builder
         */
        public Builder codeAlias(final String codeAlias) {
            this.m_codeAlias = codeAlias;
            return this;
        }

        /**
         * @param subItemCodeAliasTemplate A Handlebars.js template that is used for code alias insertion of one or
         *            multiple sub items. It should have a single input parameter { subItems: { name: string,
         *            insertionText: string | null }[] } that can be used to fill in the subItems. Can be null in which
         *            case code insertion is disabled (default).
         * @return this builder
         */
        public Builder subItemCodeAliasTemplate(final String subItemCodeAliasTemplate) {
            this.m_subItemCodeAliasTemplate = subItemCodeAliasTemplate;
            return this;
        }

        /**
         * @param requiredImport The import statement that is needed to use this object or null if there is none
         * @return this builder
         */
        public Builder requiredImport(final String requiredImport) {
            this.m_requiredImport = requiredImport;
            return this;
        }

        /**
         * @param multiSelection Whether to enable simultaneous selection of multiple subitems in the frontend (default:
         *            false)
         * @return this builder
         */
        public Builder multiSelection(final boolean multiSelection) {
            this.m_multiSelection = multiSelection;
            return this;
        }

        /**
         * Add subItems to the builder. If subItems have already been added, this will append the new subItems to the
         * existing ones.
         *
         * @param subItems The subItems to add
         * @return this builder
         */
        public Builder subItems(final Collection<InputOutputModelSubItem> subItems) {
            if (this.m_subItems == null) {
                this.m_subItems = new ArrayList<>();
            }
            this.m_subItems.addAll(subItems);
            return this;
        }

        /**
         * Add subItems to the builder from the columns of the {@link DataTableSpec}. If subItems have already been
         * added, this will append the new subItems to the existing ones. All columns are marked as supported.
         *
         * @param spec The {@link DataTableSpec} to get the columns from
         * @param typeNameMapper A function that maps a {@link DataType} to a string that represents the type
         * @return this builder
         */
        public Builder subItems(final DataTableSpec spec, final Function<DataType, String> typeNameMapper) {
            return subItems(spec, typeNameMapper, dt -> true);
        }

        /**
         * Add subItems to the builder from the columns of the {@link DataTableSpec}. If subItems have already been
         * added, this will append the new subItems to the existing ones.
         *
         * @param spec The {@link DataTableSpec} to get the columns from
         * @param typeNameMapper A function that maps a {@link DataType} to a string that represents the type
         * @param isSupported A predicate that checks whether a {@link DataType} is supported by the editor
         * @return this builder
         */
        public Builder subItems(final DataTableSpec spec, final Function<DataType, String> typeNameMapper,
            final Predicate<DataType> isSupported) {
            Function<DataType, String> fallbackTypeNameMapper =
                dt -> isSupported.test(dt) ? typeNameMapper.apply(dt) : UNSUPPORTED_TYPE;
            return subItems( //
                StreamSupport.stream(spec.spliterator(), false) //
                    .map(colSpec -> new InputOutputModelSubItem(colSpec.getName(),
                        fallbackTypeNameMapper.apply(colSpec.getType()), isSupported.test(colSpec.getType()), null)) //
                    .toList() //
            );
        }

        /**
         * Add subItems to the builder from the collection of {@link FlowVariable}s. If subItems have already been
         * added, this will append the new subItems to the existing ones. Uses the {@link VariableType#toString()} as
         * the type name.
         *
         * @param flowVariables The flow variables to get the subItems from
         * @param isSupportedPredicate A predicate that checks whether a {@link VariableType} is supported by the editor
         * @return this builder
         */
        public Builder subItems(final Collection<FlowVariable> flowVariables,
            final Predicate<VariableType<?>> isSupportedPredicate) {
            return subItems(flowVariables, type -> type.toString(), isSupportedPredicate);
        }

        /**
         * Add subItems to the builder from the collection of {@link FlowVariable}s. If subItems have already been
         * added, this will append the new subItems to the existing ones.
         *
         * @param flowVariables The flow variables to get the subItems from
         * @param typeNameMapper A function that maps a {@link VariableType} to a string that represents the type
         * @param isSupportedPredicate A predicate that checks whether a {@link VariableType} is supported by the editor
         * @return this builder
         */
        public Builder subItems(final Collection<FlowVariable> flowVariables,
            final Function<VariableType<?>, String> typeNameMapper,
            final Predicate<VariableType<?>> isSupportedPredicate) {

            return subItems( //
                flowVariables.stream() //
                    .map(f -> new InputOutputModelSubItem( //
                        f.getName(), //
                        isSupportedPredicate.test(f.getVariableType()) ? typeNameMapper.apply(f.getVariableType())
                            : UNSUPPORTED_TYPE, //
                        isSupportedPredicate.test(f.getVariableType()), //
                        null //
                    )).toList() //
            );
        }

        /** @return the {@link InputOutputModel} with the current state of the builder */
        public InputOutputModel build() {
            var subItems = m_subItems == null ? null : m_subItems.toArray(InputOutputModelSubItem[]::new);
            return new InputOutputModel(m_name, m_codeAlias, m_subItemCodeAliasTemplate, m_requiredImport,
                m_multiSelection, subItems, m_portType, m_portIconColor);
        }
    }
}
