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
 *   Dec 1, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory;
import org.knime.filehandling.core.connections.base.auth.AuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.AuthType;
import org.knime.filehandling.core.connections.base.auth.IDWithSecretAuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.widget.credentials.Credentials;

/**
 * {@link AuthProviderSettings} implementation for {@link LegacyCredentials}. Use this to replace the legacy
 * {@link IDWithSecretAuthProviderSettings} in new nodes that use the WebUI dialog by using the
 * {@link LegacyCredentials} in the parameters as explained as follows:
 *
 * <pre>
 * &#64;Widget(title = "Username & Password", description = "Username and password authentication")
 * &#64;Migration(FromUserPasswordAuthProviderSettingsMigration.class)
 * &#64;Persist(configKey = "credentials") // Same key as used in for the new auth type
 * LegacyCredentials m_cred = new LegacyCredentials();
 *
 * static final class FromUserPasswordAuthProviderSettingsMigration
 *     extends LegacyCredentialsAuthProviderSettings.FromUserPasswordAuthProviderSettingsMigration {
 *     protected FromUserPasswordAuthProviderSettingsMigration(final IDWithSecretAuthProviderSettings legacySettings) {
 *         super(new UserPasswordAuthProviderSettings(StandardAuthTypes.USER_PASSWORD, true));
 *     }
 * }
 * </pre>
 *
 * Additionally, you'll have to register an instance these new auth provider settings in the model and use it wherever
 * the settings you migrated from were being used (in addition, not replacing them). The field controlling the auth type
 * in the parameters should load the key of the new auth type whenever the key of the replaced one is found.
 *
 * @author Paul Baernreuther, KNIME GmbH
 * @noreference non-public API
 */
public class LegacyCredentialsAuthProviderSettings implements AuthProviderSettings {

    private final AuthType m_authType;

    private final boolean m_allowBlankSecret;

    private LegacyCredentials m_legacyCredentials = new LegacyCredentials();

    /**
     * Creates settings for credentials-based authentication where blank passwords are not allowed.
     *
     * @param authType the authentication type
     */
    public LegacyCredentialsAuthProviderSettings(final AuthType authType) {
        this(authType, false);
    }

    /**
     * Creates settings for credentials-based authentication.
     *
     * @param authType the authentication type
     * @param allowBlankPassword whether blank passwords are allowed
     */
    public LegacyCredentialsAuthProviderSettings(final AuthType authType, final boolean allowBlankPassword) {
        m_allowBlankSecret = allowBlankPassword;
        m_authType = authType;
    }

    /**
     * Creates settings for credentials-based authentication.
     *
     * @param authType the authentication type
     * @param allowBlankPassword whether blank passwords are allowed
     * @param legacyCredentials the legacy credentials to use
     */
    public LegacyCredentialsAuthProviderSettings(final AuthType authType, final boolean allowBlankPassword,
        final LegacyCredentials legacyCredentials) {
        m_authType = authType;
        m_allowBlankSecret = allowBlankPassword;
        m_legacyCredentials = legacyCredentials;
    }

    @Override
    public boolean isEnabled() {
        // No enablement management. These settings are only to be used in a webUI context.
        return false;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        // No enablement management. These settings are only to be used in a webUI context.
    }

    @Override
    public AuthType getAuthType() {
        return m_authType;
    }

