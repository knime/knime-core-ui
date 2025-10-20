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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;

/**
 * Example demonstrating the indexed config keys pattern for custom array persistence.
 *
 * <p>
 * This pattern is used when array elements should be persisted with config keys that include the element's index, such
 * as "value0", "value1", etc. This is useful for maintaining compatibility with existing settings formats that use
 * indexed keys rather than array structures.
 * </p>
 *
 * <p>
 * Example settings structure:
 * </p>
 *
 * <pre>
 * numberOfItems: 2
 * value0: "first"
 * number0: 42
 * value1: "second"
 * number1: 99
 * </pre>
 *
 * @author Paul Bärnreuther
 */
public final class IndexedConfigKeysExample {

    private IndexedConfigKeysExample() {
        // Utility class
    }

    /**
     * Settings class containing an array field with custom indexed persistence.
     */
    public static final class Settings implements NodeParameters {

        /**
         * Array field with custom indexed persistence. Each element will be saved with indexed config keys.
         */
        @PersistArray(IndexedArrayPersistor.class)
        public Item[] m_items;
    }

    /**
     * Element class representing a single array item. Each field must be annotated with {@link PersistArrayElement} to
     * specify its custom persistor.
     */
    public static final class Item implements NodeParameters {

        @PersistArrayElement(ValuePersistor.class)
        public String m_value;

        @PersistArrayElement(NumberPersistor.class)
        public int m_number;
    }

    /**
     * DTO for saving array elements with indexed config keys. Provides access to the element's index and allows element
     * field persistors to add settings modifications.
     */
    static final class IndexedSaveDTO {
        private final int m_index;

        private final List<Consumer<NodeSettingsWO>> m_nodeSettingsConsumers = new ArrayList<>();

        IndexedSaveDTO(final int index) {
            m_index = index;
        }

        int getIndex() {
            return m_index;
        }

        void addNodeSettingsModification(final Consumer<NodeSettingsWO> consumer) {
            m_nodeSettingsConsumers.add(consumer);
        }

        List<Consumer<NodeSettingsWO>> getNodeSettingsConsumers() {
            return m_nodeSettingsConsumers;
        }
    }

    /**
     * Array persistor that uses indexed config keys for array elements.
     */
    static final class IndexedArrayPersistor implements ArrayPersistor<Integer, IndexedSaveDTO> {

        private static final String NUMBER_OF_ITEMS = "numberOfItems";

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            return nodeSettings.getInt(NUMBER_OF_ITEMS, 0);
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public IndexedSaveDTO createElementSaveDTO(final int index) {
            return new IndexedSaveDTO(index);
        }

        @Override
        public void save(final List<IndexedSaveDTO> savedElements, final NodeSettingsWO nodeSettings) {
            nodeSettings.addInt(NUMBER_OF_ITEMS, savedElements.size());
            for (IndexedSaveDTO saveDTO : savedElements) {
                for (Consumer<NodeSettingsWO> consumer : saveDTO.getNodeSettingsConsumers()) {
                    consumer.accept(nodeSettings);
                }
            }
        }
    }

    /**
     * Element field persistor for the string value field using indexed config keys.
     */
    static final class ValuePersistor implements ElementFieldPersistor<String, Integer, IndexedSaveDTO> {

        private static final String VALUE_KEY = "value";

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            return nodeSettings.getString(VALUE_KEY + loadContext);
        }

        @Override
        public void save(final String param, final IndexedSaveDTO saveDTO) {
            final var index = saveDTO.getIndex();
            saveDTO.addNodeSettingsModification(settings -> settings.addString(VALUE_KEY + index, param));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{VALUE_KEY + ARRAY_INDEX_PLACEHOLDER}};
        }
    }

    /**
     * Element field persistor for the integer number field using indexed config keys.
     */
    static final class NumberPersistor implements ElementFieldPersistor<Integer, Integer, IndexedSaveDTO> {

        private static final String NUMBER_KEY = "number";

        @Override
        public Integer load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            return nodeSettings.getInt(NUMBER_KEY + loadContext);
        }

        @Override
        public void save(final Integer param, final IndexedSaveDTO saveDTO) {
            final var index = saveDTO.getIndex();
            saveDTO.addNodeSettingsModification(settings -> settings.addInt(NUMBER_KEY + index, param));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NUMBER_KEY + ARRAY_INDEX_PLACEHOLDER}};
        }
    }
}
