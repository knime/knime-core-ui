import { computed, nextTick, ref, useTemplateRef } from "vue";

import { TableUI } from "@knime/knime-ui-table";

import type TableCreatorLayout from "../TableCreatorLayout.vue";
import ColumnHeaderInput from "../components/ColumnHeaderInput.vue";
import type { ColumnParameters } from "../types";
import { getUniqueColumnNameWithinArray } from "../utils/columnNaming";

export const useSelection = ({
  tableRef,
  tableCreatorLayoutRef,
  columnHeaderInputRef,
  getColumns,
  getNumRows,
  setAdjusted,
}: {
  /**
   * Attached to the TableUI component
   */
  tableRef: string;
  tableCreatorLayoutRef: string;
  columnHeaderInputRef: string;
  getColumns: () => ColumnParameters[];
  getNumRows: () => number;
  setAdjusted: () => void;
}) => {
  const selectedCellPosition = ref<{ x: number; y: number } | null>(null);

  const onCellPositionChange = (newPos: { x: number; y: number } | null) => {
    selectedCellPosition.value = newPos;
  };

  const selectedColumnIndex = computed(() => {
    return selectedCellPosition.value ? selectedCellPosition.value.x : -1;
  });

  /**
   * -1 if either no selection or header cell is selected.
   */
  const selectedRowIndex = computed(() => {
    return selectedCellPosition.value ? selectedCellPosition.value.y : -1;
  });

  const tableComponent = useTemplateRef<typeof TableUI>(tableRef);
  const tableCreatorLayoutComponent = useTemplateRef<typeof TableCreatorLayout>(
    tableCreatorLayoutRef,
  );
  const columnHeaderInputComponent =
    useTemplateRef<InstanceType<typeof ColumnHeaderInput>>(
      columnHeaderInputRef,
    );

  const refocusTable = (event?: FocusEvent) => {
    if (!event || event.target === event.currentTarget) {
      tableComponent.value?.refocusSelection();
    }
  };

  const onHeaderCellStartEditing = async (
    _columnIndex: number,
    initialValue?: string,
  ) => {
    tableCreatorLayoutComponent.value?.showPanelContent();
    await nextTick();
    columnHeaderInputComponent.value?.focusColumnNameInput(initialValue);
  };

  const uniquifyColumnNames = () => {
    if (selectedRowIndex.value === -1 && selectedColumnIndex.value >= 0) {
      const cols = getColumns();
      const uniqueName = getUniqueColumnNameWithinArray(
        cols.map((col) => col.name),
        selectedColumnIndex.value,
      );
      const currentCol = cols[selectedColumnIndex.value];
      if (uniqueName !== currentCol.name) {
        currentCol.name = uniqueName;
        setAdjusted();
      }
    }
  };

  const focusHeaderCell = (columnIndex: number) => {
    tableComponent.value?.focusHeaderCell(columnIndex);
  };

  const updateCellSelection = (
    ...args: Parameters<
      NonNullable<typeof tableComponent.value>["updateCellSelection"]
    >
  ) => {
    tableComponent.value?.updateCellSelection(...args);
  };

  const focusCell = (x: number, y: number, rectId?: number | null) =>
    updateCellSelection(
      {
        minX: x,
        minY: y,
        maxX: x,
        maxY: y,
      },
      rectId,
    );

  const selectFirstCellInColumn = (columnIndex: number) => {
    const numRows = getNumRows();
    if (numRows === 0) {
      return;
    }
    focusCell(columnIndex, 0);
  };

  return {
    selectedColumnIndex,
    selectedRowIndex,
    onCellPositionChange,
    uniquifyColumnNames,
    refocusTable,
    onHeaderCellStartEditing,
    focusHeaderCell,
    focusCell,
    selectFirstCellInColumn,
  };
};
