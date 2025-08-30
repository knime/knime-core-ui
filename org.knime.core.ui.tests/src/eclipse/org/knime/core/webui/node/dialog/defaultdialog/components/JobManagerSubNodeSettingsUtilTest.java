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
 *   28 Jul 2025 (Robin Gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersSubNodeUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jobmanager.JobManagerParametersUtil;
import org.knime.shared.workflow.storage.multidir.util.IOConst;

import com.fasterxml.jackson.databind.ObjectMapper;

class JobManagerSubNodeSettingsUtilTest {

    static final String STREAMING_JOB_MANAGER_NODE_SETTINGS = String.format(//
        "<config key=\"%s\">" //
            + "    <entry key=\"%s\" type=\"xstring\" value=\"%s\"/>\n" //
            + "    <config key=\"%s\">\n" //
            + "        <entry key=\"%s\" type=\"xint\" value=\"60\"/>\n" //
            + "    </config>\n"//
            + "</config>",
        IOConst.JOB_MANAGER_KEY.get(), IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(),
        JobManagerParametersSubNodeUtil.SIMPLE_STREAMING_JOB_MANAGER_NODE_FACTORY,
        IOConst.JOB_MANAGER_SETTINGS_KEY.get(), JobManagerParametersSubNodeUtil.STREAMING_MANAGER_CHUNK_SIZE_KEY);

    @Test
    void testNodeSettingsToJSONToNodeSettings() throws InvalidSettingsException, IOException {
        final var jobManagerNodeSettings = NodeSettings.loadFromXML(
            new ByteArrayInputStream(STREAMING_JOB_MANAGER_NODE_SETTINGS.getBytes(StandardCharsets.UTF_8)));

        final var mapper = new ObjectMapper();
        final var data = mapper.createObjectNode();
        JobManagerParametersSubNodeUtil.fromNodeSettings(data, jobManagerNodeSettings);
        final var resultJobManagerSettings = JobManagerParametersSubNodeUtil.toNodeSettings(data);

        assertTrue(jobManagerNodeSettings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get())
            .equals(resultJobManagerSettings.getString(IOConst.JOB_MANAGER_FACTORY_ID_KEY.get())));

        assertTrue(jobManagerNodeSettings.getNodeSettings(IOConst.JOB_MANAGER_SETTINGS_KEY.get())
            .equals(resultJobManagerSettings.getNodeSettings(IOConst.JOB_MANAGER_SETTINGS_KEY.get())));
    }

    static final String CUSTOM_JOB_MANAGER_NODE_SETTINGS = String.format(//
        "<config key=\"%s\">" //
            + "    <entry key=\"%s\" type=\"xstring\" value=\"%s\"/>\n" //
            + "    <config key=\"%s\">\n" //
            + "    </config>\n"//
            + "</config>",
        IOConst.JOB_MANAGER_KEY.get(), IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(),
        "org.custom.plugin.CustomJobManagerFactory", IOConst.JOB_MANAGER_SETTINGS_KEY.get());

    @Test
    void testThrowsWhenAnInvalidCustomManagerIsUsed() throws InvalidSettingsException, IOException {
        final var jobManagerNodeSettings = NodeSettings
            .loadFromXML(new ByteArrayInputStream(CUSTOM_JOB_MANAGER_NODE_SETTINGS.getBytes(StandardCharsets.UTF_8)));

        final var mapper = new ObjectMapper();
        final var data = mapper.createObjectNode();
        JobManagerParametersSubNodeUtil.fromNodeSettings(data, jobManagerNodeSettings);
        assertThat(
            assertThrows(InvalidSettingsException.class, () -> JobManagerParametersSubNodeUtil.toNodeSettings(data))
                .getMessage()).isEqualTo(
                    "Custom job managers for components are not supported. Please select a valid job manager.");
    }

    static final String DEFAULT_JOB_MANAGER_NODE_SETTINGS = String.format(//
        "<config key=\"%s\">" //
            + "    <entry key=\"%s\" type=\"xstring\" value=\"%s\"/>\n" //
            + "    <config key=\"%s\">\n" //
            + "    </config>\n"//
            + "</config>",
        IOConst.JOB_MANAGER_KEY.get(), IOConst.JOB_MANAGER_FACTORY_ID_KEY.get(),
        JobManagerParametersUtil.DEFAULT_JOB_MANAGER_FACTORY_ID, IOConst.JOB_MANAGER_SETTINGS_KEY.get());

    @Test
    void testReturnsEmptySettingsForTheDefaultJobManager() throws InvalidSettingsException, IOException {
        final var jobManagerNodeSettings = NodeSettings
            .loadFromXML(new ByteArrayInputStream(DEFAULT_JOB_MANAGER_NODE_SETTINGS.getBytes(StandardCharsets.UTF_8)));

        final var mapper = new ObjectMapper();
        final var data = mapper.createObjectNode();
        JobManagerParametersSubNodeUtil.fromNodeSettings(data, jobManagerNodeSettings);
        final var resultJobManagerSettings = JobManagerParametersSubNodeUtil.toNodeSettings(data);
        assertTrue(resultJobManagerSettings.isLeaf());
    }

}
