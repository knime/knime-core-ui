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
 *   Apr 7, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.fromwidgettree;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMaxValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMinValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.TypeDependentMaxValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.TypeDependentMinValidation;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;

class NumberRenderer extends WidgetTreeControlRendererSpec implements NumberRendererSpec {

    private final Optional<NumberInputWidget> m_annotation;

    private final TypeBounds m_typeBounds;

    private Optional<MinValidation> m_minValidation;

    private Optional<MaxValidation> m_maxValidation;

    private Optional<Double> m_stepSize;

    private boolean m_minValidationProviderPresent;

    private boolean m_maxValidationProviderPresent;

    NumberRenderer(final TreeNode<WidgetGroup> node) {
        super(node);
        m_annotation = node.getAnnotation(NumberInputWidget.class);
        m_typeBounds = getTypeBounds(node);
        // Filter out -1 sentinel value
        m_stepSize = m_annotation.map(NumberInputWidget::stepSize).filter(size -> size > 0);
        m_minValidation = m_annotation.map(ann -> ann.minValidation())
            .filter(cls -> !TypeDependentMinValidation.class.equals(cls)).map(InstantiationUtil::createInstance);
        m_maxValidation = m_annotation.map(ann -> ann.maxValidation())
            .filter(cls -> !TypeDependentMaxValidation.class.equals(cls)).map(InstantiationUtil::createInstance);
        m_minValidationProviderPresent = m_annotation.map(ann -> ann.minValidationProvider())
            .filter(cls -> !NoopMinValidationProvider.class.equals(cls)).isPresent();
        m_maxValidationProviderPresent = m_annotation.map(ann -> ann.maxValidationProvider())
            .filter(cls -> !NoopMaxValidationProvider.class.equals(cls)).isPresent();
        validateAnnotation();
    }

    private void validateAnnotation() {
        if (m_minValidation.isPresent() && m_minValidationProviderPresent) {
            throw new UiSchemaGenerationException("NumberInputWidget cannot have both static minValidation and"
                + " dynamic minValidationProvider defined.");
        }
        if (m_maxValidation.isPresent() && m_maxValidationProviderPresent) {
            throw new UiSchemaGenerationException("NumberInputWidget cannot have both static maxValidation and"
                + " dynamic maxValidationProvider defined.");
        }
    }

    @Override
    public Optional<NumberRendererOptions> getCustomOptions() {
        if (m_annotation.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NumberRendererOptions() {
            @Override
            public Optional<Double> getStepSize() {
                return m_stepSize;
            }

            @Override
            public Optional<NumberRendererValidationOptions> getValidation() {
                return Optional.of(new NumberRendererValidationOptions() {

                    @Override
                    public Optional<MinValidation> getMin() {
                        return m_minValidation;
                    }

                    @Override
                    public Optional<MaxValidation> getMax() {
                        return m_maxValidation;
                    }
                });

            }

        });
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        final Map<String, Class<? extends StateProvider>> stateProviderClasses = new HashMap<>();
        if (m_minValidationProviderPresent) {
            stateProviderClasses.put(TAG_MIN_VALIDATION, m_annotation.get().minValidationProvider());
        }
        if (m_maxValidationProviderPresent) {
            stateProviderClasses.put(TAG_MAX_VALIDATION, m_annotation.get().maxValidationProvider());
        }
        return stateProviderClasses;

    }

    private static TypeBounds getTypeBounds(final TreeNode<WidgetGroup> node) {
        final var rawClass = ClassUtils.primitiveToWrapper(node.getRawClass());
        if (rawClass.equals(Long.class)) {
            return TypeBounds.LONG;
        }
        if (rawClass.equals(Integer.class)) {
            return TypeBounds.INTEGER;
        }
        if (rawClass.equals(Byte.class)) {
            return TypeBounds.BYTE;
        }
        return TypeBounds.NONE;
    }

    @Override
    public TypeBounds getTypeBounds() {
        return m_typeBounds;
    }

}
