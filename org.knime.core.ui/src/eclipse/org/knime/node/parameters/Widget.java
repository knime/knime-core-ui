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
 *   4 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * An annotation for a field indicating that its contributing to the dialog UI. And it allows one to control common
 * widget metadata of the field.
 *
 * Depending on the type of the field being annotated and in case there is <b> no</b>
 * {@link org.knime.core.webui.node.dialog.defaultdialog.widget other widget annotation} present, a default widget will
 * be displayed in the dialog (see {@link NodeParameters} for details). In case the default widget is not desired,
 * an additional specialized widget-annotation (e.g. {@link TextInputWidget}) can be used to customize it.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface Widget {

    /**
     * @return the title / label of the field. If empty, the widgets default name is used.
     * If the title should be empty in the dialog, use
     */
    String title();

    /**
     * @return the description of the field (for tooltips or node descriptions)
     */
    String description();

    /**
     * @return true if the annotated setting is advanced
     * @see Advanced
     */
    boolean advanced() default false;

    /**
     * Add an effect annotation here as an alternative to putting it on the annotated field directly. if an effect
     * annotation also exists on the field, an error is thrown.
     *
     * @return whether the widget should be disabled or hidden.
     * @see Effect
     *
     */
    Effect effect() default @Effect(predicate = EffectPredicateProvider.class, type = EffectType.SHOW);

}
