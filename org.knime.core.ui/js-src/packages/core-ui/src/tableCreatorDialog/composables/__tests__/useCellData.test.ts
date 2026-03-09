import { describe, expect, it, vi } from "vitest";

import type { ColumnType } from "@knime/knime-ui-table";

import type { ColumnParameters, InitialData } from "@/tableCreatorDialog/types";
import { useCellData } from "../useCellData";
import type { CellData } from "../useTableConfig";

const intType = "IntType" as ColumnType;
const stringType = "StringType" as ColumnType;

const createColumn = (
  overrides: Partial<ColumnParameters> = {},
): ColumnParameters => ({
  name: "col",
  type: stringType,
  values: [],
  ...overrides,
});

const createInitialData = (columns: ColumnParameters[] = [], numRows = 0) =>
  ({
    data: { model: { numRows, columns } },
  }) as InitialData;

const createMocks = () => ({
  validator: {
    validateCell: vi.fn(),
    validateColumn: vi.fn(),
    validateArea: vi.fn(),
  },
  setAdjusted: vi.fn(),
  getColumnDataType: vi.fn(
    (_colIndex: number) => "StringType" as string | null,
  ),
});

const setup = (initialData: InitialData, mocks = createMocks()) => {
  const result = useCellData({
    dialogInitialData: initialData,
    getColumnDataType: mocks.getColumnDataType,
    validator: mocks.validator,
    setAdjusted: mocks.setAdjusted,
  });
  return { ...result, mocks, initialData };
};

