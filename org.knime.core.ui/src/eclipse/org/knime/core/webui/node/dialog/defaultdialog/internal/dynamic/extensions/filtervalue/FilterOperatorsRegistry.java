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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * The registry of all available filter operators, that is built-in operators and operators provided via the
 * "org.knime.core.ui.filterOperators" extension point.
 */
public final class FilterOperatorsRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FilterOperatorsRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.ui.filterOperators";

    private static final FilterOperatorsRegistry INSTANCE = new FilterOperatorsRegistry();

    private final Map<DataType, Set<FilterOp>> m_filterOperators;

    private FilterOperatorsRegistry() {
        // utility class
        m_filterOperators = Stream.concat(getCoreFilterOperators(), getFilterOperatorExtensions()) //
            .collect(Collectors.groupingBy(Pair::getFirst, //
                Collectors.flatMapping(p -> p.getSecond().stream(), new FilterOpAcc())));
    }

    /**
     * Gets the singleton instance of this registry.
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
        return m_filterOperators.getOrDefault(type, Collections.emptySet()) //
            .stream().map(FilterOp::getOperator).toList();
    }

    /**
     * Gets all currently registered parameter classes.
     *
     * @return all currently registered parameter classes
     */
    public List<Class<FilterValueParameters>> getAllParameterClasses() {
        return m_filterOperators.values().stream() //
            .flatMap(Set::stream).map(FilterOp::getOperator).map(FilterOperator::getNodeParametersClass).distinct()
            .toList();
    }

    private static Pair<DataType, List<FilterOp>> toDataTypePair(final FilterOperators ops) {
        return new Pair<>(ops.getDataType(), //
            ops.getOperators() //
                .stream() //
                .map(FilterOp::new) //
                .toList());
    }

    private static Stream<Pair<DataType, List<FilterOp>>> getCoreFilterOperators() {
        final FilterOperators intOps = new CoreFilterValueOperators.IntCellOperators();
        final FilterOperators longOps = new CoreFilterValueOperators.LongCellOperators();
        final FilterOperators doubleOps = new CoreFilterValueOperators.DoubleCellOperators();

        return Stream.of(intOps, longOps, doubleOps) //
            .map(FilterOperatorsRegistry::toDataTypePair) //
        ;
    }

    /**
     * Reads all extensions implementing {@link FilterOperators} from the extension point
     *
     * @return a map from data type to the corresponding to be preferred parameters class to create a cell of that type
     */
    private static Stream<Pair<DataType, List<FilterOp>>> getFilterOperatorExtensions() {
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

    /**
     * Helper accumulator that complains about duplicate IDs and ignores duplicate operators.
     */
    private static final class FilterOpAcc implements Collector<FilterOp, FilterOpAcc, Set<FilterOp>> {

        private final Set<FilterOp> m_ops = new LinkedHashSet<>();

        private final Set<String> m_ids = new HashSet<>();

        void add(final FilterOp op) {
            if (!m_ids.add(op.getId())) {
                LOGGER.coding(String.format("Operator \"%s\" defines already present ID \"%s\". ",
                    op.getOperator().getClass().getSimpleName(), op.getId())
                    + "Subsequent operations might ignore this operator.");
            }
            if (!m_ops.add(op)) {
                LOGGER.coding(String.format(
                    "Operator \"%s\" defines already present ID \"%s\" with same parameters and will be ignored.",
                    op.getOperator().getClass().getSimpleName(), op.getId()));
            }
        }

        FilterOpAcc combine(final FilterOpAcc other) {
            other.m_ops.forEach(this::add);
            return this;
        }
        @Override
        public Supplier<FilterOpAcc> supplier() {
            return FilterOpAcc::new;
        }

        @Override
        public BiConsumer<FilterOpAcc, FilterOp> accumulator() {
            return FilterOpAcc::add;
        }

        @Override
        public BinaryOperator<FilterOpAcc> combiner() {
            return FilterOpAcc::combine;
        }

        @Override
        public Function<FilterOpAcc, Set<FilterOp>> finisher() {
            return acc -> acc.m_ops;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.emptySet();
        }
    }

    /**
     * Helper class for filter operator identity based on ID and parameter class. The parameter class is included to
     * distinguish between internal operators with legacy settings and new internal operators.
     */
    private static final class FilterOp {

        private final String m_id;

        private final FilterOperator<FilterValueParameters> m_operator;

        private FilterOp(final FilterOperator<FilterValueParameters> operator) {
            m_id = operator.getId();
            m_operator = operator;
        }

        private String getId() {
            return m_id;
        }

        private FilterOperator<FilterValueParameters> getOperator() {
            return m_operator;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37) //
                .append(m_id) //
                .append(m_operator.getNodeParametersClass()) //
                .toHashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof FilterOp otherOp)) {
                return false;
            }
            return new EqualsBuilder() //
                .append(m_id, otherOp.m_id) //
                .append(m_operator.getNodeParametersClass(), otherOp.m_operator.getNodeParametersClass()) //
                .isEquals();
        }

    }

}
