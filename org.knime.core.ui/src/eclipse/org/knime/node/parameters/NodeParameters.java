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
package org.knime.node.parameters;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeInPort;
import org.knime.core.webui.data.util.InputPortUtil;
import org.knime.core.webui.node.dialog.configmapping.ConfigMappings;
import org.knime.core.webui.node.dialog.configmapping.NodeSettingsCorrectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.examples.ArrayWidgetExample;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.ConfigMappingsFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.StringFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable.FlowVariableFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.RichTextInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

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
 * {@link NodeParametersInput}.
 * <li>Fields must be of any of the supported types given in the below table overview.</li>
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
 * {@link Effect} on how to hide a widget depending on other settings. With {@link Advanced}, widgets are hidden per
 * default, but can be shown when enabling advanced settings in the dialog.
 *
 * Getters and setters are ignored by the framework and to avoid confusion, they should simply not be used.
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
 * <th>Default Widget and Choices Configuration</th>
 * <th>Compatible widget annotations</th>
 * <th>Can be wrapped in {@link Optional}? (***)</th>
 * </tr>
 * <tr>
 * <td>boolean</td>
 * <td>Checkbox</td>
 * <td></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>byte, int, long, double, float</td>
 * <td>Number Input</td>
 * <td>{@link NumberInputWidget}</td>
 * <td>✓ (using the boxed types, e.g. {@link Integer})</td>
 * </tr>
 * <tr>
 * <td rowspan="2">String</td>
 * <td>Text Input</td>
 * <td>{@link TextAreaWidget}<br>
 * {@link TextInputWidget}<br>
 * {@link RichTextInputWidget}<br>
 * {@link DateTimeFormatPickerWidget}</td>
 * <td rowspan="2">✓</td>
 * </tr>
 * <tr>
 * <td>Text with {@link ChoicesProvider} → Drop Down</td>
 * <td>Other choices widgets are only available for enums.</td>
 * </tr>
 * <td>{@link LocalDate}</td>
 * <td>Date Picker</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link LocalTime}</td>
 * <td>Time Picker</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link LocalDateTime}</td>
 * <td>Date and Time Picker</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link ZonedDateTime}</td>
 * <td>Date and Time Picker</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link ZoneId}</td>
 * <td>Time Zone Picker. Optionally, one can add {@link ChoicesProvider} for defining a list of possible time zoned to
 * choose from, manually.</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link Interval}</td>
 * <td>Date or time interval</td>
 * <td>{@link IntervalWidget} (for switching between date and time)</td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link TimeInterval}</td>
 * <td>Time interval</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link DateInterval}</td>
 * <td>Date interval</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link TemporalFormat}</td>
 * <td></td>
 * <td>{@link DateTimeFormatPickerWidget}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>String[]</td>
 * <td>Combo Box. Add a {@link ChoicesProvider} with a {@link StringChoicesProvider}.</td>
 * <td>{@link TwinlistWidget}</td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>Enums(*)</td>
 * <td>Drop Down. Optionally, add a {@link ChoicesProvider} with an {@link EnumChoicesProvider}.</td>
 * <td>{@link ValueSwitchWidget}<br>
 * {@link RadioButtonsWidget}</td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link StringOrEnum}</td>
 * <td>Use a {@link ChoicesProvider} for setting the dynamic choices.</td>
 * <td><br>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link Array}/{@link Collection}/{@link List} of {@link NodeParameters} (**)</td>
 * <td>Array Widget ({@link ArrayWidgetExample example})</td>
 * <td>{@link ArrayWidget}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link StringFilter}</td>
 * <td>Add a {@link ChoicesProvider} with a {@link StringChoicesProvider}.</td>
 * <td><br>
 * {@link TwinlistWidget}
 * <td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link ColumnFilter}</td>
 * <td></td>
 * <td>{@link ColumnFilterWidget}<br>
 * {@link TwinlistWidget}
 * <td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link FlowVariableFilter}</td>
 * <td></td>
 * <td>{@link FlowVariableFilterWidget}<br>
 * {@link TwinlistWidget}
 * <td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link DataType}</td>
 * <td>A drop down of all available data types. Add a {@link ChoicesProvider} using a
 * {@link DataTypeChoicesProvider}.</td>
 * <td></td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link Credentials}</td>
 * <td>Credentials Input (username + password)</td>
 * <td>{@link CredentialsWidget} (for customizing username + password)<br>
 * {@link PasswordWidget} (password only)<br>
 * {@link UsernameWidget} (username only)</td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link Void} (i.e. this is not a setting but only something that is displayed)</td>
 * <td></td>
 * <td>{@link TextMessage}</td>
 * <td></td>
 * </tr>
 *
 * </table>
 *
 * <p>
 * (*) Note on enums: In order to control the labels of enum-values to be used within the respective widget (e.g. Value
 * Switch), the {@link Label}-annotation can be used.
 * </p>
 * <p>
 * (**) Note on arrays: Arrays of Strings or boxed types will not lead to an array layout. Instead these need to be
 * wrapped inside a {@link NodeParameters} element class.
 * </p>
 * <p>
 * (***) Optional widgets: For some types it is possible to wrap the field type in an {@link Optional} to enable the
 * user to (de-)activate the setting in the dialog. To supply a default value on activation, the {@link OptionalWidget}
 * annotation can be used.
 * </p>
 *
 * <h4>Nested settings</h4> For nested fields to be transformed to dialog widgets themselves the containing class has to
 * be annotated with {@link WidgetGroup}
 *
 *
 *
 * <h4>Layouting</h4> Additional annotations can be used to set the layouting for the generated dialog. See the
 * {@link org.knime.core.webui.node.dialog.defaultdialog.layout layout package} for further information. The class of
 * the elements of an arrays are treated as self-contained settings with their own layouting.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("rawtypes")
