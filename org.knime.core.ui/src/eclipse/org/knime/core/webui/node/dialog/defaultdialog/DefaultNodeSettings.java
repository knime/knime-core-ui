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
 *   Jan 5, 2022 (hornm): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.data.util.InputPortUtil;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.examples.ArrayWidgetExample;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigMappingsFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.NameFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.fileselection.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComboBoxWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RichTextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.PasswordWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.UsernameWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;

/**
 * Marker interface for implementations that define a {@link DefaultNodeDialog}. The implementations allow one to
 * declare the dialog's settings and widgets in a compact manner. See
 * {@link org.knime.core.webui.node.dialog.defaultdialog package.info} for some known limitations.
 *
 * <h3>Constructors and fields</h3>
 * <p>
 * The implementations must follow the following conventions:
 * <ol>
 * <li>It must provide an empty constructor and optionally a constructor that receives a
 * {@link DefaultNodeSettingsContext}.
 * <li>Fields must be of any of the following supported types:
 * <ul>
 * <li>boolean, int, long, double, float, String, char, or byte
 * <li>POJOs, arrays or Collections holding other supported types
 * </ul>
 * <li>Fields should be initialized with proper default values.
 * <ul>
 * <li>If no default value is provided, then the Java default is used
 * <li>Make sure that the persistors of non-primitive fields support null values if no proper default is provided
 * </ul>
 * </ol>
 * <h3>Dialog widgets</h3>
 * <p>
 * All fields with visibility of at least 'package scope' and annotated with {@link Widget} are represented as dialog
 * widgets. They can additionally be annotated with {@link org.knime.core.webui.node.dialog.defaultdialog.widget other
 * widget annotations} to supply additional information (e.g. description, domain info, ...).
 *
 * Fields without a widget annotation are still persisted and passed to the frontend as 'data' (but will not be visible
 * to the user). This can e.g. be used when a setting is only used in a dialog-less context, e.g. a port view. Refer to
 * {@link Effect} on how to hide a widget depending on other settings.
 *
 * The table below lists all the supported type with
 * <ul>
 * <li>the default widget being displayed if no specific widget annotation is given</li>
 * <li>the widget annotations that are compatible with the type</li>
 * </ul>
 *
 * <table border="1" cellpadding="3" cellspacing="0">
 * <caption>Type to Widget Mapping</caption>
 * <tr>
 * <th>Type</th>
 * <th>Default Widget</th>
 * <th>Compatible widget annotations</th>
 * </tr>
 * <tr>
 * <td>boolean</td>
 * <td>Checkbox</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>byte, int, long, double, float, {@link Duration}</td>
 * <td>Number Input</td>
 * <td>{@link NumberInputWidget}</td>
 * </tr>
 * <tr>
 * <td>String</td>
 * <td>Text Input</td>
 * <td>{@link ChoicesWidget} (twin-list)<br>
 * {@link TextInputWidget}<br>
 * {@link DateTimeWidget}<br>
 * {@link LocalFileReaderWidget}<br>
 * {@link LocalFileWriterWidget}<br>
 * {@link RichTextInputWidget}</td>
 * </tr>
 * <tr>
 * <td>{@link LocalDate}</td>
 * <td>Date Picker</td>
 * <td>{@link DateWidget}</td>
 * </tr>
 * <tr>
 * <td>{@link LocalTime}</td>
 * <td>Time Picker</td>
 * <td></td>
 * <tr>
 * <td>{@link Interval}</td>
 * <td>Date or time interval</td>
 * <td>{@link IntervalWidget} (for switching between date and time)</td>
 * </tr>
 * <tr>
 * <td>{@link TimeInterval}</td>
 * <td>Time interval</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link DateInterval}</td>
 * <td>Date interval</td>
 * <td></td>
 * </tr>
 * </tr>
 * <td>String[]</td>
 * <td></td>
 * <td>{@link ChoicesWidget} (drop-down) <br>
 * {@link ComboBoxWidget} <br>
 * {@link SortListWidget} <br>
 * </td>
 * </tr>
 * <tr>
 * <td>Enums(*)</td>
 * <td>Drop Down</td>
 * <td>{@link ValueSwitchWidget}<br>
 * {@link RadioButtonsWidget}<br>
 * {@link ChoicesWidget} (Drop Down; Can be used to set dynamic values. This will be replaced with a different
 * annotation with UIEXT-1681)</td>
 * </tr>
 * <tr>
 * <td>Arrays/Collections of {@link DefaultNodeSettings} (**)</td>
 * <td>Array Widget ({@link ArrayWidgetExample example})</td>
 * <td>{@link ArrayWidget}</td>
 * </tr>
 * <tr>
 * <td>{@link ColumnSelection}</td>
 * <td></td>
 * <td>{@link ChoicesWidget} (drop-down)</td>
 * </tr>
 * <tr>
 * <td>{@link ColumnFilter}</td>
 * <td></td>
 * <td>{@link ChoicesWidget} (twin-list)</td>
 * </tr>
 * <tr>
 * <td>{@link NameFilter}</td>
 * <td></td>
 * <td>{@link ChoicesWidget} (twin-list)</td>
 * </tr>
 * <tr>
 * <td>{@link Credentials}</td>
 * <td>Credentials Input (username + password)</td>
 * <td>{@link CredentialsWidget} (for customizing username + password)<br>
 * {@link PasswordWidget} (password only)<br>
 * {@link UsernameWidget} (username only)</td>
 * </tr>
 * <tr>
 * <td>{@link FileSelection}</td>
 * <td>Path file chooser (currently with options "Local File System" and "Custom/KNIME URL")</td>
 * <td>{@link FileReaderWidget}<br>
 * {@link FileWriterWidget}</td>
 * </tr>
 * <tr>
 * <td>{@link Void} (i.e. this is not a setting but only something that is displayed)</td>
 * <td></td>
 * <td>{@link SimpleButtonWidget} (button with backend-side action handler)<br>
 * {@link TextMessage}</td>
 * </tr>
 * <td>Any type</td>
 * <td></td>
 * <td>{@link ButtonWidget} (button with backend-side action handler)</td>
 * </tr>
 * </table>
 *
 * <p>
 * (*) Note on enums: In order to control the labels of enum-values to be used within the respective widget (e.g. Value
 * Switch), the {@link Label}-annotation can be used.
 * </p>
 * <p>
 * (**) Note on arrays: Arrays of Strings or boxed types will not lead to an array layout. Instead these need to be
 * wrapped inside a {@link DefaultNodeSettings} element class.
 * </p>
 *
 * <h4>Nested settings</h4> For nested fields to be transformed to dialog widgets themselves the containing class has to
 * be annotated with {@link WidgetGroup}
 *
 * <h4>Layouting</h4> Additional annotations can be used to set the layouting for the generated dialog. See the
 * {@link org.knime.core.webui.node.dialog.defaultdialog.layout layout package} for further information. The class of
 * the elements of an arrays are treated as self-contained settings with their own layouting.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface DefaultNodeSettings extends PersistableSettings, WidgetGroup {

    /**
     * A context that holds any available information that might be relevant for creating a new instance of
     * {@link DefaultNodeSettings}.
     */
    final class DefaultNodeSettingsContext {

        private final PortType[] m_inTypes;

        private final PortObjectSpec[] m_specs;

        private final FlowObjectStack m_stack;

        private final CredentialsProvider m_credentialsProvider;

        private final PortObject[] m_inputPortObjects;

        DefaultNodeSettingsContext(final PortType[] inTypes, final PortObjectSpec[] specs, final FlowObjectStack stack,
            final CredentialsProvider credentialsProvider, final PortObject[] inputPortObjects) {
            m_inTypes = inTypes;
            m_specs = specs;
            m_stack = stack;
            m_credentialsProvider = credentialsProvider;
            m_inputPortObjects = inputPortObjects;
        }

        DefaultNodeSettingsContext(final PortType[] inTypes, final PortObjectSpec[] specs, final FlowObjectStack stack,
            final CredentialsProvider credentialsProvider) {
            this(inTypes, specs, stack, credentialsProvider, null);
        }

        /**
         * Widens scope of constructor of {@link DefaultNodeSettingsContext}. Only used in tests.
         */
        @SuppressWarnings("javadoc")
        public static DefaultNodeSettingsContext createDefaultNodeSettingsContext(final PortType[] inPortTypes,
            final PortObjectSpec[] specs, final FlowObjectStack stack, final CredentialsProvider credentialsProvider) {
            return new DefaultNodeSettingsContext(inPortTypes, specs, stack, credentialsProvider, null);
        }

        /**
         * The node's input types. Not null and not containing null.
         *
         * @return the inTypes
         */
        public PortType[] getInPortTypes() {
            return m_inTypes;
        }

        /**
         * @return the input {@link PortObjectSpec PortObjectSpecs} of the node; NOTE: array of specs can contain
         *         {@code null} values, e.g., if input port is not connected!
         */
        public PortObjectSpec[] getPortObjectSpecs() {
            return m_specs;
        }

        /**
         * @param portIndex the port for which to retrieve the spec
         * @return the {@link PortObjectSpec} at the given portIndex or {@link Optional#empty()} if it is not available
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        public Optional<PortObjectSpec> getPortObjectSpec(final int portIndex) {
            return Optional.ofNullable(m_specs[portIndex]);
        }

        /**
         * @return the input {@link DataTableSpec DataTableSpecs} of the node; NOTE: array of specs can contain
         *         {@code null} values, e.g., if input port is not connected or inactive!
         * @throws ClassCastException if any of the node's input ports does not hold a {@link DataTableSpec}
         */
        public DataTableSpec[] getDataTableSpecs() {
            return Arrays.stream(m_specs).map(spec -> spec instanceof DataTableSpec dts ? dts : null)
                .toArray(DataTableSpec[]::new);
        }

        /**
         * @param portIndex the port for which to retrieve the spec
         * @return the {@link DataTableSpec} at the given portIndex or {@link Optional#empty()} if it is not available
         * @throws ClassCastException if the requested port is not a table port
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        public Optional<DataTableSpec> getDataTableSpec(final int portIndex) {
            return getPortObjectSpec(portIndex).map(DataTableSpec.class::cast);
        }

        /**
         * @return the input {@link PortObject}s of the node
         */
        public PortObject[] getInputPortObjects() {
            return m_inputPortObjects;
        }

        /**
         * @param portIndex
         * @return the {@link PortObject} at the given portIndex or {@link Optional#empty()} if it is not available
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        public Optional<PortObject> getInputPortObject(final int portIndex) {
            return Optional.ofNullable(m_inputPortObjects[portIndex]);
        }

        /**
         * @param portIndex the port for which to retrieve the object
         * @return the {@link DataTable} at the given portIndex or {@link Optional#empty()} if it is not available
         * @throws ClassCastException if the requested port is not a table port
         * @throws IndexOutOfBoundsException if the portIndex does not match the ports of the node
         */
        public Optional<DataTable> getDataTable(final int portIndex) {
            return getInputPortObject(portIndex).map(DataTable.class::cast);
        }

        /**
         * @param name the name of the variable
         * @param type the {@link VariableType} of the variable
         * @param <T> the simple value type of the variable
         * @return the simple non-null value of the top-most variable with the argument name and type, if present,
         *         otherwise an empty {@link Optional}
         * @throws NullPointerException if any argument is null
         * @see FlowObjectStack#peekFlowVariable(String, VariableType)
         */
        public <T> Optional<T> peekFlowVariable(final String name, final VariableType<T> type) {
            return m_stack.peekFlowVariable(name, type).map(flowVariable -> flowVariable.getValue(type));
        }

        /**
         * @param types the {@link VariableType VariableTypes} of the requested {@link FlowVariable FlowVariables}
         * @return the non-null read-only map of flow variable name -&gt; {@link FlowVariable}
         * @throws NullPointerException if the argument is null
         * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
         */
        public Map<String, FlowVariable> getAvailableInputFlowVariables(final VariableType<?>... types) {
            Objects.requireNonNull(types, () -> "Variable types must not be null.");
            return Collections.unmodifiableMap(Optional.ofNullable(m_stack)
                .map(stack -> stack.getAvailableFlowVariables(types)).orElse(Collections.emptyMap()));
        }

        /**
         * @return the names of the available flow variables or an empty array if there are no flow variables available
         */
        public String[] getAvailableFlowVariableNames() {
            return m_stack != null ? m_stack.getAllAvailableFlowVariables().keySet().toArray(String[]::new)
                : new String[0];
        }

        /**
         * @param name the name of a flow variable
         * @return the associated flow variable if it exists
         */
        public Optional<FlowVariable> getFlowVariableByName(final String name) {
            if (m_stack == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(m_stack.getAllAvailableFlowVariables().get(name));
        }

        /**
         * @return the {@link CredentialsProvider} associated with the node. Can be empty, e.g., if the node is a
         *         component
         */
        public Optional<CredentialsProvider> getCredentialsProvider() {
            return Optional.ofNullable(m_credentialsProvider);
        }

    }

    /**
     * Verifies a given node settings implementation, making sure that it follows the contract of
     * {@link DefaultNodeSettings}, as defined in its documentation.
     *
     * @param settingsClass the settings class to verify
     */
    static void verifySettings(final Class<? extends DefaultNodeSettings> settingsClass) {
        try {
            settingsClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            NodeLogger.getLogger(DefaultNodeSettings.class).errorWithFormat(
                "Default node settings class %s does not provide a default constructor.",
                settingsClass.getSimpleName());
        } catch (SecurityException e) {
            NodeLogger.getLogger(DefaultNodeSettings.class)
                .error(String.format(
                    "Exception when attempting to access default constructor of default node settings class %s.",
                    settingsClass.getSimpleName()), e);
        }
    }

    /**
     * Helper to serialize a {@link DefaultNodeSettings} of specified class from a {@link NodeSettingsRO}-object.
     *
     * @param <S>
     * @param settings the settings-object to create the instance from
     * @param clazz default node settings class
     * @return a new {@link DefaultNodeSettings}-instance
     * @throws InvalidSettingsException if the settings are invalid
     */
    static <S extends DefaultNodeSettings> S loadSettings(final NodeSettingsRO settings, final Class<S> clazz)
        throws InvalidSettingsException {
        return SettingsLoaderFactory.loadSettings(clazz, settings);
    }

    /**
     * Helper to create a new {@link DefaultNodeSettings} of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz default node settings class
     * @param specs the specs with which to create the settings. NOTE: can contain {@code null} values, e.g., if input
     *            port is not connected
     * @return a new {@link DefaultNodeSettings}-instance
     */
    static <S extends DefaultNodeSettings> S createSettings(final Class<S> clazz, final PortObjectSpec[] specs) {
        return InstantiationUtil.createDefaultNodeSettings(clazz, createDefaultNodeSettingsContext(specs));
    }

    /**
     * Helper to create a new {@link DefaultNodeSettings} of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz default node settings class
     * @param context the {@link DefaultNodeSettingsContext} to be used as constructor argument
     * @return a new {@link DefaultNodeSettings}-instance
     */
    static <S extends DefaultNodeSettings> S createSettings(final Class<S> clazz,
        final DefaultNodeSettingsContext context) {
        return InstantiationUtil.createDefaultNodeSettings(clazz, context);
    }

    /**
     * Creates a new {@link DefaultNodeSettings} object of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz the class of the DefaultNodeSettings type
     * @return a new instance of the DefaultNodeSettingsType
     */
    static <S extends DefaultNodeSettings> S createSettings(final Class<S> clazz) {
        return InstantiationUtil.createInstance(clazz);
    }

    @SuppressWarnings("javadoc")
    static void saveSettings(final Class<? extends DefaultNodeSettings> settingsClass,
        final DefaultNodeSettings settingsObject, final NodeSettingsWO settings) {
        castAndSaveSettings(settingsClass, settingsObject, settings);
    }

    @SuppressWarnings("unchecked") // we check that the cast is save
    private static <S extends DefaultNodeSettings> void castAndSaveSettings(final Class<S> settingsClass,
        final DefaultNodeSettings settingsObject, final NodeSettingsWO settings) {
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
    static <S extends DefaultNodeSettings> ConfigMappings
        getConfigMappings(final Class<S> settingsClass,
        final DefaultNodeSettings settingsObject) {
        return ConfigMappingsFactory.getConfigMappings(settingsClass, settingsObject);
    }

    /**
     * Method to create a new {@link DefaultNodeSettingsContext} from input {@link PortObjectSpec PortObjectSpecs}.
     *
     * @param specs the non-null specs with which to create the schema
     * @return the newly created context
     * @throws NullPointerException if the argument is null
     */
    static DefaultNodeSettingsContext createDefaultNodeSettingsContext(final PortObjectSpec[] specs) {
        Objects.requireNonNull(specs, () -> "Port object specs must not be null.");
        final var nodeContext = NodeContext.getContext();
        if (nodeContext == null) {
            // can only happen during tests
            return new DefaultNodeSettingsContext(fallbackPortTypesFor(specs), specs, null, null, null);
        }
        final var nc = nodeContext.getNodeContainer();
        final CredentialsProvider credentialsProvider;
        final PortType[] inPortTypes;
        if (nc instanceof NativeNodeContainer nnc) {
            credentialsProvider = nnc.getNode().getCredentialsProvider();
            // skip hidden flow variable input (mickey mouse ear) - not exposed to node implementation
            inPortTypes = IntStream.range(1, nnc.getNrInPorts()).mapToObj(nnc::getInPort).map(NodeInPort::getPortType)
                .toArray(PortType[]::new);
        } else {
            credentialsProvider = null;
            inPortTypes = fallbackPortTypesFor(specs);
        }

        final var inPortObjects = nc.getParent() == null // This function is used by tests that mock the container
            ? new PortObject[0] // When mocked the container is not a child of a workflow manager
            : InputPortUtil.getInputPortObjectsExcludingVariablePort(nc);

        return new DefaultNodeSettingsContext(inPortTypes, specs, nc.getFlowObjectStack(), credentialsProvider,
            inPortObjects);
    }

    private static PortType[] fallbackPortTypesFor(final PortObjectSpec[] specs) {
        return IntStream.range(0, specs.length).mapToObj(i -> PortObject.TYPE).toArray(PortType[]::new);
    }

}
