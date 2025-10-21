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
 *   Oct 17, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.persistence;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.Persistor;

/**
 * Attach this to an array parameter to customize where elements are loaded from without providing a fully custom
 * {@link Persistor}. This makes sense, whenever a custom mechanism is needed and adding a {@link Persistor} would lead
 * to bad UX, since one can link certain fields in the arrays elements to certain input configs but all config keys
 * provided in the arrays global persistor would be displayed.
 *
 * @author Paul Bärnreuther
 * @param <L> a custom load context type that is supplied to the corresponding element loaders
 * @param <S> a custom save context type that is supplied to the corresponding element savers
 */
public interface ArrayPersistor<L, S> {

    /**
     * Get the length of the array stored in the given node settings.
     *
     * @param nodeSettings the node settings to load from
     * @return the length of the array
     * @throws InvalidSettingsException if the settings are invalid to retrieve the array length
     */
    int getArrayLength(NodeSettingsRO nodeSettings) throws InvalidSettingsException;

    /**
     * Create a load context for the element at the given index.
     *
     * @param index the index of the element
     * @return the load context
     */
    L createElementLoadContext(int index);

    /**
     * Create a save context for the element at the given index.
     *
     * @param index the index of the element
     * @return the save context
     */
    S createElementSaveDTO(int index);

    /**
     * Saves the given elements to the given node settings.
     *
     * @param savedElements the element dtos as constructed via {@link #createElementSaveDTO(int)} that have since been
     *            populated with the data to save by {@link ElementFieldPersistor#save(Object, Object)}
     * @param nodeSettings to save to
     */
    void save(List<S> savedElements, NodeSettingsWO nodeSettings);

}
