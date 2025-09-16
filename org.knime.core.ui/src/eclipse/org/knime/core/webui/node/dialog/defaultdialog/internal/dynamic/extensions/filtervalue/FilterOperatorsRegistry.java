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
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * A utility class to deal with the filter operators extension point.
 */
public final class FilterOperatorsRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FilterOperatorsRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.filterOperators";

    private static final FilterOperatorsRegistry INSTANCE = new FilterOperatorsRegistry();

    private final Map<DataType, List<ValueFilterOperator<DataValue, FilterValueParameters>>> m_filterOperators;

    private FilterOperatorsRegistry() {
        // utility class
        m_filterOperators = Stream.concat(getCoreFilterOperators(), Stream.empty()) //
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));//, getFilterOperatorExtensions());
    }

    public static FilterOperatorsRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the filter operators for the given data type.
     *
     * @param type data type
     * @return the filter operators for the given data type, or an empty list if there are none
     */
    public List<ValueFilterOperator<DataValue, FilterValueParameters>> getOperators(final DataType type) {
        return m_filterOperators.getOrDefault(type, List.of());
    }

    /**
     * Gets all currently registered parameter classes.
     *
     * @return all currently registered parameter classes
     */
    public List<Class<FilterValueParameters>> getAllParameterClasses() {
        return m_filterOperators.values().stream().flatMap(List::stream)
            .map(ValueFilterOperator::getNodeParametersClass).distinct().toList();
    }

    private static Pair<DataType, List<ValueFilterOperator<DataValue, FilterValueParameters>>>
        familiesToPair(final FilterOperators<? extends DataValue> ops) {
        return new Pair<>(ops.getDataType(), //
            ops.getOperators() //
                .stream() //
                .map(mapper -> (ValueFilterOperator<DataValue, FilterValueParameters>)mapper) //
                .toList());
    }

    private static Stream<Pair<DataType, List<ValueFilterOperator<DataValue, FilterValueParameters>>>>
        getCoreFilterOperators() {
        final FilterOperators<DataValue> intOps = new CoreFilterValueOperators.IntCellOperators();
        final FilterOperators<DataValue> longOps = new CoreFilterValueOperators.LongCellOperators();
        final FilterOperators<DataValue> doubleOps = new CoreFilterValueOperators.DoubleCellOperators();

        return Stream.of(intOps, longOps, doubleOps) //
            .map(FilterOperatorsRegistry::familiesToPair) //
        ;
    }

    /**
     * Reads all extensions implementing {@link FilterOperators} from the extension point
     *
     * @return a map from data type to the corresponding to be preferred parameters class to create a cell of that type
     */
    private static Stream<FilterOperators> getFilterOperatorExtensions() {
        final var registry = Platform.getExtensionRegistry();
        final var point = registry.getExtensionPoint(EXT_POINT_ID);
        return Stream.of(point.getExtensions()) //
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
            .map(FilterOperatorsRegistry::readFilterOperatorsFactory) //
            .filter(Objects::nonNull);
    }

    private static FilterOperators readFilterOperatorsFactory(final IConfigurationElement cfe) {
        try {
            final var createDataCellParametersFactory = (FilterOperators)cfe.createExecutableExtension("factoryClass");
            LOGGER.debugWithFormat("Added filter operator parameters '%s' from '%s'",
                createDataCellParametersFactory.getClass().getName(), cfe.getContributor().getName());
            return createDataCellParametersFactory;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s", FilterOperators.class.getName(),
                cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

}
