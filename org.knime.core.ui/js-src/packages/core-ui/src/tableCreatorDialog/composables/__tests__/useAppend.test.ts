import { describe, expect, it, vi } from "vitest";

import type { ColumnType } from "@knime/knime-ui-table";

import type { TableCreatorParameters } from "../../types";
import { useAppend } from "../useAppend";

describe("useAppend", () => {
  const defaultColumnType = "StringValue" as ColumnType;

  const createData = (
    overrides?: Partial<TableCreatorParameters>,
  ): TableCreatorParameters => ({
    numRows: 0,
    columns: [],
    ...overrides,
  });

  const setup = ({
    data = createData(),
    onAppendColumn,
    onAppendRow,
  }: {
    data?: TableCreatorParameters;
    onAppendColumn?: () => void;
    onAppendRow?: () => void;
  } = {}) => {
    const getData = vi.fn(() => data);
    const setAdjusted = vi.fn();
    const result = useAppend({
      defaultColumnType,
      getData,
      setAdjusted,
      onAppendColumn,
      onAppendRow,
    });
    return { ...result, getData, setAdjusted, data };
  };

  describe("appendColumn", () => {
    it("adds a column with the default name and type to empty data", () => {
      const { appendColumn, data } = setup();
      appendColumn();
      expect(data.columns).toHaveLength(1);
      expect(data.columns[0]).toEqual({
        name: "Column 1",
        type: defaultColumnType,
        values: [],
      });
    });

    it("generates a unique default name when columns already exist", () => {
      const data = createData({
        columns: [{ name: "Column 1", type: defaultColumnType, values: [] }],
      });
      const { appendColumn } = setup({ data });
      appendColumn();
      expect(data.columns).toHaveLength(2);
      expect(data.columns[1].name).toBe("Column 2");
    });

    it("calls setAdjusted", () => {
      const { appendColumn, setAdjusted } = setup();
      appendColumn();
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("calls onAppendColumn callback when provided", () => {
      const onAppendColumn = vi.fn();
      const { appendColumn } = setup({ onAppendColumn });
      appendColumn();
      expect(onAppendColumn).toHaveBeenCalledOnce();
    });

    it("does not crash when onAppendColumn is not provided", () => {
      const { appendColumn } = setup();
      expect(() => appendColumn()).not.toThrow();
    });
  });

  describe("appendRow", () => {
    it("pushes null to each column's values", () => {
      const data = createData({
        columns: [
          { name: "Column 1", type: defaultColumnType, values: ["a"] },
          { name: "Column 2", type: defaultColumnType, values: ["b"] },
        ],
      });
      const { appendRow } = setup({ data });
      appendRow();
      expect(data.columns[0].values).toEqual(["a", null]);
      expect(data.columns[1].values).toEqual(["b", null]);
    });

    it("handles empty columns array", () => {
      const data = createData({ columns: [] });
      const { appendRow } = setup({ data });
      expect(() => appendRow()).not.toThrow();
    });

    it("calls setAdjusted", () => {
      const data = createData({
        columns: [{ name: "Column 1", type: defaultColumnType, values: [] }],
      });
      const { appendRow, setAdjusted } = setup({ data });
      appendRow();
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("calls onAppendRow callback when provided", () => {
      const onAppendRow = vi.fn();
      const data = createData({
        columns: [{ name: "Column 1", type: defaultColumnType, values: [] }],
      });
      const { appendRow } = setup({ data, onAppendRow });
      appendRow();
      expect(onAppendRow).toHaveBeenCalledOnce();
    });

    it("does not crash when onAppendRow is not provided", () => {
      const data = createData({
        columns: [{ name: "Column 1", type: defaultColumnType, values: [] }],
      });
      const { appendRow } = setup({ data });
      expect(() => appendRow()).not.toThrow();
    });
  });
});
