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

import static org.knime.core.webui.node.view.table.RowHeightPersistorUtil.createDefaultConfigsDeprecations;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.StringArrayToColumnFilterMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.selection.SelectionCheckboxesToSelectionModeMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.selection.SelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.view.table.TableViewLayout.DataSection;
import org.knime.core.webui.node.view.table.TableViewLayout.InteractivitySection;
import org.knime.core.webui.node.view.table.TableViewLayout.ViewSection;
import org.knime.core.webui.node.view.table.TableViewViewSettings.RowHeightMode.CompactModeAndLegacyRowHeightModePersistor;
import org.knime.core.webui.node.view.table.TableViewViewSettings.VerticalPaddingMode.VerticalPaddingModePersistor;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.NodeParameters;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("java:S103") // we accept too long lines
public class TableViewViewSettings implements NodeParameters {

    /**
     * Previously, displayedColumnsV2 was called displayedColumns and stored a string array.
     */
    static final class DisplayedColumnsMigrator extends StringArrayToColumnFilterMigration {

        public DisplayedColumnsMigrator() {
            super("displayedColumns");
        }

    }

    /**
     * This modification is to be used for default node settings extending {@link TableViewViewSettings} which need to
     * replace the displayed columns.
     *
     * More precisely, this is in place for the statistics view. Other settings are hidden by reflection but because the
     * persisted settings differ here, this is not an option for the displayed columns.
     *
     * @author Paul Bärnreuther
     */
    public static final class RemoveDisplayedColumns implements Modification.Modifier {

