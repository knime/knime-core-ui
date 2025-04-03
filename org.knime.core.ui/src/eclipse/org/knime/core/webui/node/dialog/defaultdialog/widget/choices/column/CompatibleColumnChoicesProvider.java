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
 *   Feb 10, 2025 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column;

import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;

/**
 * A {@link StringChoicesProvider} which can be given one or multiple classes extending {@link DataValue} and provides
 * the compatible columns of a given spec at the first input port.
 *
 * @author Paul Bärnreuther
 */
public class CompatibleColumnChoicesProvider implements ColumnChoicesProvider {

    private final Collection<Class<? extends DataValue>> m_valueClasses;

    /**
     * @param valueClass the class for which compatible columns should be provided
     */
    protected CompatibleColumnChoicesProvider(final Class<? extends DataValue> valueClass) {
        m_valueClasses = List.of(valueClass);
    }

    /**
     * @param valueClasses a list of classes for which compatible columns should be provided
     */
    protected CompatibleColumnChoicesProvider(final Collection<Class<? extends DataValue>> valueClasses) {
        m_valueClasses = valueClasses;
    }

    /**
     * @param spec
     * @param valueClasses a list of classes for which compatible columns should be determined
     * @return the columns of the spec which are compatible with any of the given value classes
     */
    public static List<DataColumnSpec> getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {
        return spec.stream()
            .filter(s -> valueClasses.stream().anyMatch(valueClass -> s.getType().isCompatible(valueClass))).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
        final var spec = context.getDataTableSpec(0);
        if (spec.isEmpty()) {
            return List.of();
        } else {
            return getCompatibleColumns(spec.get(), m_valueClasses);
        }
    }

    /**
     * ChoicesProvider providing all columns which are compatible with {@link StringValue}
     *
     * @author Paul Bärnreuther
     */
    public static final class StringColumnChoicesProvider extends CompatibleColumnChoicesProvider {

        StringColumnChoicesProvider() {
            super(StringValue.class);
        }

    }

    /**
     * ChoicesProvider providing all columns which are compatible with {@link DoubleValue}
     *
     * @author Paul Bärnreuther
     */
    public static final class DoubleColumnChoicesProvider extends CompatibleColumnChoicesProvider {

        DoubleColumnChoicesProvider() {
            super(DoubleValue.class);
        }

    }
}
