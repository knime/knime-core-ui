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
 *   Mar 6, 2026 (gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield;

import java.awt.Color;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.DefaultFieldNodeSettingsPersistorFactory.OptionalContentPersistor;

final class ColorPersistor implements OptionalContentPersistor<Color> {

    private final String m_configKey;

    ColorPersistor(final String configKey) {
        m_configKey = configKey;
    }

    @Override
    public Color load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var value = settings.getString(m_configKey);
        if (value == null) {
            return null;
        }

        if (!value.matches("#?[0-9a-fA-F]{6}")) {
            throw new InvalidSettingsException("Failed to parse '%s' as 6-digit hex color.".formatted(value));
        }

        try {
            final var normalized = value.startsWith("#") ? value : "#" + value;
            return Color.decode(normalized);
        } catch (NumberFormatException ex) {
            throw new InvalidSettingsException("Failed to parse '%s' as 6-digit hex color.".formatted(value), ex);
        }
    }

    @Override
    public void save(final Color color, final NodeSettingsWO settings) {
        settings.addString(m_configKey,
            color == null ? null : "#%02X%02X%02X".formatted(color.getRed(), color.getGreen(), color.getBlue()));
    }

    @Override
    public void saveEmpty(final NodeSettingsWO settings) {
        settings.addString(m_configKey, null);
    }

}
