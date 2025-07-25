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
package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.node.parameters.widget.choices.TypedStringChoice.PossibleTypeValue;

/**
 * Used to construct possible type values used in the dialog to select types in the {@link ColumnTypeFilter}.
 *
 * @author Paul Bärnreuther
 */
public final class ColumnTypeToPossibleTypeValueUtil {

    private ColumnTypeToPossibleTypeValueUtil() {
        // utility class
    }

    private static final Map<String, String> PREFERRED_VALUE_CLASS_TO_DISPLAY =
        DataTypeRegistry.getInstance().availableDataTypes().stream()//
            .collect(toMap(//
                ColumnTypeToPossibleTypeValueUtil::getTypeId, //
                ColumnTypeToPossibleTypeValueUtil::getTypeName, //
                (l, r) -> l));

    /**
     * Converts the data type of a flow variable to representation fit to be serialized to the frontend.
     *
     * @param dataType of a flow variable
     * @return the corresponding used possible type value
     */
    public static PossibleTypeValue fromVariableType(final DataType dataType) {
        return new PossibleTypeValue(getTypeId(dataType), getTypeName(dataType));
    }

    private static String getTypeName(final DataType dataType) {
        return dataType.getName();
    }

    private static String getTypeId(final DataType dataType) {
        return dataType.getPreferredValueClass().getName();
    }

    /**
     * Creates the ColumnTypeDisplay from the preferred value class.
     *
     * @param preferredValueClass the name of the {@link DataType#getPreferredValueClass()}
     * @return the display for the given preferredValueClass
     */
    public static Optional<PossibleTypeValue> fromPreferredValueClassString(final String preferredValueClass) {
        return getText(preferredValueClass).map(t -> new PossibleTypeValue(preferredValueClass, t));
    }

    private static Optional<String> getText(final String preferredValueClass) {
        var text = PREFERRED_VALUE_CLASS_TO_DISPLAY.get(preferredValueClass);
        if (text == null) {
            // some (old) DataTypes may not be registered via ExtensionPoint -> go through the utility factory
            return getNameFromUtilityFactory(preferredValueClass);
        }
        return Optional.of(text);
    }

    private static Optional<String> getNameFromUtilityFactory(final String preferredValueClass) {
        var valueClass = DataTypeRegistry.getInstance().getValueClass(preferredValueClass);
        if (valueClass.isPresent()) {
            var utilityFactory = DataType.getUtilityFor(valueClass.get());
            if (utilityFactory instanceof ExtensibleUtilityFactory) {
                return Optional.of(((ExtensibleUtilityFactory)utilityFactory).getName());
            }
        }
        return Optional.empty();
    }

}
