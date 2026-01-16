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
 *   Jan 16, 2026 (magnus): created
 */
package org.knime.node.parameters.persistence.legacy;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;

/**
 * Legacy authentication type selection parameters for migration of {@link SettingsModelAuthentication}.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@PersistWithin("..")
public final class LegacyAuthenticationTypeSelection implements NodeParameters {

    private static final String SECRET_KEY = "c-rH4Tkyk";

    /**
     * Modification to adapt legacy authentication type selection.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class LegacyAuthenticationModification implements Modification.Modifier {

        private Class<? extends EnumChoicesProvider<AuthTypes>> m_authTypeChoicesProvider;

        private String m_authTypeSelectionTitle;

        private List<Triple<AuthTypes, String, String>> m_supportedAuthTypes;

        /**
         * Constructor for legacy authentication modification.
         *
         * @param authTypeChoicesProvider the choices provider for authentication types
         * @param authTypeSelectionTitle the title for the authentication type selection
         * @param supportedAuthTypes list of triples of supported authentication types, their labels and descriptions
         */
        protected LegacyAuthenticationModification(
            final Class<? extends EnumChoicesProvider<AuthTypes>> authTypeChoicesProvider,
            final String authTypeSelectionTitle, final List<Triple<AuthTypes, String, String>> supportedAuthTypes) {
            m_authTypeSelectionTitle = authTypeSelectionTitle;
            m_supportedAuthTypes = supportedAuthTypes;
            m_authTypeChoicesProvider = authTypeChoicesProvider;
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_authTypeChoicesProvider != null) {
                group.find(SelectedTypeModRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(m_authTypeChoicesProvider)
                .modify();
            }
            if (m_authTypeSelectionTitle != null && m_supportedAuthTypes != null) {
                group.find(SelectedTypeModRef.class).modifyAnnotation(Widget.class)
                .withProperty("title", m_authTypeSelectionTitle)
                .withProperty("description", getAuthTypeDescription())
                .modify();
            }
        }

        private String getAuthTypeDescription() {
            var description = "Method for authentication <br/>";
            final var labelsAndDescriptions = m_supportedAuthTypes.stream()
                .map(triple -> Pair.of(triple.getMiddle(), triple.getRight())).toList();
            for (Pair<String, String> labelAndDescription : labelsAndDescriptions) {
                description += String.format("<b>%s</b><br/>%s<br/>",
                    labelAndDescription.getLeft(), labelAndDescription.getRight());
            }
            return description;
        }

    }

    static final String SETTINGS_MODEL_KEY_TYPE = "selectedType";

    @Persistor(AuthenticationTypePersistor.class)
    @Widget(title = "Authentication Type", description = """
            The flow variable containing the credentials which are used for authentication.
    """)
    @Modification.WidgetReference(SelectedTypeModRef.class)
    @ValueReference(SelectedAuthenticationTypeRef.class)
    AuthTypes m_selectedType;

    /**
     * Parameter reference for the selected authentication type.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public interface SelectedAuthenticationTypeRef extends ParameterReference<AuthTypes> {
    }

    interface SelectedTypeModRef extends ParameterReference<AuthTypes>, Modification.Reference {
    }

    /**
     * Authentication parameters for username authentication.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @PersistWithin("..")
    public static final class UserAuth implements NodeParameters {

        /**
         * Modification to adapt user authentication widget.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        public abstract static class UserAuthModification implements Modification.Modifier {

            private String m_title;

            private String m_description;

            private String m_usernameLabel;

            /**
             * Constructor for user authentication modification.
             *
             * @param title the title for the authentication
             * @param description the description for the authentication
             * @param usernameLabel the label for the username field
             */
            protected UserAuthModification(final String title, final String description, final String usernameLabel) {
                m_title = title;
                m_description = description;
                m_usernameLabel = usernameLabel;
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                if (m_title != null) {
                    group.find(UsernameModRef.class).modifyAnnotation(Widget.class).withProperty("title", m_title)
                        .modify();
                }
                if (m_description != null) {
                    group.find(UsernameModRef.class).modifyAnnotation(Widget.class)
                        .withProperty("description", m_description).modify();
                }
                if (m_usernameLabel != null) {
                    group.find(UsernameModRef.class).modifyAnnotation(UsernameWidget.class)
                        .withProperty("value", m_usernameLabel).modify();
                }
            }

        }

        @Persistor(AccessKeyIdAndSecretKeyPersistor.class)
        @Widget(title = "Username", description = "Set the username for authentication.")
        @UsernameWidget
        @Modification.WidgetReference(UsernameModRef.class)
        @Effect(predicate = IsUserAuth.class, type = EffectType.SHOW)
        Credentials m_username = new Credentials("", "");

        interface UsernameModRef extends ParameterReference<Credentials>, Modification.Reference {
        }

        static final class AccessKeyIdAndSecretKeyPersistor extends CredentialsPersistor {

            protected AccessKeyIdAndSecretKeyPersistor() {
                super(AuthTypes.USER);
            }

        }

    }

    /**
     * Authentication parameters for username and password authentication.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @PersistWithin("..")
    public static final class UserPwdAuth implements NodeParameters {

        /**
         * Modification to user and password authentication widget.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        public abstract static class UserPwdAuthModification implements Modification.Modifier {

            private String m_title;

            private String m_description;

            private String m_usernameLabel;

            private String m_passwordLabel;

            /**
             * Constructor for user and password authentication modification.
             *
             * @param title the title for the authentication
             * @param description the description for the authentication
             * @param usernameLabel the label for the username field
             * @param passwordLabel the label for the password field
             */
            protected UserPwdAuthModification(final String title, final String description, final String usernameLabel,
                final String passwordLabel) {
                m_title = title;
                m_description = description;
                m_usernameLabel = usernameLabel;
                m_passwordLabel = passwordLabel;
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                if (m_title != null) {
                    group.find(UsernameAndPasswordModRef.class).modifyAnnotation(Widget.class)
                        .withProperty("title", m_title).modify();
                }
                if (m_description != null) {
                    group.find(UsernameAndPasswordModRef.class).modifyAnnotation(Widget.class)
                        .withProperty("description", m_description).modify();
                }
                if (m_usernameLabel != null) {
                    group.find(UsernameAndPasswordModRef.class).modifyAnnotation(CredentialsWidget.class)
                        .withProperty("usernameLabel", m_usernameLabel).modify();
                }
                if (m_passwordLabel != null) {
                    group.find(UsernameAndPasswordModRef.class).modifyAnnotation(CredentialsWidget.class)
                        .withProperty("passwordLabel", m_passwordLabel).modify();
                }
            }

        }

        @Persistor(UsernameAndPasswordPersistor.class)
        @Widget(title = "Username & password", description = """
                Set the username and password for authentication.
                """)
        @CredentialsWidget
        @Modification.WidgetReference(UsernameAndPasswordModRef.class)
        @ValueProvider(RemovePasswordOnTypeChangeProvider.class)
        @ValueReference(UsernameAndPasswordRef.class)
        @Effect(predicate = IsUserPwdAuth.class, type = EffectType.SHOW)
        Credentials m_usernameAndPassword = new Credentials("", "");

        interface UsernameAndPasswordModRef extends ParameterReference<Credentials>, Modification.Reference {
        }

        interface UsernameAndPasswordRef extends ParameterReference<Credentials> {
        }

        static final class RemovePasswordOnTypeChangeProvider extends RemovePasswordProvider {

            protected RemovePasswordOnTypeChangeProvider() {
                super(UsernameAndPasswordRef.class);
            }

        }

        static final class UsernameAndPasswordPersistor extends CredentialsPersistor {

            protected UsernameAndPasswordPersistor() {
                super(AuthTypes.USER_PWD);
            }

        }

    }

    /**
     * Authentication parameters for password authentication.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @PersistWithin("..")
    public static final class PwdAuth implements NodeParameters {

        /**
         * Modification to adapt password authentication widget.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        public abstract static class PwdAuthModification implements Modification.Modifier {

            private String m_title;

            private String m_description;

            private String m_passwordLabel;

            /**
             * Constructor for password authentication modification.
             *
             * @param title the title for the authentication
             * @param description the description for the authentication
             * @param passwordLabel the label for the password field
             */
            protected PwdAuthModification(final String title, final String description, final String passwordLabel) {
                m_title = title;
                m_description = description;
                m_passwordLabel = passwordLabel;
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                if (m_title != null) {
                    group.find(PasswordModRef.class).modifyAnnotation(Widget.class).withProperty("title", m_title)
                        .modify();
                }
                if (m_description != null) {
                    group.find(PasswordModRef.class).modifyAnnotation(Widget.class)
                        .withProperty("description", m_description).modify();
                }
                if (m_passwordLabel != null) {
                    group.find(PasswordModRef.class).modifyAnnotation(PasswordWidget.class)
                        .withProperty("passwordLabel", m_passwordLabel).modify();
                }
            }

        }

        @Persistor(PasswordPersistor.class)
        @Widget(title = "Password", description = """
                Set the password for authentication.
                """)
        @PasswordWidget
        @Modification.WidgetReference(PasswordModRef.class)
        @ValueProvider(RemovePasswordOnTypeChangeProvider.class)
        @ValueReference(PasswordRef.class)
        @Effect(predicate = IsPwdAuth.class, type = EffectType.SHOW)
        Credentials m_password = new Credentials("", "");

        interface PasswordModRef extends ParameterReference<Credentials>, Modification.Reference {
        }

        interface PasswordRef extends ParameterReference<Credentials> {
        }

        static final class RemovePasswordOnTypeChangeProvider extends RemovePasswordProvider {

            protected RemovePasswordOnTypeChangeProvider() {
                super(PasswordRef.class);
            }

        }

        static final class PasswordPersistor extends CredentialsPersistor {

            protected PasswordPersistor() {
                super(AuthTypes.PWD);
            }

        }

    }

    /**
     * Authentication parameters for credentials flow variable authentication.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    @PersistWithin("..")
    public static final class CredentialsAuth implements NodeParameters {

        /**
         * Modification to adapt credentials authentication.
         *
         * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
         */
        public abstract static class CredentialsAuthModification implements Modification.Modifier {

            private String m_title;

            private String m_description;

            /**
             * Constructor for credential authentication modification.
             *
             * @param title the title for the authentication
             * @param description the description for the authentication
             */
            protected CredentialsAuthModification(final String title, final String description) {
                m_title = title;
                m_description = description;
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                if (m_title != null) {
                    group.find(CredentialsModRef.class).modifyAnnotation(Widget.class).withProperty("title", m_title)
                        .modify();
                }
                if (m_description != null) {
                    group.find(CredentialsModRef.class).modifyAnnotation(Widget.class)
                        .withProperty("description", m_description).modify();
                }
            }

        }

        @Persist(configKey = "credentials")
        @Widget(title = "Use credentials", description = "Use credentials from a flow variable for authentication.")
        @ChoicesProvider(CredentialFlowVariablesProvider.class)
        @Modification.WidgetReference(CredentialsModRef.class)
        @Effect(predicate = IsCredentialsAuth.class, type = EffectType.SHOW)
        String m_credentialsFlowVarName;

        interface CredentialsModRef extends ParameterReference<String>, Modification.Reference {
        }

    }

    static final class IsUserAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectedAuthenticationTypeRef.class).isOneOf(AuthTypes.USER);
        }

    }

    static final class IsUserPwdAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectedAuthenticationTypeRef.class).isOneOf(AuthTypes.USER_PWD);
        }

    }

    static final class IsPwdAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectedAuthenticationTypeRef.class).isOneOf(AuthTypes.PWD);
        }

    }

    static final class IsCredentialsAuth implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectedAuthenticationTypeRef.class).isOneOf(AuthTypes.CREDENTIALS);
        }

    }

    abstract static class RemovePasswordProvider implements StateProvider<Credentials> {

        private Class<? extends ParameterReference<Credentials>> m_credentialsRef;

        protected RemovePasswordProvider(final Class<? extends ParameterReference<Credentials>> credentialsRef) {
            m_credentialsRef = credentialsRef;
        }

        Supplier<AuthTypes> m_selectedTypeSupplier;

        Supplier<Credentials> m_credentialsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_selectedTypeSupplier = initializer.computeFromValueSupplier(SelectedAuthenticationTypeRef.class);
            m_credentialsSupplier = initializer.getValueSupplier(m_credentialsRef);
        }

        @Override
        public Credentials computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_selectedTypeSupplier.get() != AuthTypes.USER_PWD && m_selectedTypeSupplier.get() != AuthTypes.PWD) {
                if (m_credentialsSupplier.get() != null) {
                    return new Credentials(m_credentialsSupplier.get().getUsername(), null);
                }
            }
            throw new StateComputationFailureException();
        }

    }

    /**
     * Choices provider for credential flow variables.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public static final class CredentialFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(VariableType.CredentialsType.INSTANCE)
                    .values().stream().toList();
        }

    }

    static final class AuthenticationTypePersistor implements NodeParametersPersistor<AuthTypes> {

        @Override
        public AuthTypes load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return AuthTypes.getFromValue(
                settings.getString(SETTINGS_MODEL_KEY_TYPE, AuthTypes.NONE.name()));
        }

        @Override
        public void save(final AuthTypes param, final NodeSettingsWO settings) {
            settings.addString(SETTINGS_MODEL_KEY_TYPE, param.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SETTINGS_MODEL_KEY_TYPE}};
        }

    }

    abstract static class CredentialsPersistor implements NodeParametersPersistor<Credentials> {

        private AuthTypes m_selectedType;

        private String m_cfgUsername;

        private String m_cfgPassword;

        /**
         * Constructor with default configuration keys.
         *
         * @param selectedType the selected authentication type
         */
        protected CredentialsPersistor(final AuthTypes selectedType) {
            this(selectedType, "username", "password");
        }

        /**
         * Constructor with custom configuration keys.
         *
         * @param selectedType the selected authentication type
         * @param cfgUsername the configuration key for the username
         * @param cfgPassword the configuration key for the password
         */
        protected CredentialsPersistor(final AuthTypes selectedType,
            final String cfgUsername, final String cfgPassword) {
            m_selectedType = selectedType;
            m_cfgUsername = cfgUsername;
            m_cfgPassword = cfgPassword;
        }

        @Override
        public Credentials load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return switch (m_selectedType) {
                case USER -> new Credentials(settings.getString(m_cfgUsername, null), null);
                case PWD -> new Credentials(null, settings.getPassword(m_cfgPassword, SECRET_KEY, null));
                case USER_PWD -> new Credentials(settings.getString(m_cfgUsername, null),
                    settings.getPassword(m_cfgPassword, SECRET_KEY, null));
                default -> throw new InvalidSettingsException("Unexpected value: " + m_selectedType);
            };
        }

        @Override
        public void save(final Credentials param, final NodeSettingsWO settings) {
            switch (m_selectedType) {
                case USER: {
                    settings.addString(m_cfgUsername, param.getUsername());
                    break;
                    }
                case PWD: {
                    settings.addPassword(m_cfgPassword, SECRET_KEY, param.getPassword());
                    break;}
                case USER_PWD: {
                    settings.addString(m_cfgUsername, param.getUsername());
                    settings.addPassword(m_cfgPassword, SECRET_KEY, param.getPassword());
                    break;
                    }
                default: break;
            }
        }

        @Override
        public String[][] getConfigPaths() {
            // AP-14067: Only possible to overwrite user name
            return switch (m_selectedType) {
                case USER -> new String[][]{{m_cfgUsername}};
                case USER_PWD -> new String[][]{{m_cfgUsername}};
                default -> new String[][]{{}};
            };
        }

    }

    /**
     * Enumeration of supported authentication types.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public enum AuthTypes {

        /** No authentication */
        @Label("No authentication")
        NONE,
        /** Username authentication */
        @Label("Username")
        USER,
        /** Username and password authentication */
        @Label("Username & password")
        USER_PWD,
        /** Password authentication */
        @Label("Password")
        PWD,
        /** Credentials authentication */
        @Label("Credentials")
        CREDENTIALS,
        /** Kerberos authentication */
        @Label("Kerberos")
        KERBEROS;

        /**
         * Gets the authentication type from its string value.
         *
         * @param value the string value
         * @return the corresponding authentication type
         * @throws InvalidSettingsException if the value is invalid
         */
        public static AuthTypes getFromValue(final String value) throws InvalidSettingsException {
            for (final AuthTypes authType : values()) {
                if (authType.name().equals(value)) {
                    return authType;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = Arrays.stream(AuthTypes.values()).map(AuthTypes::name)
                    .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    /**
     * Default constructor setting the authentication type to NONE.
     */
    public LegacyAuthenticationTypeSelection() {
        this(AuthTypes.NONE);
    }

    /**
     * Constructor with selected authentication type.
     *
     * @param selectedType the selected authentication type
     */
    public LegacyAuthenticationTypeSelection(final AuthTypes selectedType) {
        m_selectedType = selectedType;
    }

}
