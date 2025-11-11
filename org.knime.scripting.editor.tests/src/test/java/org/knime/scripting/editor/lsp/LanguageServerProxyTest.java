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
 */
package org.knime.scripting.editor.lsp;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

/**
 * Unit tests for the {@link LanguageServerProxy}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class LanguageServerProxyTest {

    private MockLanguageServer m_server;

    private LanguageServerProxy proxy;

    @BeforeEach
    void before() throws IOException {
        m_server = new MockLanguageServer();
        proxy = new LanguageServerProxy(m_server.m_mockedPb);
    }

    @AfterEach
    void after() throws IOException {
        proxy.close();
        m_server.close();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void readMessageFromServer() throws Exception {
        var recievedMessage = new CompletableFuture<String>();
        proxy.setMessageListener((l) -> recievedMessage.complete(l));

        var message = "Hello World";
        var messageBytes = message.getBytes(StandardCharsets.UTF_8);
        m_server.writeStdout("Content-Length: " + messageBytes.length + "\r\n");
        m_server.writeStdout("\r\n");
        m_server.writeStdout(messageBytes);

        Assertions.assertEquals(message, recievedMessage.get());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void readMessageWithSpecialCharsFromServer() throws Exception {
        var recievedMessage = new CompletableFuture<String>();
        proxy.setMessageListener((l) -> recievedMessage.complete(l));

        var message = "Hello Wörld";
        var messageBytes = message.getBytes(StandardCharsets.UTF_8);
        m_server.writeStdout("Content-Length: " + messageBytes.length + "\r\n");
        m_server.writeStdout("\r\n");
        m_server.writeStdout(messageBytes);

        Assertions.assertEquals(message, recievedMessage.get());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void ignoresContentTypeHeaderFromServer() throws Exception {
        var recievedMessage = new CompletableFuture<String>();
        proxy.setMessageListener((l) -> recievedMessage.complete(l));

        var message = "foo";
        var messageBytes = message.getBytes(StandardCharsets.UTF_8);
        m_server.writeStdout("Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n");
        m_server.writeStdout("Content-Length: " + messageBytes.length + "\r\n");
        m_server.writeStdout("Content-Type: application/vscode-jsonrpc; charset=utf8\r\n");
        m_server.writeStdout("\r\n");
        m_server.writeStdout(messageBytes);

        Assertions.assertEquals(message, recievedMessage.get());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void writesMessageToServer() throws Exception {
        var message = "Hello world";
        proxy.sendMessage(message);

        m_server.expectStdin("Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        m_server.expectStdin("\r\n");
        m_server.expectStdin(message);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void writesMessageWithSpecialCharsToServer() throws Exception {
        var message = "Hello Wörld";
        proxy.sendMessage(message);

        m_server.expectStdin("Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        m_server.expectStdin("\r\n");
        m_server.expectStdin(message);
    }

    static final class MockLanguageServer {
        private final ProcessBuilder m_mockedPb;

        private final PipedOutputStream m_stdout;

        private final PipedOutputStream m_stderr;

        private final PipedInputStream m_stdin;

        @SuppressWarnings("resource")
        MockLanguageServer() throws IOException {
            m_stdout = new PipedOutputStream();
            m_stderr = new PipedOutputStream();
            m_stdin = new PipedInputStream();

            var mockedProcess = Mockito.mock(Process.class);
            Mockito.when(mockedProcess.getInputStream()).thenReturn(new PipedInputStream(m_stdout));
            Mockito.when(mockedProcess.getErrorStream()).thenReturn(new PipedInputStream(m_stderr));
            Mockito.when(mockedProcess.getOutputStream()).thenReturn(new PipedOutputStream(m_stdin));

            m_mockedPb = Mockito.mock(ProcessBuilder.class);
            Mockito.when(m_mockedPb.start()).thenReturn(mockedProcess);

        }

        void writeStdout(final byte[] data) throws IOException {
            m_stdout.write(data);
            m_stdout.flush();
        }

        void writeStdout(final String data) throws IOException {
            writeStdout(data.getBytes(StandardCharsets.UTF_8));
        }

        void readStdin(final byte[] data) throws IOException {
            m_stdin.read(data);
        }

        void expectStdin(final String expected) throws IOException {
            var data = new byte[expected.getBytes(StandardCharsets.UTF_8).length];
            readStdin(data);
            Assertions.assertEquals(expected, new String(data));
        }

        void close() throws IOException {
            m_stdout.close();
            m_stderr.close();
            m_stdin.close();
        }
    }
}
