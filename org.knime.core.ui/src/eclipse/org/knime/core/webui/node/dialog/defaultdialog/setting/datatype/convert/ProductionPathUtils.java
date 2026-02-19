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

import java.io.IOException;
import java.io.StringReader;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ProductionPathSerializer;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Utilities for showing choices and storing a {@link ProductionPath} in WebUI dialogs.
 *
 * Use a string field, initialize it from a {@link ProductionPath} by calling
 * {@link #getPathIdentifier(ProductionPath, ProductionPathSerializer)} and store the resulting and use a custom
 * persistor extending {@link ProductionPathPersistor} to save/load the value as a pair of producer and converter
 * factory identifiers.
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

    /**
     * Gets a unique string identifier for the given production path by serializing the production path settings to
     * JSON. This is the identifier handled by the {@link ProductionPathPersistor}.
     *
     * @param productionPath the production path
     * @param serializer the serializer to use to serialize the production path settings
     * @return the unique string identifier
     */
    public static String getPathIdentifier(final ProductionPath productionPath,
        final ProductionPathSerializer serializer) {
        final var settings = new NodeSettings("");
        serializer.saveProductionPath(productionPath, settings, "");
        return getPathIdentifier(settings);
    }

    /**
     * The reversal of {@link #getPathIdentifier(ProductionPath, ProductionPathSerializer)}.
     *
     * @param pathIdentifier the path identifier
     * @param serializer the serializer to use for loading the production path
     * @return the production path
     * @throws InvalidSettingsException if loading the production path failed
     */
    public static ProductionPath fromPathIdentifier(final String pathIdentifier,
        final ProductionPathSerializer serializer) throws InvalidSettingsException {
        final var settings = fromPathIdentifier(pathIdentifier);
        return serializer.loadProductionPath(settings, "");
    }

    /**
     * Checks whether the given string is a valid path identifier (which implicitly means that it is a node settings
     * JSON). Note: This method only is a basic heuristic check trying to identify JSON-serialized production paths.
     *
     * @param pathIdentifier the path identifier
     * @return true if valid, false otherwise
     */
    public static boolean isPathIdentifier(final String pathIdentifier) {
        var settings = new NodeSettings("");
        try {
            JSONConfig.readJSON(settings, new StringReader(pathIdentifier));
            if (settings.containsKey("_producer") && settings.containsKey("_converter")) {
                // seems to be a valid production path settings JSON
                return true;
            }
        } catch (IOException e) {
            // not even JSON
            return false;
        }
        return false;
    }

    private static String getPathIdentifier(final NodeSettingsRO productionPathSettings) {
        final var settingsWithoutKey = new NodeSettings("");
        productionPathSettings.copyTo(settingsWithoutKey);
        return JSONConfig.toJSONString(settingsWithoutKey, JSONConfig.WriterConfig.DEFAULT);
    }

    private static NodeSettingsRO fromPathIdentifier(final String pathIdentifier) {
        final var settings = new NodeSettings("");
        try {
            JSONConfig.readJSON(settings, new StringReader(pathIdentifier));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not deserialize path identifier", e);
        }
        return settings;
    }

    /**
     * A persistor that saves and loads a production path serialized to a single (JSON) string as mapped by
     * {@link #getPathIdentifier(ProductionPath, ProductionPathSerializer)} and {@link #fromPathIdentifier(String)}.
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
            return getPathIdentifier(pathSettings);
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            final NodeSettingsWO pathSettings = settings.addNodeSettings(m_configKey);
            fromPathIdentifier(param).copyTo(pathSettings);
        }

        @Override
        public String[][] getConfigPaths() {
            // no flow variables for production paths
            return new String[0][];
        }

    }

}
