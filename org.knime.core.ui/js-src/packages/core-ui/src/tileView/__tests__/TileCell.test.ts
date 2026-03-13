import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";

import TileCell from "../TileCell/TileCell.vue";

vi.mock("../TileCell/TileCellInline.vue", () => ({
  default: {
    name: "TileCellInline",
    template: '<div class="tile-cell-inline-stub" />',
    props: ["cell", "columnName", "displayColumnHeaders", "contentType"],
  },
}));

vi.mock("../TileCell/TileCellStacked.vue", () => ({
  default: {
    name: "TileCellStacked",
    template: '<div class="tile-cell-stacked-stub" />',
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

const defaultProps: InstanceType<typeof TileCell>["$props"] = {
  cell: { value: "cell content", isMissing: false },
  columnName: "My Column",
  displayColumnHeaders: true,
  contentType: "txt" as const,
  tileWidth: 200,
  isResizeActive: false,
  isReport: false,
};

const mountComponent = (propsOverrides: Partial<typeof defaultProps> = {}) =>
  mount(TileCell, { props: { ...defaultProps, ...propsOverrides } });

describe("TileCell.vue", () => {
  it("renders inline layout for plain text content", () => {
    const wrapper = mountComponent();
    expect(wrapper.find(".tile-cell-inline-stub").exists()).toBeTruthy();
    expect(wrapper.find(".tile-cell-stacked-stub").exists()).toBeFalsy();
  });

  it("renders stacked layout for html content", () => {
    const wrapper = mountComponent({ contentType: "html" });
    expect(wrapper.find(".tile-cell-stacked-stub").exists()).toBeTruthy();
    expect(wrapper.find(".tile-cell-inline-stub").exists()).toBeFalsy();
  });

  it("renders stacked layout for image content", () => {
    const wrapper = mountComponent({ contentType: "img_path" });
    expect(wrapper.find(".tile-cell-stacked-stub").exists()).toBeTruthy();
    expect(wrapper.find(".tile-cell-inline-stub").exists()).toBeFalsy();
  });

  it("forwards image events only from stacked layout", async () => {
    const wrapper = mountComponent({ contentType: "html" });
    const stacked = wrapper.findComponent({ name: "TileCellStacked" });

    await stacked.vm.$emit("pending-image", "img-1");
    await stacked.vm.$emit("rendered-image", "img-1");

    expect(wrapper.emitted("pendingImage")?.[0]).toStrictEqual(["img-1"]);
    expect(wrapper.emitted("renderedImage")?.[0]).toStrictEqual(["img-1"]);
  });
});
