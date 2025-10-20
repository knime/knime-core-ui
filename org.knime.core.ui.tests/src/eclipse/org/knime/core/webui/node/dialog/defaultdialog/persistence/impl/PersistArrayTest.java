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
 *   Dec 1, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.saveSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;

/**
 * Tests for the {@link FieldBasedNodeSettingsPersistor}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"javadoc", "java:S2698"})
public class PersistArrayTest {

    private static final String ROOT_KEY = "Test";

    // ===== Pattern 1: Array with indexed config keys =====

    @Test
    void testPersistArrayConfigKeysWithIndex() throws InvalidSettingsException {
        var obj = new ArrayConfigKeysObject();
        obj.m_items = new ItemWithIndexedKeys[2];

        obj.m_items[0] = new ItemWithIndexedKeys();
        obj.m_items[0].m_value = "first";
        obj.m_items[0].m_number = 42;

        obj.m_items[1] = new ItemWithIndexedKeys();
        obj.m_items[1].m_value = "second";
        obj.m_items[1].m_number = 99;

        testSaveLoad(obj);
    }

    public static final class ArrayConfigKeysObject extends AbstractTestNodeSettings<ArrayConfigKeysObject> {

        @PersistArray(IndexedArrayPersistor.class)
        ItemWithIndexedKeys[] m_items;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            if (m_items != null) {
                settings.addInt("numberOfItems", m_items.length);
                for (int i = 0; i < m_items.length; i++) {
                    settings.addString("value" + i, m_items[i].m_value);
                    settings.addInt("number" + i, m_items[i].m_number);
                }
            } else {
                settings.addInt("numberOfItems", 0);
            }
        }

        @Override
        protected boolean equalSettings(final ArrayConfigKeysObject settings) {
            if (m_items == null && settings.m_items == null) {
                return true;
            }
            if (m_items == null || settings.m_items == null) {
                return false;
            }
            if (m_items.length != settings.m_items.length) {
                return false;
            }
            for (int i = 0; i < m_items.length; i++) {
                if (!m_items[i].equals(settings.m_items[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected int computeHashCode() {
            return Arrays.hashCode(m_items);
        }
    }

    static final class ItemWithIndexedKeys implements NodeParameters {

        @PersistArrayElement(IndexedValuePersistor.class)
        String m_value;

        @PersistArrayElement(IndexedNumberPersistor.class)
        int m_number;

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ItemWithIndexedKeys other = (ItemWithIndexedKeys)obj;
            return m_number == other.m_number && Objects.equals(m_value, other.m_value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_value, m_number);
        }
    }

    static final class IndexedSaveDTO {
        private final int m_index;

        private List<Consumer<NodeSettingsWO>> nodeSettingsConsumers = new ArrayList<>();

        IndexedSaveDTO(final int index) {
            m_index = index;
        }

        int getIndex() {
            return m_index;
        }

        void addNodeSettingsModification(final Consumer<NodeSettingsWO> consumer) {
            getNodeSettingsConsumers().add(consumer);
        }

        List<Consumer<NodeSettingsWO>> getNodeSettingsConsumers() {
            return nodeSettingsConsumers;
        }

    }

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

    static final class IndexedValuePersistor implements ElementFieldPersistor<String, Integer, IndexedSaveDTO> {

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

    static final class IndexedNumberPersistor implements ElementFieldPersistor<Integer, Integer, IndexedSaveDTO> {

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

    // ===== Pattern 2: Array with parallel array configs =====

    @Test
    void testPersistArrayWithArrayConfigs() throws InvalidSettingsException {
        var obj = new ArrayConfigsObject();
        obj.m_items = new ItemWithArrayConfigs[2];

        obj.m_items[0] = new ItemWithArrayConfigs();
        obj.m_items[0].m_prefix = "pre";
        obj.m_items[0].m_namespace = "http://example.org";

        obj.m_items[1] = new ItemWithArrayConfigs();
        obj.m_items[1].m_prefix = "html";
        obj.m_items[1].m_namespace = "http://www.w3.org/1999/xhtml";

        testSaveLoad(obj);
    }

    public static final class ArrayConfigsObject extends AbstractTestNodeSettings<ArrayConfigsObject> {

        @PersistArray(ParallelArrayPersistor.class)
        ItemWithArrayConfigs[] m_items;

        @Override
        public void saveExpected(final NodeSettingsWO settings) {
            if (m_items != null) {
                String[] prefixes = new String[m_items.length];
                String[] namespaces = new String[m_items.length];
                for (int i = 0; i < m_items.length; i++) {
                    prefixes[i] = m_items[i].m_prefix;
                    namespaces[i] = m_items[i].m_namespace;
                }
                settings.addStringArray("prefixes", prefixes);
                settings.addStringArray("namespaces", namespaces);
            } else {
                settings.addStringArray("prefixes", new String[0]);
                settings.addStringArray("namespaces", new String[0]);
            }
        }

        @Override
        protected boolean equalSettings(final ArrayConfigsObject settings) {
            if (m_items == null && settings.m_items == null) {
                return true;
            }
            if (m_items == null || settings.m_items == null) {
                return false;
            }
            if (m_items.length != settings.m_items.length) {
                return false;
            }
            for (int i = 0; i < m_items.length; i++) {
                if (!m_items[i].equals(settings.m_items[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected int computeHashCode() {
            return Arrays.hashCode(m_items);
        }
    }

    static final class ItemWithArrayConfigs implements NodeParameters {

        @PersistArrayElement(PrefixPersistor.class)
        String m_prefix;

        @PersistArrayElement(NamespacePersistor.class)
        String m_namespace;

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ItemWithArrayConfigs other = (ItemWithArrayConfigs)obj;
            return Objects.equals(m_prefix, other.m_prefix) && Objects.equals(m_namespace, other.m_namespace);
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_prefix, m_namespace);
        }
    }

    static final class ArraySaveDTO {
        String m_prefix;

        String m_namespace;
    }

    static final class ParallelArrayPersistor implements ArrayPersistor<Integer, ArraySaveDTO> {

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
        public ArraySaveDTO createElementSaveDTO(final int index) {
            return new ArraySaveDTO();
        }

        @Override
        public void save(final List<ArraySaveDTO> savedElements, final NodeSettingsWO nodeSettings) {
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

    static final class PrefixPersistor implements ElementFieldPersistor<String, Integer, ArraySaveDTO> {

        private static final String PREFIXES = "prefixes";

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            String[] prefixes = nodeSettings.getStringArray(PREFIXES, new String[0]);
            return loadContext < prefixes.length ? prefixes[loadContext] : "";
        }

        @Override
        public void save(final String param, final ArraySaveDTO saveDTO) {
            saveDTO.m_prefix = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{PREFIXES}};
        }
    }

    static final class NamespacePersistor implements ElementFieldPersistor<String, Integer, ArraySaveDTO> {

        private static final String NAMESPACES = "namespaces";

        @Override
        public String load(final NodeSettingsRO nodeSettings, final Integer loadContext)
            throws InvalidSettingsException {
            String[] namespaces = nodeSettings.getStringArray(NAMESPACES, new String[0]);
            return loadContext < namespaces.length ? namespaces[loadContext] : "";
        }

        @Override
        public void save(final String param, final ArraySaveDTO saveDTO) {
            saveDTO.m_namespace = param;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{NAMESPACES}};
        }
    }

    private static <S extends TestNodeSettings> void testSaveLoad(final S obj) throws InvalidSettingsException {
        var expected = new NodeSettings(ROOT_KEY);
        obj.saveExpected(expected);

        var actual = new NodeSettings(ROOT_KEY);
        saveSettings(obj, actual);

        assertEquals(expected, actual, "The settings saved by the persistor are not as expected.");

        var loaded = loadSettings(obj.getClass(), expected);

        assertEquals(obj, loaded, "The loaded settings are not as expected");
    }

    private interface TestNodeSettings extends NodeParameters {

        void saveExpected(final NodeSettingsWO settings);

        @Override
        boolean equals(Object obj);

        @Override
        int hashCode();
    }

    private abstract static class AbstractTestNodeSettings<S extends AbstractTestNodeSettings<S>>
        implements TestNodeSettings {

        @SuppressWarnings("unchecked")
        @Override
        public final boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass().equals(obj.getClass())) {
                return equalSettings((S)obj);
            } else {
                return false;
            }
        }

        @Override
        public final int hashCode() {
            return computeHashCode();
        }

        protected abstract int computeHashCode();

        protected abstract boolean equalSettings(final S settings);
    }

    @Test
    void testThrowsIfElementIsMissingPersistArrayElementAnnotation() {
        final var obj = new MissingPersistArrayElementAnnotationArrayObject();
        final var nodeSettings = new NodeSettings(ROOT_KEY);
        assertThat(assertThrows(IllegalStateException.class, () -> SettingsSaverFactory.saveSettings(obj, nodeSettings))
            .getMessage()).contains("PersistArrayElement", "value");
    }

    static final class MissingPersistArrayElementAnnotationArrayPersistor implements ArrayPersistor<Integer, Object> {

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) {
            return 0;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public Object createElementSaveDTO(final int index) {
            return null;
        }

        @Override
        public void save(final List<Object> savedElements, final NodeSettingsWO nodeSettings) {
            // do nothing
        }
    }

    static final class ItemWithMissingPersistArrayElementAnnotation implements NodeParameters {
        String m_value;
    }

    static final class MissingPersistArrayElementAnnotationArrayObject implements NodeParameters {

        @PersistArray(MissingPersistArrayElementAnnotationArrayPersistor.class)
        ItemWithMissingPersistArrayElementAnnotation[] m_items;

    }

}