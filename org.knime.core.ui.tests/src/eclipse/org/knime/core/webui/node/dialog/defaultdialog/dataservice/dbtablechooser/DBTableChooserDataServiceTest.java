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
 *   May 20, 2025 (david): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBItem;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBItemType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableMetadata;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableType;
import org.knime.node.parameters.NodeParametersInput;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests for the {@link DBTableChooserDataService} and its adapter.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class DBTableChooserDataServiceTest {

    MockedStatic<DataServiceContext> m_staticContextMock;

    @BeforeEach
    void setup() {
        var mockedContext = mock(DataServiceContext.class);
        Mockito.when(mockedContext.getInputSpecs()).thenReturn(new PortObjectSpec[0]);

        m_staticContextMock = mockStatic(DataServiceContext.class);
        m_staticContextMock.when(DataServiceContext::get).thenReturn(mockedContext);
    }

    @AfterEach
    void tearDown() {
        m_staticContextMock.close();
    }

    @Nested
    final class TestsWithoutCatalogues {

        @Test
        void testListSchemas() {
            var service = new DBTableChooserDataService(AdapterWithoutCatalogues.class);
            var schemas = service.listItems(List.of(), null);

            assertNotNull(schemas.nextValidData(), "expected valid data");
            assertNull(schemas.errorMessage(), "expected no errors");

            var data = schemas.nextValidData();

            assertEquals(2, data.children().size());
            assertTrue(data.children().contains(new DBItem("schema1", DBItemType.SCHEMA)));
            assertTrue(data.children().contains(new DBItem("schema2", DBItemType.SCHEMA)));
        }

        @Test
        void testListTables() {
            var service = new DBTableChooserDataService(AdapterWithoutCatalogues.class);

            var tablesInSchema1 = service.listItems(List.of("schema1"), null);
            assertNotNull(tablesInSchema1.nextValidData(), "expected valid data");
            assertNull(tablesInSchema1.errorMessage(), "expected no errors");

            assertEquals(2, tablesInSchema1.nextValidData().children().size());
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table1", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table1", null))));
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table2", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table2", null))));

            var tablesInSchema2 = service.listItems(List.of("schema2"), null);
            assertNotNull(tablesInSchema2.nextValidData(), "expected valid data");
            assertNull(tablesInSchema2.errorMessage(), "expected no errors");

            assertEquals(2, tablesInSchema2.nextValidData().children().size());
            assertTrue(tablesInSchema2.nextValidData().children().contains(new DBItem("table3", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table3", null))));
            assertTrue(tablesInSchema2.nextValidData().children().contains(new DBItem("table4", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table4", null))));

            var emptyResult = service.listItems(List.of("nonexistent_schema"), null);
            assertNotNull(emptyResult.nextValidData(), "expected errors");
            assertTrue(emptyResult.nextValidData().pathParts().isEmpty(), "expected root to be returned");
        }

        @Test
        void testItemType() throws SQLException {
            var service = new DBTableChooserDataService(AdapterWithoutCatalogues.class);
            var tableItemType = service.itemType(List.of("schema1", "table1"));
            assertEquals(DBItemType.TABLE, tableItemType, "Expected item type to be TABLE");

            var schemaItemType = service.itemType(List.of("schema1"));
            assertEquals(DBItemType.SCHEMA, schemaItemType, "Expected item type to be SCHEMA");

            var rootItemType = service.itemType(List.of());
            assertEquals(DBItemType.ROOT, rootItemType, "Expected item type to be ROOT");

            var nonExistentItemType = service.itemType(List.of("nonexistent_schema", "nonexistent_table"));
            assertNull(nonExistentItemType, "Expected item type to be null for non-existent items");
        }

        @Test
        void testListItemsWithTableArgument() throws SQLException {
            var service = new DBTableChooserDataService(AdapterWithoutCatalogues.class);
            var tables = service.listItems(List.of("schema1"), "table1");

            assertNotNull(tables.nextValidData(), "expected valid data");
            assertNull(tables.errorMessage(), "expected no errors");

            var tablesWithInvalidTableArg = service.listItems(List.of("schema1"), "nonexistent_table");
            assertNotNull(tablesWithInvalidTableArg.errorMessage(), "expected errors");
            assertNotNull(tablesWithInvalidTableArg.nextValidData(), "expected valid data even with errors");
            assertEquals(tablesWithInvalidTableArg.nextValidData(), tables.nextValidData(),
                "Expected the same data to be returned when an invalid table argument is provided");

            var tablesWithIncompletePath = service.listItems(List.of(), "nonexistent_table");
            assertNull(tablesWithIncompletePath.errorMessage(),
                "Expected no error message when listing items with an incomplete path and invalid table");
            assertNotNull(tablesWithIncompletePath.nextValidData(), "expected valid data");
            assertTrue(tablesWithIncompletePath.nextValidData().pathParts().isEmpty(),
                "Expected the root to be returned");
        }
    }

    @Nested
    final class TestsWithCatalogues {

        @Test
        void testListCatalogues() {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class);
            var result = service.listItems(List.of(), null);

            assertNotNull(result.nextValidData(), "expected valid data");
            assertNull(result.errorMessage(), "expected no errors");

            assertEquals(2, result.nextValidData().children().size());
            assertTrue(result.nextValidData().children().contains(new DBItem("catalogue1", DBItemType.CATALOG)));
            assertTrue(result.nextValidData().children().contains(new DBItem("catalogue2", DBItemType.CATALOG)));
        }

        @Test
        void testListSchemas() {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class);
            var schemas = service.listItems(List.of("catalogue1"), null);

            assertNotNull(schemas.nextValidData(), "expected valid data");
            assertNull(schemas.errorMessage(), "expected no errors");

            assertEquals(2, schemas.nextValidData().children().size());
            assertTrue(schemas.nextValidData().children().contains(new DBItem("schema1", DBItemType.SCHEMA)));
            assertTrue(schemas.nextValidData().children().contains(new DBItem("schema2", DBItemType.SCHEMA)));

            schemas = service.listItems(List.of("catalogue2"), null);

            assertNotNull(schemas.nextValidData(), "expected valid data");
            assertNull(schemas.errorMessage(), "expected no errors");

            assertEquals(2, schemas.nextValidData().children().size());
            assertTrue(schemas.nextValidData().children().contains(new DBItem("schema3", DBItemType.SCHEMA)));
            assertTrue(schemas.nextValidData().children().contains(new DBItem("schema4", DBItemType.SCHEMA)));

            // test for non-existent catalogue
            var emptyResult = service.listItems(List.of("nonexistent_catalogue", "schema1"), null);
            assertNotNull(emptyResult.errorMessage(), "expected errors");
            assertTrue(emptyResult.nextValidData().pathParts().isEmpty(), "expected root to be returned");
        }

        @Test
        void testListTables() {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class);

            var tablesInSchema1 = service.listItems(List.of("catalogue1", "schema1"), null);

            assertNotNull(tablesInSchema1.nextValidData(), "expected valid data");
            assertNull(tablesInSchema1.errorMessage(), "expected no errors");

            assertEquals(2, tablesInSchema1.nextValidData().children().size());
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table1", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table1", "Catalog of table1"))));
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table2", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table2", "Catalog of table2"))));

            var tablesInSchema2 = service.listItems(List.of("catalogue1", "schema2"), null);

            assertNotNull(tablesInSchema2.nextValidData(), "expected valid data");
            assertNull(tablesInSchema2.errorMessage(), "expected no errors");

            assertEquals(2, tablesInSchema2.nextValidData().children().size());
            assertTrue(tablesInSchema2.nextValidData().children().contains(new DBItem("table3", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table3", "Catalog of table3"))));
            assertTrue(tablesInSchema2.nextValidData().children().contains(new DBItem("table4", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table4", "Catalog of table4"))));

            // test empty result for non-existent schema
            var invalidResult = service.listItems(List.of("catalogue1", "nonexistent_schema"), null);
            assertNotNull(invalidResult.errorMessage(), "expected errors");
            assertEquals(List.of("catalogue1"), invalidResult.nextValidData().pathParts(),
                "expected the first valid ancestor path to be returned");

            // test empty result for non-existent catalogue
            var emptyResult = service.listItems(List.of("nonexistent_catalogue", "schema1"), null);
            assertNotNull(emptyResult.errorMessage(), "expected errors");
            assertTrue(emptyResult.nextValidData().pathParts().isEmpty(), "expected root to be returned");

            // and for both catalogue and schema non-existent
            var nonExistentResult = service.listItems(List.of("nonexistent_catalogue", "nonexistent_schema"), null);
            assertNotNull(nonExistentResult.errorMessage(), "expected errors");
            assertTrue(nonExistentResult.nextValidData().pathParts().isEmpty(), "expected root to be returned");
        }

        @Test
        void testListTablesAndViews() {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class, true);

            var tablesInSchema1 = service.listItems(List.of("catalogue1", "schema1"), null);

            assertNotNull(tablesInSchema1.nextValidData(), "expected valid data");
            assertNull(tablesInSchema1.errorMessage(), "expected no errors");

            assertEquals(3, tablesInSchema1.nextValidData().children().size());
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table1", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table1", "Catalog of table1"))));
            assertTrue(tablesInSchema1.nextValidData().children().contains(new DBItem("table2", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table2", "Catalog of table2"))));
            assertTrue(tablesInSchema1.nextValidData().children().contains(
                new DBItem("view1", DBItemType.TABLE, new DBTableMetadata(DBTableType.VIEW, "schema1", "catalogue1"))));

        }

        @Test
        void testItemType() throws SQLException {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class);
            var tableItemType = service.itemType(List.of("catalogue1", "schema1", "table1"));
            assertEquals(DBItemType.TABLE, tableItemType, "Expected item type to be TABLE");

            var schemaItemType = service.itemType(List.of("catalogue1", "schema1"));
            assertEquals(DBItemType.SCHEMA, schemaItemType, "Expected item type to be SCHEMA");

            var catalogItemType = service.itemType(List.of("catalogue1"));
            assertEquals(DBItemType.CATALOG, catalogItemType, "Expected item type to be CATALOG");

            var rootItemType = service.itemType(List.of());
            assertEquals(DBItemType.ROOT, rootItemType, "Expected item type to be ROOT");

            var nonExistentItemType =
                service.itemType(List.of("nonexistent_catalogue", "nonexistent_schema", "nonexistent_table"));
            assertNull(nonExistentItemType, "Expected item type to be null for non-existent items");

            // what if only the catalogue is non-existent?
            var catalogueNonExistentItemType = service.itemType(List.of("nonexistent_catalogue", "schema1"));
            assertNull(catalogueNonExistentItemType,
                "Expected item type to be null for non-existent catalogue with valid schema");

            // what if only the schema is non-existent?
            var schemaNonExistentItemType = service.itemType(List.of("catalogue1", "nonexistent_schema"));
            assertNull(schemaNonExistentItemType,
                "Expected item type to be null for non-existent schema with valid catalogue");

            // and if only the table is non-existent?
            var tableNonExistentItemType = service.itemType(List.of("catalogue1", "schema1", "nonexistent_table"));
            assertNull(tableNonExistentItemType,
                "Expected item type to be null for non-existent table with valid catalogue and schema");
        }

        @Test
        void testListItemsWithTableArgument() throws SQLException {
            var service = new DBTableChooserDataService(AdapterWithCatalogues.class);
            var tables = service.listItems(List.of("catalogue1", "schema1"), "table1");

            assertNotNull(tables.nextValidData(), "expected valid data");
            assertNull(tables.errorMessage(), "expected no errors");

            assertEquals(2, tables.nextValidData().children().size());
            assertTrue(tables.nextValidData().children().contains(new DBItem("table1", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table1", "Catalog of table1"))));
            assertTrue(tables.nextValidData().children().contains(new DBItem("table2", DBItemType.TABLE,
                new DBTableMetadata(DBTableType.TABLE, "Schema of table2", "Catalog of table2"))));

            var tablesWithInvalidTableArg = service.listItems(List.of("catalogue1", "schema1"), "nonexistent_table");
            assertNotNull(tablesWithInvalidTableArg.errorMessage(), "expected errors");
            assertNotNull(tablesWithInvalidTableArg.nextValidData(), "expected valid data even with errors");
            assertEquals(tablesWithInvalidTableArg.nextValidData(), tables.nextValidData(),
                "Expected the same data to be returned when an invalid table argument is provided");

            var tablesWithIncompletePath = service.listItems(List.of("catalogue1"), "nonexistent_table");
            assertNull(tablesWithIncompletePath.errorMessage(),
                "Expected no error message when listing items with an incomplete path and invalid table");
            assertNotNull(tablesWithIncompletePath.nextValidData(), "expected valid data");

            tablesWithIncompletePath = service.listItems(List.of(), "nonexistent_table");
            assertNull(tablesWithIncompletePath.errorMessage(),
                "Expected no error message when listing items with an incomplete path and invalid table");
            assertTrue(tablesWithIncompletePath.nextValidData().pathParts().isEmpty(),
                "Expected the root to be returned");
        }

    }

    @SuppressWarnings("unused")
    @Test
    void testServiceInstantiationFailsWithIncorrectAdapters() {
        Throwable cause;

        cause = assertThrows(IllegalStateException.class, () -> {
            new DBTableChooserDataService(ThrowingAdapter.class);
        });
        assertTrue(cause.getMessage().contains("threw an exception when trying to create"),
            "Expected message '%s' to contain something".formatted(cause.getMessage()));

        cause = assertThrows(IllegalStateException.class, () -> {
            new DBTableChooserDataService(AdapterWithMissingConstructor.class);
        });
        assertTrue(cause.getMessage().contains("does not have the required constructor"),
            "Expected message '%s' to contain something".formatted(cause.getMessage()));

        cause = assertThrows(IllegalStateException.class, () -> {
            new DBTableChooserDataService(AbstractAdapter.class);
        });
        assertTrue(cause.getMessage().contains("abstract"),
            "Expected message '%s' to contain something".formatted(cause.getMessage()));
    }

    static class BaseTestAdapter extends DBTableAdapter {

        protected BaseTestAdapter(final Supplier<PortObjectSpec[]> context) {
        }

        @Override
        public boolean supportsCatalogs() throws SQLException {
            return true;
        }

        @Override
        public Optional<List<String>> listCatalogs() {
            return Optional.of(List.of("catalogue1"));
        }

        @Override
        public List<String> listSchemas(final String catalogue) {
            return List.of("schema1", "schema2");
        }

        @Override
        public List<AdapterTable> listTables(final String catalogue, final String schema) {
            return switch (schema) {
                case "schema1" -> Stream.of("table1", "table2").map(t -> new AdapterTable(t, schema, catalogue))
                    .toList();
                case "schema2" -> Stream.of("table3", "table4").map(t -> new AdapterTable(t, schema, catalogue))
                    .toList();
                default -> throw new IllegalArgumentException("Unknown schema: " + schema);
            };
        }

        @Override
        public List<AdapterTable> listViews(final String catalogue, final String schema) throws SQLException {
            return switch (schema) {
                case "schema1" -> List.of(new AdapterTable("view1", schema, catalogue));
                case "schema2" -> List.of();
                default -> throw new IllegalArgumentException("Unknown schema: " + schema);
            };
        }

        @Override
        public Optional<String> getDbConnectionError(final NodeParametersInput input) {
            return Optional.empty();
        }
    }

    static class AdapterWithCatalogues extends BaseTestAdapter {
        protected AdapterWithCatalogues(final Supplier<PortObjectSpec[]> context) {
            super(context);
        }

        @Override
        public Optional<List<String>> listCatalogs() {
            return Optional.of(List.of("catalogue1", "catalogue2"));
        }

        @Override
        public List<String> listSchemas(final String catalogue) {
            return switch (catalogue) {
                case "catalogue1" -> List.of("schema1", "schema2");
                case "catalogue2" -> List.of("schema3", "schema4");
                default -> throw new IllegalArgumentException("Unknown catalogue: " + catalogue);
            };
        }

        @Override
        public List<AdapterTable> listTables(final String catalogue, final String schema) {
            return (switch (schema) {
                case "schema1" -> Stream.of("table1", "table2");
                case "schema2" -> Stream.of("table3", "table4");
                case "schema3" -> Stream.of("table5", "table6");
                case "schema4" -> Stream.of("table7", "table8");
                default -> throw new IllegalArgumentException("Unknown schema: " + schema);
            }).map(t -> new AdapterTable(t, String.format("Schema of %s", t), String.format("Catalog of %s", t)))
                .toList();
        }
    }

    static class AdapterWithoutCatalogues extends BaseTestAdapter {
        protected AdapterWithoutCatalogues(final Supplier<PortObjectSpec[]> context) {
            super(context);
        }

        @Override
        public boolean supportsCatalogs() throws SQLException {
            return false;
        }

        @Override
        public Optional<List<String>> listCatalogs() {
            return Optional.empty();
        }

        @Override
        public List<String> listSchemas(final String catalogue) {
            return List.of("schema1", "schema2");
        }

        @Override
        public List<AdapterTable> listTables(final String catalogue, final String schema) {
            return (switch (schema) {
                case "schema1" -> Stream.of("table1", "table2");
                case "schema2" -> Stream.of("table3", "table4");
                default -> throw new IllegalArgumentException("Unknown schema: " + schema);
            }).map(t -> new AdapterTable(t, String.format("Schema of %s", t), null)).toList();
        }
    }

    static class ThrowingAdapter extends BaseTestAdapter {

        ThrowingAdapter(final Supplier<PortObjectSpec[]> context) {
            super(context);
            throw new IllegalStateException("Test exception");
        }
    }

    static class AdapterWithMissingConstructor extends BaseTestAdapter {

        AdapterWithMissingConstructor() {
            super(null);
        }
    }

    abstract static class AbstractAdapter extends BaseTestAdapter {

        AbstractAdapter(final Supplier<PortObjectSpec[]> context) {
            super(context);
        }
    }

    static class DisconnectedDbAdapter extends BaseTestAdapter {

        DisconnectedDbAdapter(final Supplier<PortObjectSpec[]> context) {
            super(context);
        }

        @Override
        public Optional<String> getDbConnectionError(final NodeParametersInput input) {
            return Optional.of("Please connect a DB!");
        }
    }
}
