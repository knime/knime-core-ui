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
 *   Mar 27, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static java.util.function.Predicate.not;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.StatesWithLayout.fromIntermediateStates;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.LeafState;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.LeafState.LayoutTreeNodeState;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.StatesWithLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.AfterAllOf;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.BeforeAllOf;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Inside;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * This utility class is used to resolve all layout annotations of a collection of widget trees
 *
 * @author Paul Bärnreuther
 */
public class WidgetTreeToLayoutTree {

    private WidgetTreeToLayoutTree() {
        // utility class
    }

    static LayoutTreeNode<LeafState> widgetTreesToLayoutTreeRoot(final Collection<Tree<WidgetGroup>> trees) {
        final var intermediateStates = trees.stream().map(WidgetTreeToLayoutTree::processRootGroup).toList();
        return fromIntermediateStates(intermediateStates).toLayoutTreeNode();
    }

    private static IntermediateState processRootGroup(final Tree<WidgetGroup> tree) {
        CheckUtils.checkArgument(tree.isRoot(), "Root group is expected here");
        return combineChildren(tree);
    }

    private static IntermediateState processGroup(final Tree<WidgetGroup> tree) {
        final var combined = combineChildren(tree);
        final var widgetGroupClass = tree.getType();
        if (List.of(Inside.class, After.class, Before.class, AfterAllOf.class, BeforeAllOf.class).stream()
            .anyMatch(ann -> widgetGroupClass.isAnnotationPresent(ann))) {
            return combined;
        }
        return combined.combineToLeafIfSelfContained(widgetGroupClass);
    }

    private static IntermediateState.StatesWithLayout combineChildren(final Tree<WidgetGroup> tree) {
        final var childStates = tree.getChildren().stream().filter(not(WidgetTreeToLayoutTree::isHidden))
            .map(WidgetTreeToLayoutTree::processTreeNode).toList();
        return fromIntermediateStates(childStates);
    }

    private static IntermediateState processTreeNode(final TreeNode<WidgetGroup> treeNode) {
        final var state =
            treeNode instanceof Tree<WidgetGroup> tree ? processGroup(tree) : new LeafState.TreeNodeState(treeNode);
        final var layoutAnnotation = treeNode.getAnnotation(Layout.class);
        return layoutAnnotation.map(Layout::value).<IntermediateState> map(state::atLayout).orElse(state);
    }

    private static final List<Class<? extends Annotation>> VISIBLE_WITHOUT_WIDGET_ANNOTATION =
        List.of(TextMessage.class);

    private static boolean isHidden(final TreeNode<WidgetGroup> node) {
        if (node instanceof Tree<WidgetGroup>) {
            return false;
        }
        return node.getAnnotation(Widget.class).isEmpty() && VISIBLE_WITHOUT_WIDGET_ANNOTATION.stream()
            .filter(node.getPossibleAnnotations()::contains).map(node::getAnnotation).allMatch(Optional::isEmpty);
    }

    record LeafStateWithLayout(IntermediateState.LeafState state, Optional<Class<?>> layout,
        Optional<Class<?>> defaultLayout) {

        LeafStateWithLayout atLayout(final Class<?> newLayout) {
            if (this.layout.isPresent()) {
                return this;
            }
            return new LeafStateWithLayout(state, Optional.of(newLayout), Optional.empty());
        }

        static LeafStateWithLayout fromLeafState(final IntermediateState.LeafState state) {
            return new LeafStateWithLayout(state, Optional.empty(), Optional.empty());
        }

    }

    public abstract static sealed class IntermediateState permits LeafState, StatesWithLayout {

        abstract Stream<LeafStateWithLayout> stream();

        abstract StatesWithLayout atLayout(Class<?> layout);

