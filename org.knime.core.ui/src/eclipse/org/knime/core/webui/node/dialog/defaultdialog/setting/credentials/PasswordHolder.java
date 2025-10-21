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
 *   Oct 19, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;
import org.knime.node.parameters.widget.credentials.Credentials;

/**
 * Since we do not want to send passwords to the frontend when serializing {@link Credentials}, we need to store them
 * temporarily in order to receive passwords again when deserializing. When passwords are no longer needed, because the
 * associated node dialog has been deactivated, they should be removed from the map here again. This is why we also
 * remember the node id for every stored password.
 *
 * @author Paul Bärnreuther
 */
public final class PasswordHolder {

    private static final ThreadLocal<Location> location = new ThreadLocal<>();

    private PasswordHolder() {
        // Cannot be instantiated
    }

    private static final Map<String, String> PASSWORDS = new HashMap<>();

    private static final Map<NodeID, Set<String>> PASSWORD_IDS_PER_NODE_ID = new HashMap<>();

    static synchronized void addPassword(final NodeID nodeID, final String passwordId, final String password) {
        final var combinedId = combineNodeIdAndPasswordId(nodeID, passwordId);
        accociatePasswordIdToNode(combinedId, nodeID);
        PASSWORDS.put(combinedId, password);
    }

    private static String combineNodeIdAndPasswordId(final NodeID nodeId, final String passwordId) {
        return String.format("%s:%s", nodeId, passwordId);
    }

    static synchronized String get(final NodeID nodeID, final String passwordId) {
        final var combinedId = combineNodeIdAndPasswordId(nodeID, passwordId);
        return PASSWORDS.get(combinedId);
    }

    private static void accociatePasswordIdToNode(final String passwordId, final NodeID nodeId) {
        PASSWORD_IDS_PER_NODE_ID.compute(nodeId, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(passwordId);
            return v;
        });
    }

    /**
     * Tear down method to remove all passwords from the global map when they are not necessary anymore.
     *
     * @param nodeId the id of node container associated to the dialog.
     */
    public static synchronized void removeAllPasswordsOfDialog(final NodeID nodeId) {
        final var passwordIds = PASSWORD_IDS_PER_NODE_ID.remove(nodeId);
        if (passwordIds == null) {
            return;
        }
        passwordIds.stream().forEach(PasswordHolder::remove);
    }

    private static void remove(final String combinedId) {
        PASSWORDS.remove(combinedId);
    }

    /**
     * In the rare situation that a credentials field or one of its parent fields is used as a dependency of a state
     * provider, we need to know the location of the source field in order to resolve the password ids.
     *
     * @param loc the location to set as current location
     */
    public static void setCurrentLocation(final Location loc) {
        location.set(loc);
    }

    /**
     * Remove the current location set with {@link #setCurrentLocation(Location)}
     */
    public static void removeCurrentLocation() {
        location.remove();
    }

    static Optional<List<String>> getSuppliedLocation() {
        return Optional.ofNullable(location.get()).map(Location::paths).map(PasswordHolder::ensureOnlyOnePath);
    }

    static List<String> ensureOnlyOnePath(final List<List<String>> paths) {
        if (paths.isEmpty()) {
            throw new IllegalStateException("Location without any paths");
        }
        if (paths.size() > 1) {
            throw new UnsupportedOperationException("Updates on credentials within arrays are not possible.");
        }
        return paths.get(0);
    }

}
