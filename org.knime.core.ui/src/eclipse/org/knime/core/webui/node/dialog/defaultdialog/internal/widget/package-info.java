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
 * This package contains widget annotations that are **internal** and not intended for public use. These annotations
 * exist either for KNIME-internal purposes or are not polished enough for public exposure. As such, the APIs provided
 * in this package may change without notice and do **not** follow the stability guarantees of the public API. They may
 * be modified or removed in future releases.
 *
 * <h3>Internal Counterparts</h3>
 *
 * Some public widgets have internal counterparts that provide additional configuration options.
 *
 * <p>
 * <strong>Naming Convention:</strong> Internal widgets should follow the naming pattern: <code>*Internal</code>.
 * </p>
 *
 * <p>
 * The following table maps public widgets to their internal counterparts, which offer additional configuration options:
 * </p>
 *
 * <table border="1" cellpadding="3" cellspacing="0">
 * <caption>Public to Internal Widget Mapping</caption>
 * <tr>
 * <th>Public Widget</th>
 * <th>Internal Widget</th>
 * <th>When to Use</th>
 * </tr>
 * <tr>
 * <td>{@link ArrayWidget}</td>
 * <td>{@link org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal
 * ArrayWidgetInternal}</td>
 * <td>Dynamic titles and edit/reset buttons in array elements</td>
 * </tr>
 * <tr>
 * <td>{@link Widget}</td>
 * <td>{@link org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal
 * WidgetInternal}</td>
 * <td>Hide the title, flow variable, and description button of an element</td>
 * </tr>
 * </table>
 * <br>
 *
 * <h3>Additional internal widgets that are not tied to a specific type or public widget:</h3>
 * <ul>
 * <li>{@link org.knime.core.webui.node.dialog.defaultdialog.internal.widget.OverwriteDialogTitleInternal
 * OverwriteDialogTitleInternal}: For showing a different title in the dialog as in the node description.</li>
 * <li>{@link org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget SortListWidget}: For a
 * widget that allows sorting a list of columns in the node dialog.</li>
 * </ul>
 *
 * @author Robin Gerling
 */

package org.knime.core.webui.node.dialog.defaultdialog.internal.widget;
