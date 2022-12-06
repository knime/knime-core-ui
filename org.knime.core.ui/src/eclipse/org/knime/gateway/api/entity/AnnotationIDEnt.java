/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.gateway.api.entity;

import java.util.Objects;

import org.knime.core.node.workflow.WorkflowAnnotationID;

/**
 * Represents a (workflow) annotation id as used by gateway entities and services. Equivalent to the core's
 * {@link org.knime.core.node.workflow.WorkflowAnnotationID}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class AnnotationIDEnt {

    private NodeIDEnt m_nodeId;

    private int m_index;

    /**
     * Creates a new annotation id entity from a node id entity and an index.
     *
     * @param nodeId the node id of the workflow annotation is part of
     * @param index
     */
    public AnnotationIDEnt(final NodeIDEnt nodeId, final int index) {
        m_nodeId = nodeId;
        m_index = index;
    }

    /**
     * Creates a new annotation id entity from a {@link WorkflowAnnotationID}.
     *
     * @param id
     */
    public AnnotationIDEnt(final WorkflowAnnotationID id) {
        this(new NodeIDEnt(id.getNodeID()), id.getIndex());
    }

    /**
     * Deserialization constructor.
     *
     * @param s string representation as returned by {@link #toString()}
     */
    public AnnotationIDEnt(final String s) {
        String[] split = s.split("_");
        m_nodeId = new NodeIDEnt(split[0]);
        m_index = Integer.parseInt(split[1]);
    }

    @Override
    public String toString() {
        return m_nodeId.toString() + "_" + m_index;
    }

    /**
     * @return the id of the node the workflow annotation is part of
     */
    public NodeIDEnt getNodeIDEnt() {
        return m_nodeId;
    }

    /**
     * @return its index in that workflow
     */
    public int getIndex() {
        return m_index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_nodeId.hashCode(), m_index);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        AnnotationIDEnt ent = (AnnotationIDEnt)o;
        return Objects.equals(m_nodeId, ent.m_nodeId) && m_index == ent.m_index;
    }

}
