import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";

import { SelectionMode } from "@/tableView/types/ViewSettings";
import Tile from "../TileCell/Tile.vue";

vi.mock("../TileCell/TileCell.vue", () => ({
  default: {
    name: "TileCell",
    template: '<div class="tile-cell-stub" />',
    props: [
      "cell",
      "columnName",
      "displayColumnHeaders",
      "contentType",
      "tileWidth",
      "isResizeActive",
      "isReport",
    ],
  },
}));

vi.mock("@knime/styles/img/icons/circle-help.svg", () => ({
  default: { template: '<svg class="circle-help-icon-stub" />' },
}));

const defaultProps: InstanceType<typeof Tile>["$props"] = {
  rowIndex: 0,
  selected: false,
  row: ["0", "row-1", "col-a-value", "col-b-value"],
  title: "Row Title",
  showTitle: true,
  columns: ["Column A", "Column B"],
  displayColumnHeaders: true,
  color: null,
  selectionMode: SelectionMode.EDIT,
  columnContentTypes: ["txt", "txt"] as any[],
  tileWidth: 200,
  isResizeActive: false,
  isReport: false,
};

const mountComponent = (propsOverrides: Partial<typeof defaultProps> = {}) =>
  mount(Tile, { props: { ...defaultProps, ...propsOverrides } });

describe("Tile.vue", () => {
  it("renders the .tile container", () => {
    const wrapper = mountComponent();
    expect(wrapper.find(".tile").exists()).toBeTruthy();
  });

  describe("title section", () => {
    it("shows the title when showTitle=true", () => {
      const wrapper = mountComponent({ showTitle: true, title: "My Title" });
      expect(wrapper.find(".tile-title").exists()).toBeTruthy();
      expect(wrapper.find(".tile-title-text").text()).toBe("My Title");
    });

    it("hides the title when showTitle=false", () => {
      const wrapper = mountComponent({ showTitle: false });
      expect(wrapper.find(".tile-title").exists()).toBeFalsy();
    });

    it("shows missing value icon when title is null", () => {
      const wrapper = mountComponent({ showTitle: true, title: null });
      expect(wrapper.find(".missing-value-icon").exists()).toBeTruthy();
      expect(wrapper.find(".tile-title-text").exists()).toBeFalsy();
    });
  });

  describe("tile cells", () => {
    it("renders a TileCell for each data column (skipping row index and row id)", () => {
      const wrapper = mountComponent();
      // row has [index, rowId, col-a, col-b] => 2 data cells
      expect(wrapper.findAll(".tile-cell-stub")).toHaveLength(2);
    });

    it("renders no tile cells for a row with only index and rowId", () => {
      const wrapper = mountComponent({ row: ["0", "row-1"], columns: [] });
      expect(wrapper.findAll(".tile-cell-stub")).toHaveLength(0);
    });
  });

  describe("cSS classes", () => {
    it("applies .selected class when selected=true and selectionMode is EDIT", () => {
      const wrapper = mountComponent({ selected: true });
      expect(wrapper.find(".tile").classes()).toContain("selected");
    });

    it("does not apply .selected class when selected=false", () => {
      const wrapper = mountComponent({ selected: false });
      expect(wrapper.find(".tile").classes()).not.toContain("selected");
    });

    it("does not apply .selected class when selectionMode is OFF even if selected=true", () => {
      const wrapper = mountComponent({
        selected: true,
        selectionMode: SelectionMode.OFF,
      });
      expect(wrapper.find(".tile").classes()).not.toContain("selected");
    });

    it("adds selection-enabled class only in EDIT mode", () => {
      const editable = mountComponent({ selectionMode: SelectionMode.EDIT });
      expect(editable.find(".tile").classes()).toContain("selection-enabled");

      const readOnly = mountComponent({ selectionMode: SelectionMode.SHOW });
      expect(readOnly.find(".tile").classes()).not.toContain(
        "selection-enabled",
      );
    });

    it("applies .colored class when color is set", () => {
      const wrapper = mountComponent({ color: "#ff0000" });
      expect(wrapper.find(".tile").classes()).toContain("colored");
    });

    it("does not apply .colored class when color is null", () => {
      const wrapper = mountComponent({ color: null });
      expect(wrapper.find(".tile").classes()).not.toContain("colored");
    });
  });

  describe("tile click handler", () => {
    it("emits update-selection with toggled value when clicking the tile in EDIT mode", async () => {
      const wrapper = mountComponent({
        selectionMode: SelectionMode.EDIT,
        selected: false,
      });
      await wrapper.find(".tile").trigger("click");
      expect(wrapper.emitted("updateSelection")).toBeTruthy();
      const emittedArgs = wrapper.emitted("updateSelection")![0];
      expect(emittedArgs[0]).toBe("row-1");
      expect(emittedArgs[1]).toBe(true);
    });

    it("emits update-selection with false when clicking a selected tile in EDIT mode", async () => {
      const wrapper = mountComponent({
        selectionMode: SelectionMode.EDIT,
        selected: true,
      });
      await wrapper.find(".tile").trigger("click");
      const emittedArgs = wrapper.emitted("updateSelection")![0];
      expect(emittedArgs[1]).toBe(false);
    });

    it("does not emit update-selection when clicking the tile in SHOW mode", async () => {
      const wrapper = mountComponent({ selectionMode: SelectionMode.SHOW });
      await wrapper.find(".tile").trigger("click");
      expect(wrapper.emitted("updateSelection")).toBeFalsy();
    });

    it("does not emit update-selection when clicking the tile in OFF mode", async () => {
      const wrapper = mountComponent({ selectionMode: SelectionMode.OFF });
      await wrapper.find(".tile").trigger("click");
      expect(wrapper.emitted("updateSelection")).toBeFalsy();
    });
  });

  describe("gridRow span style", () => {
    it("applies gridRow span based on rowSpan", () => {
      const wrapper = mountComponent();
      const tile = wrapper.find(".tile");
      // showTitle=true, 2 data cols => rowSpan=3
      expect(tile.attributes("style")).toContain("grid-row: span 3");
    });
  });

  describe("color CSS variable", () => {
    it("sets --tile-color CSS variable when color is provided", () => {
      const wrapper = mountComponent({ color: "#abc123" });
      const style = wrapper.find(".tile").attributes("style");
      expect(style).toContain("--tile-color: #abc123");
    });

    it("does not set --tile-color when color is null", () => {
      const wrapper = mountComponent({ color: null });
      const style = wrapper.find(".tile").attributes("style") ?? "";
      expect(style).not.toContain("--tile-color");
    });
  });

  describe("cell event forwarding", () => {
    it("forwards pending-image and rendered-image from TileCell", async () => {
      const wrapper = mountComponent();
      const cell = wrapper.findComponent({ name: "TileCell" });

      await cell.vm.$emit("pending-image", "img-1");
      await cell.vm.$emit("rendered-image", "img-1");

      expect(wrapper.emitted("pendingImage")?.[0]).toStrictEqual(["img-1"]);
      expect(wrapper.emitted("renderedImage")?.[0]).toStrictEqual(["img-1"]);
    });
  });
});
