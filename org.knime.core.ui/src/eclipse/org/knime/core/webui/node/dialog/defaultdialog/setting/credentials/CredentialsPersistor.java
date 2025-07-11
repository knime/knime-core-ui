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
 *   Jul 11, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import static org.knime.core.node.workflow.VariableType.CredentialsType.CFG_IS_CREDENTIALS_FLAG;
import static org.knime.core.node.workflow.VariableType.CredentialsType.CFG_LOGIN;
import static org.knime.core.node.workflow.VariableType.CredentialsType.CFG_NAME;
import static org.knime.core.node.workflow.VariableType.CredentialsType.CFG_TRANSIENT_PASSWORD;
import static org.knime.core.node.workflow.VariableType.CredentialsType.CFG_TRANSIENT_SECOND_FACTOR;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.OptionalContentPersistor;
import org.knime.node.parameters.parameter.credentials.Credentials;
import org.knime.node.parameters.persistence.NodeSettingsPersistor;

/**
 * A {@link NodeSettingsPersistor} for {@link Credentials} objects.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class CredentialsPersistor implements OptionalContentPersistor<Credentials> {

    private static final String CFG_PASSWORD = "weaklyEncryptedPassword";

    private static final String CFG_SECOND_FACTOR = "weaklyEncryptedSecondFactor";

    private static final String PASSWORD_SECRET = "XKdPobvbDEBZEJmBsbMq"; // NOSONAR (weak symmetric encryption)

    private static final String SECOND_FACTOR_SECRET = "lLNIScQYgDJJXUrUdhSG";

    private final String m_configKey;

    /**
     * @param configKey
     */
    public CredentialsPersistor(final String configKey) {
        m_configKey = configKey;
    }

    @Override
    public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var credentialsConfig = settings.getNodeSettings(m_configKey);
        final var username = credentialsConfig.getString(CFG_LOGIN);
        final var password = credentialsConfig.containsKey(CFG_TRANSIENT_PASSWORD)
            ? credentialsConfig.getTransientString(CFG_TRANSIENT_PASSWORD) // overwritten via variable
            : credentialsConfig.getPassword(CFG_PASSWORD, PASSWORD_SECRET, "");

        final var secondFactor = credentialsConfig.containsKey(CFG_TRANSIENT_SECOND_FACTOR)
            ? credentialsConfig.getTransientString(CFG_TRANSIENT_SECOND_FACTOR) // overwritten via variable
            : credentialsConfig.getPassword(CFG_SECOND_FACTOR, SECOND_FACTOR_SECRET, "");
        return new Credentials(username, password, secondFactor);
    }

    @Override
    public void save(final Credentials credentials, final NodeSettingsWO settings) {
        final var credentialsConfig = settings.addNodeSettings(m_configKey);
        // only to comply to schema for variable type detection (value doesn't matter)
        credentialsConfig.addBoolean(CFG_IS_CREDENTIALS_FLAG, true);

        credentialsConfig.addString(CFG_NAME, "");
        if (credentials != null) {
            persistCredentials(credentials, credentialsConfig);
        } else {
            persistCredentials(new Credentials(), credentialsConfig);
        }
    }

    private static void persistCredentials(final Credentials credentials, final NodeSettingsWO credentialsConfig) {
        credentialsConfig.addString(CFG_LOGIN, credentials.getUsername());
        final var password = credentials.getPassword();
        final var secondFactor = credentials.getSecondFactor();
        // when exposing variable
        credentialsConfig.addTransientString(CFG_TRANSIENT_PASSWORD, password);
        credentialsConfig.addTransientString(CFG_TRANSIENT_SECOND_FACTOR, secondFactor);
        if (password != null && !password.isEmpty()) {
            credentialsConfig.addPassword(CFG_PASSWORD, PASSWORD_SECRET, password);
        }
        if (secondFactor != null && !secondFactor.isEmpty()) {
            credentialsConfig.addPassword(CFG_SECOND_FACTOR, SECOND_FACTOR_SECRET, secondFactor);
        }
    }

    @Override
    public void saveEmpty(final NodeSettingsWO settings) {
        final var emptyCredentials = new Credentials();
        save(emptyCredentials, settings);
    }
}