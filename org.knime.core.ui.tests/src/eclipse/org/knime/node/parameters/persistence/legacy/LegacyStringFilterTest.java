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
 *   Jan 20, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.node.parameters.persistence.legacy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsLoaderFactory.loadSettings;
import static org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.SettingsSaverFactory.saveSettings;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.persistence.Persist;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("java:S2698") // we accept assertions without message
final class LegacyStringFilterTest {

    static final String CFG_KEY = "filter";

    static final String[] INCL_LIST = new String[]{"foo"};

    static final String[] EXCL_LIST = new String[]{"bar"};

    static final class TestParameters implements NodeParameters {
        @Persist(configKey = CFG_KEY)
        LegacyStringFilter stringFilter = new LegacyStringFilter(INCL_LIST, EXCL_LIST);
    }

    @Test
    void testSaveLegacyStringFilter() throws InvalidSettingsException {
        var testParameters = new TestParameters();
        testParameters.stringFilter = new LegacyStringFilter(INCL_LIST, EXCL_LIST);
        testParameters.stringFilter.m_keepAllColumnsSelected = true;
        var settings = new NodeSettings("Test");
        saveSettings(testParameters, settings);
        var settingsModelFilterString = new SettingsModelFilterString(CFG_KEY);
        settingsModelFilterString.loadSettingsFrom(settings);
        assertArrayEquals(INCL_LIST, settingsModelFilterString.getIncludeList().toArray(new String[0]));
        assertArrayEquals(EXCL_LIST, settingsModelFilterString.getExcludeList().toArray(new String[0]));
        assertEquals(true, settingsModelFilterString.isKeepAllSelected());
    }

    @Test
    void testSaveSettingsModelFilterString() throws InvalidSettingsException {
        var settingsModelFilterString = new SettingsModelFilterString(CFG_KEY, INCL_LIST, EXCL_LIST, true);
        var settings = new NodeSettings("Test");
        settingsModelFilterString.saveSettingsTo(settings);
        TestParameters testParameters = loadSettings(TestParameters.class, settings);
        assertArrayEquals(INCL_LIST, testParameters.stringFilter.m_twinList.m_inclList);
        assertArrayEquals(EXCL_LIST, testParameters.stringFilter.m_twinList.m_exclList);
        assertEquals(true, testParameters.stringFilter.m_keepAllColumnsSelected);
    }
}
