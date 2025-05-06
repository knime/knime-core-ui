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
 *   Apr 7, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;

/**
 * Renderer to input credentials.
 *
 * @author Paul Bärnreuther
 */
public interface CredentialsRendererSpec extends ControlRendererSpec {

    @Override
    default Optional<CredentialsRendererOptions> getOptions() {
        return Optional.empty();
    }

    @Override
    default Optional<String> getFormat() {
        return Optional.of(UiSchema.Format.CREDENTIALS);
    }

    /**
     * Options for rendering a credentials input.
     */
    interface CredentialsRendererOptions {

        default Optional<Boolean> getHideUsername() {
            return Optional.empty();
        }

        /**
         * Use instead of {@link #getHideUsername()} to dynamically show/hide the username field.
         *
         * @return the id of a state provider
         */
        default Optional<String> getHasUsernameProvider() {
            return Optional.empty();
        }

        default Optional<Boolean> getHidePassword() {
            return Optional.empty();
        }

        /**
         * Use instead of {@link #getHidePassword()} to dynamically show/hide the password field.
         *
         * @return the id of a state provider
         */
        default Optional<String> getHasPasswordProvider() {
            return Optional.empty();
        }

        default Optional<Boolean> getShowSecondFactor() {
            return Optional.empty();
        }

        default Optional<String> getUsernameLabel() {
            return Optional.empty();
        }

        default Optional<String> getPasswordLabel() {
            return Optional.empty();
        }

        default Optional<String> getSecondFactorLabel() {
            return Optional.empty();
        }

    }

    @Override
    default JsonDataType getDataType() {
        return JsonDataType.OBJECT;
    }

}
