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
 *   Dec 13, 2024 (hornm): created
 */
package org.knime.core.webui.page;

/**
 * See {@link Page#getReusablePage(String)}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class ReusablePage extends FromFilePage {

    private final String m_pageName;

    ReusablePage(final FromFilePage p, final String pageName) {
        super(p);
        m_pageName = pageName;
    }

    /**
     * Creates the page id for a re-usuable and completely static page (see {@link FromFilePage#getResource(String)}).
     *
     * @return the page-id
     * @throws IllegalStateException if the page is marked as re-usable but not completely static (see
     *             {@link Page#isCompletelyStatic()}); or if 're-usability' is not supported for the page's
     *             {@link ContentType}.
     */
    public String getPageId() {
        if (!isCompletelyStatic()) {
            throw new IllegalStateException("Page is marked as 're-usable' but not completely static.");
        }

        switch (getContentType()) {
            case SHADOW_APP:
                return m_pageName;
            case HTML:
                // combine page-name with identity hash code of this object in order to guarantee global uniqueness
                return m_pageName + ":" + Integer.toString(System.identityHashCode(this));
            default:
                throw new IllegalStateException("Page with content type " + getContentType() + " can't be re-used.");
        }
    }

}
