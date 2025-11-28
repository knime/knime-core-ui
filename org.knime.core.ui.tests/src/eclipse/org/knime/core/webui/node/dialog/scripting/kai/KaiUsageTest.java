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
 *   Nov 28, 2024 (benjaminwilhelm): created
 */
package org.knime.core.webui.node.dialog.scripting.kai;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link KaiUsage} to ensure proper JSON serialization matching the TypeScript {@code UsageData} type.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
class KaiUsageTest {

    private static final ObjectMapper MAPPER = ObjectMapperUtil.getInstance().getObjectMapper();

    @Test
    void testUnknownUsage() throws Exception {
        var usage = new KaiUsage.Unknown();
        assertEquals("UNKNOWN", usage.type(), "Unknown usage type should be 'UNKNOWN'");

        // Verify JSON structure matches TypeScript type
        var json = MAPPER.writeValueAsString(usage);
        assertThatJson(json).isEqualTo(Map.of("type", "UNKNOWN"));
    }

    @Test
    void testLimitedUsage() throws Exception {
        var usage = new KaiUsage.Limited(100, 42);
        assertEquals("LIMITED", usage.type(), "Limited usage type should be 'LIMITED'");
        assertEquals(100, usage.limit(), "Limited usage limit should be 100");
        assertEquals(42, usage.used(), "Limited usage used should be 42");

        // Verify JSON structure matches TypeScript type
        var json = MAPPER.writeValueAsString(usage);
        assertThatJson(json).isEqualTo(Map.of("type", "LIMITED", "limit", 100, "used", 42));
    }

    @Test
    void testUnlimitedUsage() throws Exception {
        var usage = new KaiUsage.Unlimited();
        assertEquals("UNLIMITED", usage.type(), "Unlimited usage type should be 'UNLIMITED'");

        // Verify JSON structure matches TypeScript type
        var json = MAPPER.writeValueAsString(usage);
        assertThatJson(json).isEqualTo(Map.of("type", "UNLIMITED"));
    }

    @Test
    void testUnlicensedUsage() throws Exception {
        var usage = new KaiUsage.Unlicensed("Please purchase a license");
        assertEquals("UNLICENSED", usage.type(), "Unlicensed usage type should be 'UNLICENSED'");
        assertEquals("Please purchase a license", usage.message(),
            "Unlicensed usage message should match the provided message");

        // Verify JSON structure matches TypeScript type
        var json = MAPPER.writeValueAsString(usage);
        assertThatJson(json).isEqualTo(Map.of("type", "UNLICENSED", "message", "Please purchase a license"));
    }
}
