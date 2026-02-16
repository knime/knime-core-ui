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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.workflow.NodeID;

/**
 * Holds arbitrary objects server-side during dialog serialization, analogous to how
 * {@link org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.PasswordHolder} holds passwords. Objects
 * stored here are not sent to the frontend and can be retrieved during deserialization. When the associated node dialog
 * is deactivated, all objects for that node should be removed.
 *
 * @author Paul Bärnreuther
 */
public final class CustomObjectHolder {

    private CustomObjectHolder() {
        // Cannot be instantiated
    }

    private static final Map<String, Object> OBJECTS = new HashMap<>();

    private static final Map<NodeID, Set<String>> OBJECT_IDS_PER_NODE_ID = new HashMap<>();

    /**
     * Store an object for the given node and object ID.
     *
     * @param nodeID the node ID
     * @param objectId the object identifier (typically the field path)
     * @param object the object to store
     */
    public static synchronized void addObject(final NodeID nodeID, final String objectId, final Object object) {
        final var combinedId = combineNodeIdAndObjectId(nodeID, objectId);
        associateObjectIdToNode(combinedId, nodeID);
        OBJECTS.put(combinedId, object);
    }

    private static String combineNodeIdAndObjectId(final NodeID nodeId, final String objectId) {
        return String.format("%s:%s", nodeId, objectId);
    }

    /**
     * Retrieve a previously stored object.
     *
     * @param nodeID the node ID
     * @param objectId the object identifier
     * @return the stored object, or {@code null} if not found
     */
    public static synchronized Object get(final NodeID nodeID, final String objectId) {
        final var combinedId = combineNodeIdAndObjectId(nodeID, objectId);
        return OBJECTS.get(combinedId);
    }

    private static void associateObjectIdToNode(final String objectId, final NodeID nodeId) {
        OBJECT_IDS_PER_NODE_ID.compute(nodeId, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(objectId);
            return v;
        });
    }

    /**
     * Remove all objects associated with the given node dialog.
     *
     * @param nodeId the id of node container associated to the dialog.
     */
    public static synchronized void removeAllObjectsOfDialog(final NodeID nodeId) {
        final var objectIds = OBJECT_IDS_PER_NODE_ID.remove(nodeId);
        if (objectIds == null) {
            return;
        }
        objectIds.forEach(OBJECTS::remove);
    }
}
