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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.ControlRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;

/**
 *
 * @author Paul Bärnreuther
 */
public class WidgetTreeRenderers {

    record WidgetTreeNodeTester(//
        Function<TreeNode<WidgetGroup>, ControlRendererSpec> creator, //
        Predicate<TreeNode<WidgetGroup>> tester //
    ) {
    }

    static final WidgetTreeNodeTester[] TESTERS = new WidgetTreeNodeTester[]{//
        new WidgetTreeNodeTester(TextRenderer::new, //
            node -> String.class.equals(node.getRawClass())), //
        new WidgetTreeNodeTester(IntegerRenderer::new,
            node -> List.of(Byte.class, Integer.class).contains(ClassUtils.primitiveToWrapper(node.getRawClass()))), // bytes and integers
        new WidgetTreeNodeTester(NumberRenderer::new,
            node -> Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(node.getRawClass()))), // all other numbers
        new WidgetTreeNodeTester(CheckboxRenderer::new,
            node -> Boolean.class.equals(ClassUtils.primitiveToWrapper(node.getRawClass()))), //
        new WidgetTreeNodeTester(DateRenderer::new, //
            node -> LocalDate.class.equals(node.getRawClass())) //
    };

    /**
     * @param node the node to check
     * @return the renderer spec for the given node or {@code null} if not supported
     */
    public static ControlRendererSpec getRendererSpec(final TreeNode<WidgetGroup> node) {
        return Stream.of(TESTERS).filter(tester -> tester.tester().test(node))//
            .findFirst()//
            .map(tester -> tester.creator().apply(node))//
            .orElse(null);
    }

}
