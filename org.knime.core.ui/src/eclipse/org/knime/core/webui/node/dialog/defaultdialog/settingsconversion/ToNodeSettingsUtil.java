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
 *   Aug 30, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.settingsconversion;

import java.util.Map;
import java.util.function.Function;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil;
import org.knime.node.parameters.NodeParameters;

/**
 * Each implementation of this class provides a way to construct {@link NodeParameters} for each type of settings.
 * These can be transformed to node settings using one of the {@link ToNodeSettings#toNodeSettings} methods.
 *
 * @author Paul Bärnreuther
 */
public final class ToNodeSettingsUtil {

    private ToNodeSettingsUtil() {
        // Utility
    }

    /**
     * @param type of the settings to extract the root config key from
     * @param defaultNodeSettings the to be converted defaultNodeSettings
     * @return the constructed {@link NodeSettings}
     */
    public static NodeSettings toNodeSettings(final SettingsType type, final NodeParameters defaultNodeSettings) {
        var res = new NodeSettings(type.getConfigKey());
        toNodeSettings(res, defaultNodeSettings);
        return res;
    }

    /**
     * @param defaultNodeSettings
     * @return the extracted node settings
     */
    public static Map<SettingsType, NodeSettings>
        toNodeSettings(final Map<SettingsType, NodeParameters> defaultNodeSettings) {
        return MapValuesUtil.mapValuesWithKeys(defaultNodeSettings, (k, v) -> toNodeSettings(k, v));
    }

    /**
     * @param nodeSettings to be written to
     * @param constructDefaultNodeSettings called for every key of the provided map
     */
    public static void constructNodeSettings(final Map<SettingsType, NodeSettingsWO> nodeSettings,
        final Function<SettingsType, NodeParameters> constructDefaultNodeSettings) {
        nodeSettings
            .entrySet().forEach(entry -> toNodeSettings(entry.getValue(),
            constructDefaultNodeSettings.apply(entry.getKey())));
    }

    /**
     * @param nodeSettings the output to which the extracted {@link NodeSettings} are written
     * @param type of the settings
     */
    private static void toNodeSettings(final NodeSettingsWO nodeSettings,
        final NodeParameters defaultNodeSettings) {
        NodeParametersUtil.saveSettings(defaultNodeSettings.getClass(), defaultNodeSettings, nodeSettings);
    }

}
