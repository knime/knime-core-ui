import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent } from "vue";
import { shallowMount } from "@vue/test-utils";

import { JsonDataService } from "@knime/ui-extension-service";

import TileViewReport from "../TileViewReport.vue";
import { DEFAULT_SETTINGS } from "../composables/useSettings";

let jsonDataServiceMock: {
  initialData: ReturnType<typeof vi.fn>;
  data: ReturnType<typeof vi.fn>;
};

vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: vi.fn(),
}));

const makeInitialData = (rowCount = 3) => ({
  settings: {
    ...DEFAULT_SETTINGS,
    title: "Report Tile View",
    displayedColumns: { selected: ["col-a"] },
  },
  table: {
    columnContentTypes: ["txt"],
    rowCount,
    rows: [["0", "row-0", "val-0"]],
    rowTitles: ["row-0"],
    rowColors: [null],
    displayedColumns: ["col-a"],
  },
});

const makeFullTable = (rowCount = 3) => ({
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
});

const shallowMountComponent = (options = {}) =>
  shallowMount(TileViewReport, {
    global: {
      provide: { getKnimeService: () => ({}) },
      stubs: {
        TileViewDisplay: false,
        ...(options as { stubs?: Record<string, unknown> }).stubs,
      },
    },
  });

describe("TileViewReport.vue", () => {
  beforeEach(() => {
    jsonDataServiceMock = {
      initialData: vi.fn().mockResolvedValue(makeInitialData()),
      data: vi.fn().mockResolvedValue(makeFullTable()),
    };
    (JsonDataService as unknown as ReturnType<typeof vi.fn>).mockImplementation(
      () => jsonDataServiceMock,
    );
  });

  it("fetches all rows on mounted and does not use pagination", async () => {
    shallowMountComponent();
    await vi.dynamicImportSettled();
    await Promise.resolve();

    expect(jsonDataServiceMock.data).toHaveBeenCalledWith({
      method: "getTable",
      options: [
        ["col-a"],
        { specialChoice: "ROW_ID" },
        { specialChoice: "NONE" },
        0,
        3,
        true,
        false,
      ],
    });
  });

  it("emits rendered after data is rendered and all images are loaded", async () => {
    const tileStub = defineComponent({
      name: "Tile",
      emits: ["pending-image", "rendered-image"],
      setup(_, { emit }) {
        emit("pending-image", "img-1");
        return () => null;
      },
    });

    const wrapper = shallowMountComponent({
      stubs: {
        Tile: tileStub,
      },
    });

    await vi.dynamicImportSettled();
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("rendered")).toBeFalsy();
    wrapper.findComponent(tileStub).vm.$emit("rendered-image", "img-1");
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("rendered")).toBeTruthy();
  });
});
