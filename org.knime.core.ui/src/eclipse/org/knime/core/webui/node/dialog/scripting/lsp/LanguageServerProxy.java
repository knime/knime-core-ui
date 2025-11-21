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
 *   Jul 1, 2022 (benjamin): created
 */
package org.knime.core.webui.node.dialog.scripting.lsp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.knime.core.node.NodeLogger;

/**
 * A {@link LanguageServerProxy} implements the basic messaging for the language server protocol (LSP). Messages from a
 * client can be sent to the server and a listener is notified if the server sends a message to the client.
 *
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @see <a href="https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/">
 *      https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/</a>
 * @noreference This class is not intended to be referenced by clients.
 */
public final class LanguageServerProxy implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LanguageServerProxy.class);

    private static final AtomicInteger STDOUT_THREAD_ID = new AtomicInteger(0);

    private static final AtomicInteger STDERR_THREAD_ID = new AtomicInteger(0);

    private final Process m_process;

    private final Thread m_stdoutReaderThread;

    private final Thread m_stderrReaderThread;

    private final OutputStream m_stdinStream;

    private Consumer<String> m_messageListener;

    private AtomicBoolean m_closed = new AtomicBoolean(false);

    /**
     * Create a new {@link LanguageServerProxy} that uses the given {@link ProcessBuilder} to start the language server.
     *
     * @param pb a process builder which can start the language server process
     * @throws IOException if an I/O error occurs starting the process
     */
    public LanguageServerProxy(final ProcessBuilder pb) throws IOException {
        m_process = pb.start();
        m_stdinStream = new BufferedOutputStream(m_process.getOutputStream());

        // Start reading the messages from the server
        m_stdoutReaderThread = new Thread(this::readServerMessages);
        m_stdoutReaderThread.setName("lsp-proxy-stdout-" + STDOUT_THREAD_ID.getAndIncrement());
        m_stdoutReaderThread.start();

        // Consume the stderr stream
        m_stderrReaderThread = new Thread(this::readServerStderr);
        m_stderrReaderThread.setName("lsp-proxy-stderr-" + STDERR_THREAD_ID.getAndIncrement());
        m_stderrReaderThread.start();
    }

    /**
     * Send the given JSON-RPC message to the language server.
     *
     * @param message the JSON-RPC message
     */
    public synchronized void sendMessage(final String message) {
        // Send the message
        LOGGER.debug("LSP message - client: '" + message + "'");

        try {
            var messageBytes = message.getBytes(StandardCharsets.UTF_8);
            // NB: The default Content-Type of "application/vscode-jsonrpc; charset=utf-8" is fine
            var header = "Content-Length: " + messageBytes.length + "\r\n\r\n";
            m_stdinStream.write(header.getBytes(StandardCharsets.UTF_8));
            m_stdinStream.write(messageBytes);
            m_stdinStream.flush();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the language listener which is notified whenever the language server sends a message.
     *
     * @param messageListener a listener which consumes JSON-RPC messages from the language server
     */
    public void setMessageListener(final Consumer<String> messageListener) {
        m_messageListener = messageListener;
    }

    @Override
    public void close() {
        if (!m_closed.getAndSet(true)) {
            m_stdoutReaderThread.interrupt();
            m_stderrReaderThread.interrupt();
            m_process.destroy();
        }
    }

    /** Reads messages from the server stdout and calls the listener until the proxy is closed */
    private void readServerMessages() {
        try (var stdout = new BufferedInputStream(m_process.getInputStream())) {
            while (!m_closed.get()) {
                var contentLength = readServerMessageHeader(stdout);
                var content = readServerMessageContent(stdout, contentLength);
                LOGGER.debug("LSP message - server: " + content);
                if (m_messageListener != null) {
                    m_messageListener.accept(content);
                }
            }
        } catch (IOException e) {
            if (!m_closed.get()) {
                LOGGER.error(e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    /** Read the header from the server stdout and return the content length */
    private static int readServerMessageHeader(final InputStream stdout) throws IOException {
        int contentLength = -1;
        while (true) {
            var header = readServerMessageHeaderLine(stdout);
            if (header.isBlank()) {
                if (contentLength == -1) {
                    // We did not read the content length yet
                    LOGGER.warn("LSP server printed empty line but header is missing");
                } else {
                    // End of header lines
                    return contentLength;
                }
            } else if (!header.startsWith("Content")) {
                LOGGER.warn("LSP server printed unexpected line instead of header: " + header);
            } else {
                // Read the header line
                final String[] lineKeyVal = header.split(":");
                final String headerKey = lineKeyVal[0];
                final String headerVal = lineKeyVal[1];
                switch (headerKey.strip()) {
                    case "Content-Length":
                        contentLength = Integer.parseInt(headerVal.strip());
                        break;

                    case "Content-Type":
                        handleServerMessageContentType(headerVal);
                        break;

                    default:
                        LOGGER.error("LSP server printed invalid header field: " + headerKey);
                }
            }
        }
    }

    /** Read a Header line from the stdout stream which is terminated by '\r\n' */
    private static String readServerMessageHeaderLine(final InputStream stdout) throws IOException {
        var bos = new ByteArrayOutputStream();
        var lastByte = 0;
        int byteRead;
        while ((byteRead = stdout.read()) != -1) {
            bos.write(byteRead);

            // NB: The LSP defines that a Header line terminates with \r\n
            if (lastByte == '\r' && byteRead == '\n') {
                break;
            }
            lastByte = byteRead;
        }
        if (bos.size() >= 2) {
            var bytes = bos.toByteArray();
            return new String(bytes, 0, bytes.length - 2, StandardCharsets.UTF_8);
        } else {
            throw new IOException("Unexpected end of stream while reading a header line");
        }
    }

    /** Read the content with the specified amount of bytes from the server stdout */
    private static String readServerMessageContent(final InputStream stdout, final int contentLength)
        throws IOException {
        var content = stdout.readNBytes(contentLength);
        return new String(content, StandardCharsets.UTF_8);
    }

    private void readServerStderr() {
        try (var stderr = m_process.getErrorStream()) {
            var stderrReader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8));

            while (!m_closed.get()) {
                var line = stderrReader.readLine();
                LOGGER.debug("LSP stderr line: '" + line + "'");
            }
        } catch (IOException e) {
            if (!m_closed.get()) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /** Logs an error if the content type is unexpected */
    private static void handleServerMessageContentType(final String contentType) {
        // NB: We do not fail but just try to read the content as UTF-8
        if (!("application/vscode-jsonrpc; charset=utf-8".equals(contentType.strip())
            // NB: "utf8" instead of "utf-8" for backwards compatibility
            || "application/vscode-jsonrpc; charset=utf8".equals(contentType.strip()))) {
            LOGGER.error("Language server used invalid content type: " + contentType);
        }
    }
}
