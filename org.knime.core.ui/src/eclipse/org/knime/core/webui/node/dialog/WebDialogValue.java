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
 *   Apr 15, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.knime.core.node.dialog.DialogNodeValue;
import org.knime.core.node.web.WebViewContent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Value of a configuration node whose representation is a {@link WebDialogNodeRepresentation}. It allows translating to
 * and from json.
 *
 * @author Paul Bärnreuther
 */
public interface WebDialogValue extends DialogNodeValue {

    /**
     * Transform the value to a json that is to be used in a web dialog.
     *
     * @return a json representation.
     * @throws IOException Exception that can occur serializing the value.
     */
    JsonNode toDialogJson() throws IOException;

    /**
     * Inverse method to {@link #toDialogJson()}. Up until now this method does not need to be able to deserialize from
     * previous versions of generated json in case {@link #toDialogJson()} changed.
     *
     * @param json a json representation from a web dialog which has the same structure as the returned value of
     *            {@link #toDialogJson()}
     *
     * @throws IOException Exception that can occur on construction..
     */
    void fromDialogJson(JsonNode json) throws IOException;

    /**
     * Default implementation of {@link WebDialogValue} for a {@link WebViewContent}.
     *
     * @author Paul Bärnreuther
     */
    interface WebDialogContent extends WebViewContent, WebDialogValue {

        ObjectMapper MAPPER = new ObjectMapper();

        @Override
        default JsonNode toDialogJson() throws IOException {
            try (final var stream = (ByteArrayOutputStream)saveToStream()) {
                return MAPPER.readTree(stream.toByteArray());
            }

        }

        @Override
        default void fromDialogJson(final JsonNode json) throws IOException {
            final var stream = new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
            loadFromStream(stream);
        }

    }

}
