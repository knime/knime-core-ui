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
 *   Jul 11, 2025 (marcbux): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.util.Objects;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.webui.data.util.InputPortUtil;
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
     * Helper to serialize a {@link NodeParameters} of specified class from a {@link NodeSettingsRO}-object.
     *
     * @param <P> the type of the NodeParameters
     * @param settings the settings object to create the instance from
     * @param clazz node parameters class
     * @return a new {@link NodeParameters} instance
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static <P extends NodeParameters> P loadParameters(final NodeSettingsRO settings, final Class<P> clazz)
        throws InvalidSettingsException {
        return SettingsLoaderFactory.loadSettings(clazz, settings);
    }

    /**
     * Helper to create a new {@link NodeParameters} of the specified type.
     *
     * @param <P> the type of NodeParameters
     * @param clazz node parameters class
     * @param specs the specs with which to create the parameters. NOTE: can contain {@code null} values, e.g., if input
     *            port is not connected
     * @return a new {@link NodeParameters} instance
     */
    public static <P extends NodeParameters> P createParamaters(final Class<P> clazz, final PortObjectSpec[] specs) {
        return InstantiationUtil.createNodeParameters(clazz,
            NodeParametersUtil.createDefaultNodeSettingsContext(specs));
    }

    /**
     * Helper to create a new {@link NodeParameters} of the specified type.
     *
     * @param <P> the type of NodeParameters
     * @param clazz node parameters class
     * @param context the {@link NodeParametersInput} to be used as constructor argument
     * @return a new {@link NodeParameters} instance
     */
    public static <P extends NodeParameters> P createParameters(final Class<P> clazz,
        final NodeParametersInput context) {
        return InstantiationUtil.createNodeParameters(clazz, context);
    }

    /**
     * Creates a new {@link NodeParameters} object of the specified type.
     *
     * @param <P> the type of NodeParameters
     * @param clazz node parameters class
     * @return a new {@link NodeParameters} instance
     */
    public static <P extends NodeParameters> P createParameters(final Class<P> clazz) {
        return InstantiationUtil.createInstance(clazz);
    }

    /**
     * Helper to save a {@link NodeParameters} of specified class to a {@link NodeSettingsWO} object.
     *
     * @param clazz node parameters class
     * @param parameters parameters instance to save
     * @param settings settings object to save the parameters to
     */
    public static void saveParameters(final Class<? extends NodeParameters> clazz, final NodeParameters parameters,
        final NodeSettingsWO settings) {
        castAndSaveParameters(clazz, parameters, settings);
    }

    @SuppressWarnings("unchecked") // we check that the cast is save
    private static <S extends NodeParameters> void castAndSaveParameters(final Class<S> clazz,
        final NodeParameters parameters, final NodeSettingsWO settings) {
        CheckUtils.checkArgument(clazz.isInstance(parameters),
            "The provided parameters object is not an instance of the provided parameters class.");
        SettingsSaverFactory.saveSettings((S)parameters, settings);
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
            return new NodeParametersInput(fallbackPortTypesFor(specs), specs, null, null, null, null);
        }
        final var nc = nodeContext.getNodeContainer();
        final CredentialsProvider credentialsProvider;
        final PortType[] inPortTypes;
        DialogNode dialogNode = null;
        if (nc instanceof NativeNodeContainer nnc) {
            credentialsProvider = nnc.getNode().getCredentialsProvider();
            // skip hidden flow variable input (mickey mouse ear) - not exposed to node implementation
            inPortTypes = IntStream.range(1, nnc.getNrInPorts()).mapToObj(nnc::getInPort).map(NodeInPort::getPortType)
                .toArray(PortType[]::new);
            if (nnc.getNode().getNodeModel() instanceof DialogNode model) {
                dialogNode = model;
            }
        } else {
            credentialsProvider = null;
            inPortTypes = fallbackPortTypesFor(specs);
        }

        final var inPortObjects = nc.getParent() == null // This function is used by tests that mock the container
            ? new PortObject[0] // When mocked the container is not a child of a workflow manager
            : InputPortUtil.getInputPortObjectsExcludingVariablePort(nc);

        return new NodeParametersInput(inPortTypes, specs, nc.getFlowObjectStack(), credentialsProvider,
            inPortObjects, dialogNode);
    }

    private static PortType[] fallbackPortTypesFor(final PortObjectSpec[] specs) {
        return IntStream.range(0, specs.length).mapToObj(i -> PortObject.TYPE).toArray(PortType[]::new);
    }
}
