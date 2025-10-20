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
package org.knime.core.webui.node.dialog.defaultdialog.internal.widget.persistarray.examples;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;

/**
 * Example demonstrating the parallel arrays pattern for custom array persistence.
 *
 * <p>
 * This pattern is used when array elements should be persisted as parallel arrays, where each field of the element
 * class is stored in a separate array. This is useful for maintaining compatibility with existing settings formats that
 * use this structure, such as namespace prefix/URI pairs.
 * </p>
 *
 * <p>
 * Example settings structure:
 * </p>
 *
 * <pre>
 * prefixes: ["pre", "html"]
 * namespaces: ["http://example.org", "http://www.w3.org/1999/xhtml"]
 * </pre>
 *
 * @author Paul Bärnreuther
 */
public final class ParallelArraysExample {

    private ParallelArraysExample() {
        // Utility class
    }

    /**
     * Settings class containing an array field with custom parallel array persistence.
     */
    public static final class Settings implements NodeParameters {

        /**
         * Array field with custom parallel array persistence. Each element will be saved across multiple parallel
         * arrays.
         */
        @PersistArray(ParallelArrayPersistor.class)
        public Item[] m_items;
    }

    /**
     * Element class representing a single array item. Each field must be annotated with {@link PersistArrayElement} to
     * specify its custom persistor.
     */
    public static final class Item implements NodeParameters {

        @PersistArrayElement(PrefixPersistor.class)
        public String m_prefix;

        @PersistArrayElement(NamespacePersistor.class)
        public String m_namespace;
    }

    /**
     * DTO for saving array elements using parallel arrays. Element field persistors populate the fields in this DTO,
     * and the array persistor combines them into parallel arrays.
     */
    static final class ParallelArraySaveDTO {
        String m_prefix;

        String m_namespace;
    }

    /**
     * Array persistor that uses parallel arrays for array elements.
     */
    static final class ParallelArrayPersistor implements ArrayPersistor<Integer, ParallelArraySaveDTO> {

        private static final String PREFIXES = "prefixes";

        private static final String NAMESPACES = "namespaces";

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            return nodeSettings.getStringArray(PREFIXES, new String[0]).length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public ParallelArraySaveDTO createElementSaveDTO(final int index) {
            return new ParallelArraySaveDTO();
        }

        @Override
        public void save(final List<ParallelArraySaveDTO> savedElements, final NodeSettingsWO nodeSettings) {
            String[] prefixes = new String[savedElements.size()];
            String[] namespaces = new String[savedElements.size()];
            for (int i = 0; i < savedElements.size(); i++) {
                prefixes[i] = savedElements.get(i).m_prefix;
                namespaces[i] = savedElements.get(i).m_namespace;
            }
            nodeSettings.addStringArray(PREFIXES, prefixes);
            nodeSettings.addStringArray(NAMESPACES, namespaces);
        }
    }

    /**
     * Element field persistor for the prefix field that reads from and writes to a parallel array.
     */
    static final class PrefixPersistor implements ElementFieldPersistor<String, Integer, ParallelArraySaveDTO> {

        private static final String PREFIXES = "prefixes";

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            String[] prefixes = nodeSettings.getStringArray(PREFIXES, new String[0]);
            return loadContext < prefixes.length ? prefixes[loadContext] : "";
        }

        @Override
        public void save(final String param, final ParallelArraySaveDTO saveDTO) {
            saveDTO.m_prefix = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{PREFIXES}};
        }
    }

    /**
     * Element field persistor for the namespace field that reads from and writes to a parallel array.
     */
    static final class NamespacePersistor implements ElementFieldPersistor<String, Integer, ParallelArraySaveDTO> {

        private static final String NAMESPACES = "namespaces";

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            String[] namespaces = nodeSettings.getStringArray(NAMESPACES, new String[0]);
            return loadContext < namespaces.length ? namespaces[loadContext] : "";
        }

        @Override
        public void save(final String param, final ParallelArraySaveDTO saveDTO) {
            saveDTO.m_namespace = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NAMESPACES}};
        }
    }
}
