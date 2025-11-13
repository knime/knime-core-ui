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
 *   Nov 13, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.tree.syntax;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.widgettree.WidgetTreeFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;

/**
 * Internal utility class for validating the syntax of node parameters by constructing the internal tree representation
 * from a given node parameters class. Used for testing, i.e. the validations are more strict than in production code.
 *
 * @author Paul Bärnreuther
 */
public class NodeParametersSyntaxValidationUtil {

    private NodeParametersSyntaxValidationUtil() {
        // prevent instantiation
    }

    /**
     * Exception indicating a syntax error in node parameters.
     */
    public static final class NodeParametersSyntaxError extends Exception {
        private static final long serialVersionUID = 1L;

        NodeParametersSyntaxError(final String message) {
            super(message);
        }
    }

    static final WidgetTreeFactory widgetTreeFactory = new WidgetTreeFactory();

    /**
     * Validates the syntax of the given node parameters class for the given settings type.
     *
     * @param settingsType the settings type
     * @param nodeParametersClass the node parameters class
     * @throws NodeParametersSyntaxError if a syntax error is found
     */
    public static void validateNodeParametersSyntax(final SettingsType settingsType,
        final Class<? extends NodeParameters> nodeParametersClass) throws NodeParametersSyntaxError {
        final var widgetTree = widgetTreeFactory.createTree(nodeParametersClass, settingsType);
        validateNoWidgetAnnotationsWithEmptyTitle(widgetTree);
    }

    private static void validateNoWidgetAnnotationsWithEmptyTitle(final Tree<WidgetGroup> widgetTree)
        throws NodeParametersSyntaxError {
        for (var node : widgetTree.getWidgetNodes().toList()) {
            final var widgetAnnotation = node.getAnnotation(Widget.class);
            if (widgetAnnotation.isPresent()) {
                final var title = widgetAnnotation.get().title();
                if (title == null || title.isBlank()) {
                    throw new NodeParametersSyntaxError("Widget annotation on " + node.getPath() + " has empty title.");
                }
            }
            if (node instanceof ArrayParentNode<WidgetGroup> arrayParentNode) {
                validateNoWidgetAnnotationsWithEmptyTitle(arrayParentNode.getElementTree());
            }

        }
    }

}