describe("useCellData", () => {
  describe("getCellValue", () => {
    it("returns null if columns array is falsy", () => {
      const data = createInitialData();
      (data.data.model as any).columns = undefined;
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 0)).toBeNull();
    });

    it("returns null if column at index does not exist", () => {
      const data = createInitialData([]);
      const { getCellValue } = setup(data);
      expect(getCellValue(5, 0)).toBeNull();
    });

    it("returns null if column.values is falsy", () => {
      const col = createColumn();
      (col as any).values = undefined;
      const data = createInitialData([col]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 0)).toBeNull();
    });

    it("returns null if rowIndex >= values length", () => {
      const data = createInitialData([createColumn({ values: ["a"] })]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 5)).toBeNull();
    });

    it("returns null if value at rowIndex is null", () => {
      const data = createInitialData([createColumn({ values: [null] })]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 0)).toBeNull();
    });

    it("returns CellData with isValid true when isInvalidAt is undefined", () => {
      const data = createInitialData([createColumn({ values: ["hello"] })]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 0)).toEqual({ value: "hello", isValid: true });
    });

    it("returns CellData with isValid false when isInvalidAt marks row as invalid", () => {
      const data = createInitialData([
        createColumn({ values: ["hello"], isInvalidAt: [true] }),
      ]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 0)).toEqual({ value: "hello", isValid: false });
    });

    it("returns isValid true when isInvalidAt has undefined at the row index", () => {
      const data = createInitialData([
        createColumn({
          values: ["a", "b"],
          isInvalidAt: [undefined, undefined],
        }),
      ]);
      const { getCellValue } = setup(data);
      expect(getCellValue(0, 1)).toEqual({ value: "b", isValid: true });
    });
  });

  describe("setCellValue", () => {
    it("sets a cell value and calls setAdjusted and validateCell", () => {
      const data = createInitialData([createColumn({ values: ["old"] })]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, { value: "new", isValid: true });

      expect(data.data.model.columns[0].values[0]).toBe("new");
      expect(mocks.setAdjusted).toHaveBeenCalled();
      expect(mocks.validator.validateCell).toHaveBeenCalledWith(
        "StringType",
        "new",
        0,
        0,
      );
    });

    it("sets value to null when CellData is null", () => {
      const data = createInitialData([createColumn({ values: ["old"] })]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, null);

      expect(data.data.model.columns[0].values[0]).toBeNull();
      expect(mocks.setAdjusted).toHaveBeenCalled();
      // Should NOT call validateCell when value is null
      expect(mocks.validator.validateCell).not.toHaveBeenCalled();
    });

    it("does not validate when dataType is null", () => {
      const data = createInitialData([createColumn({ values: ["old"] })]);
      const mocks = createMocks();
      mocks.getColumnDataType.mockReturnValue(null);
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, { value: "new", isValid: true });

      expect(data.data.model.columns[0].values[0]).toBe("new");
      expect(mocks.setAdjusted).toHaveBeenCalled();
      expect(mocks.validator.validateCell).not.toHaveBeenCalled();
    });

    it("deletes isInvalidAt entry for the row", () => {
      const data = createInitialData([
        createColumn({ values: ["a"], isInvalidAt: [true] }),
      ]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, { value: "b", isValid: true });

      // isInvalidAt[0] should be deleted (becomes undefined via delete operator)
      expect(data.data.model.columns[0].isInvalidAt![0]).toBeUndefined();
    });

    it("fills sparse values with null when setting beyond current length", () => {
      const data = createInitialData([createColumn({ values: [] })]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 3, { value: "far", isValid: true });

      const vals = data.data.model.columns[0].values;
      expect(vals).toEqual([null, null, null, "far"]);
    });

    it("initializes values array if falsy", () => {
      const col = createColumn();
      (col as any).values = undefined;
      const data = createInitialData([col]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, { value: "x", isValid: true });

      expect(data.data.model.columns[0].values[0]).toBe("x");
    });

    it("skips setting null when values length <= rowIndex and value is null", () => {
      const data = createInitialData([createColumn({ values: ["a"] })]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      // rowIndex 5 is beyond current length (1), and value is null => skip
      setCellValue(0, 5, null);

      // Values array should remain unchanged
      expect(data.data.model.columns[0].values).toEqual(["a"]);
      // setAdjusted is still called (it's called outside setCellValueUnvalidated)
      expect(mocks.setAdjusted).toHaveBeenCalled();
    });

    it("initializes isInvalidAt if it was undefined", () => {
      const col = createColumn({ values: ["a"] });
      delete col.isInvalidAt;
      const data = createInitialData([col]);
      const mocks = createMocks();
      const { setCellValue } = setup(data, mocks);

      setCellValue(0, 0, { value: "b", isValid: true });

      expect(data.data.model.columns[0].isInvalidAt).toBeDefined();
    });
  });

  describe("settings column params", () => {
    it("replaces column name and calls setAdjusted", () => {
      const data = createInitialData([
        createColumn({ name: "old", type: stringType, values: ["a"] }),
      ]);
      const mocks = createMocks();
      const { setColumnName } = setup(data, mocks);

      setColumnName(0, "new");

      expect(data.data.model.columns[0].name).toBe("new");
      expect(mocks.setAdjusted).toHaveBeenCalled();
    });

    it("triggers revalidation when type changes", () => {
      const data = createInitialData([
        createColumn({ name: "col1", type: stringType, values: ["1"] }),
      ]);
      const mocks = createMocks();
      const { setColumnType } = setup(data, mocks);

      setColumnType(0, intType);

      expect(data.data.model.columns[0].type).toBe(intType);
      expect(mocks.validator.validateColumn).toHaveBeenCalledWith(
        intType,
        ["1"],
        0,
      );
    });

    it("does not trigger revalidation when type stays the same", () => {
      const data = createInitialData([
        createColumn({ name: "col1", type: stringType, values: ["a"] }),
      ]);
      const mocks = createMocks();
      const { setColumnType } = setup(data, mocks);

      setColumnType(0, stringType);

      expect(data.data.model.columns[0].type).toBe("StringType");
      expect(mocks.validator.validateColumn).not.toHaveBeenCalled();
    });
  });

  describe("getColumnParams", () => {
    it("returns the column at the given index", () => {
      const col = createColumn({
        name: "myCol",
        type: intType,
        values: ["1"],
      });
      const data = createInitialData([col]);
      const { getColumnParams } = setup(data);

      expect(getColumnParams(0)).toBe(col);
    });

    it("returns undefined for non-existent column index", () => {
      const data = createInitialData([]);
      const { getColumnParams } = setup(data);

      expect(getColumnParams(99)).toBeUndefined();
    });
  });

  describe("setCellArea", () => {
    it("sets multiple cell values across columns and rows", () => {
      const data = createInitialData([
        createColumn({ name: "c1", values: ["", ""] }),
        createColumn({ name: "c2", values: ["", ""] }),
      ]);
      const mocks = createMocks();
      const { setCellArea } = setup(data, mocks);

      const values: CellData[][] = [
        [
          { value: "a", isValid: true },
          { value: "b", isValid: true },
        ],
        [
          { value: "c", isValid: true },
          { value: "d", isValid: true },
        ],
      ];
      setCellArea(0, 0, values);

      expect(data.data.model.columns[0].values).toEqual(["a", "c"]);
      expect(data.data.model.columns[1].values).toEqual(["b", "d"]);
      expect(mocks.validator.validateArea).toHaveBeenCalledWith([
        {
          dataType: "StringType",
          values: ["a", "c"],
          columnIndex: 0,
          rowIndices: [0, 1],
        },
        {
          dataType: "StringType",
          values: ["b", "d"],
          columnIndex: 1,
          rowIndices: [0, 1],
        },
      ]);
      expect(mocks.setAdjusted).toHaveBeenCalled();
    });

    it("returns early when values array is empty", () => {
      const data = createInitialData([createColumn()]);
      const mocks = createMocks();
      const { setCellArea } = setup(data, mocks);

      setCellArea(0, 0, []);

      expect(mocks.validator.validateArea).not.toHaveBeenCalled();
      expect(mocks.setAdjusted).not.toHaveBeenCalled();
    });

    it("returns early when first row is empty", () => {
      const data = createInitialData([createColumn()]);
      const mocks = createMocks();
      const { setCellArea } = setup(data, mocks);

      setCellArea(0, 0, [[]]);

      expect(mocks.validator.validateArea).not.toHaveBeenCalled();
      expect(mocks.setAdjusted).not.toHaveBeenCalled();
    });

    it("handles null CellData in the area", () => {
      const data = createInitialData([createColumn({ values: ["old"] })]);
      const mocks = createMocks();
      const { setCellArea } = setup(data, mocks);

      setCellArea(0, 0, [[null]]);

      expect(data.data.model.columns[0].values[0]).toBeNull();
      expect(mocks.validator.validateArea).toHaveBeenCalledWith([
        {
          dataType: "StringType",
          values: [null],
          columnIndex: 0,
          rowIndices: [0],
        },
      ]);
    });

    it("skips columns with null data type in validation params", () => {
      const data = createInitialData([
        createColumn({ values: [""] }),
        createColumn({ values: [""] }),
      ]);
      const mocks = createMocks();
      mocks.getColumnDataType.mockImplementation((colIndex: number) =>
        colIndex === 0 ? "StringType" : null,
      );
      const { setCellArea } = setup(data, mocks);

      setCellArea(0, 0, [
        [
          { value: "a", isValid: true },
          { value: "b", isValid: true },
        ],
      ]);

      // Only column 0 should be in validation params since column 1 has null dataType
      expect(mocks.validator.validateArea).toHaveBeenCalledWith([
        {
          dataType: "StringType",
          values: ["a"],
          columnIndex: 0,
          rowIndices: [0],
        },
      ]);
    });

    it("sets cell area at an offset", () => {
      const data = createInitialData([
        createColumn({ name: "c0", values: ["x", "x", "x"] }),
        createColumn({ name: "c1", values: ["x", "x", "x"] }),
        createColumn({ name: "c2", values: ["x", "x", "x"] }),
      ]);
      const mocks = createMocks();
      const { setCellArea } = setup(data, mocks);

      setCellArea(1, 1, [[{ value: "p", isValid: true }]]);

      // Only column 1, row 1 should be changed
      expect(data.data.model.columns[0].values).toEqual(["x", "x", "x"]);
      expect(data.data.model.columns[1].values[1]).toBe("p");
      expect(data.data.model.columns[2].values).toEqual(["x", "x", "x"]);
    });
  });
});
