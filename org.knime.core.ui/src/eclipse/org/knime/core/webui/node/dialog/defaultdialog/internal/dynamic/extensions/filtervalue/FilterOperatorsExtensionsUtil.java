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
 *   11 Nov 2024 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.BuiltinOperator.SingleCellOperatorFamily.ComparableCellOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.DoubleCellParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.IntCellParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.CoreFilterValueParameters.LongCellParameters;

/**
 * A utility class to deal with the filter operators extension point.
 */
@SuppressWarnings("rawtypes")
public final class FilterOperatorsExtensionsUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FilterOperatorsExtensionsUtil.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.filterOperators";

    private static final Map<DataType, List<FilterOperator2<?>>> FILTER_OPERATORS_CORE = Map.ofEntries( //
        Map.entry(IntCellFactory.TYPE, List.of(new ComparableCellOperatorFamily<>(IntCellParameters.class))), //
        Map.entry(DoubleCellFactory.TYPE, List.of(new ComparableCellOperatorFamily<>(DoubleCellParameters.class))), //
        Map.entry(LongCellFactory.TYPE, List.of(new ComparableCellOperatorFamily<>(LongCellParameters.class))) //
    );

    private FilterOperatorsExtensionsUtil() {
        // utility class
    }

    /**
     * Reads all extensions implementing {@link FilterOperatorsFactory} from the extension point
     *
     * @return a map from data type to the corresponding to be preferred parameters class to create a cell of that type
     */
    public static Map<DataType, List<FilterOperator2<?>>> getFilterOperatorExtensions() {
        if (true) {
            return FILTER_OPERATORS_CORE;
        }
        final var registry = Platform.getExtensionRegistry();
        final var point = registry.getExtensionPoint(EXT_POINT_ID);
        final var extensionProvidedClasses = Stream.of(point.getExtensions()) //
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
            .map(FilterOperatorsExtensionsUtil::readFiterOperatorsFactory) //
            .filter(Objects::nonNull) //
            .collect(Collectors.toMap(//
                FilterOperatorsFactory::getDataType, //
                FilterOperatorsFactory::getOperators//
            ));
        return Stream.concat(FILTER_OPERATORS_CORE.entrySet().stream(), extensionProvidedClasses.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    private static FilterOperatorsFactory readFiterOperatorsFactory(final IConfigurationElement cfe) {
        try {
            final var createDataCellParametersFactory =
                (FilterOperatorsFactory)cfe.createExecutableExtension("factoryClass");
            LOGGER.debugWithFormat("Added filter operator parameters '%s' from '%s'",
                createDataCellParametersFactory.getClass().getName(), cfe.getContributor().getName());
            return createDataCellParametersFactory;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s",
                FilterOperatorsFactory.class.getName(), cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

}
