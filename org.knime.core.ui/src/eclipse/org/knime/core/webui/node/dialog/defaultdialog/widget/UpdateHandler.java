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
 *   Jul 10, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DependencyHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.widget.choices.ChoicesProvider;

/**
 * This interface is used for defining any kind of update of settings values or context information (e.g. possible
 * values in a {@link ChoicesProvider} whenever one setting depends on at least one other setting. The update handler is
 * linked to the target of such a dependency by annotations.
 *
 * @param <R> the return type of the update method. This is the information that is transmitted to the frontend on an
 *            update, i.e. its shape depends on what this update handler is used for there.
 * @param <S> the settings, the targeted setting depends on (see {@link DependencyHandler}).
 *
 * @author Paul Bärnreuther
 */
public interface UpdateHandler<R, S> extends DependencyHandler<S> {
    /**
     * This method is called when one of the dependency settings defined by {@code S} changes in order to determine the
     * immediate effect.
     *
     * @param settings the dependency settings on update
     * @param context the current {@link NodeParametersInput}
     *
     * @return result defining state changes in the fronted.
     * @throws WidgetHandlerException if the request should fail providing the error message to the frontend
     */
    R update(S settings, NodeParametersInput context) throws WidgetHandlerException;

    @SuppressWarnings({"javadoc"})
    default R castAndUpdate(final Object settings, final NodeParametersInput context)
        throws WidgetHandlerException {
        return update(castToDependencies(settings), context);
    }
}
