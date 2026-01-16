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
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.credentials.Credentials;

/**
 * Legacy base class for authentication related node parameters.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
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

        private String m_authTypeSelectionDescription;

        /**
         * Constructor for legacy authentication modification.
         *
         * @param authTypeChoicesProvider the choices provider for authentication types
         * @param authTypeSelectionTitle the title for the authentication type selection
         * @param authTypeSelectionDescription the description for the authentication type selection
         */
        protected LegacyAuthenticationModification(
            final Class<? extends EnumChoicesProvider<AuthTypes>> authTypeChoicesProvider,
            final String authTypeSelectionTitle, final String authTypeSelectionDescription) {
            m_authTypeSelectionTitle = authTypeSelectionTitle;
            m_authTypeSelectionDescription = authTypeSelectionDescription;
            m_authTypeChoicesProvider = authTypeChoicesProvider;
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            if (m_authTypeChoicesProvider != null) {
                group.find(SelectedTypeModRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(m_authTypeChoicesProvider)
                .modify();
            }
            if (m_authTypeSelectionTitle != null && m_authTypeSelectionDescription != null) {
                group.find(SelectedTypeModRef.class).modifyAnnotation(Widget.class)
                .withProperty("title", m_authTypeSelectionTitle)
                .withProperty("description", m_authTypeSelectionDescription)
                .modify();
            }
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

    /**
     * Persistor for credentials based on selected authentication type.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class CredentialsPersistor implements NodeParametersPersistor<Credentials> {

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
                case USER: {settings.addString(m_cfgUsername, param.getUsername());}
                case PWD: {settings.addPassword(m_cfgPassword, SECRET_KEY, param.getPassword());}
                case USER_PWD: {
                    settings.addString(m_cfgUsername, param.getUsername());
                    settings.addPassword(m_cfgPassword, SECRET_KEY, param.getPassword());
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
        @Label("No Authentication")
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
