/*
 * ------------------------------------------------------------------------
 *
 *  Website: http://www.knime.com; Email: contact@knime.com
 *  Copyright by KNIME AG, Zurich, Switzerland
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil.createInstance;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.RequiresPasswordProvider;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.RequiresUsernameProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.TestStateProviderInitializer;
import org.knime.node.parameters.updates.ParameterReference;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("java:S2698") // we accept assertions without messages
class AuthenticationSettingsTest {

    @Test
    void testEqualsHashCodeContracts() {
        EqualsVerifier.forClass(AuthenticationSettings.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }

    static class CredentialsStateProviderInitializer extends TestStateProviderInitializer {

        private final AuthenticationType m_type;

        private boolean m_computedBeforeOpenDialog;

        CredentialsStateProviderInitializer(final AuthenticationSettings.AuthenticationType type) {
            m_type = type;
        }

        @Override
        public void computeBeforeOpenDialog() {
            m_computedBeforeOpenDialog = true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Supplier<T> computeFromValueSupplier(final Class<? extends ParameterReference<T>> ref) {
            return () -> (T)m_type;
        }

        public boolean isComputedBeforeOpenDialog() {
            return m_computedBeforeOpenDialog;
        }

    }

    static Stream<Arguments> requiresPasswordSource() {
        return Stream.of( //
            Arguments.of(AuthenticationType.PWD, true), //
            Arguments.of(AuthenticationType.USER_PWD, true), //
            Arguments.of(AuthenticationType.KERBEROS, false), //
            Arguments.of(AuthenticationType.NONE, false), //
            Arguments.of(AuthenticationType.USER, false) //
        );
    }

    @ParameterizedTest
    @MethodSource("requiresPasswordSource")
    void testRequiresPasswordProvider(final AuthenticationType type, final boolean expected) {
        final var requiresPasswordProvider = createInstance(RequiresPasswordProvider.class);
        final var initializer = new CredentialsStateProviderInitializer(type);
        requiresPasswordProvider.init(initializer);
        assertThat(requiresPasswordProvider.computeState(null)).isEqualTo(expected);
        assertThat(initializer.isComputedBeforeOpenDialog()).isTrue();
    }

    static Stream<Arguments> requiresUsernameSource() {
        return Stream.of( //
            Arguments.of(AuthenticationType.USER, true), //
            Arguments.of(AuthenticationType.USER_PWD, true), //
            Arguments.of(AuthenticationType.KERBEROS, false), //
            Arguments.of(AuthenticationType.NONE, false), //
            Arguments.of(AuthenticationType.PWD, false) //
        );
    }

    @ParameterizedTest
    @MethodSource("requiresUsernameSource")
    void testRequiresUsernameProvider(final AuthenticationType type, final boolean expected) {
        final var requiresUsernameProvider = createInstance(RequiresUsernameProvider.class);
        final var initializer = new CredentialsStateProviderInitializer(type);
        requiresUsernameProvider.init(initializer);
        assertThat(requiresUsernameProvider.computeState(null)).isEqualTo(expected);
        assertThat(initializer.isComputedBeforeOpenDialog()).isTrue();
    }
}
