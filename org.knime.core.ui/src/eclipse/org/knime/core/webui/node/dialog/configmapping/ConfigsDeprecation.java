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
 *   Feb 16, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.configmapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;

/**
 * This class provides a connection from an array of config paths relative to the base config of the field to another
 * one and specifies how old configs should be loaded.
 * <ul>
 * <li>Deprecated config paths: Configs which are respected during {@link FieldNodeSettingsPersistor#load load} but are
 * not written to when saving the loaded data back during {@link FieldNodeSettingsPersistor#save save}.</li>
 * <li>New config paths: Those configs that are affected by the deprecated config paths. I.e. on
 * {@link FieldNodeSettingsPersistor#save save} their values might differ depending on the values of any of the
 * deprecated config paths during {@link FieldNodeSettingsPersistor#load load}</li>
 * </ul>
 * The matcher determines whether the settings contain deprecated configs and should be loaded in a different way by the
 * loader. Matcher and loader cannot be specified at all, but in case matcher is specified, a loader must be specified.
 *
 * @author Paul Bärnreuther
 */
public final class ConfigsDeprecation {

    /**
     *
     * @param <T>
     */
    @FunctionalInterface
    public interface DeprecationLoader<T> {
        /**
         * @param settings the settings containing the deprecated config to load
         * @return T the type of the new config
         * @throws InvalidSettingsException
         */
        T apply(NodeSettingsRO settings) throws InvalidSettingsException;
    }

    /**
     *
     */
    @FunctionalInterface
    public interface DeprecationMatcher {
        /**
         * @param settings the settings on which to search a deprecated config
         * @return whether a deprecated config was found
         * @throws InvalidSettingsException
         */
        boolean test(NodeSettingsRO settings) throws InvalidSettingsException;
    }

    /**
     *
     * @param newConfigPaths (see {@link ConfigsDeprecation})
     * @param deprecatedConfigPaths (see {@link ConfigsDeprecation})
     */
    public record NewAndDeprecatedConfigPaths(List<ConfigPath> newConfigPaths, List<ConfigPath> deprecatedConfigPaths) {
    }

    private Collection<NewAndDeprecatedConfigPaths> m_newAndDeprecatedConfigPaths;

    private DeprecationMatcher m_matcher;

    private DeprecationLoader<?> m_loader;

    /**
     * Private. Use the {@link Builder} instead.
     */
    private ConfigsDeprecation(final Collection<NewAndDeprecatedConfigPaths> newAndDeprecatedConfigPaths,
        final DeprecationMatcher matcher, final DeprecationLoader<?> loader) {
        this.m_newAndDeprecatedConfigPaths = newAndDeprecatedConfigPaths;
        this.m_matcher = matcher;
        this.m_loader = loader;
    }

    /**
     * @return the newConfigPaths and the deprecatedConfigPaths relative to the base config path of the annotated
     *         setting
     */
    public Collection<NewAndDeprecatedConfigPaths> getNewAndDeprecatedConfigPaths() {
        return m_newAndDeprecatedConfigPaths;
    }

    /**
     * @return the loader, which converts the old config into the new one during load
     */
    public DeprecationLoader<?> getLoader() {
        return m_loader;
    }

    /**
     * @return the matchers, which determines when to use the loader
     */
    public DeprecationMatcher getMatcher() {
        return m_matcher;
    }

    /**
     * Builder for {@link ConfigsDeprecation}.
     *
     * @author Paul Bärnreuther
     */
    public static final class Builder {

        private final List<NewAndDeprecatedConfigPaths> m_newAndDeprecatedConfigPaths = new ArrayList<>(0);

        private DeprecationMatcher m_matcher = null;

        private DeprecationLoader<?> m_loader = null;

        /**
         * Builder for {@link ConfigsDeprecation}. See {@link ConfigsDeprecation} for more information.
         */
        public Builder() {
            // Builder
        }

        /**
         * Both parameters represent configKeys forming a path from the base config of the
         * {@link Persist#customPersistor} to a desired subconfig. This method can called multiple times.
         *
         * @param newConfigKeys the configKeys set during {@link FieldNodeSettingsPersistor#save} affected by the values
         *            of all of the config paths set with {@code deprecatedConfigKeys}.
         * @param deprecatedConfigKeys the configKeys used during {@link FieldNodeSettingsPersistor#load} which are not
         *            written to in {@link FieldNodeSettingsPersistor#save} but instead affect the values of the configs
         *            specified with {@code newConfigKeys}.
         * @return the builder
         */
        public Builder forNewAndDeprecatedConfigPaths(final List<List<String>> newConfigKeys,
            final List<List<String>> deprecatedConfigKeys) {
            m_newAndDeprecatedConfigPaths.add(new NewAndDeprecatedConfigPaths(mapToListOfConfigPaths(newConfigKeys),
                mapToListOfConfigPaths(deprecatedConfigKeys)));
            return this;
        }

