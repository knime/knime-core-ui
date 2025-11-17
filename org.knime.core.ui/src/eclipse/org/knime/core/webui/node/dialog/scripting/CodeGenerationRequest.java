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
 *   Apr 4, 2025 (benjaminwilhelm): created
 */
package org.knime.core.webui.node.dialog.scripting;

import org.knime.core.webui.node.dialog.scripting.InputOutputModelNameAndTypeUtils.NameAndType;

/**
 * A request for the K-AI code generation.
 *
 * @param endpointPath The endpoint path to send the request to.
 * @param body The body of the request.
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public record CodeGenerationRequest(String endpointPath, RequestBody body) {

    /**
     * The body of a code generation request.
     *
     * @param code The existing code in the scripting node.
     * @param user_query The user's query for code generation.
     * @param inputs The input data available in the node.
     * @param outputs The expected outputs of the node.
     * @param version The version of the plugin making the request (can be <code>null</code>).
     */
    public record RequestBody(String code, String user_query, CodeGenerationRequest.Inputs inputs,
        CodeGenerationRequest.Outputs outputs, String version) {

        /**
         * Constructor without version.
         *
         * @param code The existing code in the scripting node.
         * @param user_query The user's query for code generation.
         * @param inputs The input data available in the node.
         * @param outputs The expected outputs of the node.
         */
        public RequestBody(final String code, final String user_query, final CodeGenerationRequest.Inputs inputs,
            final CodeGenerationRequest.Outputs outputs) {
            this(code, user_query, inputs, outputs, null);
        }
    }

    /**
     * Inputs available to the node.
     *
     * @param tables The names and types of the input columns for each input table.
     * @param num_objects The number of input objects (tables, images, etc.).
     * @param flow_variables The names and types of the available flow variables.
     */
    public record Inputs(NameAndType[][] tables, long num_objects, NameAndType[] flow_variables) { // NOSONAR: we don't need hash or equals here
    }

    /**
     * Outputs expected from the node.
     *
     * @param num_tables The number of output tables.
     * @param num_objects The number of output objects (tables, images, etc.).
     * @param num_images The number of output images.
     * @param has_view Whether the node has a view.
     */
    public record Outputs(long num_tables, long num_objects, long num_images, boolean has_view) {
    }
}
