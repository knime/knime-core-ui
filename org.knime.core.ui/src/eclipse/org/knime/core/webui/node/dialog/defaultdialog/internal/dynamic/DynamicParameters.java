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
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.schema.JsonFormsSchemaUtil.buildSchema;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil.buildUISchema;
import static org.knime.core.webui.node.dialog.defaultdialog.util.JacksonSerializationUtil.serialize;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.PersistUtil;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl.DynamicNodeParametersDeserializer;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.impl.DynamicNodeParametersSerializer;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persisttree.PersistTreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.TriggerInvocationHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.StateProvider;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Put this annotation on a field of an interface/abstract class type extending {@link DynamicNodeParameters} in order
 * to provide concrete implementations of that type within the dialog.
 *
 * This is mandatory, as there will be a runtime exception if such fields are not annotated with this annotation.
 *
 * @author Paul Bärnreuther
 */
@Retention(RUNTIME)
@Target(FIELD)
@JacksonAnnotationsInside
@JsonDeserialize(using = DynamicNodeParametersDeserializer.class)
@JsonSerialize(using = DynamicNodeParametersSerializer.class)
public @interface DynamicParameters {

    /**
     * The class of the {@link StateProvider} that provides the dialog.
     *
     * @return the class of the {@link StateProvider} that provides the dialog
     */
    Class<? extends DynamicParametersProvider<?>> value();

    /**
     * Dynamic parameters result in a schema `{ "type": "object" }` that is not useful for KAI to configure them
     * correctly.
     *
     * With this field one can adjust that schema to another constant schema. This schema should mention explicit
     * '@class' values, e.g. like so: <code>
     *  {
     *   "type": "object",
     *   "properties": {
     *     "@class": {
     *       "const": "org.knime.fully.qualified.ClassName$OfClassThatCanHandleThis"
     *     },
     *     "otherProperty": {
     *        "type": "string",
     *        "title": "A title",
     *        "description": "A description"
     *     }
     *  }
     *  </code>
     *
     * @return the schema to present to KAI instead of the default `{ "type": "object" }` schema
     */
    String schemaForDefaultKaiNodeInterface()

    default "";

    /**
     * Title and description provided here are only shown in the node description. If this field is not set, the dynamic
     * parameters are omitted from the node description.
     *
     * @return the title of the dynamic parameters
     */
    Widget widgetAppearingInNodeDescription() default @Widget(title = "", description = "");

    /**
     * Value of the {@link DynamicParameters} annotation. Use this to define a dynamic part of a dialog by providing the
     * {@link NodeParameters} of that part as state.
     *
     * @param <T> the common interface/abstract class used for these dynamic settings. I.e. this should be the type of
     *            the annotated field.
     */
    interface DynamicParametersProvider<T extends DynamicNodeParameters> extends StateProvider<DataAndDialog<Object>> {

        /**
         * Provides the strategy for handling class identification. This method returns an instance that can map between
         * class identifiers and actual classes, supporting backwards compatibility and custom naming.
         *
         * @return the class identification strategy for this provider
         */
        ClassIdStrategy<T> getClassIdStrategy();

        /**
         * @noimplement this default-implementation should not be overwritten.
         */
        @Override
        default DataAndDialog<Object> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var data = computeParameters(parametersInput);
            final var dataClass = data.getClass();
            final var dataMapper = JsonFormsDataUtil.getMapper();
            final var serializedData = serialize(data, new DynamicNodeParametersSerializer(this));

            final var widgetTree = new WidgetTreeFactory().createTree(dataClass);
            final var schema = buildSchema(dataClass, widgetTree, parametersInput, dataMapper);
            final var uiSchema = buildUISchema(List.of(widgetTree), parametersInput);
            final var updates =
                UpdatesUtil.constructUpdates(widgetTree, (ObjectNode)serializedData, parametersInput, null);
            final Supplier<TriggerInvocationHandler<String>> triggerInvocationHandler =
                () -> TriggerInvocationHandler.fromWidgetTrees(List.of(widgetTree), parametersInput);