        private static final List<ConfigPath> mapToListOfConfigPaths(final List<List<String>> values) {
            return values.stream().map(ConfigPath::new).toList();
        }

        private static final List<ConfigPath> mapToListOfConfigPaths(final Optional<String> value) {
            return List.of(new ConfigPath(value.map(v -> List.of(v)).orElse(Collections.emptyList())));
        }

        /**
         * Both parameters represent a configKey that is part of the base config of the {@link Persist#customPersistor}.
         * This method can called multiple times.
         *
         * @param newConfigKey the configKey set during {@link FieldNodeSettingsPersistor#save} affected by the value of
         *            the config key set with {@code deprecatedConfigKeys}.
         * @param deprecatedConfigKey the configKey used during {@link FieldNodeSettingsPersistor#load} which is not
         *            written to in {@link FieldNodeSettingsPersistor#save} but instead affects the value of the config
         *            specified with {@code newConfigKey}.
         * @return the builder
         */
        public Builder forNewAndDeprecatedConfigPaths(final Optional<String> newConfigKey,
            final Optional<String> deprecatedConfigKey) {
            m_newAndDeprecatedConfigPaths.add(new NewAndDeprecatedConfigPaths(mapToListOfConfigPaths(newConfigKey),
                mapToListOfConfigPaths(deprecatedConfigKey)));
            return this;
        }

        /**
         * Specify a predicate, which determines based on the {@link NodeSettingsRO} whether a deprecated config exists
         * which should be loaded in a different way by a {@link #withLoader(DeprecationLoader) loader}. This method can
         * be called once. If it is not called and a {@link #withLoader loader} is specified, a matcher will be created
         * based on the deprecatedConfigKeys given with {@link #forNewAndDeprecatedConfigPaths(List, List)}. In case all
         * deprecated config keys are contained in the settings the matcher will return {@code true}. It cannot be used
         * without specifying a {@link #withLoader loader}.
         *
         * @param matcher the matcher used to determine whether settings are deprecated and should be loaded in a
         *            different way
         * @return the builder
         */
        public Builder withMatcher(final DeprecationMatcher matcher) {
            m_matcher = matcher;
            return this;
        }

        private void setMatcher(final DeprecationMatcher matcher) {
            this.m_matcher = matcher;
        }

        /**
         * Specify a loader which loads deprecated configs in the {@link NodeSettingsRO} into the new configs. This
         * method can be called once.
         *
         * @param loader the loader which uses the deprecated config to load the settings based on the new config
         * @return the builder
         */
        public Builder withLoader(final DeprecationLoader<?> loader) {
            m_loader = loader;
            return this;
        }

        /**
         * @return a new {@link ConfigsDeprecation} to be used in
         *         {@link FieldNodeSettingsPersistor#getConfigsDeprecations()}
         */
        public ConfigsDeprecation build() {
            final var matcherIsNull = m_matcher == null;
            final var loaderIsNull = m_loader == null;
            if (!matcherIsNull && loaderIsNull) {
                throw new NullPointerException(
                    "A ConfigsDeprecation loader must be specified when specifying a matcher.");
            }
            if (matcherIsNull && !loaderIsNull) {
                createMatcherByDeprecatedConfigPaths();
            }
            return new ConfigsDeprecation(m_newAndDeprecatedConfigPaths, m_matcher, m_loader);
        }

        private void createMatcherByDeprecatedConfigPaths() {
            final List<ConfigPath> deprecatedConfigPaths = m_newAndDeprecatedConfigPaths.stream()
                .flatMap(newAndDeprecatedConfigPaths -> newAndDeprecatedConfigPaths.deprecatedConfigPaths().stream())
                .toList();
            if (deprecatedConfigPaths.isEmpty()) {
                throw new NullPointerException("Cannot create a matcher, because no DeprecatedConfigPath exists.");
            }

            this.setMatcher(settings -> deprecatedConfigPaths.stream()
                .allMatch(configPath -> traversedSettingsContainConfigPath(settings, configPath.path(), 0)));
        }

        private boolean traversedSettingsContainConfigPath(final NodeSettingsRO settings, final List<String> configPath,
            final int index) {
            if (index == configPath.size() - 1) {
                return settings.containsKey(configPath.get(index));
            }
            return traversedSettingsContainConfigPath(settings, configPath, index + 1);
        }
    }
}
