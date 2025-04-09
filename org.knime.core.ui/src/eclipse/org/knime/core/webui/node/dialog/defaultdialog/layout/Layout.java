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
 *   Mar 21, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.layout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation for controlling which part of a layout a given field should be.
 *
 * <h2>Setting the default for multiple settings</h2>
 * <p>
 * The annotation can also target a class. This behaves as follows:
 * </p>
 * <ul>
 * <li>The annotation on a class is equivalent to the same annotation on all of its fields</li>
 * <li>Every additional annotation on a field overrides the class annotation for that field</li>
 * <li>For nested settings, an error is thrown if there are annotations on the enclosing field and on its class</li>
 * </ul>
 * <h2>Layout parts</h2>
 * <p>
 * The annotation may target any of the following classes representing layout parts:
 * </p>
 *
 * <ul>
 * <li>An interface annotated with {@link Section @Section}.</li>
 * <li>An interface annotated with {@link HorizontalLayout @HorizontalLayout}.</li>
 * <li>An interface without any annotations which serves only as a placeholder to position settings or other layout
 * parts within the layout.</li>
 * </ul>
 * <h2>Ordering</h2>
 * <ul>
 * <li>Without any given order specific annotations on layout parts, they will be ordered alphabetically with respect to
 * their class name.</li>
 * <li>It is possible to adjust this order using {@link After} and {@link Before} annotations.</li>
 * <li>All layout parts referenced via @Layout annotations used throughout the settings of a single node dialog must
 * share a common root, since the order would be unclear if multiple roots are present. However it is possible to inject
 * a layout inside another one by using {@link Before} or {@link After} annotations defining the order. For example, one
 * can use a common layout for multiple nodes and for one specific node insert a new section between two other
 * sections.</li>
 * </ul>
 *
 * <h2>Array Layout</h2>
 * <p>
 * The class of the elements of an arrays are treated as self-contained settings with their own layouting. In
 * particular, the root layout interface should not be nested inside any layout interfaces from the outside layout. For
 * example, if the array layout field is part of a section and fields inside its elements point to nested layout parts
 * of that section, the section is displayed <u><b>inside each of the elements in addition to outside of it</b></u>.
 * </p>
 *
 *
 * <h2>Widget Groups</h2> Using a widget group to encapsulate multiple settings into one field is recommended for better
 * organization of the code and for deduplication in case the same set of settings is needed in multiple places. There
 * are two options for defining a layout when it comes to widget groups:
 * <ul>
 * <li>The layout within the widget group is "self-contained" (see what this means below), which means that the widget
 * group will appear as one block in the outer layout.</li>
 * <li>It is not "self-contained" and the widgets are arranged in the same way as if they were not in a widget group.
 * This means that, e.g. when such a widget group defines a horizontal layout, and this widget group is used twice in
 * the settings, only one horizontal layout with settings from both widget groups will be the result.</li>
 * </ul>
 * A widget group is considered "self-contained" if it itself can be chosen as the root of the layout (as defined
 * above). I.e. either none of the widgets have a {@link Layout} annotation or all used {@link Layout Layouts} are
 * nested types inside the widget group class (or one of its super-classes). Additionally, a widget group loses its
 * self-contained property if the widget group class is annotated with a {@link Inside} an {@link After} or a
 * {@link Before} annotation itself.
 *
 * @see Before
 * @see After
 *
 * @author Paul Bärnreuther
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Layout {

    /**
     * @return the targeted layout part
     */
    Class<?> value();
}
