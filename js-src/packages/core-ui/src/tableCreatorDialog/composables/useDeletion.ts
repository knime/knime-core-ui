import type { TableCreatorParameters } from "../types";

export const useDeletion = ({
  getData,
  setAdjusted,
  onDeleteRow,
  onDeleteColumn,
}: {
  getData: () => TableCreatorParameters;
  setAdjusted: () => void;
  onDeleteRow?: (rowIndex: number) => void;
  onDeleteColumn?: (columnIndex: number) => void;
}) => {
  const deleteColumn = (columnIndex: number) => {
    const cols = getData().columns;
    if (columnIndex < 0 || columnIndex >= cols.length) {
      return;
    }
    cols.splice(columnIndex, 1);
    onDeleteColumn?.(columnIndex);
    setAdjusted();
  };

  const deleteRow = (rowIndex: number) => {
    const cols = getData().columns;
    if (rowIndex < 0) {
      return;
    }
    for (const col of cols) {
      col.values.splice(rowIndex, 1);
      col.isInvalidAt?.splice(rowIndex, 1);
    }
    onDeleteRow?.(rowIndex);
    setAdjusted();
  };

  return {
    deleteColumn,
    deleteRow,
  };
};
