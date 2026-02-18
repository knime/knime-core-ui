import { describe, expect, it, vi } from "vitest";

import type { ColumnType } from "@knime/knime-ui-table";

import type { TableCreatorParameters } from "../../types";
import { useDeletion } from "../useDeletion";

const makeData = (
  overrides?: Partial<TableCreatorParameters>,
): TableCreatorParameters => ({
  numRows: 3,
  columns: [
    {
      name: "col0",
      type: "StringType" as ColumnType,
      values: ["a", "b", "c"],
      isInvalidAt: [false, true, false],
    },
    {
      name: "col1",
      type: "IntType" as ColumnType,
      values: ["1", "2", "3"],
      isInvalidAt: [true, false, false],
    },
  ],
  ...overrides,
});

describe("useDeletion", () => {
  describe("deleteColumn", () => {
    it("removes the column at the given index and calls callbacks", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const onDeleteColumn = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
        onDeleteColumn,
      });

      deleteColumn(0);

      expect(data.columns).toHaveLength(1);
      expect(data.columns[0].name).toBe("col1");
      expect(onDeleteColumn).toHaveBeenCalledWith(0);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("removes the last column", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteColumn(1);

      expect(data.columns).toHaveLength(1);
      expect(data.columns[0].name).toBe("col0");
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("does nothing for a negative column index", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const onDeleteColumn = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
        onDeleteColumn,
      });

      deleteColumn(-1);

      expect(data.columns).toHaveLength(2);
      expect(onDeleteColumn).not.toHaveBeenCalled();
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("does nothing for an index equal to columns length (out of bounds)", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const onDeleteColumn = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
        onDeleteColumn,
      });

      deleteColumn(2);

      expect(data.columns).toHaveLength(2);
      expect(onDeleteColumn).not.toHaveBeenCalled();
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("does nothing for a large out-of-bounds index", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteColumn(100);

      expect(data.columns).toHaveLength(2);
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("works without onDeleteColumn callback", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteColumn } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteColumn(0);

      expect(data.columns).toHaveLength(1);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });
  });

  describe("deleteRow", () => {
    it("removes row values and isInvalidAt from all columns and calls callbacks", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const onDeleteRow = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
        onDeleteRow,
      });

      deleteRow(1);

      expect(data.columns[0].values).toEqual(["a", "c"]);
      expect(data.columns[0].isInvalidAt).toEqual([false, false]);
      expect(data.columns[1].values).toEqual(["1", "3"]);
      expect(data.columns[1].isInvalidAt).toEqual([true, false]);
      expect(onDeleteRow).toHaveBeenCalledWith(1);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("removes the first row", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteRow(0);

      expect(data.columns[0].values).toEqual(["b", "c"]);
      expect(data.columns[1].values).toEqual(["2", "3"]);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("removes the last row", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteRow(2);

      expect(data.columns[0].values).toEqual(["a", "b"]);
      expect(data.columns[1].values).toEqual(["1", "2"]);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("does nothing for a negative row index", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const onDeleteRow = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
        onDeleteRow,
      });

      deleteRow(-1);

      expect(data.columns[0].values).toEqual(["a", "b", "c"]);
      expect(data.columns[1].values).toEqual(["1", "2", "3"]);
      expect(onDeleteRow).not.toHaveBeenCalled();
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("handles columns without isInvalidAt", () => {
      const data: TableCreatorParameters = {
        numRows: 2,
        columns: [
          {
            name: "col0",
            type: "StringType" as ColumnType,
            values: ["x", "y"],
          },
        ],
      };
      const setAdjusted = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteRow(0);

      expect(data.columns[0].values).toEqual(["y"]);
      expect(data.columns[0].isInvalidAt).toBeUndefined();
      expect(setAdjusted).toHaveBeenCalledOnce();
    });

    it("works without onDeleteRow callback", () => {
      const data = makeData();
      const setAdjusted = vi.fn();
      const { deleteRow } = useDeletion({
        getData: () => data,
        setAdjusted,
      });

      deleteRow(0);

      expect(data.columns[0].values).toHaveLength(2);
      expect(setAdjusted).toHaveBeenCalledOnce();
    });
  });
});
