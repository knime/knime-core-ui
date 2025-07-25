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
 *   9 Nov 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil.getMapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.widget.credentials.Credentials;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class JsonFormsDataUtilTest {

    @SuppressWarnings("unused")
    private static class TestSettings implements NodeParameters {
        String fromSettings = "def";

        String m_fromSpec = "def";

        TestSettings() {
        }

        TestSettings(final String settings) {
            fromSettings = settings;
        }

        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static class TestSettingsSpec extends TestSettings {
        @SuppressWarnings("unused")
        TestSettingsSpec() {
        }

        TestSettingsSpec(final NodeParametersInput context) {
            m_fromSpec = context.getInTableSpecs()[0].getColumnSpec(0).getName();
        }
    }

    static PortObjectSpec[] createSpecs(final String name) {
        return new PortObjectSpec[]{new DataTableSpec(new DataColumnSpecCreator(name, StringCell.TYPE).createSpec())};
    }

    @Test
    void testToJsonData() {
        assertThatJson(JsonFormsDataUtil.toJsonData(new TestSettings("foo")))//
            .isObject()//
            .containsEntry("fromSettings", "foo")//
            .containsEntry("fromSpec", "def");
    }

    @Test
    void testToDefaultNodeSettings() throws JsonProcessingException {
        assertThat(JsonFormsDataUtil.toDefaultNodeSettings(getMapper().createObjectNode().put("fromSettings", "foo"),
            TestSettings.class)).isEqualTo(new TestSettings("foo"));
    }

    @Test
    void testCreateDefaultNodeSettingsWithSpecs() {
        assertThat(NodeParametersUtil.createSettings(TestSettingsSpec.class, createSpecs("bar")))
            .isEqualTo(new TestSettingsSpec(NodeParametersUtil.createDefaultNodeSettingsContext(createSpecs("bar"))));
    }

    @Test
    void testCreateDefaultNodeSettingsWithSpecsDefault() {
        assertThat(NodeParametersUtil.createSettings(TestSettings.class, createSpecs("bar")))
            .isEqualTo(new TestSettings());
    }

    @Test
    void registersCredentialsSerializersToHidePassword() {
        @SuppressWarnings("unused")
        final class TestCredentialsSettings implements NodeParameters {
            Credentials m_credentials = new Credentials("username", "password");
        }

        final var nodeContainerMock = mock(NodeContainer.class);
        when(nodeContainerMock.getID()).thenReturn(new NodeID(0));

        NodeContext.pushContext(nodeContainerMock);
        try {
            assertThatJson(JsonFormsDataUtil.toJsonData(new TestCredentialsSettings()))//
                .inPath("credentials").isObject()//
                .containsEntry("username", "username")//
                .containsEntry("isHiddenPassword", true) //
                .doesNotContainKey("password");
        } finally {
            NodeContext.removeLastContext();
        }
    }

    final static class TestZonedDateTimeSettings implements NodeParameters {
        ZonedDateTime m_zonedDateTime = ZonedDateTime.of(2021, 11, 9, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));
    }

    @Test
    void testSerializationOfZonedDateTimes() throws JsonProcessingException {

        var settingsToSerialise = new TestZonedDateTimeSettings();
        var serialised = JsonFormsDataUtil.toJsonData(settingsToSerialise);

        assertThatJson(serialised) //
            .inPath("zonedDateTime").isObject().contains( //
                Map.entry("dateTime", "2021-11-09T15:30:00"), //
                Map.entry("timeZone", "Europe/Berlin") //
            );

        // also test deserialization
        var deserialised = JsonFormsDataUtil.toDefaultNodeSettings(serialised, TestZonedDateTimeSettings.class);
        assertEquals(settingsToSerialise.m_zonedDateTime, deserialised.m_zonedDateTime);
    }
}
