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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.saveSettings;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.webui.node.dialog.defaultdialog.PersistUtilTest;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyAuthenticationSettings.SettingsModelAuthenticationBackwardsCompatibleLoader;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // We allow assertions without messages
class LegacyAuthenticationSettingsTest {

    final static String PASSWORD = "myPassword";

    final static String USERNAME = "myUsername";

    final static String SECOND_FACTOR = "mySecondFactor";

    final static Credentials CREDENTIALS = new Credentials(USERNAME, PASSWORD, SECOND_FACTOR);

    final static String FLOW_VAR_NAME = "myFlowVarName";

    private static CredentialsProvider m_credentialsProvider;

    @BeforeAll
    static void mockCredentialsProvider() {
        m_credentialsProvider = mock(CredentialsProvider.class);
        when(m_credentialsProvider.get(FLOW_VAR_NAME)).thenReturn(new ICredentials() {

            @Override
            public String getPassword() {
                return PASSWORD;
            }

            @Override
            public Optional<String> getSecondAuthenticationFactor() {
                return Optional.of(SECOND_FACTOR);
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getLogin() {
                return USERNAME;
            }
        });
    }

    @Test
    void testToAuthenticationSettings() {
        final var authenticationSettings =
            new AuthenticationSettings(AuthenticationType.PWD, new Credentials("username", "password"));
        final var legacyAuthenticationSettings = new LegacyAuthenticationSettings(authenticationSettings);
        assertEquals(authenticationSettings, legacyAuthenticationSettings.toAuthenticationSettings());
    }

    @Test
    void testThrowsOnToAuthenticationSettingsOnLegacyFlowVariable() {
        final var legacyAuthenticationSettings = new LegacyAuthenticationSettings(AuthenticationType.PWD,
            new LegacyCredentials(new Credentials(), "flowVarName"));
        assertThrows(IllegalStateException.class, () -> legacyAuthenticationSettings.toAuthenticationSettings());
    }

    @Test
    void testToAuthenticationSettingsWithCredentialsProvider() {

        final var expected = new AuthenticationSettings(AuthenticationType.PWD, CREDENTIALS);
        final var legacyAuthenticationSettings = new LegacyAuthenticationSettings(AuthenticationType.PWD,
            new LegacyCredentials(new Credentials(), FLOW_VAR_NAME));
        assertEquals(expected, legacyAuthenticationSettings.toAuthenticationSettings(m_credentialsProvider));
    }

    @Nested
    class SettingsModelAuthenticationPersistorTest {

        final static String CFG_KEY_PWD = "authenticationSettingsPWD";

        final static String CFG_KEY_USER = "authenticationSettingsUSER";

        final static String CFG_KEY_USER_PWD = "authenticationSettingsUSER_PWD";

        static final class TestSettings implements PersistableSettings {

            static final class PWDBackwardsCompatibleLoader
                extends SettingsModelAuthenticationBackwardsCompatibleLoader {

                public PWDBackwardsCompatibleLoader() {
                    super(CFG_KEY_PWD, AuthenticationType.PWD);
                }

            }

            @Migration(PWDBackwardsCompatibleLoader.class)
            @Persist(configKey = CFG_KEY_PWD)
            LegacyAuthenticationSettings first;

            static final class USERBackwardsCompatibleLoader
                extends SettingsModelAuthenticationBackwardsCompatibleLoader {

                public USERBackwardsCompatibleLoader() {
                    super(CFG_KEY_USER, AuthenticationType.USER);
                }

            }

            @Migration(USERBackwardsCompatibleLoader.class)
            @Persist(configKey = CFG_KEY_USER)
            LegacyAuthenticationSettings second;

            static final class USER_PWDBackwardsCompatibleLoader
                extends SettingsModelAuthenticationBackwardsCompatibleLoader {

                public USER_PWDBackwardsCompatibleLoader() {
                    super(CFG_KEY_USER_PWD, AuthenticationType.USER_PWD);
                }

            }

            @Migration(USER_PWDBackwardsCompatibleLoader.class)
            @Persist(configKey = CFG_KEY_USER_PWD)
            LegacyAuthenticationSettings third;

        }

        private static NodeSettings getLegacyNodeSettings(final SettingsModelAuthentication.AuthenticationType type,
            final String username, final String password, final String flowVarName) {

            final Function<String, SettingsModelAuthentication> createModel =
                cfgKey -> new SettingsModelAuthentication(cfgKey, type, username, password, flowVarName);

            final var passwordModel = createModel.apply(CFG_KEY_PWD);
            final var userModel = createModel.apply(CFG_KEY_USER);
            final var userPwdModel = createModel.apply(CFG_KEY_USER_PWD);
            final var nodeSettings = new NodeSettings("root");
            passwordModel.saveSettingsTo(nodeSettings);
            userModel.saveSettingsTo(nodeSettings);
            userPwdModel.saveSettingsTo(nodeSettings);
            return nodeSettings;
        }

        @Test
        void testLoadWithLegacyCredentialsType() throws InvalidSettingsException {
            final var nodeSettings = getLegacyNodeSettings(SettingsModelAuthentication.AuthenticationType.CREDENTIALS,
                null, null, FLOW_VAR_NAME);
            final var loadedSettings = loadSettings(TestSettings.class, nodeSettings);
            assertEquals(new AuthenticationSettings(AuthenticationType.PWD, CREDENTIALS),
                loadedSettings.first.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(new AuthenticationSettings(AuthenticationType.USER, CREDENTIALS),
                loadedSettings.second.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(new AuthenticationSettings(AuthenticationType.USER_PWD, CREDENTIALS),
                loadedSettings.third.toAuthenticationSettings(m_credentialsProvider));
        }

        @Test
        void testLoadWithNonCredentialsLegacyType() throws InvalidSettingsException {
            final var nodeSettings = getLegacyNodeSettings(SettingsModelAuthentication.AuthenticationType.USER_PWD,
                USERNAME, PASSWORD, null);
            final var loadedSettings = loadSettings(TestSettings.class, nodeSettings);
            final var expected =
                new AuthenticationSettings(AuthenticationType.USER_PWD, new Credentials(USERNAME, PASSWORD));
            assertEquals(expected, loadedSettings.first.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(expected, loadedSettings.second.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(expected, loadedSettings.third.toAuthenticationSettings(m_credentialsProvider));
        }

        @Test
        void testSaveLoad() throws InvalidSettingsException {
            final var authenticationSettings =
                new AuthenticationSettings(AuthenticationType.USER_PWD, new Credentials(USERNAME, PASSWORD));
            final var legacyAuthenticationSettings = new LegacyAuthenticationSettings(authenticationSettings);
            final var testSettings = new TestSettings();
            testSettings.first = legacyAuthenticationSettings;
            testSettings.second = legacyAuthenticationSettings;
            testSettings.third = legacyAuthenticationSettings;
            final var nodeSettings = new NodeSettings("root");
            saveSettings(testSettings, nodeSettings);
            final var savedAndLoaded = loadSettings(TestSettings.class, nodeSettings);
            assertEquals(authenticationSettings, savedAndLoaded.first.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(authenticationSettings, savedAndLoaded.second.toAuthenticationSettings(m_credentialsProvider));
            assertEquals(authenticationSettings, savedAndLoaded.third.toAuthenticationSettings(m_credentialsProvider));
        }

        @Test
        void testPersistSchema() {
            final var result = PersistUtilTest.getPersistSchema(TestSettings.class);
            assertThatJson(result).inPath("$.properties.model.properties.first.deprecatedConfigKeys").isArray()
                .hasSize(1);
            assertThatJson(result).inPath("$.properties.model.properties.first.deprecatedConfigKeys[0].deprecated")
                .isArray()
                .isEqualTo(new String[][]{{"authenticationSettingsPWD", "credentials"},
                    {"authenticationSettingsPWD", "password"}, {"authenticationSettingsPWD", "username"},
                    {"authenticationSettingsPWD", "selectedType"}});
        }

    }
}
