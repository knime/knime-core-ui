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

import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * A setting that represents a particular table from a database chosen by the user.
 *
 * Note that although this class implements {@link WidgetGroup}, it has its own renderer in the frontend which also
 * includes a browser to select a table and schema.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DBTableSelection implements PersistableSettings, WidgetGroup {

    /**
     * The (optional) name of the schema to use. If blank, the default schema is used.
     */
    @Widget(title = "Schema name", description = """
            The name of the schema to use. If blank, the default schema is \
            used.
            """)
    public String m_schemaName;

    /**
     * The name of the table to use.
     */
    @Widget(title = "Table name", description = "The name of the table to use.")
    public String m_tableName;

    /**
     * Only for deserialization.
     */
    DBTableSelection() {
        this("", "");
    }

    /**
     * Constructor.
     *
     * @param schemaName the name of the schema to use, not null.
     * @param tableName the name of the table to use, not null.
     */
    public DBTableSelection(final String schemaName, final String tableName) {
        m_schemaName = schemaName;
        m_tableName = tableName;
    }
}
