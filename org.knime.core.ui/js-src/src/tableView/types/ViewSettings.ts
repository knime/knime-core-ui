export enum RowHeightMode {
  COMPACT = "COMPACT",
  DEFAULT = "DEFAULT",
  CUSTOM = "CUSTOM",
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

type TableViewViewSettings = {
  showRowKeys: boolean;
  showRowIndices: boolean;
  showColumnDataType: boolean;
  enableRendererSelection: boolean;
  showTableSize: boolean;
  rowHeightMode: RowHeightMode;
  customRowHeight: number;
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
  displayedColumns: { selected: string[] };
  enableCellCopying: boolean;
};

export default TableViewViewSettings;

export type StatisticsDialogViewSettings = Pick<
  TableViewViewSettings,
  | "title"
  | "showTableSize"
  | "enablePagination"
  | "pageSize"
  | "autoSizeColumnsToContent"
  | "enableGlobalSearch"
  | "enableColumnSearch"
  | "enableSortingByHeader"
  | "enableCellCopying"
> & {
  displayedColumns: string[];
};

export const isStatisticsSettings = (
  data: StatisticsDialogViewSettings | TableViewViewSettings,
): data is StatisticsDialogViewSettings =>
  !data.hasOwnProperty("selectionMode");

export const statisticsToTableViewSettings = (
  statisticsDialogSettings: StatisticsDialogViewSettings,
): TableViewViewSettings => ({
  ...statisticsDialogSettings,
  displayedColumns: { selected: statisticsDialogSettings.displayedColumns },
  showColumnDataType: false,
  showRowIndices: false,
  showRowKeys: false,
  selectionMode: SelectionMode.OFF,
  rowHeightMode: RowHeightMode.DEFAULT,
  customRowHeight: 80,
  enableRendererSelection: false,
  showOnlySelectedRows: false,
  showOnlySelectedRowsConfigurable: false,
  skipRemainingColumns: false,
});
