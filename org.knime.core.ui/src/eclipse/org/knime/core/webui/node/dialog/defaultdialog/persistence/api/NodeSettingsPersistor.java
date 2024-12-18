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
 *   Feb 8, 2023 (Benjamin Wilhelm, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.api;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A persistor that is to be used within an {@link Persistor} annotation to define or refine the way a certain field or
 * class is persisted to {@link NodeSettings}.
 *
 * For an implementation to be used by the {@link Persist} annotation, it must have a non-private constructor that
 * either takes no arguments or a single {@link NodeSettingsPersistorContext} argument.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther, KNIME GmbH, Konstanz, Germany
 * @param <T> type of object loaded by the persistor
 */
public interface NodeSettingsPersistor<T> extends SettingsSaver<T>, SettingsLoader<T> {

    /**
     * Loads the object from the provided settings.
     *
     * @param settings to load from
     * @return the loaded object
     * @throws InvalidSettingsException if the settings are invalid
     */
    @Override
    T load(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the provided object into the settings.
     *
     * @param obj to save
     * @param settings to save into
     */
    @Override
    void save(T obj, NodeSettingsWO settings);

    /**
     * Each element in the array contains a path on how to get to the final nested config from here. E.g. if this
     * persistor saves to the config named "foo" with sub configs "bar" and "baz", the result here should be [["foo",
     * "bar"], ["foo", baz"]].
     *
     * @return an array of all config paths that are used to save the settings to the node settings or null if those
     *         should be inferred as if this persistor was not present.
     */
    String[][] getConfigPaths();

}
