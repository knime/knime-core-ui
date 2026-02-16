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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.Location;

import com.fasterxml.jackson.core.JsonStreamContext;

/**
 * Shared utility for computing field locations from Jackson streaming contexts and managing the ThreadLocal
 * {@link Location} used for state provider dependency deserialization.
 *
 * @author Paul Bärnreuther
 */
public final class FieldLocationUtil {

    private static final ThreadLocal<Location> LOCATION = new ThreadLocal<>();

    private FieldLocationUtil() {
        // Cannot be instantiated
    }

    /**
     * In the rare situation that a credentials field or one of its parent fields is used as a dependency of a state
     * provider, we need to know the location of the source field in order to resolve the field ids.
     *
     * @param loc the location to set as current location
     */
    public static void setCurrentLocation(final Location loc) {
        LOCATION.set(loc);
    }

    /**
     * Remove the current location set with {@link #setCurrentLocation(Location)}
     */
    public static void removeCurrentLocation() {
        LOCATION.remove();
    }

    static Optional<List<String>> getSuppliedLocation() {
        return Optional.ofNullable(LOCATION.get()).map(Location::paths).map(FieldLocationUtil::ensureOnlyOnePath);
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

    /**
     * Compute a dot-joined field ID from a Jackson streaming context.
     *
     * @param context the Jackson streaming context (from generator or parser)
     * @return the dot-joined field path
     */
    public static String toFieldId(final JsonStreamContext context) {
        return String.join(".", getFieldLocation(context).toList());
    }

    /**
     * Recursively walk up the Jackson context tree to build the field location path.
     */
    static Stream<String> getFieldLocation(final JsonStreamContext context) {
        final var parent = context.getParent();
        if (parent == null) {
            return getSuppliedParentLocation();
        }
        Stream<String> parentFieldLocation;
        if (parent.inRoot() && context.getCurrentValue() != null) {
            parentFieldLocation = getSuppliedParentLocation();
        } else {
            parentFieldLocation = getFieldLocation(parent);
        }
        if (context.hasCurrentName()) {
            return Stream.concat(parentFieldLocation, Stream.of(context.getCurrentName()));
        }
        return parentFieldLocation;
    }

    private static Stream<String> getSuppliedParentLocation() {
        return getSuppliedLocation().stream().flatMap(List::stream);
    }

    /**
     * Get the {@link NodeID} from the current {@link NodeContext}.
     *
     * @return the node ID
     */
    public static NodeID getNodeId() {
        return NodeContext.getContext().getNodeContainer().getID();
    }
}
