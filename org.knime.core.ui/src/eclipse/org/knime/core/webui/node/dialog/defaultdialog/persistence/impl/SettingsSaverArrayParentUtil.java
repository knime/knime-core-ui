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
 *   Jun 5, 2025 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.persistence.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

final class SettingsSaverArrayParentUtil {

    private SettingsSaverArrayParentUtil() {
        // Utility class
    }

    /**
     * Interface for saving elements at a specific index in a container.
     */
    interface AtIndexSaver {

        /**
         * Saves the given element at the specified index in the container.
         *
         * @param index the index at which to save the element
         * @param element the element to save
         */
        void saveAtIndex(int index, Object element);
    }

    static void save(final Object container, final AtIndexSaver elementSaver) {
        if (container == null) {
            return;
        }
        if (container.getClass().isArray()) {
            saveArray((Object[])container, elementSaver);
        } else if (container instanceof Collection<?> collection) {
            saveCollection(collection, elementSaver);
        } else {
            throw new IllegalArgumentException("Unsupported container type for saving: " + container.getClass());
        }
    }

    private static void saveArray(final Object[] array, final AtIndexSaver saver) {
        iterateIndexed(array.length, i -> saver.saveAtIndex(i, array[i]));
    }

    private static void saveCollection(final Collection<?> collection, final AtIndexSaver saver) {
        Iterator<?> it = collection.iterator();
        iterateIndexed(collection.size(), i -> saver.saveAtIndex(i, it.next()));
    }

    private static void iterateIndexed(final int size, final IntConsumer indexedAction) {
        IntStream.range(0, size).forEach(indexedAction);
    }
}
