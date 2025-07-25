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
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;
import org.knime.node.parameters.NodeParameters;

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
     * @param dbTableAdapter the DBTableAdapter to use - this should be an annotation on the {@link NodeParameters}
     *            class that needs this service to exist.
     */
    public DBTableChooserDataService(final Class<? extends DBTableAdapter> dbTableAdapter) {
        Objects.requireNonNull(dbTableAdapter, "dbTableAdapter must not be null");

        m_adapterInstance = DBTableAdapterProvider.DBTableAdapter.instantiate(dbTableAdapter,
            () -> DataServiceContext.get().getInputSpecs());
    }

    enum DBItemType {
            ROOT, CATALOG, SCHEMA, TABLE;

        DBItemType nextDown(final boolean supportsCatalogues) {
            return switch (this) {
                case ROOT -> supportsCatalogues ? CATALOG : SCHEMA;
                case CATALOG -> SCHEMA;
                case SCHEMA -> TABLE;
                case TABLE -> throw new IllegalArgumentException("Nothing comes below table");
            };
        }

        String niceName() {
            return this.name().toLowerCase(Locale.getDefault());
        }
    }

    record DBContainerAndChildren(List<String> pathParts, List<DBItem> children) {

        private static <T> List<T> append(final List<T> list1, final T newItem) {
            return Stream.concat(list1.stream(), Stream.of(newItem)).toList();
        }

        private DBContainerAndChildren childToContainer(final DBItem childOfThisContainer, final DBTableAdapter adapter)
            throws SQLException {

            var newPathParts = append(pathParts, childOfThisContainer.name);

            return switch (childOfThisContainer.type) {
                case CATALOG -> new DBContainerAndChildren( //
                    newPathParts, //
                    adapter.listSchemas(childOfThisContainer.name).stream().map(DBItem::schema).toList() //
                    );
                case SCHEMA -> new DBContainerAndChildren( //
                    newPathParts, //
                    adapter.listTables(newPathParts.get(0), newPathParts.get(newPathParts.size() - 1)).stream() //
                        .map(DBItem::table) //
                        .toList());
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

        boolean hasChild(final String childName) {
            return children.stream().anyMatch(item -> item.name.equalsIgnoreCase(childName));
        }
    }

    record DBItem(String name, DBItemType type) {

        static DBItem table(final String name) {
            return new DBItem(name, DBItemType.TABLE);
        }

        static DBItem schema(final String name) {
            return new DBItem(name, DBItemType.SCHEMA);
        }

        static DBItem catalog(final String name) {
            return new DBItem(name, DBItemType.CATALOG);
        }
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
    private static List<String> normalisePathParts(final List<String> pathParts) {
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

                if (!currentContainer.hasChild(nextPart)) {
                    return ListItemsResult.of( //
                        currentContainer, //
                        "Could not find %s '%s'".formatted( //
                            currentContainerType.nextDown(supportsCatalogues).niceName(), //
                            nextPart //
                        ) //
                    );
                }

                currentContainer = currentContainer.childToContainer(nextPart, m_adapterInstance);
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
                isSchemaAndHasNoMatchingChildren ? "Could not find table '%s'.".formatted(tableName) : null //
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
                level = level.childToContainer(pathParts.get(i), m_adapterInstance);
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
         * Intended to be subclassed by the user of this annotation. Note that the class must have a constructor with a
         * single argument of type {@link DataServiceContext} - this will be provided by the service so you don't need
         * to worry about where it comes from.
         */
        abstract class DBTableAdapter {

            protected final Supplier<PortObjectSpec[]> m_inputPortSpecSupplier;

            /**
             * Constructor for the DBTableAdapter.
             *
             * @param inputPortSpecSupplier the supplier for the input ports specced to use, which should be passed by
             *            the subclass. The subclass can get it from the service by providing a single-argument
             *            constructor that takes a {@link Supplier} supplier parameter.
             */
            protected DBTableAdapter(final Supplier<PortObjectSpec[]> inputPortSpecSupplier) {
                m_inputPortSpecSupplier = inputPortSpecSupplier;
            }

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

            /**
             * Checks if the database is connected.
             *
             * @return true if the database is connected, false otherwise.
             */
            public abstract boolean isDbConnected();

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
