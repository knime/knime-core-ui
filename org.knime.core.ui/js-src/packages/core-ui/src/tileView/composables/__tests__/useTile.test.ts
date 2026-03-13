import { describe, expect, it } from "vitest";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import { useTile } from "../useTile";

const makeProps = (overrides: Partial<Parameters<typeof useTile>[0]> = {}) => ({
  row: [0, "row-1", "val-a", "val-b"] as (
    | { metadata: null; value?: string }
    | string
    | null
  )[],
  title: "Title Value",
  showTitle: true,
  selectionMode: SelectionMode.EDIT,
  isReport: false,
  ...overrides,
});

describe("useTile", () => {
  describe("transformedRow", () => {
    it("maps row cells to { value, isMissing } objects", () => {
      const { transformedRow } = useTile(makeProps());
      expect(transformedRow.value).toHaveLength(4);
      expect(transformedRow.value[2]).toStrictEqual({
        value: "val-a",
        isMissing: false,
      });
    });

    it("marks null cells as missing", () => {
      const { transformedRow } = useTile(
        makeProps({ row: ["0", "row-1", null, "val-b"] }),
      );
      expect(transformedRow.value[2].isMissing).toBeTruthy();
    });

    it("marks metadata cells as missing", () => {
      const { transformedRow } = useTile(
        makeProps({
          row: [
            "0",
            "row-1",
            { metadata: "Some missing value reason" },
            "val-b",
          ],
        }),
      );
      expect(transformedRow.value[2].isMissing).toBeTruthy();
      expect(transformedRow.value[2].value).toBe(
        "Missing Value (Some missing value reason)",
      );
    });
  });

  describe("titleCell", () => {
    it("extracts value and isMissing from the title", () => {
      const { titleCell } = useTile(makeProps({ title: "My Title" }));
      expect(titleCell.value).toStrictEqual({
        value: "My Title",
        isMissing: false,
      });
    });

    it("marks a null title as missing", () => {
      const { titleCell } = useTile(makeProps({ title: null }));
      expect(titleCell.value.isMissing).toBeTruthy();
    });
  });

  describe("showSelection", () => {
    it.each([
      [SelectionMode.EDIT, true],
      [SelectionMode.SHOW, true],
      [SelectionMode.OFF, false],
    ] as const)("selectionMode %s -> %s", (selectionMode, expected) => {
      const { showSelection } = useTile(makeProps({ selectionMode }));
      expect(showSelection.value).toBe(expected);
    });

    it("does not show selection when isReport=true, even if selectionMode is not OFF", () => {
      const { showSelection } = useTile(
        makeProps({ selectionMode: SelectionMode.EDIT, isReport: true }),
      );
      expect(showSelection.value).toBeFalsy();
    });
  });

  describe("enableSelection", () => {
    it.each([
      [SelectionMode.EDIT, true],
      [SelectionMode.SHOW, false],
      [SelectionMode.OFF, false],
    ] as const)("selectionMode %s -> %s", (selectionMode, expected) => {
      const { enableSelection } = useTile(makeProps({ selectionMode }));
      expect(enableSelection.value).toBe(expected);
    });
  });

  describe("rowSpan", () => {
    it("counts title row and data columns", () => {
      // row has 4 entries: index + rowId + 2 data columns => 2 data columns
      // showTitle=true => +1; total = 2 + 1 = 3
      const { rowSpan } = useTile(makeProps());
      expect(rowSpan.value).toBe(3);
    });

    it("excludes title row when showTitle=false", () => {
      const { rowSpan } = useTile(makeProps({ showTitle: false }));
      expect(rowSpan.value).toBe(2);
    });

    it("is unchanged by selectionMode", () => {
      const { rowSpan } = useTile(
        makeProps({ selectionMode: SelectionMode.OFF }),
      );
      expect(rowSpan.value).toBe(3);
    });

    it("counts only data columns when both title and selection are off", () => {
      const { rowSpan } = useTile(
        makeProps({ showTitle: false, selectionMode: SelectionMode.OFF }),
      );
      expect(rowSpan.value).toBe(2);
    });
  });
});
