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
 *   Aug 5, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.tree;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.Function;

import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persistable;

import com.fasterxml.jackson.databind.JavaType;

/**
 *
 * A node representing a final leaf of the {@link Tree}, i.e. it corresponds to a field within a
 * {@link Persistable} and its type is neither another {@link Persistable} nor an array of such.
 *
 * @param <S> the type of the [S]ettings. Either {@link Persistable} or {@link WidgetGroup}
 * @author Paul Bärnreuther
 */
public final class LeafNode<S> extends TreeNode<S> {

    private final Class<?> m_contentType;

    LeafNode(final Tree<S> parent, final JavaType type, final Class<?> contentType,
        final Function<Class<? extends Annotation>, Annotation> annotations,
        final Collection<Class<? extends Annotation>> possibleAnnotations, final Field underlyingField) {
        super(parent, parent.getSettingsType(), type, annotations, possibleAnnotations, underlyingField);
        m_contentType = contentType;
    }

    /**
     * @return the contentType if the type is an array/collection type or null if not
     */
    public Class<?> getContentType() {
        return m_contentType;
    }

    /**
     * Use this method to access the raw class of the parent {@link Tree} of this leaf node.
     *
     * @return the type of the containing parent of this node.
     */
    public Class<? extends S> getParentType() {
        return getParent().getRawClass();
    }

}
