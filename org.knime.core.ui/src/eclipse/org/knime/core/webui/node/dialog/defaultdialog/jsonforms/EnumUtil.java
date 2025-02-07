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
 *   Feb 6, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.util.DescriptionUtil.TitleAndDescription;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class EnumUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EnumUtil.class);

    /**
     * converts an enum constant to a title and description.
     *
     * @param constant the enum constant to convert
     * @param <E> the type of enum
     * @return the converted enum constant
     */
    public static TitleAndDescription createConstantEntry(final Enum<?> constant) {
        var enumClass = constant.getDeclaringClass();
        var name = constant.name();
        try {
            final var field = enumClass.getField(name);
            if (field.isAnnotationPresent(Widget.class)) {
                throw new IllegalStateException(String.format(
                    "There is a @Widget annotation present at the enum field %s. Use the @Label annotation instead.",
                    name));
            }
            if (field.isAnnotationPresent(Label.class)) {
                final var label = field.getAnnotation(Label.class);
                return new TitleAndDescription(label.value(), label.description());
            }
        } catch (NoSuchFieldException | SecurityException e) {
            LOGGER.error(String.format("Exception when accessing field %s.", name), e);
        }
        var label = StringUtils.capitalize(name.toLowerCase(Locale.getDefault()).replace("_", " "));
        return new TitleAndDescription(label, null);
    }

    private EnumUtil() {
        // util class
    }
}
