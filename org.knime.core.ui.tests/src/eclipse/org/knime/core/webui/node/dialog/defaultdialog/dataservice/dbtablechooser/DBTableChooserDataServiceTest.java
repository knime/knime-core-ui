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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.webui.data.DataServiceContext;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider.DBTableAdapter;

/**
 *
 * @author david
 */
final class DBTableChooserDataServiceTest {

    Adapter m_adapter;

    @BeforeEach
    void setup() {
        m_adapter = new Adapter(mock(DataServiceContext.class));
    }

    @Test
    void testListSchemas() {
        List<String> schemas = m_adapter.listSchemas("catalogue1");
        assertEquals(2, schemas.size());
        assertTrue(schemas.contains("schema1"));
        assertTrue(schemas.contains("schema2"));
    }

    @Test
    void testListTables() {
        List<String> tables = m_adapter.listTables("catalogue1", "schema1");
        assertEquals(2, tables.size());
        assertTrue(tables.contains("table1"));
        assertTrue(tables.contains("table2"));

        tables = m_adapter.listTables("catalogue1", "schema2");
        assertEquals(2, tables.size());
        assertTrue(tables.contains("table3"));
        assertTrue(tables.contains("table4"));
    }

    @SuppressWarnings({"unused", "static-method"})
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

    static class Adapter extends DBTableAdapter {

        protected Adapter(final DataServiceContext context) {
            super(context);
        }

        @Override
        public Optional<List<String>> listCatalogues() {
            return Optional.of(List.of("catalogue1"));
        }

        @Override
        public List<String> listSchemas(final String catalogue) {
            return List.of("schema1", "schema2");
        }

        @Override
        public List<String> listTables(final String catalogue, final String schema) {
            return switch (schema) {
                case "schema1" -> List.of("table1", "table2");
                case "schema2" -> List.of("table3", "table4");
                default -> throw new IllegalArgumentException("Unknown schema: " + schema);
            };
        }
    }

    static class ThrowingAdapter extends Adapter {

        ThrowingAdapter(final DataServiceContext context) {
            super(context);
            throw new IllegalStateException("Test exception");
        }
    }

    static class AdapterWithMissingConstructor extends Adapter {

        AdapterWithMissingConstructor() {
            super(null);
        }
    }

    abstract static class AbstractAdapter extends Adapter {

        AbstractAdapter(final DataServiceContext context) {
            super(context);
        }
    }
}
