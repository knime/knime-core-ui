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

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsConsts.UiSchema;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.TextRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema.UiSchemaGenerationException;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMaxLengthValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopMinLengthValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopPatternValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.NoopStringProvider;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MinLengthValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

final class TextRenderer extends WidgetTreeControlRendererSpec implements TextRendererSpec {

    private Optional<TextInputWidget> m_annotation;

    private final boolean m_isCharacterType;

    private boolean m_isPrimitiveCharacter;

    TextRenderer(final TreeNode<WidgetGroup> node) {
        super(node);
        m_annotation = node.getAnnotation(TextInputWidget.class);
        m_isCharacterType = node.getRawClass() == char.class || node.getRawClass() == Character.class;
        m_isPrimitiveCharacter = m_isCharacterType && node.getRawClass().isPrimitive();
    }

    @Override
    public Optional<TextRendererOptions> getOptions() {
        final Optional<MinLengthValidation> minLengthValidation;
        final Optional<MaxLengthValidation> maxLengthValidation;
        final Optional<PatternValidation> patternValidation;
        final Optional<String> placeholder;

        if (m_annotation.isEmpty()) {
            minLengthValidation = Optional.empty();
            maxLengthValidation = Optional.empty();
            patternValidation = getDefaultCharacterValidation();
            placeholder = Optional.empty();
        } else {
            final var annotation = m_annotation.get();
            minLengthValidation = getValidation(MinLengthValidation.class, annotation.minLengthValidation());
            maxLengthValidation = getValidation(MaxLengthValidation.class, annotation.maxLengthValidation());
            patternValidation = getValidation(PatternValidation.class, annotation.patternValidation())
                .or(this::getDefaultCharacterValidation);
            placeholder = Optional.of(annotation.placeholder()).filter(s -> !s.isEmpty());

            validateCharacterTypeConstraints(minLengthValidation.isPresent(), maxLengthValidation.isPresent());
        }

        if (patternValidation.isEmpty() && minLengthValidation.isEmpty() && maxLengthValidation.isEmpty()
            && placeholder.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new TextRendererOptions() {

            @Override
            public Optional<TextRendererValidationOptions> getValidation() {
                if (patternValidation.isEmpty() && minLengthValidation.isEmpty() && maxLengthValidation.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(new TextRendererValidationOptions() {

                    @Override
                    public Optional<PatternValidation> getPattern() {
                        return patternValidation;
                    }

                    @Override
                    public Optional<MinLengthValidation> getMinLength() {
                        return minLengthValidation;
                    }

                    @Override
                    public Optional<MaxLengthValidation> getMaxLength() {
                        return maxLengthValidation;
                    }
                });
            }

            @Override
            public Optional<String> getPlaceholder() {
                return placeholder;
            }

        });
    }

    @Override
    public Map<String, Class<? extends StateProvider>> getStateProviderClasses() {
        if (m_annotation.isEmpty()) {
            return Map.of();
        }
        final var annotation = m_annotation.get();
        final Map<String, Class<? extends StateProvider>> stateProviderClasses = new HashMap<>();
        if (annotation.placeholderProvider() != NoopStringProvider.class) {
            stateProviderClasses.put(UiSchema.TAG_PLACEHOLDER, annotation.placeholderProvider());
        }
        if (annotation.minLengthValidationProvider() != NoopMinLengthValidationProvider.class) {
            stateProviderClasses.put(TAG_MIN_LENGTH_VALIDATION, annotation.minLengthValidationProvider());
        }
        if (annotation.maxLengthValidationProvider() != NoopMaxLengthValidationProvider.class) {
            stateProviderClasses.put(TAG_MAX_LENGTH_VALIDATION, annotation.maxLengthValidationProvider());
        }
        if (annotation.patternValidationProvider() != NoopPatternValidationProvider.class) {
            stateProviderClasses.put(TAG_PATTERN_VALIDATION, annotation.patternValidationProvider());
        }
        return stateProviderClasses;
    }

    private void validateCharacterTypeConstraints(final boolean hasMinLengthValidation,
        final boolean hasMaxLengthValidation) {
        if (!m_isCharacterType) {
            return;
        }

        if (hasMinLengthValidation) {
            throw new UiSchemaGenerationException("Min length validation is not applicable for character field.");
        }

        if (hasMaxLengthValidation) {
            throw new UiSchemaGenerationException("Max length validation is not applicable for character field.");
        }
    }

    private static <T> Optional<T> getValidation(final Class<T> ignoredDefault, final Class<? extends T> clazz) {
        return Optional.of(clazz).filter(cls -> !ignoredDefault.equals(cls)).map(InstantiationUtil::createInstance);
    }

    private Optional<PatternValidation> getDefaultCharacterValidation() {
        if (!m_isCharacterType) {
            return Optional.empty();
        }

        final boolean isPrimitive = m_isPrimitiveCharacter;
        if (isPrimitive) {
            return Optional.of(new IsSingleCharacterValidation());
        } else {
            return Optional.of(new PatternValidation() {
                @Override
                protected String getPattern() {
                    return ".{0,1}";
                }

                @Override
                public String getErrorMessage() {
                    return "Only one character is allowed.";
                }
            });
        }
    }

}
