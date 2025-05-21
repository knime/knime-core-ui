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
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data service for the DBTableChooser dialog.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DBTableChooserDataService {

    private final DBTableAdapter m_adapterInstance;

    /**
     * Constructor for the DBTableChooserDataService.
     *
     * @param dbTableAdapter the DBTableAdapter to use - this should be an annotation on the {@link DefaultNodeSettings}
     *            class that needs this service to exist.
     */
    public DBTableChooserDataService(final Class<? extends DBTableAdapter> dbTableAdapter) {
        Objects.requireNonNull(dbTableAdapter, "dbTableAdapter must not be null");

        try {
            var constructor = dbTableAdapter //
                .getDeclaredConstructor(DataServiceContext.class);

            constructor.setAccessible(!Modifier.isPrivate(constructor.getModifiers()));

            m_adapterInstance = constructor.newInstance(DataServiceContext.get());
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("""
                    The DBTableAdapter subclass %s either does not have the required constructor \
                    with a single argument of type DataServiceContext, or it could not be accessed \
                    due to security or visibility issues.
                    """.formatted(dbTableAdapter), ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("""
                    The DBTableAdapter subclass %s threw an exception when trying to create an \
                    instance.
                    """.formatted(dbTableAdapter), ex);
        } catch (InstantiationException ex) {
            throw new IllegalStateException("""
                    Could not create DBTableAdapter subclass instance. This might mean that it is \
                    an abstract class, interface, or enum, or that it is a non-static inner class.
                    """, ex);
        }
    }

    enum DBItemType {
            ROOT, CATALOGUE, SCHEMA, TABLE;

        DBItemType nextDown() {
            return switch (this) {
                case ROOT -> CATALOGUE;
                case CATALOGUE -> SCHEMA;
                case SCHEMA -> TABLE;
                case TABLE -> throw new IllegalArgumentException("Nothing comes below table");
            };
        }

        DBItemType nextUp() {
            return switch (this) {
                case ROOT -> throw new IllegalArgumentException("Nothing comes above root");
                case CATALOGUE -> ROOT;
                case SCHEMA -> CATALOGUE;
                case TABLE -> SCHEMA;
            };
        }

        String niceName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    record DBContainerAndChildren(List<String> pathParts, List<DBItem> children) {

        private static <T> List<T> append(final List<T> list1, final T newItem) {
            return Stream.concat(list1.stream(), Stream.of(newItem)).toList();
        }

        private static List<DBItem> listTables(final DBTableAdapter adapter, final String catalogue,
            final String schema) throws SQLException {
            return adapter.listTables(catalogue, schema).stream() //
                .map(DBItem::table) //
                .toList();
        }

        DBContainerAndChildren childToContainer(final DBItem childOfThisContainer, final DBTableAdapter adapter)
            throws SQLException {

            var newParts = append(pathParts, childOfThisContainer.name);

            return switch (childOfThisContainer.type) {
                case CATALOGUE -> new DBContainerAndChildren( //
                    newParts, //
                    adapter.listSchemas(childOfThisContainer.name).stream().map(DBItem::schema).toList() //
                    );
                case SCHEMA -> new DBContainerAndChildren( //
                    newParts, //
                    DBContainerAndChildren.listTables(adapter, newParts.get(0), newParts.get(newParts.size() - 1)) //
                    );
                case TABLE -> throw new IllegalArgumentException("Table can't be viewed as a container");
                case ROOT -> throw new IllegalArgumentException("Root can't be a child");
            };
        }

        DBContainerAndChildren childToContainer(final String childOfThisContainer, final DBTableAdapter adapter)
            throws SQLException {

            var childAsDBItem = children.stream() //
                .filter(item -> item.name.equalsIgnoreCase(childOfThisContainer)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Child not found: " + childOfThisContainer));

            return childToContainer(childAsDBItem, adapter);
        }
    }

    record DBItem(String name, DBItemType type) {

        static DBItem table(final String name) {
            return new DBItem(name, DBItemType.TABLE);
        }

        static DBItem schema(final String name) {
            return new DBItem(name, DBItemType.SCHEMA);
        }

        static DBItem catalogue(final String name) {
            return new DBItem(name, DBItemType.CATALOGUE);
        }
    }

    static sealed class ListItemsResult {

        @JsonProperty("type")
        final String m_type;

        private ListItemsResult(final String type) {
            // only intended to be referenced by subclasses
            m_type = type;
        }

        static final class Error extends ListItemsResult {

            @JsonProperty("message")
            final String m_message;

            Error(final String message) {
                super("ERROR");
                m_message = message;
            }

        }

        static final class Success extends ListItemsResult {

            @JsonProperty("data")
            final DBContainerAndChildren m_data;

            Success(final DBContainerAndChildren data) {
                super("SUCCESS");
                m_data = data;
            }
        }

        static ListItemsResult success(final DBContainerAndChildren container) {
            return new Success(container);
        }

        static ListItemsResult error(final String errorMessage) {
            return new Error(errorMessage);
        }
    }

    private DBContainerAndChildren rootContainer() throws SQLException {
        var catalogues = m_adapterInstance.listCatalogues();
        var children = catalogues.isPresent() //
            ? catalogues.get().stream().map(DBItem::catalogue).toList() //
            : m_adapterInstance.listSchemas(null).stream().map(DBItem::schema).toList();
        return new DBContainerAndChildren( //
            List.of(), //
            children //
        );
    }

    /**
     * Lists the items in the database under the given path. The path is a list of 0-2 strings, where the first string
     * is a catalogue if catalogues are supported (otherwise a schema), the second is a schema if catalogues are
     * supported. Of course, not all parts need to be specified - if only the catalogue is provided, then the result
     * will be a list of the schemas within it.
     *
     * @param pathParts the parts of the path - catalogue, schema, in that order (and catalogue is to be omitted if
     *            unsupported). An empty path will list the root items, which may be a list of catalogues or a list of
     *            schemas depending on whether catalogues are supported.
     * @return the result containing either a list of items or an error message
     */
    public ListItemsResult listItems(List<String> pathParts) {
        int maxPathParts;
        try {
            maxPathParts = supportsCatalogs() ? 2 : 1;
        } catch (SQLException ex) { // NOSONAR don't need to rethrow
            return ListItemsResult.error("Databse error: " + ex.getMessage());
        }
        if (pathParts.size() > maxPathParts) {
            // shouldn't happen if the frontend is correct
            throw new IllegalArgumentException("Path parts must be at most " + maxPathParts);
        }

        DBContainerAndChildren currentContainer;
        try {
            currentContainer = rootContainer();
        } catch (SQLException ex) { // NOSONAR don't need to rethrow
            return ListItemsResult.error("Database error: " + ex.getMessage());
        }
        var currentContainerType = DBItemType.ROOT;

        while (!pathParts.isEmpty()) {
            var nextPart = pathParts.get(0);

            try {
                currentContainer = currentContainer.childToContainer(nextPart, m_adapterInstance);
                currentContainerType = currentContainerType.nextDown();
            } catch (IllegalArgumentException ex) { // NOSONAR don't need to rethrow
                return ListItemsResult
                    .error("Could not find %s '%s'".formatted(currentContainerType.nextDown().niceName(), nextPart));
            } catch (SQLException ex) { // NOSONAR don't need to rethrow
                return ListItemsResult.error("Database error: " + ex.getMessage());
            }
            pathParts = pathParts.subList(1, pathParts.size());
        }

        return ListItemsResult.success(currentContainer);
    }

    /**
     * Checks if the database supports catalogues. This is usually false for most databases, but not all of them.
     *
     * @return true if the database supports catalogues, false otherwise
     * @throws SQLException if a database error occurs while checking for catalogues
     */
    public boolean supportsCatalogs() throws SQLException {
        return m_adapterInstance.listCatalogues().isPresent();
    }

    /**
     * Put this annotation on your {@link DefaultNodeSettings} class to indicate that you want to use the
     * DBTableChooserDataService.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface DBTableAdapterProvider {

        /**
         * The class that provides the data needed by the service.
         */
        Class<? extends DBTableAdapter> value();

        /**
         * Intended to be subclassed by the user of this annotation. Note that the class must have a constructor with a
         * single argument of type {@link DataServiceContext} - this will be provided by the service so you don't need
         * to worry about where it comes from.
         */
        abstract class DBTableAdapter {

            protected final DataServiceContext m_context;

            /**
             * Constructor for the DBTableAdapter.
             *
             * @param context the DataServiceContext to use, which should be passed by the subclass. The subclass can
             *            get it from the service by providing a single-argument constructor that takes a
             *            {@link DataServiceContext} parameter.
             */
            protected DBTableAdapter(final DataServiceContext context) {
                m_context = context;
            }

            /**
             * Lists the catalogues in the database. If the database does not support catalogues (e.g. SQLite and most
             * databases), this should return an empty optional.
             *
             * @return the list of catalogues in the database
             * @throws SQLException if a database error occurs while listing the catalogues
             */
            public abstract Optional<List<String>> listCatalogues() throws SQLException;

            /**
             * Lists the schemas in the database. If the database does not support schemas (e.g. MySQL) this will still
             * be a list of strings, which will contain one entry 'default'.
             *
             * @param catalogue the catalogue to list schemas from. If the database does not support catalogues this
             *            should be ignored (and most databases don't support it).
             *
             * @return the list of schemas in the database
             * @throws SQLException if a database error occurs while listing the schemas
             */
            public abstract List<String> listSchemas(String catalogue) throws SQLException;

            /**
             * Lists the tables in the database under the given schema.
             *
             * @param catalogue the catalogue to list tables from. If the database does not support catalogues this
             *            should be ignored.
             * @param schema the schema to list tables from. If the database does not support schemas this should be
             *            ignored.
             * @return the list of tables in the schema
             * @throws SQLException if a database error occurs while listing the tables
             */
            public abstract List<String> listTables(String catalogue, String schema) throws SQLException;
        }
    }
}
