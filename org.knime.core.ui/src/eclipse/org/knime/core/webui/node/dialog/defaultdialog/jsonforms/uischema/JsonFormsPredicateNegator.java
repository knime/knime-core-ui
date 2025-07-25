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
 *   Apr 4, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.FIELD_NAME_SCHEMA;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_NOT;

import java.util.List;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.And;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ConstantPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.FrameworkPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Not;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Or;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.PredicateVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ScopedPredicate;
import org.knime.node.parameters.updates.EffectPredicate;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A visitor used to resolve the "not" operation in the {@link JsonFormsPredicateResolver}
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("javadoc")
final class JsonFormsPredicateNegator implements PredicateVisitor<ObjectNode> {

    private final PredicateVisitor<ObjectNode> m_predicateResolver;

    /**
     * @param predicateResolver
     */
    public JsonFormsPredicateNegator(final JsonFormsPredicateResolver predicateResolver) {
        m_predicateResolver = predicateResolver;
    }

    @Override
    public ObjectNode visit(final And and) {
        final var resolvedOperation = new Or(reverseAll(and.getChildren()));
        return resolvedOperation.accept(m_predicateResolver);
    }

    @Override
    public ObjectNode visit(final Or or) {
        final var resolvedOperation = new And(reverseAll(or.getChildren()));
        return resolvedOperation.accept(m_predicateResolver);
    }

    private static EffectPredicate[] reverseAll(final List<EffectPredicate> predicates) {
        return predicates.stream().map(Not::new).toArray(EffectPredicate[]::new);
    }

    @Override
    public ObjectNode visit(final Not not) {
        return not.getChildPredicate().accept(m_predicateResolver);
    }

    @Override
    public ObjectNode visit(final ConstantPredicate constantPredicate) {
        return visitAtomicPredicate(constantPredicate);
    }

    @Override
    public ObjectNode visit(final ScopedPredicate scopedPredicate) {
        return visitAtomicPredicate(scopedPredicate);
    }

    @Override
    public ObjectNode visit(final FrameworkPredicate frameworkPredicate) {
        return visitAtomicPredicate(frameworkPredicate);
    }

    private ObjectNode visitAtomicPredicate(final EffectPredicate predicate) {
        final var node = predicate.accept(m_predicateResolver);
        final var positiveSchema = node.get(FIELD_NAME_SCHEMA);
        node.replace(FIELD_NAME_SCHEMA,
            JsonFormsUiSchemaUtil.getMapper().createObjectNode().set(TAG_NOT, positiveSchema));
        return node;
    }

}
