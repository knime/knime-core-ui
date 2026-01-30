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
 *   Jan 30, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultProductionPathSerializer;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Utilities for showing choices and storing a {@link ProductionPath} in WebUI dialogs.
 *
 * Use a string field, initializer it from a {@link ProductionPath} by calling
 * {@link #getPathIdentifier(ProductionPath)} and store the resulting and use a custom persistor extending
 * {@link ProductionPathPersistor} to save/load the value as a pair of producer and converter factory identifiers.
 *
 * TODO: UIEXT-3273: Instead of providing utilities how to use a String field to store a ProductionPath, we should
 * provide a proper ProductionPathParameter implementation that can be used in the WebUI dialog framework.
 *
 * @author Paul Bärnreuther
 */
public class ProductionPathUtils {

    private ProductionPathUtils() {
        // prevent instantiation
    }

    private static final String PRODUCER_CFG_KEY = "_producer";

    private static final String CONVERTER_CFG_KEY = "_converter";

    /**
     * Some delimiter that is assumed to not appear in factory identifiers. We use it to concatenate producer and
     * converter identifiers into a single string to use it as a key in the frontend.
     */
    static final String DELIMITER = "\0";

    /**
     * Gets a unique string identifier for the given production path by concatenating producer and converter factory
     * This is the identifier by the {@link ProductionPathPersistor}.
     *
     * @param productionPath the production path
     * @return the unique string identifier
     */
    public static String getPathIdentifier(final ProductionPath productionPath) {
        final var converterId = productionPath.getConverterFactory().getIdentifier();
        final var producerId = productionPath.getProducerFactory().getIdentifier();
        return getPathIdentifier(producerId, converterId);
    }

    private static String getPathIdentifier(final String producerId, final String converterId) {
        return producerId + DELIMITER + converterId;
    }

    /**
     * Checks whether the given string is a valid path identifier (which implicitly means that it contains the
     * delimiter).
     *
     * @param pathIdentifier the path identifier
     * @return true if valid, false otherwise
     */
    public static boolean isPathIdentifier(final String pathIdentifier) {
        return pathIdentifier != null && pathIdentifier.contains(DELIMITER);
    }

    /**
     * The reversal of {@link #getPathIdentifier(ProductionPath)}.
     *
     * @param pathIdentifier the path identifier
     * @param producerRegistry the producer registry to use
     * @return the production path
     * @throws InvalidSettingsException if loading the production path failed
     */
    public static ProductionPath fromPathIdentifier(final String pathIdentifier,
        final ProducerRegistry<?, ?> producerRegistry) throws InvalidSettingsException {
        final var serializer = new DefaultProductionPathSerializer(producerRegistry);
        final var nodeSettings = new NodeSettings("does_not_matter");
        final var parts = pathIdentifier.split(DELIMITER, 2);
        final var converterId = parts[1];
        final var producerId = parts[0];
        nodeSettings.addString(CONVERTER_CFG_KEY, converterId);
        nodeSettings.addString(PRODUCER_CFG_KEY, producerId);
        nodeSettings.addConfig(PRODUCER_CFG_KEY + "_config");
        return serializer.loadProductionPath(nodeSettings, "");
    }

    /**
     * A persistor that saves and loads a production path as a pair of producer and converter factory identifiers
     */
    public abstract static class ProductionPathPersistor implements NodeParametersPersistor<String> {

        String m_configKey;

        /**
         * Constructor.
         *
         * @param configKey the key to store the config holding converter and producer identifiers
         */
        protected ProductionPathPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final NodeSettingsRO pathSettings = settings.getNodeSettings(m_configKey);
            final var converterId = pathSettings.getString(CONVERTER_CFG_KEY);
            final var producerId = pathSettings.getString(PRODUCER_CFG_KEY);
            return producerId + DELIMITER + converterId;
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            final NodeSettingsWO pathSettings = settings.addNodeSettings(m_configKey);
            final var parts = param.split(DELIMITER, 2);
            final var converterId = parts[1];
            final var producerId = parts[0];
            pathSettings.addString(CONVERTER_CFG_KEY, converterId);
            pathSettings.addString(PRODUCER_CFG_KEY, producerId);
        }

        @Override
        public String[][] getConfigPaths() {
            // no flow variables for production paths
            return new String[0][];
        }

    }

}
