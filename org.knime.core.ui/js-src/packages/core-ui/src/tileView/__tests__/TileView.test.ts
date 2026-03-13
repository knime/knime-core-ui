import { beforeEach, describe, expect, it, vi } from "vitest";
import { shallowMount } from "@vue/test-utils";

import { TopControls } from "@knime/knime-ui-table";
import type { TableConfig } from "@knime/knime-ui-table";
import {
  CachingSelectionService,
  JsonDataService,
  SharedDataService,
} from "@knime/ui-extension-service";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import TileViewInteractive from "../TileViewInteractive.vue";
import { DEFAULT_SETTINGS } from "../composables/useSettings";

// --- service mocks ---

let jsonDataServiceMock: {
    initialData: ReturnType<typeof vi.fn>;
    data: ReturnType<typeof vi.fn>;
  },
  selectionServiceMock: {
    initialSelection: ReturnType<typeof vi.fn>;
    addOnSelectionChangeCallback: ReturnType<typeof vi.fn>;
    getCachedSelection: ReturnType<typeof vi.fn>;
    add: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  },
  sharedDataServiceMock: { addSharedDataListener: ReturnType<typeof vi.fn> };

vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: vi.fn(),
  CachingSelectionService: vi.fn(),
  SharedDataService: vi.fn(),
}));

const makeInitialData = (rowCount = 2) => ({
  settings: {
    ...DEFAULT_SETTINGS,
    title: "Test View",
    displayedColumns: { selected: ["col-a"] },
  },
  table: {
    columnContentTypes: ["txt"],
    rowCount,
    rows: Array.from({ length: rowCount }, (_, i) => [
      `${i}`,
      `row-${i}`,
      `val-${i}`,
    ]),
    rowTitles: Array.from({ length: rowCount }, (_, i) => `row-${i}`),
    rowColors: Array.from({ length: rowCount }, () => null),
    displayedColumns: ["col-a"],
  },
});

const shallowMountComponent = () =>
  shallowMount(TileViewInteractive, {
    global: {
      provide: { getKnimeService: () => ({}) },
      stubs: {
        TileViewDisplay: false,
      },
    },
  });

