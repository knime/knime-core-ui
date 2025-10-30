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
 *   Dec 1, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.createSettingsSaver;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.saveSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;

/**
 * Tests for the {@link FieldBasedNodeSettingsPersistor}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S2698") // We allow assertions without messages
class FieldBasedNodeSettingsPersistorTest {

    private static final String ROOT_KEY = "Test";

    @Test
    void testFlatDefaultPersistance() throws InvalidSettingsException {
        var obj = new FlatNodeSettingsObject();
        obj.m_intSetting = 4;
        obj.m_doubleSetting = 3.4;
        obj.m_stringSetting = "foo";
        testSaveLoad(obj);
    }

    @Test
    void testFlatCustomKeysPersistance() throws InvalidSettingsException {
        var obj = new FlatCustomKeysNodeSettingsObject();
        obj.m_intSetting = 42;
        obj.m_longSetting = 32;
        obj.m_doubleSetting = 13.37;
        obj.m_stringSetting = "bar";

        testSaveLoad(obj);
    }

    @Test
    void testCustomFieldPersistor() throws InvalidSettingsException {
        var obj = new SettingsWithCustomFieldPersistor();
        obj.m_foo = "fuchs";
        testSaveLoad(obj);
    }

    private static <S extends TestNodeSettings> void testSaveLoad(final S obj) throws InvalidSettingsException {
        var expected = new NodeSettings(ROOT_KEY);
        obj.saveExpected(expected);

        var actual = new NodeSettings(ROOT_KEY);
        saveSettings(obj, actual);

        assertEquals(expected, actual, "The settings saved by the persistor are not as expected.");

        var loaded = loadSettings(obj.getClass(), expected);

        assertEquals(obj, loaded, "The loaded settings are not as expected");
    }

    @Test
    void testCustomPersistorWithFailingConstructor() {
        assertThrows(IllegalStateException.class,
            () -> loadSettings(FailingConstructorFieldPersistorSettings.class, null),
            "Custom persistor whose constructor fails was not detected.");
    }

    @Test
    void testPrivateConstructorCustomFieldPersistor() throws InvalidSettingsException {
        var obj = new PrivateConstructorPersistorSettings();
        obj.m_foo = "bar";
        testSaveLoad(obj);
    }

    @Test
    void testNestedSettings() throws InvalidSettingsException {
        var obj = new OuterNodeSettings();
        obj.m_bar = "baz";
        obj.m_inner.m_foo = "bal";
        testSaveLoad(obj);
    }

    @Test
    void testNestedSettignsWithCustomInnerPersistor() throws InvalidSettingsException {
        var obj = new OuterSettingsWithCustomPersistorInnerSettings();
        obj.m_bar = "bimms";
        obj.m_inner.m_foo = "bamms";
        testSaveLoad(obj);
    }

    @Test
    void testStaticFieldsAreIgnored() throws InvalidSettingsException {
        testSaveLoad(new SettingsWithStaticField());
    }

    @Test
    void testStaticFinalFieldsAreIgnored() throws InvalidSettingsException {
        testSaveLoad(new SettingsWithStaticFinalField());
    }

    @Test
    void testArraySettings() throws InvalidSettingsException {
        var arraySettings = new AllAllowedTypesArraySettings();
        arraySettings.m_bar = new ElementSettings[3];
        for (int i = 0; i < 3; i++) {
            var element = new ElementSettings();
            element.m_foo = "baz" + i;
            arraySettings.m_bar[i] = element;
        }
        arraySettings.m_baz = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            var element = new ElementSettings();
            element.m_foo = "baz" + i;
            arraySettings.m_baz.add(element);
        }
        arraySettings.m_bimms = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var element = new ElementSettings();
            element.m_foo = "bimms" + i;
            arraySettings.m_bimms.add(element);
        }
        arraySettings.m_bams = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            var element = new ElementSettings();
            element.m_foo = "bams" + i;
            arraySettings.m_bams.add(element);
        }
        testSaveLoad(arraySettings);
    }

    @Test
    void testThrowsOnOtherCollections() throws InvalidSettingsException {
        var settings = new NonAllowedCollectionsSettigns();

        settings.m_bar = Set.of(new ElementSettings());
        assertThat(assertThrows(IllegalStateException.class, () -> testSaveLoad(settings),
            "Persistor should not allow non-allowed collections.")).message().contains("Set");
    }

    @Test
    void testArraySettingsWithInvalidKeys() throws InvalidSettingsException {
        var saved = new NodeSettings(ROOT_KEY);
        saved.addNodeSettings("bar").addBoolean("null_Internal", true);
        var loaded = loadSettings(ArraySettings.class, saved);
        assertArrayEquals(new ElementSettings[0], loaded.m_bar);
    }

    @Test
    void testSaveNullArraySettings() throws InvalidSettingsException {
        final var saver = createSettingsSaver(ArraySettings.class);
        var root = new NodeSettings(ROOT_KEY);
        assertThrows(NullPointerException.class, () -> saver.save(null, root));
    }

    @Test
    void testDynamicParameterInterface() throws InvalidSettingsException {
        testSaveLoad(new SettingsWithParametersInterface());
        final var settingsWithNullValue = new SettingsWithParametersInterface();
        settingsWithNullValue.m_params = null;
        testSaveLoad(settingsWithNullValue);
    }

    private interface TestNodeSettings extends NodeParameters {

        void saveExpected(final NodeSettingsWO settings);

        @Override
        boolean equals(Object obj);

        @Override
        int hashCode();
    }

    private abstract static class AbstractTestNodeSettings<S extends AbstractTestNodeSettings<S>>
        implements TestNodeSettings {

        @SuppressWarnings("unchecked")
        @Override
        public final boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass().equals(obj.getClass())) {
                return equalSettings((S)obj);
            } else {
                return false;
            }
        }

        @Override
        public final int hashCode() {
            return computeHashCode();
        }

        protected abstract int computeHashCode();

        protected abstract boolean equalSettings(final S settings);
    }

    static final class FlatNodeSettingsObject extends AbstractTestNodeSettings<FlatNodeSettingsObject> {

        int m_intSetting;

        double m_doubleSetting;

        long m_longSetting;

        String m_stringSetting;

        // omit the m_ to test if settings without m_ prefix also work
        boolean booleanSetting; //NOSONAR

        private String privateSetting = "test";

        String getSetting() {
            return "test";
        }

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("intSetting", m_intSetting);
            settings.addDouble("doubleSetting", m_doubleSetting);
            settings.addLong("longSetting", m_longSetting);
            settings.addString("stringSetting", m_stringSetting);
            settings.addBoolean("booleanSetting", booleanSetting);
        }

        @Override
        protected boolean equalSettings(final FlatNodeSettingsObject settings) {
            return m_intSetting == settings.m_intSetting//
                && m_doubleSetting == settings.m_doubleSetting//
                && m_longSetting == settings.m_longSetting//
                && Objects.equals(m_stringSetting, settings.m_stringSetting)//
                && booleanSetting == settings.booleanSetting//
                && privateSetting == settings.privateSetting;
        }

        @Override
        public int computeHashCode() {
            return Objects.hash(m_intSetting, m_longSetting, m_doubleSetting, m_stringSetting, booleanSetting);
        }

    }

    static final class FlatCustomKeysNodeSettingsObject
        extends AbstractTestNodeSettings<FlatCustomKeysNodeSettingsObject> {

        @Persist(configKey = "my_int_setting")
        int m_intSetting;

        @Persist(configKey = "my_double_setting")
        double m_doubleSetting;

        @Persist(configKey = "my_long_setting")
        long m_longSetting;

        @Persist(configKey = "my_string_setting")
        String m_stringSetting;

        @Persist(configKey = "my_boolean_setting")
        boolean m_booleanSetting;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("my_int_setting", m_intSetting);
            settings.addDouble("my_double_setting", m_doubleSetting);
            settings.addLong("my_long_setting", m_longSetting);
            settings.addString("my_string_setting", m_stringSetting);
            settings.addBoolean("my_boolean_setting", m_booleanSetting);
        }

        @Override
        protected boolean equalSettings(final FlatCustomKeysNodeSettingsObject settings) {
            return m_intSetting == settings.m_intSetting//
                && m_doubleSetting == settings.m_doubleSetting//
                && m_longSetting == settings.m_longSetting//
                && Objects.equals(m_stringSetting, settings.m_stringSetting)//
                && m_booleanSetting == settings.m_booleanSetting;
        }

        @Override
        public int computeHashCode() {
            return Objects.hash(m_intSetting, m_longSetting, m_doubleSetting, m_stringSetting, m_booleanSetting);
        }

    }

    enum TestEnum {
            FOO, BAR, BAZ;
    }

    private static final class SettingsWithCustomFieldPersistor
        extends AbstractTestNodeSettings<SettingsWithCustomFieldPersistor> {

        @Persistor(CustomFieldPersistor.class)
        String m_foo;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addString("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hashCode(m_foo);
        }

        @Override
        protected boolean equalSettings(final SettingsWithCustomFieldPersistor settings) {
            return Objects.equals(m_foo, settings.m_foo);
        }
    }

    private static final class CustomFieldPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString("foo");
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            settings.addString("foo", value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"foo"}};
        }
    }

    private static final class FailingConstructorFieldPersistor implements NodeParametersPersistor<String> {
        @SuppressWarnings("unused")
        public FailingConstructorFieldPersistor() {
            throw new IllegalArgumentException("Failing constructor.");
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            throw new NotImplementedException("This method should not be called.");
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            throw new NotImplementedException("This method should not be called.");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][];
        }
    }

    private static final class FailingConstructorFieldPersistorSettings implements NodeParameters {
        @Persistor(FailingConstructorFieldPersistor.class)
        String m_foo;
    }

    private static final class PrivateConstructorPersistor implements NodeParametersPersistor<String> {
        private static final String CONFIG_KEY = "foo";

        private PrivateConstructorPersistor() {
            // make private to provoke an access exception
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString(CONFIG_KEY);
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, obj);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }

    private static final class PrivateConstructorPersistorSettings
        extends AbstractTestNodeSettings<PrivateConstructorPersistorSettings> {
        @Persistor(PrivateConstructorPersistor.class)
        String m_foo;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addString("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hashCode(m_foo);
        }

        @Override
        protected boolean equalSettings(final PrivateConstructorPersistorSettings settings) {
            return Objects.equals(m_foo, settings.m_foo);
        }
    }

    private static final class InnerNodeSettings extends AbstractTestNodeSettings<InnerNodeSettings> {

        String m_foo;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addString("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hashCode(m_foo);
        }

        @Override
        protected boolean equalSettings(final InnerNodeSettings settings) {
            return Objects.equals(m_foo, settings.m_foo);
        }
    }

    private static final class OuterNodeSettings extends AbstractTestNodeSettings<OuterNodeSettings> {

        InnerNodeSettings m_inner = new InnerNodeSettings();

        String m_bar;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            m_inner.saveExpected(settings.addNodeSettings("inner"));
            settings.addString("bar", m_bar);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_inner, m_bar);
        }

        @Override
        protected boolean equalSettings(final OuterNodeSettings settings) {
            return Objects.equals(m_inner, settings.m_inner) && Objects.equals(m_bar, settings.m_bar);
        }
    }

    private static final class SettingsWithStaticField extends AbstractTestNodeSettings<SettingsWithStaticField> {

        @SuppressWarnings("unused")
        private static String STATIC_FIELD = "foo"; //NOSONAR

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            // no settings
        }

        @Override
        protected int computeHashCode() {
            return 0;
        }

        @Override
        protected boolean equalSettings(final SettingsWithStaticField settings) {
            return true;
        }

    }

    @Persistor(InnerSettingsWithCustomPersistor.CustomPersistor.class)
    private static final class InnerSettingsWithCustomPersistor
        extends AbstractTestNodeSettings<InnerSettingsWithCustomPersistor> {

        String m_foo;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addString("custom_foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hashCode(m_foo);
        }

        @Override
        protected boolean equalSettings(final InnerSettingsWithCustomPersistor settings) {
            return Objects.equals(m_foo, settings.m_foo);
        }

        private static final class CustomPersistor
            implements NodeParametersPersistor<InnerSettingsWithCustomPersistor> {

            @Override
            public InnerSettingsWithCustomPersistor load(final NodeSettingsRO settings)
                throws InvalidSettingsException {
                var loaded = new InnerSettingsWithCustomPersistor();
                loaded.m_foo = settings.getString("custom_foo");
                return loaded;
            }

            @Override
            public void save(final InnerSettingsWithCustomPersistor obj, final NodeSettingsWO settings) {
                settings.addString("custom_foo", obj.m_foo);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }

        }

    }

    private static final class SettingsWithStaticFinalField
        extends AbstractTestNodeSettings<SettingsWithStaticFinalField> {

        @SuppressWarnings("unused")
        private static final String STATIC_FINAL_FIELD = "bar";

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            // no settings
        }

        @Override
        protected int computeHashCode() {
            return 0;
        }

        @Override
        protected boolean equalSettings(final SettingsWithStaticFinalField settings) {
            return true;
        }

    }

    private static final class OuterSettingsWithCustomPersistorInnerSettings
        extends AbstractTestNodeSettings<OuterSettingsWithCustomPersistorInnerSettings> {

        @Persist(configKey = "my-inner")
        InnerSettingsWithCustomPersistor m_inner = new InnerSettingsWithCustomPersistor();

        String m_bar;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            m_inner.saveExpected(settings.addNodeSettings("my-inner"));
            settings.addString("bar", m_bar);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_inner, m_bar);
        }

        @Override
        protected boolean equalSettings(final OuterSettingsWithCustomPersistorInnerSettings settings) {
            return Objects.equals(m_bar, settings.m_bar) && Objects.equals(m_inner, settings.m_inner);
        }
    }

    private static final class ElementSettings extends AbstractTestNodeSettings<ElementSettings> {

        String m_foo;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addString("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_foo);
        }

        @Override
        protected boolean equalSettings(final ElementSettings settings) {
            return Objects.equals(m_foo, settings.m_foo);
        }

    }

    private static final class ArraySettings extends AbstractTestNodeSettings<ArraySettings> {

        ElementSettings[] m_bar;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            var barSettings = settings.addNodeSettings("bar");
            for (int i = 0; i < m_bar.length; i++) {
                m_bar[i].saveExpected(barSettings.addNodeSettings(Integer.toString(i)));
            }

        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_bar);
        }

        @Override
        protected boolean equalSettings(final ArraySettings settings) {
            return Objects.deepEquals(m_bar, settings.m_bar);
        }

    }

    private static final class AllAllowedTypesArraySettings
        extends AbstractTestNodeSettings<AllAllowedTypesArraySettings> {

        ElementSettings[] m_bar;

        List<ElementSettings> m_baz;

        Collection<ElementSettings> m_bimms;

        ArrayList<ElementSettings> m_bams;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            var barSettings = settings.addNodeSettings("bar");
            for (int i = 0; i < m_bar.length; i++) {
                m_bar[i].saveExpected(barSettings.addNodeSettings(Integer.toString(i)));
            }
            var bazSettings = settings.addNodeSettings("baz");
            for (int i = 0; i < m_baz.size(); i++) {
                m_baz.get(i).saveExpected(bazSettings.addNodeSettings(Integer.toString(i)));
            }
            var bimmsSettings = settings.addNodeSettings("bimms");
            final var bimmsAsList = new ArrayList<>(m_bimms);
            for (int i = 0; i < bimmsAsList.size(); i++) {
                bimmsAsList.get(i).saveExpected(bimmsSettings.addNodeSettings(Integer.toString(i)));
            }
            var bamsSettings = settings.addNodeSettings("bams");
            for (int i = 0; i < m_bams.size(); i++) {
                m_bams.get(i).saveExpected(bamsSettings.addNodeSettings(Integer.toString(i)));
            }

        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_bar, m_baz, m_bimms, m_bams);
        }

        @Override
        protected boolean equalSettings(final AllAllowedTypesArraySettings settings) {
            return Objects.deepEquals(m_bar, settings.m_bar) && m_baz.equals(settings.m_baz)
                && m_bimms.equals(settings.m_bimms) && m_bams.equals(settings.m_bams);
        }

    }

    private static final class NonAllowedCollectionsSettigns
        extends AbstractTestNodeSettings<NonAllowedCollectionsSettigns> {

        Set<ElementSettings> m_bar;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            var barSettings = settings.addNodeSettings("bar");
            var barAsList = new ArrayList<>(m_bar);
            for (int i = 0; i < barAsList.size(); i++) {
                barAsList.get(i).saveExpected(barSettings.addNodeSettings(Integer.toString(i)));
            }
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_bar);
        }

        @Override
        protected boolean equalSettings(final NonAllowedCollectionsSettigns settings) {
            throw new UnsupportedOperationException(
                "This method should not be called, as the persistor does not support collections.");
        }
    }

    interface MyParameters extends DynamicParameters.DynamicNodeParameters {

    }

    static class MyParametersImpl implements MyParameters {

        int m_foo = 42;
    }

    @DynamicParameters.OriginalClassName(MyOtherParametersImpl.ORIGINAL_CLASS_NAME)
    static class MyOtherParametersImpl implements MyParameters {

        static final String ORIGINAL_CLASS_NAME = "some.other.OriginalClassName";

        String m_bar = "test";
    }

    static final class SettingsWithParametersInterface implements TestNodeSettings {
        @DynamicParameters(MyParametersProvider.class)
        MyParameters m_params = new MyParametersImpl();

        @DynamicParameters(MyParametersProvider.class)
        MyParameters m_otherParams = new MyOtherParametersImpl();

        /**
         * {@inheritDoc}
         */
        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            final var params = settings.addNodeSettings("params");
            if (m_params == null) {
                params.addString("@class", null);
            } else {
                params.addString("@class", m_params.getClass().getName());
                params.addInt("foo", ((MyParametersImpl)m_params).m_foo);
            }

            final var otherParams = settings.addNodeSettings("otherParams");
            otherParams.addString("@class", MyOtherParametersImpl.ORIGINAL_CLASS_NAME);
            otherParams.addString("bar", ((MyOtherParametersImpl)m_otherParams).m_bar);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(paramHash(m_params), paramHash(m_otherParams));
        }

        private static int paramHash(final Object p) {
            if (p instanceof MyParametersImpl mp) {
                return Integer.hashCode(mp.m_foo);
            }
            if (p instanceof MyOtherParametersImpl mo) {
                return java.util.Objects.hashCode(mo.m_bar);
            }
            return 0;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SettingsWithParametersInterface other)) {
                return false;
            }
            return equalParam(m_params, other.m_params) && equalParam(m_otherParams, other.m_otherParams);
        }

        private static boolean equalParam(final Object a, final Object b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            if (a.getClass() != b.getClass()) {
                return false;
            }

            if (a instanceof MyParametersImpl pA && b instanceof MyParametersImpl pB) {
                return pA.m_foo == pB.m_foo;
            }
            if (a instanceof MyOtherParametersImpl pA && b instanceof MyOtherParametersImpl pB) {
                return java.util.Objects.equals(pA.m_bar, pB.m_bar);
            }
            return false;
        }
    }

    static final class MyParametersProvider implements DynamicParameters.DynamicParametersProvider<MyParameters> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            throw new UnsupportedOperationException("Not used in tests");
        }

        static final List<Class<? extends MyParameters>> SUPPORTED_CLASSES =
            List.of(MyParametersImpl.class, MyOtherParametersImpl.class);



        @Override
        public ClassIdStrategy<MyParameters> getClassIdStrategy() {
            return new DefaultClassIdStrategy<>(SUPPORTED_CLASSES);
        }

        @Override
        public MyParameters computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            throw new UnsupportedOperationException("Not used in tests");
        }

    }

    @Test
    void testOptionalSettings() throws InvalidSettingsException {
        var optionalSettings = new OptionalSettings();
        optionalSettings.m_foo = 13;
        testSaveLoad(optionalSettings);

        var nodeSettings = new NodeSettings(ROOT_KEY);
        var loadedSettings = loadSettings(OptionalSettings.class, nodeSettings);
        assertEquals(new OptionalSettings(), loadedSettings);
    }

    private static final class OptionalSettings extends AbstractTestNodeSettings<OptionalSettings> {

        @Migrate(loadDefaultIfAbsent = true)
        int m_foo = 42;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return m_foo;
        }

        @Override
        protected boolean equalSettings(final OptionalSettings settings) {
            return m_foo == settings.m_foo;
        }

    }

    @Test
    void testOptionalSettingsForAllAbsentFields() throws InvalidSettingsException {
        var optionalSettings = new OptionalSettingsForEachAbsentField();
        optionalSettings.m_foo = 13;
        optionalSettings.m_bar = "other string";
        testSaveLoad(optionalSettings);

        var nodeSettings = new NodeSettings(ROOT_KEY);
        var loadedSettings = loadSettings(OptionalSettingsForEachAbsentField.class, nodeSettings);
        assertEquals(new OptionalSettingsForEachAbsentField(), loadedSettings);
    }

    @LoadDefaultsForAbsentFields
    private static final class OptionalSettingsForEachAbsentField
        extends AbstractTestNodeSettings<OptionalSettingsForEachAbsentField> {

        int m_foo = 42;

        String m_bar = "some string";

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("foo", m_foo);
            settings.addString("bar", m_bar);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_foo, m_bar);
        }

        @Override
        protected boolean equalSettings(final OptionalSettingsForEachAbsentField settings) {
            return m_foo == settings.m_foo && m_bar.equals(settings.m_bar);
        }
    }

    @Test
    void testSettingsWithDefaultProvider() throws InvalidSettingsException {
        var nodeSettings = new NodeSettings(ROOT_KEY);
        var loaded = loadSettings(DefaultProviderSettings.class, nodeSettings);
        assertEquals(1337, loaded.m_foo);
    }

    private static final class DefaultProviderSettings extends AbstractTestNodeSettings<DefaultProviderSettings> {

        @Migration(FooDefaultProvider.class)
        int m_foo = 42;

        private static final class FooDefaultProvider implements DefaultProvider<Integer> {

            @Override
            public Integer getDefault() {
                return 1337;
            }

        }

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("foo", m_foo);
        }

        @Override
        protected int computeHashCode() {
            return m_foo;
        }

        @Override
        protected boolean equalSettings(final DefaultProviderSettings settings) {
            return m_foo == settings.m_foo;
        }

    }

    @Test
    void throwsIfPersistorAndPersistAreUsedAtTheSameTime() {
        assertThat(assertThrows(IllegalStateException.class,
            () -> loadSettings(PersistAndPersistorUsedAtTheSameTime.class, null))).hasMessageContaining("Persistor",
                "Persist", "fieldName");
    }

    static final class PersistAndPersistorUsedAtTheSameTime implements NodeParameters {

        static final class PersistorClass implements NodeParametersPersistor<Integer> {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                throw new IllegalStateException("not used by tests");
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                throw new IllegalStateException("not used by tests");
            }

            @Override
            public String[][] getConfigPaths() {
                throw new IllegalStateException("not used by tests");
            }
        }

        @Persistor(PersistorClass.class)
        @Persist(configKey = "bar")
        int m_fieldName;
    }

    @Test
    void throwsIfMigrationAndMigrateAreUsedAtTheSameTime() {
        assertThat(assertThrows(IllegalStateException.class,
            () -> loadSettings(MigrationAndMigrateUsedAtTheSameTime.class, null))).hasMessageContaining("Migration",
                "Migrate", "fieldName");
    }

    static final class MigrationAndMigrateUsedAtTheSameTime implements NodeParameters {

        static final class MigratorClass implements NodeParametersMigration<Integer> {

            @Override
            public List<ConfigMigration<Integer>> getConfigMigrations() {
                throw new IllegalStateException("not used by tests");

            }

        }

        @Migration(MigratorClass.class)
        @Migrate(loadDefaultIfAbsent = true)
        int m_fieldName;
    }

    @Test
    void throwsIfPersistorWithEmptyConfigPathsAndMigrateAreUsedAtTheSameTime() {
        assertThrows(IllegalStateException.class,
            () -> loadSettings(PersistorWithEmptyConfigPathsAndMigrate.class, null));
    }

    static final class PersistorWithEmptyConfigPathsAndMigrate implements NodeParameters {

        static final class EmptyConfigPathsPersistor implements NodeParametersPersistor<Integer> {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }

        @Persistor(EmptyConfigPathsPersistor.class)
        @Migrate(loadDefaultIfAbsent = true)
        int m_fieldName;
    }

    @Test
    void allowsPersistorWithEmptyConfigPathsAndMigrationWithDeprecatedKeys() throws InvalidSettingsException {
        // This should NOT throw an exception since the migration has deprecated config keys
        var obj = new PersistorWithEmptyConfigPathsAndMigration();
        obj.m_fieldName = 123;
        testSaveLoad(obj);
    }

    static final class PersistorWithEmptyConfigPathsAndMigration extends AbstractTestNodeSettings<PersistorWithEmptyConfigPathsAndMigration> {

        static final class EmptyConfigPathsPersistor implements NodeParametersPersistor<Integer> {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getInt("fieldName", 0);
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                settings.addInt("fieldName", obj);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }

        static final class MigrationWithDeprecatedKeys implements NodeParametersMigration<Integer> {

            @Override
            public List<ConfigMigration<Integer>> getConfigMigrations() {
                return List.of(ConfigMigration.builder(settings -> 42).withDeprecatedConfigPath("deprecated").build());
            }
        }

        @Persistor(EmptyConfigPathsPersistor.class)
        @Migration(MigrationWithDeprecatedKeys.class)
        int m_fieldName;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            settings.addInt("fieldName", m_fieldName);
        }

        @Override
        protected int computeHashCode() {
            return Objects.hash(m_fieldName);
        }

        @Override
        protected boolean equalSettings(final PersistorWithEmptyConfigPathsAndMigration settings) {
            return m_fieldName == settings.m_fieldName;
        }
    }

    @Test
    void throwsIfPersistorWithEmptyConfigPathsAndMigrationWithoutDeprecatedKeysAreUsedAtTheSameTime() {
        assertThrows(IllegalStateException.class,
            () -> loadSettings(PersistorWithEmptyConfigPathsAndMigrationWithoutDeprecatedKeys.class, null));
    }

    static final class PersistorWithEmptyConfigPathsAndMigrationWithoutDeprecatedKeys implements NodeParameters {

        static final class EmptyConfigPathsPersistor implements NodeParametersPersistor<Integer> {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                throw new UnsupportedOperationException("not used by tests");
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }

        static final class MigrationWithoutDeprecatedKeys implements NodeParametersMigration<Integer> {

            @Override
            public List<ConfigMigration<Integer>> getConfigMigrations() {
                // Migration without deprecated config keys - just using builder without withDeprecatedConfigPath
                return List.of(ConfigMigration.builder(settings -> 42).build());
            }
        }

        @Persistor(EmptyConfigPathsPersistor.class)
        @Migration(MigrationWithoutDeprecatedKeys.class)
        int m_fieldName;
    }



    @Test
    void testPersistorWithEmptyConfigPathsAndLoadDefaultForAbsentFields() throws InvalidSettingsException {
        // Test that no default should be loaded when @LoadDefaultsForAbsentFields is used with empty config paths
        var nodeSettings = new NodeSettings(ROOT_KEY);
        nodeSettings.addInt("fieldName", 84);
        var loaded = loadSettings(PersistorWithEmptyConfigPathsAndLoadDefault.class, nodeSettings);
        // The value from settings (84) should be loaded rather than the default constructor value (42)
        assertEquals(84, loaded.m_fieldName);
    }

    @LoadDefaultsForAbsentFields
    static final class PersistorWithEmptyConfigPathsAndLoadDefault implements NodeParameters {

        static final class EmptyConfigPathsPersistor implements NodeParametersPersistor<Integer> {

            @Override
            public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getInt("fieldName", 0);
            }

            @Override
            public void save(final Integer obj, final NodeSettingsWO settings) {
                settings.addInt("fieldName", obj);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][];
            }
        }

        @Persistor(EmptyConfigPathsPersistor.class)
        int m_fieldName = 42; // Default value that should NOT be loaded
    }

}
