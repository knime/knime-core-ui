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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * A {@link Page} created from a file.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public sealed class FromFilePage extends Page permits ReusablePage {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FromFilePage.class);

    private final Class<?> m_clazz;

    private final String m_bundleID;

    private final String m_basePath;

    FromFilePage(final Class<?> clazz, final String basePath, final String relativeFilePath) {
        super(createFileResource(clazz, null, basePath, relativeFilePath), true);
        m_clazz = clazz;
        m_bundleID = null;
        m_basePath = basePath;
    }

    FromFilePage(final String bundleID, final String basePath, final String relativeFilePath) {
        super(createFileResource(null, bundleID, basePath, relativeFilePath), true);
        m_clazz = null;
        m_bundleID = bundleID;
        m_basePath = basePath;
    }

    /**
     * Shallow copy constructor.
     *
     * @param p
     */
    protected FromFilePage(final FromFilePage p) {
        super(p);
        m_clazz = p.m_clazz;
        m_bundleID = p.m_bundleID;
        m_basePath = p.m_basePath;
    }

    /* OPTIONAL PROPERTIES */

    /**
     * Adds another resource file to the 'context' of a page (such as a js-file).
     *
     * NOTE: the referenced resource-file is expected to be UTF-8-encoded.
     *
     * @param relativeFilePath the relative path to the file
     * @return this {@link FromFilePage}
     */
    public FromFilePage addResourceFile(final String relativeFilePath) {
        var resource = createFileResource(m_clazz, m_bundleID, m_basePath, relativeFilePath);
        m_resources.put(resource.getRelativePath(), resource);
        return this;
    }

    /**
     * Adds all files in the given directory to the 'context' of a page (a directory containing, e.g., js- and
     * css-files).
     *
     * NOTE: the referenced resource-files are expected to be UTF-8-encoded.
     *
     * @param relativeDirPath the relative path to the directory
     * @return this {@link FromFilePage}
     */
    public FromFilePage addResourceDirectory(final String relativeDirPath) {
        var root = getAbsoluteBasePath(m_clazz, m_bundleID, m_basePath);
        createResourcesFromDir(root, root.resolve(relativeDirPath), m_resources);
        return this;
    }

    private static FileResource createFileResource(final Class<?> clazz, final String bundleID, final String basePath,
        final String relativeFilePath) {
        var relFile = Paths.get(relativeFilePath);
        Path file = getAbsoluteBasePath(clazz, bundleID, basePath).resolve(relFile);
        return createResourceFromFile(relFile, file);
    }

    private static Path getAbsoluteBasePath(final Class<?> clazz, final String bundleID, final String baseDir) {
        if (clazz != null) {
            return getAbsoluteBasePath(FrameworkUtil.getBundle(clazz), baseDir);
        } else {
            return getAbsoluteBasePath(Platform.getBundle(bundleID), baseDir);
        }
    }

    /*
     * The bundle path + base path.
     */
    private static Path getAbsoluteBasePath(final Bundle bundle, final String baseDir) {
        var bundleUrl = bundle.getEntry(".");
        try {
            // must not use url.toURI() -- FileLocator leaves spaces in the URL (see eclipse bug 145096)
            // -- taken from TableauHyperActivator.java line 158
            var url = FileLocator.toFileURL(bundleUrl);
            return Paths.get(new URI(url.getProtocol(), url.getFile(), null)).resolve(baseDir).normalize();
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalStateException("Failed to resolve the directory " + baseDir, ex);
        }
    }

    private static FileResource createResourceFromFile(final Path relativeFilePath, final Path file) {
        assert !relativeFilePath.isAbsolute();
        if (!Files.isRegularFile(file)) {
            LOGGER.codingWithFormat("The file '%s' doesn't exist (or is not a regular file)", file);
        }
        return new FileResource(file, relativeFilePath);
    }

    private static void createResourcesFromDir(final Path root, final Path dir, final Map<String, Resource> res) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            dirStream.forEach(f -> {
                if (Files.isDirectory(f)) {
                    createResourcesFromDir(root, f, res);
                } else {
                    var resource = createResourceFromFile(root.relativize(f).normalize(), f);
                    res.put(resource.getRelativePath(), resource);
                }
            });
        } catch (IOException ex) {
            LOGGER.codingWithFormat("Failed to resolve resources from directory %s", dir, ex);
        }
    }

    /* GETTERS */

    /**
     * Creates a page that can be re-used by different nodes (e.g., as node dialog and/or view) and/or port (as port
     * view). A benefit of re-using pages is that the resources associated with the page only need to be requested once
     * for different nodes/ports.
     *
     * Note that page re-use is only possible if the page itself and all associated resources are static (see
     * {@link Resource#isStatic()}).
     *
     * And also note that in order for a page to be truly re-used, the very same instance of the page must be used by
     * the different nodes/ports.
     *
     * @param pageName a name for the re-usable page; must not be {@code null}
     * @return a new {@link ReusablePage} that wraps this {@link FromFilePage}
     */
    public ReusablePage getReusablePage(final String pageName) {
        Objects.requireNonNull(pageName);
        return new ReusablePage(this, pageName);
    }

    /*
     * -------------------------------------------------------------------------------------------------
     * Methods overwritten from the parent class in order to mark the page is _not_ completely static anymore.
     * -------------------------------------------------------------------------------------------------
     */

    @Override
    public FromFilePage addResource(final Supplier<InputStream> content, final String relativePath) {
        m_isCompletelyStatic = false;
        super.addResource(content, relativePath);
        return this;
    }

    @Override
    public FromFilePage addResourceFromString(final Supplier<String> content, final String relativePath) {
        m_isCompletelyStatic = false;
        super.addResourceFromString(content, relativePath);
        return this;
    }

    @Override
    public FromFilePage addResources(final Function<String, InputStream> supplier, final String relativePathPrefix,
        final boolean areStatic) {
        if (!areStatic) {
            m_isCompletelyStatic = false;
        }
        super.addResources(supplier, relativePathPrefix, areStatic);
        return this;
    }

    @Override
    public Page addResources(final Function<String, InputStream> supplier, final String relativePathPrefix,
        final boolean areStatic, final Charset charset) {

        if (!areStatic) {
            m_isCompletelyStatic = false;
        }
        super.addResources(supplier, relativePathPrefix, areStatic, charset);
        return this;
    }
}
