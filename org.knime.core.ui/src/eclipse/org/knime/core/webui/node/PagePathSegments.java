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
 *   May 2, 2023 (hornm): created
 */
package org.knime.core.webui.node;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.webui.node.NodeWrapper.CustomNodeWrapperTypeIdProvider;
import org.knime.core.webui.node.PageCache.PageIdType;
import org.knime.core.webui.page.Page;

/**
 * The path segments that contribute to a page path as it's returned by
 * {@link PageResourceManager#getPagePath(NodeWrapper)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @param pathPrefix the path prefix more or less encoding the ui-extension type (view, dialog etc.) - see
 *            {@link PageResourceManager#getPagePathPrefix(org.knime.core.webui.node.PageResourceManager.PageType)}
 * @param pageId the page-id which is essentially tied to a {@link Page}-instance. As a result it is build differently
 *            depending on whether the page is static, 'static-reusable' or non-static (see {@link PageIdType}) -
 *            because these page-instances have different life cycles (e.g. there will be only one single instance of a
 *            'static-reusable' page across different node instances and ui-extension-types)
 * @param pageContentId a optional path segment (i.e. can be {@code null}) reflecting the content of the page
 * @param relativePagePath the relative path to the actual page(-resource)
 */
public record PagePathSegments(String pathPrefix, String pageId, String pageContentId, String relativePagePath) {

    /**
     * @param ncUI the node container for which to create the id.
     * @return an id uniquely identifying the node container to be used as pageId in case of a static page.
     */
    public static String getStaticPageId(final NodeContainerUI ncUI) {
        var nc = Wrapper.unwrapOptional(ncUI, NodeContainer.class).orElse(null);
        if (nc != null) {
            if (nc instanceof NativeNodeContainer nnc) {
                var factory = nnc.getNode().getFactory();
                if (factory instanceof CustomNodeWrapperTypeIdProvider p) {
                    return p.getNodeWrapperTypeId(nnc);
                } else {
                    return factory.getClass().getName();
                }
            } else if (nc instanceof SubNodeContainer) {
                return SubNodeContainer.class.getName();
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (ncUI instanceof NativeNodeContainerUI nncUI) {
            return nncUI.getNodeFactoryClassName();
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
