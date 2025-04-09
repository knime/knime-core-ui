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
 *   Jan 4, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class to enrich {@link UiSchemaGenerationException} messages with information about context nodes.
 *
 * @author Paul Bärnreuther
 */
final class LayoutTreeViewUtil {

    private LayoutTreeViewUtil() {
        // Util
    }

    static <T> String getTreeView(final List<LayoutTreeNode<T>> contextNodes) {
        return getCommonParent(contextNodes).toString();
    }

    private static <T> LayoutTreeNode<T> getCommonParent(final List<LayoutTreeNode<T>> contextNodes) {
        final var firstNode = contextNodes.get(0);
        final var otherNodes = contextNodes.subList(1, contextNodes.size());
        final var otherNodesParentsList = otherNodes.stream().map(LayoutTreeViewUtil::getParents).toList();
        final var commonParent = getCommonParent(firstNode, otherNodesParentsList);
        return commonParent.orElseGet(() -> createCommonParent(contextNodes));
    }

    private static <T> Set<LayoutTreeNode<T>> getParents(final LayoutTreeNode<T> node) {
        if (node.isRoot()) {
            return Set.of(node);
        }
        return Stream.concat(Stream.of(node), getParents(node.getParent()).stream()).collect(Collectors.toSet());
    }

    private static <T> Optional<LayoutTreeNode<T>> getCommonParent(final LayoutTreeNode<T> node,
        final Collection<Set<LayoutTreeNode<T>>> parentsOfOtherNodes) {
        if (parentsOfOtherNodes.stream().allMatch(parents -> parents.contains(node))) {
            return Optional.of(node);
        }
        if (node.isRoot()) {
            return Optional.empty();
        }
        return getCommonParent(node.getParent(), parentsOfOtherNodes);
    }

    private static <T> LayoutTreeNode<T> createCommonParent(final List<LayoutTreeNode<T>> contextNodes) {
        final LayoutTreeNode<T> artificialCommonParent = new LayoutTreeNode<>(null);
        artificialCommonParent.getChildren().addAll(contextNodes);
        return artificialCommonParent;
    }
}
