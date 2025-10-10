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
 *   Sep 29, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.node.parameters.updates.legacy;

import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;

/**
 * A {@link StateProvider} that updates its value when the node is opened, but only if the current value is considered
 * empty. The new value is determined by the abstract {@link #autoGuessValue(NodeParametersInput)} method.
 *
 * @author Paul Bärnreuther
 * @param <S> The type of the provided state
 */
public abstract class AutoGuessValueProvider<S> extends UpdateOnOpenValueProvider<S> {

    /**
     * Attach the self reference to the same field that has this provider as value provider. I.e.
     *
     * <pre>
     *
     *  interface ColumnNameReference extends ParameterReference<String> {
     *  }
     *
     *  &#64;ValueReference(ColumnNameReference.class)
     *  &#64;ValueProvider(MyUpdateOnOpenValueProvider.class)
     *  String m_columnName;
     *
     *  static final class MyUpdateOnOpenValueProvider extends AutoGuessOnOpenValueProvider<String> {
     *
     *      MyUpdateOnOpenValueProvider() {
     *          super(ColumnNameReference.class);
     *      }
     *
     *      ...
     *  }
     *
     * </pre>
     *
     *
     * @param selfReference the self reference to the same field that has this provider as value provider
     */
    protected AutoGuessValueProvider(final Class<? extends ParameterReference<S>> selfReference) {
        super(selfReference);
    }

    @Override
    protected final S getValueOnOpen(final S currentValue, final NodeParametersInput parametersInput)
        throws StateComputationFailureException {
        if (isEmpty(currentValue)) {
            return autoGuessValue(parametersInput);
        }
        // Abort update if the current value is not empty
        throw new StateComputationFailureException();

    }

    /**
     * Determine whether the given value is considered empty, i.e. has not been applied by the user.
     *
     * @param value the value to check
     * @return true if the value is considered empty
     */
    protected abstract boolean isEmpty(final S value);

    /**
     * Compute a new value based on the given {@link NodeParametersInput}.
     *
     * @param parametersInput the current {@link NodeParametersInput }
     * @return the new auto-guessed default value
     * @throws StateComputationFailureException if no reasonable value could be determined
     */
    protected abstract S autoGuessValue(final NodeParametersInput parametersInput)
        throws StateComputationFailureException;

}
