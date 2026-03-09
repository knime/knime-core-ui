import { useCellIdentifiers } from "./useCellIdentifiers";
import { useValidationBackend } from "./useValidationBackend";

/**
 * API is index-based but internally we use stable IDs for rows and columns to keep track of validation results even when rows/columns are deleted.
 *
 * To handle this correctly, these ids need to be updated whenever the table structure changes. Current supported operations are
 * append and deletion of rows and columns.
 */
export const useValidation = ({
  onValidityUpdate,
  cacheSize = 100,
}: {
  /**
   * Called whenever a validation result for a specific cell is available. The indices refer to the current position of the row/column in the table, which can change when rows/columns are deleted.
   */
  onValidityUpdate: (
    colIdx: number,
    rowIdx: number,
    isValid: boolean,
    validatedValue: string,
  ) => void;
  /**
   * Only configurable for testing.
   */
  cacheSize?: number;
}) => {
  const {
    getColumnIdIndex,
    getRowIdIndex,
    getColumnId,
    getRowId,
    deleteColumnId,
    deleteRowId,
    appendColumnId,
    appendRowId,
  } = useCellIdentifiers();

  const onValidityUpdateIdBased = (
    columnId: string,
    rowId: string,
    isValid: boolean,
    validatedValue: string,
  ) => {
    const columnIdx = getColumnIdIndex(columnId);
    const rowIdx = getRowIdIndex(rowId);
    if (columnIdx === null || rowIdx === null) {
      return; // Column or row was deleted
    }
    onValidityUpdate(columnIdx, rowIdx, isValid, validatedValue);
  };

  // Validation cache composable
  const {
    validateCell: validateCellIdBased,
    validateColumn: validateColumnIdBased,
    validateArea: validateAreaIdBased,
  } = useValidationBackend({
    onValidityUpdate: onValidityUpdateIdBased,
    cacheSize,
  });

  const validateCell = (
    dataType: string,
    value: string,
    colIdx: number,
    rowIdx: number,
  ) => {
    const columnId = getColumnId(colIdx);
    const rowId = getRowId(rowIdx);
    if (columnId === null || rowId === null) {
      return; // should not happen
    }
    validateCellIdBased(dataType, value, columnId, rowId);
  };

  const toIdBasedColumnData = (params: {
    dataType: string;
    values: (string | null)[];
    columnIndex: number;
    rowIndices: number[];
  }) => {
    const columnId = getColumnId(params.columnIndex);
    if (columnId === null) {
      return null; // should not happen
    }
    const rowIds = params.rowIndices
      .map(getRowId)
      .filter((id): id is string => id !== null);
    if (rowIds.length !== params.rowIndices.length) {
      return null; // should not happen
    }
    return {
      dataType: params.dataType,
      values: params.values,
      columnId,
      rowIds,
    };
  };

  const validateColumn = (
    dataType: string,
    values: (string | null)[],
    colIdx: number,
    rowIndices?: number[],
  ) => {
    const idBasedColumnData = toIdBasedColumnData({
      dataType,
      values,
      columnIndex: colIdx,
      rowIndices: rowIndices ?? values.map((_, idx) => idx),
    });
    if (idBasedColumnData === null) {
      return; // should not happen
    }
    validateColumnIdBased(
      idBasedColumnData.dataType,
      idBasedColumnData.values,
      idBasedColumnData.columnId,
      idBasedColumnData.rowIds,
    );
  };

  const validateArea = (
    params: {
      dataType: string;
      values: (string | null)[];
      columnIndex: number;
      rowIndices: number[];
    }[],
  ) => {
    const idBasedColumnsData = params
      .map(toIdBasedColumnData)
      .filter((data): data is NonNullable<typeof data> => data !== null);
    if (idBasedColumnsData.length !== params.length) {
      return; // should not happen
    }
    validateAreaIdBased(idBasedColumnsData);
  };

  const setInitialDimensions = ({
    numRows,
    numColumns,
  }: {
    numRows: number;
    numColumns: number;
  }) => {
    for (let i = 0; i < numColumns; i++) {
      appendColumnId();
    }
    for (let i = 0; i < numRows; i++) {
      appendRowId();
    }
  };

  return {
    validateCell,
    validateColumn,
    validateArea,
    deleteColumn: deleteColumnId,
    deleteRow: deleteRowId,
    appendNewColumn: appendColumnId,
    appendNewRow: appendRowId,
    setInitialDimensions,
  };
};
