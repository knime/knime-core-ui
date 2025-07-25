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
 *   Sep 18, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Persistor for {@link SettingsModelString} that saves and loads the value as an enum.
 *
 * @param <E> the type of the enum
 * @author Paul Bärnreuther
 */
public class EnumSettingsModelStringPersistor<E extends Enum<E>> implements NodeParametersPersistor<E> {

    private final String m_configKey;

    private Class<E> m_enumClass;

    /**
     * @param configKey the config key of the settings model
     * @param enumClass the class of the generic type
     */
    protected EnumSettingsModelStringPersistor(final String configKey, final Class<E> enumClass) {
        m_configKey = configKey;
        m_enumClass = enumClass;
    }

    @Override
    public void save(final E obj, final NodeSettingsWO settings) {
        final var valueAsString = obj != null ? obj.name() : null;
        final var model = new SettingsModelString(m_configKey, valueAsString);
        model.saveSettingsTo(settings);
    }

    @Override
    public E load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var model = new SettingsModelString(m_configKey, "");
        model.loadSettingsFrom(settings);
        var name = model.getStringValue();
        return name != null ? Enum.valueOf(m_enumClass, name) : null;
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][]{{m_configKey}};
    }

}
