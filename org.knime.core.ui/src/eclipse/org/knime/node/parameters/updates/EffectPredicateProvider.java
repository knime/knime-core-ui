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
package org.knime.node.parameters.updates;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.And;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Not;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.predicates.Or;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;

/**
 * Use the initializer to create a predicate depending on other fields in the current {@link NodeParameters} or the
 * {@link NodeParametersInput}.
 *
 * <h5>For array layouts:</h5>
 * <ul>
 * <li>If this predicate provider is used within the element settings of an array layout, only fields within these
 * element settings can be referenced.</li>
 * <li>Referencing fields within the element settings of an array layout from outside is only possible via referencing
 * the array widget field via {@link PredicateInitializer#getArray}</li>
 * </ul>
 *
 * @author Paul Bärnreuther
 */
@FunctionalInterface
public interface EffectPredicateProvider {

    /**
     * Use the initializer to create a predicate depending on other fields in the current {@link NodeParameters}, the
     * {@link NodeParametersInput} or on another {@link EffectPredicateProvider}.
     *
     * @param i
     * @return the provided predicate
     */
    EffectPredicate init(PredicateInitializer i);

    /**
     *
     * Used wihtin the {@link EffectPredicateProvider#init} method to declare predicates referencing other fields, the
     * {@link NodeParametersInput} or another {@link EffectPredicateProvider}
     *
     * <h5>Referencing other fields:</h5>
     * <p>
     * Currently, one can create predicates on other fields of the following types:
     * </p>
     *
     * <ul>
     * <li>{@link #getBoolean Booleans}</li>
     * <li>{@link #getString Strings}</li>
     * <li>{@link #getEnum Enums}</li>
     * <li>{@link #getArray Arrays of widget groups (Array widget)}</li>
     * <li>{@link #getStringOrEnum StringOrEnum}</li>
     * </ul>
     *
     * Note on array widgets:
     * <ul>
     * <li>If this predicate provider is used within the element settings of an array layout, only fields within these
     * element settings can be referenced.</li>
     * <li>Referencing fields within the element settings of an array layout from outside is only possible via
     * referencing the array widget field via {@link PredicateInitializer#getArray}</li>
     * </ul>
     *
     * <h5>Using the NodeParametersInput:</h5>
     * <p>
     * Using {@link #getConstant}
     * </p>
     *
     * <h5>Combining different Predicates</h5>
     * <p>
     * <ul>
     * <li>Once a predicate is constructed, one can use {@link EffectPredicate#and}, {@link EffectPredicate#or} and
     * {@link EffectPredicate#negate}</li>
     * <li>Different syntax with the same effect {@link EffectPredicateProvider#and}, {@link EffectPredicateProvider#or}
     * and {@link EffectPredicateProvider#not}</li>
     * <li>To make use of the predicate provided by another {@link EffectPredicateProvider}, use {@link #getPredicate}
     * either by class or by instance.
     * </ul>
     * </p>
     *
     * <h5>Ignoring missing references</h5>
     *
     * <p>
     * Use {@link #isMissing} to check for missing references if those are expected (this only occurs in the special
     * case where a {@link EffectPredicateProvider} is used in the context of different {@link NodeParameters}).
     * </p>
     *
     * @author Paul Bärnreuther
     */
    interface PredicateInitializer {

        /**
         * @param reference bound to exactly one String field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        StringReference getString(Class<? extends ParameterReference<String>> reference);

        /**
         * @param reference bound to exactly one boolean field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        BooleanReference getBoolean(Class<? extends ParameterReference<Boolean>> reference);

        /**
         * @param reference bound to exactly one array widget field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        <T> ArrayReference getArray(Class<? extends ParameterReference<T[]>> reference);

        /**
         * @param reference bound to exactly one {@link StringOrEnum} field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        <E extends Enum<E>> StringOrEnumReference<E>
            getStringOrEnum(Class<? extends ParameterReference<StringOrEnum<E>>> reference);

        /**
         * @param reference bound to exactly one enum field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        <E extends Enum<E>> EnumReference<E> getEnum(Class<? extends ParameterReference<E>> reference);

        /**
         * @param reference bound to exactly one {@link MultiFileSelection} field via {@link ValueReference}
         * @return an object that can be further transformed to a predicate using one of its methods
         */
        <F extends FileChooserFilters> MultiFileSelectionReference
            getMultiFileSelectionMode(Class<? extends ParameterReference<MultiFileSelection<F>>> reference);

