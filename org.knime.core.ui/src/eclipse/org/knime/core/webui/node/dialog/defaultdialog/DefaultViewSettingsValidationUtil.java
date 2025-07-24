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
 *   Sep 26, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.NodeParameters;

/**
 * A utility class for validating view {@link NodeParameters} instances overwritten by flow variables. Use this in
 * a node model (within {@link NodeModel#validateViewSettings}) for a node using {@link NodeParameters} view
 * variables. As a typical example where this validation is useful: When overwriting an enum field with a string flow
 * variable which does not have one of the string values, overwriting the node settings with the default node settings
 * does not throw an error. This error is only thrown when loading the node settings to {@link NodeParameters}
 * which, in case of view settings, is not caught unless this class is used as node model.
 *
 * @author Paul Bärnreuther
 */
public final class DefaultViewSettingsValidationUtil {

    private DefaultViewSettingsValidationUtil() {
        // utility
    }

    /**
     * Try to load the view settings (already overwritten by flow variables) to {@link NodeParameters}.
     *
     * @param viewSettings the to be validated view settings
     * @param viewSettingsClass the class of the view settings. Although it is odd, that any information regarding view
     *            settings is part of the node model, it is wanted here, as view settings should be treated as model
     *            settings when loading them is erroneous. But this class is and should only be used for the validation
     *            here within the node model.
     * @throws InvalidSettingsException
     */
    public static void validateViewSettings(final NodeSettingsRO viewSettings,
        final Class<? extends NodeParameters> viewSettingsClass) throws InvalidSettingsException {
        NodeParametersUtil.loadSettings(viewSettings, viewSettingsClass);
    }

}