describe("TileViewInteractive.vue", () => {
  beforeEach(() => {
    jsonDataServiceMock = {
      initialData: vi.fn().mockResolvedValue(makeInitialData()),
      data: vi.fn().mockResolvedValue(makeInitialData().table),
    };
    selectionServiceMock = {
      initialSelection: vi.fn().mockResolvedValue(undefined),
      addOnSelectionChangeCallback: vi.fn(),
      getCachedSelection: vi.fn().mockReturnValue(new Set()),
      add: vi.fn(),
      remove: vi.fn(),
    };
    sharedDataServiceMock = { addSharedDataListener: vi.fn() };
    (JsonDataService as unknown as ReturnType<typeof vi.fn>).mockImplementation(
      () => jsonDataServiceMock,
    );
    (
      CachingSelectionService as unknown as ReturnType<typeof vi.fn>
    ).mockImplementation(() => selectionServiceMock);
    (
      SharedDataService as unknown as ReturnType<typeof vi.fn>
    ).mockImplementation(() => sharedDataServiceMock);
  });

  it("shows EmptyDataState before data is loaded", () => {
    const wrapper = shallowMountComponent();
    expect(wrapper.find(".tile-view-wrapper-data").exists()).toBeFalsy();
    expect(wrapper.findComponent({ name: "EmptyDataState" }).exists()).toBe(
      true,
    );
  });

  it("shows tile grid after onMounted resolves", async () => {
    const wrapper = shallowMountComponent();
    await vi.dynamicImportSettled();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".tile-view-wrapper-data").exists()).toBeTruthy();
    expect(wrapper.findAllComponents({ name: "Tile" })).toHaveLength(2);
  });

  it("shows tile view when no displayed columns are returned but row titles are available", async () => {
    jsonDataServiceMock.initialData.mockResolvedValue({
      ...makeInitialData(0),
      table: { ...makeInitialData(0).table, displayedColumns: [] },
    });
    const wrapper = shallowMountComponent();
    await vi.dynamicImportSettled();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".tile-view-wrapper-data").exists()).toBeTruthy();
    expect(wrapper.findComponent({ name: "EmptyDataState" }).exists()).toBe(
      false,
    );
  });

  it("shows EmptyDataState when neither displayed columns nor row titles are available", async () => {
    jsonDataServiceMock.initialData.mockResolvedValue({
      ...makeInitialData(0),
      table: {
        ...makeInitialData(0).table,
        displayedColumns: [],
        rowTitles: null,
      },
    });
    const wrapper = shallowMountComponent();
    await vi.dynamicImportSettled();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".tile-view-wrapper-data").exists()).toBeFalsy();
    expect(wrapper.findComponent({ name: "EmptyDataState" }).exists()).toBe(
      true,
    );
  });

  it("shows empty tile grid (no Tile components) when there are no rows but columns exist", async () => {
    jsonDataServiceMock.initialData.mockResolvedValue(makeInitialData(0));
    const wrapper = shallowMountComponent();
    await vi.dynamicImportSettled();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.find(".tile-view-wrapper-data").exists()).toBeTruthy();
    expect(wrapper.findAllComponents({ name: "Tile" })).toHaveLength(0);
    expect(wrapper.findComponent({ name: "EmptyDataState" }).exists()).toBe(
      false,
    );
  });

  it("calls initialData and initialSelection on mount", async () => {
    shallowMountComponent();
    await vi.dynamicImportSettled();
    await Promise.resolve();

    expect(jsonDataServiceMock.initialData).toHaveBeenCalled();
    expect(selectionServiceMock.initialSelection).toHaveBeenCalled();
  });

  it("registers a shared data listener on mount", async () => {
    shallowMountComponent();
    await vi.dynamicImportSettled();
    await Promise.resolve();

    expect(sharedDataServiceMock.addSharedDataListener).toHaveBeenCalled();
  });

  it("registers a selection change callback on mount", async () => {
    shallowMountComponent();
    await vi.dynamicImportSettled();
    await Promise.resolve();

    expect(
      selectionServiceMock.addOnSelectionChangeCallback,
    ).toHaveBeenCalled();
  });

  describe("page navigation", () => {
    const mountAndLoad = async () => {
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();
      return wrapper;
    };

    it("increments page and fetches on next-page", async () => {
      const wrapper = await mountAndLoad();
      jsonDataServiceMock.data.mockClear();

      await wrapper.findComponent(TopControls).vm.$emit("next-page");
      await wrapper.vm.$nextTick();

      expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
        method: "getTable",
        options: [
          ["col-a"],
          { specialChoice: "ROW_ID" },
          { specialChoice: "NONE" },
          10,
          10,
          true,
          false,
        ],
      });
    });

    it("decrements page and fetches on prev-page", async () => {
      const wrapper = await mountAndLoad();
      // advance to page 1 first
      await wrapper.findComponent(TopControls).vm.$emit("next-page");
      await wrapper.vm.$nextTick();
      jsonDataServiceMock.data.mockClear();

      await wrapper.findComponent(TopControls).vm.$emit("prev-page");
      await wrapper.vm.$nextTick();

      expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
        method: "getTable",
        options: [
          ["col-a"],
          { specialChoice: "ROW_ID" },
          { specialChoice: "NONE" },
          0,
          10,
          true,
          false,
        ],
      });
    });
  });

  describe("grid layout", () => {
    it("sets gridTemplateColumns based on tilesPerRow", async () => {
      jsonDataServiceMock.initialData.mockResolvedValue({
        ...makeInitialData(),
        settings: { ...DEFAULT_SETTINGS, tilesPerRow: 4 },
      });
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();

      const grid = wrapper.find(".tile-grid");
      expect(grid.attributes("style")).toContain(
        "grid-template-columns: repeat(4, 1fr)",
      );
    });
  });

  describe("selection", () => {
    it("passes selected state to Tile based on cached selection", async () => {
      selectionServiceMock.getCachedSelection.mockReturnValue(
        new Set(["row-0"]),
      );
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();

      const tiles = wrapper.findAllComponents({ name: "Tile" });
      expect(tiles[0].props("selected")).toBeTruthy();
      expect(tiles[1].props("selected")).toBeFalsy();
    });

    it("passes selectionMode from settings to Tile", async () => {
      jsonDataServiceMock.initialData.mockResolvedValue({
        ...makeInitialData(),
        settings: {
          ...DEFAULT_SETTINGS,
          selectionMode: SelectionMode.SHOW,
        },
      });
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();

      const tile = wrapper.findComponent({ name: "Tile" });
      expect(tile.props("selectionMode")).toBe(SelectionMode.SHOW);
    });
  });

  describe("handleNewSettings", () => {
    const mountAndLoad = async () => {
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();
      return wrapper;
    };

    const getSettingsListener = () =>
      sharedDataServiceMock.addSharedDataListener.mock.calls[0][0] as (
        payload: unknown,
      ) => void;

    const fireSettingsChange = (
      wrapper: ReturnType<typeof shallowMountComponent>,
      overrides = {},
    ) => {
      getSettingsListener()({
        data: {
          view: {
            ...DEFAULT_SETTINGS,
            title: "Test View",
            displayedColumns: { selected: ["col-a"] },
            ...overrides,
          },
        },
      });
    };

    it("resets page to 0 and fetches with clearImageDataCache=true when pageSize changes", async () => {
      const wrapper = await mountAndLoad();
      // Advance to page 1 first
      await wrapper.findComponent(TopControls).vm.$emit("next-page");
      await wrapper.vm.$nextTick();
      jsonDataServiceMock.data.mockClear();

      fireSettingsChange(wrapper, { pageSize: 5 });
      await wrapper.vm.$nextTick();

      expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
        method: "getTable",
        options: [
          ["col-a"],
          { specialChoice: "ROW_ID" },
          { specialChoice: "NONE" },
          0,
          5,
          true,
          false,
        ],
      });
    });

    it("does not fetch when only non-data settings change (title, tilesPerRow)", async () => {
      const wrapper = await mountAndLoad();
      jsonDataServiceMock.data.mockClear();

      fireSettingsChange(wrapper, { title: "New Title", tilesPerRow: 5 });
      await wrapper.vm.$nextTick();

      expect(jsonDataServiceMock.data).not.toHaveBeenCalled();
    });
  });

  describe("onViewSettingsChange", () => {
    const mountAndLoad = async () => {
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();
      return wrapper;
    };

    const getSettingsListener = () =>
      sharedDataServiceMock.addSharedDataListener.mock.calls[0][0] as (
        payload: unknown,
      ) => void;

    it("preserves current displayedColumns.selected when payload has undefined", async () => {
      const wrapper = await mountAndLoad();
      jsonDataServiceMock.data.mockClear();

      // Payload with undefined selected — should fall back to currently loaded ["col-a"]
      getSettingsListener()({
        data: {
          view: {
            ...DEFAULT_SETTINGS,
            displayedColumns: { selected: undefined },
          },
        },
      });
      await wrapper.vm.$nextTick();

      // displayedColumns effectively unchanged → no refetch
      expect(jsonDataServiceMock.data).not.toHaveBeenCalled();
    });
  });

  describe("tableConfig", () => {
    const mountAndLoad = async () => {
      const wrapper = shallowMountComponent();
      await vi.dynamicImportSettled();
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();
      return wrapper;
    };

    it("provides correct pageConfig to TopControls after load", async () => {
      const wrapper = await mountAndLoad();
      const { pageConfig } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;

      expect(pageConfig).toMatchObject({
        currentSize: 2,
        tableSize: 2,
        pageSize: 10,
        currentPage: 1,
        columnCount: 0,
        showTableSize: true,
        showPageControls: true,
        rowLabel: "Showing",
      });
    });

    it("has empty settingsItems when showOnlySelectedRowsConfigurable is false", async () => {
      const wrapper = await mountAndLoad();
      const { settingsItems } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;

      expect(settingsItems).toEqual([]);
    });

    it("includes show-only-selected-rows checkbox when showOnlySelectedRowsConfigurable is true", async () => {
      jsonDataServiceMock.initialData.mockResolvedValue({
        ...makeInitialData(),
        settings: {
          ...DEFAULT_SETTINGS,
          title: "Test View",
          displayedColumns: { selected: ["col-a"] },
          showOnlySelectedRowsConfigurable: true,
          showOnlySelectedRows: false,
        },
      });
      const wrapper = await mountAndLoad();
      const { settingsItems } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;

      expect(settingsItems).toHaveLength(1);
      expect(settingsItems![0].text).toBe("Show only selected rows");
      expect(settingsItems![0].checkbox!.checked).toBe(false);
    });

    it("initialises tableSize from initialData.table.rowCount", async () => {
      // initialData reports 5 total rows
      jsonDataServiceMock.initialData.mockResolvedValue(makeInitialData(5));
      const wrapper = await mountAndLoad();

      const { pageConfig } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;

      // tableSize is captured once from initialData on mount
      expect(pageConfig!.tableSize).toBe(5);
      // currentSize also reflects the initially loaded row count
      expect(pageConfig!.currentSize).toBe(5);
    });

    it("keeps tableSize stable during filtering while currentSize reflects the size of the fetched page", async () => {
      // 10 total rows, checkbox available
      jsonDataServiceMock.initialData.mockResolvedValue({
        ...makeInitialData(10),
        settings: {
          ...DEFAULT_SETTINGS,
          title: "Test View",
          displayedColumns: { selected: ["col-a"] },
          showOnlySelectedRowsConfigurable: true,
          showOnlySelectedRows: false,
        },
      });
      const wrapper = await mountAndLoad();

      // Enabling "show only selected rows" re-fetches and returns only 4 matching rows
      jsonDataServiceMock.data.mockResolvedValue({
        ...makeInitialData(4).table,
        rowCount: 4,
      });

      const { settingsItems } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;
      await settingsItems![0].checkbox!.setBoolean(true);
      await wrapper.vm.$nextTick();
      await wrapper.vm.$nextTick();

      const { pageConfig } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;

      expect(pageConfig!.tableSize).toBe(10); // unchanged from initialData
      expect(pageConfig!.currentSize).toBe(4); // filtered row count
    });

    it("checkbox setBoolean resets page and fetches with showOnlySelectedRows=true", async () => {
      jsonDataServiceMock.initialData.mockResolvedValue({
        ...makeInitialData(),
        settings: {
          ...DEFAULT_SETTINGS,
          title: "Test View",
          displayedColumns: { selected: ["col-a"] },
          showOnlySelectedRowsConfigurable: true,
          showOnlySelectedRows: false,
        },
      });
      const wrapper = await mountAndLoad();
      jsonDataServiceMock.data.mockClear();

      const { settingsItems } = wrapper
        .findComponent(TopControls)
        .props("tableConfig") as TableConfig;
      await settingsItems![0].checkbox!.setBoolean(true);
      await wrapper.vm.$nextTick();

      expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
        method: "getTable",
        options: [
          ["col-a"],
          { specialChoice: "ROW_ID" },
          { specialChoice: "NONE" },
          0,
          10,
          true,
          true,
        ],
      });
    });
  });
});
