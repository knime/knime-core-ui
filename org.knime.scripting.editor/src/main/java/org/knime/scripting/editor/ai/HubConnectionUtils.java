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
 *   28 Aug 2023 (chaubold): created
 */
package org.knime.scripting.editor.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.widgets.Display;
import org.knime.ai.assistant.java.prefs.KaiPreferences;
import org.knime.ai.assistant.java.prefs.KaiPreferences.NoServerException;
import org.knime.ai.assistant.java.prefs.KaiPreferences.Protocol;
import org.knime.core.util.auth.CouldNotAuthorizeException;
import org.knime.workbench.explorer.ExplorerMountTable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knime.enterprise.client.rest.HubContent;
import com.knime.explorer.server.internal.WorkflowHubContentProvider;

/**
 * Utilities to query the currently selected KNIME Hub for the AI assistant and to get the authentication token.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class HubConnectionUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HubConnectionUtils() {

    }

    /**
     * @return get the base URL of the currently selected KNIME Hub
     * @throws NoServerException
     */
    public static URI getHubBaseUrl() throws NoServerException {
        return KaiPreferences.getServerAddress(Protocol.HTTP);
    }

    private static String getHubBaseUrlString() throws IOException {
        try {
            return getHubBaseUrl().toString();
        } catch (NoServerException e) {
            throw new IOException(
                "Could not connect to AI service, no valid Hub selected that provides the AI code generation");
        }
    }

    /**
     * @return the authentication token of the currently selected KNIME Hub
     * @throws ConnectException if the user is not logged in or the Hub cannot be reached
     */
    private static String getAuthenticationToken() throws ConnectException {
        var hubMountID = KaiPreferences.getHubId();
        var contentProvider = ExplorerMountTable.getMountedContent().get(hubMountID);
        if (contentProvider == null) {
            throw new ConnectException(
                "Please add the %s to your hosted mountpoints and login to use K-AI.".formatted(hubMountID));
        } else if (contentProvider instanceof WorkflowHubContentProvider hubContentProvider) {
            var remoteFileSystem = hubContentProvider.getRemoteFileSystem();
            if (remoteFileSystem instanceof HubContent hubContent) {
                try {
                    return hubContent.getAuthenticator().getAuthorization();
                } catch (CouldNotAuthorizeException ex) {
                    throw new ConnectException(
                        "Could not authorize. Please log into %s. Caused by %s".formatted(hubMountID, ex));
                }
            }
            throw new ConnectException("Could not access HubContent.");
        }
        throw new ConnectException("Unexpected content provider for mount ID '%s'.".formatted(hubMountID));
    }

    /**
     * Log in to the currently selected Hub end point
     *
     * @return true if already logged in or the login was successful
     */
    public static boolean loginToHub() {
        var hubMountID = KaiPreferences.getHubId();
        var contentProvider = ExplorerMountTable.getMountedContent().get(hubMountID);
        if (contentProvider == null) {
            return false;
        } else if (contentProvider instanceof WorkflowHubContentProvider hubContentProvider) {
            var remoteFileSystem = hubContentProvider.getRemoteFileSystem();
            if (remoteFileSystem instanceof HubContent hubContent) {
                if (hubContent.isAuthenticated()) {
                    return true;
                }

                // This needs to be called on the display thread -- at least on Mac
                Display.getDefault().syncExec(contentProvider::connect);

                return hubContent.isAuthenticated();
            }
            return false;
        }

        return false;
    }

    /**
     * Send a POST request to the provided end point path at the selected KNIME Hub
     *
     * @param endpointPath The end point at the selected KNIME Hub to call
     * @param request An object that will be turned into JSON using Jackson and sent as data to the endpoint
     * @return The response of the request as String (probably contains JSON content)
     * @throws IOException In case of connection errors or malformed request data.
     */
    public static String sendRequest(final String endpointPath, final Object request) throws IOException {
        URL url;
        String baseUrl = getHubBaseUrlString();
        try {
            url = new URL(baseUrl + endpointPath);
        } catch (MalformedURLException e) {
            throw new IOException("Could not connect to the AI service, invalid URL: " + baseUrl + endpointPath);
        }

        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        String requestJson = MAPPER.writeValueAsString(request);

        byte[] out = requestJson.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.setRequestProperty("Authorization", getAuthenticationToken());
        http.connect();

        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }

        try {
            int responseCode = http.getResponseCode();
            try (InputStream errorStream = http.getErrorStream()) {
                if (errorStream != null) {
                    String msg = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    throw new IOException(
                        "Could not connect to AI service, received response: (" + responseCode + ") " + msg);
                }
            }

            String response;
            try (InputStream is = http.getInputStream()) {
                response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (responseCode != 200) {
                throw new IOException("Error %d occurred: %s".formatted(responseCode, response));
            }

            return response;
        } catch (HttpRetryException retryException) {
            // Somehow a 401 unauthorized error prevents getResponseCode from working but throws this error,
            // so we bail out with an informative IOException here
            throw new IOException("Could not connect to AI service due to an authentication error."
                + " Please make sure you are logged in");
        }
    }
}
