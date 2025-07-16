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
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.LeafState;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.LeafState.TraversableLayoutTreeNodeState;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.WidgetTreeToLayoutTree.IntermediateState.LeafState.TreeNodeState;
import org.knime.core.webui.node.dialog.defaultdialog.layout.AfterAllOf;
import org.knime.core.webui.node.dialog.defaultdialog.layout.BeforeAllOf;
import org.knime.core.webui.node.dialog.defaultdialog.setting.dbtableselection.DBTableSelection;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * This utility class is used to resolve all layout annotations of a collection of widget trees.
 *
 * @author Paul Bärnreuther
 */
final class WidgetTreeToLayoutTree {

    private WidgetTreeToLayoutTree() {
        // utility class
    }

    /**
     * Builds the layout tree from the ground up. The ground are the leaves of the provided trees. While the main logic
     * of how layouts are connected, ordered and prepared for traversal is implemented in {@link LayoutTree} and
     * {@link LayoutTreeNode}, this utility class is used to process intermediate trees of {@link WidgetGroup
     * WidgetGroups} and combine them into a single layout tree.
     *
     * @param trees the settings trees
     * @return the root of the layout tree ready for traversal
     */
    static TraversableLayoutTreeNode<TreeNode<WidgetGroup>>
        widgetTreesToLayoutTreeRoot(final Collection<Tree<WidgetGroup>> trees) {
        final var intermediateStates = trees.stream().map(WidgetTreeToLayoutTree::processRootGroup).toList();
        return toTreeNodeTraversal(fromIntermediateStates(intermediateStates).toLayoutTreeNode());
    }

    /**
     * Process top-level settings groups (e.g. all model settings). This is different to processing widget groups: We do
     * not want to check for self-contained-ness here, since it would disable defining the layout of a node within one
     * of the settings classes (e.g. the view settings) and hooking into that from the other settings. Furthermore it
     * would not add any benefit if we would treat those as self-contained, since we usually do so to avoid having
     * multiple roots, which would not be the result here.
     */
    private static IntermediateState processRootGroup(final Tree<WidgetGroup> tree) {
        CheckUtils.checkArgument(tree.isRoot(), "Root group is expected here");
        return combineChildren(tree);
    }

    /**
     * When processing a group, we process all its children and then try to combine the result to a self-contained
     * layout tree.
     */
    private static IntermediateState processGroup(final Tree<WidgetGroup> tree) {
        return combineChildren(tree).combineToLeafIfSelfContained(tree.getRawClass());
    }

    private static IntermediateState.StatesWithLayout combineChildren(final Tree<WidgetGroup> tree) {
        final var childStates = tree.getChildren().stream().filter(not(WidgetTreeToLayoutTree::isHidden))
            .map(WidgetTreeToLayoutTree::processTreeNode).toList();
        return fromIntermediateStates(childStates);
    }

    /**
     * In case of a single widget, we wrap it into a {@link LeafState}. Otherwise, we use {@link #processGroup(Tree)}.
     *
     * This method is also responsible for attaching the layout annotation to the state if it is present.
     */
    private static IntermediateState processTreeNode(final TreeNode<WidgetGroup> treeNode) {
        final var state =
            (treeNode instanceof Tree<WidgetGroup> tree && !MultiFileSelection.class.equals(tree.getRawClass())
                && !DBTableSelection.class.equals(tree.getRawClass())) ? processGroup(tree)
                    : new LeafState.TreeNodeState(treeNode);
        final var layoutAnnotation = treeNode.getAnnotation(Layout.class);
        return layoutAnnotation.map(Layout::value).<IntermediateState> map(state::atLayout).orElse(state);
    }

    private static final List<Class<? extends Annotation>> VISIBLE_WITHOUT_WIDGET_ANNOTATION =
        List.of(TextMessage.class, DynamicSettingsWidget.class);

    private static boolean isHidden(final TreeNode<WidgetGroup> node) {
        if (node instanceof Tree<WidgetGroup>) {
            return false;
        }
        return node.getAnnotation(Widget.class).isEmpty() && VISIBLE_WITHOUT_WIDGET_ANNOTATION.stream()
            .filter(node.getPossibleAnnotations()::contains).map(node::getAnnotation).allMatch(Optional::isEmpty);
    }

    record LeafStateWithLayout(IntermediateState.LeafState state, Optional<Class<?>> layout) {

        LeafStateWithLayout atLayout(final Class<?> newLayout) {
            if (this.layout.isPresent()) {
                return this;
            }
            return new LeafStateWithLayout(state, Optional.of(newLayout));
        }

        static LeafStateWithLayout fromLeafState(final IntermediateState.LeafState state) {
            return new LeafStateWithLayout(state, Optional.empty());
        }

    }

    /**
     * For the construction of the layout tree, we start with controls and build a tree of controls and layouts. Since
     * at every step where a {@link WidgetGroup} is processed, we have to check whether it is "self-contained" (see
     * definition in javadocs of {@link Layout}), we need a more complex structure than a simple tree.
     *
     * This class represents the intermediate states of the layout tree construction. It can either hold a leaf, i.e. a
     * control ({@link TreeNodeState}) or something else self-contained ({@link TraversableLayoutTreeNodeState}), or a
     * collection of such associated to various layout classes ({@link StatesWithLayout}).
     */
    abstract static sealed class IntermediateState {

        abstract Stream<LeafStateWithLayout> stream();

        abstract StatesWithLayout atLayout(Class<?> layout);

