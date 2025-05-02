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
 *   Dec 7, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.DefaultFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;

@SuppressWarnings("java:S2698")
class OptionalPersistorTest {

    static class TestClass<T> implements PersistableSettings {

        Optional<T> m_testSetting;

    }

    private static final String CFG_KEY = "test";

    static final PersistTreeFactory FACTORY = new PersistTreeFactory();

    @SuppressWarnings("unchecked")
    static final <T> DefaultFieldPersistor<Optional<T>>
        createOptionalPersistor(final Class<? extends TestClass<T>> testClass) {

        final var tree = FACTORY.createTree(testClass);
        final var optionalTreeNode = (LeafNode<PersistableSettings>)tree.getChildByName("testSetting");

        return (DefaultFieldPersistor<Optional<T>>)DefaultFieldNodeSettingsPersistorFactory
            .createPersistor(optionalTreeNode, CFG_KEY);

    }

    private static <T> void testSaveLoadOptional(final Class<? extends TestClass<T>> type, final T value)
        throws InvalidSettingsException {
        assertEquals(Optional.of(value), saveLoad(type, Optional.of(value)));
        assertEquals(Optional.empty(), saveLoad(type, Optional.empty()));
    }

    private static <T> Optional<T> saveLoad(final Class<? extends TestClass<T>> type, final Optional<T> value)
        throws InvalidSettingsException {
        var persistor = createOptionalPersistor(type);
        var nodeSettings = new NodeSettings(CFG_KEY);
        persistor.save(value, nodeSettings);
        Optional<T> loaded = persistor.load(nodeSettings);
        return loaded;
    }

    static final class StringTestClass extends TestClass<String> {
    }

    @Test
    void testSaveLoadOptionalString() throws InvalidSettingsException {
        testSaveLoadOptional(StringTestClass.class, "test");
    }

    static final class IntTestClass extends TestClass<Integer> {
    }

    @Test
    void testSaveLoadOptionalInteger() throws InvalidSettingsException {
        testSaveLoadOptional(IntTestClass.class, 42);
    }

    static final class LongTestClass extends TestClass<Long> {
    }

    @Test
    void testSaveLoadOptionalLong() throws InvalidSettingsException {
        testSaveLoadOptional(LongTestClass.class, 123456789L);
    }

    static final class DoubleTestClass extends TestClass<Double> {
    }

    @Test
    void testSaveLoadOptionalDouble() throws InvalidSettingsException {
        testSaveLoadOptional(DoubleTestClass.class, 3.14);
    }

    static final class FloatTestClass extends TestClass<Float> {
    }

    @Test
    void testSaveLoadOptionalFloat() throws InvalidSettingsException {
        testSaveLoadOptional(FloatTestClass.class, 1.23f);
    }

    static final class ByteTestClass extends TestClass<Byte> {
    }

    @Test
    void testSaveLoadOptionalByte() throws InvalidSettingsException {
        testSaveLoadOptional(ByteTestClass.class, (byte)7);
    }

    static final class LocalDateTestClass extends TestClass<LocalDate> {
    }

    @Test
    void testSaveLoadOptionalLocalDate() throws InvalidSettingsException {
        testSaveLoadOptional(LocalDateTestClass.class, LocalDate.of(2024, 5, 2));
    }

    static final class LocalTimeTestClass extends TestClass<LocalTime> {
    }

    @Test
    void testSaveLoadOptionalLocalTime() throws InvalidSettingsException {
        testSaveLoadOptional(LocalTimeTestClass.class, LocalTime.of(10, 30));
    }

    static final class LocalDateTimeTestClass extends TestClass<LocalDateTime> {
    }

    @Test
    void testSaveLoadOptionalLocalDateTime() throws InvalidSettingsException {
        testSaveLoadOptional(LocalDateTimeTestClass.class, LocalDateTime.of(2024, 5, 2, 10, 30));
    }

    static final class ZonedDateTimeTestClass extends TestClass<ZonedDateTime> {
    }

    @Test
    void testSaveLoadOptionalZonedDateTime() throws InvalidSettingsException {
        testSaveLoadOptional(ZonedDateTimeTestClass.class, ZonedDateTime.now());
    }

    static final class ZoneIdTestClass extends TestClass<ZoneId> {
    }

    @Test
    void testSaveLoadOptionalZoneId() throws InvalidSettingsException {
        testSaveLoadOptional(ZoneIdTestClass.class, ZoneId.systemDefault());
    }

    static final class IntervalTestClass extends TestClass<Interval> {
    }

    @Test
    void testSaveLoadOptionalInterval() throws InvalidSettingsException {
        testSaveLoadOptional(IntervalTestClass.class, Interval.parseHumanReadable("1 year"));
    }

    static final class TimeIntervalTestClass extends TestClass<TimeInterval> {
    }

    @Test
    void testSaveLoadOptionalTimeInterval() throws InvalidSettingsException {
        testSaveLoadOptional(TimeIntervalTestClass.class, TimeInterval.of(0, 0, 0, 0));
    }

    static final class DateIntervalTestClass extends TestClass<DateInterval> {
    }

    @Test
    void testSaveLoadOptionalDateInterval() throws InvalidSettingsException {
        testSaveLoadOptional(DateIntervalTestClass.class, DateInterval.of(0, 0, 0, 0));
    }

    static final class StringArrayTestClass extends TestClass<String[]> {
    }

    @Test
    void testSaveLoadOptionalStringArray() throws InvalidSettingsException {
        final var value = new String[]{"one", "two"};
        assertArrayEquals(value, saveLoad(StringArrayTestClass.class, Optional.of(value)).orElseThrow());
    }

    enum MyEnum {
            VALUE1, VALUE2
    }

    static final class EnumTestClass extends TestClass<MyEnum> {
    }

    @Test
    void testSaveLoadOptionalEnum() throws InvalidSettingsException {
        testSaveLoadOptional(EnumTestClass.class, MyEnum.VALUE1);
    }

    static final class DataTypeTestClass extends TestClass<DataType> {
    }

    @Test
    void testSaveLoadOptionalDataType() throws InvalidSettingsException {
        testSaveLoadOptional(DataTypeTestClass.class, StringCell.TYPE);
    }

    static final class CredentialsTestClass extends TestClass<Credentials> {
    }

    @Test
    void testSaveLoadOptionalCredentials() throws InvalidSettingsException {
        testSaveLoadOptional(CredentialsTestClass.class, new Credentials("user", "pass"));
    }

}
