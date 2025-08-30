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
 *   May 15, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.dbtableselection;

import java.util.Objects;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persistable;

/**
 * A setting that represents a particular table from a database chosen by the user.
 *
 * Note that although this class implements {@link WidgetGroup}, it has its own renderer in the frontend which also
 * includes a browser to select a table and schema.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DBTableSelection implements Persistable, WidgetGroup {

    @Widget(title = "Database name", description = "The name of the database (or catalogue) to use.")
    @Modification.WidgetReference(CatalogNameRef.class)
    String m_catalogName = "";

    /**
     * Access this method only if the database supports catalogues.
     *
     * @return the name of the database to use.
     */
    public String getCatalogName() {
        return m_catalogName;
    }

    interface CatalogNameRef extends Modification.Reference {
    }

    @Widget(title = "Schema name", description = """
            The database schema to read the table from. If not provided, \
            the default schema of the database will be used.
            """)
    @Modification.WidgetReference(SchemaNameRef.class)
    String m_schemaName; // null means default schema

    /**
     * Gets the name of the schema to use. When an empty optional is returned, the default schema of the database should
     * be used.
     *
     * @return The name of the schema to use. Never null, but possibly an empty. If validated, it is guaranteed that the
     *         value is not blank if it is non-empty.
     */
    public String getSchemaName() {
        return m_schemaName == null ? "" : m_schemaName;
    }

    interface SchemaNameRef extends Modification.Reference {
    }

    /**
     * The name of the table to use.
     */
    @Widget(title = "Table name", description = """
            Name of the database table to read data from. Must match the \
            table name as it appears in the selected schema.
            """)
    @Modification.WidgetReference(TableNameRef.class)
    String m_tableName = "";

    /**
     * Gets the name of the table to use.
     *
     * @return The name of the table to use.
     */
    public String getTableName() {
        return m_tableName;
    }

    interface TableNameRef extends Modification.Reference {
    }

    /**
     * Use to create an empty instance. Empty table and catalogue name, null schema name (i.e. default schema).
     */
    public DBTableSelection() {
    }

    /**
     * Constructor. If this is used, the catalogue name is not set. This makes sense if the database does not support
     * catalogues.
     *
     * @param tableName the name of the table to use, not null.
     */
    public DBTableSelection(final String tableName) {
        m_tableName = Objects.requireNonNull(tableName);
    }

    /**
     * Constructor. This is used if the database supports catalogues.
     *
     * @param catalogName the name of the catalogue to use, not null. Use an empty string if the database does not
     *            support catalogues.
     * @param schemaName the name of the schema to use, possibly null.
     * @param tableName the name of the table to use, not null.
     */
    public DBTableSelection(final String catalogName, final String schemaName, final String tableName) {
        m_catalogName = Objects.requireNonNull(catalogName);
        m_schemaName = schemaName;
        m_tableName = Objects.requireNonNull(tableName);
    }

    /**
     * Modification class to override descriptions.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public abstract static class DBTableSelectionModification implements Modification.Modifier {

        private static final String DESCRIPTION = "description";

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var catalogDesc = getCatalogNameDescription();
            if (catalogDesc.isPresent()) {
                group.find(CatalogNameRef.class).modifyAnnotation(Widget.class) //
                    .withProperty(DESCRIPTION, catalogDesc.get()).modify();
            }
            final var schemaDesc = getSchemaNameDescription();
            if (schemaDesc.isPresent()) {
                group.find(SchemaNameRef.class).modifyAnnotation(Widget.class) //
                    .withProperty(DESCRIPTION, schemaDesc.get()).modify();
            }
            final var tableDesc = getTableNameDescription();
            if (tableDesc.isPresent()) {
                group.find(TableNameRef.class).modifyAnnotation(Widget.class) //
                    .withProperty(DESCRIPTION, tableDesc.get()).modify();
            }
        }

        /**
         * @return the description of the Catalog field.
         */
        protected Optional<String> getCatalogNameDescription() {
            return Optional.empty();
        }

        /**
         * @return the description of the Schema field.
         */
        protected Optional<String> getSchemaNameDescription() {
            return Optional.empty();
        }

        /**
         * @return the description of the Table field.
         */
        protected Optional<String> getTableNameDescription() {
            return Optional.empty();
        }
    }

    /**
     * Validates the settings of this table selection excluding catalogue name.
     *
     * @throws InvalidSettingsException if the schema is present but blank.
     */
    public void validateSchemaAndTable() throws InvalidSettingsException {
        if (m_schemaName != null && !m_schemaName.isEmpty() && m_schemaName.isBlank()) {
            throw new InvalidSettingsException("The schema name must not be blank.");
        }
        if (m_tableName.isBlank()) {
            throw new InvalidSettingsException("The table name must not be blank.");
        }
    }
}
