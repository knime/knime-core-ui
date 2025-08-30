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
 *   Oct 17, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.util;

import static org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil.mapValues;
import static org.knime.core.webui.node.dialog.defaultdialog.util.MapValuesUtil.mapValuesWithKeys;

import java.util.Comparator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.webui.node.dialog.SettingsType;

/**
 * Some nodes rely on view settings within the same section as model settings appear beneath those. So any map used to
 * store settings should be sorted by {@link SettingsType}.
 */
public final class SettingsTypeMapUtil {

    private static final Comparator<SettingsType> SETTINGS_TYPE_COMPARATOR =
        Comparator.comparing(SettingsType::ordinal);

    /**
     * Transform values using keys and sort keys by {@link SettingsType}.
     *
     * @param <T> values before mapping
     * @param <U> values after mapping
     * @param inMap the map the mapping should be applied to
     * @param mapping to be applied to the values
     * @return a new map with the same keys and mapped values
     */
    public static <T, U> Map<SettingsType, U> map(final Map<SettingsType, T> inMap,
        final BiFunction<SettingsType, T, U> mapping) {
        return mapValuesWithKeys(inMap, mapping, SETTINGS_TYPE_COMPARATOR);
    }

    /**
     * Keep only node-specific settings and sort keys by {@link SettingsType}.
     *
     * @param <T> values
     * @param inMap the map the filtering should be applied to
     * @return a new map with the same keys and values that are node-specific
     */
    public static <T> Map<SettingsType, T> keepNodeSpecificSettings(final Map<SettingsType, T> inMap) {
        final var filteredMap = inMap.entrySet().stream().filter(entry -> entry.getKey().isNodeSpecific())
            .collect(Collectors.toMap( //
                Map.Entry::getKey, //
                Map.Entry::getValue //
            ));
        return mapValues(filteredMap, Function.identity()); // sort
    }

    /**
     * Transform values and sort keys by {@link SettingsType}.
     *
     * @param <T> values before mapping
     * @param <U> values after mapping
     * @param inMap the map the mapping should be applied to
     * @param mapping to be applied to the entries
     * @return a new map with the same keys and mapped values
     */
    public static <T, U> Map<SettingsType, U> map(final Map<SettingsType, T> inMap, final Function<T, U> mapping) {
        return mapValues(inMap, mapping, SETTINGS_TYPE_COMPARATOR);
    }

    /**
     * Restrict values and sort keys by {@link SettingsType}.
     *
     * @param <T> values before
     * @param <U> values after
     * @param inMap the map the restriction should be applied to
     * @return a new map with the same keys and mapped values
     */
    public static <T extends U, U> Map<SettingsType, U> map(final Map<SettingsType, T> inMap) {
        return map(inMap, t -> t);
    }

    private SettingsTypeMapUtil() {
        // utility class
    }
}
