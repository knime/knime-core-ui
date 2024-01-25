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
 *   Jan 24, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.alias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.webui.node.impl.PortDescription;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class NodeAliasSpec {

    private final String m_name;

    private final String m_description;

    private final List<PortDescription> m_inputPorts;

    private final List<PortDescription> m_outputPorts;

    private NodeAliasSpec(final Builder builder) {
        m_name = builder.m_name;
        m_description = builder.m_description;
        m_inputPorts = Collections.unmodifiableList(new ArrayList<>(builder.m_inputPorts));
        m_outputPorts = Collections.unmodifiableList(new ArrayList<>(builder.m_outputPorts));
    }

    /**
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the inputPorts
     */
    public List<PortDescription> getInputPorts() {
        return m_inputPorts;
    }

    /**
     * @return the outputPorts
     */
    public List<PortDescription> getOutputPorts() {
        return m_outputPorts;
    }

    /**
     * @return a Builder for NodeAliasSpec
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for NodeAliasSpec.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Builder {

        private final List<PortDescription> m_inputPorts = new ArrayList<>();

        private final List<PortDescription> m_outputPorts = new ArrayList<>();

        private String m_name;

        private String m_description;

        private Builder() {

        }

        public Builder withName(final String name) {
            m_name = name;
            return this;
        }

        public Builder withDescription(final String description) {
            m_description = description;
            return this;
        }

        public Builder withInputPort(final PortDescription portDescription) {
            m_inputPorts.add(portDescription);
            return this;
        }

        public Builder withOutputPort(final PortDescription portDescription) {
            m_outputPorts.add(portDescription);
            return this;
        }

        public NodeAliasSpec build() {
            return new NodeAliasSpec(this);
        }
    }

}
