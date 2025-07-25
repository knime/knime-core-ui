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
 *   Apr 6, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_CONST;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ITEMS_MIN;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ONEOF;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PATTERN;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PROPERTIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_REQUIRED;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_CONTAINS;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_NOT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil.getMapper;

import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ArrayContainsCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ConditionVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.FalseCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.HasMultipleItemsCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.IsEnumChoiceCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.IsSpecificStringCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.IsStringChoiceCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.PatternCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.TrueCondition;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
class JsonFormsConditionResolver implements ConditionVisitor<ObjectNode> {

    private final TreeNode<WidgetGroup> m_widgetTreeNode;

    private final NodeParametersInput m_context;

    JsonFormsConditionResolver(final TreeNode<WidgetGroup> node, final NodeParametersInput context) {
        m_widgetTreeNode = node;
        m_context = context;
    }

    @Override
    public <E extends Enum<E>> ObjectNode visit(final OneOfEnumCondition<E> oneOfEnumCondition) {
        final var node = getMapper().createObjectNode();
        final var oneOf = node.putArray(TAG_ONEOF);
        for (var option : oneOfEnumCondition.oneOf()) {
            oneOf.addObject().put(TAG_CONST, option.name());
        }
        return node;
    }

    @Override
    public ObjectNode visit(final TrueCondition trueCondition) {
        return createConstTrueCondition();
    }

    static ObjectNode createConstTrueCondition() {
        return getMapper().createObjectNode().put(TAG_CONST, true);
    }

    @Override
    public ObjectNode visit(final FalseCondition falseCondition) {
        return getMapper().createObjectNode().put(TAG_CONST, false);
    }

    @Override
    public ObjectNode visit(final HasMultipleItemsCondition hasMultipleItemsCondition) {
        return getMapper().createObjectNode().put(TAG_ITEMS_MIN, 2);
    }

    @Override
    public ObjectNode visit(final IsSpecificStringCondition isSpecificStringCondition) {
        return getMapper().createObjectNode() //
            .put(TAG_CONST, isSpecificStringCondition.getValue());
    }

    @Override
    public ObjectNode visit(final PatternCondition patternCondition) {
        return getMapper().createObjectNode() //
            .put(TAG_PATTERN, patternCondition.getPattern());
    }

    @Override
    public ObjectNode visit(final ArrayContainsCondition arrayContainsCondition) {
        if (m_widgetTreeNode instanceof ArrayParentNode<WidgetGroup> arrayWidgetNode) {
            final var elementPredicate = new PredicateExtractor(arrayWidgetNode.getElementTree(), m_context)
                .createPredicate(arrayContainsCondition.getElementPredicate());
            final var containsCondition = elementPredicate.accept(new SchemaInternalPredicateVisitor(m_context));

            final var conditionObjectNode = getMapper().createObjectNode();
            conditionObjectNode.putObject(TAG_CONTAINS).setAll(containsCondition);
            return conditionObjectNode;
        }
        throw new UiSchemaGenerationException(
            "Invalid reference used for an \"array contains\" condition. It has to be an array of widget groups.");
    }

    @Override
    public ObjectNode visit(final IsEnumChoiceCondition isSpecialChoiceCondition) {
        final var node = getMapper().createObjectNode();
        node.putObject(TAG_PROPERTIES).set(IsEnumChoiceCondition.PROPERTY_NAME,
            isSpecialChoiceCondition.condition().accept(this));
        node.putArray(TAG_REQUIRED).add(IsEnumChoiceCondition.PROPERTY_NAME);
        return node;
    }

    @Override
    public ObjectNode visit(final IsStringChoiceCondition isRegularStringCondition) {
        final var node = getMapper().createObjectNode();
        final var properties = node.putObject(TAG_PROPERTIES);
        node.putArray(TAG_REQUIRED).add(IsStringChoiceCondition.REGULAR_CHOICE_PROPERTY_NAME);
        properties.putObject(IsStringChoiceCondition.ENFORCE_SPECIAL_PROPERTY_NAME).putObject(TAG_NOT).put(TAG_CONST,
            true);
        properties.set(IsStringChoiceCondition.REGULAR_CHOICE_PROPERTY_NAME,
            isRegularStringCondition.condition().accept(this));
        return node;
    }

}
