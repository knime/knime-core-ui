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
 *   Jan 23, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.schema;

import static java.util.function.Predicate.not;

import java.math.BigDecimal;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget.DoubleProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.schema.JsonSchemaExtractor.PropertyAccessor;

import com.github.victools.jsonschema.generator.FieldScope;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class WidgetPropertyAccessor implements PropertyAccessor {

    private final FieldScope m_field;

    private final DefaultNodeSettingsContext m_context;

    WidgetPropertyAccessor(final FieldScope field, final DefaultNodeSettingsContext context) {
        m_field = field;
        m_context = context;
    }

    @Override
    public Optional<Object> getDefault() {
        return Optional.ofNullable(new DefaultResolver(m_context).apply(m_field));
    }

    @Override
    public Optional<String> getTitle() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(Widget.class))//
                .map(Widget::title)//
                .filter(not(String::isEmpty));
    }

    @Override
    public Optional<String> getDescription() {
        return Optional
                .ofNullable(m_field.getAnnotationConsideringFieldAndGetter(Widget.class))//
                .map(Widget::description)//
                .filter(not(String::isEmpty));
    }

    @Override
    public Optional<BigDecimal> getMin() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(NumberInputWidget.class))//
                .map(numberInput -> resolveDouble(m_context, numberInput.minProvider(), numberInput.min()));
    }

    @Override
    public Optional<BigDecimal> getMax() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(NumberInputWidget.class))//
                .map(numberInput -> resolveDouble(m_context, numberInput.maxProvider(), numberInput.max()));
    }

    @Override
    public Optional<Integer> getStringMinLength() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(TextInputWidget.class))//
                .map(TextInputWidget::minLength)//
                .filter(length -> length >= 0);
    }

    @Override
    public Optional<Integer> getStringMaxLength() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(TextInputWidget.class))//
                .map(TextInputWidget::maxLength)//
                .filter(length -> length >= 0);
    }

    @Override
    public Optional<String> getStringPattern() {
        return Optional.ofNullable(m_field.getAnnotationConsideringFieldAndGetter(TextInputWidget.class))//
                .map(TextInputWidget::pattern)//
                .filter(pattern -> !pattern.isEmpty());
    }

    private static BigDecimal resolveDouble(final DefaultNodeSettingsContext context,
        final Class<? extends DoubleProvider> providerClass, final double value) {
        if (!DoubleProvider.class.equals(providerClass)) {
            var provider = InstantiationUtil.createInstance(providerClass);
            return BigDecimal.valueOf(provider.getValue(context));
        }
        if (!Double.isNaN(value)) {
            return BigDecimal.valueOf(value);
        }
        return null;
    }

}
