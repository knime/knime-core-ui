import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";

import type { CachingSelectionService } from "@knime/ui-extension-service";

import { useSelection } from "../useSelection";
import { DEFAULT_SETTINGS } from "../useSettings";
import type { TileViewTableData } from "../useTableData";

const makeTable = (rowIds: string[]): TileViewTableData => ({
  columnContentTypes: [],
  rowCount: rowIds.length,
  rows: rowIds.map((id, i) => [`${i}`, id, `val-${i}`]),
  rowTitles: rowIds,
  rowColors: [],
  displayedColumns: [],
});

describe("useSelection", () => {
  let cachedSelection: Set<string>,
    selectionServiceMock: CachingSelectionService,
    fetchTableMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    cachedSelection = new Set();
    selectionServiceMock = {
      getCachedSelection: vi.fn(() => cachedSelection),
      add: vi.fn(),
      remove: vi.fn(),
    } as unknown as CachingSelectionService;
    fetchTableMock = vi.fn().mockResolvedValue(undefined);
  });

  const mount = (rowIds: string[] = ["row-1", "row-2", "row-3"]) => {
    const table = ref(makeTable(rowIds));
    const settings = ref({ ...DEFAULT_SETTINGS });
    const currentPage = ref(0);
    const comp = useSelection(
      selectionServiceMock,
      table,
      settings,
      currentPage,
      fetchTableMock,
    );
    return { comp, table, settings, currentPage };
  };

  it("initialises selection as an empty array", () => {
    const { comp } = mount();
    expect(comp.selection.value).toStrictEqual([]);
  });

  describe("transformSelection", () => {
    it("maps rows to false when nothing is selected", () => {
      const { comp } = mount();
      comp.transformSelection();
      expect(comp.selection.value).toStrictEqual([false, false, false]);
    });

    it("maps rows to true when they are in the cached selection", () => {
      cachedSelection.add("row-1");
      cachedSelection.add("row-3");
      const { comp } = mount();
      comp.transformSelection();
      expect(comp.selection.value).toStrictEqual([true, false, true]);
    });
  });

  describe("onSelectionChange", () => {
    it("calls transformSelection without fetching when showOnlySelectedRows=false", async () => {
      const { comp } = mount();
      await comp.onSelectionChange();
      expect(fetchTableMock).not.toHaveBeenCalled();
      expect(selectionServiceMock.getCachedSelection).toHaveBeenCalled();
    });

    it("re-fetches and resets page when showOnlySelectedRows=true", async () => {
      const { comp, settings, currentPage } = mount();
      settings.value.showOnlySelectedRows = true;
      currentPage.value = 3;
      await comp.onSelectionChange();
      expect(currentPage.value).toBe(0);
      expect(fetchTableMock).toHaveBeenCalledWith({
        clearImageDataCache: true,
        fromIndex: 0,
      });
      expect(selectionServiceMock.getCachedSelection).toHaveBeenCalled();
    });
  });

  describe("updateSelection", () => {
    it("calls selectionService.add when selected=true", () => {
      const { comp } = mount();
      comp.updateSelection("row-2", true);
      expect(selectionServiceMock.add).toHaveBeenCalledWith(["row-2"]);
      expect(selectionServiceMock.remove).not.toHaveBeenCalled();
    });

    it("calls selectionService.remove when selected=false", () => {
      const { comp } = mount();
      comp.updateSelection("row-2", false);
      expect(selectionServiceMock.remove).toHaveBeenCalledWith(["row-2"]);
      expect(selectionServiceMock.add).not.toHaveBeenCalled();
    });
  });
});
