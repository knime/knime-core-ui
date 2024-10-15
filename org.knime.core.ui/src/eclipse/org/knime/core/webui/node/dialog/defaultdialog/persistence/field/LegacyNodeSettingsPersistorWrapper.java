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
 *   15 Oct 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.field;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;

/**
 * A wrapper for custom persistors that loads settings as specified by the matcher and loader given by
 * {@link ConfigsDeprecation}. In case a matcher matches, the corresponding loader is used to load the settings. Else,
 * the default persistor is used during load which is always used during save.
 *
 * @author Robin Gerling
 * @param <T> the type of object loaded by the persistor
 */
public final class LegacyNodeSettingsPersistorWrapper<T> implements FieldNodeSettingsPersistor<T> {

    final LegacyNodeSettingsPersistor<T> m_legacyNodeSettingsPersistor;

    final NodeSettingsPersistor<T> m_defaultPersistor;

    /**
     * @param legacyNodeSettingsPersistor the custom persistor of the node settings field handling legacy configs
     * @param defaultPersistor the default persistor of the node settings field
     *
     */
    public LegacyNodeSettingsPersistorWrapper(final LegacyNodeSettingsPersistor<T> legacyNodeSettingsPersistor,
        final NodeSettingsPersistor<T> defaultPersistor) {
        m_legacyNodeSettingsPersistor = legacyNodeSettingsPersistor;
        m_defaultPersistor = defaultPersistor;
    }

    @Override
    public void save(final T obj, final NodeSettingsWO settings) {
        m_defaultPersistor.save(obj, settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var configDeprecations = m_legacyNodeSettingsPersistor.getConfigsDeprecations();
        for (final var configDeprecation : configDeprecations) {
            final var matcher = configDeprecation.getMatcher();
            final var loader = configDeprecation.getLoader();

            if (matcher != null && matcher.test(settings)) {
                return (T)loader.apply(settings);
            }
        }
        return m_defaultPersistor.load(settings);
    }

    @Override
    public ConfigsDeprecation[] getConfigsDeprecations() {
        return m_legacyNodeSettingsPersistor.getConfigsDeprecations();
    }

    @Override
    public String[] getConfigKeys() {
        return null;
    }

}