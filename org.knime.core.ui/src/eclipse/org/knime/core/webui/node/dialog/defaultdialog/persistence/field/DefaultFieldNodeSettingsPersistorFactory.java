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
 *   Dec 4, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.field;

import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.DateIntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.IntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.LocalDatePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.LocalTimePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.TimeIntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DateTimePersistorUtils.TimeZonePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * Factory for default persistors either for arrays or {@link PersistableSettings} or settings that store values
 * directly in NodeSettings.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultFieldNodeSettingsPersistorFactory {

    /**
     * This method can be used in a {@link Persist#customPersistor()} for additional adaptations of the default
     * persistors either for arrays or {@link PersistableSettings} or settings that store values directly in
     * NodeSettings.
     *
     * @param node the node associated to the field the created persistor should persist
     * @param configKey the key to use for storing and retrieving the value to and from the NodeSettings
     * @return a new persistor
     * @throws IllegalArgumentException if there is no persistor available for the provided fieldType
     */
    static FieldNodeSettingsPersistorWithInferredConfigs<?>
        createDefaultPersistor(final TreeNode<PersistableSettings> node, final String configKey) {
        if (node instanceof ArrayParentNode<PersistableSettings> array) {
            return createDefaultArrayPersistor(array.getElementTree(), configKey);
        } else if (node instanceof Tree<PersistableSettings> tree) {
            return createNestedFieldBasedPersistor(configKey, tree);
        } else {
            return createPersistor(node.getType(), configKey);
        }
    }

    private static <S extends PersistableSettings> ArrayFieldPersistor<S>
        createDefaultArrayPersistor(final Tree<PersistableSettings> elementTree, final String configKey) {
        return new ArrayFieldPersistor<>(elementTree, configKey);
    }

    private static NestedPersistor<?> createNestedFieldBasedPersistor(final String configKey,
        final Tree<PersistableSettings> tree) {
        return new NestedPersistor<>(configKey, NodeSettingsPersistorFactory.createPersistor(tree));
    }

    static final class NestedPersistor<S extends PersistableSettings>
        implements FieldNodeSettingsPersistorWithInferredConfigs<S> {

        private final String m_configKey;

        private final NodeSettingsPersistor<S> m_delegate;

        NestedPersistor(final String configKey, final NodeSettingsPersistor<S> delegate) {
            m_configKey = configKey;
            m_delegate = delegate;
        }

        @Override
        public S load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return m_delegate.load(settings.getNodeSettings(m_configKey));
        }

        @Override
        public void save(final S obj, final NodeSettingsWO settings) {
            m_delegate.save(obj, settings.addNodeSettings(m_configKey));
        }

        @Override
        public ConfigMappings getConfigMappings(final S obj) {
            return new ConfigMappings(m_configKey, List.of(m_delegate.getConfigMappings(obj)));
        }

        @Override
        public String getConfigKey() {
            return m_configKey;
        }

    }

    static final class ArrayFieldPersistor<S extends PersistableSettings>
        implements FieldNodeSettingsPersistorWithInferredConfigs<S[]> {

        private final String m_configKey;

        private final Tree<PersistableSettings> m_elementTree;

        private final List<NodeSettingsPersistor<S>> m_persistors = new ArrayList<>();

        private static final Pattern IS_DIGIT = Pattern.compile("^\\d+$");

        ArrayFieldPersistor(final Tree<PersistableSettings> elementTree, final String configKey) {
            m_configKey = configKey;
            m_elementTree = elementTree;
        }

        @Override
        public S[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var arraySettings = settings.getNodeSettings(m_configKey);
            int size = (int)arraySettings.keySet().stream().filter(s -> IS_DIGIT.matcher(s).matches()).count();
            ensureEnoughPersistors(size);
            @SuppressWarnings("unchecked")
            var values = (S[])Array.newInstance(m_elementTree.getType(), size);
            for (int i = 0; i < size; i++) {//NOSONAR
                values[i] = m_persistors.get(i).load(arraySettings);
            }
            return values;
        }

        private synchronized void ensureEnoughPersistors(final int numPersistors) {
            for (int i = m_persistors.size(); i < numPersistors; i++) {
                m_persistors
                    .add((NodeSettingsPersistor<S>)createNestedFieldBasedPersistor(Integer.toString(i), m_elementTree));
            }
        }

        @Override
        public void save(final S[] array, final NodeSettingsWO settings) {
            ensureEnoughPersistors(array.length);
            var arraySettings = settings.addNodeSettings(m_configKey);
            for (int i = 0; i < array.length; i++) {//NOSONAR
                m_persistors.get(i).save(array[i], arraySettings);
            }
        }

        @Override
        public ConfigMappings getConfigMappings(final S[] array) {
            ensureEnoughPersistors(array.length);
            return new ConfigMappings(m_configKey, IntStream.range(0, array.length)
                .mapToObj(i -> m_persistors.get(i).getConfigMappings(array[i])).toList());
        }

        @Override
        public String getConfigKey() {
            return m_configKey;
        }

    }

    private static final Map<Class<?>, FieldPersistor<?>> IMPL_MAP = Stream.of(PersistorImpl.values())//
        .collect(toMap(PersistorImpl::getFieldType, PersistorImpl::getFieldPersistor));

    /**
     * Creates a persistor for the provided type that uses the configKey to store and retrieve the value.
     *
     * @param <T> the type of field
     * @param fieldType the type of field the created persistor should persist
     * @param configKey the key to use for storing and retrieving the value to and from the NodeSettings
     * @return a new persistor
     * @throws IllegalArgumentException if there is no persistor available for the provided fieldType
     */
    public static <T> FieldNodeSettingsPersistorWithInferredConfigs<T> createPersistor(final Class<T> fieldType,
        final String configKey) {
        @SuppressWarnings("unchecked") // Type-save since IMPL_MAP maps Class<T> to FieldPersistor<T>
        var impl = (FieldPersistor<T>)IMPL_MAP.get(fieldType);
        return createPersistorFromImpl(fieldType, configKey, impl);
    }

    @SuppressWarnings("unchecked")
    private static <T> FieldNodeSettingsPersistorWithInferredConfigs<T>
        createPersistorFromImpl(final Class<T> fieldType, final String configKey, final FieldPersistor<T> impl) {
        if (impl != null) {
            return new DefaultFieldNodeSettingsPersistor<>(configKey, impl);
        } else if (fieldType.isEnum()) {
            return createEnumPersistor(configKey, fieldType);
        } else if (fieldType.equals(LocalDate.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createLocalDatePersistor(configKey);
        } else if (fieldType.equals(LocalTime.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createLocalTimePersistor(configKey);
        } else if (fieldType.equals(ZoneId.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createTimeZonePersistor(configKey);
        } else if (fieldType.equals(Interval.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createIntervalPersistor(configKey);
        } else if (fieldType.equals(DateInterval.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createDateIntervalPersistor(configKey);
        } else if (fieldType.equals(TimeInterval.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createTimeIntervalPersistor(configKey);
        } else if (fieldType.equals(Credentials.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createCredentialsPersistor(configKey);
        } else if (fieldType.equals(FSLocation.class)) {
            return (FieldNodeSettingsPersistorWithInferredConfigs<T>)createFSLocationPersistor(configKey);
        } else {
            throw new IllegalArgumentException(
                String.format("No default persistor available for type '%s'.", fieldType));
        }
    }

    /**
     * When extending this enum only use lambdas if the definition fits a single line, otherwise use function references
     * as is done for {@link PersistorImpl#CHARACTER}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private enum PersistorImpl {
            VOID(Void.class, (s, k) -> null, (v, s, k) -> {
            }), //
            INT(int.class, ConfigBaseRO::getInt, (v, s, k) -> s.addInt(k, v)),
            DOUBLE(double.class, ConfigBaseRO::getDouble, (v, s, k) -> s.addDouble(k, v)),
            LONG(long.class, ConfigBaseRO::getLong, (v, s, k) -> s.addLong(k, v)),
            STRING(String.class, ConfigBaseRO::getString, (v, s, k) -> s.addString(k, v)),
            BOOLEAN(boolean.class, ConfigBaseRO::getBoolean, (v, s, k) -> s.addBoolean(k, v)),
            FLOAT(float.class, ConfigBaseRO::getFloat, (v, s, k) -> s.addFloat(k, v)),
            CHAR(char.class, ConfigBaseRO::getChar, (v, s, k) -> s.addChar(k, v)),
            BYTE(byte.class, ConfigBaseRO::getByte, (v, s, k) -> s.addByte(k, v)),
            INT_ARRAY(int[].class, ConfigBaseRO::getIntArray, (v, s, k) -> s.addIntArray(k, v)),
            DOUBLE_ARRAY(double[].class, ConfigBaseRO::getDoubleArray, (v, s, k) -> s.addDoubleArray(k, v)),
            LONG_ARRAY(long[].class, ConfigBaseRO::getLongArray, (v, s, k) -> s.addLongArray(k, v)),
            STRING_ARRAY(String[].class, ConfigBaseRO::getStringArray, (v, s, k) -> s.addStringArray(k, v)),
            BOOLEAN_ARRAY(boolean[].class, ConfigBaseRO::getBooleanArray, (v, s, k) -> s.addBooleanArray(k, v)),
            FLOAT_ARRAY(float[].class, ConfigBaseRO::getFloatArray, (v, s, k) -> s.addFloatArray(k, v)),
            CHAR_ARRAY(char[].class, ConfigBaseRO::getCharArray, (v, s, k) -> s.addCharArray(k, v)),
            BYTE_ARRAY(byte[].class, ConfigBaseRO::getByteArray, (v, s, k) -> s.addByteArray(k, v));

        private final Class<?> m_type;

        private final FieldPersistor<?> m_fieldPersistor;

        <T> PersistorImpl(final Class<T> type, final FieldLoader<T> loader, final FieldSaver<T> saver) {
            m_type = type;
            m_fieldPersistor = new FieldPersistorLoaderSaverAdapter<>(loader, saver);
        }

        Class<?> getFieldType() {
            return m_type;
        }

        FieldPersistor<?> getFieldPersistor() {
            return m_fieldPersistor;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> FieldNodeSettingsPersistorWithInferredConfigs<T> createEnumPersistor(final String configKey,
        final Class<T> fieldType) {
        return new EnumFieldPersistor<>(configKey, (Class)fieldType);
    }

    private static FieldNodeSettingsPersistor<LocalDate> createLocalDatePersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(LocalDatePersistor.class, LocalDate.class, configKey);
    }

    private static FieldNodeSettingsPersistor<LocalTime> createLocalTimePersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(LocalTimePersistor.class, LocalTime.class, configKey);
    }

    private static FieldNodeSettingsPersistor<ZoneId> createTimeZonePersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(TimeZonePersistor.class, ZoneId.class, configKey);
    }

    private static FieldNodeSettingsPersistor<Interval> createIntervalPersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(IntervalPersistor.class, Interval.class, configKey);
    }

    private static FieldNodeSettingsPersistor<DateInterval> createDateIntervalPersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(DateIntervalPersistor.class, DateInterval.class, configKey);
    }

    private static FieldNodeSettingsPersistor<TimeInterval> createTimeIntervalPersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(TimeIntervalPersistor.class, TimeInterval.class, configKey);
    }

    private static FieldNodeSettingsPersistorWithInferredConfigs<Credentials>
        createCredentialsPersistor(final String configKey) {
        return FieldNodeSettingsPersistor.createInstance(Credentials.CredentialsPersistor.class, Credentials.class,
            configKey);
    }

    private static FieldNodeSettingsPersistor<FSLocation> createFSLocationPersistor(final String configKey) {
        final var persistor = new FSLocationPersistor();
        persistor.setConfigKey(configKey);
        return persistor;
    }

    private DefaultFieldNodeSettingsPersistorFactory() {

    }

}
