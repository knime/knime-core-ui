import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";

import type { JsonDataService } from "@knime/ui-extension-service";

import { DEFAULT_SETTINGS } from "../useSettings";
import { type TileViewTableData, useTableData } from "../useTableData";

const makeEmptyTable = (): TileViewTableData => ({
  columnContentTypes: [],
  rowCount: 0,
  rows: [],
  rowTitles: [],
  rowColors: [],
  displayedColumns: [],
});

const makeResultTable = (): TileViewTableData => ({
  columnContentTypes: ["txt"],
  rowCount: 3,
  rows: [
    ["0", "row-1", "a"],
    ["1", "row-2", "b"],
    ["2", "row-3", "c"],
  ],
  rowTitles: ["row-1", "row-2", "row-3"],
  rowColors: ["#ff0000", "#00ff00", "#0000ff"],
  displayedColumns: ["col-a"],
});

describe("useTableData", () => {
  let jsonDataServiceMock: { data: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    jsonDataServiceMock = {
      data: vi.fn().mockResolvedValue(makeResultTable()),
    };
  });

  const mount = (pageOverride = 0) => {
    const settings = ref({ ...DEFAULT_SETTINGS });
    const currentPage = ref(pageOverride);
    const comp = useTableData(
      jsonDataServiceMock as unknown as JsonDataService,
      settings,
      currentPage,
    );
    return { comp, settings, currentPage };
  };

  it("initialises table to an empty default", () => {
    const { comp } = mount();
    expect(comp.table.value).toStrictEqual(makeEmptyTable());
  });

  describe("fetchTable", () => {
    it("calls jsonDataService.data with getTable method and mapped options", async () => {
      const { comp, settings } = mount();
      await comp.fetchTable();

      expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
        method: "getTable",
        options: [
          [],
          settings.value.titleColumn,
          settings.value.colorColumn,
          0, // page 0 * pageSize 10
          10,
          false,
          false,
        ],
      });
    });

    it("updates table.value with fetched data", async () => {
      const { comp } = mount();
      await comp.fetchTable();
      expect(comp.table.value).toStrictEqual(makeResultTable());
    });

    it("computes fromIndex from currentPage and pageSize", async () => {
      const { comp } = mount(2);
      await comp.fetchTable();
      // page 2, pageSize 10 => fromIndex 20
      const callArgs = jsonDataServiceMock.data.mock.calls[0][0];
      expect(callArgs.options[3]).toBe(20);
    });

    it("respects overrides passed to fetchTable", async () => {
      const { comp } = mount();
      await comp.fetchTable({
        clearImageDataCache: true,
      });
      const callArgs = jsonDataServiceMock.data.mock.calls[0][0];
      expect(callArgs.options[5]).toBeTruthy(); // clearImageDataCache
    });

    it("falls back to table.displayedColumns when settings has no selected columns", async () => {
      const { comp, settings } = mount();
      settings.value.displayedColumns.selected = undefined;
      comp.table.value.displayedColumns = ["fallback-col"];
      await comp.fetchTable();
      const callArgs = jsonDataServiceMock.data.mock.calls[0][0];
      expect(callArgs.options[0]).toStrictEqual(["fallback-col"]);
    });

    it("passes showOnlySelectedRows from settings", async () => {
      const { comp, settings } = mount();
      settings.value.showOnlySelectedRows = true;
      await comp.fetchTable();
      const callArgs = jsonDataServiceMock.data.mock.calls[0][0];
      expect(callArgs.options[6]).toBeTruthy();
    });

    it("override fromIndex takes precedence over computed value", async () => {
      const { comp } = mount(5);
      await comp.fetchTable({ fromIndex: 0 });
      const callArgs = jsonDataServiceMock.data.mock.calls[0][0];
      expect(callArgs.options[3]).toBe(0);
    });
  });
});
