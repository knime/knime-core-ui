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
 *   May 5, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation;

/**
 * Annotate a {@link String} setting with this in order to provide settings and validation instructions.
 *
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface TextInputWidget {

    /**
     * With this field set to true, the input also has a checkbox which indicates whether the value is null. If so, the
     * input is hidden.
     *
     * @return whether the input should be hidden in case the value is null.
     */
    boolean optional() default false;

    String placeholder() default "";

    Class<? extends StateProvider<String>> placeholderProvider() default NoopStringProvider.class;

    /**
     * Add this field to define validation instructions for the text input. Validation instructions specified should be
     * constant, i.e. the value to validate against does not depend on the context of the node. To validate against a
     * dynamic value which depends on the context of the node use {@link #validationProvider()}. See
     * {@link TextInputWidgetValidation} for possible validation instructions such as validating the input against a
     * pattern ({@link PatternValidation}).
     *
     * @return the validations to apply to the input
     */
    Class<? extends TextInputWidgetValidation>[] validation() default {};

    /**
     * Add this field to define validation instructions for the text input. Validation instructions specified should be
     * dynamic, i.e. the values to validate against depend on the context of the node. To validate against a constant
     * value which does not depend on the context of the node use {@link #validation()}. See
     * {@link TextInputWidgetValidation} for possible validation instructions such as validating the input against a
     * pattern ({@link PatternValidation}).
     *
     * @return the providers specifying validations to apply to the input that depend on the context of the node
     */
    Class<? extends StateProvider<? extends TextInputWidgetValidation>>[] validationProvider() default {};
}