        /**
         * Returned by {@link PredicateInitializer#getString}
         *
         * @author Paul Bärnreuther
         */
        interface StringReference {
            /**
             * Note that for a null value, the respective field has to be annotated with
             * com.fasterxml.jackson.annotation.JsonInclude(Include.ALWAYS}
             *
             * @param value
             * @return predicate
             */
            EffectPredicate isEqualTo(String value);

            /**
             * @param pattern a regular predicate pattern
             * @return predicate
             */
            EffectPredicate matchesPattern(String pattern);

        }

        /**
         * Returned by {@link PredicateInitializer#getBoolean}
         *
         * @author Paul Bärnreuther
         */
        interface BooleanReference {
            EffectPredicate isTrue();

            EffectPredicate isFalse();
        }

        /**
         * Returned by {@link PredicateInitializer#getEnum}
         *
         * @author Paul Bärnreuther
         * @param <E>
         */
        interface EnumReference<E extends Enum<E>> {
            /**
             * @param values
             * @return predicate that is fulfilled, when any of the given values is selected.
             */
            @SuppressWarnings("unchecked")
            EffectPredicate isOneOf(E... values);

        }

        /**
         * Returned by {@link PredicateInitializer#getMultiFileSelectionMode}
         *
         * @author Paul Bärnreuther
         */
        interface MultiFileSelectionReference {

            /**
             * Whether one of the {@link MultiFileSelectionMode}s that allow multiple files is selected.
             *
             * @return predicate that is fulfilled, when multiple file selection mode is selected.
             */
            default EffectPredicate isMultiFileSelection() {
                return getSelectionMode().isOneOf(//
                    MultiFileSelectionMode.FILES_IN_FOLDERS, //
                    MultiFileSelectionMode.FOLDERS, //
                    MultiFileSelectionMode.FILES_AND_FOLDERS//
                );
            }

            EnumReference<MultiFileSelectionMode> getSelectionMode();

        }

        /**
         * Returned by {@link PredicateInitializer#getArray}
         *
         * @author Paul Bärnreuther
         */
        interface ArrayReference {

            /**
             * @return predicate that is fulfilled, when the referenced array has at least two elements.
             */
            EffectPredicate hasMultipleItems();

            /**
             *
             * Note that {@link EffectPredicateProvider} is a functional interface, so an implementation could look like
             * this:
             *
             * {@code  i.getArray(ArrayFieldReference.class).containsElementSatisfying(
             *      el -> el.getBoolean(ElementBooleanFieldReference.class).isTrue()
             * );}
             *
             * @param elementPredicate as it could also be used for a field within the element settings (i.e. it
             *            references only fields within the element settings).
             * @return a predicate that is fulfilled if the elementPredicate is fulfilled for one of the elements of the
             *         referenced array.
             */
            EffectPredicate containsElementSatisfying(EffectPredicateProvider elementPredicate);
        }

        /**
         * Returned by {@link PredicateInitializer#getStringOrEnum}
         *
         * @author Paul Bärnreuther
         * @param <E> the type of the enum
         */
        interface StringOrEnumReference<E extends Enum<E>> {
            /**
             * A {@link StringOrEnum} can be given by a string choice or an enum choice. This method does not yet return
             * a predicate but a reference with which to create a predicate on an enum choice.
             *
             * @return a reference create a predicate on an enum choice.
             */
            EnumReference<E> isEnumChoice();

