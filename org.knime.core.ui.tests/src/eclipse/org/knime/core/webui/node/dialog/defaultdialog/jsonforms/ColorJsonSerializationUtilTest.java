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
 *   Mar 11, 2026 (gerling): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.jsonforms;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsDataUtil.getMapper;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.knime.node.parameters.NodeParameters;

import com.fasterxml.jackson.core.JsonProcessingException;

@SuppressWarnings({"unused", "java:S2698"}) // we accept assertions without messages
class ColorJsonSerializationUtilTest {

    final static class TestColorSettings implements NodeParameters {
        Color m_color = new Color(0xAA, 0xBB, 0xCC);
    }

    @Test
    void testSerializationOfColors() throws JsonProcessingException {
        var settingsToSerialise = new TestColorSettings();
        var serialised = JsonFormsDataUtil.toJsonData(settingsToSerialise);

        assertThatJson(serialised).inPath("color").isString().isEqualTo("#AABBCC");

        var deserialised = JsonFormsDataUtil.toDefaultNodeSettings(serialised, TestColorSettings.class);
        assertEquals(settingsToSerialise.m_color, deserialised.m_color);
    }

    @Test
    void testDeserializationOfSixDigitColors() throws JsonProcessingException {
        // standard six-digit color with leading '#'
        var sixDigitColorJson = getMapper().createObjectNode().put("color", "#AABBCC");
        var deserialised = JsonFormsDataUtil.toDefaultNodeSettings(sixDigitColorJson, TestColorSettings.class);
        assertEquals(new Color(0xAA, 0xBB, 0xCC), deserialised.m_color);
        // six-digit color without leading '#'
        var sixDigitColorWithoutHashJson = getMapper().createObjectNode().put("color", "AABBCC");
        deserialised = JsonFormsDataUtil.toDefaultNodeSettings(sixDigitColorWithoutHashJson, TestColorSettings.class);
        assertEquals(new Color(0xAA, 0xBB, 0xCC), deserialised.m_color);
        // six-digit color with leading and trailing whitespace
        var sixDigitColorWithWhitespaceJson = getMapper().createObjectNode().put("color", "  #AABBCC  ");
        deserialised =
            JsonFormsDataUtil.toDefaultNodeSettings(sixDigitColorWithWhitespaceJson, TestColorSettings.class);
        assertEquals(new Color(0xAA, 0xBB, 0xCC), deserialised.m_color);
    }

    @Test
    void testDeserializationOfThreeDigitColorsExpandsToEquivalentSixDigitColorValue() throws JsonProcessingException {
        var threeDigitColorJson = getMapper().createObjectNode().put("color", "#ABC");

        var deserialised = JsonFormsDataUtil.toDefaultNodeSettings(threeDigitColorJson, TestColorSettings.class);
        assertEquals(new Color(0xAA, 0xBB, 0xCC), deserialised.m_color);
    }

    @Test
    void testDeserializationOfInvalidColorsFallsBackToWhite() throws JsonProcessingException {
        var invalidColorJson = getMapper().createObjectNode().put("color", "invalid");
        var deserialised = JsonFormsDataUtil.toDefaultNodeSettings(invalidColorJson, TestColorSettings.class);
        assertEquals(Color.WHITE, deserialised.m_color);

        invalidColorJson = getMapper().createObjectNode().put("color", "#GG");
        deserialised = JsonFormsDataUtil.toDefaultNodeSettings(invalidColorJson, TestColorSettings.class);
        assertEquals(Color.WHITE, deserialised.m_color);
    }
}
