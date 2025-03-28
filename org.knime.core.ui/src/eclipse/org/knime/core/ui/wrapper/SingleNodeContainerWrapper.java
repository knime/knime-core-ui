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
 *   Oct 18, 2016 (hornm): created
 */
package org.knime.core.ui.wrapper;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.w3c.dom.Element;

/**
 * UI-interface implementation that wraps a {@link SingleNodeContainer}.
 *
 * @author Martin Horn, University of Konstanz
 * @param <W> the type of node container it wraps
 */
public abstract class SingleNodeContainerWrapper<W extends SingleNodeContainer> extends NodeContainerWrapper<W>
    implements SingleNodeContainerUI {

    /**
     * @param delegate the single node container to be wrapped
     */
    protected SingleNodeContainerWrapper(final W delegate) {
        super(delegate);
    }

    /**
     * Wraps the object via {@link Wrapper#wrapOrGet(Object, java.util.function.Function)}.
     *
     * @param snc the object to be wrapped
     * @return a new wrapper or a already existing one
     */
    public static final SingleNodeContainerWrapper<? extends SingleNodeContainer> wrap(final SingleNodeContainer snc) {
        if (snc instanceof NativeNodeContainer nnc) {
            return NativeNodeContainerWrapper.wrap(nnc);
        } else if (snc instanceof SubNodeContainer component) {
            return SubNodeContainerWrapper.wrap(component);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean isMemberOfScope() {
        return unwrap().isMemberOfScope();
    }

    @Override
    public boolean isInactive() {
        return unwrap().isInactive();
    }

    @Override
    public Element getXMLDescription() {
        return unwrap().getXMLDescription();
    }
}
