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
 *   Apr 30, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms.uischema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.OptionalWidget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Responsible for the logic as described within {@link OptionalWidget};
 *
 * @author Paul Bärnreuther
 */
final class OptionalWidgetOptionsUtil {

    OptionalWidgetOptionsUtil() {
        // prevent instantiation
    }

    private static final ObjectMapper MAPPER = JsonFormsDataUtil.getMapper();

    /**
     * Either missing or true. Indicates that the widget should be hidden if the value is null.
     */
    private static final String FLAG = "hideOnNull";

    /**
     * The default value for the widget if no {@link OptionalWidget} is provided.
     */
    static final String STATIC_DEFAULT = "default";

    /**
     * The id of the supplied {@link OptionalWidget}.
     */
    private static final String DEFAULT_PROVIDER = "defaultProvider";

    static void addOptionalWidgetOptions(final TreeNode<WidgetGroup> optionalNode, final ObjectNode options) {
        options.put(FLAG, true);
        optionalNode.getAnnotation(OptionalWidget.class).ifPresentOrElse(
            provider -> options.put(DEFAULT_PROVIDER, provider.defaultProvider().getName()),
            () -> options.set(STATIC_DEFAULT, getDefaultValue(optionalNode))//
        );
    }

    private static JsonNode getDefaultValue(final TreeNode<WidgetGroup> node) {
        if (node.getType().isArrayType()) {
            return MAPPER.createArrayNode();
        }
        return MAPPER.valueToTree(getDefaultObjectValue(node));
    }

    private static Object getDefaultObjectValue(final TreeNode<WidgetGroup> node) {
        final Class<?> fieldClass = node.getRawClass();
        if (fieldClass.equals(String.class)) {
            return "";
        }
        if (Number.class.isAssignableFrom(fieldClass)) {
            return 0;
        }
        if (fieldClass.equals(LocalDate.class)) {
            return LocalDate.now();
        }
        if (fieldClass.equals(LocalTime.class)) {
            return LocalTime.now();
        }
        if (fieldClass.equals(LocalDateTime.class)) {
            return LocalDateTime.now();
        }
        if (fieldClass.equals(ZonedDateTime.class)) {
            return ZonedDateTime.now();
        }
        if (fieldClass.equals(ZoneId.class)) {
            return ZoneId.systemDefault();
        }
        throw new IllegalStateException(String.format(
            "Default value for optional widget at %s of type %s not supported."
                + " Use the @%s annotation to provide a default.",
            node.getPath(), fieldClass.getSimpleName(), OptionalWidget.class.getSimpleName()));
    }

}
