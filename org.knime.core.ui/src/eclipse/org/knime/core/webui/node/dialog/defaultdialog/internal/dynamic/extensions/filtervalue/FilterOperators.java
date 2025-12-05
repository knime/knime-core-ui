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
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataType;

/**
 * Provide this factory via the "org.knime.core.ui.filterOperators" extension point to provide filter operators for a
 * {@link DataType}.
 *
 * <h3>ID namespacing for extension operators</h3>
 * <p>
 * <b>Important:</b> if the operators of this factory are contributed via the extension point and they are determined to
 * be third-party contributions, the IDs of operators returned by this factory will be automatically transformed to
 * prevent conflicts with built-in operators:
 * </p>
 * <ul>
 * <li>The ID will be converted to uppercase</li>
 * <li>The ID will be prefixed with {@code EXT_}</li>
 * </ul>
 * <p>
 * For example, if your operator returns {@code "myOperator"} from {@link FilterOperator#getId()}, the effective
 * registered ID will be {@code "EXT_MYOPERATOR"}.
 * </p>
 *
 * <h4>Best Practices</h4>
 * <ul>
 * <li><b>Use unique prefixes:</b> Include a short unique prefix in your operator IDs to avoid conflicts with other
 * extensions (e.g., {@code "myext_contains"} becomes {@code "EXT_MYEXT_CONTAINS"})</li>
 * <li><b>Avoid versioning IDs:</b> Do not include version numbers in your operator IDs. Instead, use a new parameter
 * class with appropriate {@link org.knime.node.parameters.persistence.Persistor} or
 * {@link org.knime.node.parameters.migration.Migration} logic to handle backward compatibility.</li>
 * <li><b>Keep IDs stable:</b> Maintain the same IDs across versions of your extension to ensure saved workflows
 * continue to work.</li>
 * </ul>
 *
 * @author Paul Bärnreuther
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public interface FilterOperators {

    /**
     * Returns the one data type this factory is used for. There must not be more than one factory per data type.
     *
     * @return the one data type this factory is used for
     */
    DataType getDataType();

    /**
     * Returns the operator families provided by this factory.
     *
     * @return the operator families provided by this factory
     */
    List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies(); // NOSONAR we need the wildcard here

    /**
     * Returns all operators provided by this factory. The default implementation collects all operators of all
     * families.
     *
     * @return all operators provided by this factory
     */
    @SuppressWarnings("unchecked")
    default List<FilterOperator<FilterValueParameters>> getOperators() {
        final List<FilterOperator<FilterValueParameters>> ops = new ArrayList<>();
        for (final var fam : getOperatorFamilies()) {
            for (final FilterOperator<? extends FilterValueParameters> op : fam.getOperators()) {
                ops.add((FilterOperator<FilterValueParameters>)op);
            }
        }
        return ops;
    }
}
