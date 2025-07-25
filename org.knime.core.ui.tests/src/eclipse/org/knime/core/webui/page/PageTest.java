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
 *   Sep 15, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knime.core.webui.page.Resource.ContentType;

/**
 * Tests {@link Page}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class PageTest {

    /**
     * The bundle id of this test fragment.
     */
    public static final String BUNDLE_ID = "org.knime.core.ui.tests";

    /**
     * Tests {@link Page#isCompletelyStatic()}.
     */
    @Test
    void testIsCompletelyStaticPage() {
        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResourceFromString(() -> "resource content", "resource.html");
        assertThat(page.isCompletelyStatic()).isFalse();
        page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResourceFile("resource.html");
        assertThat(page.isCompletelyStatic()).isTrue();
    }

    /**
     * Tests {@link Page#getContentType()}.
     */
    @Test
    void testIsComponent() {
        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html");
        assertThat(page.getContentType()).isEqualTo(ContentType.HTML);

        page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("component.js");
        assertThat(page.getContentType()).isEqualTo(ContentType.SHADOW_APP);

        var page2 = Page.create().fromString(() -> "content").relativePath("component.blub");
        Assertions.assertThatThrownBy(page2::getContentType).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests a page that references an entire directory to define other page-related resources.
     */
    @Test
    void testCreateResourcesFromDir() {
        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResourceDirectory("dir");
        assertThat(page.getResource("dir/subdir/res.html")).isPresent();
        assertThat(page.getResource("dir/res2.js")).isPresent();
        assertThat(page.getResource("dir/res1.html")).isPresent();
        assertThat(page.getResource("path/to/non/existent/resource.html")).isEmpty();
    }

    /**
     * Tests page resources added via {@link PageBuilder#addResources(Function, String, boolean)}.
     *
     * @throws IOException
     */
    @Test
    void testCreateResourcesWithDynamicPaths() throws IOException {
        Function<String, InputStream> resourceSupplier = relativePath -> {
            if (relativePath.equals("null")) {
                return null;
            } else if (relativePath.equals("path/to/a/resource")) {
                return stringToInputStream("resource supplier - known path");
            } else {
                return stringToInputStream("resource supplier - another path");
            }
        };
        Function<String, InputStream> resourceSupplier2 =
            relativePath -> stringToInputStream("resource supplier 2 - " + relativePath);

        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResources(resourceSupplier, "path/prefix", true)
            .addResources(resourceSupplier2, "path/prefix/2", false);
        assertThat(page.isCompletelyStatic()).isFalse();

        assertThat(page.getResource("path/prefix/null")).isEmpty();
        assertThat(resourceToString(page.getResource("path/prefix/path/to/a/resource").get()))
            .isEqualTo("resource supplier - known path");
        assertThat(page.getResource("path/prefix/path/to/a/resource").get().isStatic()).isTrue();
        assertThat(resourceToString(page.getResource("path/prefix/path/to/a/resource2").get()))
            .isEqualTo("resource supplier - another path");
        assertThat(resourceToString(page.getResource("path/prefix/2/path/to/another/resource").get()))
            .isEqualTo("resource supplier 2 - path/to/another/resource");
        assertThat(page.getResource("path/prefix/2/path/to/another/resource").get().isStatic()).isFalse();
        assertThat(page.getResource("path/to/nonexisting/resource")).isEmpty();
    }

    /**
     * Tests page resources added via {@link PageBuilder#addResources(Function, String, boolean)} but with an empty
     * relative path prefix.
     *
     * @throws IOException
     */
    @Test
    void testCreateResourcesWithDynamicEmptyPathPrefix() throws IOException {
        Function<String, InputStream> resourceSupplier = relativePath -> {
            if (relativePath.equals("resource")) {
                return stringToInputStream("resource supplier - known path");
            } else {
                return stringToInputStream("resource supplier - another path");
            }
        };
        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .addResources(resourceSupplier, "", true);
        assertThat(resourceToString(page.getResource("resource").get())).isEqualTo("resource supplier - known path");
    }

    /**
     * Tests {@link FromFilePageBuilder#markAsReusable(String)} and the impact on the resulting {@link Page}.
     */
    @Test
    void testGetReusablePage() {
        final var pageName = "page-name";
        var page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.html")
            .getReusablePage(pageName);
        assertThat(page.getPageId()).isEqualTo(pageName + ":" + System.identityHashCode(page));

        page = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files").relativeFilePath("page.js")
            .getReusablePage(pageName);
        assertThat(page.getPageId()).isEqualTo(pageName);

        var illegalStatePage = Page.create().fromFile().bundleID(BUNDLE_ID).basePath("files")
            .relativeFilePath("page.html").addResource(() -> null, "resource.html").getReusablePage(pageName);
        Assertions.assertThatThrownBy(() -> illegalStatePage.getPageId()) // NOSONAR
            .withFailMessage("test").isInstanceOf(IllegalStateException.class);
    }

    private static InputStream stringToInputStream(final String s) {
        return new ByteArrayInputStream((s).getBytes(StandardCharsets.UTF_8));
    }

    private static String resourceToString(final Resource r) throws IOException {
        try (var is = r.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
