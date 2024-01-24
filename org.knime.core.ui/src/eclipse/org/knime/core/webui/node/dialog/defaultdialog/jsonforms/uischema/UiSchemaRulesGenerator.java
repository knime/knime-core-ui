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
 *   Apr 3, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_CONDITION;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_EFFECT;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema.TAG_RULE;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.rule.ConstantExpression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Expression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.ConstantSignal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.JsonFormsExpression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Operator;
import org.knime.core.webui.node.dialog.defaultdialog.rule.ScopedExpression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Paul Bärnreuther
 */
final class UiSchemaRulesGenerator {

    private final ObjectMapper m_mapper;

    private final Map<Class<?>, ScopedExpression> m_signalsMap;

    private final Effect m_effect;

    private final DefaultNodeSettingsContext m_nodeSettingsContext;

    private JsonFormsExpressionResolver m_visitor;

    /**
     * @param mapper an object mapper used to resolve given conditions
     * @param field a field for which the effect of a rule is to be determined
     * @param signalsMap the map of all signals in the settings context at hand. It maps the ids of {@link Signal}
     *            annotations to a construct holding the respective condition and the scope of the associated field.
     * @param nodeSettingsContext the node's context (inputs, flow vars)
     */
    UiSchemaRulesGenerator(final ObjectMapper mapper, final Effect effect,
        final Map<Class<?>, ScopedExpression> signalsMap, final DefaultNodeSettingsContext nodeSettingsContext) {
        m_mapper = mapper;
        m_effect = effect;
        m_signalsMap = signalsMap;
        m_nodeSettingsContext = nodeSettingsContext;
        m_visitor = new JsonFormsExpressionResolver(m_mapper);
    }

    /**
     * Applies a rule to an object node based on the {@link Effect} annotation of a given field. The linked sources are
     * fetched and combined using the provided operator, with nested operations being resolved recursively. For more
     * information on the resolution of different operations, see {@link JsonFormsUiSchemaUtil}.
     *
     * @param control the object node to which the rule should be applied
     */
    public void applyRulesTo(final ObjectNode control) {
        if (m_effect == null) {
            return;
        }
        final var signalClasses = m_effect.signals();
        final var expressionList = new ArrayList<JsonFormsExpression>();
        for (var i = 0; i < signalClasses.length; i++) {
            final var signalClass = signalClasses[i];
            final var expressionOptional = createExpressionFromSignal(signalClass);
            if (expressionOptional.isEmpty()) {
                return; // "ignoreOnMissingSignals"
            }
            expressionList.add(expressionOptional.get());
        }
        final var rule = control.putObject(TAG_RULE).put(TAG_EFFECT, String.valueOf(m_effect.type()));

        final var operationClass = m_effect.operation();
        rule.set(TAG_CONDITION,
            instantiateOperation(operationClass, expressionList.toArray(JsonFormsExpression[]::new)).accept(m_visitor));
    }

    /**
     * Extracts the expression from a signal added to an effect.
     *
     * @param signalClass The signal class as defined in the @Effect annotation.
     * @return Either an expression or an empty optional if the effect has the {@link Effect#ignoreOnMissingSignals()}
     *         set.
     */
    private Optional<JsonFormsExpression> createExpressionFromSignal(final Class<?> signalClass) {
        JsonFormsExpression expression = m_signalsMap.get(signalClass);
        if (expression == null) {
            if (ConstantSignal.class.isAssignableFrom(signalClass)) {
                try {
                    expression = new ConstantExpression(signalClass.asSubclass(ConstantSignal.class)
                        .getDeclaredConstructor().newInstance().applies(m_nodeSettingsContext));
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                    throw new UiSchemaGenerationException(
                        "Unable to instantiate instance of " + signalClass.getName(), ex);
                }
            } else if (m_effect.ignoreOnMissingSignals()) {
                return Optional.empty();
            } else {
                throw new UiSchemaGenerationException(String.format(
                    "Missing source annotation: %s. "
                            + "If this is wanted and the rule should be ignored instead of throwing this error, "
                            + "use the respective flag in the @Effect annotation.",
                            signalClass.getName()));
            }
        } else {
            // (artificial design limitation:) input signals not to be used as identifiers in @Signal annotation
            CheckUtils.check(!ConstantSignal.class.isAssignableFrom(signalClass), UiSchemaGenerationException::new,
                () -> String.format(
                    "Invalid source annotation: %s - it denotes a %s, "
                            + "which can not be referenced by a @Signal annotation.", //
                            signalClass, ConstantSignal.class.getSimpleName()));
        }
        return Optional.of(expression);
    }

    @SuppressWarnings("unchecked")
    private static Expression<JsonFormsExpression> instantiateOperation(
        @SuppressWarnings("rawtypes") final Class<? extends Operator> operationClass,
        final JsonFormsExpression[] expressions) {
        try {
            return instantiateWithSuitableConstructor(operationClass, expressions);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException ex) {
            throw new UiSchemaGenerationException(
                String.format("Failed to instantiate operation %s", operationClass.getSimpleName()), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Operator<JsonFormsExpression> instantiateWithSuitableConstructor(
        @SuppressWarnings("rawtypes") final Class<? extends Operator> operationClass,
        final JsonFormsExpression[] expressions)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final var constructors = operationClass.getDeclaredConstructors();
        final var multiParameterConstructor = getMultiParameterConstructor(constructors, expressions.length);
        if (multiParameterConstructor != null) {
            return (Operator<JsonFormsExpression>)multiParameterConstructor.newInstance((Object[])expressions);
        }
        final var arrayConstructor = getArrayConstructor(constructors);
        if (arrayConstructor != null) {
            final Object[] parameters = new Object[]{expressions};
            return (Operator<JsonFormsExpression>)arrayConstructor.newInstance(parameters);
        }
        throw new UiSchemaGenerationException(
            String.format("No valid constructor found for operation %s with %s expressions",
                operationClass.getSimpleName(), expressions.length));
    }

    /**
     * Finds a suitable constructor from the given array of constructors. A suitable constructor is defined as one that
     * accepts a specified number of parameters, each of type {@link Expression}.
     *
     * @param constructors an array of constructors to search through
     * @param numParams the number of parameters the suitable constructor should accept
     * @return the suitable constructor, or null if none is found
     *
     * @example public MyClass(Expression expression1, Expression expression2, Expression expression3) {}
     */
    private static Constructor<?> getMultiParameterConstructor(final Constructor<?>[] constructors,
        final int numParams) {
        return Arrays.asList(constructors).stream().filter(constructor -> {
            final var parameters = constructor.getParameterTypes();
            return parameters.length == numParams && Arrays.asList(parameters).stream()
                .allMatch(paramType -> paramType.isAssignableFrom(Expression.class));
        }).findAny().orElse(null);
    }

    /**
     * Finds a suitable constructor from the given array of constructors. A suitable constructor is defined as one that
     * accepts an array of {@link Expression}s as its sole parameter.
     *
     * @param constructors an array of constructors to search through
     * @return the suitable constructor, or null if none is found
     *
     * @example public MyClass(Expression[] expressions) {}
     * @example public MyClass(Expression... expressions) {}
     *
     */
    private static Constructor<?> getArrayConstructor(final Constructor<?>[] constructors) {
        return Arrays.asList(constructors).stream().filter(constructor -> {
            final var parameters = constructor.getParameterTypes();
            return parameters.length == 1 && parameters[0].isArray()
                && parameters[0].componentType().isAssignableFrom(Expression.class);
        }).findAny().orElse(null);
    }

}
