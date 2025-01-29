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
 *   May 29, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.configmapping;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

import org.knime.core.node.NodeSettingsRO;

/**
 * Provides necessary information required to handle, e.g., config deprecations.
 *
 * @see NodeSettingsCorrectionUtil
 *
 * @author Paul Bärnreuther
 */
public final class ConfigMappings {

    final Collection<ConfigMappings> m_children;

    final String m_key;

    final Collection<ConfigPath> m_newConfigPaths;

    final Collection<ConfigPath> m_deprecatedConfigPaths;

    final UnaryOperator<NodeSettingsRO> m_oldSettingsToNewSettings;

    /**
     * @param children
     */
    public ConfigMappings(final Collection<ConfigMappings> children) {
        this(null, children);
    }

    /**
     * @param key by which the children can be accessed
     * @param children
     */
    public ConfigMappings(final String key, final Collection<ConfigMappings> children) {
        m_children = children;
        m_newConfigPaths = null;
        m_deprecatedConfigPaths = null;
        m_oldSettingsToNewSettings = null;
        m_key = key;
    }

    /**
     * @param deprecatedConfigPaths the configs that possibly need to be corrected because of mismatches between
     *            settings and flow variables because
     * @param newConfigPaths the new config paths that are used instead of the deprecated ones
     * @param oldSettingsToNewSettings a method that is able to transform the previous node settings to new node
     *            settings. It is used only when the previous value should be used but overwritten by a new flow
     *            variable, i.e. when the user de-selected a deprecated flow variable and selects a new one before
     *            applying.
     */
    public ConfigMappings(final Collection<ConfigPath> deprecatedConfigPaths,
        final Collection<ConfigPath> newConfigPaths, final UnaryOperator<NodeSettingsRO> oldSettingsToNewSettings) {
        m_newConfigPaths = newConfigPaths;
        m_deprecatedConfigPaths = deprecatedConfigPaths;
        m_oldSettingsToNewSettings = oldSettingsToNewSettings;
        m_children = List.of();
        m_key = null;
    }
}
