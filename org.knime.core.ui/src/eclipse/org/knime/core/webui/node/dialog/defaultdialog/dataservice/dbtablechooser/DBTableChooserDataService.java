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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter.AdapterTable;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;

/**
 * Data service for the DBTableChooser dialog.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DBTableChooserDataService {

    private final DBTableAdapter m_adapterInstance;

    private final boolean m_allowViews;

    /**
     * Constructor for the DBTableChooserDataService.
     *
     * @param dbTableAdapter the DBTableAdapter to use - this should be an annotation on the {@link NodeParameters}
     *            class that needs this service to exist.
     * @param allowViews whether views are sent to the frontend next to tables. See
     *            {@link DBTableAdapterProvider#allowViews()}.
     */
    public DBTableChooserDataService(final Class<? extends DBTableAdapter> dbTableAdapter, final boolean allowViews) {
        Objects.requireNonNull(dbTableAdapter, "dbTableAdapter must not be null");

        m_adapterInstance = DBTableAdapterProvider.DBTableAdapter.instantiate(dbTableAdapter,
            () -> DataServiceContext.get().getInputSpecs());
        m_allowViews = allowViews;
    }

    DBTableChooserDataService(final Class<? extends DBTableAdapter> dbTableAdapter) {
        this(dbTableAdapter, false);
    }

    enum DBItemType {
            ROOT, CATALOG, SCHEMA,
            /**
             * Including views, see {@link DBTableType}.
             */
            TABLE;

        DBItemType nextDown(final boolean supportsCatalogues) {
            return switch (this) {
                case ROOT -> supportsCatalogues ? CATALOG : SCHEMA;
                case CATALOG -> SCHEMA;
                case SCHEMA -> TABLE;
                default -> throw new IllegalArgumentException("Nothing comes below table");
            };
        }

        String niceName() {
            return this.name().toLowerCase(Locale.getDefault());
        }
    }

    enum DBTableType {
            TABLE, VIEW;
    }

    record DBContainerAndChildren(List<String> pathParts, List<DBItem> children) {

        private static <T> List<T> append(final List<T> list1, final T newItem) {
            return Stream.concat(list1.stream(), Stream.of(newItem)).toList();
        }

        private DBContainerAndChildren childToContainer(final DBItem childOfThisContainer, final DBTableAdapter adapter,
            final boolean allowViews) throws SQLException {

            var newPathParts = append(pathParts, childOfThisContainer.name);

            return switch (childOfThisContainer.type) {
                case CATALOG -> new DBContainerAndChildren( //
                    newPathParts, //
                    adapter.listSchemas(childOfThisContainer.name).stream().map(DBItem::schema).toList() //
                    );
                case SCHEMA -> new DBContainerAndChildren( //
                    newPathParts, //
                    Stream.concat( //
                        getTables(newPathParts.get(0), childOfThisContainer.name, adapter), //
                        allowViews ? getViews(newPathParts.get(0), childOfThisContainer.name, adapter) //
                            : Stream.<DBItem> empty() //
                    ).toList()); //
                default -> throw new IllegalArgumentException("Only catalogues and schemas can be listed.");

            };
        }

        private static Stream<DBItem> getTables(final String catalogue, final String schema,
            final DBTableAdapter adapter) throws SQLException {
            return adapter.listTables(catalogue, schema).stream().map(t -> DBItem.table(t, DBTableType.TABLE));
        }

        private static Stream<DBItem> getViews(final String catalogue, final String schema,
            final DBTableAdapter adapter) throws SQLException {
            return adapter.listViews(catalogue, schema).stream().map(t -> DBItem.table(t, DBTableType.VIEW));
        }

        DBContainerAndChildren childToContainer(final String childOfThisContainer, final DBTableAdapter adapter,
            final boolean allowViews) throws SQLException {

            var childAsDBItem = children.stream() //
                .filter(item -> item.name.equalsIgnoreCase(childOfThisContainer)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Child not found: " + childOfThisContainer));

            return childToContainer(childAsDBItem, adapter, allowViews);
        }

        boolean hasChild(final String childName) {
            return children.stream().anyMatch(item -> item.name.equalsIgnoreCase(childName));
        }
    }

    /**
     * The representation of an item in the database - either a catalogue, schema, or table.
     *
     * @param name the name of the item
     * @param type the type of the item
     * @param tableMetadata present metadata when the item type is TABLE, otherwise null.
     */
    record DBItem(String name, DBItemType type, DBTableMetadata tableMetadata) {

        DBItem(final String name, final DBItemType type) {
            this(name, type, null);
            CheckUtils.checkArgument(type != DBItemType.TABLE, "Table metadata must be provided for table items");
        }

        static DBItem table(final AdapterTable table, final DBTableType tableType) {
            return new DBItem(table.name(), DBItemType.TABLE,
                new DBTableMetadata(tableType, table.containingSchema(), table.containingCatalogue()));
        }

        static DBItem schema(final String name) {
            return new DBItem(name, DBItemType.SCHEMA);
        }

        static DBItem catalog(final String name) {
            return new DBItem(name, DBItemType.CATALOG);
        }
    }

    record DBTableMetadata(DBTableType tableType, String containingSchema, String containingCatalogue) {
    }

    /**
     * Result of listing database items.
     *
     * @param errorMessage if an error occurred, this will contain the error message. If no error occurred, this will be
     *            null.
     * @param nextValidData if some data was successfully retrieved, this will contain the next valid data to display in
     *            the browser. May also contain data if an error occurred, in which case this will be the next valid
     *            container in the given path's hierarchy. If no data was retrieved, this will be null.
     */
    record ListItemsResult(String errorMessage, DBContainerAndChildren nextValidData) {

        static ListItemsResult of(final DBContainerAndChildren container, final String errorMessage) {
            return new ListItemsResult(errorMessage, container);
        }

        static ListItemsResult of(final String errorMessage) {
            return new ListItemsResult(errorMessage, null);
        }

        static ListItemsResult of(final DBContainerAndChildren currentContainer) {
            return new ListItemsResult(null, currentContainer);
        }
    }

    private DBContainerAndChildren rootContainer() throws SQLException {
        var catalogues = m_adapterInstance.listCatalogs();
        var children = catalogues.isPresent() //
            ? catalogues.get().stream().map(DBItem::catalog).toList() //
            : m_adapterInstance.listSchemas(null).stream().map(DBItem::schema).toList();
        return new DBContainerAndChildren( //
            List.of(), //
            children //
        );
    }

    /**
     * Normalises the path parts by removing any ".." that would cancel out a previous part. So for example, the path
     * parts ["a", "b", "..", "c"] would be normalised to ["a", "c"]. If the path parts start with "..", an
     * IllegalArgumentException is thrown, since we only want absolute paths.
     *
     * @param pathParts the path parts to normalise, which must not start with ".."
     * @return the normalised path parts
     */
    private static List<String> normalisePathParts(List<String> pathParts) {
        // truncate the path parts at the first null
        pathParts = pathParts.stream() //
            .takeWhile(Objects::nonNull) //
            .toList();

        if (pathParts.isEmpty()) {
            return List.of(); // nothing to normalise
        } else if (pathParts.get(0).equals("..")) {
            // this is malformed since we want absolute paths
            throw new IllegalArgumentException("Path parts must not start with '..'");
        }
        // any ".." in the path should cancel out the previous part
        var normalisedParts = new java.util.ArrayList<String>();
        for (int i = 0; i < pathParts.size(); ++i) {
            var part = pathParts.get(i);
            if (part.equals("..")) {
                normalisedParts.remove(normalisedParts.size() - 1);
            } else {
                normalisedParts.add(part);
            }
        }

        return normalisedParts;
    }

    private static String capitalise(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
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
     * @param tableName the name of a table which is selected by the frontend. If this is provided, it will only affect
     *            the error message, i.e. if the table is not found in the given schema and catalogue (but the schema
     *            and catalogue are valid) there will be an error message. If you don't want to use this, make it null
     *            or empty and it will be ignored. It will also be ignored if the path parts do not lead to a schema
     *            (e.g. if they lead only to a catalogue or the root, or they're invalid in any other way).
     * @return the result containing either a list of items or an error message
     */
    public ListItemsResult listItems(List<String> pathParts, final String tableName) {
        pathParts = normalisePathParts(pathParts);

        try {
            boolean supportsCatalogues = m_adapterInstance.listCatalogs().isPresent();
            int maxPathParts = supportsCatalogues ? 2 : 1;
            if (pathParts.size() > maxPathParts) {
                // shouldn't happen if the frontend is correct
                throw new IllegalArgumentException("Path parts must be at most " + maxPathParts);
            }

            var currentContainer = rootContainer();
            var currentContainerType = DBItemType.ROOT;

            while (!pathParts.isEmpty()) {
                var nextPart = pathParts.get(0);

                if (nextPart == null || !currentContainer.hasChild(nextPart)) {
                    return ListItemsResult.of( //
                        currentContainer, //
                        "%s '%s' not found. See available %ss below.".formatted( //
                            capitalise(currentContainerType.nextDown(supportsCatalogues).niceName()), //
                            nextPart, //
                            currentContainerType.nextDown(supportsCatalogues).niceName() //
                        ) //
                    );
                }

                currentContainer = currentContainer.childToContainer(nextPart, m_adapterInstance, m_allowViews);
                currentContainerType = currentContainerType.nextDown(supportsCatalogues);
                pathParts = pathParts.subList(1, pathParts.size());
            }

            // Check the table name if we are in a schema and return an error message
            // if it doesn't exist (but return the requested container too)
            var isSchemaAndHasNoMatchingChildren = currentContainerType == DBItemType.SCHEMA //
                && tableName != null //
                && !tableName.isEmpty() //
                && currentContainer.children().stream().noneMatch(item -> item.name.equalsIgnoreCase(tableName));

            return ListItemsResult.of( //
                currentContainer, //
                isSchemaAndHasNoMatchingChildren
                    ? "Table '%s' not found. See available tables below.".formatted(tableName) : null //
            );
        } catch (SQLException ex) { // NOSONAR don't need to rethrow
            return ListItemsResult.of("Database error: " + ex.getMessage());
        }
    }

    /**
     * Get the type of the given path. This will return the type of the last part of the path, or null if the path does
     * not exist in the database.
     *
     * This is extremely useful for validating user inputs, since non-existent paths will return null.
     *
     * @param pathParts the parts of the path - catalogue, schema, table in that order (and catalogue is to be omitted
     *            if unsupported). An empty path will return the root type.
     * @return the type of the last part of the path, or null if the path does not exist
     * @throws SQLException if a database error occurs while checking the path
     */
    public DBItemType itemType(List<String> pathParts) throws SQLException {
        pathParts = normalisePathParts(pathParts);

        boolean supportsCatalogues;
        try {
            supportsCatalogues = m_adapterInstance.listCatalogs().isPresent();
        } catch (SQLException ex) { // NOSONAR don't need to rethrow
            throw new IllegalStateException("Database error: " + ex.getMessage(), ex);
        }

        var type = DBItemType.ROOT;
        var level = rootContainer();
        for (int i = 0; i < pathParts.size(); ++i) {
            type = type.nextDown(supportsCatalogues);
            if (!level.hasChild(pathParts.get(i))) {
                return null; // no such child -> no type
            }

            // we shouldn't do this if this is the last part since it could be
            // a table, which is not a container
            if (i != pathParts.size() - 1) {
                level = level.childToContainer(pathParts.get(i), m_adapterInstance, m_allowViews);
            }
        }

        return type;
    }

    /**
     * Put this annotation on your {@link NodeParameters} class to indicate that you want to use the
     * DBTableChooserDataService.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface DBTableAdapterProvider {

        /**
         * The class that provides the data needed by the service.
         */
        Class<? extends DBTableAdapter> value();

        /**
         * Show validation error for the schema. When set to true (default), the component checks if the specified
         * schema exists in the database, showing a validation error if it does not. Useful for selecting existing
         * schemas.
         */
        boolean validateSchema() default true;

        /**
         * Show validation error for the table. When set to true (default), the component checks if the specified table
         * exists in the database, showing a validation error if it does not. Useful for selecting existing tables.
         * Setting to false allows for the creation of new tables without validation errors.
         */
        boolean validateTable() default true;

        /**
         * If true, views are enabled next to tables.
         *
         * I.e. set this to false if the operation is not read-only.
         *
         * @return true if views should be shown
         */
        boolean allowViews();

        /**
         * Intended to be subclassed by the user of this annotation. Note that the class must have a constructor with a
         * single argument of type {@link DataServiceContext} - this will be provided by the service so you don't need
         * to worry about where it comes from.
         *
         * Contract: Define a constructor like this:
         *
         * <code>
         * public YourDBTableAdapter(final Supplier<PortObjectSpec[]> portObjectSpecSupplier) {
         *
         * }
         * </code>
         *
         * It is guaranteed that {@link #getDbConnectionError} is called before (not in the same instance!) any of the
         * other methods, so you can assume that the specs are of the correct type if the specs from the node parameters
         * input are validated there .
         */
        abstract class DBTableAdapter {

            /**
             * Lists the catalogues in the database. If the database does not support catalogues (e.g. SQLite and most
             * databases), this should return an empty optional.
             *
             * @return the list of catalogues in the database
             * @throws SQLException if a database error occurs while listing the catalogues
             */
            public abstract Optional<List<String>> listCatalogs() throws SQLException;

            /**
             * Lists the schemas in the database. If the database does not support schemas (e.g. MySQL) this will still
             * be a list of strings, which will contain one entry 'default'.
             *
             * @param catalogue to list schemas from. If the database does not support catalogues this should be ignored
             *            (and most databases don't support it).
             *
             * @return the list of schemas in the database
             * @throws SQLException if a database error occurs while listing the schemas
             */
            public abstract List<String> listSchemas(String catalogue) throws SQLException;

            /**
             * Lists the tables in the database under the given schema.
             *
             * @param catalogue to list tables from. If the database does not support catalogues this should be ignored.
             * @param schema the schema to list tables from. If the database does not support schemas this should be
             *            ignored.
             * @return the list of tables in the schema excluding views
             * @throws SQLException if a database error occurs while listing the tables
             */
            public abstract List<AdapterTable> listTables(String catalogue, String schema) throws SQLException;

            /**
             * Lists the views in the database under the given schema.
             *
             * @param catalogue to list views from. If the database does not support catalogues this should be ignored.
             * @param schema the schema to list views from. If the database does not support schemas this should be
             *            ignored.
             * @return the list of views in the schema excluding tables
             * @throws SQLException if a database error occurs while listing the views
             */
            public abstract List<AdapterTable> listViews(String catalogue, String schema) throws SQLException;

            /**
             * The representation of a table or view in the database. This is used to provide additional metadata about
             * the table or view.
             *
             * @param name the name of the table or view
             * @param containingSchema the schema that contains the table or view. This does not need to be the name of
             *            the schema name used to list the tables, since that is sometimes just a dummy display name.
             *            Can be null.
             * @param containingCatalogue the catalogue that contains the table or view.
             *
             * @author Paul Bärnreuther
             */
            public record AdapterTable(String name, String containingSchema, String containingCatalogue) {
            }

            /**
             * Checks if the database is connected.
             *
             * @param input the node parameters input, which can be used to get and validate the specs of the input
             *            ports.
             *
             * @return true if the database is connected, false otherwise.
             */
            public abstract Optional<String> getDbConnectionError(NodeParametersInput input);

            /**
             * Helper method to instantiate a DBTableAdapter subclass. Node developers will probably not need this, but
             * it's useful in multiple places within the WebUI framework.
             *
             * @param <T> the type of the DBTableAdapter subclass
             * @param clazz the class of the DBTableAdapter subclass to instantiate
             * @param supplier the supplier for the input ports specced to use, which should be accepted by the
             *            constructor of the subclass.
             * @return an instance of the DBTableAdapter subclass
             */
            public static <T extends DBTableAdapter> T instantiate(final Class<T> clazz,
                final Supplier<PortObjectSpec[]> supplier) {
                try {
                    var constructor = clazz //
                        .getDeclaredConstructor(Supplier.class);
                    constructor.setAccessible(!Modifier.isPrivate(constructor.getModifiers()));
                    return constructor.newInstance(supplier);
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    throw new IllegalStateException("""
                            The DBTableAdapter subclass %s either does not have the required constructor \
                            with a single argument of type Supplier<PortObjectSpec[]>, or it could not be accessed \
                            due to security or visibility issues. This could also be caused by the class being a \
                            non-static inner class.
                            """.formatted(clazz), ex);
                } catch (InvocationTargetException ex) {
                    throw new IllegalStateException("""
                            The DBTableAdapter subclass %s threw an exception when trying to create an \
                            instance.
                            """.formatted(clazz), ex);
                } catch (InstantiationException ex) {
                    throw new IllegalStateException("""
                            Could not create DBTableAdapter subclass instance. This might mean that it is \
                            an abstract class, interface, or enum.
                            """, ex);
                }
            }
        }
    }
}
