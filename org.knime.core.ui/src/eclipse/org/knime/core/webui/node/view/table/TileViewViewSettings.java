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
 *   Feb 11, 2026: created
 */
package org.knime.core.webui.node.view.table;

import java.util.stream.Stream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.selection.SelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColorColumnsProvider;
import org.knime.core.webui.node.view.table.TableViewLayout.DataSection;
import org.knime.core.webui.node.view.table.TableViewLayout.InteractivitySection;
import org.knime.core.webui.node.view.table.TableViewLayout.ViewSection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * View settings for the Tile View node.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public final class TileViewViewSettings implements NodeParameters {

    TileViewViewSettings() {
        this((DataTableSpec)null);
    }

    TileViewViewSettings(final NodeParametersInput context) {
        this(context.getInTableSpecs()[0]);
    }

    TileViewViewSettings(final DataTableSpec spec) {
        final String[] allColumnNames = spec == null ? new String[0] : spec.getColumnNames();
        m_displayedColumns = new ColumnFilter(allColumnNames);
    }

    @Widget(title = "Displayed columns", description = "Select the columns that should be displayed in the tiles.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(DataSection.class)
    public ColumnFilter m_displayedColumns;

    @Widget(title = "Title", description = "The title of the view.")
    @Layout(ViewSection.class)
    String m_title;

    @Widget(title = "Tile title column", description = "Defines the column used to display the title of each tile.")
    @Layout(ViewSection.class)
    StringOrEnum<RowIDOrNoneChoice> m_titleColumn = new StringOrEnum<>(RowIDOrNoneChoice.ROW_ID);

    @Widget(title = "Color column",
        description = "Defines the column used to color the data by."
            + " Only columns which have a color scheme associated can be selected."
            + " You might need to use a Color Designer node before this node to add color schemes to specific columns.")
    @ChoicesProvider(ColorColumnsProvider.class)
    @Layout(ViewSection.class)
    StringOrEnum<NoneChoice> m_colorColumn = new StringOrEnum<>(NoneChoice.NONE);

    @Widget(title = "Tiles per row",
        description = "Number of tiles to display per row."
            + " The width of these tiles will change as the window size changes.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(ViewSection.class)
    int m_tilesPerRow = 1;

    @Widget(title = "Display column headers",
        description = "Enable or disable the display of column headers."
            + " The column headers will be shown along with the cell entries in each tile.")
    @Layout(ViewSection.class)
    boolean m_displayColumnHeaders = true;

    @Widget(title = "Text alignment", description = "The alignment of the text in the tiles.")
    @ValueSwitchWidget
    @Layout(ViewSection.class)
    TextAlignment m_textAlignment = TextAlignment.LEFT;

    @Widget(title = "Page size", description = "Select the amount of tiles shown per page.")
    @Layout(ViewSection.class)
    public int m_pageSize = 10;

    @Widget(title = "Selection",
        description = "“Show” makes this view receive notifications about changes of the selection. "
            + "“Edit” also allows you to change the selection and propagate any changes you make here"
            + " to other views that show the selection.")
    @ValueSwitchWidget
    @Layout(InteractivitySection.class)
    SelectionMode m_selectionMode = SelectionMode.EDIT;

    @Widget(title = "Show only selected rows",
        description = "When checked, only the selected rows are shown in the table view.")
    @Layout(InteractivitySection.class)
    public boolean m_showOnlySelectedRows = false;

    @Widget(title = "Enable toggle 'Show only selected rows'",
        description = "When checked, it is possible to configure from within the view"
            + " whether only the selected rows are shown.")
    @Layout(InteractivitySection.class)
    boolean m_showOnlySelectedRowsConfigurable = true;

    @Widget(title = "Enable 'Clear selection' button",
        description = "When checked, a button to clear the selection is shown in the view.")
    @Layout(InteractivitySection.class)
    boolean m_showClearSelectionButton = true;

    @Widget(title = "Enable 'Select all' button",
        description = "When checked, it is possible to configure from within the view"
            + " whether only the selected rows are shown.")
    @Layout(InteractivitySection.class)
    boolean m_showSelectAllButton = true;

    enum RowIDOrNoneChoice {
            @Label(RowIDChoice.ROW_ID_LABEL)
            ROW_ID, //
            NONE
    }

    enum TextAlignment {
            LEFT, CENTER, RIGHT
    }

    @SuppressWarnings("javadoc")
    public String[] getDisplayedColumns(final DataTableSpec spec) {
        return Stream.concat(//
            Stream.of(m_displayedColumns.getMissingSelectedFromFullSpec(spec)), //
            Stream.of(m_displayedColumns.filterFromFullSpec(spec))//
        ).toArray(String[]::new);
    }

}
