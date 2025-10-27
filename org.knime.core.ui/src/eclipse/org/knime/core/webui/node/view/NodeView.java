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
 *   Aug 23, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.webui.UIExtension;
import org.knime.core.webui.data.DataServiceProvider;

/**
 * Represents a view of a node.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 *
 * @since 4.5
 */
public interface NodeView extends UIExtension, DataServiceProvider {

    /**
     * Validates the given settings before loading it via {@link #loadValidatedSettingsFrom(NodeSettingsRO)}. This is
     * called as soon as new settings are applied via the node's dialog.
     *
     * Please also see {@link NodeModel#validateViewSettings} in order to validate view settings at configuration time.
     *
     * @param settings settings to validate
     * @throws InvalidSettingsException if the validation failed
     */
    void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads validated settings.
     *
     * @param settings settings to load
     */
    void loadValidatedSettingsFrom(NodeSettingsRO settings);

    /**
     * Returns the node message to be displayed in the view. By default, it's the same messages as shown for the node
     * itself.
     *
     * @param nodeMessage the node messages of certain type currently shown on the node
     * @return the node message to be displayed in the view
     */
    default NodeMessage getViewNodeMessage(final NodeMessage nodeMessage) {
        return nodeMessage;
    }

    /**
     * The default page format is being used to determine the size of the page if it's being displayed together with
     * other pages (aka composite view).
     *
     * @return the page format
     */
    default PageFormat getDefaultPageFormat() {
        return PageFormat.ASPECT_RATIO_4BY3;
    }

    /**
     * Whether this node view can be used within a report (if the node view is created to be rendered into a report). If
     * not, a 'not supported' placeholder will be added instead.
     *
     * If a node view can be used in a report, its frontend implementation must actively inform the framework that it's
     * either done rendering or directly supply the content (image, html, ...) to it. This is done via the
     * ReportingService in the knime-ui-extension-service package.
     *
     * @return {@code true} if this node view can be used in a report (and exhibits the necessary capabilities);
     *         otherwise {@code false}
     */
    default boolean canBeUsedInReport() {
        return false;
    }

}
