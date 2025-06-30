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
 *   May 8, 2023 (benjamin): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.knime.core.webui.node.dialog.defaultdialog.examples.ArrayWidgetExample;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

/**
 * An annotation to set the button text and element title of array or collection settings.
 *
 * See {@link ArrayWidgetExample} for an example on how to use the annotation.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ArrayWidget {

    /**
     * @return the label of the add button which adds new elements to the settings
     */
    String addButtonText() default "";

    /**
     * Choose between a standard card style with a header or a more compact single line style.
     *
     * @return the style in which the elements should be displayed in the array widget.
     */
    ElementLayout elementLayout() default ElementLayout.VERTICAL_CARD;

    /**
     * This title is used when {@link #elementLayout()} is set to {@link ElementLayout#VERTICAL_CARD} as a header within
     * each card. The header adds the index of the element, i.e. setting this field to "Column" would result in "Column
     * 1", "Column 2", etc. being shown as the header of each card.
     *
     * If {@link #elementLayout()} is set to {@link ElementLayout#HORIZONTAL_SINGLE_LINE}, this title is ignored.
     *
     * When not setting this field, the element title will be inferred from the array layout field name by removing the
     * plural and resolving camelCase to separate words.
     *
     * @return a title that is shown above each element of the array
     */
    String elementTitle() default "";

    /**
     * @return whether sort buttons should be shown that allow to change the order of the array elements
     */
    boolean showSortButtons() default false;

    /**
     * @return whether add and delete buttons should be hidden such that the size of the array cannot be changed
     */
    boolean hasFixedSize() default false;

    /**
     * @return a {@link StateProvider} that determines the default value when adding a new element to the array. The
     *         {@link StateProvider} must provide a value of the element's type. If this attribute is not set, the
     *         default constructor of the element's type will be called.
     */
    Class<? extends StateProvider> elementDefaultValueProvider() default StateProvider.class; // NOSONAR

    /**
     * An enum to define the layout of the elements in the array widget.
     */
    enum ElementLayout {
            /**
             * Elements are styled as a card and displayed vertically. Each element has a header showing the element
             * title and controls are positioned on the right side of that header.
             *
             * Note that controls can still be wrapped in a horizontal layout and only top-level blocks are displayed
             * vertically.
             */
            VERTICAL_CARD,

            /**
             * A more compact version that shows only one line per element of the array. Elements are shown in a
             * horizontal layout without a header showing the element title. Controls are positioned on the right of
             * this line. I.e. in particular, there is no need to wrap the elements in a horizontal layout additionally.
             */
            HORIZONTAL_SINGLE_LINE;

    }
}
