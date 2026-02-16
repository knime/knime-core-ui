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
 *   Feb 16, 2026 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.holder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.NodeID;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class CustomObjectHolderTest {

    private static final NodeID NODE_ID_1 = new NodeID(1);

    private static final NodeID NODE_ID_2 = new NodeID(2);

    @AfterEach
    void cleanup() {
        CustomObjectHolder.removeAllObjectsOfDialog(NODE_ID_1);
        CustomObjectHolder.removeAllObjectsOfDialog(NODE_ID_2);
    }

    @Test
    void testAddAndGet() {
        final var obj = new Object();
        CustomObjectHolder.addObject(NODE_ID_1, "field", obj);
        assertThat(CustomObjectHolder.get(NODE_ID_1, "field")).isSameAs(obj);
    }

    @Test
    void testGetReturnsNullForUnknownKey() {
        assertThat(CustomObjectHolder.get(NODE_ID_1, "nonexistent")).isNull();
    }

    @Test
    void testGetReturnsNullForWrongNodeId() {
        CustomObjectHolder.addObject(NODE_ID_1, "field", "value");
        assertThat(CustomObjectHolder.get(NODE_ID_2, "field")).isNull();
    }

    @Test
    void testRemoveAllObjectsOfDialog() {
        CustomObjectHolder.addObject(NODE_ID_1, "a", "objA");
        CustomObjectHolder.addObject(NODE_ID_1, "b", "objB");
        CustomObjectHolder.addObject(NODE_ID_2, "a", "objC");

        CustomObjectHolder.removeAllObjectsOfDialog(NODE_ID_1);

        assertThat(CustomObjectHolder.get(NODE_ID_1, "a")).isNull();
        assertThat(CustomObjectHolder.get(NODE_ID_1, "b")).isNull();
        assertThat(CustomObjectHolder.get(NODE_ID_2, "a")).isEqualTo("objC");
    }

    @Test
    void testRemoveAllObjectsOfDialogWithNoObjects() {
        // should not throw
        CustomObjectHolder.removeAllObjectsOfDialog(NODE_ID_1);
    }

    @Test
    void testOverwriteExistingObject() {
        CustomObjectHolder.addObject(NODE_ID_1, "field", "first");
        CustomObjectHolder.addObject(NODE_ID_1, "field", "second");
        assertThat(CustomObjectHolder.get(NODE_ID_1, "field")).isEqualTo("second");
    }

    @Test
    void testMultipleNodeIdsIndependent() {
        CustomObjectHolder.addObject(NODE_ID_1, "field", "obj1");
        CustomObjectHolder.addObject(NODE_ID_2, "field", "obj2");

        assertThat(CustomObjectHolder.get(NODE_ID_1, "field")).isEqualTo("obj1");
        assertThat(CustomObjectHolder.get(NODE_ID_2, "field")).isEqualTo("obj2");
    }
}
