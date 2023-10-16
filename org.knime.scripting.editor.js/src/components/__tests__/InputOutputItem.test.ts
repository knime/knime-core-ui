import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import InputOutputItem, {
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "../InputOutputItem.vue";
import Collapser from "webapps-common/ui/components/Collapser.vue";
import { createDragGhost, removeDragGhost } from "../utils/dragGhost";
import Handlebars from "handlebars";
import { useInputOutputSelectionStore } from "@/store";

vi.mock("monaco-editor");
vi.mock("@/scripting-service");
vi.mock("../utils/dragGhost", () => ({
  createDragGhost: vi.fn(),
  removeDragGhost: vi.fn(),
}));

describe("InputOutputItem", () => {
  const inputOutputItemMinimal: InputOutputModel = {
    name: "mocked item",
  };

  const inputOutputItemWithRowsAndAlias: InputOutputModel = {
    name: "supermock",
    codeAlias: "super.mock",
    subItemCodeAliasTemplate: "my template {{ subItems }}",
    subItems: [
      { name: "row 1", type: "String", codeAlias: "alias 1" },
      { name: "row 2", type: "Double", codeAlias: "alias 2" },
    ],
  };

  const dragEventMock = {
    dataTransfer: {
      setDragImage: vi.fn(),
      setData: vi.fn(),
    },
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("with collapser", () => {
    it("renders collapser if item contains rows / columnInfo", () => {
      const wrapper = mount(InputOutputItem, {
        props: { inputOutputItem: inputOutputItemWithRowsAndAlias },
      });
      expect(wrapper.findComponent(Collapser).exists()).toBeTruthy();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      expect(
        wrapper.find(".top-card").find(".code-alias").exists(),
      ).toBeTruthy();
    });

    it("collapser title creates drag ghost on drag start", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart");
      expect(createDragGhost).toHaveBeenCalledWith({
        elements: [{ text: "super.mock" }],
        width: "auto",
        font: "monospace",
        numSelectedItems: 0,
      });
    });

    it("collapser title removes drag ghost on drag end", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalled();
    });

    it("dragging collapser title adds code alias to drag event", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart", dragEventMock);
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        1,
        "text",
        "super.mock",
      );
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        2,
        "eventId",
        INPUT_OUTPUT_DRAG_EVENT_ID,
      );
    });

    it("subitem creates drag ghost on drag start", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const subItem = wrapper.findAll(".sub-item")[0];
      const dragGhostMock = { drag: "my drag ghost" };
      (createDragGhost as any).mockReturnValueOnce(dragGhostMock);
      subItem.trigger("dragstart", dragEventMock);
      expect(createDragGhost).toHaveBeenCalledWith({
        elements: [
          {
            text: "row 1",
          },
          {
            text: "String",
          },
        ],
        numSelectedItems: 1,
        width: expect.anything(),
      });
      expect(dragEventMock.dataTransfer.setDragImage).toHaveBeenCalledWith(
        dragGhostMock,
        0,
        0,
      );
    });

    it("subitem removes drag ghost on drag end", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const subItem = wrapper.findAll(".sub-item")[0];
      subItem.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalledWith();
    });

    it("dragging subitem uses code alias template", () => {
      const handleBarsSpy = vi.spyOn(Handlebars, "compile");
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      expect(handleBarsSpy).toHaveBeenCalledWith(
        inputOutputItemWithRowsAndAlias.subItemCodeAliasTemplate,
      );
      const template = (wrapper.vm as any).subItemCodeAliasTemplate;
      const subItem = wrapper.findAll(".sub-item")[0];
      subItem.trigger("dragstart", dragEventMock);
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        1,
        "text",
        template({ subItems: "row 1" }),
      );
    });
  });

  describe("no collapser", () => {
    it("does not render collapser if item contains no subitems", () => {
      const wrapper = mount(InputOutputItem, {
        props: { inputOutputItem: inputOutputItemMinimal },
      });
      expect(wrapper.findComponent(Collapser).exists()).toBeFalsy();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: {
            ...inputOutputItemMinimal,
            codeAlias: "myAlias",
          },
        },
      });
      expect(wrapper.find(".code-alias").exists()).toBeTruthy();
    });

    it("title creates drag ghost on drag start", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart");
      expect(createDragGhost).toHaveBeenCalledWith({
        elements: [{ text: "super.mock" }],
        width: "auto",
        font: "monospace",
        numSelectedItems: 1,
      });
    });

    it("title removes drag ghost on drag end", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalled();
    });

    it("dragging title adds code alias to drag event", () => {
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart", dragEventMock);
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        1,
        "text",
        "super.mock",
      );
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        2,
        "eventId",
        INPUT_OUTPUT_DRAG_EVENT_ID,
      );
    });
  });

  describe("uses selection store", () => {
    beforeEach(() => {
      useInputOutputSelectionStore().clearSelection();
    });

    it("click on subitem calls handleSelection store method", () => {
      const handleSelectionSpy = vi.spyOn(
        useInputOutputSelectionStore(),
        "handleSelection",
      );
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const subItem = wrapper.findAll(".sub-item")[0];
      subItem.trigger("click");
      expect(handleSelectionSpy).toHaveBeenCalledWith(
        inputOutputItemWithRowsAndAlias,
        false,
        0,
      );
    });

    it("drag start calls handleSelection store method", () => {
      const handleSelectionSpy = vi.spyOn(
        useInputOutputSelectionStore(),
        "handleSelection",
      );
      const wrapper = mount(InputOutputItem, {
        props: {
          inputOutputItem: inputOutputItemWithRowsAndAlias,
        },
      });
      const subItem = wrapper.findAll(".sub-item")[0];
      subItem.trigger("dragstart");
      expect(handleSelectionSpy).toHaveBeenCalledWith(
        inputOutputItemWithRowsAndAlias,
        true,
        0,
      );
    });
  });
});
