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
 *   Aug 23, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.node.FluentNodeAPI;

/**
 * A (html) page of a ui-extension, e.g. a node view, port view or node dialog.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public sealed class Page implements Resource, FluentNodeAPI permits FromFilePage {

    /**
     * Entry point to the fluent API in order to create a page.
     *
     * @return the first stage to create a page
     */
    public static RequireFromFileOrString create() {
        return new RequireFromFileOrString() { // NOSONAR
            @Override
            public RequireBundle fromFile() {
                return new RequireBundle() {

                    @Override
                    public RequireBasePath bundleClass(final Class<?> clazz) {
                        return basePath -> relativeFilePath -> new FromFilePage(clazz, basePath, relativeFilePath);
                    }

                    @Override
                    public RequireBasePath bundleID(final String bundleID) {
                        return basePath -> relativeFilePath -> new FromFilePage(bundleID, basePath, relativeFilePath);
                    }
                };
            }

            @Override
            public RequireRelativePath fromString(final InputStreamSupplier content, final Charset charset) {
                return relativePath -> new Page(createResource(content, relativePath, false, charset), false);
            }

            @Override
            public RequireRelativePath fromString(final StringSupplier content, final Charset charset) {
                return relativePath -> new Page(
                    createResource(() -> new ByteArrayInputStream(content.get().getBytes(charset)), relativePath, false,
                        charset),
                    false);
            }
        };
    }

    private final Resource m_pageResource;

    @SuppressWarnings("javadoc")
    protected final Map<String, Resource> m_resources;

    private final Map<String, Function<String, Resource>> m_dynamicResources;

    @SuppressWarnings("javadoc")
    protected boolean m_isCompletelyStatic;

    @SuppressWarnings("javadoc")
    protected Page(final Resource pageResource, final boolean isCompleteStatic) {
        m_pageResource = pageResource;
        m_resources = new HashMap<>();
        // using this custom comparator, we make sure that the longest pathPrefix always comes first
        m_dynamicResources = new TreeMap<>(Comparator.comparingInt(String::length).reversed());
        m_isCompletelyStatic = isCompleteStatic;
    }

    /**
     * Shallow copy constructor.
     *
     * @param p page to copy
     */
    protected Page(final Page p) {
        m_pageResource = p.m_pageResource;
        m_resources = p.m_resources;
        m_dynamicResources = p.m_dynamicResources;
        m_isCompletelyStatic = p.m_isCompletelyStatic;
    }

    /* REQUIRED PROPERTIES */

    /**
     * The build stage that requires the a file or string.
     */
    public interface RequireFromFileOrString {

        /**
         * Create a (static) {@link FromFilePage page} (and associated resources) from files.
         *
         * @return the subsequent build stage
         */
        RequireBundle fromFile();

        /**
         * Create a (dynamic) {@link Page page} from a {@link InputStreamSupplier}.
         *
         * @param content the page content supplier for lazy initialization
         * @return the subsequent build stage
         */
        default RequireRelativePath fromString(final InputStreamSupplier content) {
            return fromString(content, null);
        }

        /**
         * Create a (dynamic) {@link Page page} from a {@link StringSupplier}.
         *
         * @param content the page content supplier for lazy initialization
         * @return the subsequent build stage
         */
        default RequireRelativePath fromString(final StringSupplier content) {
            return fromString(content, StandardCharsets.UTF_8);
        }

        /**
         * Create a (dynamic) {@link Page page} from a {@link InputStreamSupplier} with a custom {@link Charset}.
         *
         * @param content the page content supplier for lazy initialization
         * @param charset the charset to use for the content
         * @return the subsequent build stage
         */
        RequireRelativePath fromString(InputStreamSupplier content, Charset charset);

        /**
         * Create a (dynamic) {@link Page page} from a {@link StringSupplier} with a custom {@link Charset}.
         *
         * @param content the page content supplier for lazy initialization
         * @param charset the charset to use for the content
         * @return the subsequent build stage
         */
        RequireRelativePath fromString(StringSupplier content, Charset charset);
    }

    /**
     * The build stage that requires the bundle when creating a page from file(s).
     */
    public interface RequireBundle {

        /**
         * Specifies the bundle where the references files are located by its {@link Class}.
         *
         * @param clazz a class which is part of the bundle where the references files are located
         * @return the subsequent build stage
         */
        RequireBasePath bundleClass(Class<?> clazz);

        /**
         * Specifies the bundle where the references files are located by its id.
         *
         * @param bundleID the id of the bundle where the references files are located
         * @return the subsequent build stage
         */
        RequireBasePath bundleID(String bundleID);
    }

    /**
     * The build stage that requires the base path when creating a page from file(s).
     */
    public interface RequireBasePath {

        /**
         * Specifies the base path (beneath the bundle root).
         *
         * @param basePath the base part (beneath the bundle root)
         * @return the next step to create the page
         */
        RequireRelativeFilePath basePath(String basePath);
    }

    /**
     * The build stage that requires the relative file path when creating a page from file(s).
     */
    public interface RequireRelativeFilePath {

        /**
         * Specifies the file to get the page content from.
         *
         * @param relativeFilePath the file to get the page content from
         * @return the {@link FromFilePage}
         */
        FromFilePage relativeFilePath(String relativeFilePath);
    }

    /**
     * The build stage that requires the relative path when creating a page from string.
     */
    public interface RequireRelativePath {

        /**
         * Specifies the relative path of the page (including the page resource name itself).
         *
         * Some notes on this relative path:
         *
         * <ul>
         * <li>For some relative path <code>index.html</code>, the frontend can load the page at a location like
         * <code>http://org.knime.node.view/ui-ext/dialog/5:3:3/index.html</code>.</li>
         * <li>The file extension of the relative path (e.g., .html) is used to derive the
         * {@link org.knime.core.webui.page.Resource.ContentType}.</li>
         * <li>If additional resources are {@link Page#addResource(Supplier, String) added}, their path is relative to
         * the relative path of the page. E.g.,
         * <code>fromString("foo").relativePath("index.html").addResource("bar", "link.html")</code> is valid, just as
         * <code>fromString("foo").relativePath("index.html").addResource("bar", "../../../link.html")</code> is.</li>
         * </ul>
         *
         * @param relativePath the relative path of the page (including the page resource name itself)
         * @return the {@link Page}
         */
        Page relativePath(String relativePath);
    }

    /* OPTIONAL PROPERTIES */

    /**
     * Adds another resource to the 'context' of a page (such as a js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this {@link Page}
     */
    public Page addResource(final Supplier<InputStream> content, final String relativePath) {
        return addResource(content, relativePath, null);
    }

    /**
     * Adds another resource to the 'context' of a page (such as a js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @param charset the encoding of the content of all the resources
     * @return this {@link Page}
     */
    public Page addResource(final Supplier<InputStream> content, final String relativePath, final Charset charset) {
        var resource = createResource(content, relativePath, false, charset);
        m_resources.put(resource.getRelativePath(), resource);
        return this;
    }

    /**
     * Allows one to add multiple resources at once with a single function which dynamically maps paths to resources.
     * I.e. no need to define the exact path upfront (apart from a path-prefix).
     *
     * @param supplier the mapping function from relative path to resource content
     * @param relativePathPrefix the path prefix; if there are resources registered with 'overlapping' path prefixes,
     *            the resources with the 'longest' match are being used
     * @param areStatic whether the returned resources can be considered static or not - see {@link Resource#isStatic()}
     * @return this {@link Page}
     */
    public Page addResources(final Function<String, InputStream> supplier, final String relativePathPrefix,
        final boolean areStatic) {
        return addResources(supplier, relativePathPrefix, areStatic, null);
    }

    /**
     * Allows one to add multiple resources at once with a single function which dynamically maps paths to resources.
     * I.e. no need to define the exact path upfront (apart from a path-prefix).
     *
     * @param supplier the mapping function from relative path to resource content
     * @param relativePathPrefix the path prefix; if there are resources registered with 'overlapping' path prefixes,
     *            the resources with the 'longest' match are being used
     * @param areStatic whether the returned resources can be considered static or not - see {@link Resource#isStatic()}
     * @param charset the encoding of the content of all the resources
     * @return this {@link Page}
     */
    public Page addResources(final Function<String, InputStream> supplier, final String relativePathPrefix,
        final boolean areStatic, final Charset charset) {
        if (!areStatic && isCompletelyStatic()) {
            throw new IllegalStateException("Cannot add dynamic resources to a completely static page.");
        }
        m_dynamicResources.put(relativePathPrefix, relativePath -> { // NOSONAR
            var inputStream = supplier.apply(relativePath);
            if (inputStream == null) {
                return null;
            }
            return createResource(() -> inputStream, relativePath, areStatic, charset);
        });
        return this;
    }

    /**
     * Adds another resource to the 'context' of a page (such as js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this {@link Page}
     */
    public Page addResourceFromString(final Supplier<String> content, final String relativePath) {
        addResource(() -> new ByteArrayInputStream(content.get().getBytes(StandardCharsets.UTF_8)), relativePath,
            StandardCharsets.UTF_8);
        return this;
    }

    /* GETTERS */

    /**
     * Additional resources required by the page.
     *
     * @param relativePath the relative path to get the resource for
     *
     * @return the resource for the given relative path or an empty optional if there isn't any
     */
    public Optional<Resource> getResource(final String relativePath) {
        var resource = m_resources.get(relativePath);
        if (resource != null) {
            return Optional.of(resource);
        }
        if (m_dynamicResources.isEmpty()) {
            return Optional.empty();
        }
        return m_dynamicResources.entrySet().stream()//
            .filter(e -> relativePath.startsWith(e.getKey()))//
            .map(e -> {
                var pathPrefix = e.getKey();
                String relativePathWithoutPrefix;
                if (pathPrefix.isEmpty()) {
                    relativePathWithoutPrefix = relativePath;
                } else {
                    relativePathWithoutPrefix = relativePath.substring(e.getKey().length() + 1);
                }
                return e.getValue().apply(relativePathWithoutPrefix);
            })//
            .filter(Objects::nonNull)//
            .findFirst();
    }

    @Override
    public String getRelativePath() {
        return m_pageResource.getRelativePath();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return m_pageResource.getInputStream();
    }

    @Override
    public boolean isStatic() {
        return m_pageResource.isStatic();
    }

    /**
     * A page is regarded completely static if the page itself and all associated resources are static.
     *
     * @return <code>true</code> if the page itself and all the associated resources are static (i.e. invariable)
     */
    public boolean isCompletelyStatic() {
        return m_isCompletelyStatic;
    }

    @Override
    public ContentType getContentType() {
        return m_pageResource.getContentType();
    }

    @Override
    public Optional<Charset> getCharset() {
        return m_pageResource.getCharset();
    }

    /**
     * {@link Supplier} of a {@link String}.
     */
    public interface StringSupplier extends Supplier<String> {
        //
    }

    /**
     * {@link Supplier} of a {@link InputStream}.
     */
    public interface InputStreamSupplier extends Supplier<InputStream> {
        //
    }

    private static Resource createResource(final Supplier<InputStream> content, final String relativePath,
        final boolean isStatic, final Charset charset) {
        return new Resource() { // NOSONAR

            @Override
            public String getRelativePath() {
                return relativePath;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return content.get();
            }

            @Override
            public boolean isStatic() {
                return isStatic;
            }

            @Override
            public ContentType getContentType() {
                return ContentType.determineType(relativePath);
            }

            @Override
            public Optional<Charset> getCharset() {
                return Charset.defaultCharset().equals(charset) ? Optional.<Charset> empty()
                    : Optional.ofNullable(charset);
            }
        };
    }
}
