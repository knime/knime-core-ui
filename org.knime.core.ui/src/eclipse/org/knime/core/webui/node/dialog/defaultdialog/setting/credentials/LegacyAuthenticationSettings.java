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
 *   Feb 28, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.credentials;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.SettingsModelAuthenticationMigrator.SETTINGS_MODEL_KEY_CREDENTIAL;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.SettingsModelAuthenticationMigrator.SETTINGS_MODEL_KEY_PASSWORD;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.SettingsModelAuthenticationMigrator.SETTINGS_MODEL_KEY_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.SettingsModelAuthenticationMigrator.SETTINGS_MODEL_KEY_USERNAME;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigrator;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationTypeRef;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.RequiresPasswordProvider;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.RequiresUsernameProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Similarly to {@link AuthenticationSettings}, but additionally supports the
 * {@link org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType#CREDENTIALS
 * "Credentials"} authentication type in legacy dialogs by setting the respective flow variable once the dialog is
 * opened.
 *
 * To use it use the {@link SettingsModelAuthenticationBackwardsCompatibleLoader} as a {@link Migration} for the field.
 *
 * @author Paul Bärnreuther
 */
@Persistor(LegacyAuthenticationSettings.SettingsModelAuthenticationPersistor.class)
public final class LegacyAuthenticationSettings implements WidgetGroup, PersistableSettings {

    @Widget(title = "Authentication type", description = "The type of the used authentication.")
    @ValueReference(AuthenticationTypeRef.class)
    @RadioButtonsWidget(horizontal = true)
    final AuthenticationType m_type;

    @Widget(title = "Credentials", description = "The credentials used for the authentication.")
    @Effect(predicate = AuthenticationTypeRef.RequiresCredentials.class, type = EffectType.SHOW)
    @CredentialsWidget(hasPasswordProvider = RequiresPasswordProvider.class,
        hasUsernameProvider = RequiresUsernameProvider.class)
    final LegacyCredentials m_legacyCredentials;

    /**
     * @param authenticationSettings
     */
    public LegacyAuthenticationSettings(final AuthenticationSettings authenticationSettings) {
        this(authenticationSettings.m_type, new LegacyCredentials(authenticationSettings.m_credentials));
    }

    /**
     * package scope for test purposes
     */
    LegacyAuthenticationSettings(final AuthenticationType type, final LegacyCredentials legacyCredentials) {
        m_type = type;
        m_legacyCredentials = legacyCredentials;
    }

    /**
     * @param provider that is used when
     *            {@link org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType#CREDENTIALS
     *            "Credentials"} has been set as authentication type and the dialog was not applied since then
     * @return the {@link AuthenticationSettings} equivalent
     */
    public AuthenticationSettings toAuthenticationSettings(final CredentialsProvider provider) {
        return new AuthenticationSettings(m_type, m_legacyCredentials.toCredentials(provider));
    }

    /**
     * package scope for test purposes
     */
    AuthenticationSettings toAuthenticationSettings() {
        return new AuthenticationSettings(m_type, m_legacyCredentials.toCredentials());
    }

    static class SettingsModelAuthenticationPersistor implements NodeSettingsPersistor<LegacyAuthenticationSettings> {

        @Override
        public LegacyAuthenticationSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LegacyAuthenticationSettings(
                SettingsLoaderFactory.getSettingsLoader(AuthenticationSettings.class).load(settings));
        }

        @Override
        public void save(final LegacyAuthenticationSettings obj, final NodeSettingsWO settings) {
            SettingsSaverFactory.getSettingsSaver(AuthenticationSettings.class).save(obj.toAuthenticationSettings(),
                settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{new String[]{}};
        }

    }

    /**
     * Extend this and use it on a {@link LegacyAuthenticationSettings} field to make it backwards compatible with
     * respect to {@link SettingsModelAuthentication}.
     *
     * @author Paul Bärnreuther
     */
    public abstract static class SettingsModelAuthenticationBackwardsCompatibleLoader
        implements NodeSettingsMigrator<LegacyAuthenticationSettings> {

        private AuthenticationSettings.SettingsModelAuthenticationMigrator m_settingsModelAuthenticationPersistor;

        private final String m_configKey;

        private final AuthenticationType m_authenticationTypeDefaultForCredentials;

        /**
         * @param configKey the config key used before AND after the migration to load and save the settings.
         * @param authenticationTypeDefaultForCredentials the an authentication type (either USER_PWD, PWD or USER) to
         *            set this as the new selected option whenever CREDENTIALS was selected in the old dialog.
         */
        protected SettingsModelAuthenticationBackwardsCompatibleLoader(final String configKey,
            final AuthenticationType authenticationTypeDefaultForCredentials) {
            m_configKey = configKey;
            m_authenticationTypeDefaultForCredentials = authenticationTypeDefaultForCredentials;
            m_settingsModelAuthenticationPersistor =
                new AuthenticationSettings.SettingsModelAuthenticationMigrator(m_configKey);
        }

        @Override
        public List<ConfigsDeprecation<LegacyAuthenticationSettings>> getConfigsDeprecations() {
            return List.of(new Builder<LegacyAuthenticationSettings>(this::loadFromSettingsModelSettings)//
                .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_CREDENTIAL) //
                .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_PASSWORD) //
                .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_USERNAME) //
                .withDeprecatedConfigPath(m_configKey, SETTINGS_MODEL_KEY_TYPE) //
                .withMatcher(m_settingsModelAuthenticationPersistor::isSettingsSavedBySettingsModel).build());
        }

        private LegacyAuthenticationSettings loadFromSettingsModelSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            final var model = m_settingsModelAuthenticationPersistor.loadModelFromSettings(settings);
            if (model.getAuthenticationType() == SettingsModelAuthentication.AuthenticationType.CREDENTIALS) {
                return loadFromCredentialsSettingsModel(model);
            }
            return new LegacyAuthenticationSettings(
                AuthenticationSettings.SettingsModelAuthenticationMigrator.loadFromModel(model));
        }

        private LegacyAuthenticationSettings loadFromCredentialsSettingsModel(final SettingsModelAuthentication model) {
            final var credentials = AuthenticationSettings.SettingsModelAuthenticationMigrator.toCredentials(model);
            final var flowVariableName = model.getCredential();
            return new LegacyAuthenticationSettings(m_authenticationTypeDefaultForCredentials,
                new LegacyCredentials(credentials, flowVariableName));
        }

    }

}
