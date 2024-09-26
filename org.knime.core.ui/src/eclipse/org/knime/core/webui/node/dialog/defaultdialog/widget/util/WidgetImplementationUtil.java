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
 *   May 5, 2023 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.util;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.NameFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.Credentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.credentials.LegacyCredentials;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComboBoxWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RichTextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextMessage;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.CredentialsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.PasswordWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.credentials.UsernameWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * This utility class defines defaults and registers additional annotations used to define the format of an ui element.
 *
 * @author Paul Bärnreuther
 */
public final class WidgetImplementationUtil {

    private WidgetImplementationUtil() {
        // Utility class
    }

    /**
     *
     * @author Paul Bärnreuther
     * @param applicableFields a list of classes one of which needs to be assignable from the annotated field
     * @param widgetAnnotation the class of the annotation
     */
    public record WidgetAnnotation(List<Class<?>> applicableFields, Class<? extends Annotation> widgetAnnotation) {

        /**
         * @param widgetAnnotation an annotation which is applicable to all types of fields
         */
        public WidgetAnnotation(final Class<? extends Annotation> widgetAnnotation) {
            this(null, widgetAnnotation);
        }
    }

    /**
     * Extend this by a new element for each new default format of a ui element.
     *
     * !!! WHEN ADDING A NEW ELEMENT HERE, ALSO ADD TO THE DOCUMENTATION OF {@link DefaultNodeSettings} !!!
     *
     * @author Paul Bärnreuther
     */
    @SuppressWarnings("javadoc")
    public enum DefaultWidgetType {
            CHECKBOX, COLUMN_FILTER, NAME_FILTER, COLUMN_SELECTION, LOCAL_DATE, STRING_ARRAY, CREDENTIALS,
            LEGACY_CREDENTIALS, FILE_CHOOSER, DYNAMIC_VALUE
    }

    /**
     * @param applicableFields a list of classes one of which needs to be assignable from the annotated field
     * @param type a {@link DefaultWidgetType} used by the implementation to identify the default
     */
    public record DefaultWidget(List<Class<?>> applicableFields, DefaultWidgetType type) {
    }

    /**
     * Extend this by every new annotation defining the format of the annotated ui element.
     *
     * !!! WHEN ADDING A NEW ELEMENT HERE, ALSO ADD TO THE DOCUMENTATION OF {@link DefaultNodeSettings} !!!
     */
    private static WidgetAnnotation[] widgetAnnotations = new WidgetAnnotation[]{//
        new WidgetAnnotation(Widget.class), //
        new WidgetAnnotation(List.of(Enum.class), RadioButtonsWidget.class), //
        new WidgetAnnotation(List.of(Enum.class), ValueSwitchWidget.class), //
        new WidgetAnnotation(ChoicesWidget.class), //
        new WidgetAnnotation(List.of(String[].class), ComboBoxWidget.class), //
        new WidgetAnnotation(List.of(String[].class), SortListWidget.class), //
        new WidgetAnnotation(ButtonWidget.class), //
        new WidgetAnnotation(List.of(String.class), DateTimeWidget.class), //
        new WidgetAnnotation(List.of(LocalDate.class), DateWidget.class), //
        new WidgetAnnotation(List.of(String.class), RichTextInputWidget.class), //
        new WidgetAnnotation(List.of(Credentials.class, LegacyCredentials.class), CredentialsWidget.class), //
        new WidgetAnnotation(List.of(Credentials.class, LegacyCredentials.class), PasswordWidget.class), //
        new WidgetAnnotation(List.of(Credentials.class, LegacyCredentials.class), UsernameWidget.class), //
        new WidgetAnnotation(List.of(FileChooser.class), FileReaderWidget.class), //
        new WidgetAnnotation(List.of(FileChooser.class), FileWriterWidget.class), //
        new WidgetAnnotation(List.of(String.class), LocalFileReaderWidget.class), //
        new WidgetAnnotation(List.of(String.class), LocalFileWriterWidget.class), //
        new WidgetAnnotation(List.of(String.class), TextInputWidget.class), //
        new WidgetAnnotation(List.of(Void.class), SimpleButtonWidget.class), //
        new WidgetAnnotation(List.of(Void.class), TextMessage.class)
    };

    /**
     *
     * Extend this for every fields type which has default format set. !!! WHEN ADDING A NEW ELEMENT HERE, ALSO ADD TO
     * THE DOCUMENTATION OF {@link DefaultNodeSettings} !!!
     */
    private static DefaultWidget[] defaultWidgets = new DefaultWidget[]{//
        new DefaultWidget(List.of(boolean.class, Boolean.class), DefaultWidgetType.CHECKBOX), //
        new DefaultWidget(List.of(ColumnFilter.class), DefaultWidgetType.COLUMN_FILTER), //
        new DefaultWidget(List.of(NameFilter.class), DefaultWidgetType.NAME_FILTER), //
        new DefaultWidget(List.of(ColumnSelection.class), DefaultWidgetType.COLUMN_SELECTION), //
        new DefaultWidget(List.of(LocalDate.class), DefaultWidgetType.LOCAL_DATE), //
        new DefaultWidget(List.of(String[].class), DefaultWidgetType.STRING_ARRAY), //
        new DefaultWidget(List.of(Credentials.class), DefaultWidgetType.CREDENTIALS), //
        new DefaultWidget(List.of(LegacyCredentials.class), DefaultWidgetType.LEGACY_CREDENTIALS), //
        new DefaultWidget(List.of(FileChooser.class), DefaultWidgetType.FILE_CHOOSER), //
        new DefaultWidget(List.of(DynamicValuesInput.class), DefaultWidgetType.DYNAMIC_VALUE)//
    };

    /**
     * @param annotationIsPresent a function returning whether an annotation is present (on a given field)
     * @param fieldType the type of the annotated field
     * @return a partition of the present widget annotations by whether they are applicable
     */
    public static Map<Boolean, List<WidgetAnnotation>> partitionWidgetAnnotationsByApplicability(
        final Predicate<Class<? extends Annotation>> annotationIsPresent, final Class<?> fieldType) {
        return getPresentWidgetAnnotations(annotationIsPresent).stream().collect(
            Collectors.partitioningBy(widgetAnnotation -> isApplicable(fieldType, widgetAnnotation.applicableFields)));
    }

    /**
     * @param fieldType the type of the field for which the defaults are to be collected
     * @return the default widget formats
     */
    public static List<DefaultWidget> getApplicableDefaults(final Class<?> fieldType) {
        return Arrays.asList(defaultWidgets).stream()
            .filter(defaultWidget -> isApplicable(fieldType, defaultWidget.applicableFields)).toList();
    }

    private static List<WidgetAnnotation>
        getPresentWidgetAnnotations(final Predicate<Class<? extends Annotation>> annotationIsPresent) {
        return Arrays.asList(widgetAnnotations).stream().filter(ann -> annotationIsPresent.test(ann.widgetAnnotation))
            .toList();
    }

    private static boolean isApplicable(final Class<?> fieldType, final List<Class<?>> applicableFields) {
        if (applicableFields == null) {
            return true;
        }
        return applicableFields.stream().anyMatch(field -> field.isAssignableFrom(fieldType));
    }
}
