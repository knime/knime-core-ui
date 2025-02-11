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
 *   Feb 12, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.widget.choices;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProviderUtil.DataTypeChoicesProvider;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
final class ChoicesProviderUtilTest {

    @Test
    void testDataTypeChoicesProvider() {
        final var dataTypes = List.of(StringCellFactory.TYPE, IntCellFactory.TYPE, DoubleCellFactory.TYPE,
            BooleanCellFactory.TYPE, LongCellFactory.TYPE);
        final var mockRegistry = mock(DataTypeRegistry.class);
        try (final var mockStaticRegistry = mockStatic(DataTypeRegistry.class)) {
            mockStaticRegistry.when(DataTypeRegistry::getInstance).thenReturn(mockRegistry);
            when(mockRegistry.availableDataTypes()).thenReturn(dataTypes);

            final var choices = new DataTypeChoicesProvider().choices(null);

            assertNotNull(choices, "Choices should not be null");
            assertEquals(dataTypes.size(), choices.length, "Should return all types");
            assertArrayEquals(dataTypes.stream().map(DataType::getName).toArray(String[]::new), choices,
                "Should have the correct values");
        }
    }
}
