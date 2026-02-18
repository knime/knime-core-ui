import { describe, expect, it } from "vitest";

import { useDataType } from "../useDataType";

const createMockInitialData = ({
  columns = [
    { name: "col0", type: "type-a", values: ["v1"] },
    { name: "col1", type: "type-b", values: ["v2"] },
  ],
  possibleValues = [
    { id: "type-a", type: { id: "type-a", text: "Type A" } },
    { id: "type-b", type: { id: "type-b", text: "Type B" } },
  ],
}: {
  columns?: { name: string; type: string; values: (string | null)[] }[];
  possibleValues?: { id: string; type: { id: string; text: string } }[];
} = {}) =>
  ({
    data: { model: { numRows: 1, columns } },
    initialUpdates: [{ values: [{ value: possibleValues }] }],
  }) as any;

describe("useDataType", () => {
  describe("getColumnDataType", () => {
    it("returns the type for a valid column index", () => {
      const { getColumnDataType } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getColumnDataType(0)).toBe("type-a");
      expect(getColumnDataType(1)).toBe("type-b");
    });

    it("returns null for an out-of-bounds index", () => {
      const { getColumnDataType } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getColumnDataType(99)).toBeNull();
    });

    it("returns null when columns array is falsy", () => {
      const data = createMockInitialData();
      data.data.model.columns = null;
      const { getColumnDataType } = useDataType({
        dialogInitialData: data,
      });
      expect(getColumnDataType(0)).toBeNull();
    });

    it("returns null when column has no type property", () => {
      const { getColumnDataType } = useDataType({
        dialogInitialData: createMockInitialData({
          columns: [{ name: "col0", values: ["v"] } as any],
        }),
      });
      expect(getColumnDataType(0)).toBeNull();
    });
  });

  describe("getTypeIdAndText", () => {
    it("returns matching type id and text for a known typeId", () => {
      const { getTypeIdAndText } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getTypeIdAndText("type-a")).toEqual({
        id: "type-a",
        text: "Type A",
      });
    });

    it("returns unknown datatype for an unrecognized typeId", () => {
      const { getTypeIdAndText } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getTypeIdAndText("nonexistent")).toEqual({
        id: "unknown-datatype",
        text: "Unknown datatype",
      });
    });

    it("returns unknown datatype when typeId is undefined", () => {
      const { getTypeIdAndText } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getTypeIdAndText(undefined)).toEqual({
        id: "unknown-datatype",
        text: "Unknown datatype",
      });
    });

    it("returns unknown datatype when typeId is an empty string", () => {
      const { getTypeIdAndText } = useDataType({
        dialogInitialData: createMockInitialData(),
      });
      expect(getTypeIdAndText("")).toEqual({
        id: "unknown-datatype",
        text: "Unknown datatype",
      });
    });
  });
});
