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
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsLoader;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.SettingsSaver;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.DateIntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.IntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.LocalDatePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.LocalDateTimePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.LocalTimePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.TimeIntervalPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.TimeZonePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DateTimePersistorUtils.ZonedDateTimePersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * Factory for default persistors either for arrays or {@link PersistableSettings} or settings that store values
 * directly in NodeSettings.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultFieldNodeSettingsPersistorFactory {

    private static final Map<Class<?>, FieldPersistor<?>> IMPL_MAP = Stream.of(PersistorImpl.values())//
        .collect(toMap(PersistorImpl::getFieldType, PersistorImpl::getFieldPersistor));

    /**
     * Similar to {@link NodeSettingsPersistor} but since every default persistor should persist to the given config
     * key, it is only possible to declare sub config keys if present.
     *
     * @param <S> the type of the settings object
     *
     */
    public interface DefaultFieldPersistor<S> extends SettingsSaver<S>, SettingsLoader<S>, SubConfigPathProvider {

    }

    /**
     *
     * @param node
     * @param configKey
     * @return how to save and load this node to the config key
     */
    public static DefaultFieldPersistor<?> createPersistor(final LeafNode<PersistableSettings> node,
        final String configKey) {
        return createPersistor(node.getType(), configKey);
    }

    /**
     * Creates a persistor for the provided type that uses the configKey to store and retrieve the value.
     *
     * @param <T> the type of field
     * @param fieldType the type of field the created persistor should persist
     * @param configKey the key to use for storing and retrieving the value to and from the NodeSettings
     * @return a new persistor
     * @throws IllegalArgumentException if there is no persistor available for the provided fieldType
     */
    public static <T> DefaultFieldPersistor<T> createPersistor(final Class<T> fieldType, final String configKey) {
        @SuppressWarnings("unchecked") // Type-save since IMPL_MAP maps Class<T> to FieldPersistor<T>
        var impl = (FieldPersistor<T>)IMPL_MAP.get(fieldType);
        return createPersistorFromImpl(fieldType, configKey, impl);
    }

    @SuppressWarnings("unchecked")
    private static <T> DefaultFieldPersistor<T> createPersistorFromImpl(final Class<T> fieldType,
        final String configKey, final FieldPersistor<T> impl) {
        if (impl != null) {
            return new DefaultFieldNodeSettingsPersistor<>(configKey, impl);
        } else if (fieldType.isEnum()) {
            return createEnumPersistor(configKey, fieldType);
        } else if (fieldType.equals(LocalDate.class)) {
            return (DefaultFieldPersistor<T>)createLocalDatePersistor(configKey);
        } else if (fieldType.equals(LocalTime.class)) {
            return (DefaultFieldPersistor<T>)createLocalTimePersistor(configKey);
        } else if (fieldType.equals(LocalDateTime.class)) {
            return (DefaultFieldPersistor<T>)createLocalDateTimePersistor(configKey);
        } else if (fieldType.equals(ZonedDateTime.class)) {
            return (DefaultFieldPersistor<T>)createZonedDateTimePersistor(configKey);
        } else if (fieldType.equals(ZoneId.class)) {
            return (DefaultFieldPersistor<T>)createTimeZonePersistor(configKey);
        } else if (fieldType.equals(Interval.class)) {
            return (DefaultFieldPersistor<T>)createIntervalPersistor(configKey);
        } else if (fieldType.equals(DateInterval.class)) {
            return (DefaultFieldPersistor<T>)createDateIntervalPersistor(configKey);
        } else if (fieldType.equals(TimeInterval.class)) {
            return (DefaultFieldPersistor<T>)createTimeIntervalPersistor(configKey);
        } else if (fieldType.equals(Credentials.class)) {
            return (DefaultFieldPersistor<T>)createCredentialsPersistor(configKey);
        } else if (fieldType.equals(FSLocation.class)) {
            return (DefaultFieldPersistor<T>)createFSLocationPersistor(configKey);
        } else if (fieldType.equals(TemporalFormat.class)) {
            return (DefaultFieldPersistor<T>)createTemporalFormatPersistor(configKey);
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
            BYTE_ARRAY(byte[].class, ConfigBaseRO::getByteArray, (v, s, k) -> s.addByteArray(k, v)),
            DATA_TYPE(DataType.class, ConfigRO::getDataType, (v, s, k) -> s.addDataType(k, v), List.of("cell_class"));

        private final Class<?> m_type;

        private final FieldPersistor<?> m_fieldPersistor;

        <T> PersistorImpl(final Class<T> type, final FieldLoader<T> loader, final FieldSaver<T> saver) {
            this(type, loader, saver, null);
        }

        <T> PersistorImpl(final Class<T> type, final FieldLoader<T> loader, final FieldSaver<T> saver,
            final List<String> subConfigPaths) {
            m_type = type;
            m_fieldPersistor = new FieldPersistor<T>() {

                @Override
                public T load(final NodeSettingsRO settings, final String configKey) throws InvalidSettingsException {
                    return loader.load(settings, configKey);
                }

                @Override
                public void save(final T value, final NodeSettingsWO settings, final String configKey) {
                    saver.save(value, settings, configKey);
                }

                @Override
                public Optional<List<String>> getSubConfigPath() {
                    return Optional.ofNullable(subConfigPaths);
                }

            };
        }

        Class<?> getFieldType() {
            return m_type;
        }

        FieldPersistor<?> getFieldPersistor() {
            return m_fieldPersistor;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> DefaultFieldPersistor<T> createEnumPersistor(final String configKey, final Class<T> fieldType) {
        return new EnumFieldPersistor<>(configKey, (Class)fieldType);
    }

    private static DefaultFieldPersistor<LocalDate> createLocalDatePersistor(final String configKey) {
        return new LocalDatePersistor(configKey);
    }

    private static DefaultFieldPersistor<LocalTime> createLocalTimePersistor(final String configKey) {
        return new LocalTimePersistor(configKey);
    }

    private static DefaultFieldPersistor<LocalDateTime> createLocalDateTimePersistor(final String configKey) {
        return new LocalDateTimePersistor(configKey);
    }

    private static DefaultFieldPersistor<ZonedDateTime> createZonedDateTimePersistor(final String configKey) {
        return new ZonedDateTimePersistor(configKey);
    }

    private static DefaultFieldPersistor<ZoneId> createTimeZonePersistor(final String configKey) {
        return new TimeZonePersistor(configKey);
    }

    private static DefaultFieldPersistor<Interval> createIntervalPersistor(final String configKey) {
        return new IntervalPersistor(configKey);
    }

    private static DefaultFieldPersistor<DateInterval> createDateIntervalPersistor(final String configKey) {
        return new DateIntervalPersistor(configKey);
    }

    private static DefaultFieldPersistor<TimeInterval> createTimeIntervalPersistor(final String configKey) {
        return new TimeIntervalPersistor(configKey);
    }

    private static DefaultFieldPersistor<Credentials> createCredentialsPersistor(final String configKey) {
        return new Credentials.CredentialsPersistor(configKey);
    }

    private static DefaultFieldPersistor<FSLocation> createFSLocationPersistor(final String configKey) {
        return new FSLocationPersistor(configKey);
    }

    private static DefaultFieldPersistor<TemporalFormat> createTemporalFormatPersistor(final String configKey) {
        return new TemporalFormat.TemporalFormatPersistor(configKey);
    }

    private DefaultFieldNodeSettingsPersistorFactory() {

    }

}
