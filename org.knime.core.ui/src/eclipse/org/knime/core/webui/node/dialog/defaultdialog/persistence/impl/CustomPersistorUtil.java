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

import static org.knime.core.webui.node.dialog.configmapping.NodeSettingsAtPathUtil.hasPath;

import java.util.Arrays;

import org.knime.core.webui.node.dialog.configmapping.ConfigPath;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DeprecatedSettingsLoadDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.internal.NewSettingsMissingMatcher;
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

    static <T> SettingsLoader<T> getLoadMethod(final NodeSettingsPersistor<T> customOrDefaultPersistor,
        final DeprecatedSettingsLoadDefinition<T> deprecatedSettingsLoadDefinition) {

        final var configDeprecations = deprecatedSettingsLoadDefinition.getConfigsDeprecations();
        return settings -> {
            for (final var configDeprecation : configDeprecations) {
                final var matcher = configDeprecation.getMatcher();
                final var loader = configDeprecation.getLoader();

                if (matcher instanceof NewSettingsMissingMatcher) {
                    final var inferredConfigPaths = getNonNullConfigPaths(customOrDefaultPersistor);
                    if (inferredConfigPaths == null || inferredConfigPaths.length == 0) {
                        throw new IllegalStateException(
                            "There exists a custom persistor without or with empty config paths but "
                                + "a deprecation matcher was set via 'withNewSettingsMissingMatcher' is used."
                                + "Either supply a custom matcher via 'withMatcher' or define config paths.");
                    }
                    if (Arrays.stream(inferredConfigPaths).map(Arrays::asList).map(ConfigPath::new)
                        .allMatch(configPath -> !hasPath(settings, configPath))) {
                        return loader.apply(settings);
                    }

                } else if (matcher.test(settings)) {
                    return loader.apply(settings);
                }
            }
            return customOrDefaultPersistor.load(settings);
        };

    }

    static <T> String[][] getNonNullConfigPaths(final NodeSettingsPersistor<T> persistor) {
        if (persistor instanceof NodeSettingsPersistorWithInferredConfigs<T> withInferredConfigs) {
            return withInferredConfigs.getNonNullPaths();
        } else {
            return persistor.getConfigPaths();
        }
    }

}