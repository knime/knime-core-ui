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
 *   15 Dec 2022 Paul Bärnreuther: created
 */

package org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes;

import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.util.PatternFilter.PatternMode;

/**
 * This enum lists the possibilities of how to choose from a set of table columns
 *
 * @author Paul Bärnreuther
 */
public enum TypedStringFilterMode {
        /**
         * manual selection, i.e. a stored list of manually selected columns
         */
        MANUAL,
        /**
         * selection by matching to a regex pattern
         */
        REGEX,
        /**
         * selection by matching to a wildcard pattern
         */
        WILDCARD,
        /**
         * selection by filtering by the types of the table columns with respect to a list of selected types
         */
        TYPE;

    /**
     * Transforms this enum to a {@link PatternMode}. Check that a pattern is used before using this method.
     *
     * @return the corresponding {@link PatternMode}
     */
    public PatternMode toPatternMode() {
        return this == REGEX ? PatternMode.REGEX : PatternMode.WILDCARD;
    }

    /**
     * Converts a {@link PatternMode} to a {@link TypedStringFilterMode}
     *
     * @param mode the pattern mode
     * @return the corresponding {@link TypedStringFilterMode}
     */
    public static TypedStringFilterMode toTypedNameFilterMode(final PatternMode mode) {
        return mode == PatternMode.REGEX ? REGEX : WILDCARD;
    }

}