        private interface DisplayedColumnsWidgetRef extends Modification.Reference {
        }

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(DisplayedColumnsWidgetRef.class).removeAnnotation(Widget.class);
        }
    }

    /**
     * The selected columns to be displayed.
     */
    @Widget(title = "Displayed columns", description = "Select the columns that should be displayed in the table")
    @ChoicesProvider(AllColumnsProvider.class)
    @Migration(DisplayedColumnsMigrator.class)
    @Layout(DataSection.class)
    @Persist(configKey = "displayedColumnsV2") // V2 Required for migration
    @Modification.WidgetReference(RemoveDisplayedColumns.DisplayedColumnsWidgetRef.class)
    public ColumnFilter m_displayedColumns;

    /**
     * If the row numbers should be displayed
     */
    @Widget(title = "Show row numbers", description = "Whether to display the row numbers or not")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(DataSection.class)
    public boolean m_showRowIndices;

    /**
     * If the rows keys should be displayed
     */
    @Widget(title = "Show RowIDs", description = "Whether to display the RowIDs or not")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(DataSection.class)
    public boolean m_showRowKeys = true;

    /**
     * The title of the table
     */
    @Widget(title = "Title",
        description = "The title of the table shown above the generated image. If left blank, no title will be shown.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(ViewSection.class)
    public String m_title = "Table View";

    /**
     * Whether to show the number of rows and columns above the table or or not
     */
    @Widget(title = "Show table size",
        description = "Whether to display the number of rows " + "and columns above the table or not.")
    @Migrate(loadDefaultIfAbsent = true)
    @Effect(predicate = EnablePagination.class, type = EffectType.HIDE)
    @Layout(ViewSection.class)
    public boolean m_showTableSize = true;

    /**
     * Whether to show the data type of every column in the header or not
     */
    @Widget(title = "Show column data types in header",
        description = "Whether to display the data type of the columns in the header or not")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(ViewSection.class)
    public boolean m_showColumnDataType = true;

    static class EnablePagination implements BooleanReference {

    }

    /**
     * If true only a certain number of rows is shown
     */
    @Widget(title = "Pagination",
        description = "Enables or disables the ability to only show a certain number of rows. "
            + "Enabling pagination hides the option “Show table size”.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(ViewSection.class)
    @ValueReference(EnablePagination.class)
    public boolean m_enablePagination;

    /**
     * The page size, i.e., number of rows to be displayed.
     */
    @Widget(title = "Page size", description = "Select the amount of rows shown per page")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(ViewSection.class)
    @Effect(predicate = EnablePagination.class, type = EffectType.SHOW)
    public int m_pageSize = 10;

    @SuppressWarnings("javadoc")
    public enum AUTO_SIZE_COLUMNS {
            @Label(value = "Fixed", description = "the column sizing is fixed")
            FIXED, //
            @Label(value = "Fit content",
                description = "the columns are sized according to the largest element in the column within the first"
                    + " 11 rows or within the current page when the page size is smaller than 11.")
            FIT_CONTENT, //
            @Label(value = "Fit content and header",
                description = "the content and the headers are considered for the size calculation of the columns.")
            FIT_CONTENT_AND_HEADER;
    }

    /**
     * If the column widths should be calculated from the first rows
     */
    @Widget(title = "Column width", description = "Set the width of the individual columns:")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(ViewSection.class)
    @ValueSwitchWidget
    public AUTO_SIZE_COLUMNS m_autoSizeColumnsToContent = AUTO_SIZE_COLUMNS.FIXED;

    @SuppressWarnings("javadoc")
    public enum RowHeightMode {
            @Label(value = "Auto",
                description = "the rows are sized according to the largest element across all columns and rows within"
                    + " the first 11 rows or within the current page when the page size is smaller than 11. In case of"
                    + " reporting each row will be as high as its content.")
            AUTO, //
            @Label(value = "Custom",
                description = "shows as much as you need. For instance, shows images at a size that enables to grasp"
                    + " their gist.")
            CUSTOM;

        class Ref implements ParameterReference<RowHeightMode> {

        }

        static final class IsCustom implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(Ref.class).isOneOf(CUSTOM);
            }

        }

        static final class IsAuto implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(Ref.class).isOneOf(AUTO);
            }

        }

        static final class CompactModeAndLegacyRowHeightModePersistor implements NodeParametersMigration<RowHeightMode> {

            @Override
            public List<ConfigMigration<RowHeightMode>> getConfigMigrations() {
                return createDefaultConfigsDeprecations((loadResult, settings) -> loadResult.getRowHeightMode());
            }
        }
    }

    static final String CURRENT_ROW_HEIGHT_MODE_CFG_KEY = "rowHeightModeV2";

    /**
     * The mode of the row height. Either a compact small height, a default height or a custom larger height.
     */
    @Widget(title = "Row height", description = "Set the initial height of the rows.")
    @ValueSwitchWidget
    @Layout(ViewSection.class)
    @Migration(CompactModeAndLegacyRowHeightModePersistor.class)
    @Persist(configKey = CURRENT_ROW_HEIGHT_MODE_CFG_KEY)
    @ValueReference(RowHeightMode.Ref.class)
    public RowHeightMode m_rowHeightMode = RowHeightMode.AUTO;

    static final int DEFAULT_CUSTOM_ROW_HEIGHT = 80;

    static final class CustomRowHeightPersistor implements NodeParametersMigration<Integer> {

        @Override
        public List<ConfigMigration<Integer>> getConfigMigrations() {
            return createDefaultConfigsDeprecations((loadResult, settings) -> {
                final var customRowHeight = loadResult.getCustomRowHeight();
                if (customRowHeight.isPresent()) {
                    return customRowHeight.get();
                }
                return settings.getInt("customRowHeight", DEFAULT_CUSTOM_ROW_HEIGHT);
            });
        }
    }

    static final class RowHeightMinValidation extends MinValidation {
        @Override
        public double getMin() {
            return 24.0;
        }
    }

    static final class RowHeightMaxValidation extends MaxValidation {
        @Override
        public double getMax() {
            return 1000000.0;
        }
    }

    /**
     * The custom row height used when m_rowHeightMode is custom
     */
    @Widget(title = "Custom row height", description = "Set the initial height of the rows.")
    @NumberInputWidget(minValidation = RowHeightMinValidation.class, maxValidation = RowHeightMaxValidation.class)
    @Layout(ViewSection.class)
    @Migration(CustomRowHeightPersistor.class)
    @Effect(predicate = RowHeightMode.IsCustom.class, type = EffectType.SHOW)
    public int m_customRowHeight = DEFAULT_CUSTOM_ROW_HEIGHT;

    @SuppressWarnings("javadoc")
    public enum VerticalPaddingMode {
            @Label(value = "Default",
                description = "sets the default amount of white space to increase the differentiation of the rows.")
            DEFAULT, //
            @Label(value = "Compact",
                description = "reduces white space around rows to a minimum. Choose this option to show as many rows"
                    + " as possible in given space.")
            COMPACT;

        static final class VerticalPaddingModePersistor implements NodeParametersMigration<VerticalPaddingMode> {

            @Override
            public List<ConfigMigration<VerticalPaddingMode>> getConfigMigrations() {
                return createDefaultConfigsDeprecations((loadResult, settings) -> loadResult.getVerticalPaddingMode());
            }
        }
    }

    /**
     * The maximum auto row height used when m_rowHeightMode is auto
     */
    @Widget(title = "Maximum auto row height",
        description = "Set the maximum height of the rows while using row height “Auto”.", advanced = true)
    @NumberInputWidget(minValidation = RowHeightMinValidation.class, maxValidation = RowHeightMaxValidation.class)
    @Layout(ViewSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    @Effect(predicate = RowHeightMode.IsAuto.class, type = EffectType.SHOW)
    public int m_maxAutoRowHeight = 120;

    /**
     * The mode of the row padding. Either a default larger padding or a compact smaller padding.
     */
    @Widget(title = "Row padding", description = "Set the vertical white space of the rows:")
    @ValueSwitchWidget
    @Layout(ViewSection.class)
    @Migration(VerticalPaddingModePersistor.class)
    public VerticalPaddingMode m_verticalPaddingMode = VerticalPaddingMode.DEFAULT;

    /**
     * If global search is enabled
     */
    @Widget(title = "Enable global search",
        description = "Enables or disables the ability to perform a global search inside the table.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableGlobalSearch = true;

    /**
     * If column search is enabled
     */
    @Widget(title = "Enable column search",
        description = "Enables or disables the ability to perform a column search inside the table.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableColumnSearch = true;

    /**
     * If sorting should be enabled
     */
    @Widget(title = "Enable sorting by header",
        description = "Enables or disables the ability to sort the table by clicking on the column headers")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableSortingByHeader = true;

    /**
     * If the renderer selection for each column should be enabled
     */
    @Widget(title = "Enable selection of column renderer",
        description = "Whether to enable the selection of a column renderer in the header or not")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableRendererSelection = true;

    /**
     * If cell selection + copying should be enabled
     */
    @Widget(title = "Enable copying cells",
        description = "When checked, the cells of the table are selectable and can be copied. "
            + "Click on a cell to select it. To select a range, select a cell, then click another "
            + "cell with shift pressed or with the left mouse button pressed, drag over other cells.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_enableCellCopying = true;

    /**
     * Whether selection should be shown and/or interactive
     */
    @Widget(title = "Selection",
        description = "“Show” makes this view receive notifications about changes of the selection. "
            + "“Edit” also allows you to change the selection and propagate any changes you make here"
            + " to other views that show the selection.")
    @ValueSwitchWidget
    @Layout(InteractivitySection.class)
    @Migration(SelectionCheckboxesToSelectionModeMigration.class)
    public SelectionMode m_selectionMode = SelectionMode.EDIT;

    /**
     * If true only the selected rows are shown
     */
    @Widget(title = "Show only selected rows",
        description = "When checked, only the selected rows are shown in the table view.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_showOnlySelectedRows = false;

    /**
     * If true only the selected rows are shown
     */
    @Widget(title = "Enable toggle 'Show only selected rows'",
        description = "When checked, it is possible to configure from within the view whether only the selected rows are shown.")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(InteractivitySection.class)
    public boolean m_showOnlySelectedRowsConfigurable = true;

    /**
     * Whether there should be a limit on rendered Columns
     */
    @Persist(hidden = true)
    @Migrate(loadDefaultIfAbsent = true)
    public boolean m_skipRemainingColumns;

    /**
     * Whether data value views are enabled. Limited to port views currently, but should be made a configurable settings
     * in the TableView once it is enabled there.
     */
    @Persist(hidden = true)
    @Migrate(loadDefaultIfAbsent = true)
    public boolean m_enableDataValueViews;

    /**
     * The label to show for rows. If not set, the default is 'Rows'
     */
    @Persist(hidden = true)
    @Migrate(loadDefaultIfAbsent = true)
    public String m_rowLabel = "Rows";

    /**
     * Whether to show the number columns above the table or or not
     */
    @Persist(hidden = true)
    @Migrate(loadDefaultIfAbsent = true)
    public boolean m_showColumnCount = true;

    /**
     * Create a new {@link TableViewViewSettings} with default values
     */
    protected TableViewViewSettings() {
        this((DataTableSpec)null);
    }

    /**
     * @param context
     */
    protected TableViewViewSettings(final NodeParametersInput context) {
        this(context.getInTableSpecs()[0]);
    }

    /**
     * @param spec
     */
    public TableViewViewSettings(final DataTableSpec spec) {
        final String[] allColumnNames = spec == null ? new String[0] : spec.getColumnNames();
        m_displayedColumns = new ColumnFilter(allColumnNames);
    }

    @SuppressWarnings("javadoc")
    public String[] getDisplayedColumns(final DataTableSpec spec) {
        return Stream.concat(//
            Stream.of(m_displayedColumns.getMissingSelectedFromFullSpec(spec)), //
            Stream.of(m_displayedColumns.filterFromFullSpec(spec))//
        ).toArray(String[]::new);
    }
}
