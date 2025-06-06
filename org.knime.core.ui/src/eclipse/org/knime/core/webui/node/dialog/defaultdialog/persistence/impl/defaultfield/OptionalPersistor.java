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
 *   Apr 29, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield;

import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.DefaultFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.OptionalContentPersistor;

/**
 * Persistor for {@link Optional} values. In case the value is not present, we still save a dummy value to allow setting
 * flow variables in this state.
 *
 * @author Paul Bärnreuther
 * @param <T> the type of the value
 */
public class OptionalPersistor<T> implements DefaultFieldPersistor<Optional<T>> {

    static final String IS_PRESENT_CFG_KEY_SUFFIX = "_is_present";

    /**
     * We need to expose this method for the aforementioned reason that setting the value flow variable is only possible
     * in case the value for this key is true.
     *
     * @param cfgKey the config key of the field
     * @return the config key for the presence of the value
     */
    public static String toIsPresentCfgKey(final String cfgKey) {
        return cfgKey + IS_PRESENT_CFG_KEY_SUFFIX;
    }

    private final OptionalContentPersistor<T> m_innerPersistor;

    private final String m_isPresentCfgKey;

    OptionalPersistor(final OptionalContentPersistor<T> innerPersistor, final String cfgKey) {
        m_innerPersistor = innerPersistor;
        m_isPresentCfgKey = toIsPresentCfgKey(cfgKey);
    }

    @Override
    public void save(final Optional<T> value, final NodeSettingsWO nodeSettings) {
        value.ifPresentOrElse(//
            val -> m_innerPersistor.save(val, nodeSettings), //
            () -> m_innerPersistor.saveEmpty(nodeSettings)//
        );
        nodeSettings.addBoolean(m_isPresentCfgKey, value.isPresent());
    }

    @Override
    public Optional<T> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.getBoolean(m_isPresentCfgKey, true)) {
            return Optional.ofNullable(m_innerPersistor.load(settings));
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getSubConfigPath() {
        return m_innerPersistor.getSubConfigPath();
    }

}
