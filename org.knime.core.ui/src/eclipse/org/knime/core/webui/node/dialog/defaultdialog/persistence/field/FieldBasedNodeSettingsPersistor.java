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
 *   Nov 14, 2022 ("Adrian Nembach, KNIME GmbH, Konstanz, Germany"): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.field;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.ReflectionUtil;

/**
 * Performs persistence of DefaultNodeSettings on a per-field basis. The persistence of individual fields can be
 * controlled with the {@link Persist} annotation.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <S> The concrete {@link PersistableSettings} class
 */
public class FieldBasedNodeSettingsPersistor<S extends PersistableSettings> implements NodeSettingsPersistor<S> {

    @SuppressWarnings("javadoc")
    protected Map<String, NodeSettingsPersistor<?>> m_persistors;

    private final Class<S> m_settingsClass;

    /**
     * Constructor.
     *
     * @param settingsClass the class of settings to persist
     */
    public FieldBasedNodeSettingsPersistor(final Class<S> settingsClass) {
        m_persistors = new FieldNodeSettingsPersistorFactory<>(settingsClass).createPersistors();
        m_settingsClass = settingsClass;
    }

    @Override
    public void save(final S obj, final NodeSettingsWO settings) {
        try {
            useBlackMagicToAccessFields((persistor, field) -> uncheckedSave(persistor, field.get(obj), settings));
        } catch (InvalidSettingsException ex) {//NOSONAR
            // because the origin of the InvalidSettingsException would be our PersistorConsumer which does not
            // throw such an exception
            throw new IllegalStateException("This catch block is not supposed to be reachable.");
        }
    }

    @Override
    public ConfigMappings getConfigMappings(final S obj) {
        List<ConfigMappings> configMappingsForFields = new ArrayList<>(m_persistors.size());
        try {
            useBlackMagicToAccessFields((persistor, field) -> configMappingsForFields
                .add(uncheckedGetModifications(persistor, field.get(obj))));
        } catch (InvalidSettingsException ex) {//NOSONAR
            // because the origin of the InvalidSettingsException would be our PersistorConsumer which does not
            // throw such an exception
            throw new IllegalStateException("This catch block is not supposed to be reachable.");
        }
        return new ConfigMappings(configMappingsForFields);
    }

    @SuppressWarnings("unchecked")
    private static <T> void uncheckedSave(final NodeSettingsPersistor<T> persistor, final Object value,
        final NodeSettingsWO nodeSettings) {
        persistor.save((T)value, nodeSettings);
    }

    @SuppressWarnings("unchecked")
    private static <T> ConfigMappings uncheckedGetModifications(final NodeSettingsPersistor<T> persistor,
        final Object value) {
        return persistor.getConfigMappings((T)value);
    }

    @FunctionalInterface
    private interface PersistorConsumer {
        void accept(final NodeSettingsPersistor<?> persistor, final Field field)
            throws InvalidSettingsException, IllegalAccessException;
    }

    @Override
    public S load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var loaded =
            ReflectionUtil.createInstance(m_settingsClass).orElseThrow(() -> new IllegalArgumentException(String
                .format("The provided PersistableSettings '%s' don't provide an empty constructor.", m_settingsClass)));
        useBlackMagicToAccessFields((persistor, field) -> field.set(loaded, persistor.load(settings)));//NOSONAR
        return loaded;
    }

    private void useBlackMagicToAccessFields(final PersistorConsumer consumer) throws InvalidSettingsException {
        for (var entry : m_persistors.entrySet()) {
            var fieldName = entry.getKey();
            try {
                var field = getFromAllFields(m_settingsClass, entry.getKey());
                field.setAccessible(true);//NOSONAR
                var persistor = entry.getValue();
                consumer.accept(persistor, field);
            } catch (IllegalAccessException ex) {
                // because we use black magic (Field#setAccessible) to make the field accessible
                throw new IllegalStateException(
                    String.format("Could not access the field '%s' although reflection was used to make it accessible.",
                        fieldName),
                    ex);
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException(String
                    .format("The field '%s' no longer exists in class '%s' although it existed during creation of the"
                        + " persistor. Most likely an implementation error.", fieldName, m_settingsClass),
                    ex);
            } catch (SecurityException ex) {
                throw new IllegalStateException(
                    "Security exception while accessing field although it was possible to access it during creation of"
                        + " the persistor. Most likely an implementation error.",
                    ex);
            }
        }

    }

    private static Field getFromAllFields(final Class<?> clazz, final String key)
        throws NoSuchFieldException, SecurityException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(key);
            } catch (NoSuchFieldException ex) { //NOSONAR
            } catch (SecurityException ex) {
                throw ex;
            }
        }
        throw new NoSuchFieldException(key);
    }

}
