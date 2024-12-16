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
 *   Jul 19, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data.render;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.PageResourceManager;
import org.knime.core.webui.node.view.table.data.TableViewDataService;

/**
 * Allows one to (short-term) register {@link DataValueImageRenderer DataValueImageRenderers} together with their
 * respective {@link DataValue DataValues} to render.
 *
 * Required because the {@link TableViewDataService} doesn't return the rendered images directly but just a relative
 * image path. The image is only rendered once the browser uses the provided path to render the image. And this class is
 * intended to serve as the interchange between the place where the image path is returned via
 * {@link #addRendererAndGetImgPath(String, DataCell, DataValueImageRenderer)} (while iterating the data table) and the
 * place where the image is finally rendered {@link #renderImage(String)} (the data value is accessed again to finally
 * render it for real).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DataValueImageRendererRegistry {

    /**
     * URL-path prefix to be able to identify image resources for data cells that are rendered into images.
     */
    public static final String RENDERED_CELL_IMAGES_PATH_PREFIX = "images";

    /**
     * Used for parsing image width from URL while the height is variable.
     */
    private static final Pattern WIDTH_PATTERN = Pattern.compile("w=(\\d+)");

    /**
     * Used for parsing image width and height from URL provided by the frontend
     */
    private static final Pattern WIDTH_AND_HEIGHT_PATTERN = Pattern.compile("w=(\\d+)&h=(\\d+)");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataValueImageRendererRegistry.class);

    private static final int MAX_NUM_ROW_BATCHES_IN_CACHE = 2;

    private final Supplier<String> m_pageIdSupplier;

    private final Map<String, Images> m_imagesPerTable = Collections.synchronizedMap(new HashMap<>());

    /**
     * @param pageIdSupplier the page id of the view (see, e.g.,
     *            {@link PageResourceManager#getPageId(org.knime.core.webui.node.NodeWrapper)}). It's used to define the
     *            relative path where image resources (output of the data value renderers) are available. Supplied
     *            lazily because the page id is not available yet on construction time of renderer factory. Can be
     *            {@code null} if no values are to be rendered into images.
     */
    public DataValueImageRendererRegistry(final Supplier<String> pageIdSupplier) {
        m_pageIdSupplier = pageIdSupplier;
    }

    /**
     * Adds a new image renderer and the data cell to render to the registry and returns the image path.
     *
     * @param tableId the table to add the renderer for; must be globally unique
     * @param cell the data cell to add and to get the image path for
     * @param renderer the renderer to add
     *
     * @return the relative path where the image can be accessed
     */
    public String addRendererAndGetImgPath(final String tableId, final DataCell cell,
        final DataValueImageRenderer renderer) {
        var images = m_imagesPerTable.get(tableId);
        if (images == null) {
            throw new IllegalStateException("'startNewBatchOfTableRows' needs to be called at least once before");
        }
        var key = images.addImage(cell, renderer);
        return String.format("%s/%s/%s/%s/%s.png", //
            PageResourceManager.getPagePathPrefix(null), //
            m_pageIdSupplier.get(), //
            RENDERED_CELL_IMAGES_PATH_PREFIX, //
            tableId, //
            key);
    }

    private static String[] extractTableIdAndImgKey(final String imgPath) {
        var tableIdAndKey = imgPath.replace(".png", "").split("/");
        return new String[]{tableIdAndKey[tableIdAndKey.length - 2], tableIdAndKey[tableIdAndKey.length - 1]};
    }

    private Image getImageByImgPath(final String imgPath) {
        var tableIdAndKey = extractTableIdAndImgKey(imgPath);
        var tableId = tableIdAndKey[0];
        var images = m_imagesPerTable.get(tableId);
        if (images == null) {
            LOGGER.debugWithFormat("There is no image data available anymore for table '%s'.", tableId);
            return null;
        }
        var key = tableIdAndKey[1];
        var image = images.getImage(key);
        if (image == null) {
            LOGGER.debugWithFormat("There is no image '%s' available (anymore)", imgPath);
            return null;
        }
        return image;
    }

    /**
     * Renders the image for the given relative image path and removes the respective renderer (and data value) from the
     * registry.
     *
     * @param imgPath the relative image path. Can have optional 'w' and 'h' query parameters (e.g.
     *            {@code ../img.png?w=10&h=10}) which define the maximal width and height the image is to be scaled down
     *            to (the returned image will never be scaled up, though). If not given, it will be returned in its
     *            original size.
     * @return the image data or an empty array if the image data can't be accessed (anymore)
     */
    public byte[] renderImage(final String imgPath) {
        var split = imgPath.split("\\?", 2);
        final var image = getImageByImgPath(split[0]);
        if (image == null) {
            return new byte[0];
        }
        if (split.length == 2) {
            var widthAndHeightMatcher = WIDTH_AND_HEIGHT_PATTERN.matcher(split[1]);
            if (widthAndHeightMatcher.matches()) {

                final var width = Integer.valueOf(widthAndHeightMatcher.group(1));
                final var height = Integer.valueOf(widthAndHeightMatcher.group(2));
                return image.getData(new Dimension(width, height));
            }
            var widthMatcher = WIDTH_PATTERN.matcher(split[1]);
            if (widthMatcher.matches()) {
                final var width = Integer.valueOf(widthMatcher.group(1));
                return image.getDataFromWidth(width);
            }
        }
        return image.getData(image.getDimensions());
    }

    /**
     * Retrieves the image dimensions for the given image path
     *
     * @param imgPath the relative image path without height and width information (see
     *            {@link #addRendererAndGetImgPath(String tableId, DataCell cell, DataValueImageRenderer renderer)})
     * @return the image dimensions or null when the images cannot be accessed anymore
     */
    public Dimension getImageDimensions(final String imgPath) {
        final var image = getImageByImgPath(imgPath);
        if (image == null) {
            return null;
        }
        return image.getDimensions();
    }

    /**
     * Signals that a new batch of table row is being requested. By that, this registry knows to what batch of rows
     * certain images belong which latter helps to partially clear the cache (e.g. only removing images from the oldest
     * batch).
     *
     * @param tableId the table to start the new batch for
     */
    public void startNewBatchOfTableRows(final String tableId) {
        if (tableId == null) {
            return;
        }
        m_imagesPerTable.computeIfAbsent(tableId, id -> new Images()).startNewBatch();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugWithFormat("New batch of to-be-rendered images started for table with id '%s'.", tableId);
            logStatisticsMessages(tableId);
        }
    }

    private void logStatisticsMessages(final String tableId) {
        var images = m_imagesPerTable.get(tableId);
        if (images != null) {
            var stats = images.getStats();
            var numImages = stats.numImages();
            if (numImages == 0) {
                return;
            }
            LOGGER.debugWithFormat("  The registry for table '%s' currently contains:", tableId);
            var batchSizes = stats.batchSizes();
            LOGGER.debugWithFormat("  %d batches of size: %s, (sum: %d)", batchSizes.length,
                Arrays.toString(batchSizes), Arrays.stream(batchSizes).sum());
            var numRenderedImages = stats.numRenderedImages();
            LOGGER.debugWithFormat("  %d images in total; %d rendered, %d un-rendered", numImages, numRenderedImages,
                (numImages - numRenderedImages));
        }
    }

    /**
     * Removes all cached resources for the given table.
     *
     * @param tableId the id of the table to clear all stored cells and renderers for
     */
    public void clearImageDataCache(final String tableId) {
        if (m_imagesPerTable.remove(tableId) != null && LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format(
                "Cached image data cleared for table with id '%s'. There is still image data cached for %d tables",
                tableId, m_imagesPerTable.size()));
        }
    }

    /**
     * @param tableId
     * @return the number of renderers registered
     */
    public int numRegisteredRenderers(final String tableId) {
        if (m_imagesPerTable.containsKey(tableId)) {
            return m_imagesPerTable.get(tableId).getStats().numImages();
        } else {
            return 0;
        }
    }

    int numRegisteredTables() {
        return m_imagesPerTable.size();
    }

    StatsPerTable getStatsPerTable(final String tableId) {
        return m_imagesPerTable.get(tableId).getStats();
    }

    interface StatsPerTable {

        int numImages();

        int numRenderImageCalls();

        int numRenderedImages();

        int[] batchSizes();

    }

    // instances are thread-safe and all fields are synchronized via the instance's monitor
    private static class Images {

        private final Map<String, Image> m_images = new HashMap<>();

        private final Deque<Set<String>> m_batches = new LinkedList<>();

        private int m_hashCollisionCount;

        private StatsPerTable m_stats;

        synchronized String addImage(final DataCell cell, final DataValueImageRenderer renderer) {
            var key = Integer.toString(31 * cell.hashCode() + renderer.getId().hashCode());
            if (m_images.containsKey(key)) {
                var existingCell = m_images.get(key).getDataCell();
                if (!cell.equals(existingCell)) {
                    // hash collision
                    key += "_" + m_hashCollisionCount;
                    m_hashCollisionCount++; // NOSONAR
                    m_images.put(key, new Image(cell, renderer));
                }
            } else {
                m_images.put(key, new Image(cell, renderer));
            }
            m_batches.getFirst().add(key);
            return key;
        }

        synchronized Image getImage(final String imageId) {
            return m_images.get(imageId);
        }

        synchronized void startNewBatch() {
            if (!m_batches.isEmpty() && m_batches.getFirst().isEmpty()) {
                return;
            }
            var imagesToKeep = m_batches.stream() //
                .limit(MAX_NUM_ROW_BATCHES_IN_CACHE - 1l).flatMap(Set::stream).collect(Collectors.toSet());
            while (m_batches.size() >= MAX_NUM_ROW_BATCHES_IN_CACHE) {
                m_batches.removeLast().forEach(id -> {
                    if (!imagesToKeep.contains(id)) {
                        m_images.remove(id);
                    }
                });
            }
            m_batches.addFirst(new HashSet<>());
        }

        synchronized StatsPerTable getStats() {
            if (m_stats == null) {
                m_stats = new StatsPerTable() { // NOSONAR

                    @Override
                    public int numImages() {
                        synchronized (Images.this) {
                            return m_images.size();
                        }
                    }

                    @Override
                    public int numRenderedImages() {
                        synchronized (Images.this) {
                            return (int)m_images.values().stream().filter(Image::isRendered).count();
                        }
                    }

                    @Override
                    public int numRenderImageCalls() {
                        synchronized (Images.this) {
                            return m_images.values().stream().mapToInt(Image::getNumRenderCalls).sum();
                        }
                    }

                    @Override
                    public int[] batchSizes() {
                        synchronized (Images.this) {
                            return m_batches.stream().mapToInt(Set::size).toArray();
                        }
                    }

                };
            }
            return m_stats;
        }

    }

    private static class Image {

        private DataCell m_cell;

        private DataValueImageRenderer m_renderer;

        private Map<String, byte[]> m_dataCache = new HashMap<>();

        private int m_numRenderCalls;

        Image(final DataCell cell, final DataValueImageRenderer renderer) {
            m_cell = cell;
            m_renderer = renderer;
        }

        byte[] getDataFromWidth(final int width) {
            return getDataAndCache(String.format("[width=%s]", width), () -> m_renderer.renderImage(m_cell, width));

        }

        byte[] getData(final Dimension dimension) {
            return getDataAndCache(dimension.toString(), () -> m_renderer.renderImage(m_cell, dimension));
        }

        byte[] getDataAndCache(final String key, final Supplier<byte[]> renderImage) {
            if (m_dataCache.containsKey(key)) {
                return m_dataCache.get(key);
            }
            m_numRenderCalls++;
            final var data = renderImage.get();
            m_dataCache.put(key, data);
            return data;
        }

        DataCell getDataCell() {
            return m_cell;
        }

        Dimension getDimensions() {
            return m_renderer.getDimension(m_cell);
        }

        boolean isRendered() {
            return !m_dataCache.isEmpty();
        }

        int getNumRenderCalls() {
            return m_numRenderCalls;
        }

    }

}
