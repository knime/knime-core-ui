export enum RowHeightMode {
  AUTO = "AUTO",
  CUSTOM = "CUSTOM",
}

export enum VerticalPaddingMode {
  DEFAULT = "DEFAULT",
  COMPACT = "COMPACT",
}

export enum AutoSizeColumnsToContent {
  FIXED = "FIXED",
  FIT_CONTENT = "FIT_CONTENT",
  FIT_CONTENT_AND_HEADER = "FIT_CONTENT_AND_HEADER",
}

export enum SelectionMode {
  SHOW = "SHOW",
  OFF = "OFF",
  EDIT = "EDIT",
}

type GenericTableViewViewSettings<T extends string[] | undefined> = {
  showRowKeys: boolean;
  showRowIndices: boolean;
  showColumnDataType: boolean;
  enableRendererSelection: boolean;
  showTableSize: boolean;
  showColumnCount: boolean;
  rowHeightMode: RowHeightMode;
  verticalPaddingMode: VerticalPaddingMode;
  customRowHeight: number;
  maxAutoRowHeight: number;
  selectionMode: SelectionMode;
  enableColumnSearch: boolean;
  enablePagination: boolean;
  pageSize: number;
  enableSortingByHeader: boolean;
  enableGlobalSearch: boolean;
  autoSizeColumnsToContent: AutoSizeColumnsToContent;
  title: string;
  skipRemainingColumns: boolean;
  showOnlySelectedRows: boolean;
  showOnlySelectedRowsConfigurable: boolean;
  displayedColumns: { selected: T };
  enableCellCopying: boolean;
  enableDataValueViews: boolean;
  rowLabel: string;
};

export type TableViewViewSettings = GenericTableViewViewSettings<string[]>;

export type TableViewDialogSettings = GenericTableViewViewSettings<
  string[] | undefined
>;

/**
 * It is not ideal that we have to handel statistics view settings here.
 * The reason for the change in type is that the statistics view dialog uses a Twinlist,
 * while the Table View dialog uses a MultiModeTwinlist (enabling e.g. column type selection).
 * The alternative would be to make the statistics view have its own view which wraps
 * the TableView, which is not as feasible as this workaround here.
 */
export type StatisticsViewSettings = Omit<
  TableViewViewSettings,
  "displayedColumns"
> & {
  displayedStatistics: string[];
};

const isStatisticsSettings = (data: any): data is StatisticsViewSettings =>
  Boolean(data.displayedStatistics);

const toTableViewSettings = (
  statisticsDialogSettings: StatisticsViewSettings,
): TableViewViewSettings => ({
  ...statisticsDialogSettings,
  displayedColumns: {
    selected: statisticsDialogSettings.displayedStatistics,
  },
});

export const parseOnViewSettingsChangeSettings = (
  dialogSettings: StatisticsViewSettings | TableViewDialogSettings,
  currentSelected: string[],
): TableViewViewSettings => {
  if (isStatisticsSettings(dialogSettings)) {
    return toTableViewSettings(dialogSettings);
  }
  const newSelected = dialogSettings.displayedColumns.selected;
  return {
    ...dialogSettings,
    displayedColumns: { selected: newSelected ?? currentSelected },
  };
};
