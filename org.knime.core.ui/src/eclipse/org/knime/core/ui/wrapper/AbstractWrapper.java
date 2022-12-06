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
 *   Sep 2, 2017 (hornm): created
 */
package org.knime.core.ui.wrapper;

import org.knime.core.ui.UI;

/**
 * Abstract implementation of the {@link Wrapper} interface that keeps the wrapped object as local member.
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class AbstractWrapper<W> implements Wrapper<W> {

    private final W m_wrappedObj;

    /**
     * @param wrappedObj object to be wrapped. This very same object is returned at the {@link #unwrap()} method.
     */
    protected AbstractWrapper(final W wrappedObj) {
        m_wrappedObj = wrappedObj;
    }

    /** {@inheritDoc} */
    @Override
    public final W unwrap() {
        return m_wrappedObj;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (Wrapper.wraps(obj, m_wrappedObj.getClass())) {
            //if the passed object wraps the same type, compare the wrapped objects directly
            return unwrap().equals(Wrapper.unwrap((UI)obj, m_wrappedObj.getClass()));
        } else {
            //the passed object obviously wraps a different object type
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return unwrap().hashCode();
    }

    @Override
    public final String toString() {
        return unwrap().toString();
    }

}
