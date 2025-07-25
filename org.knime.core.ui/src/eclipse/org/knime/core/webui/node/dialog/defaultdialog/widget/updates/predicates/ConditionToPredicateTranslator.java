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
 *   Jun 4, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates;

import java.util.function.Function;

import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer.ArrayReference;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer.BooleanReference;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer.EnumReference;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer.StringOrEnumReference;
import org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer.StringReference;

/**
 * This class allows for translating the methods provided by the reference interfaces returned by the
 * {@link EffectPredicateProvider} back to individual condition classes to be further processed using the visitor pattern.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings({"javadoc"})
public class ConditionToPredicateTranslator {

    final Function<Condition, EffectPredicate> m_conditionToPredicate;

    ConditionToPredicateTranslator(final Function<Condition, EffectPredicate> conditionToPredicate) {
        m_conditionToPredicate = conditionToPredicate;
    }

    protected <C extends Condition> EffectPredicate createPredicate(final C condition) {
        return m_conditionToPredicate.apply(condition);
    }

    public static final class StringFieldReference extends ConditionToPredicateTranslator implements StringReference {

        public StringFieldReference(final Function<Condition, EffectPredicate> conditionToPredicate) {
            super(conditionToPredicate);
        }

        @Override
        public EffectPredicate isEqualTo(final String value) {
            return this.<IsSpecificStringCondition> createPredicate(() -> value);
        }

        @Override
        public EffectPredicate matchesPattern(final String pattern) {
            return this.<PatternCondition> createPredicate(() -> pattern);
        }

    }

    public static final class EnumFieldReference<E extends Enum<E>> extends ConditionToPredicateTranslator
        implements EnumReference<E> {

        public EnumFieldReference(final Function<Condition, EffectPredicate> conditionToPredicate) {
            super(conditionToPredicate);
        }

        @Override
        public EffectPredicate isOneOf(@SuppressWarnings("unchecked") final E... values) {
            return this.<OneOfEnumCondition<E>> createPredicate(() -> values);
        }

    }

    public static final class BooleanFieldReference extends ConditionToPredicateTranslator implements BooleanReference {

        public BooleanFieldReference(final Function<Condition, EffectPredicate> conditionToPredicate) {
            super(conditionToPredicate);
        }

        @Override
        public EffectPredicate isTrue() {
            return createPredicate(new TrueCondition());
        }

        @Override
        public EffectPredicate isFalse() {
            return createPredicate(new FalseCondition());
        }

    }

    public static final class ArrayFieldReference extends ConditionToPredicateTranslator implements ArrayReference {

        public ArrayFieldReference(final Function<Condition, EffectPredicate> conditionToPredicate) {
            super(conditionToPredicate);
        }

        @Override
        public EffectPredicate hasMultipleItems() {
            return createPredicate(new HasMultipleItemsCondition());
        }

        @Override
        public EffectPredicate containsElementSatisfying(final EffectPredicateProvider elementPredicate) {
            return this.<ArrayContainsCondition> createPredicate(() -> elementPredicate);
        }

    }

    public static final class StringOrEnumFieldReference<E extends Enum<E>> extends ConditionToPredicateTranslator
        implements StringOrEnumReference<E> {

        public StringOrEnumFieldReference(final Function<Condition, EffectPredicate> conditionToPredicate) {
            super(conditionToPredicate);
        }

        @Override
        public EnumReference<E> isEnumChoice() {
            return new EnumFieldReference<>(condition -> this.<IsEnumChoiceCondition> createPredicate(() -> condition));
        }

        @Override
        public StringReference isStringChoice() {
            return new StringFieldReference(
                condition -> this.<IsStringChoiceCondition> createPredicate(() -> condition));
        }

    }

}