        /**
         * Either a state coming from a {@link TreeNode} or the result of constructing the self-contained layout tree of
         * a {@link WidgetGroup}.
         */
        static sealed class LeafState extends IntermediateState {
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
             */
            static final class TreeNodeState extends LeafState {
                final TreeNode<WidgetGroup> m_leaf;

                TreeNodeState(final TreeNode<WidgetGroup> leaf) {
                    m_leaf = leaf;
                }

                /**
                 * Receive the {@link TreeNode} of {@link WidgetGroup} which this state holds
                 *
                 * @return the leaf
                 */
                TreeNode<WidgetGroup> getTreeNode() {
                    return m_leaf;
                }

                @Override
                public String toString() {
                    return m_leaf.toString();
                }

            }

            /**
             * An intermediate state holding a {@link TraversableLayoutTreeNode} for further traversal. This is used for
             * handing the self-contained-ness of a group of widgets.
             */
            static final class TraversableLayoutTreeNodeState extends LeafState {

                final TraversableLayoutTreeNode<LeafState> m_layoutTreeNode;

                TraversableLayoutTreeNodeState(final TraversableLayoutTreeNode<LeafState> layoutTreeNode) {
                    m_layoutTreeNode = layoutTreeNode;
                }

                /**
                 * Receive the layout tree node which this state holds
                 *
                 * @return the layout tree node
                 */
                TraversableLayoutTreeNode<LeafState> getLayoutTreeNode() {
                    return m_layoutTreeNode;
                }

                @Override
                public String toString() {
                    final Optional<Class<?>> rootValue = m_layoutTreeNode.layoutClass();
                    final Supplier<Optional<Class<?>>> firstChildValue = () -> m_layoutTreeNode.children().stream()
                        .findFirst().flatMap(TraversableLayoutTreeNode::layoutClass);
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
            Stream<LeafStateWithLayout> stream() {
                return m_states.stream();
            }

            StatesWithLayout(final LeafState state) {
                this(state, null);
            }

            StatesWithLayout(final LeafState state, final Class<?> layout) {
                this(List.of(new LeafStateWithLayout(state, Optional.ofNullable(layout))));
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
            StatesWithLayout atLayout(final Class<?> layout) {
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

            /**
             * See the javadocs of {@link Layout} where we define what self-contained-ness means
             */
            IntermediateState combineToLeafIfSelfContained(final Class<?> containerClass) {
                if (List.of(Inside.class, After.class, Before.class, AfterAllOf.class, BeforeAllOf.class).stream()
                    .anyMatch(containerClass::isAnnotationPresent)) {
                    return this;
                }
                final var layoutTree = constructLayoutTree();
                if (layoutTree.hasMultipleRoots()) {
                    return this;
                }

                final var rootNode = layoutTree.getRootNode();
                if (checkSelfContainedness(rootNode, containerClass)) {
                    return new TraversableLayoutTreeNodeState(rootNode);
                }
                return this;
            }

            private static boolean checkSelfContainedness(final TraversableLayoutTreeNode<LeafState> rootNode,
                final Class<?> containerClass) {
                final var rootNodeClass = rootNode.layoutClass();

                if (rootNodeClass.isPresent() && rootNodeClass.get().isAssignableFrom(containerClass)) {
                    return true;
                }
                final var nextChildren = rootNode.children();
                if (nextChildren.size() != 1) {
                    return false;
                }
                final var nextChild = nextChildren.stream().findFirst().get(); // NOSONAR since we checked the existence above
                return checkSelfContainedness(nextChild, containerClass);
            }

            TraversableLayoutTreeNode<LeafState> toLayoutTreeNode() {
                final var layoutTree = constructLayoutTree();
                layoutTree.assertSingleRoot();
                return layoutTree.getRootNode();

            }

        }

    }

    /**
     * As a final step of the algorithm, since we do not want to expose the intermediate states to the outside, we have
     * to convert the leaf states that can represent either a self-contained layout tree or a single control to layout
     * tree nodes of widget tree nodes.
     *
     * It is important to highlight here, that the order of the converted children matters and is preserved.
     */
    private static TraversableLayoutTreeNode<TreeNode<WidgetGroup>>
        toTreeNodeTraversal(final TraversableLayoutTreeNode<LeafState> leafStateTraversal) {
        final var leafStates = leafStateTraversal.controls();
        final var newChildren = leafStates.stream().map(WidgetTreeToLayoutTree::toTreeNodeTraversal).toList();
        final var mappedExistingChildren =
            leafStateTraversal.children().stream().map(WidgetTreeToLayoutTree::toTreeNodeTraversal).toList();
        final var combinedChildren = Stream.concat(newChildren.stream(), mappedExistingChildren.stream()).toList();
        final List<TreeNode<WidgetGroup>> newControls = List.of();
        return new TraversableLayoutTreeNode<>(newControls, combinedChildren, leafStateTraversal.layoutClass());
    }

    private static TraversableLayoutTreeNode<TreeNode<WidgetGroup>> toTreeNodeTraversal(final LeafState leafState) {
        if (leafState instanceof TreeNodeState treeNodeState) {
            final var treeNode = treeNodeState.getTreeNode();
            final List<TreeNode<WidgetGroup>> newControls = List.of(treeNode);
            final List<TraversableLayoutTreeNode<TreeNode<WidgetGroup>>> newChildren = List.of();
            return new TraversableLayoutTreeNode<>(newControls, newChildren);
        } else {
            final var layoutTreeNode = ((TraversableLayoutTreeNodeState)leafState).getLayoutTreeNode();
            return toTreeNodeTraversal(layoutTreeNode);
        }
    }

}
