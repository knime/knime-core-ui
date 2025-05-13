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
 *   Feb 6, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util.updates;

import static org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil.createInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
final class StateVertex extends Vertex {

    private final Supplier<StateProvider> m_stateProviderSupplier; //NOSONAR, generic type is unknown here

    private final Object m_identifier;

    private final Map<Object, DependencyVertex> m_dependencies = new HashMap<>();

    /**
     * @param identifier an object whose {@link Object#equals} method is used to identify the state provider
     * @param stateProviderSupplier the supplier of the state provider
     */
    public StateVertex(final Object identifier, final Supplier<StateProvider> stateProviderSupplier) {
        m_identifier = identifier;
        m_stateProviderSupplier = stateProviderSupplier;
    }

    public StateVertex(final Class<? extends StateProvider> stateProviderClass) {
        this(stateProviderClass, () -> createInstance(stateProviderClass));
    }

    @Override
    public <T> T visit(final VertexVisitor<T> visitor) {
        return visitor.accept(this);
    }

    void addDependency(final Object referenceKey, final DependencyVertex dependencyVertex) {
        m_dependencies.put(referenceKey, dependencyVertex);
    }

    DependencyVertex getDependency(final Object referenceKey) {
        return m_dependencies.get(referenceKey);
    }

    public StateProvider createStateProvider() { //NOSONAR, generic type is unknown here
        return m_stateProviderSupplier.get();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof final StateVertex other) {
            return hasIdentifier(other.m_identifier);
        }
        return false;
    }

    boolean hasIdentifier(final Object identifier) {
        return m_identifier.equals(identifier);
    }

    @Override
    public int hashCode() {
        return m_identifier.hashCode();
    }

    @Override
    public String toString() {
        return m_identifier.toString();
    }

}
