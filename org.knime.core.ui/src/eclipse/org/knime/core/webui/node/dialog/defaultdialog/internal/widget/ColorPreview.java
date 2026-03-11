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
 *   Mar 10, 2026 (gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.awt.Color;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.ColorJsonSerializationUtil;
import org.knime.node.parameters.updates.StateProvider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Displays a color preview widget for either a palette or a gradient.
 *
 * Note that because this is not a setting, no {@link org.knime.node.parameters.Widget @Widget} annotation should be
 * present on this field and the field type must be {@link Void}.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ColorPreview {

    /**
     * The state provider that computes the preview payload. The provided payload must be either a {@link Palette} or a
     * {@link Gradient}, and not {@code null}.
     *
     * @return the provider of the color preview.
     */
    Class<? extends StateProvider<? extends Preview>> value();

    /**
     * Marker interface for supported preview payloads.
     */
    sealed interface Preview permits Palette, Gradient {
    }

    /**
     * A palette preview represented by a sequence of colors.
     *
     * @param colors the palette colors
     */
    record Palette( //
        @JsonSerialize(contentUsing = ColorJsonSerializationUtil.ColorSerializer.class) //
        @JsonDeserialize(contentUsing = ColorJsonSerializationUtil.ColorDeserializer.class) //
        Color[] colors) implements Preview {
        public Palette {
            Objects.requireNonNull(colors, "Colors must not be null");
        }
    }

    /**
     * A gradient preview represented by colors and optional stop positions.
     *
     * @param colors color values of the gradient; must not be {@code null}
     * @param stops gradient stops, or {@code null} if the stops are evenly distributed; if not {@code null}, the array
     *            must have the same length as {@code colors}, all values must be in the range 0 to 100 inclusive, and
     *            the values must be in ascending order
     */
    record Gradient( //
        @JsonSerialize(contentUsing = ColorJsonSerializationUtil.ColorSerializer.class) //
        @JsonDeserialize(contentUsing = ColorJsonSerializationUtil.ColorDeserializer.class) //
        Color[] colors, //
        double[] stops) implements Preview {

        public Gradient {
            Objects.requireNonNull(colors, "Colors must not be null");
            if (stops != null) {
                if (stops.length != colors.length) {
                    throw new IllegalArgumentException("Stops must have the same length as colors.");
                }
                double previous = 0.0;
                for (int i = 0; i < stops.length; i++) {
                    double stop = stops[i];
                    if (stop < 0.0 || stop > 100.0) {
                        throw new IllegalArgumentException("Stops must be between 0 and 100 inclusive: " + stop);
                    }
                    if (i > 0 && stop < previous) {
                        throw new IllegalArgumentException("Stops must be in ascending order.");
                    }
                    previous = stop;
                }

            }
        }
    }

}
