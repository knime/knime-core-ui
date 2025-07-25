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
 *   Mar 22, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.OPTIONS_IS_ADVANCED;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_DESCRIPTION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_ELEMENTS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_LABEL;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_OPTIONS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_TYPE;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_HORIZONTAL_LAYOUT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_SECTION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_SIDE_DRAWER_SECTION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TYPE_VERTICAL_LAYOUT;

import java.util.function.Function;

import org.knime.core.webui.node.dialog.defaultdialog.internal.layout.CheckboxesWithVennDiagram;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.layout.VerticalLayout;
import org.knime.node.parameters.updates.Effect;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
enum LayoutPart {
        SECTION(LayoutPart::getSection), //
        HORIZONTAL_LAYOUT(LayoutPart::getHorizontalLayout), //
        VERTICAL_LAYOUT(LayoutPart::getVerticalLayout), //
        VIRTUAL_SECTION(LayoutNodeCreationContext::parent), //
        VENN(LayoutPart::getVenn);

    private Function<LayoutNodeCreationContext, ArrayNode> m_create;

    LayoutPart(final Function<LayoutNodeCreationContext, ArrayNode> create) {
        m_create = create;
    }

    static LayoutPart determineFromClassAnnotation(final Class<?> clazz) {
        if (clazz == null) {
            return VIRTUAL_SECTION;
        }
        if (clazz.isAnnotationPresent(Section.class)) {
            return SECTION;
        }
        if (clazz.isAnnotationPresent(CheckboxesWithVennDiagram.class)) {
            return VENN;
        }
        if (clazz.isAnnotationPresent(HorizontalLayout.class)) {
            return HORIZONTAL_LAYOUT;
        }
        if (clazz.isAnnotationPresent(VerticalLayout.class)) {
            return VERTICAL_LAYOUT;
        }
        return VIRTUAL_SECTION;
    }

    ArrayNode create(final ArrayNode parent, final Class<?> layoutClass, final UiSchemaRulesGenerator rulesGenerator) {
        return m_create.apply(new LayoutNodeCreationContext(parent, layoutClass, rulesGenerator));
    }

    private static ArrayNode getSection(final LayoutNodeCreationContext creationContext) {
        final var layoutClass = creationContext.layoutClass();
        final var sectionAnnotation = layoutClass.getAnnotation(Section.class);
        final var parent = creationContext.parent();
        final var node = parent.addObject();
        final var label = sectionAnnotation.title();
        node.put(TAG_LABEL, label);
        node.put(TAG_TYPE, sectionAnnotation.sideDrawer() ? TYPE_SIDE_DRAWER_SECTION : TYPE_SECTION);
        if (!sectionAnnotation.description().isEmpty()) {
            node.put(TAG_DESCRIPTION, sectionAnnotation.description());
        }
        processAdvancedAnnotation(creationContext, node);
        applyRules(node, creationContext);
        return node.putArray(TAG_ELEMENTS);
    }

    private static ArrayNode getVenn(final LayoutNodeCreationContext creationContext) {
        final var node = creationContext.parent().addObject();
        node.put(TAG_TYPE, "VennDiagram");
        return node.putArray(TAG_ELEMENTS);
    }

    private static ArrayNode getHorizontalLayout(final LayoutNodeCreationContext creationContext) {
        final var parent = creationContext.parent();
        final var node = parent.addObject();
        node.put(TAG_TYPE, TYPE_HORIZONTAL_LAYOUT);
        applyRules(node, creationContext);
        processAdvancedAnnotation(creationContext, node);
        return node.putArray(TAG_ELEMENTS);
    }

    private static ArrayNode getVerticalLayout(final LayoutNodeCreationContext creationContext) {
        final var parent = creationContext.parent();
        final var node = parent.addObject();
        node.put(TAG_TYPE, TYPE_VERTICAL_LAYOUT);
        applyRules(node, creationContext);
        processAdvancedAnnotation(creationContext, node);
        return node.putArray(TAG_ELEMENTS);
    }

    private static void processAdvancedAnnotation(final LayoutNodeCreationContext creationContext,
        final ObjectNode node) {
        if (creationContext.layoutClass().isAnnotationPresent(Advanced.class)) {
            node.putObject(TAG_OPTIONS).put(OPTIONS_IS_ADVANCED, true);
        }
    }

    private static void applyRules(final ObjectNode node, final LayoutNodeCreationContext creationContext) {
        creationContext.rulesGenerator().applyEffectTo(creationContext.layoutClass.getAnnotation(Effect.class), node);
    }

    private record LayoutNodeCreationContext(ArrayNode parent, Class<?> layoutClass,
        UiSchemaRulesGenerator rulesGenerator) {
    }
}
