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
package org.knime.core.webui.node.dialog.defaultdialog.widget.updates;

import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author Paul Bärnreuther
 * @param <OUTPUT> The type of the output value of this action
 */
public interface Action<OUTPUT> {

    /**
     * This interface defined the methods with which an action can be bound to user actions. One can define
     * <ul>
     * <li><b>Triggers</b>: The user actions leading to an invocation</li>
     * <li><b>Dependencies</b>: The fields on whose values the output of the action depends on.</li>
     * </ul>
     */
    interface ActionInitializer {

        /**
         * Sets <b>Trigger</b> and <b>Dependency</b>, i.e.:
         *
         * Refer to a {@link Widget} with this method to trigger the action on a change of that setting while also
         * depending on the new value.
         *
         * @param <T> the type of the dependency
         * @param id used for {@link Widget#id} of a field
         * @return a supplier to be used during {@link #compute}. If the returned supplier is not needed, use
         *         {@link #setOnChangeTrigger} instead.
         */
        <T> Supplier<T> dependOnChangedValue(Class<? extends ValueId<T>> id);

        /**
         * Sets <b>Dependency</b> and not <b>Trigger</b>, i.e.:
         *
         * Refer to a {@link Widget} with this method to depend on its value without triggering this action on a change
         * of it.
         *
         * @param <T> the type of the dependency
         * @param id used for {@link Widget#id} of a field
         * @return a supplier to be used during {@link #compute}.
         */
        <T> Supplier<T> dependOnValueWhichIsNotATrigger(Class<? extends ValueId<T>> id);

        /**
         * Sets <b>Trigger</b> and not <b>Dependency</b>, i.e.:
         *
         * Refer to a {@link Widget} with this method to trigger the action on a change of that setting. If the action
         * should also depend on the value of the triggering settings, use {@link #dependOnChangedValue} instead.
         *
         * @param <T> the type of the dependency
         * @param id
         */
        <T> void setOnChangeTrigger(Class<? extends ValueId<T>> id);

        /**
         * Refer to another {@link Action} with this method to trigger this action after the other action has been
         * triggered and depend on its output.
         *
         * @param <T> the type of the output of the other action
         * @param actionClass the class of the other action
         * @return a supplier to be used during {@link #compute}
         */
        <T> Supplier<T> continueOtherAction(Class<? extends Action<T>> actionClass);

    }

    /**
     * This method is called when the dialog is opened.
     *
     * @param initializer providing configuration methods to define triggers and dependencies of the method. This
     *            instance must not be used beyond the scope of this method. Any further call to one of its methods
     *            after the invocation of this method will result in a runtime exception.
     */
    void init(ActionInitializer initializer);

    /**
     * @return the result of this action. It is either transformed directly to a specific update in the dialog or as
     *         input for another {@link Action}.
     */
    OUTPUT compute();

}
