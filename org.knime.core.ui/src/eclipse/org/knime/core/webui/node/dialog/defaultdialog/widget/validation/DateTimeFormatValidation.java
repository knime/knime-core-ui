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
 *   23 Apr 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.validation;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;

/**
 * Specifies the built-in external validation for date&time formats of fields using the
 * {@link DateTimeFormatPickerWidget}.
 *
 * @param <T> the type of the settings field
 * @author Robin Gerling
 */
public abstract sealed class DateTimeFormatValidation<T> extends ExternalValidation<T> {

    /**
     *
     * @return the format to validate as string
     */
    abstract String getFormatAsString(final T currentFormat);

    @Override
    public final Optional<String> validate(final T currentFormat) {
        try {
            DateTimeFormatter.ofPattern(getFormatAsString(currentFormat));
        } catch (IllegalArgumentException ex) {
            return Optional.of("Invalid format: %s".formatted(ex.getMessage()));
        }
        return Optional.empty();
    }

    /**
     * The class used if the settings field uses the type {@link String}.
     */
    public static final class DateTimeStringFormatValidation extends DateTimeFormatValidation<String> {
        @Override
        String getFormatAsString(final String currentFormat) {
            return currentFormat;
        }
    }

    /**
     * The class used if the settings field uses the type {@link TemporalFormat}.
     *
     */
    public static final class DateTimeTemporalFormatValidation extends DateTimeFormatValidation<TemporalFormat> {
        @Override
        String getFormatAsString(final TemporalFormat currentFormat) {
            return currentFormat.format();
        }
    }

}
