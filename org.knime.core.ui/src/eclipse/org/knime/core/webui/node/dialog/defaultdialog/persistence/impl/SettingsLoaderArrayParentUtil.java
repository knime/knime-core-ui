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

import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverArrayParentUtil.IS_NULL;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;

/**
 * Utility class for loading array settings
 *
 * @author Paul Bärnreuther
 */
final class SettingsLoaderArrayParentUtil {

    private SettingsLoaderArrayParentUtil() {
        // Utility class: no instantiation
    }

    /**
     * IntFunction but allows for InvalidSettingsException
     */
    interface AtIndexLoader {

        /**
         * Loads the element at the given index.
         *
         * @param index the index of the element to load
         * @return the element at the given index
         * @throws InvalidSettingsException if the settings are invalid or the element cannot be loaded
         */
        Object apply(int index) throws InvalidSettingsException;

    }

    private static final Pattern IS_DIGIT = Pattern.compile("^\\d+$");

    static Object instantiateFromSettings(final ArrayParentNode<?> node, final NodeSettingsRO settings,
        final AtIndexLoader elementProvider) throws InvalidSettingsException {
        if (settings.getBoolean(IS_NULL, false)) {
            return null;
        }
        int size = countIndexedChildren(settings);
        return instantiateFromSize(node, elementProvider, size);
    }

    static Object instantiateFromSize(final ArrayParentNode<?> node, final AtIndexLoader elementProvider,
        final int size) throws InvalidSettingsException {
        return instantiateContainer(node.getRawClass(), node.getElementTree().getRawClass(), size, elementProvider);
    }

    static int countIndexedChildren(final ConfigBaseRO settings) {
        return (int)settings.keySet().stream().filter(IS_DIGIT.asPredicate()).count();
    }

    static Object instantiateContainer(final Class<?> rawType, final Class<?> elementType, final int size,
        final AtIndexLoader elementProvider) throws InvalidSettingsException {
        if (rawType.isArray()) {
            return buildArray(elementType, size, elementProvider);
        }
        return buildCollection(rawType, size, elementProvider);
    }

    private static Object[] buildArray(final Class<?> elementType, final int size, final AtIndexLoader elementProvider)
        throws InvalidSettingsException {
        final var array = instantiateArray(elementType, size);
        for (int i = 0; i < size; i++) {
            array[i] = elementProvider.apply(i);
        }
        return array;
    }

    private static Object[] instantiateArray(final Class<?> elementType, final int size) {
        return (Object[])Array.newInstance(elementType, size);
    }

    private static Collection<Object> buildCollection(final Class<?> type, final int size,
        final AtIndexLoader elementProvider) throws InvalidSettingsException {
        Collection<Object> collection = instantiateCollection(type, size);
        for (int i = 0; i < size; i++) {
            collection.add(elementProvider.apply(i));
        }
        return collection;
    }

    private static Collection<Object> instantiateCollection(final Class<?> type, final int size) {
        CheckUtils.checkState(List.class.equals(type) || Collection.class.equals(type) || ArrayList.class.equals(type),
            "Unsupported collection type: %s. Only Collection and List are supported.", type.getName());
        return new ArrayList<>(size);
    }
}
