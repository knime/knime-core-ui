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
 *   Jul 18, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Objects;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.webui.data.util.InputPortUtil;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigMappingsFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class NodeParametersUtil {

    private NodeParametersUtil() {
        // utility class
    }

    /**
     * Method to create a new {@link NodeParametersInput} from input {@link PortObjectSpec PortObjectSpecs}.
     *
     * @param specs the non-null specs with which to create the schema
     * @return the newly created context
     * @throws NullPointerException if the argument is null
     */
    public static NodeParametersInput createDefaultNodeSettingsContext(final PortObjectSpec[] specs) {
        Objects.requireNonNull(specs, () -> "Port object specs must not be null.");
        final var nodeContext = NodeContext.getContext();
        if (nodeContext == null) {
            // can only happen during tests
            return new NodeParametersInputImpl(fallbackPortTypesFor(specs), null, specs, null, null, null, null, null,
                null, null);
        }
        final var nc = nodeContext.getNodeContainer();
        final PortType[] inPortTypes;
        final PortType[] outPortTypes;
        if (nc instanceof SingleNodeContainer snc) {
            // skip hidden flow variable input (mickey mouse ear) - not exposed to node implementation
            inPortTypes = IntStream.range(1, snc.getNrInPorts()).mapToObj(snc::getInPort).map(NodeInPort::getPortType)
                .toArray(PortType[]::new);
            outPortTypes = IntStream.range(1, snc.getNrOutPorts()).mapToObj(snc::getOutPort)
                .map(NodeOutPort::getPortType).toArray(PortType[]::new);
        } else {
            inPortTypes = fallbackPortTypesFor(specs);
            outPortTypes = null;
        }
        DialogNode dialogNode = null;
        WizardNode wizardNode = null;
        PortsConfiguration portConfig = null;
        URLConfiguration urlConfig = null;
        CredentialsProvider credentialsProvider = null;
        if (nc instanceof NativeNodeContainer nnc) {
            credentialsProvider = nnc.getNode().getCredentialsProvider();
            final var nodeModel = nnc.getNode().getNodeModel();
            if (nodeModel instanceof DialogNode model) {
                dialogNode = model;
            }
            if (nodeModel instanceof WizardNode model) {
                wizardNode = model;
            }
            final var creationConfig = nnc.getNode().getCopyOfCreationConfig();
            portConfig = creationConfig.flatMap(ModifiableNodeCreationConfiguration::getPortConfig).orElse(null);
            urlConfig = creationConfig.flatMap(ModifiableNodeCreationConfiguration::getURLConfig).orElse(null);
        }

        final var inPortObjects = nc.getParent() == null // This function is used by tests that mock the container
            ? new PortObject[0] // When mocked the container is not a child of a workflow manager
            : InputPortUtil.getInputPortObjectsExcludingVariablePort(nc);

        return new NodeParametersInputImpl(inPortTypes, outPortTypes, specs, nc.getFlowObjectStack(),
            credentialsProvider, inPortObjects, dialogNode, portConfig, urlConfig, wizardNode);
    }

    private static PortType[] fallbackPortTypesFor(final PortObjectSpec[] specs) {
        return IntStream.range(0, specs.length).mapToObj(i -> PortObject.TYPE).toArray(PortType[]::new);
    }

    /**
     * Verifies a given node settings implementation, making sure that it follows the contract of
     * {@link NodeParameters}, as defined in its documentation.
     *
     * @param settingsClass the settings class to verify
     */
    public static void verifySettings(final Class<? extends NodeParameters> settingsClass) {
        try {
            settingsClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            NodeLogger.getLogger(NodeParameters.class).errorWithFormat(
                "Default node settings class %s does not provide a default constructor.",
                settingsClass.getSimpleName());
        } catch (SecurityException e) {
            NodeLogger.getLogger(NodeParameters.class)
                .error(String.format(
                    "Exception when attempting to access default constructor of default node settings class %s.",
                    settingsClass.getSimpleName()), e);
        }
    }

    /**
     * Helper to serialize a {@link NodeParameters} of specified class from a {@link NodeSettingsRO}-object.
     *
     * @param <S>
     * @param settings the settings-object to create the instance from
     * @param clazz default node settings class
     * @return a new {@link NodeParameters}-instance
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static <S extends NodeParameters> S loadSettings(final NodeSettingsRO settings, final Class<S> clazz)
        throws InvalidSettingsException {
        return SettingsLoaderFactory.loadSettings(clazz, settings);
    }

    /**
     * Helper to create a new {@link NodeParameters} of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz default node settings class
     * @param specs the specs with which to create the settings. NOTE: can contain {@code null} values, e.g., if input
     *            port is not connected
     * @return a new {@link NodeParameters}-instance
     */
    public static <S extends NodeParameters> S createSettings(final Class<S> clazz, final PortObjectSpec[] specs) {
        return InstantiationUtil.createDefaultNodeSettings(clazz, createDefaultNodeSettingsContext(specs));
    }

    /**
     * Helper to create a new {@link NodeParameters} of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz default node settings class
     * @param context the {@link NodeParametersInput} to be used as constructor argument
     * @return a new {@link NodeParameters}-instance
     */
    public static <S extends NodeParameters> S createSettings(final Class<S> clazz, final NodeParametersInput context) {
        return InstantiationUtil.createDefaultNodeSettings(clazz, context);
    }

    /**
     * Creates a new {@link NodeParameters} object of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz the class of the NodeParameters type
     * @return a new instance of the DefaultNodeSettingsType
     */
    public static <S extends NodeParameters> S createSettings(final Class<S> clazz) {
        return InstantiationUtil.createInstance(clazz);
    }

    @SuppressWarnings("javadoc")
    public static void saveSettings(final Class<? extends NodeParameters> settingsClass,
        final NodeParameters settingsObject, final NodeSettingsWO settings) {
        castAndSaveSettings(settingsClass, settingsObject, settings);
    }

    @SuppressWarnings("unchecked") // we check that the cast is save
    private static <S extends NodeParameters> void castAndSaveSettings(final Class<S> settingsClass,
        final NodeParameters settingsObject, final NodeSettingsWO settings) {
        CheckUtils.checkArgument(settingsClass.isInstance(settingsObject),
            "The provided settingsObject is not an instance of the provided settingsClass.");
        SettingsSaverFactory.saveSettings((S)settingsObject, settings);

    }

    /**
     * @param <S> the type of DefaultNodeSettings
     * @param settingsClass
     * @param settingsObject
     * @return the tree of modifications that needs to be traversed after saving to node settings in order to align
     *         settings and flow variables.
     * @see NodeSettingsCorrectionUtil
     */
    public static <S extends NodeParameters> ConfigMappings getConfigMappings(final Class<S> settingsClass,
        final NodeParameters settingsObject) {
        return ConfigMappingsFactory.createConfigMappings(settingsClass, settingsObject);
    }
}
