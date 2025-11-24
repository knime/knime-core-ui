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
 *   Oct 27, 2025 (Paul Bärnreuther): created
 *   Nov 21, 2025 (Thomas Reifenberger): updated
 */
package org.knime.node.parameters.persistence.legacy;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetModifier;
import org.knime.filehandling.core.data.location.FSLocationSerializationUtils;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;

/**
 *
 * Parameters that are backwards-compatible to a {@link SettingsModelWriterFileChooser}.
 *
 * This class does not provide optional features.
 *
 * Use the {@link Modifier} interface to adapt the widgets accordingly.
 *
 * <br>
 * <br>
 *
 * The {@link LegacyFileWriter} class hierarchy provides the different options that are needed for legacy support:
 * <table border="1" cellpadding="3" cellspacing="0">
 * <tr>
 * <th>Class</th>
 * <th>Create Missing Folders</th>
 * <th>Overwrite Policy</th>
 * </tr>
 * <tr>
 * <td>{@link LegacyFileWriter}</td>
 * <td>No</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link LegacyFileWriterWithCreateMissingFolders}</td>
 * <td>Yes</td>
 * <td>No</td>
 * </tr>
 * <tr>
 * <td>{@link LegacyFileWriterWithOverwritePolicyOptions}</td>
 * <td>Yes</td>
 * <td>Yes</td>
 * </tr>
 * </table>
 * For future migrations, this hierarchy can be extended as needed and de-duplicated using composition if necessary.
 *
 * @author Paul Bärnreuther
 * @author Thomas Reifenberger
 */
public class LegacyFileWriter implements NodeParameters {

    @Persistor(FileSelectionWriterPersistor.class)
    @FileWriterWidget
    @Modification.WidgetReference(FileSelectionRef.class)
    @Widget(title = "File Selection", description = "The file selection.") // To be overwritten using the modifier
    FileSelection m_file = new FileSelection();

    static final class FileSelectionWriterPersistor implements NodeParametersPersistor<FileSelection> {

        @Override
        public FileSelection load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var fsLocation = FSLocationSerializationUtils.loadFSLocation(settings.getNodeSettings("path"));
            return new FileSelection(fsLocation);
        }

        @Override
        public void save(final FileSelection param, final NodeSettingsWO settings) {
            FSLocationSerializationUtils.saveFSLocation(param.getFSLocation(), settings.addNodeSettings("path"));
            addDummyInternalSettings(settings);
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
            return new String[][]{{"path"}};
        }

    }

    interface FileSelectionRef extends Modification.Reference {

    }

    /**
     * Modifiers for legacy {@link FileWriterWidget}.
     *
     * @author Paul Bärnreuther
     * @author Thomas Reifenberger
     */
    public interface Modifier extends Modification.Modifier {

        /**
         * Use the resulting widget modifier to set title and ui of the file selection. E.g. make it use
         * {@link SingleFileSelectionMode#FOLDER} if folders should be selected instead of files.
         *
         * @param group the widget group modifier
         * @return the file selection widget modifier
         */
        default WidgetModifier findFileSelection(final WidgetGroupModifier group) {
            return group.find(FileSelectionRef.class);
        }
    }

}
