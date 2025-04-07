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
 *   8 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import java.util.function.Function;

import org.knime.core.node.util.CheckUtils;

/**
 * Builder to harmonize the error messages for invalid column names.
 *
 * @author Robin Gerling
 */
public class ColumnNameValidationMessageBuilder {

    private String m_columnNameSettingIdentifier;

    private String m_arrayItemIdentifier;

    private ColumnNameSettingContext m_columnNameSettingContext = ColumnNameSettingContext.OUTSIDE_ARRAY_LAYOUT;

    /**
     * The value with which the setting responsible for the column name can be identified. Suitable is e.g. the name of
     * the settings (in lower case).
     *
     * @param columnNameSettingIdentifier the value with which the column name setting can be identified
     * @return the builder
     */
    public ColumnNameValidationMessageBuilder
        withColumnNameSettingIdentifier(final String columnNameSettingIdentifier) {
        m_columnNameSettingIdentifier = columnNameSettingIdentifier;
        return this;
    }

    /**
     * The value with which the item in the array layout can be identified. The value of a unique setting or the index
     * are suited for compact array layouts. The name of an array element is suited for non-compact array layouts.
     *
     * @param arrayItemIdentifier the value with which the array item can be identified
     * @return the builder
     */
    public ColumnNameValidationMessageBuilder withArrayItemIdentifier(final String arrayItemIdentifier) {
        m_arrayItemIdentifier = arrayItemIdentifier;
        return this;
    }

    /**
     * The context determines the structure of the message. Defaults to
     * {@link ColumnNameSettingContext#OUTSIDE_ARRAY_LAYOUT}.
     *
     * @param columnNameSettingContext the context in which the settings is used
     * @return the builder
     */
    public ColumnNameValidationMessageBuilder
        withSpecificSettingContext(final ColumnNameSettingContext columnNameSettingContext) {
        m_columnNameSettingContext = columnNameSettingContext;
        return this;
    }

    private String createTemplate() {
        switch (m_columnNameSettingContext) {
            case OUTSIDE_ARRAY_LAYOUT:
                return m_columnNameSettingIdentifier;
            case INSIDE_COMPACT_ARRAY_LAYOUT:
                CheckUtils.checkArgumentNotNull(m_arrayItemIdentifier,
                    "The array item identifier cannot be null when the setting is inside a compact array layout.");
                return String.format("%s for \"%s\"", m_columnNameSettingIdentifier, m_arrayItemIdentifier);
            case INSIDE_NON_COMPACT_ARRAY_LAYOUT:
                CheckUtils.checkArgumentNotNull(m_arrayItemIdentifier,
                    "The array item identifier cannot be null when the setting is inside a non-compact array layout.");
                return String.format("%s of \"%s\"", m_columnNameSettingIdentifier, m_arrayItemIdentifier);
            default:
                throw new IllegalArgumentException(
                    String.format("Unexpected value: \"%s\"", m_columnNameSettingContext));
        }
    }

    /**
     * Creates the function that maps the different invalid states to the corresponding error message based on the
     * specified parameters.
     *
     * @return the function which is used during the validation to get the error message
     */
    public Function<InvalidColumnNameState, String> build() {
        CheckUtils.checkArgumentNotNull(m_columnNameSettingIdentifier, "The settings identifier must not be null.");
        final var template = createTemplate();
        return icns -> switch (icns) {
            case EMPTY -> String.format("The %s must not be empty.", template);
            case BLANK -> String.format("The %s must not be blank.", template);
            case NOT_TRIMMED -> String.format("The %s must start and end with a non-whitespace character.", template);
            default -> throw new IllegalArgumentException(String.format("Unexpected value: \"%s\"", icns));
        };
    }

    /**
     * Specifies the different states of the context of a column name setting.
     */
    public enum ColumnNameSettingContext {
            /**
             * Column name setting is outside of an array layout
             */
            OUTSIDE_ARRAY_LAYOUT,
            /**
             * Column name setting is inside a compact array layout
             */
            INSIDE_COMPACT_ARRAY_LAYOUT,
            /**
             * Column name setting is inside a non-compact/card array layout
             */
            INSIDE_NON_COMPACT_ARRAY_LAYOUT
    }

    /**
     * Specifies the different states of an invalid column name.
     */
    public enum InvalidColumnNameState {
            /**
             * column name is null/empty
             */
            EMPTY,
            /**
             * column name is blank, but not null/empty
             */
            BLANK,
            /**
             * column name starts and/or ends with whitespace
             */
            NOT_TRIMMED
    }

}
