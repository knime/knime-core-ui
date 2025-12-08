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
 *   Nov 18, 2025 (Thomas Reifenberger): created
 */
package org.knime.node.parameters.persistence.legacy;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetModifier;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Parameters that are backwards-compatible to a {@link SettingsModelWriterFileChooser}.
 *
 * This class provides an additional option to create missing folders and to specify an overwrite policy.
 *
 * Use the {@link Modifier} interface to adapt the widgets accordingly.
 *
 * <br>
 * See {@link LegacyFileWriter} for an overview of available LegacyFileWriter... implementations.
 *
 * @author Thomas Reifenberger
 */
public class LegacyFileWriterWithOverwritePolicyOptions extends LegacyFileWriterWithCreateMissingFolders {

    private static final String CFG_IF_PATH_EXISTS = "if_path_exists";

    @Widget(title = "If exists",
        description = "Specify the behavior of the node in case the output file already exists." + "<ul>"
            + "<li><b>Fail</b>: Will issue an error during the node's execution "
            + "(to prevent unintentional overwrite).</li>" //
            + "<li><b>Overwrite</b>: Will replace any existing file.</li>"
            + "<li><b>Ignore</b>: Will ignore if a file already exists and continues the copying process.</li>"
            + "</ul>")
    @ValueSwitchWidget
    @Persist(configKey = CFG_IF_PATH_EXISTS)
    @Modification.WidgetReference(OverwritePolicyRef.class)
    OverwritePolicy m_overwritePolicy = OverwritePolicy.overwrite;

    /**
     * Overwrite policies for file writing.
     *
     * @author Thomas Reifenberger
     */
    public enum OverwritePolicy {
            /* Values are lowercase for backwards compatibility of configuration entry.
             * The descriptions are intentionally omitted and moved to the widget description to allow restricting
             * the available options with a ChoicesProvider and adapting the description with a Modifier.
             */
            @Label(value = "Fail")
            fail, // NOSONAR
            @Label(value = "Overwrite")
            overwrite, // NOSONAR
            @Label(value = "Ignore")
            ignore, // NOSONAR
    }

    /**
     * Default constructor
     */
    public LegacyFileWriterWithOverwritePolicyOptions() {
    }

    @SuppressWarnings("javadoc")
    public LegacyFileWriterWithOverwritePolicyOptions(final FileSelection fileSelection,
        final boolean createMissingFolders, final OverwritePolicy overwritePolicy) {
        super(fileSelection, createMissingFolders);
        m_overwritePolicy = overwritePolicy;
    }

    interface OverwritePolicyRef extends Modification.Reference {
    }

    /**
     * Modifiers for legacy {@link FileWriterWidget}.
     *
     * @author Thomas Reifenberger
     */
    public interface Modifier extends LegacyFileWriterWithCreateMissingFolders.Modifier {
        /**
         * Finds the "overwrite policy" widget modifier.
         *
         * @param group the widget group modifier
         * @return the "overwrite policy" widget modifier
         */
        default WidgetModifier findOverwritePolicy(final WidgetGroupModifier group) {
            return group.find(OverwritePolicyRef.class);
        }

    }

}
