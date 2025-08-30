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
 *   Aug 20, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jobmanager;

import static org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil.JOB_MANAGER_KEY_FE;

import java.util.Map;
import java.util.Optional;

import org.knime.core.webui.node.dialog.PersistSchema;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * It's not possible to set flow variables for parameters in the job manager section.
 *
 * @author Paul Bärnreuther
 */
public final class JobManagerParametersPersistUtil {

    private JobManagerParametersPersistUtil() {
        // Utility class
    }

    private static final PersistSchema.PersistLeafSchema PERSIST_LEAF_SCHEMA_WITH_EMPTY_CONFIG_PATHS =
        new PersistSchema.PersistLeafSchema() {

            @Override
            public Optional<String[][]> getConfigPaths() {
                return Optional.of(new String[0][]);
            }

        };

    /**
     * This method disables flow variable support for the job manager persist schema by adding a persist schema with
     * empty config paths
     *
     * @param persistSchemaMap the map of persist schemas to add the job manager persist schema to
     */
    public static void setPersistSchema(final Map<String, PersistSchema> persistSchemaMap) {
        persistSchemaMap.put(JOB_MANAGER_KEY_FE, PERSIST_LEAF_SCHEMA_WITH_EMPTY_CONFIG_PATHS);
    }

    /**
     * This method disables flow variable support for the job manager persist schema by adding a persist schema with
     * empty config paths
     *
     * @param persistSchema to set the persist schema on
     */
    public static void setPersistSchema(final ObjectNode persistSchema) {
        persistSchema.putObject(JOB_MANAGER_KEY_FE).putArray("configPaths"); // See {@link PersistUtil}
    }
}
