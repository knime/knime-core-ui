import { describe, expect, it, vi } from "vitest";

import type { InitialData } from "../../types";
import { useTableConfig } from "../useTableConfig";

const createInitialData = (columns: any): InitialData =>
  ({
    data: { model: { columns } },
  }) as unknown as InitialData;

describe("useTableConfig", () => {
  const twoColumns = [
    { name: "Name", type: "String", values: ["Alice", "Bob"] },
    { name: "Age", type: "Number (integer)", values: ["30", "25"] },
  ];

  describe("dataConfig", () => {
    it("maps columns to columnConfigs with correct structure", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      const configs = dataConfig.value.columnConfigs;
      expect(configs).toHaveLength(2);

      expect(configs[0]).toMatchObject({
        header: "Name",
        subHeader: "String",
        size: 150,
        type: "String",
        key: "col0",
        id: "col0",
        hasSlotContent: true,
        noPadding: true,
        noPaddingLeft: true,
        editable: true,
        deletable: true,
      });

      expect(configs[1]).toMatchObject({
        header: "Age",
        subHeader: "Number (integer)",
        key: "col1",
        id: "col1",
      });
    });

    it("sets rowConfig with correct rowHeight", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      expect(dataConfig.value.rowConfig).toEqual({ rowHeight: 40 });
    });

    it("formatter returns string representation of a value", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      const formatter = dataConfig.value.columnConfigs[0].formatter!;
      expect(formatter(42)).toBe("42");
      expect(formatter("hello")).toBe("hello");
      expect(formatter(0)).toBe("0");
      expect(formatter(false)).toBe("false");
    });

    it("formatter returns empty string for null and undefined", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      const formatter = dataConfig.value.columnConfigs[0].formatter!;
      expect(formatter(null)).toBe("");
      expect(formatter(undefined)).toBe("");
    });

    it("returns empty columnConfigs for empty columns array", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData([]),
      });

      expect(dataConfig.value.columnConfigs).toEqual([]);
    });

    it("returns empty columnConfigs when columns is not an array", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(undefined),
      });

      expect(dataConfig.value.columnConfigs).toEqual([]);
    });

    it("returns empty columnConfigs when columns is null", () => {
      const { dataConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(null),
      });

      expect(dataConfig.value.columnConfigs).toEqual([]);
    });
  });

  describe("tableData", () => {
    it("builds rows by calling getCellValue for each cell", () => {
      const getCellValue = vi.fn(
        (colIndex: number, rowIndex: number) =>
          ({
            value: `r${rowIndex}c${colIndex}`,
            isValid: true,
          }) as const,
      );

      const { tableData } = useTableConfig({
        getCellValue,
        getNumRows: () => 2,
        dialogInitialData: createInitialData(twoColumns),
      });

      const [rows] = tableData.value;
      expect(rows).toHaveLength(2);
      expect(rows[0]).toEqual({
        col0: { value: "r0c0", isValid: true },
        col1: { value: "r0c1", isValid: true },
      });
      expect(rows[1]).toEqual({
        col0: { value: "r1c0", isValid: true },
        col1: { value: "r1c1", isValid: true },
      });

      // getCellValue called for each cell: 2 cols x 2 rows = 4
      expect(getCellValue).toHaveBeenCalledTimes(4);
    });

    it("returns single group with empty rows array when numRows is 0", () => {
      const { tableData } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      expect(tableData.value).toEqual([[]]);
    });

    it("returns rows with no keys when columns is empty", () => {
      const { tableData } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 2,
        dialogInitialData: createInitialData([]),
      });

      const [rows] = tableData.value;
      expect(rows).toHaveLength(2);
      expect(rows[0]).toEqual({});
      expect(rows[1]).toEqual({});
    });
  });

  describe("tableConfig", () => {
    it("returns correct static config values", () => {
      const { tableConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 0,
        dialogInitialData: createInitialData(twoColumns),
      });

      const config = tableConfig.value;
      expect(config.showSelection).toBe(false);
      expect(config.showCollapser).toBe(false);
      expect(config.showPopovers).toBe(false);
      expect(config.showColumnFilters).toBe(false);
      expect(config.showBottomControls).toBe(false);
      expect(config.subMenuItems).toEqual([]);
      expect(config.groupSubMenuItems).toEqual([]);
      expect(config.enableRowDeletion).toBe(true);
      expect(config.enableCellSelection).toBe(true);
      expect(config.enableHeaderCellSelection).toBe(true);
      expect(config.enableVirtualScrolling).toBe(true);
      expect(config.showNewColumnAndRowButton).toBe(true);
      expect(config.enableColumnResizing).toBe(false);
    });

    it("sets pageConfig based on numRows and column count", () => {
      const { tableConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 5,
        dialogInitialData: createInitialData(twoColumns),
      });

      expect(tableConfig.value.pageConfig).toEqual({
        currentSize: 5,
        tableSize: 5,
        pageSize: 5,
        currentPage: 1,
        columnCount: 2,
        showTableSize: true,
        showPageControls: false,
      });
    });

    it("sets columnCount to 0 when columns is non-array", () => {
      const { tableConfig } = useTableConfig({
        getCellValue: vi.fn(),
        getNumRows: () => 3,
        dialogInitialData: createInitialData("not-an-array"),
      });

      expect(tableConfig.value.pageConfig?.columnCount).toBe(0);
      expect(tableConfig.value.pageConfig?.currentSize).toBe(3);
    });
  });
});
