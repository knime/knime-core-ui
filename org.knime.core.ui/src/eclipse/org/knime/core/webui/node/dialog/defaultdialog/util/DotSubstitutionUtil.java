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
 *  ECLIPSE with only the GPL license terms applicable to KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you may use Nodes as
 *  part of a combined work, and you may copy and distribute such Nodes
 *  along with the combined work.
 *
 *  Modified or added to by University of Konstanz, Germany and KNIME AG
 *
 *  KNIME and ECLIPSE are both independent programs and are not derived from
 *  each other. Should, however, the interpretation of the GNU GPL Version 3
 *  ("License") under any applicable laws result in KNIME and ECLIPSE being a
 *  combined program, KNIME AG herewith grants you the additional permission
 *  to use and propagate KNIME together with ECLIPSE with only the GPL
 *  license terms applicable to KNIME, provided the license terms of ECLIPSE
 *  themselves allow for the respective use and propagation of ECLIPSE
 *  together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you may use Nodes as
 *  part of a combined work, and you may copy and distribute such Nodes
 *  along with the combined work.
 *
 *  KNIME GmbH, Konstanz, Germany
 */
package org.knime.core.webui.node.dialog.defaultdialog.util;

import java.util.Arrays;

/**
 * Utility class for handling dot substitution in path segments for JSON forms.
 *
 * Dots have a special meaning in JSON forms (they denote sub-objects), so this utility provides methods to substitute
 * dots with placeholders and vice versa.
 *
 * @author Paul Bärnreuther
 */
public final class DotSubstitutionUtil {

    private static final String DOT_PLACEHOLDER = "<dot>";

    private DotSubstitutionUtil() {
        // utility class
    }

    /**
     * Replaces dots in path segments with placeholders since dots have a special meaning in JSON forms (they denote
     * sub-objects).
     *
     * @param path array of path segments with potential dots
     * @return array of path segments with dots replaced by placeholders
     */
    public static String[] substituteDots(final String[] path) {
        return Arrays.stream(path).map(segment -> segment.replace(".", DOT_PLACEHOLDER)).toArray(String[]::new);
    }

    /**
     * Replaces dot placeholders back with actual dots.
     *
     * @param path array of path segments with dot placeholders
     * @return array of path segments with placeholders replaced by dots
     */
    public static String[] deSubstituteDots(final String[] path) {
        return Arrays.stream(path).map(segment -> segment.replace(DOT_PLACEHOLDER, ".")).toArray(String[]::new);
    }

    /**
     * Replaces dots in a single path segment with placeholders.
     *
     * @param segment path segment with potential dots
     * @return path segment with dots replaced by placeholders
     */
    public static String substituteDots(final String segment) {
        return segment.replace(".", DOT_PLACEHOLDER);
    }

    /**
     * Replaces dot placeholders in a single path segment back with actual dots.
     *
     * @param segment path segment with dot placeholders
     * @return path segment with placeholders replaced by dots
     */
    public static String deSubstituteDots(final String segment) {
        return segment.replace(DOT_PLACEHOLDER, ".");
    }
}
