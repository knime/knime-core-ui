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
 *   Aug 13, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ALLOF;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_ANYOF;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.Schema.TAG_PROPERTIES;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_NOT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsSchemaConditionUtil.createCondition;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.JsonFormsUiSchemaUtil.getMapper;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.And;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ConstantPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.FrameworkPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Not;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Or;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.PredicateVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ScopedPredicate;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Needed for attaching a rule not to a uischema root but as a nested condition inside the schema of another condition
 * (e.g. if an array "contains" an element with another rule)
 *
 */
final class SchemaInternalPredicateVisitor implements PredicateVisitor<ObjectNode> {

    private final NodeParametersInput m_context;

    SchemaInternalPredicateVisitor(final NodeParametersInput context) {
        m_context = context;
    }

    @Override
    public ObjectNode visit(final And and) {
        final var conditionNode = getMapper().createObjectNode();
        final var arrayNode = conditionNode.putArray(TAG_ALLOF);
        for (var predicate : and.getChildren()) {
            arrayNode.add(predicate.accept(this));
        }
        return conditionNode;
    }

    @Override
    public ObjectNode visit(final Or or) {
        final var conditionNode = getMapper().createObjectNode();
        final var arrayNode = conditionNode.putArray(TAG_ANYOF);
        for (var predicate : or.getChildren()) {
            arrayNode.add(predicate.accept(this));
        }
        return conditionNode;
    }

    @Override
    public ObjectNode visit(final Not not) {
        final var conditionNode = getMapper().createObjectNode();
        conditionNode.set(TAG_NOT, not.getChildPredicate().accept(this));
        return conditionNode;
    }

    @Override
    public ObjectNode visit(final ScopedPredicate scopedPredicate) {
        final var path = scopedPredicate.node().getPath();
        final var objectNode = getMapper().createObjectNode();
        var currentNode = objectNode;
        for (final var field : path) {
            currentNode = objectNode.putObject(TAG_PROPERTIES).putObject(field);
        }
        currentNode.setAll(createCondition(scopedPredicate, m_context));
        return objectNode;
    }

    @Override
    public ObjectNode visit(final ConstantPredicate constantPredicate) {
        return createCondition(constantPredicate);
    }

    @Override
    public ObjectNode visit(final FrameworkPredicate frameworkPredicate) {
        final var frameworkPredicateProviderClass = frameworkPredicate.providerClass();
        if (ArrayWidgetInternal.ElementIsEdited.class.equals(frameworkPredicateProviderClass)) {
            final var conditionNode = getMapper().createObjectNode();
            conditionNode.putObject(TAG_PROPERTIES).putObject("_edit")
                .setAll(JsonFormsConditionResolver.createConstTrueCondition());
            return conditionNode;
        }
        throw new NotImplementedException(
            String.format("Framework provider %s is not supported.", frameworkPredicateProviderClass.getName()));
    }

}
