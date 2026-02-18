import { beforeEach, describe, expect, it, vi } from "vitest";

import { type TableUI } from "@knime/knime-ui-table";

import type TableCreatorLayout from "@/tableCreatorDialog/TableCreatorLayout.vue";
import type ColumnHeaderInput from "@/tableCreatorDialog/components/ColumnHeaderInput.vue";
import { getUniqueColumnNameWithinArray } from "../../utils/columnNaming";
import { useSelection } from "../useSelection";

type TableUIMethods = {
  refocusSelection: () => void;
  updateCellSelection: (
    rect: { minX: number; minY: number; maxX: number; maxY: number },
    rectId?: number | null,
  ) => void;
  focusHeaderCell: (columnIndex: number) => void;
};
type ColumnHeaderInputMethods = {
  focusColumnNameInput: (initialValue?: string) => void;
};
type TableCreatorLayoutMethods = {
  showPanelContent: () => void;
};

type CheckType<TMethods, _TComponent extends TMethods> = TMethods;

const mockTableComponent: CheckType<
  TableUIMethods,
  InstanceType<typeof TableUI>
> = {
  refocusSelection: vi.fn(),
  updateCellSelection: vi.fn(),
  focusHeaderCell: vi.fn(),
};
const mockColumnHeaderInput: CheckType<
  ColumnHeaderInputMethods,
  InstanceType<typeof ColumnHeaderInput>
> = {
  focusColumnNameInput: vi.fn(),
};

const mockTableCreatorLayout: CheckType<
  TableCreatorLayoutMethods,
  InstanceType<typeof TableCreatorLayout>
> = {
  showPanelContent: vi.fn(),
};

let onMountedCallback: Function | null = null;

const testTableRef = "tableRefTest";
const testTableCreatorLayoutRef = "tableCreatorLayoutRefTest";
const testColumnHeaderInputRef = "columnHeaderInputTest";

vi.mock("vue", async () => {
  const actual = await vi.importActual("vue");
  return {
    ...actual,
    useTemplateRef: vi.fn((name: string) => {
      if (name === testTableRef) {
        return { value: mockTableComponent };
      }
      if (name === testColumnHeaderInputRef) {
        return { value: mockColumnHeaderInput };
      }
      if (name === testTableCreatorLayoutRef) {
        return { value: mockTableCreatorLayout };
      }
      return { value: null };
    }),
    onMounted: vi.fn((cb: Function) => {
      onMountedCallback = cb;
    }),
  };
});

vi.mock("../../utils/columnNaming", () => ({
  getUniqueColumnNameWithinArray: vi.fn(
    (names: string[], idx: number) => `${names[idx]}_unique`,
  ),
}));
vi.mock("@knime/knime-ui-table", () => ({ TableUI: {} }));
vi.mock("../../components/ColumnHeaderInput.vue", () => ({ default: {} }));

const createColumns = (...names: string[]) =>
  names.map((name) => ({ name, type: "String" as any, values: [] }));

const setup = ({
  columns = createColumns("A", "B"),
  numRows = 3,
}: { columns?: ReturnType<typeof createColumns>; numRows?: number } = {}) => {
  const setAdjusted = vi.fn();
  onMountedCallback = null;
  const result = useSelection({
    tableRef: testTableRef,
    tableCreatorLayoutRef: testTableCreatorLayoutRef,
    columnHeaderInputRef: testColumnHeaderInputRef,
    getColumns: () => columns,
    getNumRows: () => numRows,
    setAdjusted,
  });
  return { ...result, setAdjusted, columns };
};

