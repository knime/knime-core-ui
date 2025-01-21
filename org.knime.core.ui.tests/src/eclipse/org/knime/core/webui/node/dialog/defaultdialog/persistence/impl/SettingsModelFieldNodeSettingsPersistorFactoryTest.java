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
 *   Dec 9, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistorContext;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.EnumSettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelDoublePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelIntegerPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelLongPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.AuthenticationType;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.AuthenticationSettings.SettingsModelAuthenticationMigrator;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // We allow assertions without messages
class SettingsModelPersistorTest {

    private static final String CFG_KEY = "test";

    private enum TestEnum {
            A, B, C;
    }

    static final class TestEnumSettingsModelStringPersistor extends EnumSettingsModelStringPersistor<TestEnum> {

        TestEnumSettingsModelStringPersistor(final NodeSettingsPersistorContext<TestEnum> context) {
            super(context);
        }

    }

    static final class EnumSettingsModelStringPersistorTestSettings implements PersistableSettingsWithComparableValue {

        EnumSettingsModelStringPersistorTestSettings() {
            // empty constructor required by contract
        }

        EnumSettingsModelStringPersistorTestSettings(final TestEnum value) {
            m_value = value;
        }

        @Persistor(TestEnumSettingsModelStringPersistor.class)
        TestEnum m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }

    }

    @Test
    void testEnumSettingsModelString() throws Exception {
        testSaveLoad(new EnumSettingsModelStringPersistorTestSettings(TestEnum.B));
        testSaveLoad(new EnumSettingsModelStringPersistorTestSettings(null));
    }

    static final class SettingsModelAuthenticationMigratorTestSettings
        implements PersistableSettingsWithComparableValue {

        SettingsModelAuthenticationMigratorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelAuthenticationMigratorTestSettings(final AuthenticationSettings settings) {
            m_value = settings;
        }

        static final class ValueMigrator extends SettingsModelAuthenticationMigrator {

            ValueMigrator() {
                super(CFG_KEY);
            }
        }

        @Migration(ValueMigrator.class)
        @Persist(configKey = CFG_KEY)
        AuthenticationSettings m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }

    }

    @Test
    void testAuthenticationSettingsSaveLoad() throws Exception {
        testSaveLoad(new SettingsModelAuthenticationMigratorTestSettings(new AuthenticationSettings()));
        testSaveLoad(new SettingsModelAuthenticationMigratorTestSettings(new AuthenticationSettings(
            AuthenticationSettings.AuthenticationType.USER_PWD, new Credentials("myUsername", "myPassword"))));
    }

    static Stream<Arguments> settingsModelAuthenticationLoadSource() {
        return Stream.of( //
            Arguments.of(SettingsModelAuthentication.AuthenticationType.PWD, AuthenticationType.PWD, "password", ""), //
            Arguments.of(SettingsModelAuthentication.AuthenticationType.USER_PWD, AuthenticationType.USER_PWD,
                "password", "username"), //
            Arguments.of(SettingsModelAuthentication.AuthenticationType.USER, AuthenticationType.USER, "", "username"), //
            Arguments.of(SettingsModelAuthentication.AuthenticationType.NONE, AuthenticationType.NONE, "", ""), //
            Arguments.of(SettingsModelAuthentication.AuthenticationType.KERBEROS, AuthenticationType.KERBEROS, "", ""));
    }

    @ParameterizedTest
    @MethodSource("settingsModelAuthenticationLoadSource")
    void testSettingsModelAuthenticationLoadLegacy(final SettingsModelAuthentication.AuthenticationType oldType,
        final AuthenticationType newType, final String password, final String username)
        throws InvalidSettingsException {
        final var savedSettings = new NodeSettings("node_settings");
        new SettingsModelAuthentication(CFG_KEY, oldType, username, password, null).saveSettingsTo(savedSettings);

        final var loaded =
            SettingsLoaderFactory.loadSettings(SettingsModelAuthenticationMigratorTestSettings.class, savedSettings);

        final var expected = new AuthenticationSettings(newType, new Credentials(username, password));
        assertEquals(expected, loaded.m_value);
    }

    static final class SettingsModelIntegerPersistorTestSettings implements PersistableSettingsWithComparableValue {

        SettingsModelIntegerPersistorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelIntegerPersistorTestSettings(final int value) {
            m_value = value;
        }

        static final class TestSettingsModelIntegerPersistor extends SettingsModelIntegerPersistor {
            TestSettingsModelIntegerPersistor() {
                super(CFG_KEY);
            }
        }

        @Persistor(TestSettingsModelIntegerPersistor.class)
        int m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }

    }

    @Test
    void testSettingsModelInteger() throws Exception {
        testSaveLoad(new SettingsModelIntegerPersistorTestSettings(42));
    }

    static final class SettingsModelStringPersistorTestSettings implements PersistableSettingsWithComparableValue {

        SettingsModelStringPersistorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelStringPersistorTestSettings(final String value) {
            m_value = value;
        }

        static final class TestSettingsModelStringPersistor extends SettingsModelStringPersistor {
            TestSettingsModelStringPersistor() {
                super(CFG_KEY);
            }
        }

        @Persistor(TestSettingsModelStringPersistor.class)
        String m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }

    }

    @Test
    void testSettingsModelString() throws InvalidSettingsException {
        testSaveLoad(new SettingsModelStringPersistorTestSettings("foobar"));
        testSaveLoad(new SettingsModelStringPersistorTestSettings(null));
    }

    static final class SettingsModelLongPersistorTestSettings implements PersistableSettingsWithComparableValue {

        SettingsModelLongPersistorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelLongPersistorTestSettings(final long value) {
            m_value = value;
        }

        static final class TestSettingsModelLongPersistor extends SettingsModelLongPersistor {
            TestSettingsModelLongPersistor() {
                super(CFG_KEY);
            }
        }

        @Persistor(TestSettingsModelLongPersistor.class)
        long m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }

    }

    @Test
    void testSettingsModelLong() throws Exception {
        testSaveLoad(new SettingsModelLongPersistorTestSettings(Long.MAX_VALUE));
    }

    static final class SettingsModelDoublePersistorTestSettings implements PersistableSettingsWithComparableValue {

        SettingsModelDoublePersistorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelDoublePersistorTestSettings(final double value) {
            m_value = value;
        }

        static final class TestSettingsModelDoublePersistor extends SettingsModelDoublePersistor {
            TestSettingsModelDoublePersistor() {
                super(CFG_KEY);
            }
        }

        @Persistor(TestSettingsModelDoublePersistor.class)
        double m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }
    }

    @Test
    void testSettingsModelDouble() throws Exception {
        testSaveLoad(new SettingsModelDoublePersistorTestSettings(13.37));
    }

    static final class SettingsModelBooleanPersistorTestSettings implements PersistableSettingsWithComparableValue {

        SettingsModelBooleanPersistorTestSettings() {
            // empty constructor required by contract
        }

        SettingsModelBooleanPersistorTestSettings(final boolean value) {
            m_value = value;
        }

        static final class TestSettingsModelBooleanPersistor extends SettingsModelBooleanPersistor {
            TestSettingsModelBooleanPersistor() {
                super(CFG_KEY);
            }
        }

        @Persistor(TestSettingsModelBooleanPersistor.class)
        boolean m_value;

        @Override
        public Object getCompareValue() {
            return m_value;
        }
    }

    @Test
    void testSettingsModelBoolean() throws Exception {
        testSaveLoad(new SettingsModelBooleanPersistorTestSettings(true));
        testSaveLoad(new SettingsModelBooleanPersistorTestSettings(false));
    }

    private static <T extends PersistableSettingsWithComparableValue> void testSaveLoad(final T value)
        throws InvalidSettingsException {
        var nodeSettings = new NodeSettings(CFG_KEY);
        SettingsSaverFactory.saveSettings((PersistableSettings)value, nodeSettings);
        var loaded = SettingsLoaderFactory.loadSettings(value.getClass(), nodeSettings);
        assertEquals(value.getCompareValue(), loaded.getCompareValue(),
            String.format("Should yield the initial value when saving and loading %s", value.getClass()));
    }

    interface PersistableSettingsWithComparableValue extends PersistableSettings {
        Object getCompareValue();
    }

}