            final var persistTree = new PersistTreeFactory().createTree(dataClass);
            final var persist = PersistUtil.constructPersist(persistTree);

            try {
                return new DataAndDialog<>(//
                    serializedData, //
                    dataMapper.writeValueAsString(schema), //
                    dataMapper.writeValueAsString(uiSchema), //
                    dataMapper.writeValueAsString(persist), //
                    dataMapper.writeValueAsString(updates), //
                    triggerInvocationHandler//
                );
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to serialize dialog parts.", ex);
            }

        }

        /**
         * Compute method taking value suppliers provided by the {@link #init} method and returns the to be used
         * parameters.
         *
         * Note that it might be useful to self-reference the current field using a value reference in order to preserve
         * state from one dynamic parameters implementation to the next.
         *
         * @param parametersInput the current input of parameter creation.
         * @return the next parameters as they should be updated in the dialog.
         * @throws StateComputationFailureException if this computation should be cancelled for some reason.
         */
        T computeParameters(NodeParametersInput parametersInput) throws StateComputationFailureException;

    }

    /**
     * Use this interface as {@link DynamicParametersProvider} if you want to have some or all of the dynamic parameters
     * be defined via {@link NodeSettings} and displayed as fallback dialog.
     *
     * @author Paul Bärnreuther
     * @param <T> the common interface/abstract class used for these dynamic settings.
     */
    interface DynamicParametersWithFallbackProvider<T extends DynamicNodeParameters>
        extends DynamicParametersProvider<T> {

        @Override
        default DataAndDialog<Object> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var fallbackSettings = computeFallbackSettings(parametersInput);
            if (fallbackSettings == null) {
                return DynamicParametersProvider.super.computeState(parametersInput);
            }
            final var fallbackParameters = getParametersFromFallback(fallbackSettings);
            final var jsonFormsSettings = fallbackParameters.toJsonFormsSettings();
            final var mapper = JsonFormsDataUtil.getMapper();
            try {
                return new DataAndDialog<>(//
                    jsonFormsSettings.getData(), //
                    mapper.writeValueAsString(jsonFormsSettings.getSchema()), //
                    mapper.writeValueAsString(jsonFormsSettings.getUiSchema())//
                );
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Failed to serialize dialog parts.", ex);
            }

        }

        /**
         * Return something that should be used to create a fallback dialog or null if no fallback dialog should be
         * used. In the latter case the dynamic parameters as defined by
         * {@link DynamicParametersProvider#computeParameters(NodeParametersInput)} are used.
         *
         * @param parametersInput the current input of parameter creation.
         * @return the node settings to be used for the fallback dialog or null if no fallback dialog should be
         * @throws StateComputationFailureException if this computation should be cancelled for some reason.
         */
        NodeSettings computeFallbackSettings(NodeParametersInput parametersInput)
            throws StateComputationFailureException;

        /**
         * Used for construction of the concrete non-abstract class that extends both T and
         * {@link FallbackDialogNodeParameters} from the given node settings.
         *
         * @param fallbackSettings the node settings to construct the fallback dialog from (either loaded or provided)
         *
         * @return a value that extends both T and {@link FallbackDialogNodeParameters}
         */
        FallbackDialogNodeParameters getParametersFromFallback(NodeSettingsRO fallbackSettings);

    }

    /**
     * Extend this interface and use the sub-interface as field in {@link NodeParameters} together with a
     * {@link DynamicParameters} annotation for non-concrete dynamic parts of dialogs.
     */
    interface DynamicNodeParameters extends NodeParameters {

    }

    /**
     * Use this annotation on implementations of {@link DynamicNodeParameters} in order to maintain
     * backwards-compatibility when moving or renaming the class. I.e. when no such annotation is set on a class, the
     * class name is used when persisting and restoring the settings and when it is set, the value of the annotation is
     * used instead.
     */
    @Retention(RUNTIME)
    @Target(TYPE)
    @interface OriginalClassName {
        String value();
    }

}
