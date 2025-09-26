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
 *   Sep 25, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.persistence.legacy;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;

/**
 * A persistor for loading a long setting (as used for instance for seeds in legacy dialogs) as a string. The reason for
 * this is that the maximum safe integer in JavaScript is 2^53 -1, whereas the Long.MAX_VALUE is 2^63 -1. Hence, if a
 * node was configured with a seed larger than 2^53 -1, the value will likely be changed when the modern dialog is
 * opened. For this reason, it should be treated as a string in the modern dialog until we have a BigInt widget.
 *
 * @author Ali Asghar Marvi, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public abstract class LongAsStringPersistor implements NodeParametersPersistor<String> {

    /**
     * A validation that checks whether the input is a valid integer number within long range.
     *
     * @author Ali Asghar Marvi, KNIME AG, Zurich, Switzerland
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     */
    public static final class IsLongInteger extends PatternValidation {
        @Override
        protected String getPattern() {
            // this regex allows values between 9,223,372,036,854,775,808 and 9,999,999,999,999,999,999, but since the
            // LongAsStringPersistor is only a temporary solution and the regex for checking the exact long
            // range is extremely complex, we accept this limitation for now.
            return "[+-]?(0|[1-9][0-9]{0,18})";
        }

        @Override
        public String getErrorMessage() {
            return "Please enter a valid integer number between -9223372036854775808 to 9223372036854775807.";
        }
    }

    private final String m_cfgKey;

    /**
     * Constructor.
     *
     * @param cfgKey the config key of the long setting
     */
    protected LongAsStringPersistor(final String cfgKey) {
        m_cfgKey = cfgKey;
    }

    @Override
    public void save(final String param, final NodeSettingsWO settings) {
        settings.addLong(m_cfgKey, Long.parseLong(param));
    }

    @Override
    public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
        return String.valueOf(settings.getLong(m_cfgKey));
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][]{{m_cfgKey}};
    }
}
