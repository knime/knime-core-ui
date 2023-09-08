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
 *   Dec 10, 2021 (konrad-amtenbrink): created
 */
package org.knime.core.webui.node.view.table;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.StringArrayToColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Hidden;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.view.table.TableViewLayout.DataSection;
import org.knime.core.webui.node.view.table.TableViewLayout.InteractivitySection;
import org.knime.core.webui.node.view.table.TableViewLayout.ViewSection;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class TableViewViewSettings implements DefaultNodeSettings {

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);
        }

    }

    /**
     * The selected columns to be displayed.
     */

    @Widget(title = "Displayed columns", description = "Select the columns that should be displayed in the table")
    @ChoicesWidget(choices = AllColumns.class)
    @Persist(customPersistor = StringArrayToColumnFilterPersistor.class)
    @Layout(DataSection.class)
    public ColumnFilter m_displayedColumns;

    /**
     * If the row numbers should be displayed
     */
    @Widget(title = "Show row numbers", description = "Whether to display the row numbers or not")
    @Persist(optional = true)
    @Layout(DataSection.class)
    public boolean m_showRowIndices;

    /**
     * If the rows keys should be displayed
     */
    @Widget(title = "Show RowIDs", description = "Whether to display the RowIDs or not")
    @Persist(optional = true)
    @Layout(DataSection.class)
    public boolean m_showRowKeys = true;

    /**
     * Whether to show the data type of every column in the header or not
     */
    @Widget(title = "Show column data type in header",
        description = "Whether to display the data type of the columns in the header or not")
    @Persist(optional = true)
    @Layout(DataSection.class)
    public boolean m_showColumnDataType = true;

    /**
     * Whether to show the number of rows and columns above the table or or not
     */
    @Widget(title = "Show table size",
        description = "Whether to display the number of rows " + "and columns above the table or not.")
    @Persist(optional = true)
    @Effect(signals = IsPaginationEnabled.class, type = EffectType.HIDE)
    @Layout(DataSection.class)
    public boolean m_showTableSize = true;

    /**
     * The title of the table
     */
    @Widget(title = "Title",
        description = "The title of the table shown above the generated image. If left blank, no title will be shown.")
    @Layout(ViewSection.class)
    public String m_title = "Table View";

    /**
     * whether to display the title or not
     */
    @Widget(title = "Show title", description = "Whether to display the title or not.")
    @Persist(optional = true)
    @Layout(ViewSection.class)
    public boolean m_showTitle = true;

    interface IsPaginationEnabled {
    }

    /**
     * If true only a certain number of rows is shown
     */
    @Widget(title = "Pagination",
        description = "Enables or disables the ability to only show a certain number of rows. "
            + "Enabling pagination hides the option “Show table size”.")
    @Persist(optional = true)
    @Layout(ViewSection.class)
    @Signal(id = IsPaginationEnabled.class, condition = TrueCondition.class)
    public boolean m_enablePagination;

    /**
     * The page size, i.e., number of rows to be displayed.
     */
    @Widget(title = "Page size", description = "Select the amount of rows shown per page")
    @NumberInputWidget(min = 1)
    @Persist(optional = true)
    @Layout(ViewSection.class)
    @Effect(signals = IsPaginationEnabled.class, type = EffectType.SHOW)
    public int m_pageSize = 10;

    enum AUTO_SIZE_COLUMNS {
            @Label("Fixed")
            FIXED, //
            @Label("Fit content")
            FIT_CONTENT, //
            @Label("Fit content and header")
            FIT_CONTENT_AND_HEADER;
    }

    /**
     * If the column widths should be calculated from the first rows
     */
    @Widget(title = "Columns sizing", description = "Fixed, the column sizing is fixed."
        + " Fit content, the columns are sized according to the largest element in the column within the first 10"
        + " rows or within the current page when the page size is smaller than 10."
        + " Fit content and header, the content and the headers are considered for the size calculation of the columns.")
    @Persist(optional = true)
    @Layout(ViewSection.class)
    @ValueSwitchWidget
    public AUTO_SIZE_COLUMNS m_autoSizeColumnsToContent = AUTO_SIZE_COLUMNS.FIXED;

    /**
     * See annotation.
     */
    @Widget(title = "Compact rows", description = "Whether to display the rows in a more compact form or not")
    @Persist(optional = true)
    @Layout(ViewSection.class)
    public boolean m_compactMode;

    /**
     * If global search is enabled
     */
    @Widget(title = "Enable global search",
        description = "Enables or disables the ability to perform a global search inside the table.")
    @Layout(InteractivitySection.class)
    public boolean m_enableGlobalSearch = true;

    /**
     * If column search is enabled
     */
    @Widget(title = "Enable column search",
        description = "Enables or disables the ability to perform a column search inside the table.")
    @Layout(InteractivitySection.class)
    public boolean m_enableColumnSearch = true;

    /**
     * If sorting should be enabled
     */
    @Widget(title = "Enable sorting by header",
        description = "Enables or disables the ability to sort the table by clicking on the column headers")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableSortingByHeader = true;

    @Widget(title = "Enable selection of column renderer",
        description = "Whether to enable the selection of a column renderer in the header or not")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableRendererSelection = true;

    /**
     * If cell selection + copying should be enabled
     */
    @Widget(title = "Enable copying cells",
        description = "When checked, the cells of the table are selectable and can be copied. "
            + "Click on a cell to select it. To select a range, select a cell, then click another "
            + "cell with shift pressed or with the left mouse button pressed, drag over other cells.")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableCellCopying = true;

    /**
     * If this view notifies other views when the users do a selection action
     */
    @Widget(title = "Publish selection",
        description = "When checked, the view notifies other interactive views when the user changes the selection in"
            + " the current view.")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_publishSelection = true;

    /**
     * If this view should react on selection events from other views
     */
    @Widget(title = "Subscribe to selection",
        description = "When checked, the view reacts on notifications from other interactive views that the selection"
            + " has been changed.")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_subscribeToSelection = true;

    /**
     * If true only the selected rows are shown
     */
    @Widget(title = "Show only selected rows",
        description = "When checked, only the selected rows are shown in the table view.")
    @Persist(optional = true)
    @Layout(InteractivitySection.class)
    public boolean m_showOnlySelectedRows = false;

    /**
     * If there should be a limit on rendered Columns
     */
    @Hidden
    @Persist(hidden = true, optional = true)
    public boolean m_skipRemainingColumns;

    /**
     * Create a new {@link TableViewViewSettings} with default values
     */
    protected TableViewViewSettings() {
    }

    /**
     * @param context
     */
    protected TableViewViewSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpecs()[0]);
    }

    /**
     * @param spec
     */
    public TableViewViewSettings(final DataTableSpec spec) {
        final String[] allColumnNames = spec == null ? new String[0] : spec.getColumnNames();
        m_displayedColumns = new ColumnFilter(allColumnNames);
    }

    @SuppressWarnings("javadoc")
    @JsonIgnore //
    public String[] getDisplayedColumns(final DataTableSpec spec) {
        final var choices = spec.getColumnNames();
        return m_displayedColumns.getSelected(choices, spec);
    }
}
