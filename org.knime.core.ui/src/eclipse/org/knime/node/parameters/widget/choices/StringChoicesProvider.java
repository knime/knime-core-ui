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
 *   Mar 18, 2024 (Paul Bärnreuther): created
 */
package org.knime.node.parameters.widget.choices;

import java.util.List;

import org.knime.node.parameters.NodeParametersInput;

/**
 * A class that provides an array of possible string values of a {@link ChoicesProvider} based on the current
 * {@link NodeParametersInput}.
 *
 * @author Paul Bärnreuther
 */
public non-sealed interface StringChoicesProvider extends ChoicesStateProvider<StringChoice> {

    /**
     * {@inheritDoc}
     *
     * Here, the state provider is already configured to compute the state initially before the dialog is opened. If
     * this is desired but other initial configurations (like dependencies) are desired, override this method and call
     * super.init within it. If choices should instead be asynchronously loaded once the dialog is opened, override this
     * method without calling super.init to configure the initializer to do so.
     */
    @Override
    default void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();

    }

    /**
     * Computes the array of possible values based on the {@link NodeParametersInput}.
     *
     * @param context the context that holds any available information that might be relevant for determining available
     *            choices
     * @return array of possible values, never {@code null}
     */
    default List<String> choices(final NodeParametersInput context) {
        throw new IllegalStateException("At least one method must be implemented: "
            + "StringChoicesStateProvider.choices or StringChoicesStateProvider.computeState");
    }

    @Override
    default List<StringChoice> computeState(final NodeParametersInput context) {
        return choices(context).stream().map(StringChoice::fromId).toList();
    }

}
