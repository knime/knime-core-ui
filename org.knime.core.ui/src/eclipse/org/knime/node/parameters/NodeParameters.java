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
import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.node.DefaultNode;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.examples.ArrayWidgetExample;
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
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.choices.filter.StringFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.credentials.Credentials;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.RichTextInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Marker interface for parameters of the model or a view of a {@link DefaultNode}. The implementations allow one to
 * declare the dialog's settings and widgets in a compact manner.
 *
 * <h3>Constructors and fields</h3>
 * <p>
 * The implementations must follow the following conventions:
 * <ol>
 * <li>It must provide an empty constructor and optionally a constructor that receives a
 * {@link NodeParametersInput}.</li>
 * <li>Fields must be of any of the supported types given in the below table overview.</li>
 * <li>Fields should be initialized with proper default values.
 * <ul>
 * <li>If no default value is provided, then the Java default is used
 * <li>It is recommended to not use null values, as they are also not supported with few exceptions. Instead, it is
 * possible to used {@link Optional} fields.</li>
 * </ul>
 * </ol>
 * <h3>Dialog widgets</h3>
 * <p>
 * All fields with visibility of at least 'package scope' and annotated with {@link Widget} are represented as dialog
 * widgets. They can additionally be annotated with {@link org.knime.node.parameters.widget other widget annotations} to
 * supply additional information (e.g. description, domain info, ...).
 *
 * Fields without a widget annotation are still persisted and passed to the frontend as 'data' (but will not be visible
 * to the user).
 *
 * Refer to {@link Effect} on how to hide a widget depending on other settings. With {@link Advanced}, widgets are
 * hidden per default, but can be shown when enabling advanced settings in the dialog.
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
 * {@link RichTextInputWidget}</td>
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
}
