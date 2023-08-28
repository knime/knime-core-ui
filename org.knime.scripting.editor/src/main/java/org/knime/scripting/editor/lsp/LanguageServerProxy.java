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
package org.knime.scripting.editor.lsp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
 */
public final class LanguageServerProxy implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LanguageServerProxy.class);

    private final Process m_process;

    private final Thread m_messageReaderThread;

    private final InputStream m_stdoutStream;

    private final BufferedReader m_stdoutReader;

    private final BufferedWriter m_stdinWriter;

    private Consumer<String> m_messageListener;

    /**
     * Create a new {@link LanguageServerProxy} that uses the given {@link ProcessBuilder} to start the language server.
     *
     * @param pb a process builder which can start the language server process
     */
    public LanguageServerProxy(final ProcessBuilder pb) {
        try {
            m_process = pb.start();
        } catch (IOException e) {
            // TODO(AP-19338) Handle or throw the IOException directly
            throw new IllegalStateException(e);
        }

        m_stdoutStream = m_process.getInputStream();
        m_stdoutReader = new BufferedReader(new InputStreamReader(m_stdoutStream, StandardCharsets.UTF_8));
        m_stdinWriter = new BufferedWriter(new OutputStreamWriter(m_process.getOutputStream(), StandardCharsets.UTF_8));

        m_messageReaderThread = new Thread(this::parseMessages);
        m_messageReaderThread.start();
    }

    /**
     * Send the given JSON-RPC message to the language server.
     *
     * @param message the JSON-RPC message
     */
    public synchronized void sendMessage(final String message) {
        // Send the message
        LOGGER.debug("Sending to LS server: '" + message + "'");

        try {
            // TODO(AP-19338) Content-Length should be num bytes but we set it to num chars
            m_stdinWriter.write("Content-Length: " + message.length() + "\r\n");
            m_stdinWriter.write("\r\n");
            m_stdinWriter.write(message);
            m_stdinWriter.flush();
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

    private void parseMessages() {
        String line = "";
        int nextContentLength = 0;

        while (true) { // NOSONAR: Never end  TODO
            try {
                line = m_stdoutReader.readLine();
                LOGGER.debug("Line from LS: '" + line + "'");
                if (line == null) {
                    // TODO handle if the process should not have died
                    // End of stream
                    break;
                } else if (line.isBlank()) {
                    // Blank line before the content: Read the content
                    final char[] buffer = new char[nextContentLength];
                    // TODO(AP-19338) nextContentLength is bytes but here we use it as chars
                    int read = 0;
                    while (read < nextContentLength) {
                        read += m_stdoutReader.read(buffer, read, nextContentLength - read);
                    }
                    final String message = new String(buffer);
                    LOGGER.debug("Sending to LS client: " + message);
                    notifyMessageListener(message);
                } else {
                    if (!line.startsWith("Content")) {
                        LOGGER.warn("LS line not header: '" + line + "'");
                    }
                    // Read the header line
                    final String[] lineKeyVal = line.split(":");
                    final String headerKey = lineKeyVal[0];
                    final String headerVal = lineKeyVal[1];
                    switch (headerKey.strip()) {
                        case "Content-Length":
                            nextContentLength = Integer.parseInt(headerVal.strip());
                            break;

                        case "Content-Type":
                            if (!("application/vscode-jsonrpc; charset=utf-8".equals(headerVal.strip())
                                // NB: "utf8" instead of "utf-8" for backwards compatibility
                                || "application/vscode-jsonrpc; charset=utf8".equals(headerVal.strip()))) {
                                // TODO(AP-19338) how should we handle errors?
                                LOGGER.error("Invalid content type");
                                throw new IllegalStateException("Invalid content type");
                            }
                            break;

                        default:
                            // TODO(AP-19338) how should we handle errors?
                            LOGGER.error("Invalid header field: " + headerKey);
                            throw new IllegalStateException("Invalid header field: " + headerKey);
                    }
                }
            } catch (IOException e) {
                // TODO(AP-19338) how should we handle errors?
                LOGGER.error(e);
                throw new IllegalStateException(e);
            }
        }
    }

    private void notifyMessageListener(final String message) {
        if (m_messageListener != null) {
            m_messageListener.accept(message);
        }
    }

    @Override
    public void close() {
        m_messageReaderThread.interrupt();
        m_process.destroy();
    }
}
