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
 *   Feb 16, 2026 (paulbaernreuther): created
 */
package org.knime.core.webui.node.dialog.defaultdialog.internal.filesystem;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationSerializationUtils;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Settings that are backwards-compatible to {@link SettingsModelFileSystem} when using an implementation of
 * {@link LegacyFileSystemSelection.LegacyFileSystemSelectionPersistor} on the field.
 *
 * @author Paul Bärnreuther
 */
public final class LegacyFileSystemSelection implements WidgetGroup {

    /** Constructor. */
    public LegacyFileSystemSelection() {
    }

    /**
     * Constructor
     *
     * @param input for node parameters
     */
    public LegacyFileSystemSelection(final NodeParametersInput input) {
        m_selection = new FileSystemSelection(input);
        m_legacyParameters = new NoLongerSupportedFileSystemParameters();
    }

    private LegacyFileSystemSelection(final FileSystemSelection selection,
        final NoLongerSupportedFileSystemParameters legacyParameters) {
        m_selection = selection;
        m_legacyParameters = legacyParameters;
    }

    FileSystemSelection m_selection = new FileSystemSelection();

    NoLongerSupportedFileSystemParameters m_legacyParameters = new NoLongerSupportedFileSystemParameters();

    FSLocationSpec toFSLocationSpec() {
        if (m_legacyParameters.m_legacyFileSystemConfiguration.isPresent()) {
            return m_legacyParameters.m_legacyFileSystemConfiguration.get();
        }
        return m_selection.toFSLocationSpec();
    }

    /**
     * Legacy persistor for {@link LegacyFileSystemSelection} that can be used to load and save the file system
     * selection in a way that is backwards-compatible to {@link SettingsModelFileSystem}.
     */
    public static abstract class LegacyFileSystemSelectionPersistor
        implements NodeParametersPersistor<LegacyFileSystemSelection> {

        private final String m_configKey;

        static final String CFG_LOCATION_SPEC = "location_spec";

        /**
         * Constructor.
         *
         * @param configKey the key under which the file system selection is stored in the node settings
         */
        protected LegacyFileSystemSelectionPersistor(final String configKey) {
            m_configKey = configKey;
        }

        static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

        @Override
        public LegacyFileSystemSelection load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var fsLocationSpec = FSLocationSerializationUtils
                .loadFSLocationSpec(settings.getNodeSettings(m_configKey).getNodeSettings(CFG_LOCATION_SPEC));
            if (NoLongerSupportedFileSystemParameters.isNoLongerSupportedFSLocationSpec(fsLocationSpec)) {
                return new LegacyFileSystemSelection(new FileSystemSelection(),
                    new NoLongerSupportedFileSystemParameters(fsLocationSpec));
            }
            final var newParams = FileSystemSelection.fromFSLocationSpec(fsLocationSpec);
            return new LegacyFileSystemSelection(newParams, new NoLongerSupportedFileSystemParameters());
        }

        @Override
        public void save(final LegacyFileSystemSelection param, final NodeSettingsWO settings) {
            final var rootSettings = settings.addNodeSettings(m_configKey);
            FSLocationSerializationUtils.saveFSLocationSpec(param.toFSLocationSpec(),
                rootSettings.addNodeSettings(CFG_LOCATION_SPEC));
            addDummyInternalSettings(rootSettings);
        }

        private static void addDummyInternalSettings(final NodeSettingsWO settings) {
            final var fileSystemChooserInternals = settings.addNodeSettings("file_system_chooser__Internals");
            fileSystemChooserInternals.addBoolean("has_fs_port", false);
            fileSystemChooserInternals.addBoolean("overwritten_by_variable", false);
            fileSystemChooserInternals.addString("convenience_fs_category", "LOCAL");
            fileSystemChooserInternals.addString("relative_to", "knime.workflow");
            fileSystemChooserInternals.addString("mountpoint", "LOCAL");
            fileSystemChooserInternals.addString("spaceId", "");
            fileSystemChooserInternals.addString("spaceName", "");
            fileSystemChooserInternals.addInt("custom_url_timeout", 1000);
            fileSystemChooserInternals.addBoolean("connected_fs", true);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey, CFG_LOCATION_SPEC}};
        }

    }

}
