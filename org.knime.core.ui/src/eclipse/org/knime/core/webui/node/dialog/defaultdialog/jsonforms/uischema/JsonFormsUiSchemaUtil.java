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
 *   Mar 21, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.rule.ScopedExpression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.impl.AsyncChoicesAdder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class for creating ui schema content from a settings POJO class.
 *
 *
 * The UiSchema generation follows these steps:
 * <ol type="1">
 * <li>Collect all {@link Layout} and {@link Signal} annotations and register all controls (see
 * {@link UiSchemaDefaultNodeSettingsTraverser})</li>
 * <li>Use order annotations (see e.g. {@link After}) and class hierarchies to determine a tree structure (see
 * {@link LayoutTree})</li>
 * <li>Generate the layout parts starting from the root and add the mapped controls (see
 * {@link LayoutNodesGenerator})</li>
 * </ol>
 *
 * @author Paul Bärnreuther
 */
public final class JsonFormsUiSchemaUtil {

    private JsonFormsUiSchemaUtil() {
        // utility class
    }

    private static ObjectMapper MAPPER; // NOSONAR

    /**
     * @return the configured mapper for ui-schema generation
     */
    static ObjectMapper getMapper() {
        if (MAPPER == null) {
            MAPPER = createMapper();
        }
        return MAPPER;
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper();
    }

    /**
     * Call this method to build the uischema of sub layouts which are independent from the parent layout apart from
     * having access to the parentFields
     *
     * @param parentFields the fields of the "outside" layout
     */
    static ObjectNode buildUISchema(final Map<String, Class<? extends WidgetGroup>> settingsClasses,
        final DefaultNodeSettingsContext context, final AsyncChoicesAdder asyncChoicesAdder,
        final Collection<JsonFormsControl> parentFields) {
        final var layoutSkeleton = resolveLayout(settingsClasses);
        layoutSkeleton.fields().addAll(parentFields);
        return new LayoutNodesGenerator(layoutSkeleton, context, asyncChoicesAdder).build();
    }

    /**
     * @param settingsClasses
     * @param context
     * @param asyncChoicesAdder
     * @return the ui schema resolved by the mapper from the given settings
     */
    public static ObjectNode buildUISchema(final Map<String, Class<? extends WidgetGroup>> settingsClasses,
        final DefaultNodeSettingsContext context, final AsyncChoicesAdder asyncChoicesAdder) {
        return buildUISchema(settingsClasses, context, asyncChoicesAdder, Collections.emptyList());
    }

    /**
     * Resolves a map of default node settings classes to a tree structure representing the layout of the node dialog
     *
     * @param settings the map of default node settings classes
     * @return the resolved tree structure and some additional information which is necessary to generator the uischema
     *         from that
     */
    public static LayoutSkeleton resolveLayout(final Map<String, Class<? extends WidgetGroup>> settings) {
        final var traverser = new UiSchemaDefaultNodeSettingsTraverser();
        final var traversalResult = traverser.traverse(settings);
        final var layoutTreeRoot = new LayoutTree(traversalResult.layoutPartToControls()).getRootNode();
        return new LayoutSkeleton(layoutTreeRoot, traversalResult.signals(), traversalResult.fields());
    }

    /**
     * @param layoutTreeRoot a tree structure representation of the node dialogs layout. Its leafs represent controls
     *            and other nodes can be visible layout elements or just structural placeholders.
     * @param signals a map of all present {@link Signal} annotations.
     * @param fields a collection of all traversed fields (the leaves of the tree)
     */
    public static record LayoutSkeleton(LayoutTreeNode layoutTreeRoot, Map<Class<?>, ScopedExpression> signals,
        Collection<JsonFormsControl> fields) {
    }
}
