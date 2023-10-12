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
package org.knime.core.webui.node.dialog.defaultdialog.setting.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldBasedNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;

/**
 * Tests SelectionCheckboxesToSelectionModePersistor.
 *
 * @author Paul Bärnreuther
 */
class SelectionCheckboxesToSelectionModePersistorTest {

    private static final String ROOT_KEY = "Test";

    private static final class TestSettings implements DefaultNodeSettings {

        @Persist(customPersistor = SelectionCheckboxesToSelectionModePersistor.class)
        SelectionMode m_selectionMode;
    }

    static Stream<Arguments> publishAndSubscribeAndModeSource() {
        return Stream.of( //
            Arguments.of(false, true, SelectionMode.SHOW), //
            Arguments.of(false, false, SelectionMode.OFF), //
            Arguments.of(true, false, SelectionMode.EDIT), //
            Arguments.of(true, true, SelectionMode.EDIT) //
        );
    }

    @ParameterizedTest
    @MethodSource("publishAndSubscribeAndModeSource")
    void testLoadCheckboxSettings(final boolean publish, final boolean subscribe, final SelectionMode selectionMode)
        throws InvalidSettingsException {

        final var savedSettings = new NodeSettings(ROOT_KEY);
        savedSettings.addBoolean("publishSelection", publish);
        savedSettings.addBoolean("subscribeToSelection", subscribe);

        final var persistor = new FieldBasedNodeSettingsPersistor<>(TestSettings.class);
        final var loaded = persistor.load(savedSettings);

        final var expected = new TestSettings();
        expected.m_selectionMode = selectionMode;
        assertResults(expected, loaded);
    }

    @Test
    void testLoadLegacyWithoutAnyPreviousSettings() throws InvalidSettingsException {
        final var savedSettings = new NodeSettings(ROOT_KEY);
        final var persistor = new FieldBasedNodeSettingsPersistor<>(TestSettings.class);
        final var loaded = persistor.load(savedSettings);

        final var expected = new TestSettings();
        expected.m_selectionMode = SelectionMode.EDIT;
        assertResults(expected, loaded);
    }

    @Test
    void testSaveAndLoad() throws InvalidSettingsException {
        final var savedString = "bar";

        final var expected = new TestSettings();
        expected.m_selectionMode = SelectionMode.SHOW;

        final var persistor = new FieldBasedNodeSettingsPersistor<>(TestSettings.class);

        final var savedSettings = new NodeSettings(ROOT_KEY);
        persistor.save(expected, savedSettings);
        var loaded = persistor.load(savedSettings);
        assertResults(expected, loaded);
    }

    private static void assertResults(final TestSettings expected, final TestSettings loaded) {
        assertEquals(expected.m_selectionMode, loaded.m_selectionMode, "The loaded selection mode is not as expected");

    }

}
