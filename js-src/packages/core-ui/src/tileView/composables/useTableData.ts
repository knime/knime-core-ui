import { ref } from "vue";
import type { Ref } from "vue";

import type { JsonDataService } from "@knime/ui-extension-service";

import type { Cell, ColumnContentType } from "@/tableView/types/Table";

import type { ColorColumn, TileViewSettings, TitleColumn } from "./useSettings";

export interface TileViewTableData {
  columnContentTypes: ColumnContentType[];
  rowCount: number;
  rows: (Cell | string | null)[][];
  rowTitles: (Cell | string | null)[] | null;
  rowColors: (string | null)[] | null;
  displayedColumns: string[];
}

const DEFAULT_TABLE: TileViewTableData = {
  columnContentTypes: [],
  rowCount: 0,
  rows: [],
  rowTitles: [],
  rowColors: [],
  displayedColumns: [],
};

export interface FetchTableOptions {
  displayedColumns: string[];
  titleColumn: TitleColumn;
  colorColumn: ColorColumn;
  fromIndex: number;
  pageSize: number;
  clearImageDataCache: boolean;
  showOnlySelectedRows: boolean;
}

export const useTableData = (
  jsonDataService: JsonDataService,
  settings: Ref<TileViewSettings>,
  currentPage: Ref<number>,
) => {
  const table = ref<TileViewTableData>(DEFAULT_TABLE);

  const fetchTable = async (overrides: Partial<FetchTableOptions> = {}) => {
    const options: FetchTableOptions = {
      displayedColumns: table.value.displayedColumns,
      titleColumn: settings.value.titleColumn,
      colorColumn: settings.value.colorColumn,
      fromIndex: currentPage.value * settings.value.pageSize,
      pageSize: settings.value.pageSize,
      clearImageDataCache: false,
      showOnlySelectedRows: settings.value.showOnlySelectedRows,
      ...overrides,
    };
    table.value = (await jsonDataService.data({
      method: "getTable",
      options: [
        options.displayedColumns,
        options.titleColumn,
        options.colorColumn,
        options.fromIndex,
        options.pageSize,
        options.clearImageDataCache,
        options.showOnlySelectedRows,
      ],
    })) as TileViewTableData;
  };

  return { table, fetchTable };
};
