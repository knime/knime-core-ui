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
 *   Dec 12, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * Enhances the JSON schema for K-Ai by adding missing information that is not needed in the dialogs.
 * The magic strings used for None, RowIndex and RowID in column selections are one example.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class KaiSchemaEnhancer {

    private KaiSchemaEnhancer() {
        // static utility
    }

    static void enhanceForKai(final TreeNode<WidgetGroup> treeNode) {
        // other modifications are already taken care of by WidgetTreeFactory#createTree
        if (treeNode instanceof Tree<WidgetGroup> tree) {
            tree.getChildren().stream().forEach(KaiSchemaEnhancer::enhanceForKai);
        } else if (treeNode instanceof LeafNode<WidgetGroup> leaf) {
            enhanceForKai(leaf);
        } else if (treeNode instanceof ArrayParentNode<WidgetGroup> arrayParent) {
            enhanceForKai(arrayParent.getElementTree());
        }
    }

    private static void enhanceForKai(final LeafNode<WidgetGroup> leaf) {
        var widget = leaf.getAnnotation(Widget.class);

        if (widget.isPresent()) {
            var descriptionSuffix = getColumnSelectionDescriptionSuffix(leaf);
            var description = widget.get().description() + descriptionSuffix.orElse("");
            leaf.addOrReplaceAnnotation(Widget.class, createProxy(Widget.class, description, widget.get()));
        }

        // TODO add descriptions for column filter subsettings which currently is done in the JsonFormsNodeFunc
    }


    private static Optional<String> getColumnSelectionDescriptionSuffix(final TreeNode<WidgetGroup> node) {
        return node.getAnnotation(ChoicesWidget.class).map(KaiSchemaEnhancer::getSpecialColumnsSuffix);
    }

    private static String getSpecialColumnsSuffix(final ChoicesWidget choices) {
        var suffix = "";
        if (choices.showNoneColumn()) {
            suffix += specialColumnSuffix(SpecialColumns.NONE, "no column");
        }
        if (choices.showRowKeysColumn()) {
            suffix += specialColumnSuffix(SpecialColumns.ROWID, "the special \"RowID\" column");
        }
        if (choices.showRowNumbersColumn()) {
            suffix += specialColumnSuffix(SpecialColumns.ROW_NUMBERS, "the special \"Row number\"");
        }
        return suffix;
    }

    private static String specialColumnSuffix(final SpecialColumns specialColumn, final String columnDescription) {
        // NOSONAR The string is presented to a model so we want consistent behavior independent of the client platform
        return "\nUse '%s' if %s should be selected".formatted(specialColumn.getId(), columnDescription);
    }



    private static <T extends Annotation> T createProxy(final Class<T> annotation, final String extendedDescription,
        final T delegate) throws IllegalArgumentException {

        InvocationHandler handler = (proxy, method, args) -> "description".equals(method.getName())
            ? extendedDescription : method.invoke(delegate, args);

        return (T)newProxyInstance(DefaultNodeSettingsService.class.getClassLoader(), new Class<?>[]{annotation},
            handler);
    }
}
