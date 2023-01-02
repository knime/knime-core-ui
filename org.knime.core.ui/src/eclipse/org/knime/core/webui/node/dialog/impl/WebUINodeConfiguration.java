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
 *   10 Nov 2022 (marcbux): created
 */
package org.knime.core.webui.node.dialog.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;

/**
 * Configuration for a {@link WebUINodeFactory WebUI node}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class WebUINodeConfiguration {

    /**
     * @return a new builder for assembly of {@link WebUINodeConfiguration WebUINodeConfigurations}
     */
    public static Builder builder() {
        return new Builder();
    }

    private final String m_name;

    private final String m_icon;

    private final String m_shortDescription;

    private final String m_fullDescription;

    private final Class<? extends DefaultNodeSettings> m_modelSettingsClass;

    private final PortDescription[] m_inPortDescriptions;

    private final PortDescription[] m_outPortDescriptions;

    private WebUINodeConfiguration(final RequirePorts builder) {
        m_name = builder.m_name;
        m_icon = builder.m_icon;
        m_shortDescription = builder.m_shortDescription;
        m_fullDescription = builder.m_fullDescription;
        m_modelSettingsClass = builder.m_modelSettingsClass;
        m_inPortDescriptions = builder.m_inputPortDescriptions.toArray(PortDescription[]::new);
        m_outPortDescriptions = builder.m_outputPortDescriptions.toArray(PortDescription[]::new);
    }

    String getName() {
        return m_name;
    }

    String getIcon() {
        return m_icon;
    }

    String getShortDescription() {
        return m_shortDescription;
    }

    String getFullDescription() {
        return m_fullDescription;
    }

    Class<? extends DefaultNodeSettings> getModelSettingsClass() {
        return m_modelSettingsClass;
    }

    PortDescription[] getInPortDescriptions() {
        return m_inPortDescriptions;
    }

    PortDescription[] getOutPortDescriptions() {
        return m_outPortDescriptions;
    }

    PortType[] getInputPortTypes() {
        return Stream.of(m_inPortDescriptions)//
                .map(PortDescription::getType)//
                .toArray(PortType[]::new);
    }

    PortType[] getOutputPortTypes() {
        return Stream.of(m_outPortDescriptions)//
                .map(PortDescription::getType)//
                .toArray(PortType[]::new);
    }

    /**
     * A builder for assembly of {@link WebUINodeConfiguration WebUINodeConfigurations}
     */
    public static final class Builder {

        private Builder() {
        }

        /**
         * @param name the name of the node, as shown in the node repository and description
         * @return the subsequent build stage
         */
        @SuppressWarnings("static-method")
        public RequireIcon name(final String name) {
            return icon -> shortDescription -> fullDescription -> modelSettingsClass -> new RequirePorts(name, icon,
                shortDescription, fullDescription, modelSettingsClass);
        }
    }

    /**
     * The build stage that requires an icon.
     */
    @FunctionalInterface
    public interface RequireIcon { // NOSONAR
        /**
         * @param icon relative path to the node icon
         * @return the subsequent build stage
         */
        RequireShortDescription icon(final String icon);
    }

    /**
     * The build stage that requires a short description.
     */
    @FunctionalInterface
    public interface RequireShortDescription { // NOSONAR
        /**
         * @param shortDescription the short node description
         * @return the subsequent build stage
         */
        RequireFullDescription shortDescription(final String shortDescription);
    }

    /**
     * The build stage that requires a full description.
     */
    @FunctionalInterface
    public interface RequireFullDescription { // NOSONAR
        /**
         * @param fullDescription the full node description
         * @return the subsequent build stage
         */
        RequireModelSettingsClass fullDescription(final String fullDescription);
    }

    /**
     * The build stage that requires the model settings.
     */
    @FunctionalInterface
    public interface RequireModelSettingsClass { // NOSONAR
        /**
         * @param modelSettingsClass the type of the model settings
         * @return the subsequent build stage
         */
        RequirePorts modelSettingsClass(final Class<? extends DefaultNodeSettings> modelSettingsClass);
    }

    /**
     * The (final) build stage in which the ports are defined.
     */
    public static final class RequirePorts {

        private final String m_name;

        private final String m_icon;

        private final String m_shortDescription;

        private final String m_fullDescription;

        private final Class<? extends DefaultNodeSettings> m_modelSettingsClass;

        private final List<PortDescription> m_inputPortDescriptions = new ArrayList<>();

        private final List<PortDescription> m_outputPortDescriptions = new ArrayList<>();

        RequirePorts(final String name, final String icon, final String shortDescription, final String fullDescription,
            final Class<? extends DefaultNodeSettings> modelSettingsClass) {
            m_name = name;
            m_icon = icon;
            m_shortDescription = shortDescription;
            m_fullDescription = fullDescription;
            m_modelSettingsClass = modelSettingsClass;
        }

        /**
         * Adds another input table to the node.
         *
         * @param name the name of the node's next input table
         * @param description the description of the node's next input table
         * @return this build stage
         */
        public RequirePorts addInputTable(final String name, final String description) {
            return addInputPort(name, BufferedDataTable.TYPE, description);
        }

        /**
         * Adds another input port to the node.
         *
         * @param name the name of the node's next input port
         * @param type the type of the node's next input port
         * @param description the description of the node's next input port
         * @return this build stage
         */
        public RequirePorts addInputPort(final String name, final PortType type, final String description) {
            m_inputPortDescriptions.add(new PortDescription(name, type, description));
            return this;
        }

        /**
         * Adds another output table to the node.
         *
         * @param name the name of the node's next output table
         * @param description the description of the node's next output table
         * @return this build stage
         */
        public RequirePorts addOutputTable(final String name, final String description) {
            return addOutputPort(name, BufferedDataTable.TYPE, description);
        }

        /**
         * Adds another output port to the node.
         *
         * @param name the name of the node's next output port
         * @param type the type of the node's next output port
         * @param description the description of the node's next output port
         * @return this build stage
         */
        public RequirePorts addOutputPort(final String name, final PortType type, final String description) {
            m_outputPortDescriptions.add(new PortDescription(name, type, description));
            return this;
        }

        /**
         * @return the built node
         */
        public WebUINodeConfiguration build() {
            return new WebUINodeConfiguration(this);
        }
    }

}
