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
 *   Dec 12, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultPersistorWithDeprecations;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NodeSettingsPersistorWithInferredConfigs;

/**
 * Utility class used for logic around custom {@link NodeSettingsPersistor NodeSettingsPersistors} for on field and
 * class level.
 *
 * @author Paul Bärnreuther
 */
public final class CustomPersistorUtil {

    private CustomPersistorUtil() {
        // utility class
    }

    /**
     * Public for tests, TODO: Move to testing
     *
     * @param <T>
     * @param customPersistor
     * @param defaultPersistorSupplier
     * @return
     */
    public static <T> NodeSettingsPersistor<T> prepareCustomPersistor(final NodeSettingsPersistor<T> customPersistor,
        final Supplier<NodeSettingsPersistor<T>> defaultPersistorSupplier) {
        return adaptLoadToEnableLegacyLoading(combineWithDefaultIfNecessary(customPersistor, defaultPersistorSupplier));
    }

    private static <T> NodeSettingsPersistor<T> combineWithDefaultIfNecessary(
        final NodeSettingsPersistor<T> customPersistor,
        final Supplier<NodeSettingsPersistor<T>> defaultPersistorSupplier) {
        if (customPersistor instanceof DefaultPersistorWithDeprecations<T> withDeprecations) {
            final var defaultPersistor = defaultPersistorSupplier.get();
            if (defaultPersistor instanceof NodeSettingsPersistorWithInferredConfigs<T> defaultPersistorWithInferredConfigs) {
                return new DefaultPersistorWithDeprecationsDecoratorWithInferredConfigs<>(withDeprecations,
                    defaultPersistorWithInferredConfigs);
            }
            return new DefaultPersistorWithDeprecationsDecorator<T>(withDeprecations, defaultPersistor);
        }
        return customPersistor;
    }

    private static <T> NodeSettingsPersistor<T>
        adaptLoadToEnableLegacyLoading(final NodeSettingsPersistor<T> customPersistor) {
        if (customPersistor.getConfigsDeprecations().isEmpty()) {
            return customPersistor;
        }
        return new LoadFromLegacyDecorator<>(customPersistor);
    }

    static <T> String[][] getNonNullConfigPaths(final NodeSettingsPersistor<T> persistor) {
        if (persistor instanceof NodeSettingsPersistorWithInferredConfigs<T> withInferredConfigs) {
            return withInferredConfigs.getNonNullPaths();
        } else {
            return persistor.getConfigPaths();
        }
    }

    /**
     * A wrapper for persistors which implement {@link DefaultPersistorWithDeprecations} that loads and saves settings
     * using the default persistor and the custom persistor to get the deprecated configs.
     *
     * @author Robin Gerling
     * @param <T> the type of object loaded by the persistor
     */
    private static final class DefaultPersistorWithDeprecationsDecoratorWithInferredConfigs<T>
        implements NodeSettingsPersistorWithInferredConfigs<T> {

        final DefaultPersistorWithDeprecations<T> m_customPersistor;

        final NodeSettingsPersistorWithInferredConfigs<T> m_defaultPersistor;

        /**
         * @param customPersistor the custom persistor of the node settings field handling deprecated configs
         * @param defaultPersistor the default persistor of the node settings field
         *
         */
        public DefaultPersistorWithDeprecationsDecoratorWithInferredConfigs(
            final DefaultPersistorWithDeprecations<T> customPersistor,
            final NodeSettingsPersistorWithInferredConfigs<T> defaultPersistor) {
            m_customPersistor = customPersistor;
            m_defaultPersistor = defaultPersistor;
        }

        @Override
        public void save(final T obj, final NodeSettingsWO settings) {
            m_defaultPersistor.save(obj, settings);
        }

        @Override
        public T load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return m_defaultPersistor.load(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return m_defaultPersistor.getConfigPaths();
        }

        @Override
        public List<ConfigsDeprecation<T>> getConfigsDeprecations() {
            return m_customPersistor.getConfigsDeprecations();
        }

        @Override
        public String[][] getNonNullPaths() {
            return m_defaultPersistor.getNonNullPaths();
        }
    }

    /**
     * A wrapper for persistors which implement {@link DefaultPersistorWithDeprecations} that loads and saves settings
     * using the default persistor and the custom persistor to get the deprecated configs.
     *
     * @author Robin Gerling
     * @param <T> the type of object loaded by the persistor
     */
    private static final class DefaultPersistorWithDeprecationsDecorator<T> implements NodeSettingsPersistor<T> {

        final DefaultPersistorWithDeprecations<T> m_customPersistor;

        final NodeSettingsPersistor<T> m_defaultPersistor;

        /**
         * @param customPersistor the custom persistor of the node settings field handling deprecated configs
         * @param defaultPersistor the default persistor of the node settings field
         *
         */
        public DefaultPersistorWithDeprecationsDecorator(final DefaultPersistorWithDeprecations<T> customPersistor,
            final NodeSettingsPersistor<T> defaultPersistor) {
            m_customPersistor = customPersistor;
            m_defaultPersistor = defaultPersistor;
        }

        @Override
        public void save(final T obj, final NodeSettingsWO settings) {
            m_defaultPersistor.save(obj, settings);
        }

        @Override
        public T load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return m_defaultPersistor.load(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return m_defaultPersistor.getConfigPaths();
        }

        @Override
        public List<ConfigsDeprecation<T>> getConfigsDeprecations() {
            return m_customPersistor.getConfigsDeprecations();
        }

    }

    /**
     * Adapt the loading mechanism of the settings of field not settings persistors. In case a persistor specifies
     * deprecated configs by implementing {@link #FieldNodeSettingsPersistor.getConfigsDeprecations() }, the
     * corresponding matchers are iterated by their order in the list of {@link ConfigsDeprecation ConfigsDeprecations}
     * and the settings are loaded according to the loader of the first match. If no match was found the default load of
     * the persistor is used.
     */
    private static final class LoadFromLegacyDecorator<T> implements NodeSettingsPersistor<T> {

        private final NodeSettingsPersistor<T> m_delegate;

        LoadFromLegacyDecorator(final NodeSettingsPersistor<T> delegate) {
            m_delegate = delegate;
        }

        @Override
        public T load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var configDeprecations = m_delegate.getConfigsDeprecations();

            for (final var configDeprecation : configDeprecations) {
                final var matcher = configDeprecation.getMatcher();
                final var loader = configDeprecation.getLoader();

                if (matcher.test(settings)) {
                    return loader.apply(settings);
                }
            }
            return m_delegate.load(settings);
        }

        @Override
        public void save(final T obj, final NodeSettingsWO settings) {
            m_delegate.save(obj, settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return m_delegate.getConfigPaths();
        }

        @Override
        public List<ConfigsDeprecation<T>> getConfigsDeprecations() {
            return m_delegate.getConfigsDeprecations();
        }

    }

}
