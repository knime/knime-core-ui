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
 *   11 Dec 2025 (Thomas Reifenberger): extracted from RecursiveLoopEndDynamicNodeParameters
 */
package org.knime.node.parameters.array;

import java.util.function.Supplier;

import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Base class for {@link StateProvider}s that provide per-port values for a port group. Use this class as a base class
 * for state providers that provide an array of values where the length of the array corresponds to the number of ports
 * of a specific port group (input or output).
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 *
 * @param <V> the type of the per-port values
 */
public abstract class PerPortValueProvider<V> implements StateProvider<V[]> {

    /**
     * The side of the port group (input or output).
     */
    protected enum PortGroupSide {
            /**
             * Input port group.
             */
            INPUT,
            /**
             * Output port group.
             */
            OUTPUT
    }

    private final String m_portGroupId;

    private final PortGroupSide m_portGroupSide;

    private Supplier<V[]> m_widgetSettings;

    /**
     * Constructor.
     *
     * @param portGroupId The port group ID used in the node factory
     * @param portGroupSide The side of the port group (input or output)
     */
    protected PerPortValueProvider(final String portGroupId, final PortGroupSide portGroupSide) {
        m_portGroupId = portGroupId;
        m_portGroupSide = portGroupSide;
    }

    /**
     * The implementation of this method must return a value supplier referencing the field this value provider is used
     * on, e.g. <code>return initializer.getValueSupplier(MyParameterReference.class);</code>
     *
     * @param initializer the state provider initializer
     * @return the supplier
     */
    protected abstract Supplier<V[]> supplier(StateProviderInitializer initializer);

    /**
     * The implementation of this method must return a new array of the per-port value type with the given size, e.g.
     * <code>return new MyValue[size];</code>. This array is used to create the new array when the number of ports
     * changes. Note that the array elements should all be null, as all entries of the array will be overwritten anyway.
     *
     * @param size the size of the new array
     * @return the new array
     */
    protected abstract V[] newArray(int size);

    /**
     * The implementation of this method must return a new instance of the per-port value type, e.g.
     * <code>return new MyValue();</code>. This value is used to fill newly created entries in the array when the number
     * of ports increases.
     *
     * @return the new instance
     */
    protected abstract V newInstance();

    @Override
    public void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();
        m_widgetSettings = supplier(initializer);
    }

    @Override
    public V[] computeState(final NodeParametersInput parametersInput) {
        var numberOfPorts = getNumberOfPorts(parametersInput);
        var currentSettings = m_widgetSettings.get();

        if (currentSettings.length == numberOfPorts) {
            return currentSettings;
        }

        var newSettings = newArray(numberOfPorts);

        var numCurrentSettings = Math.min(currentSettings.length, numberOfPorts);
        System.arraycopy(currentSettings, 0, newSettings, 0, numCurrentSettings);

        for (var i = numCurrentSettings; i < numberOfPorts; i++) {
            newSettings[i] = newInstance();
        }

        return newSettings;
    }

    private int getNumberOfPorts(final NodeParametersInput parametersInput) {
        try {
            var portsConfig = parametersInput.getPortsConfiguration();
            var portLocations = m_portGroupSide == PortGroupSide.INPUT ? portsConfig.getInputPortLocation()
                : portsConfig.getOutputPortLocation();
            var indices = portLocations.get(m_portGroupId);
            if (indices != null) {
                return indices.length;
            }
        } catch (IllegalStateException ex) { // NOSONAR
            // fall through to the fallback below if no ports configuration is available
        }
        return m_portGroupSide == PortGroupSide.INPUT ? parametersInput.getInPortTypes().length
            : parametersInput.getOutPortTypes().length;
    }
}
