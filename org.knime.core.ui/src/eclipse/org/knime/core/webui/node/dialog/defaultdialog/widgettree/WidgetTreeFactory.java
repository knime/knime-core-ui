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
 *   Oct 7, 2024 (Paul Bärnreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widgettree;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.dataservice.dbtablechooser.DBTableChooserDataService.DBTableAdapterProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.ButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicSettingsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.CustomFileConnectionFolderReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FolderSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.CredentialsWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.OverwriteDialogTitleInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.RichTextInputWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.tree.ArrayParentNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.Tree;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeFactory;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.credentials.CredentialsWidget;
import org.knime.node.parameters.widget.credentials.PasswordWidget;
import org.knime.node.parameters.widget.credentials.UsernameWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.RichTextInputWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * A factory for creating {@link Tree}s from {@link WidgetGroup}s.
 *
 * @author Paul Bärnreuther
 */
public class WidgetTreeFactory extends TreeFactory<WidgetGroup> {

    private static final Collection<Class<? extends Annotation>> POSSIBLE_TREE_ANNOTATIONS = List.of( //
        Layout.class, //
        Effect.class, //
        Advanced.class, //
        /*
         * Since {@link MultiFileSelection} is a {@link WidgetGroup}.
         */
        FileReaderWidget.class, //
        ValueReference.class, //
        ValueProvider.class, //
        Modification.class, //
        Modification.WidgetReference.class, //
        DBTableAdapterProvider.class, //
        DynamicParameters.class //
    );

    private static final Collection<ClassAnnotationSpec> POSSIBLE_TREE_CLASS_ANNOTATIONS = List.of( //
        new ClassAnnotationSpec(Layout.class, false), //
        new ClassAnnotationSpec(Effect.class), //
        new ClassAnnotationSpec(Advanced.class), //
        new ClassAnnotationSpec(Modification.class) //
    );

    private static final Collection<Class<? extends Annotation>> POSSIBLE_LEAF_ANNOTATIONS = List.of(//
        Advanced.class, //
        ButtonWidget.class, //
        ChoicesProvider.class, //
        ColumnFilterWidget.class, //
        CredentialsWidget.class, //
        CredentialsWidgetInternal.class, //
        CustomFileConnectionFolderReaderWidget.class, //
        CustomValidation.class, //
        DateTimeFormatPickerWidget.class, //
        Effect.class, //
        FileReaderWidget.class, //
        FileWriterWidget.class, //
        FolderSelectionWidget.class, //
        FlowVariableFilterWidget.class, //
        ArrayWidgetInternal.ElementCheckboxWidget.class, //
        IntervalWidget.class, //
        Layout.class, //
        LocalFileReaderWidget.class, //
        LocalFileWriterWidget.class, //
        Modification.WidgetReference.class, //
        NumberInputWidget.class, //
        OptionalWidget.class, //
        OverwriteDialogTitleInternal.class, //
        PasswordWidget.class, //
        RadioButtonsWidget.class, //
        RichTextInputWidget.class, //
        RichTextInputWidgetInternal.class, //
        SimpleButtonWidget.class, //
        SortListWidget.class, //
        SubParameters.class, //
        TextAreaWidget.class, //
        TextInputWidget.class, //
        TextMessage.class, //
        TwinlistWidget.class, //
        TypedStringFilterWidgetInternal.class, //
        UsernameWidget.class, //
        ValueProvider.class, //
        ValueReference.class, //
        ValueSwitchWidget.class, //
        Widget.class, //
        WidgetInternal.class, //
        DynamicSettingsWidget.class//
    );

    private static final Collection<Class<? extends Annotation>> POSSIBLE_ARRAY_ANNOTATIONS = List.of(//
        Advanced.class, //
        ArrayWidget.class, //
        Effect.class, //
        ArrayWidgetInternal.class, //
        Layout.class, //
        Modification.class, //
        Modification.WidgetReference.class, //
        SubParameters.class, //
        ValueProvider.class, //
        ValueReference.class, //
        Widget.class //
    );

    /**
     * Create a new factory. This factory is non-static since it implements an abstract factory, but it does not hold
     * any state.
     */
    public WidgetTreeFactory() {
        super(POSSIBLE_TREE_ANNOTATIONS, POSSIBLE_TREE_CLASS_ANNOTATIONS, POSSIBLE_LEAF_ANNOTATIONS,
            POSSIBLE_ARRAY_ANNOTATIONS);
    }

    @Override
    protected Class<? extends WidgetGroup> getTreeSettingsClass() {
        return WidgetGroup.class;
    }

    @Override
    public Tree<WidgetGroup> createTree(final Class<? extends WidgetGroup> rootClass, final SettingsType settingsType) {
        final var tree = super.createTree(rootClass, settingsType);
        resolveWidgetModifications(tree);
        propagateLayoutAdvancedAndEffectAnnotationsToChildren(tree);
        return tree;
    }

    private void propagateLayoutAdvancedAndEffectAnnotationsToChildren(final Tree<WidgetGroup> tree) {
        tree.getChildren().forEach(child -> {
            propagateAnnotationToChild(child, tree, Effect.class);
            propagateAnnotationToChild(child, tree, Advanced.class);
            if (tree.getAnnotation(Layout.class).isEmpty()) {
                tree.getTypeAnnotation(Layout.class)
                    .ifPresent(layout -> super.performAddAnnotation(child, Layout.class, layout));
            }
            getWidgetTreeFrom(child).ifPresent(this::propagateLayoutAdvancedAndEffectAnnotationsToChildren);
        });
    }

    private void propagateAnnotationToChild(final TreeNode<WidgetGroup> child, final Tree<WidgetGroup> tree,
        final Class<? extends Annotation> annotationClass) {
        tree.getAnnotation(annotationClass)
            .ifPresent(value -> super.performAddAnnotation(child, annotationClass, value));
    }

    private static Optional<Tree<WidgetGroup>> getWidgetTreeFrom(final TreeNode<WidgetGroup> node) {
        Tree<WidgetGroup> widgetTree = null;
        if (node instanceof ArrayParentNode<WidgetGroup> apn) {
            widgetTree = apn.getElementTree();
        } else if (node instanceof Tree<WidgetGroup> wt) {
            widgetTree = wt;
        }
        return Optional.ofNullable(widgetTree);
    }

    private void resolveWidgetModifications(final Tree<WidgetGroup> tree) {
        tree.getAnnotation(Modification.class)
            .ifPresent(widgetModification -> resolveWidgetModification(tree, widgetModification));
        tree.getChildren().forEach(child -> {
            if (child instanceof Tree<WidgetGroup> t) {
                resolveWidgetModifications(t);
            }
            if (child instanceof ArrayParentNode<WidgetGroup> apn) {
                resolveWidgetModifications(apn);
            }
        });
    }

    private void resolveWidgetModifications(final ArrayParentNode<WidgetGroup> arrayParentNode) {
        arrayParentNode.getAnnotation(Modification.class).ifPresent(
            widgetModification -> resolveWidgetModification(arrayParentNode.getElementTree(), widgetModification));
        resolveWidgetModifications(arrayParentNode.getElementTree());
    }

    private void resolveWidgetModification(final Tree<WidgetGroup> tree, final Modification widgetModification) {
        WidgetModificationUtil.resolveWidgetModification(tree, widgetModification,
            super::performAddOrReplaceAnnotation);
    }

    @Override
    @SuppressWarnings("unchecked") // checked by Effect.class.equals(annotationClass)
    protected <T extends Annotation> T getAnnotationFromField(final PropertyWriter field,
        final Class<T> annotationClass) {
        if (Effect.class.equals(annotationClass)) {
            final var widgetAnnotation = field.getAnnotation(Widget.class);
            if (widgetAnnotation != null
                && !EffectPredicateProvider.class.equals(widgetAnnotation.effect().predicate())) {
                if (field.getAnnotation(Effect.class) != null) {
                    throw new IllegalStateException(String.format(
                        "Conflicting Effect annotations on field and inside Widget annotation for field %s",
                        field.getName()));
                }
                return (T)widgetAnnotation.effect();
            }
        }
        return super.getAnnotationFromField(field, annotationClass);
    }

}
