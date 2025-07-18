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
 *   Jan 17, 2025 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static java.util.stream.Collectors.joining;
import static org.knime.core.webui.node.dialog.defaultdialog.util.SettingsTypeMapUtil.map;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.NodeDialog.OnApplyNodeModifier;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettingsImpl;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.NodeSettingsToDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.node.parameters.NodeParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Default implementation of a KaiNodeInterface for nodes that use {@link NodeParameters}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultKaiNodeInterface implements KaiNodeInterface {

    private final Map<SettingsType, Class<? extends NodeParameters>> m_settingsClasses;

    private final DefaultTextToNodeSettingsConverter m_textToNodeSettingsConverter;

    private final OnApplyNodeModifier m_onApplyModifier;

    /**
     * Constructor for nodes that are based on {@link NodeParameters}.
     *
     * @param settingsClasses the classes of settings of the node
     */
    public DefaultKaiNodeInterface(final Map<SettingsType, Class<? extends NodeParameters>> settingsClasses) {
        this(settingsClasses, null);
    }

    /**
     * Constructor for nodes that are based on {@link NodeParameters}.
     *
     * @param settingsClasses the classes of settings of the node
     * @param onApplyModifier allows to modify the node when the settings are applied
     */
    public DefaultKaiNodeInterface(final Map<SettingsType, Class<? extends NodeParameters>> settingsClasses,
        final OnApplyNodeModifier onApplyModifier) {
        m_settingsClasses = settingsClasses;
        m_textToNodeSettingsConverter = new DefaultTextToNodeSettingsConverter(settingsClasses);
        m_onApplyModifier = onApplyModifier;
    }

    @Override
    public ConfigurePrompt getConfigurePrompt(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        var jsonFormsSettings = getJsonFormsSettings(settings, specs);

        var currentSettings = jsonFormsSettings.getData();
        var schema = jsonFormsSettings.getSchema();

        var systemMessage = constructSystemMessage(currentSettings.toPrettyString(), specs);

        try {
            return new ConfigurePrompt(systemMessage, JsonFormsDataUtil.getMapper().writeValueAsString(schema));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write schema as string.", ex);
        }
    }

    private JsonFormsSettingsImpl getJsonFormsSettings(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        var context = NodeParametersUtil.createDefaultNodeSettingsContext(specs);
        final var loadedSettings = new NodeSettingsToDefaultNodeSettings(context, m_settingsClasses)
            .nodeSettingsToDefaultNodeSettingsOrDefault(map(settings));

        final var widgetTreeFactory = new WidgetTreeFactory();
        final var widgetTrees = map(loadedSettings, (type, s) -> widgetTreeFactory.createTree(s.getClass(), type));

        widgetTrees.values().forEach(KaiSchemaEnhancer::enhanceForKai);

        return new JsonFormsSettingsImpl(loadedSettings, context, widgetTrees);
    }

    /**
     * @param currentSettings json string representing the current settings
     * @param specs input specs of the dialog
     * @return a sytem message containing instructions on how to create the settings
     */
    public static String constructSystemMessage(final String currentSettings, final PortObjectSpec[] specs) {
        return new StringBuilder(
            "You are a KNIME AI assistant that configures nodes using JSON based on the given instruction.")//
                .append("\n\n")//
                .append("# Current settings\n\n")//
                .append(currentSettings)//
                .append("\n\n# Inputs\n\n")//
                .append(stringify(specs))//
                .toString();
    }

    private static String stringify(final PortObjectSpec[] specs) {
        var nc = NodeContext.getContext().getNodeContainer();
        final var flowObjectStack = getInputFlowObjectStack(nc);
        String inputs = IntStream.range(1, nc.getNrInPorts())//
            .mapToObj(i -> stringify(nc.getInPort(i).getPortType(), specs[i - 1]))//
            .collect(joining("\n"));

        inputs += "\n\n# Flow Variables\n\n" + writeFlowVarsAsJson(flowObjectStack).toPrettyString();

        return inputs;

    }

    private static String stringify(final PortType portType, final PortObjectSpec spec) {

        if (spec instanceof DataTableSpec tableSpec) {
            return tableSpec.stream()//
                .map(c -> "(%s, %s)".formatted(c.getName(), c.getType().getName()))//
                .collect(joining("[", ", ", "]"));
        }

        return portType.getName();

    }

    /**
     * Returns a JSON representation of the flow variables from the given {@link NodeContainer}.
     * <p>
     * Schema of the returned JSON:
     *
     * <pre>
     * type: object
     * properties:
     *     flow_variables:
     *         type: array
     *         items:
     *             type: object
     *             properties:
     *                 name:
     *                     type: string
     *                 type:
     *                     type: string
     * </pre>
     *
     * @param flowObjectStack the {@link NodeContainer} holding flow variable information
     * @return a {@link JsonNode} representing the flow variables
     * @throws IllegalArgumentException if the provided {@code NodeContainer} is {@code null}
     */
    static JsonNode writeFlowVarsAsJson(final FlowObjectStack flowObjectStack) {
        final var jsonNode = JsonFormsDataUtil.getMapper().createObjectNode();

        if (flowObjectStack == null) {
            return jsonNode;
        }

        final var flowVariables = jsonNode.putArray("flow_variables");
        final var allFlowVariables = flowObjectStack.getAllAvailableFlowVariables();
        allFlowVariables.values().stream().forEach(flowVar -> {
            final var varJson = flowVariables.addObject();
            varJson.put("name", flowVar.getName());
            varJson.put("type", flowVar.getVariableType().toString());
        });

        return jsonNode;
    }


    /**
     * Extracts and serializes the input flow variables of the given {@link NodeContainer}
     * into a JSON representation.
     * <p>
     * This method retrieves the {@link FlowObjectStack} representing input flow variables
     * from the specified {@code NodeContainer} and converts it to a JSON object following
     * the same schema as {@link #writeFlowVarsAsJson(FlowObjectStack)}.
     *
     * @param nc the {@link NodeContainer} from which to extract input flow variables
     * @return a {@link JsonNode} containing the serialized input flow variables
     * @throws IllegalArgumentException if the {@code NodeContainer} is {@code null}
     */
    @SuppressWarnings("javadoc")
    public static JsonNode writeInputFlowVarsAsJson(final NodeContainer nc) {
        final var inputFlowObjectStack = getInputFlowObjectStack(nc);
        return writeFlowVarsAsJson(inputFlowObjectStack);
    }


    /**
     * Extracts and serializes the output flow variables of the given {@link NodeContainer}
     * into a JSON representation.
     * <p>
     * This method retrieves the {@link FlowObjectStack} representing output flow variables
     * from the specified {@code NodeContainer} and converts it to a JSON object following
     * the same schema as {@link #writeFlowVarsAsJson(FlowObjectStack)}.
     *
     * @param nc the {@link NodeContainer} from which to extract output flow variables
     * @return a {@link JsonNode} containing the serialized output flow variables
     * @throws IllegalArgumentException if the {@code NodeContainer} is {@code null}
     */
    @SuppressWarnings("javadoc")
    public static JsonNode writeOutputFlowVarsAsJson(final NodeContainer nc) {
        final var outputFlowObjectStack = getOutputFlowObjectStack(nc);
        return writeFlowVarsAsJson(outputFlowObjectStack);
    }


    /**
     * Retrieves the {@link FlowObjectStack} from the implicit flow variable output port (index 0)
     * of the given {@link NodeContainer}. Implicit flow variable output port contains all flow variables
     * (also the ones pushed by the node).
     *
     * @param nc the {@link NodeContainer} instance from which to get the output port's flow object stack
     * @return the {@link FlowObjectStack} associated with the first output port of the given {@link NodeContainer}
     */
    private static FlowObjectStack getOutputFlowObjectStack(final NodeContainer nc) {
        return nc.getOutPort(0).getFlowObjectStack();
    }

    /**
     * Retrieves the {@link FlowObjectStack} directly associated with the given {@link NodeContainer}.
     *
     * @param nc the {@link NodeContainer} instance from which to get the input flow object stack
     * @return the {@link FlowObjectStack} directly associated with the given {@link NodeContainer}
     */
    private static FlowObjectStack getInputFlowObjectStack(final NodeContainer nc) {
        return nc.getFlowObjectStack();
    }

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return m_settingsClasses.keySet();
    }

    /**
     * Public method for analysis purposes.
     *
     * @return the settings classes of the node
     */
    public Map<SettingsType, Class<? extends NodeParameters>> getSettingsClasses() {
        return m_settingsClasses;
    }

    @Override
    public Optional<OnApplyNodeModifier> getOnApplyNodeModifier() {
        return Optional.ofNullable(m_onApplyModifier);
    }

    @Override
    public void applyConfigureResponse(final String response,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) throws InvalidSettingsException {
        m_textToNodeSettingsConverter.toNodeSettings(response, previousSettings, settings);
    }

}
