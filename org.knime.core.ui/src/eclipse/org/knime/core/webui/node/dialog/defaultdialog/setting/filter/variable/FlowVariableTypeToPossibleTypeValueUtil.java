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
 *   7 Feb 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable;

import static java.util.stream.Collectors.toMap;
import static org.knime.core.webui.node.view.flowvariable.FlowVariableDisplayUtil.getVariableTypeDisplay;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.webui.node.view.flowvariable.FlowVariableDisplayUtil;
import org.knime.node.parameters.widget.choices.TypedStringChoice.PossibleTypeValue;

/**
 * Used to construct possible type values used in the dialog to select types in the {@link FlowVariableTypeFilter}.
 *
 * @author Paul Bärnreuther
 */
public final class FlowVariableTypeToPossibleTypeValueUtil {

    private FlowVariableTypeToPossibleTypeValueUtil() {
        // utility class
    }

    private static final Map<String, String> FLOW_VARIABLE_ID_TO_DISPLAY =
        Arrays.stream(VariableTypeRegistry.getInstance().getAllTypes())//
            .collect(toMap(t -> t.getIdentifier(), FlowVariableDisplayUtil::getVariableTypeDisplay, (l, r) -> l));

    private static <T> String getTypeId(final VariableType<T> variableType) {
        return variableType.getIdentifier();
    }

    /**
     * Converts a {@link VariableType} to a {@link PossibleTypeValue} for use in the dialog.
     *
     * @param <T> the type of the variable type
     * @param variableType the variable type to convert
     * @return the used possible type value
     */
    public static <T> PossibleTypeValue fromVariableType(final VariableType<T> variableType) {
        return new PossibleTypeValue(getTypeId(variableType), getVariableTypeDisplay(variableType));
    }

    /**
     * Creates the ColumnTypeDisplay from the preferred value class.
     *
     * @param variableTypeId the identifier of a variable type
     * @return the display for the given preferredValueClass
     */
    static Optional<PossibleTypeValue> fromVariableTypeId(final String variableTypeId) {
        return getText(variableTypeId).map(t -> new PossibleTypeValue(variableTypeId, t));
    }

    private static Optional<String> getText(final String id) {
        var text = FLOW_VARIABLE_ID_TO_DISPLAY.get(id);
        return Optional.ofNullable(text);
    }

}
