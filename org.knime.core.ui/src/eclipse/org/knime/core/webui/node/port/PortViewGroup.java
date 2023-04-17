/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 */
package org.knime.core.webui.node.port;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Ties together two port views: One based on the {@link PortObjectSpec} and one based on the corresponding
 * {@link PortObject}.
 *
 * @param specViewLabel The display label of the port object spec view
 * @param specViewFactory A factory supplying the port object spec view instance
 * @param viewLabel The display label of the port object view
 * @param viewFactory A factory supplying the port object view
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"rawtypes", "java:S1124", "javadoc"})  // false positive, modifiers are indeed in right order
public record PortViewGroup(String specViewLabel, PortSpecViewFactory specViewFactory, String viewLabel,
                            PortViewFactory viewFactory) {

    public static PortViewGroupBuilder builder() {
        return new PortViewGroupBuilder();
    }

    public static PortViewGroup of(PortViewFactory viewFac) {
        return new PortViewGroup(null, null, null, viewFac);
    }

    public static class PortViewGroupBuilder {
        private String m_specViewLabel;

        private PortSpecViewFactory m_specViewFactory;

        private String m_viewLabel;

        private PortViewFactory m_viewFactory;

        public PortViewGroupBuilder setSpecViewLabel(String specViewLabel) {
            this.m_specViewLabel = specViewLabel;
            return this;
        }

        public PortViewGroupBuilder setSpecViewFactory(PortSpecViewFactory specViewFactory) {
            this.m_specViewFactory = specViewFactory;
            return this;
        }

        public PortViewGroupBuilder setViewLabel(String dataViewLabel) {
            this.m_viewLabel = dataViewLabel;
            return this;
        }

        public PortViewGroupBuilder setViewFactory(PortViewFactory dataViewFactory) {
            this.m_viewFactory = dataViewFactory;
            return this;
        }

        public PortViewGroup build() {
            return new PortViewGroup(m_specViewLabel, m_specViewFactory, m_viewLabel, m_viewFactory);
        }
    }
}
