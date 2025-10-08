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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.GreaterThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.LessThanOrEqualOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsNorMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;

/**
 * The registry of all available filter operators, that is built-in operators and operators provided via the
 * "org.knime.core.ui.filterOperators" extension point.
 */
public final class FilterOperatorsRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FilterOperatorsRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.filterOperators";

    static final Map<String, Class<? extends FilterOperatorBase>> OVERWRITE_FILTER_OPERATOR_BASES = Map.ofEntries( //
        Map.entry(EqualsOperator.ID, EqualsOperator.class), //
        Map.entry(NotEqualsOperator.ID, NotEqualsOperator.class), //
        Map.entry(NotEqualsNorMissingOperator.ID, NotEqualsNorMissingOperator.class), //
        Map.entry(LessThanOperator.ID, LessThanOperator.class), //
        Map.entry(GreaterThanOperator.ID, GreaterThanOperator.class), //
        Map.entry(LessThanOrEqualOperator.ID, LessThanOrEqualOperator.class), //
        Map.entry(GreaterThanOrEqualOperator.ID, GreaterThanOrEqualOperator.class) //
    );

    private static final FilterOperatorsRegistry INSTANCE = new FilterOperatorsRegistry();

    private final Map<DataType, List<FilterOperator<FilterValueParameters>>> m_filterOperators;

    private FilterOperatorsRegistry() {
        m_filterOperators = getFilterOperatorExtensions() //
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> {
                final List<FilterOperator<FilterValueParameters>> combined = new ArrayList<>();
                a.forEach(combined::add);
                b.forEach(combined::add);
                return combined;
            }, //
                HashMap::new));
        for (var entry : m_filterOperators.entrySet()) {
            try {
                validateFilterOperators(entry.getValue());
            } catch (IllegalStateException ex) {
                LOGGER.error(String.format(
                    "Loading operators for data type \"%s\" failed since validation errors ocurred. No operators for this data type will be available.",
                    entry.getKey().getName()), ex);
                entry.setValue(Collections.emptyList());
            }
        }

    }

    /**
     * Gets the singleton instance of this registry.
     *
     * @return the singleton instance of this registry
     */
    public static FilterOperatorsRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the filter operators for the given data type.
     *
     * @param type data type
     * @return the filter operators for the given data type, or an empty list if there are none
     */
    public List<FilterOperator<FilterValueParameters>> getOperators(final DataType type) {
        return m_filterOperators.getOrDefault(type, Collections.emptyList());
    }

    private static void validateFilterOperators(final List<FilterOperator<FilterValueParameters>> filterOperators) {
        final var partitioned = filterOperators.stream()
            .collect(Collectors.partitioningBy(FilterOperatorsRegistry::isOverwriteFilterOperator));
        validateOverwriteOperators(partitioned.get(true));
        validateNonOverwriteOperators(partitioned.get(false));
    }

    /**
     * @param overwriteOperators
     * @throws IllegalStateException if any of the operators has a wrong id of if the same interface is implemented by
     *             two operators
     */
    private static void
        validateOverwriteOperators(final List<FilterOperator<FilterValueParameters>> overwriteOperators) {
        final var partitionedByIsDeprecated =
            overwriteOperators.stream().collect(Collectors.partitioningBy(FilterOperator::isDeprecated));
        final var deprecatedOperators = partitionedByIsDeprecated.get(true);
        final var deprecatedActualIdsWithEntry = getIdWithEntryPairs(deprecatedOperators);
        validateIdWithEntryList(deprecatedActualIdsWithEntry);
        final var nonDeprecatedOperators = partitionedByIsDeprecated.get(false);
        final var actualIdWithEntry = getIdWithEntryPairs(nonDeprecatedOperators);
        validateIdWithEntryList(actualIdWithEntry);
        final var duplicateInterfaceNames = actualIdWithEntry.stream().collect(Collectors.groupingBy(Pair::getFirst))
            .entrySet().stream().filter(e -> e.getValue().size() > 1)
            .map(e -> e.getValue().get(0).getSecond().getValue().getSimpleName()).toList();
        if (duplicateInterfaceNames.size() > 0) {
            throw new IllegalStateException(
                String.format("Multiple overwrite filter operators implementing the same interface '%s' found.",
                    duplicateInterfaceNames));
        }
    }

    /**
     * @return List<Pair<actualId, Entry<expectedId, implementingClass>>>
     */
    private static List<Pair<String, Entry<String, Class<? extends FilterOperatorBase>>>>
        getIdWithEntryPairs(final List<FilterOperator<FilterValueParameters>> overwriteOperators) {
        List<Pair<String, Entry<String, Class<? extends FilterOperatorBase>>>> actualIdWithEntry =
            overwriteOperators.stream().map(op -> {
                final var entry = OVERWRITE_FILTER_OPERATOR_BASES.entrySet().stream()
                    .filter(e -> e.getValue().isInstance(op)).findFirst().orElseThrow();
                return new Pair<>(op.getId(), entry);
            }).toList();
        return actualIdWithEntry;
    }

    /**
     * The expectedId needs to match the actualId of the operator defined by OVERWRITE_FILTER_OPERATOR_BASES.
     *
     * @param actualIdWithEntry List<Pair<actualId, Entry<expectedId, implementingClass>>>
     */
    private static void validateIdWithEntryList(
        final List<Pair<String, Entry<String, Class<? extends FilterOperatorBase>>>> actualIdWithEntry) {
        for (final var idWithEntry : actualIdWithEntry) {
            if (!idWithEntry.getFirst().equals(idWithEntry.getSecond().getKey())) {
                throw new IllegalStateException(String.format(
                    "Overwrite filter operator with class '%s' has ID '%s'. This is incorrect, it must be '%s'.",
                    idWithEntry.getFirst(), idWithEntry.getSecond().getValue().getSimpleName()));
            }
        }
    }

    static void validateNonOverwriteOperators(final List<FilterOperator<FilterValueParameters>> nonOverwriteOperators) {
        final var ids = new HashSet<String>();
        for (final var op : nonOverwriteOperators) {
            if (OVERWRITE_FILTER_OPERATOR_BASES.containsKey(op.getId())) {
                throw new IllegalStateException(String
                    .format("Non-overwrite filter operator with ID '%s' found. This ID is reserved for internal use. "
                        + "Please choose a different ID for your operator.", op.getId()));
            }
            if (!op.isDeprecated() && !ids.add(op.getId())) {
                throw new IllegalStateException(String.format(
                    "Multiple non-overwrite filter operators with ID '%s' found. Please choose different IDs for your operators.",
                    op.getId()));
            }
        }
    }

    static boolean isOverwriteFilterOperator(final FilterOperator<FilterValueParameters> operator) {
        return OVERWRITE_FILTER_OPERATOR_BASES.values().stream().anyMatch(c -> c.isInstance(operator));
    }

    /**
     * Gets all currently registered parameter classes.
     *
     * @return all currently registered parameter classes
     */
    public List<Class<FilterValueParameters>> getAllParameterClasses() {
        return m_filterOperators.values().stream() //
            .flatMap(List::stream).map(FilterOperator::getNodeParametersClass).distinct().toList();
    }

    private static Pair<DataType, List<FilterOperator<FilterValueParameters>>>
        toDataTypePair(final FilterOperators ops) {
        return new Pair<>(ops.getDataType(), //
            ops.getOperators()); //
    }

    /**
     * Reads all extensions implementing {@link FilterOperators} from the extension point
     *
     * @return a map from data type to the corresponding to be preferred parameters class to create a cell of that type
     */
    private static Stream<Pair<DataType, List<FilterOperator<FilterValueParameters>>>> getFilterOperatorExtensions() {
        final var registry = Platform.getExtensionRegistry();
        final var point = registry.getExtensionPoint(EXT_POINT_ID);
        return Stream.of(point.getExtensions()) //
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())) //
            .map(FilterOperatorsRegistry::readFilterOperatorsFactory) //
            .filter(Objects::nonNull).map(FilterOperatorsRegistry::toDataTypePair);
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
