/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on May 23, 2025 by hornm
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation.internal;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation;

/**
 * Validator for workflow input/output (cp. {@link InputNode}/{@link OutputNode}) parameter names in dialogs (see also
 * {@link DialogNode#PARAMETER_NAME_PATTERN}).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class WorkflowIOParameterNameValidation extends PatternValidation {

    private static final Predicate<String> PARAMETER_IDENTIFIER_IS_VALID = //
        ((Predicate<String>)Objects::nonNull)//
            .and(Predicate.not(StringUtils::isBlank))//
            .and(DialogNode.PARAMETER_NAME_PATTERN.asMatchPredicate());

    private static final UnaryOperator<String> PARAMETER_IDENTIFIER_ERROR_MESSAGE = parameterName -> String.format("""
            Invalid parameter name%s.
            Valid parameter names consist of one or several strings, separated by dashes or underscores.
            Parameter names must not end with a digit.
            For instance, input1 is not a valid parameter name, but input1-table is.
            """, parameterName);

    /**
     * @param parameterName the name of the workflow input or output parameter to check
     * @return the unchanged parameter name
     * @throws InvalidSettingsException if the parameter name is invalid
     */
    public static String validateParameterName(final String parameterName) throws InvalidSettingsException {
        CheckUtils.checkSetting(PARAMETER_IDENTIFIER_IS_VALID.test(parameterName),
            PARAMETER_IDENTIFIER_ERROR_MESSAGE.apply(" \"" + parameterName + "\""));
        return parameterName;
    }

    @Override
    protected String getPattern() {
        return DialogNode.PARAMETER_NAME_PATTERN.pattern();
    }

    @Override
    public String getErrorMessage() {
        return PARAMETER_IDENTIFIER_ERROR_MESSAGE.apply("");
    }

}