    /**
     * Gets the credentials represented by these settings from the given credentials provider.
     * @param cp the credentials provider
     * @return the credentials
     */
    public Credentials getCredentials(final CredentialsProvider cp) {
        return m_legacyCredentials.toCredentials(cp);

    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer,
        final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        // Nothing to do here
    }

    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        throw new NotConfigurableException("CredentialsAuthProviderSettings cannot be used in legacy dialogs.");
    }

    @Override
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_legacyCredentials = SettingsLoaderFactory.loadSettings(LegacyCredentials.class, settings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var legacyCredentials = SettingsLoaderFactory.loadSettings(LegacyCredentials.class, settings);
        final var credentials = legacyCredentials.toCredentialsOrNull();
        if (credentials == null) {
            return;
        }
        validateID(credentials);
        validateSecret(credentials);
    }

    /**
     * Validates the ID. Can be overriden by subclasses to implement custom validation and error messages.
     *
     * @throws InvalidSettingsException If ID is invalid.
     */
    protected void validateID(final Credentials credentials) throws InvalidSettingsException {
        if (StringUtils.isBlank(credentials.getUsername())) {
            throw new InvalidSettingsException("Please provide valid username.");
        }
    }

    /**
     * Validates the secret. Can be overriden by subclasses to implement custom validation and error messages.
     *
     * @throws InvalidSettingsException If secret is invalid.
     */
    protected void validateSecret(final Credentials credentials) throws InvalidSettingsException {
        if (!m_allowBlankSecret && StringUtils.isBlank(credentials.getPassword())) {
            throw new InvalidSettingsException("The password must not be blank. Please provide a valid password.");
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        final var credentials = m_legacyCredentials.toCredentialsOrNull();
        if (credentials == null) {
            return;
        }
        validateID(credentials);
        validateSecret(credentials);
    }

    @Override
    public void saveSettingsForDialog(final NodeSettingsWO settings) {
        throw new UnsupportedOperationException("CredentialsAuthProviderSettings cannot be used in legacy dialogs.");
    }

    @Override
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        SettingsSaverFactory.saveSettings(m_legacyCredentials, settings);
    }

    @Override
    public void clear() {
        m_legacyCredentials = new LegacyCredentials();
    }

    @Override
    public String toString() {
        if (m_legacyCredentials.m_flowVarName != null) {
            return String.format("%s(credentials=%s)", m_authType.getSettingsKey(), m_legacyCredentials.m_flowVarName);
        } else {
            return String.format("%s(user=%s)", m_authType.getSettingsKey(),
                m_legacyCredentials.toCredentials().getUsername());
        }
    }

    @Override
    public AuthProviderSettings createClone() {
        return new LegacyCredentialsAuthProviderSettings(m_authType, m_allowBlankSecret,
            m_legacyCredentials.createCopy());
    }

    /**
     * Use this migration to migrate from legacy {@link UserPasswordAuthProviderSettings} to
     * {@link LegacyCredentialsAuthProviderSettings}. The setting for the flow variable name is migrated to be set as an
     * actual flow variable controlling the credentials.
     */
    public abstract static class FromUserPasswordAuthProviderSettingsMigration
        implements NodeParametersMigration<LegacyCredentials> {

        private final String m_cfgKeyUserPwd;

        private IDWithSecretAuthProviderSettings m_legacySettings;

        /**
         * Creates the migration from the given legacy settings.
         *
         * @param legacySettings the legacy settings to migrate from. Use the same construction as for the instance
         *            constructed in the model of the node (which is usually using AuthSettings.Builder).
         */
        protected FromUserPasswordAuthProviderSettingsMigration(final IDWithSecretAuthProviderSettings legacySettings) {
            m_cfgKeyUserPwd = legacySettings.getAuthType().getSettingsKey();
            m_legacySettings = legacySettings;
        }

        @Override
        public List<ConfigMigration<LegacyCredentials>> getConfigMigrations() {
            return List.of(
                ConfigMigration.builder(this::loadLegacyCredentials).withDeprecatedConfigPath(m_cfgKeyUserPwd).build());
        }

        private LegacyCredentials loadLegacyCredentials(final NodeSettingsRO legacySettings)
            throws InvalidSettingsException {
            final var userPwdSettings = (IDWithSecretAuthProviderSettings)m_legacySettings.createClone();
            userPwdSettings.loadSettingsForModel(legacySettings.getNodeSettings(m_cfgKeyUserPwd));
            if (userPwdSettings.useCredentials()) {
                return new LegacyCredentials(userPwdSettings.getCredentialsName());
            }
            final var user = userPwdSettings.getIDModel().getStringValue();
            final var password = userPwdSettings.getSecretModel().getStringValue();
            return new LegacyCredentials(new Credentials(user, password));
        }

    }

}
