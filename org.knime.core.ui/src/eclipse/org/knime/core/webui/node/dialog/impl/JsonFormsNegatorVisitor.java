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
package org.knime.core.webui.node.dialog.impl;

import static org.knime.core.webui.node.dialog.impl.DefaultNodeSettingsService.FIELD_NAME_SCHEMA;
import static org.knime.core.webui.node.dialog.impl.JsonFormsUiSchemaGenerator.NOT_TAG;

import java.util.List;

import org.knime.core.webui.node.dialog.ui.rule.Condition;
import org.knime.core.webui.node.dialog.ui.rule.Operation;
import org.knime.core.webui.node.dialog.ui.rule.OperationVisitor;
import org.knime.core.webui.node.dialog.ui.rule.Operation.And;
import org.knime.core.webui.node.dialog.ui.rule.Operation.Not;
import org.knime.core.webui.node.dialog.ui.rule.Operation.Or;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A visitor used to resolve the "not" operation in the {@link JsonFormsOperationVisitor}
 * @author Paul Bärnreuther
 */
final class JsonFormsNegatorVisitor implements OperationVisitor<ObjectNode> {

    private final JsonFormsOperationVisitor m_operationVisitor;

    private final ObjectMapper m_mapper;

    /**
     * @param operationVisitor
     * @param mapper
     */
    public JsonFormsNegatorVisitor(final JsonFormsOperationVisitor operationVisitor, final ObjectMapper mapper) {
        m_operationVisitor = operationVisitor;
        m_mapper = mapper;
    }

    @Override
    public ObjectNode visit(final And and) {
        final var resolvedOperation = new Or(reverseAll(and.getChildren()));
        return resolvedOperation.accept(m_operationVisitor);
    }

    @Override
    public ObjectNode visit(final Or or) {
        final var resolvedOperation = new And(reverseAll(or.getChildren()));
        return resolvedOperation.accept(m_operationVisitor);
    }

    private static Operation[] reverseAll(final List<Operation> operations) {
        return operations.stream().map(Not::new).toArray(Operation[]::new);
    }

    @Override
    public ObjectNode visit(final Not not) {
        return not.getChildOperation().accept(m_operationVisitor);
    }

    @Override
    public ObjectNode visit(final Condition condition) {
        final var node = condition.accept(m_operationVisitor);
        final var positiveSchema = node.get(FIELD_NAME_SCHEMA);
        node.replace(FIELD_NAME_SCHEMA, m_mapper.createObjectNode().set(NOT_TAG, positiveSchema));
        return node;
    }

}
