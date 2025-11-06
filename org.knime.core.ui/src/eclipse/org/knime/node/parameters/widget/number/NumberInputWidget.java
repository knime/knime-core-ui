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
package org.knime.node.parameters.widget.number;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMaxValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMinValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.TypeDependentMaxValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.TypeDependentMinValidation;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

/**
 * Annotate a number setting with this in order to provide validation instructions.
 *
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface NumberInputWidget {

    /**
     * Add this field to define a step size for the number input.
     *
     * <h3>Default Value</h3>
     * <p>
     * If not specified (default -1), the frontend will use type-appropriate defaults:
     * <p>
     *
     * @return the step size for this number input, or -1 if not specified
     */
    double stepSize() default -1;

    /**
     * Add this field to define a minimum for the number input.
     *
     * <p>
     * If the min depends on the context of the node, use {@link #minValidationProvider()} instead.
     * </p>
     *
     * <h3>Default Value</h3>
     * <p>
     * For byte and int fields, the MIN_VALUE is used per default. For long fields, JavaScripts Number.MinSafeInteger is
     * used since larger values can not be exactly handled.
     * </p>
     *
     * @return the minimum validation for this number input
     */
    Class<? extends MinValidation> minValidation() default TypeDependentMinValidation.class;

    /**
     * Add this field to define a maximum for the number input.
     *
     * <p>
     * If the max depends on the context of the node, use {@link #maxValidationProvider()} instead.
     * </p>
     *
     * <h3>Default Value</h3>
     * <p>
     * For byte and int fields, the MAX_VALUE is used per default. For long fields, JavaScripts Number.MaxSafeInteger is
     * used since larger values can not be exactly handled.
     * </p>
     *
     * @return the maximum validation for this number input
     */
    Class<? extends MaxValidation> maxValidation() default TypeDependentMaxValidation.class;

    /**
     * The dynamic way to set the minimum value of the number input.
     *
     * @see #minValidation()
     * @return a dynamically provided min validation
     */
    Class<? extends StateProvider<? extends MinValidation>> minValidationProvider() default NoopMinValidationProvider.class; // NOSONAR

    /**
     * The dynamic way to set the maximum value of the number input.
     *
     * @see #maxValidation()
     * @return a dynamically provided max validation
     */
    Class<? extends StateProvider<? extends MaxValidation>> maxValidationProvider() default NoopMaxValidationProvider.class; // NOSONAR
}
