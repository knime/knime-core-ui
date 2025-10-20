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
 *   Oct 13, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.dataservice.validation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.ConvertValueUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.node.parameters.widget.credentials.Credentials;

/**
 * An instance of this class manages the custom validation callbacks for the DefaultNodeDialog. It provides
 * functionality for registering validation callbacks with unique IDs and invoking them when validation is requested
 * from the frontend.
 *
 * @author Paul Bärnreuther
 */
public final class CustomValidationContext {

    final Map<String, ValidationCallback<?>> m_validators = new ConcurrentHashMap<>();

    /**
     * Registers a custom validator using a ValidationCallback and returns a unique UUID as the validatorId.
     *
     * @param validationCallback the callback to invoke for validation
     * @return a unique UUID that can be used as validatorId
     */
    public String registerValidator(final ValidationCallback<?> validationCallback) {

        final var validatorId = UUID.randomUUID().toString();
        m_validators.put(validatorId, validationCallback);

        return validatorId;
    }

    /**
     * Invokes the validator with the given ID.
     *
     * @param validatorId the unique ID of the validator to invoke
     * @param currentValue the current value to validate
     * @return the error message if validation fails, or null if validation succeeds
     * @throws IllegalArgumentException if no validator is found for the given ID
     */
    public String validate(final String validatorId, final Object currentValue) {
        final var validator = m_validators.get(validatorId);
        if (validator == null) {
            throw new IllegalArgumentException(
                String.format("No validator found for id %s. Most likely an implementation error.", validatorId));
        }
        final var type = GenericTypeFinderUtil.getFirstGenericType(validator.getClass(), ValidationCallback.class);
        if (Credentials.class.equals(type)) {
            throw new UnsupportedOperationException(
                "Validation of Credentials is not yet supported due to security reasons.");
        }
        final var currentConvertedValue = ConvertValueUtil.convertValue(currentValue, type, null, null);
        return validate(validator, currentConvertedValue);
    }

    private static String validate(final ValidationCallback<?> callback, final Object currentValue) {
        try {
            callback.castAndValidate(currentValue);
            return null; // Validation succeeded
        } catch (InvalidSettingsException ex) {
            return ex.getMessage(); // Validation failed
        }
    }

    /**
     * Clears all registered validators.
     */
    public void clear() {
        m_validators.clear();
    }
}