            /**
             * To check whether the value is a specific enum choice.
             *
             * @param specialChoice the special choice to check for
             * @return a predicate that is fulfilled if the value is the special choice
             */
            @SuppressWarnings("unchecked")
            default EffectPredicate isEnumChoice(final E specialChoice) {
                return isEnumChoice().isOneOf(specialChoice);
            }

            /**
             * A {@link StringOrEnum} can be given by a string choice or an enum choice. This method does not yet return
             * a predicate but a reference with which to create a predicate on a string choice.
             *
             * @return a reference create a predicate on a string choice.
             */
            StringReference isStringChoice();

        }

        /**
         * Use this method in case a {@link EffectPredicateProvider} is used in the context of two different
         * {@link NodeParameters} (e.g. by reusing the same {@link WidgetGroup} within both of them. This way, one can
         * avoid an error that a {@link ValueReference} annotations is missing in one of the cases and instead one can
         * use {@link #never} or {@link #always} to create a constant effect not depending on the reference.
         *
         * @param reference
         * @return whether the reference can be accessed using one of the other methods {@link #getString},
         *         {@link #getBoolean}, ...
         */
        boolean isMissing(Class<? extends ParameterReference<?>> reference);

        /**
         * This method can be used to create an effect that is constant as long as the {@link NodeParametersInput} does
         * not change. I.e., e.g., the one can depend on the presence of certain columns (or column types) or if dynamic
         * ports are enabled/shown or not.
         *
         * @param predicate on the {@link NodeParametersInput} when the dialog is opened
         * @return a {@link EffectPredicate} that can be returned in {@link #init} or combined with other
         *         {@link EffectPredicate}s
         */
        EffectPredicate getConstant(java.util.function.Predicate<NodeParametersInput> predicate);

        /**
         * This method should be used only in combination with {@link #isMissing}.
         *
         * @return a predicate that is never met.
         */
        default EffectPredicate never() {
            return getConstant(context -> false);
        }

        /**
         * This method should be used only in combination with {@link #isMissing}.
         *
         * @return a predicate that is always met.
         */
        default EffectPredicate always() {
            return getConstant(context -> true);
        }

        /**
         * Extract the provided predicate of another provider class. This is especially useful when this other predicate
         * is to be used in multiple places in different combinations with other conditions.
         *
         * @param predicateProviderClass
         * @return the provided predicate.
         */
        EffectPredicate getPredicate(Class<? extends EffectPredicateProvider> predicateProviderClass);

        /**
         * Extract the provided predicate of another provider. This is especially useful when this other predicate is to
         * be used in multiple places in different combinations with other conditions.
         *
         * @param predicateProvider
         * @return the provided predicate.
         */
        EffectPredicate getPredicate(EffectPredicateProvider predicateProvider);

    }

    /**
     * Use this method with {@link #init} to combine multiple predicates. Alternatively, use
     * {@link EffectPredicate#and}.
     *
     * @param predicates
     * @return A predicate that is fulfilled if and only if all of the given predicates are fulfilled.
     */
    default EffectPredicate and(final EffectPredicate... predicates) {
        return new And(predicates);
    }

    /**
     * Use this method with {@link #init} to negate a predicate. Alternatively, use {@link EffectPredicate#negate}.
     *
     * @param predicate
     * @return A predicate that is fulfilled if and only if the given predicate is not fulfilled.
     */
    default EffectPredicate not(final EffectPredicate predicate) {
        return new Not(predicate);
    }

    /**
     * Use this method with {@link #init} to combine multiple predicates. Alternatively, use {@link EffectPredicate#or}.
     *
     * @param predicates
     * @return A predicate that is fulfilled if and only if any of the given predicates are fulfilled.
     */
    default EffectPredicate or(final EffectPredicate... predicates) {
        return new Or(predicates);
    }

}
