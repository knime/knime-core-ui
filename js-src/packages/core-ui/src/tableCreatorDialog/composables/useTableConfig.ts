import { computed } from "vue";

import type {
  ColumnConfig,
  DataConfig,
  TableConfig,
} from "@knime/knime-ui-table";

import type { InitialData } from "../types";

export type CellData = {
  value: string;
  isValid: boolean;
} | null;

export const useTableConfig = ({
  getCellValue,
  getNumRows,
  dialogInitialData,
}: {
  getCellValue: (colIndex: number, rowIndex: number) => CellData;
  getNumRows: () => number;
  dialogInitialData: InitialData;
}) => {
  // Extract columns from dialog initial data
  const columns = computed(() => {
    const cols = dialogInitialData.data.model.columns;
    return Array.isArray(cols) ? cols : [];
  });

  // Data configuration computed from columns
  const dataConfig = computed<DataConfig>(() => {
    const cols = columns.value;

    const columnConfigs: ColumnConfig[] = cols.map((col, index) => ({
      header: col.name,
      subHeader: col.type,
      size: 150,
      type: col.type,
      key: `col${index}`,
      id: `col${index}`,
      hasSlotContent: true,
      noPadding: true,
      noPaddingLeft: true,
      editable: true,
      deletable: true,
      formatter: (value: any) =>
        // eslint-disable-next-line no-undefined
        value !== undefined && value !== null ? String(value) : "",
    }));

    return {
      columnConfigs,
      rowConfig: {
        rowHeight: 40,
      },
    };
  });

  const tableData = computed(() => {
    const cols = columns.value;
    const numRows = getNumRows();
    const rows: Record<string, CellData>[] = Array.from(
      { length: numRows },
      (_, rowIndex) => {
        const row: Record<string, CellData> = {};
        for (let colIndex = 0; colIndex < cols.length; colIndex++) {
          row[`col${colIndex}`] = getCellValue(colIndex, rowIndex);
        }
        return row;
      },
    );
    return [rows]; // single "group" in TableUI
  });

  const tableConfig = computed<TableConfig>(() => {
    const cols = columns.value;
    const numRows = getNumRows();

    return {
      showSelection: false,
      showCollapser: false,
      showPopovers: false,
      showColumnFilters: false,
      showBottomControls: false,
      subMenuItems: [],
      groupSubMenuItems: [],
      enableRowDeletion: true,
      enableCellSelection: true,
      enableHeaderCellSelection: true,
      enableVirtualScrolling: true,
      showNewColumnAndRowButton: true,
      enableColumnResizing: false,
      pageConfig: {
        currentSize: numRows,
        tableSize: numRows,
        pageSize: numRows,
        currentPage: 1,
        columnCount: cols.length,
        showTableSize: true,
        showPageControls: false,
      },
    };
  });

  return { dataConfig, tableData, tableConfig };
};
