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
 *   18 Mar 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.DialogElementRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.RendererToJsonFormsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Put this annotation on a Map<String, Object> field in a {@link DefaultNodeSettings} class to provide a dialog to
 * operate on that map.
 *
 * @author Robin Gerling
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface DynamicSettingsWidget {

    /**
     * The class of the {@link StateProvider} that provides the dialog.
     *
     * @return the class of the {@link StateProvider} that provides the dialog
     */
    Class<? extends StateProvider<? extends DataAndDialog<?>>> value();

    /**
     * The provided value of the given state provider. This will update the value of the annotated field in case a
     * non-null value is provided and it will define the next dialog specification that is to be used.
     *
     * @author Paul Bärnreuther
     * @param <T> the type of data that is provided by the state provider
     */
    class DataAndDialog<T> {

        @JsonProperty("data")
        T m_data;

        @JsonProperty("schema")
        String m_schema;

        @JsonProperty("uiSchema")
        String m_uiSchema;

        /**
         * Creates a new instance of {@link DataAndDialog}.
         *
         * @param data the data to be used in the dialog. Use null to prevent an update
         * @param schema the JSON schema to be used in the dialog.
         * @param uiSchema the JSON UI schema to be used in the dialog.
         */
        public DataAndDialog(final T data, final String schema, final String uiSchema) {
            m_data = data;
            m_schema = schema;
            m_uiSchema = uiSchema;
        }

    }

    /**
     * Use this interface to provide a dialog for a map of settings in an imperative way.
     *
     * TODO: UIEXT-2592 introduce a declarative version of this interface, which allows to specify only an instance of
     * {@link DefaultNodeSettings} using the JsonFormsSettingsImpl
     */
    interface ImperativeDialogProvider extends StateProvider<DataAndDialog<Map<String, Object>>> {

        @Override
        default DataAndDialog<Map<String, Object>> computeState(final DefaultNodeSettingsContext context)
            throws StateComputationFailureException {
            final var pair = computeSettingsAndDialog(context);
            final var dialog = pair.getSecond();
            final var uiSchema = RendererToJsonFormsUtil.toUiSchemaElement(dialog);
            final var schema = RendererToJsonFormsUtil.constructSchema(dialog);
            return new DataAndDialog<>(pair.getFirst(), schema.toString(), uiSchema.toString());
        }

        /**
         * The next settings together with a suitable dialog element renderer to display them in the dialog. Controls
         * used within the dialog need to be localized to keys of the map.
         *
         * @param context the context in which the dialog element renderer is computed
         * @return the next settings and the associated dialog element renderer specification
         * @throws StateComputationFailureException if the computation of the settings or dialog fails in an expected
         *             way
         */
        Pair<Map<String, Object>, DialogElementRendererSpec<?>>
            computeSettingsAndDialog(DefaultNodeSettingsContext context) throws StateComputationFailureException;

    }

}