        public static sealed class LeafState extends IntermediateState
            permits LeafState.TreeNodeState, LeafState.LayoutTreeNodeState {
            @Override
            Stream<LeafStateWithLayout> stream() {
                return Stream.of(LeafStateWithLayout.fromLeafState(this));
            }

            @Override
            StatesWithLayout atLayout(final Class<?> layout) {
                return new StatesWithLayout(this, layout);
            }

            /**
             * An leaf state holding a {@link TreeNode} of {@link WidgetGroup}
             *
             * @author paulbaernreuther
             */
            public static final class TreeNodeState extends LeafState {
                final TreeNode<WidgetGroup> m_leaf;

                TreeNodeState(final TreeNode<WidgetGroup> leaf) {
                    m_leaf = leaf;
                }

                /**
                 * Receive the {@link TreeNode} of {@link WidgetGroup} which this state holds
                 *
                 * @return the leaf
                 */
                public TreeNode<WidgetGroup> getTreeNode() {
                    return m_leaf;
                }

                @Override
                public String toString() {
                    return m_leaf.toString();
                }

            }

            /**
             * An intermediate state holding a {@link LayoutTreeNode} for further traversal
             *
             * @author Paul Bärnreuther
             */
            public static final class LayoutTreeNodeState extends LeafState {

                final LayoutTreeNode<LeafState> m_layoutTreeNode;

                LayoutTreeNodeState(final LayoutTreeNode<LeafState> layoutTreeNode) {
                    m_layoutTreeNode = layoutTreeNode;
                }

                /**
                 * Receive the layout tree node which this state holds
                 *
                 * @return the layout tree node
                 */
                public LayoutTreeNode<LeafState> getLayoutTreeNode() {
                    return m_layoutTreeNode;
                }

                @Override
                public String toString() {
                    final Optional<Class<?>> rootValue = Optional.ofNullable(m_layoutTreeNode.getValue());
                    final Supplier<Optional<Class<?>>> firstChildValue =
                        () -> m_layoutTreeNode.getChildren().stream().findFirst().map(LayoutTreeNode::getValue);
                    return String.format("[Child layout (%s)]", //
                        rootValue.or(firstChildValue).map(Class::getSimpleName));
                }

            }
        }

        static final class StatesWithLayout extends IntermediateState {

            List<LeafStateWithLayout> m_states;

            StatesWithLayout(final List<LeafStateWithLayout> states) {
                m_states = states;
            }

            @Override
            public Stream<LeafStateWithLayout> stream() {
                return m_states.stream();
            }

            StatesWithLayout(final LeafState state) {
                this(state, null);
            }

            StatesWithLayout(final LeafState state, final Class<?> layout) {
                this(List.of(new LeafStateWithLayout(state, Optional.ofNullable(layout), Optional.empty())));
            }

            static StatesWithLayout fromIntermediateStates(final Collection<IntermediateState> states) {
                return new StatesWithLayout(states.stream().flatMap(IntermediateState::stream).toList());
            }

            /**
             * In case a layout is set on a group, the states with default (class) layouts have to be associated to that
             * new layout instead
             *
             * @param layout
             */
            @Override
            public StatesWithLayout atLayout(final Class<?> layout) {
                final var newStates = m_states.stream().map(state -> state.atLayout(layout)).toList();
                return new StatesWithLayout(newStates);
            }

            LayoutTree<LeafState> constructLayoutTree() {
                final Map<Class<?>, List<LeafState>> layoutClassesToControl = m_states.stream()
                    .filter(state -> state.layout().isPresent()).collect(Collectors.toMap(//
                        state -> state.layout().get(), state -> List.of(state.state()), //
                        (a, b) -> Stream.concat(a.stream(), b.stream()).toList()));
                final List<LeafState> statesWithNoLayout = m_states.stream().filter(state -> state.layout().isEmpty())
                    .map(LeafStateWithLayout::state).toList();

                return new LayoutTree<>(layoutClassesToControl, statesWithNoLayout);
            }

            IntermediateState combineToLeafIfSelfContained(final Class<?> containerClass) {
                final var layoutTree = constructLayoutTree();
                if (layoutTree.hasMultipleRoots()) {
                    return this;
                }

                final var rootNode = layoutTree.getRootNode();
                if (checkSelfContainedness(rootNode, containerClass)) {
                    return new LayoutTreeNodeState(rootNode);
                }
                return this;
            }

            /**
             * See the javadocs of {@link Layout} where we define what self-contained-ness means
             */
            private boolean checkSelfContainedness(final LayoutTreeNode<LeafState> rootNode,
                final Class<?> containerClass) {
                final var rootNodeClass = rootNode.getValue();

                if (rootNodeClass != null && rootNodeClass.isAssignableFrom(containerClass)) {
                    return true;
                }
                final var nextChildren = rootNode.getChildren();
                if (nextChildren.size() != 1) {
                    return false;
                }
                return checkSelfContainedness(nextChildren.stream().findFirst().get(), containerClass);
            }

            LayoutTreeNode<LeafState> toLayoutTreeNode() {
                final var layoutTree = constructLayoutTree();
                layoutTree.assertSingleRoot();
                return layoutTree.getRootNode();

            }

        }

    }

}
