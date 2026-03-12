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
 *
 * ------------------------------------------------------------------------
 */
package org.knime.core.webui.headless;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.report.ReportUtil.ImageFormat;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Service interface for generating static content (images, reports, PDFs) from node and component views
 * using a headless browser.
 *
 * <p>Implementations may use a local headless browser (e.g. Equo Chromium) or delegate to a remote
 * headless-browser-service via HTTP.
 *
 * @since 5.12
 */
public interface HeadlessContentService {

    /**
     * Generates an image from a native node view.
     *
     * @param nc the native node container whose view to render
     * @param exec execution monitor for cancellation and progress
     * @param timeout maximum time to wait for the image to be generated
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @param imageFormat the desired image format (SVG or PNG)
     * @return the image data as an encoded string (base64 for PNG, URL-encoded for SVG)
     * @throws CanceledExecutionException if execution was canceled
     * @throws TimeoutException if the operation timed out
     * @throws ExecutionException if an error occurred during rendering
     * @throws InterruptedException if the thread was interrupted
     */
    String generateImage(NativeNodeContainer nc, ExecutionMonitor exec, Duration timeout, int width, int height,
        ImageFormat imageFormat)
        throws CanceledExecutionException, TimeoutException, ExecutionException, InterruptedException;

    /**
     * Generates a report (HTML fragment) from a component (composite) view.
     *
     * @param snc the sub-node container (component) whose view to render
     * @param exec execution monitor for cancellation and progress
     * @param timeout maximum time to wait for the report to be generated
     * @param width the width of the rendering viewport in pixels
     * @param height the height of the rendering viewport in pixels
     * @param imageFormat the image format for embedded images in the report
     * @return the generated report as an HTML string
     * @throws CanceledExecutionException if execution was canceled
     * @throws TimeoutException if the operation timed out
     * @throws ExecutionException if an error occurred during rendering
     * @throws InterruptedException if the thread was interrupted
     */
    String generateReport(SubNodeContainer snc, ExecutionMonitor exec, Duration timeout, int width, int height,
        ImageFormat imageFormat)
        throws CanceledExecutionException, TimeoutException, ExecutionException, InterruptedException;

    /**
     * Prints HTML content to a PDF file using a headless browser.
     *
     * @param htmlContentStream input stream providing the HTML content
     * @param outputFile the path where the PDF file should be written
     * @param exec execution monitor for cancellation and progress
     * @param timeout maximum time to wait for the PDF to be generated
     * @throws CanceledExecutionException if execution was canceled
     * @throws TimeoutException if the operation timed out
     * @throws ExecutionException if an error occurred during rendering
     * @throws InterruptedException if the thread was interrupted
     * @throws IOException if an I/O error occurs reading HTML or writing PDF
     */
    void printToPdf(InputStream htmlContentStream, Path outputFile, ExecutionMonitor exec, Duration timeout)
        throws CanceledExecutionException, TimeoutException, ExecutionException, InterruptedException, IOException;
}
