import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";

import TileCell from "../TileCell.vue";

// Stub heavy renderer components
vi.mock("@/tableView/renderers/ImageRenderer.vue", () => ({
  default: { template: '<div class="image-renderer-stub" />' },
}));
vi.mock("@/tableView/renderers/HtmlRenderer.vue", () => ({
  default: { template: '<div class="html-renderer-stub" />' },
}));
vi.mock("@/tableView/renderers/MultiLineTextRenderer.vue", () => ({
  default: { template: '<div class="multi-line-text-renderer-stub" />' },
}));
vi.mock("@knime/styles/img/icons/circle-help.svg", () => ({
  default: { template: '<svg class="circle-help-icon-stub" />' },
}));

const defaultProps: InstanceType<typeof TileCell>["$props"] = {
  cell: { value: "cell content", isMissing: false },
  columnName: "My Column",
  displayColumnHeaders: true,
  iconAlign: "flex-start",
  contentType: "txt" as const,
  tileWidth: 200,
  isResizeActive: false,
  isReport: false,
};

const mountComponent = (propsOverrides: Partial<typeof defaultProps> = {}) =>
  mount(TileCell, { props: { ...defaultProps, ...propsOverrides } });

describe("TileCell.vue", () => {
  it("renders the .tile-row container", () => {
    const wrapper = mountComponent();
    expect(wrapper.find(".tile-row").exists()).toBeTruthy();
  });

  describe("column header", () => {
    it("shows the column label when displayColumnHeaders=true", () => {
      const wrapper = mountComponent({ displayColumnHeaders: true });
      expect(wrapper.find(".column-label").exists()).toBeTruthy();
      expect(wrapper.find(".column-label").text()).toContain("My Column:");
    });

    it("hides the column label when displayColumnHeaders=false", () => {
      const wrapper = mountComponent({ displayColumnHeaders: false });
      expect(wrapper.find(".column-label").exists()).toBeFalsy();
    });
  });

  describe("missing value icon", () => {
    it("shows missing value icon when cell.isMissing=true", () => {
      const wrapper = mountComponent({
        cell: { value: "", isMissing: true },
      });
      expect(wrapper.find(".missing-value-icon").exists()).toBeTruthy();
    });

    it("does not show missing value icon when cell.isMissing=false", () => {
      const wrapper = mountComponent({
        cell: { value: "some value", isMissing: false },
      });
      expect(wrapper.find(".missing-value-icon").exists()).toBeFalsy();
    });
  });

  describe("plain text rendering", () => {
    it("renders cell value in .cell-value for txt content type", () => {
      const wrapper = mountComponent({ contentType: "txt" });
      expect(wrapper.find(".cell-value").exists()).toBeTruthy();
      expect(wrapper.find(".cell-value").text()).toBe("cell content");
    });

    it("does not render the image renderer for txt content type", () => {
      const wrapper = mountComponent({ contentType: "txt" });
      expect(wrapper.find(".image-renderer-stub").exists()).toBeFalsy();
    });
  });

  describe("image rendering", () => {
    it("renders ImageRenderer for img_path content type", () => {
      const wrapper = mountComponent({ contentType: "img_path" });
      expect(wrapper.find(".image-renderer-stub").exists()).toBeTruthy();
    });

    it("does not render .cell-value for img_path content type", () => {
      const wrapper = mountComponent({ contentType: "img_path" });
      expect(wrapper.find(".cell-value").exists()).toBeFalsy();
    });
  });

  describe("hTML rendering", () => {
    it("renders HtmlRenderer for html content type", () => {
      const wrapper = mountComponent({ contentType: "html" });
      expect(wrapper.find(".html-renderer-stub").exists()).toBeTruthy();
    });

    it("does not render .cell-value for html content type", () => {
      const wrapper = mountComponent({ contentType: "html" });
      expect(wrapper.find(".cell-value").exists()).toBeFalsy();
    });
  });

  describe("multi-line text rendering", () => {
    it("renders MultiLineTextRenderer for multi_line_txt content type", () => {
      const wrapper = mountComponent({ contentType: "multi_line_txt" });
      expect(
        wrapper.find(".multi-line-text-renderer-stub").exists(),
      ).toBeTruthy();
    });

    it("does not render .cell-value for multi_line_txt content type", () => {
      const wrapper = mountComponent({ contentType: "multi_line_txt" });
      expect(wrapper.find(".cell-value").exists()).toBeFalsy();
    });
  });
});
