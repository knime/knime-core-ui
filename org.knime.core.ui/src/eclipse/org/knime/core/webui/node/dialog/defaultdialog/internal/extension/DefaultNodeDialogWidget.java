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
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.extension;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.renderers.WidgetRendererSpec;
import org.knime.core.webui.node.dialog.defaultdialog.tree.LeafNode;
import org.knime.core.webui.node.dialog.defaultdialog.tree.TreeNode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.updates.StateProvider;

/**
 * Provide this factory via the "org.knime.core.ui.defaultNodeDialogWidget" extension point to provide parts of webUI
 * dialogs from within an extension that uses those parts.
 *
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("rawtypes")
public interface DefaultNodeDialogWidget {

    /**
     * Whether the given node is to be interpreted using this widget. While this is being tested before default
     * renderers, the order of testing from different implementations of this interface is non-deterministic. It is thus
     * recommended to be specific enough to guarantee that no other widget tester would match the given node (e.g. by
     * using a data type of annotation specific to this widget.
     *
     * @param node extracted from a field of {@link NodeParameters}
     * @return true if {@link #getRenderer} should be called to get a renderer for this node instead of trying to find
     *         another widget that is suitable to render this.
     */
    boolean isApplicable(TreeNode<WidgetGroup> node);

    /**
     * Use the builder of {@link CustomWidgetRenderer} to create the renderer specification for the given node.
     *
     * @param node the node extracted from a field of {@link NodeParameters}
     * @param parametersInput the parameters input the dialog is created for
     * @return the renderer specification for the given node
     */
    CustomWidgetRenderer createRendererSpec(TreeNode<WidgetGroup> node, NodeParametersInput parametersInput);

    /**
     * The js-implementation of a json forms renderer suitable to render this widget.
     *
     * @return an input stream of a javascript file with the renderer in the frontend as default export
     */
    InputStream getRenderer();

    /**
     * Provide an object with methods that are to be called from the frontend of this renderer. Or an empty optional of
     * if no such RPC calls are required.
     *
     * @return the RPC data service specific to this widget.
     */
    Optional<Object> getRpcDataService();

    /**
     * Define what additional annotations and properties of annotations that should be linked to the state provider
     * framework come with this widget.
     *
     * @return the annotation syntax of this widget
     */
    AnnotationSyntax getSyntax();

    /**
     * The syntax of a widget built from a field in {@link NodeParameters}.
     *
     * @param annotations the annotations that should be available in a {@link LeafNode} when it is given in
     *            {@link #isAppliccable} and {@link #createRendererSpec}. I.e. any new annotation that is not present in
     *            the API defined by org.knime.core.ui.
     * @param stateProviders new state providers in these new annotations
     */
    record AnnotationSyntax(//
        List<Class<? extends Annotation>> annotations, //
        List<StateProviderSyntax> stateProviders //
    ) {

        /**
         * Use this to indicate that no specific annotation is required by this widget.
         *
         * @return an empty annotation syntax
         */
        public static AnnotationSyntax noSpecificAnnotation() {
            return new AnnotationSyntax(List.of(), List.of());
        }
    }

    /**
     * The syntax defining how state providers should be recognized.
     *
     * @param <T> the annotation defining the property setting the state provider class
     * @param <S> the class of the state provider
     * @param providedOptionName The identifier of this ui state within the updated control. This has to be set in
     *            {@link WidgetRendererSpec#getStateProviderClasses} to make use of the provided option in the frontend.
     * @param annotationClass the annotation class that defines the ui state
     * @param getProviderParameter the parameter of the provider that needs to be instantiated to retrieve the state
     * @param ignoredDefaultParameter the value that should be ignored if it is set, since it is the default. If null,
     *            no default is ignored.
     */
    record StateProviderSyntax<T extends Annotation, S extends StateProvider>( //
        String providedOptionName, //
        Class<T> annotationClass, //
        Function<T, Class<? extends S>> getProviderParameter, //
        Class<? extends S> ignoredDefaultParameter //
    ) {
    }

}
