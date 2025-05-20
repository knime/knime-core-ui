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
 *   Apr 29, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.impl.analytics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettingsImpl;
import org.knime.core.webui.node.dialog.defaultdialog.util.GenericTypeFinderUtil;
import org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for extracting information about a nodes webUI dialog based on its factory.
 *
 * @author Paul Bärnreuther
 */
public final class WebUIDialogDetailsUtil {

    private WebUIDialogDetailsUtil() {
        // Utility
    }

    /**
     * Details on the existence and configuration of the WebUI dialog of a node.
     *
     * @param hasWebUIDialog true if the node factory implements a dialog
     * @param hasWebUIModel for the model extracted from the node factory
     * @param hasWebUIModelMessage that can be present in case hasWebUIModel is false
     * @param settings the extracted settings
     * @param settingsMessage that can be present in case settings is null
     */
    public record WebUIDialogDetails(boolean hasWebUIDialog, boolean hasWebUIModel, String hasWebUIModelMessage,
        WebUISettings settings, String settingsMessage) {

    }

    /**
     * Settings configuration for the WebUI dialog of a node.
     *
     * @param schema the schema
     * @param uiSchema the UI schema
     */
    public record WebUISettings(String schema, String uiSchema) {

        static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

        WebUISettings(final JsonFormsSettings settings) throws JsonProcessingException {
            this(//
                MAPPER.writeValueAsString(settings.getSchema()), //
                MAPPER.writeValueAsString(settings.getUiSchema())//
            );
        }

    }

    /**
     * Extract information on the WebUI dialog and the WebUI model settings from the given node factory. Currently, only
     * model settings are considered even if a node that has view settings is passed in.
     *
     * @param factory the node factory
     * @return the information on the WebUI dialog and the WebUI model settings
     */
    @SuppressWarnings({"unchecked", "restriction"})
    public static WebUIDialogDetails extractWebUIStatistics(final NodeFactory<?> factory) {
        final var hasWebUIDialog = factory instanceof NodeDialogFactory;
        if (!hasWebUIDialog) {
            return new WebUIDialogDetails(false, false, null, null, null);
        }
        if (factory instanceof KaiNodeInterfaceFactory kaiNodeInterfaceFactory
            && kaiNodeInterfaceFactory.createKaiNodeInterface() instanceof DefaultKaiNodeInterface kaiNodeInterface) {
            return extractDetailsFromKaiNodeInterface(kaiNodeInterface, factory);
        }
        final UnaryOperator<String> toNotKaiNodeInterfaceMessage =
            string -> String.format("Node factory does not extend KAINodeInterface; %s", string);

        final var nodeModelClassOptional = extractNodeModelClass(factory);
        if (nodeModelClassOptional.isEmpty()) {
            return new WebUIDialogDetails(true, false,
                "Unable to extract the node model class from the node factory class", null,
                toNotKaiNodeInterfaceMessage.apply("No model settings could be extracted"));
        }
        final var nodeModelClass = nodeModelClassOptional.get().getSecond();

        final var hasWebUIModel = WebUINodeModel.class.isAssignableFrom(nodeModelClass)
            || WebUISimpleStreamableFunctionNodeModel.class.isAssignableFrom(nodeModelClass);

        if (!hasWebUIModel) {
            return new WebUIDialogDetails(true, false, null, null, toNotKaiNodeInterfaceMessage
                .apply("Unable to extract model settings from node model since it is not webUI."));
        }

        Class<? extends DefaultNodeSettings> modelSettingsClass;
        try {
            final Class<?> genericSuperClass = WebUINodeModel.class.isAssignableFrom(nodeModelClass)
                ? WebUINodeModel.class : WebUISimpleStreamableFunctionNodeModel.class;
            final var nodeModelType = nodeModelClassOptional.get().getFirst();
            modelSettingsClass = (Class<? extends DefaultNodeSettings>)GenericTypeFinderUtil
                .getFirstGenericTypeFromType(nodeModelType, genericSuperClass);
        } catch (IndexOutOfBoundsException e) { // NOSONAR
            return new WebUIDialogDetails(hasWebUIDialog, true, null, null, toNotKaiNodeInterfaceMessage
                .apply("Unable to extract the model settings class from the generic types of the webUI node model."));
        }
        return constructDetails(true, null, Map.of(SettingsType.MODEL, modelSettingsClass));
    }

    private static Optional<Pair<Type, Class<?>>> extractNodeModelClass(final NodeFactory<?> factory) {
        Type nodeModelType;
        try {
            nodeModelType = GenericTypeFinderUtil.getFirstGenericType(factory.getClass(), NodeFactory.class);
        } catch (IndexOutOfBoundsException ex) { // NOSONAR
            nodeModelType = null;
        }
        if (!(nodeModelType instanceof ParameterizedType) && !(nodeModelType instanceof Class<?>)) {
            try {
                nodeModelType = factory.createNodeModel().getClass();
            } catch (Exception ex) { // NOSONAR
                return Optional.empty();

            }
        }
        Class<?> nodeModelClass;
        if (nodeModelType instanceof Class<?> nodeModelClass1) {
            nodeModelClass = nodeModelClass1;
        } else {
            nodeModelClass = (Class<?>)((ParameterizedType)nodeModelType).getRawType();
        }
        return Optional.of(new Pair<>(nodeModelType, nodeModelClass));

    }

    private static WebUIDialogDetails extractDetailsFromKaiNodeInterface(final DefaultKaiNodeInterface kaiNodeInterface,
        final NodeFactory<?> factory) {
        final boolean hasWebUIModel;
        final String hasWebUIModelMessage;
        final var nodeModelClassOptional = extractNodeModelClass(factory);
        if (nodeModelClassOptional.isEmpty()) {
            hasWebUIModel = false;
            hasWebUIModelMessage = "Unable to extract the node model class from the node factory class";
        } else {
            final var nodeModelClass = nodeModelClassOptional.get().getSecond();
            hasWebUIModel = WebUINodeModel.class.isAssignableFrom(nodeModelClass)
                || WebUISimpleStreamableFunctionNodeModel.class.isAssignableFrom(nodeModelClass);
            hasWebUIModelMessage = null;
        }
        final var settingsClasses = kaiNodeInterface.getSettingsClasses();
        return constructDetails(hasWebUIModel, hasWebUIModelMessage, settingsClasses);

    }

    private static WebUIDialogDetails constructDetails(final boolean hasWebUIModel, final String hasWebUIModelMessage,
        final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses) {
        try {
            final var jsonFormsSettings = extractJsonFormsSettings(settingsClasses);
            return new WebUIDialogDetails(true, hasWebUIModel, hasWebUIModelMessage, jsonFormsSettings, null);
        } catch (Exception ex) { //NOSONAR
            return new WebUIDialogDetails(true, hasWebUIModel, hasWebUIModelMessage, null,
                "Unable to extract the settings: " + ex.getMessage());
        }
    }

    private static WebUISettings extractJsonFormsSettings(
        final Map<SettingsType, Class<? extends DefaultNodeSettings>> settingsClasses) throws JsonProcessingException {
        final Map<SettingsType, DefaultNodeSettings> settings =
            MapValuesUtil.mapValues(settingsClasses, DefaultNodeSettings::createSettings);
        final var context = DefaultNodeSettings.createDefaultNodeSettingsContext(new PortObjectSpec[0]);
        final var jsonFormsSettings = new JsonFormsSettingsImpl(settings, context);
        return new WebUISettings(jsonFormsSettings);
    }

}