describe("useSelection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("onCellPositionChange", () => {
    it("updates selectedColumnIndex and selectedRowIndex", () => {
      const { onCellPositionChange, selectedColumnIndex, selectedRowIndex } =
        setup();
      onCellPositionChange({ x: 2, y: 5 });
      expect(selectedColumnIndex.value).toBe(2);
      expect(selectedRowIndex.value).toBe(5);
    });

    it("sets indices to -1 when position is null", () => {
      const { onCellPositionChange, selectedColumnIndex, selectedRowIndex } =
        setup();
      onCellPositionChange({ x: 1, y: 1 });
      onCellPositionChange(null);
      expect(selectedColumnIndex.value).toBe(-1);
      expect(selectedRowIndex.value).toBe(-1);
    });
  });

  describe("selectedColumnIndex", () => {
    it("returns -1 when no selection", () => {
      const { selectedColumnIndex } = setup();
      expect(selectedColumnIndex.value).toBe(-1);
    });
  });

  describe("selectedRowIndex", () => {
    it("returns -1 when no selection", () => {
      const { selectedRowIndex } = setup();
      expect(selectedRowIndex.value).toBe(-1);
    });
  });

  describe("uniquifyColumnNames", () => {
    it("renames column when header is selected (y=-1, x>=0) and name is not unique", () => {
      const columns = createColumns("A", "A");
      const { onCellPositionChange, uniquifyColumnNames, setAdjusted } = setup({
        columns,
      });
      // Select header cell of column 1 (y = -1 means header)
      onCellPositionChange({ x: 1, y: -1 });
      uniquifyColumnNames();
      expect(getUniqueColumnNameWithinArray).toHaveBeenCalledWith(
        ["A", "A"],
        1,
      );
      // The mock returns names[idx] + "_unique" = "A_unique"
      expect(columns[1].name).toBe("A_unique");
      expect(setAdjusted).toHaveBeenCalled();
    });

    it("skips renaming when name is already unique", () => {
      const columns = createColumns("A", "B");
      const { onCellPositionChange, uniquifyColumnNames, setAdjusted } = setup({
        columns,
      });
      // Mock returns "B_unique" but let's make it return the same name
      vi.mocked(getUniqueColumnNameWithinArray).mockReturnValueOnce("B");
      onCellPositionChange({ x: 1, y: -1 });
      uniquifyColumnNames();
      expect(columns[1].name).toBe("B");
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("skips when a row is selected (y >= 0)", () => {
      const columns = createColumns("A", "B");
      const { onCellPositionChange, uniquifyColumnNames, setAdjusted } = setup({
        columns,
      });
      onCellPositionChange({ x: 1, y: 0 });
      uniquifyColumnNames();
      expect(getUniqueColumnNameWithinArray).not.toHaveBeenCalled();
      expect(setAdjusted).not.toHaveBeenCalled();
    });

    it("skips when no cell is selected (both -1)", () => {
      const { uniquifyColumnNames, setAdjusted } = setup();
      uniquifyColumnNames();
      expect(getUniqueColumnNameWithinArray).not.toHaveBeenCalled();
      expect(setAdjusted).not.toHaveBeenCalled();
    });
  });

  describe("refocusTable", () => {
    it("calls refocusSelection when target equals currentTarget", () => {
      const { refocusTable } = setup();
      const el = document.createElement("div");
      refocusTable({ target: el, currentTarget: el } as unknown as FocusEvent);
      expect(mockTableComponent.refocusSelection).toHaveBeenCalled();
    });

    it("does NOT call refocusSelection when target differs from currentTarget", () => {
      const { refocusTable } = setup();
      refocusTable({
        target: document.createElement("div"),
        currentTarget: document.createElement("span"),
      } as unknown as FocusEvent);
      expect(mockTableComponent.refocusSelection).not.toHaveBeenCalled();
    });
  });

  describe("onHeaderCellStartEditing", () => {
    it("calls focusColumnNameInput with initialValue", async () => {
      const { onHeaderCellStartEditing } = setup();
      await onHeaderCellStartEditing(0, "hello");
      expect(mockTableCreatorLayout.showPanelContent).toHaveBeenCalled();
      expect(mockColumnHeaderInput.focusColumnNameInput).toHaveBeenCalledWith(
        "hello",
      );
    });

    it("calls focusColumnNameInput without initialValue", async () => {
      const { onHeaderCellStartEditing } = setup();
      await onHeaderCellStartEditing(2);
      expect(mockColumnHeaderInput.focusColumnNameInput).toHaveBeenCalledWith(
        undefined,
      );
    });
  });

  describe("focusHeaderCell", () => {
    it("calls tableComponent focusHeaderCell with column index", () => {
      const { focusHeaderCell } = setup();
      focusHeaderCell(3);
      expect(mockTableComponent.focusHeaderCell).toHaveBeenCalledWith(3);
    });
  });

  describe("focusCell", () => {
    it("calls updateCellSelection with single-cell rect", () => {
      const { focusCell } = setup();
      focusCell(2, 4);
      expect(mockTableComponent.updateCellSelection).toHaveBeenCalledWith(
        { minX: 2, minY: 4, maxX: 2, maxY: 4 },
        undefined,
      );
    });

    it("passes rectId when provided", () => {
      const { focusCell } = setup();
      focusCell(1, 3, 42);
      expect(mockTableComponent.updateCellSelection).toHaveBeenCalledWith(
        { minX: 1, minY: 3, maxX: 1, maxY: 3 },
        42,
      );
    });

    it("passes null rectId when explicitly null", () => {
      const { focusCell } = setup();
      focusCell(0, 0, null);
      expect(mockTableComponent.updateCellSelection).toHaveBeenCalledWith(
        { minX: 0, minY: 0, maxX: 0, maxY: 0 },
        null,
      );
    });

    it("selects first cell in column", () => {
      const { selectFirstCellInColumn } = setup({ numRows: 5 });
      selectFirstCellInColumn(2);
      expect(mockTableComponent.updateCellSelection).toHaveBeenCalledWith(
        { minX: 2, minY: 0, maxX: 2, maxY: 0 },
        undefined,
      );
    });

    it("does not select first cell in column when no rows", () => {
      const { selectFirstCellInColumn } = setup({ numRows: 0 });
      selectFirstCellInColumn(2);
      expect(mockTableComponent.updateCellSelection).not.toHaveBeenCalled();
    });
  });

  describe("onMounted", () => {
    it("calls updateCellSelection when columns and rows exist", async () => {
      setup({ columns: createColumns("A"), numRows: 1 });
      expect(onMountedCallback).not.toBeNull();
      await onMountedCallback!();
      expect(mockTableComponent.updateCellSelection).toHaveBeenCalledWith({
        minX: 0,
        minY: 0,
        maxX: 0,
        maxY: 0,
      });
    });

    it("does NOT call updateCellSelection when columns are empty", async () => {
      setup({ columns: [], numRows: 1 });
      await onMountedCallback!();
      expect(mockTableComponent.updateCellSelection).not.toHaveBeenCalled();
    });

    it("does NOT call updateCellSelection when numRows is 0", async () => {
      setup({ columns: createColumns("A"), numRows: 0 });
      await onMountedCallback!();
      expect(mockTableComponent.updateCellSelection).not.toHaveBeenCalled();
    });
  });
});
