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
 *   Feb 9, 2023 (benjamin): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;

/**
 * Contains unit tests for the {@link ConfigKeyUtil}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
class ConfigKeyUtilTest {

    private static class Settings implements Persistable {

        @Widget(title = "", description = "")
        int setting0;

        @Widget(title = "", description = "")
        int m_setting1;

        @Persist
        @Widget(title = "", description = "")
        int setting2;

        @Persist(configKey = "foo")
        @Widget(title = "", description = "")
        int setting3;

        @Persist(hidden = true)
        @Widget(title = "", description = "")
        int setting4;

    }

    @Test
    void testConfigKeyFromFieldName() throws NoSuchFieldException, SecurityException {
        assertEquals("setting0", configKeyFor("setting0"), "should use field name for config key");
    }

    @Test
    void testConfigKeyFromFieldNameRemovePrefix() throws NoSuchFieldException, SecurityException {
        assertEquals("setting1", configKeyFor("setting1"), "should remove 'm_' prefix from field name for config key");
    }

    @Test
    void testConfigKeyFromFieldNameWithPersist() throws NoSuchFieldException, SecurityException {
        assertEquals("setting2", configKeyFor("setting2"), "should use field name for config key");
    }

    @Test
    void testConfigKeyFromPersistAnnotation() throws NoSuchFieldException, SecurityException {
        assertEquals("foo", configKeyFor("setting3"), "should extract custom configKey from annotation");
    }

    @Test
    void testConfigKeyForHidden() throws NoSuchFieldException, SecurityException {
        assertEquals("setting4_Internals", configKeyFor("setting4"),
            "should append '_Internals' to hidden settings config keys");
    }

    private static String configKeyFor(final String fieldName) throws NoSuchFieldException {
        return ConfigKeyUtil.getConfigKey(getField(fieldName));
    }

    private static Tree<Persistable> SETTINGS_TREE = new PersistTreeFactory().createTree(Settings.class);

    private static TreeNode<Persistable> getField(final String fieldName) throws NoSuchFieldException {
        return SETTINGS_TREE.getChildByName(fieldName);
    }
}
