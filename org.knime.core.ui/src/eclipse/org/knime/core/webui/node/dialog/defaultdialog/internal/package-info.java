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
 *   Jul 30, 2024 (Paul Bärnreuther): created
 */
/**
 * This package contains features of default node dialogs that are still subject to change / preliminary versions. They
 * are to be used only internally only and not intended for public use.
 *
 * As such, the APIs provided in this package may change without notice and do <i>not</i> follow the stability
 * guarantees of the public API. They may be modified or removed in future releases.
 *
 * <p>
 * The table below lists the supported types of the internal API (which are <i>not</i> part of the public API),
 * including:
 * </p>
 * <ul>
 * <li>The default widget displayed if no specific annotation is provided</li>
 * <li>The internal widget annotations compatible with the given type</li>
 * </ul>
 *
 * <table border="1" cellpadding="3" cellspacing="0">
 * <caption>Type to Widget Mapping</caption>
 * <tr>
 * <th>Type</th>
 * <th>Default Widget and Choices Configuration</th>
 * <th>Compatible Widget Annotations</th>
 * <th>Supports {@link Optional}?</th>
 * </tr>
 * <tr>
 * <td>Any type</td>
 * <td></td>
 * <td>{@link ButtonWidget} (button with backend-side action handler)</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Map&ltString,Object&gt;</td>
 * <td></td>
 * <td>{@link DynamicSettingsWidget} for creating an imperative dialog for this map.</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link java.lang.Void Void} (i.e. this is not a setting but only something that is displayed)</td>
 * <td></td>
 * <td>{@link SimpleButtonWidget} (button with backend-side handler using the state provider mechanism).</td>
 * <td></td>
 * </tr>
 * <td>String</td>
 * <td>Text Input</td>
 * <td>{@link DateTimeFormatPickerWidget}<br>
 * To make it a file selection of one specific file system, use:
 * <ul>
 * <li>{@link FileSelectionWidget} or {@link FileReaderWidget} or {@link FileWriterWidget}</li>
 * <li>{@link WithFileSystem} or {@link WithCustomFileSystem}</li>
 * </ul>
 * </td>
 * <td>✓</td>
 * </tr>
 * <tr>
 * <td>{@link FileSelection}</td>
 * <td>Path file chooser (with limited file system options)</td>
 * <td>{@link FileSelectionWidget} (enables changing the selection mode to folder selection)<br>
 * {@link FileReaderWidget}<br>
 * {@link FileWriterWidget}<br>
 * {@link WithFileSystem}<br>
 * </td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@link MultiFileSelection}</td>
 * <td>File chooser that allows switching between different filter modes including filter options. It is required to
 * define the used filter modes using a {@link MultiFileSelectionWidget} annotation.</td></td>
 * <td>{@link FileReaderWidget}<br>
 * {@link WithFileSystem}</td>
 * <td></td>
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
 * *
 * <tr>
 * <td>{@link StringOrEnum}</td>
 * <td>Use a {@link ChoicesProvider} for setting the dynamic choices.</td>
 * <td><br>
 * <td></td>
 * </tr>
 * </table>
 * <br>
 *
 * @author Paul Bärnreuther
 */

package org.knime.core.webui.node.dialog.defaultdialog.internal;
