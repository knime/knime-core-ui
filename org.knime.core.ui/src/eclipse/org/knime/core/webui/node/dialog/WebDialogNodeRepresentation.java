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
 *   Apr 10, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.dialog.DialogNodeRepresentation;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Let a dialog node representation implement this interface to enable being part of a WebUI component dialog.
 *
 * @author Paul Bärnreuther
 * @param <VAL> the dialog node value type.
 */
public interface WebDialogNodeRepresentation<VAL extends WebDialogValue> extends DialogNodeRepresentation<VAL> {

    /**
     * @return the specification of a renderer that should be used in a WebUI dialog. Used control renderers (i.e.
     *         renderers controlling a setting) have to be localized to a json path within the json generated from a
     *         value via {@link WebDialogValue#toDialogJson}.
     *
     *         E.g. if {@link WebDialogValue#toDialogJson} returns an object with a string property "value" and a
     *         renderer spec operating on a string value is to be used (e.g. the {@link TextRendererSpec}), that has to
     *         be localized to "value".
     */
    @SuppressWarnings("rawtypes")
    @JsonIgnore // otherwise a cyclic dependency arises
    DialogElementRendererSpec getWebUIDialogElementRendererSpec();

    /**
     * This method has to be overwritten in case the settings structure of the JSON value of the dialog node is not the
     * same as the structure of the saved {@link NodeSettings}.
     *
     * @return an optional schema that describes the relation to the persisted structure of the JSON dialog node value.
     */
    @JsonIgnore
    default Optional<PersistSchema> getPersistSchema() {
        // By default, the dialog node value is persisted in the same structure as it is rendered in the dialog.
        return Optional.empty();
    }

    /**
     * This method transforms the value of this node to a JSON representation suitable to be rendered using the result
     * of {@link #getWebUIDialogElementRendererSpec()}.
     *
     * @param value the to be transformed value
     * @return a json representation.
     * @throws IOException Exception that can occur serializing the value.
     */
    JsonNode transformValueToDialogJson(VAL value) throws IOException;

    /**
     * Necessary, since we extract representation and value separately from the node.
     *
     * @noimplement
     */
    @SuppressWarnings({"javadoc", "unchecked"})
    default JsonNode castAndTransformValueToDialogJson(final Object value) throws IOException {
        return transformValueToDialogJson((VAL)value);
    }

    /**
     * Inverse method to {@link #transformValueToDialogJson}. Up until now this method does not need to be able to
     * deserialize from previous versions of generated json in case {@link #transformValueToDialogJson} changed.
     *
     * @param json a json representation from a web dialog which has the same structure as the returned value of
     *            {@link #transformValueToDialogJson}
     * @param value the value to be set
     * @param dialogNodeRepresentation the node representation in the dialog
     *
     * @throws IOException Exception that can occur on construction..
     */
    void setValueFromDialogJson(JsonNode json, VAL value) throws IOException;

    /**
     * Necessary, since we extract representation and value separately from the node.
     *
     * @noimplement
     */
    @SuppressWarnings({"javadoc", "unchecked"})
    default void castAndSetValueFromDialogJson(final JsonNode json, final Object value) throws IOException {
        setValueFromDialogJson(json, (VAL)value);
    }

    /**
     * Default implementation of {@link WebDialogNodeRepresentation} for a value whose json representation does not
     * depend on the node representation.
     *
     * @param <T> the dialog node value type
     */
    interface DefaultWebDialogNodeRepresentation<T extends WebDialogValue> extends WebDialogNodeRepresentation<T> {

        @Override
        default JsonNode transformValueToDialogJson(final T value) throws IOException {
            return value.toDialogJson();
        }

        @Override
        default void setValueFromDialogJson(final JsonNode json, final T value) throws IOException {
            value.fromDialogJson(json);
        }

    }

}

