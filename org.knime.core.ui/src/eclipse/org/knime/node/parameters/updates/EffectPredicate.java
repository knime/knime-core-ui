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
 *   22 Mar 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.updates;

import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.And;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ConstantPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.FrameworkPredicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Not;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Operator;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Or;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.PredicateVisitor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.ScopedPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer;

/**
 * The only way to construct such a predicate as a user of this API is by using the provided methods in the
 * {@link PredicateInitializer} and the operations {@link #and} {@link #or} and {@link #negate}.
 *
 * It can either be one of the three {@link Operator}s ({@link And}, {@link Or} or {@link Not}) defining how to combine
 * multiple predicates or an atomic predicate which is either constant ({@link ConstantPredicate}) depending on a value
 * ({@link ScopedPredicate}) or provided by the framework ({@link FrameworkPredicate}).
 *
 * Predicates are resolved via {@link PredicateVisitor}s.
 *
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface EffectPredicate {

    /**
     * @param <T> the type of the resolved value
     * @param visitor an implementation dependent predicate resolver
     * @return a resolved value of the predicate depending on the implementation.
     */
    <T> T accept(PredicateVisitor<T> visitor);

    /**
     * @param other
     * @return this and the other predicate is fulfilled
     */
    default EffectPredicate and(final EffectPredicate other) {
        return new And(this, other);
    }

    /**
     * @param other
     * @return this or the other predicate is fulfilled
     */
    default EffectPredicate or(final EffectPredicate other) {
        return new Or(this, other);
    }

    /**
     * @return the opposite predicate
     */
    default EffectPredicate negate() {
        return new Not(this);
    }

}
