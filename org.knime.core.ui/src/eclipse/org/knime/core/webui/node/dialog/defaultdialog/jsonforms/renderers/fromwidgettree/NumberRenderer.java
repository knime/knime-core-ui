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

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.NumberRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.util.InstantiationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation;

class NumberRenderer extends WidgetTreeControlRendererSpec implements NumberRendererSpec {

    private final Optional<NumberInputWidget> m_annotation;

    private final TypeBounds m_typeBounds;

    NumberRenderer(final TreeNode<WidgetGroup> node) {
        super(node);
        m_annotation = node.getAnnotation(NumberInputWidget.class);
        m_typeBounds = getTypeBounds(node);
    }

    @Override
    public Optional<NumberRendererOptions> getCustomOptions() {
        if (m_annotation.isEmpty()) {
            return Optional.empty();
        }
        final var validations = m_annotation.stream().flatMap(ann -> Stream.of(ann.validation()))
            .map(InstantiationUtil::createInstance).toArray(NumberInputWidgetValidation[]::new);
        final var validationProviders =
            m_annotation.stream().flatMap(ann -> Stream.of(ann.validationProvider())).toList();

        return Optional.of(new NumberRendererOptions() {

            @Override
            public Optional<NumberInputWidgetValidation[]> getValidations() {
                return Optional.of(validations).filter(val -> val.length > 0);
            }

            @Override
            public Optional<String[]> getValidationProviders() {
                return Optional.of(validationProviders.stream().map(Class::getName).toArray(String[]::new))
                    .filter(val -> val.length > 0);
            }

        });
    }

    private static TypeBounds getTypeBounds(final TreeNode<WidgetGroup> node) {
        final var rawClass = node.getRawClass();
        if (rawClass.equals(long.class)) {
            return TypeBounds.LONG;
        }
        if (rawClass.equals(int.class)) {
            return TypeBounds.INTEGER;
        }
        if (rawClass.equals(byte.class)) {
            return TypeBounds.BYTE;
        }
        return TypeBounds.NONE;
    }

    @Override
    public TypeBounds getTypeBounds() {
        return m_typeBounds;
    }

}
