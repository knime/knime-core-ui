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
 *   Feb 26, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.migration.ConfigMigration.Builder;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;

/**
 * A switch on the type of authentication that is to be used plus a {@link Credentials} widget that is shown
 * appropriately configured whenever an authentication type requires additional user input.
 *
 * @author Paul Bärnreuther
 */
public final class AuthenticationSettings extends BaseAuthenticationSettings {

    /**
     * The types of authentications.
     *
     * @author Paul Bärnreuther
     */
    public enum AuthenticationType {
            /** No authentication required. */
            @Label(value = "None", description = "No authentication is required.")
            NONE,
            /** Authentication with username. */
            @Label(value = "Username", description = "Username-based authentication. No password required.")
            USER,
            /** Authentication with username and password. */
            @Label(value = "Username and Password", description = "Username and password based authentication.")
            USER_PWD,
            /** Authentication with password. */
            @Label(value = "Password", description = "Password based authentication. No username required.")
            PWD,
            /** Authentication with kerberos. */
            @Label(value = "Kerberos", description = "Kerberos ticket based authentication")
            KERBEROS;

        boolean requiresUsername() {
            return List.of(USER, USER_PWD).contains(this);
        }

        boolean requiresPassword() {
            return List.of(PWD, USER_PWD).contains(this);
        }

        static final AuthenticationType[] REQUIRE_CREDENTIALS = new AuthenticationType[]{USER_PWD, PWD, USER};

    }

    abstract static class AuthenticationTypeDependentProvider implements StateProvider<Boolean> {

        protected Supplier<AuthenticationType> m_typeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_typeSupplier = initializer.computeFromValueSupplier(AuthenticationTypeRef.class);
        }

    }

    static final class RequiresUsernameProvider extends AuthenticationTypeDependentProvider {

        @Override
        public Boolean computeState(final NodeParametersInput context) {
            return m_typeSupplier.get().requiresUsername();
        }

    }

    static final class RequiresPasswordProvider extends AuthenticationTypeDependentProvider {

        @Override
        public Boolean computeState(final NodeParametersInput context) {
            return m_typeSupplier.get().requiresPassword();
        }

    }

    @Widget(title = "Credentials", description = "The credentials used for the authentication.")
    @Effect(predicate = AuthenticationTypeRef.RequiresCredentials.class, type = EffectType.SHOW)
    @CredentialsWidget
    @CredentialsWidgetInternal(hasPasswordProvider = RequiresPasswordProvider.class,
        hasUsernameProvider = RequiresUsernameProvider.class)
    final Credentials m_credentials;

    /**
     * @return the type
     */
    public AuthenticationType getType() {
        return m_type;
    }

    /**
     * @return the credentials
     */
    public Credentials getCredentials() {
        return m_credentials;
    }

    /**
     * NONE selected and empty credentials
     */
    public AuthenticationSettings() {
        this(AuthenticationType.NONE, new Credentials());
    }

    /**
     * @param type the selected type of authentication
     * @param credentials the initial credentials
     */
    public AuthenticationSettings(final AuthenticationType type, final Credentials credentials) {
        m_type = type;
        m_credentials = credentials;
    }

    /**
     * Used to make the {@link AuthenticationSettings} backwards-compatible with respect to the
     * {@link SettingsModelAuthentication} if used within a {@link Migration} at the field.
     *
     * @author Paul Bärnreuther
     */
    public static class SettingsModelAuthenticationMigrator implements NodeParametersMigration<AuthenticationSettings> {

        private final String m_configKey;

        /**
         * @param configKey the config key under which the authentication settings have been stored.
         */
        public SettingsModelAuthenticationMigrator(final String configKey) {
            m_configKey = configKey;
        }

        static final String SETTINGS_MODEL_KEY_TYPE = "selectedType";

        static final String SETTINGS_MODEL_KEY_CREDENTIAL = "credentials";

        static final String SETTINGS_MODEL_KEY_PASSWORD = "password";

        static final String SETTINGS_MODEL_KEY_USERNAME = "username";

        /**
         * This method should never be called with {@link SettingsModelAuthentication.AuthenticationType#CREDENTIALS}.
         *
         * If it is, this probably means that {@link LegacyAuthenticationSettings} are to be used here instead of
         * {@link AuthenticationSettings}.
         *
         * @throws InvalidSettingsException
         */
        static AuthenticationType toAuthenticationType(
            final SettingsModelAuthentication.AuthenticationType legacySettingsType) throws InvalidSettingsException {
            return switch (legacySettingsType) {
                case NONE -> AuthenticationType.NONE;
                case PWD -> AuthenticationType.PWD;
                case USER -> AuthenticationType.USER;
                case USER_PWD -> AuthenticationType.USER_PWD;
                case KERBEROS -> AuthenticationType.KERBEROS;
                default -> throw new InvalidSettingsException(
                    "Unexpected settings model authentication type value: " + legacySettingsType);
            };
        }

        boolean isSettingsSavedBySettingsModel(final NodeSettingsRO settings) {
            try {
                return settings.getNodeSettings(m_configKey).containsKey(SETTINGS_MODEL_KEY_TYPE);
            } catch (InvalidSettingsException ex) { // NOSONAR
                return false;
            }
        }

        SettingsModelAuthentication loadModelFromSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            final var model =
                new SettingsModelAuthentication(m_configKey, SettingsModelAuthentication.AuthenticationType.NONE);
            model.loadSettingsFrom(settings);
            return model;
        }

        static AuthenticationSettings loadFromModel(final SettingsModelAuthentication model)
            throws InvalidSettingsException {
            final var type = toAuthenticationType(model.getAuthenticationType());
            return new AuthenticationSettings(type, toCredentials(model));
        }

        static Credentials toCredentials(final SettingsModelAuthentication model) {
            return new Credentials(model.getUsername(), model.getPassword());
        }

        @Override
        public List<ConfigMigration<AuthenticationSettings>> getConfigMigrations() {
            return List
                .of(new Builder<AuthenticationSettings>(settings -> loadFromModel(loadModelFromSettings(settings)))
                    .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_CREDENTIAL) //
                    .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_PASSWORD) //
                    .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_USERNAME) //
                    .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_TYPE) //
                    .withMatcher(this::isSettingsSavedBySettingsModel).build());
        }

    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AuthenticationSettings other) {
            return Objects.equals(m_credentials, other.m_credentials) && Objects.equals(m_type, other.m_type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_credentials, m_type);
    }

}
