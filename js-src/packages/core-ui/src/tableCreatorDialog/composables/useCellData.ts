import type { ColumnType } from "@knime/knime-ui-table";

import type { ColumnParameters, InitialData } from "../types";

import type { CellData } from "./useTableConfig";

export const useCellData = ({
  dialogInitialData,
  getColumnDataType,
  validator,
  setAdjusted,
}: {
  dialogInitialData: InitialData;
  getColumnDataType: (colIndex: number) => string | null;
  validator: {
    validateCell: (
      dataType: string,
      value: string,
      colIdx: number,
      rowIdx: number,
    ) => void;
    validateColumn: (
      dataType: string,
      values: (string | null)[],
      colIdx: number,
    ) => void;
    validateArea: (
      params: {
        dataType: string;
        values: (string | null)[];
        columnIndex: number;
        rowIndices: number[];
      }[],
    ) => void;
  };
  setAdjusted: () => void;
}) => {
  const setCellValueUnvalidated = (
    colIndex: number,
    rowIndex: number,
    value: CellData,
  ) => {
    const column = dialogInitialData.data.model.columns[colIndex];

    if (column.values?.length <= rowIndex && value === null) {
      return; // No need to set value
    }

    if (!column.values) {
      column.values = [];
    }
    for (let i = column.values.length; i < rowIndex; i++) {
      column.values.push(null);
    }
    column.values[rowIndex] = value ? value.value : null;
    column.isInvalidAt = column.isInvalidAt || [];
    column.isInvalidAt[rowIndex] = undefined; // eslint-disable-line no-undefined
  };

  /**
   * Sets a newly entered cell value and triggers validation for it. The validation does not block the
   * value update and we are optimistic that the value is valid until we know better.
   */
  const setCellValue = (
    colIndex: number,
    rowIndex: number,
    value: CellData,
  ) => {
    setCellValueUnvalidated(colIndex, rowIndex, value);
    setAdjusted();
    const dataType = getColumnDataType(colIndex);
    if (!dataType || value === null) {
      return;
    }
    validator.validateCell(dataType, value.value, colIndex, rowIndex);
  };

  const getCellValue = (colIndex: number, rowIndex: number): CellData => {
    const cols = dialogInitialData.data.model.columns;
    if (!cols || !cols[colIndex]) {
      return null;
    }
    const column = cols[colIndex];
    if (!column.values || rowIndex >= column.values.length) {
      return null;
    }
    const value = column.values[rowIndex];
    if (value === null) {
      return null;
    }
    return {
      value,
      isValid: !column.isInvalidAt?.[rowIndex],
    };
  };

  const revalidateColumn = (colIndex: number, dataType: string) => {
    const cols = dialogInitialData.data.model.columns;
    const column = cols[colIndex];
    validator.validateColumn(dataType, column.values, colIndex);
  };

  const setColumnType = (colIndex: number, type: ColumnType) => {
    const cols = dialogInitialData.data.model.columns;
    const oldType = cols[colIndex].type;

    cols[colIndex].type = type;

    if (oldType !== type && type) {
      revalidateColumn(colIndex, type);
    }
    setAdjusted();
  };

  const setColumnName = (colIndex: number, name: string) => {
    const cols = dialogInitialData.data.model.columns;
    cols[colIndex].name = name;
    setAdjusted();
  };

  const getColumnParams = (colIndex: number): ColumnParameters => {
    const cols = dialogInitialData.data.model.columns;
    return cols[colIndex];
  };

  const setCellArea = (
    colIndex: number,
    rowIndex: number,
    values: CellData[][],
  ) => {
    if (values.length === 0 || values[0].length === 0) {
      return;
    }
    values.forEach((row, i) =>
      row.forEach((cellValue, j) =>
        setCellValueUnvalidated(colIndex + j, rowIndex + i, cellValue),
      ),
    );
    const numColumns = values[0].length;

    const validationParams: {
      dataType: string;
      values: (string | null)[];
      columnIndex: number;
      rowIndices: number[];
    }[] = [];
    for (let colIterIdx = 0; colIterIdx < numColumns; colIterIdx++) {
      const columnDataType = getColumnDataType(colIndex + colIterIdx);
      if (!columnDataType) {
        continue;
      }
      validationParams.push({
        dataType: columnDataType,
        values: values.map((row) => row[colIterIdx]?.value ?? null),
        columnIndex: colIndex + colIterIdx,
        rowIndices: values.map((_, i) => rowIndex + i),
      });
    }

    validator.validateArea(validationParams);
    setAdjusted();
  };

  return {
    setCellValue,
    getCellValue,
    getColumnParams,
    setColumnName,
    setColumnType,
    setCellArea,
  };
};