public interface NodeParameters extends Persistable, WidgetGroup {

    /**
     * Use this method to validate a loaded instance of NodeParameters. Settings should be invalid if it should be
     * impossible to apply them.
     *
     * @throws InvalidSettingsException if a setting is invalid
     */
    default void validate() throws InvalidSettingsException {
        // No validation per default
    }

    /**
     * Verifies a given node settings implementation, making sure that it follows the contract of
     * {@link NodeParameters}, as defined in its documentation.
     *
     * @param settingsClass the settings class to verify
     */
    static void verifySettings(final Class<? extends NodeParameters> settingsClass) {
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
    static <S extends NodeParameters> S loadSettings(final NodeSettingsRO settings, final Class<S> clazz)
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
    static <S extends NodeParameters> S createSettings(final Class<S> clazz, final PortObjectSpec[] specs) {
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
    static <S extends NodeParameters> S createSettings(final Class<S> clazz,
        final NodeParametersInput context) {
        return InstantiationUtil.createDefaultNodeSettings(clazz, context);
    }

    /**
     * Creates a new {@link NodeParameters} object of the specified type.
     *
     * @param <S> the type of DefaultNodeSettings
     * @param clazz the class of the NodeParameters type
     * @return a new instance of the DefaultNodeSettingsType
     */
    static <S extends NodeParameters> S createSettings(final Class<S> clazz) {
        return InstantiationUtil.createInstance(clazz);
    }

    @SuppressWarnings("javadoc")
    static void saveSettings(final Class<? extends NodeParameters> settingsClass,
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
    static <S extends NodeParameters> ConfigMappings getConfigMappings(final Class<S> settingsClass,
        final NodeParameters settingsObject) {
        return ConfigMappingsFactory.createConfigMappings(settingsClass, settingsObject);
    }

    /**
     * Method to create a new {@link NodeParametersInput} from input {@link PortObjectSpec PortObjectSpecs}.
     *
     * @param specs the non-null specs with which to create the schema
     * @return the newly created context
     * @throws NullPointerException if the argument is null
     */
    static NodeParametersInput createDefaultNodeSettingsContext(final PortObjectSpec[] specs) {
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
