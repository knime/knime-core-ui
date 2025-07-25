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
 *   Jun 15, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.button;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DependencyHandler;

/**
 *
 * This annotation can be applied to a field of any serializable type, in order to display a button widget which, on
 * click, invokes an action specified by the given actionHandler. The returned value is set to the setting on a
 * successful response.
 *
 * @see org.knime.core.webui.node.dialog.defaultdialog.dataservice
 *
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ButtonWidget {

    /**
     * @return the action handler that is to be triggered on click. A successful result should be of the same type as
     *         the setting that is implemented. The second generic type of the {@link ButtonActionHandler} controls
     *         which other settings are available during the invocation. Without a {@link ButtonWidget#updateHandler}
     *         set, this does not mean that the buttons state gets changed when one of these settings is changed.
     *
     *         See {@link DependencyHandler} for further information on how to define these other settings.
     */
    Class<? extends ButtonActionHandler<?, ?, ?>> actionHandler(); //NOSONAR

    /**
     * @return a handler that controls which other settings trigger a reset of the button when they change (i.e. delete
     *         the saved value and enable the button again). See there for further information on how to use this. While
     *         the first and third generic type have to agree with the ones of {@link ButtonWidget#actionHandler}, the
     *         second one can, in principle, be different if the settings triggering an update and the settings used on
     *         invocation differ.
     *
     *         See {@link DependencyHandler} for further information on how to define these other settings.
     */
    Class<? extends ButtonUpdateHandler<?, ?, ?>> updateHandler() default NoopButtonUpdateHandler.class; //NOSONAR

    /**
     * @return if set to true, error messages are displayed besides the button.
     */
    boolean displayErrorMessage() default true;

    /**
     * @return if set to true, title and description will be shown above the ButtonWidget
     */
    boolean showTitleAndDescription() default true;

}
