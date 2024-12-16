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
package org.knime.core.webui.node.dialog.defaultdialog.persistence.internal;

import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;

/**
 * A {@link NodeSettingsPersistor} should extend this interface if the config keys to which it saves can be inferred by
 * the framework. We need to know these config keys for {@link ConfigMappings} to work.
 *
 * @param <T> the type of the persisted object
 * @author Paul Bärnreuther
 */
public interface NodeSettingsPersistorWithInferredConfigs<T> extends NodeSettingsPersistor<T> {

    /**
     * With few exceptions, the default field and class persistors return null in {@link #getConfigPaths()}. Setting the
     * values in {@link #getConfigPaths()} to null is allowed and intended since otherwise all of these paths are
     * connected to all nested fields.
     *
     * This method is called when there exists a {@link ConfigsDeprecation} where the new keys should be automatically
     * inferred. For {@link ConfigMappings} we need to make the new config paths precise in order to be able to remove
     * those.
     *
     * @return A list of config paths relative to the current level that are saved to on save.
     */
    default String[][] getNonNullPaths() {
        final var configPaths = getConfigPaths();
        if (configPaths == null) {
            throw new IllegalStateException(
                "Every persistor either has to implement NodeSettingsPersistorWithInferredConfigs or"
                    + " provide a non-null result in getConfigPaths().");
        }
        return configPaths;
    }

}
