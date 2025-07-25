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
 *   May 2, 2023 (Paul Bärnreuther): created
 */

/**
 * This package offers a default implementation of a Node Dialog as described in
 * {@link org.knime.core.webui.node.dialog}. It utilizes <a href="https://jsonforms.io/">JsonForms</a> to represent the
 * dialog and its components. The text representation of the settings is a JSON with three parts:
 * <ul>
 * <li><b>Data</b> holding the state of the NodeSettings.</li>
 * <li>A <b>Schema</b> specifying the data.</li>
 * <li>A <b>UISchema</b> specifying the UI.</li>
 * </ul>
 *
 * See {@link org.knime.node.parameters.NodeParameters NodeParameters} on how to define
 * the dialog via a declarative API.
 *
 * <h4>Known limitations</h4>
 * <ol>
 * <li>Updates between settings are not possible if the target setting is nested within an array layout setting and as a
 * source it is only possible to trigger an update by every change of an array layout, not individual changes of
 * specific nested settings.</li>
 * <li>Dependency chains are not possible, i.e. if setting A gets updated by setting B and setting B gets updated by
 * setting C, an update of setting C does not influence A. In order to circumvent this, it is currently necessary to
 * have a direct dependency from A to C, too.</li>
 * <li>The order in which updates due to dependencies between settings are resolved is non-deterministic.</li>
 * <li>It is not possible at the moment to have the same combination of settings name and containing class for two
 * separate {@link Credentials}. I.e. for a single dialog it is not possible to reuse a common class containing
 * credentials twice. The same holds for multiple dialogs and in particular it will not work to open a dialog which
 * contains credentials twice at the same time currently. (TODO: UIEXT-1375 resolve this limitation)</li>
 * </ol>
 *
 * @see org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettingsImpl
 *
 * @author Paul Bärnreuther
 */
package org.knime.core.webui.node.dialog.defaultdialog;
