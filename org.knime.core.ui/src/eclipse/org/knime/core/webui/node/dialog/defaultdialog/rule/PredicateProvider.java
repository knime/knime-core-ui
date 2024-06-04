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
package org.knime.core.webui.node.dialog.defaultdialog.rule;

import org.knime.core.data.DataValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

/**
 * TODO
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
@FunctionalInterface
public interface PredicateProvider {

    /**
     * The provided predicate. The only way to construct such a predicate as a user of this API is by using the provided
     * methods in the {@link PredicateInitializer} and the operations {@link #and} {@link #or} and {@link #not}.
     *
     * @author Paul Bärnreuther
     */
    interface Predicate {

        default Predicate and(final Predicate other) {
            return new And(this, other);
        }

        default Predicate or(final Predicate other) {
            return new Or(this, other);
        }

        default Predicate negate() {
            return new Not(this);
        }
    }

    /**
     * TODO
     *
     * @param i
     * @param context
     * @return
     */
    Predicate init(PredicateInitializer i);

    /**
     * TODO
     *
     * @author Paul Bärnreuther
     */
    public interface PredicateInitializer {

        /**
         * TODO
         *
         * @param reference
         * @return
         */
        StringReference getString(Class<? extends Reference<String>> reference);

        /**
         * TODO
         *
         * @param reference
         * @return
         */
        BooleanReference getBoolean(Class<? extends Reference<Boolean>> reference);

        /**
         * TODO
         *
         * @param reference
         * @return
         */
        <T> ArrayReference getArray(Class<? extends Reference<T[]>> reference);

        /**
         * TODO
         *
         * @param reference
         * @return
         */
        ColumnSelectionReference getColumnSelection(Class<? extends Reference<ColumnSelection>> reference);

        /**
         * TODO
         *
         * @param <E>
         * @param reference
         * @return
         */
        <E extends Enum<E>> EnumReference<E> getEnum(Class<? extends Reference<E>> reference);

        /**
         * TODO
         *
         * @author Paul Bärnreuther
         */
        interface StringReference {
            Predicate isEqualTo(String value);

            Predicate matchesPattern(String value);

            Predicate isNoneString();
        }

        /**
         * TODO
         *
         * @author Paul Bärnreuther
         */
        interface BooleanReference {
            Predicate isTrue();

            Predicate isFalse();
        }

        /**
         * TODO
         *
         * @author Paul Bärnreuther
         */
        interface EnumReference<E extends Enum<E>> {
            @SuppressWarnings("unchecked")
            Predicate isOneOf(E... values);
        }

        /**
         * TODO
         *
         * @author Paul Bärnreuther
         */
        interface ArrayReference {

            Predicate hasMultipleItems();
        }

        /**
         * TODO
         *
         * @author Paul Bärnreuther
         */
        interface ColumnSelectionReference {

            Predicate isNoneColumn();

            Expression hasColumnName(String columnName);

            Expression hasColumnType(Class<? extends DataValue> type);
        }

        /**
         * TODO
         *
         * @param predicate
         * @return
         */
        Predicate getConstant(java.util.function.Predicate<DefaultNodeSettingsContext> predicate);

        /**
         * TODO
         *
         * @param predicateProviderClass
         * @return
         */
        Predicate getPredicate(Class<? extends PredicateProvider> predicateProviderClass);

    }

    /**
     * TODO
     *
     * @param predicates
     * @return
     */
    @SuppressWarnings("unchecked")
    default Predicate and(final Predicate... predicates) {
        return new And(predicates);
    }

    /**
     * TODO
     *
     * @param predicates
     * @return
     */
    @SuppressWarnings("unchecked")
    default Predicate not(final Predicate predicates) {
        return new Not(predicates);
    }

    /**
     * TODO
     *
     * @param predicates
     * @return
     */
    @SuppressWarnings("unchecked")
    default Predicate or(final Predicate... predicates) {
        return new Or(predicates);
    }

}
