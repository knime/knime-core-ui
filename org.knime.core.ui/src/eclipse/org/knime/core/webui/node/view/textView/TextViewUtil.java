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
 *   31 Oct 2022 (marcbux): created
 */
package org.knime.core.webui.node.view.textView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettingsSerializer;
import org.knime.core.webui.node.view.table.data.render.DataValueImageRendererRegistry;
import org.knime.core.webui.node.view.textView.data.TextViewInitialData;
import org.knime.core.webui.node.view.textView.data.TextViewInitialDataImpl;
import org.knime.core.webui.page.Page;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class TextViewUtil {

    // Note on the 'static' page id: the entire TableView-page can be considered 'completely static'
    // because the page, represented by a vue component, is just a file (won't change at runtime)
    // And the image resources associated with a page of an individual table view instance are
    // served with a globally unique 'table id' in the path.
    private static final String TEXT_VIEW_PAGE_ID = "textview";

    /**
     * The page representing the table view.
     */
    public static final Page PAGE = Page.builder(TextViewUtil.class, "js-src/dist", "TextView.umd.js")
        .markAsReusable(TEXT_VIEW_PAGE_ID).addResources(createTableCellImageResourceSupplier(),
            DataValueImageRendererRegistry.RENDERED_CELL_IMAGES_PATH_PREFIX, true)
        .build();

    // This is workaround/hack for the lack of proper random-access functionality for a (BufferedData)Table.
    // For more details see the class' javadoc.
    // It's static because it's registered and kept with the page which in turn is assumed to be static
    // (i.e. doesn't change between node instances and, hence, won't be re-created for each node instance).
    static final DataValueImageRendererRegistry RENDERER_REGISTRY =
        new DataValueImageRendererRegistry(() -> TEXT_VIEW_PAGE_ID);

    private TextViewUtil() {
        // utility class
    }

    private static Function<String, InputStream> createTableCellImageResourceSupplier() {
        return relativePath -> {
            var bytes = RENDERER_REGISTRY.renderImage(relativePath);
            return new ByteArrayInputStream(bytes);
        };
    }

    /**
     * @param settings table view view settings
     * @param table the table to create the initial data for
     * @param tableId a globally unique id to be able to uniquely identify the images belonging to the table used here
     * @return the table view's initial data object
     */
    static TextViewInitialData createInitialData(final TextViewViewSettings settings) {
        return new TextViewInitialDataImpl(settings);
    }

    /**
     * @param settingsSupplier
     * @param tableSupplier
     * @param tableId
     * @return the table view initial data service
     */
    public static InitialDataService<TextViewInitialData> createInitialDataService(
        final Supplier<TextViewViewSettings> settingsSupplier) {
        return InitialDataService.builder(() -> createInitialData(settingsSupplier.get())) //
            .serializer(new DefaultNodeSettingsSerializer<>()) //
            .build();
    }
}