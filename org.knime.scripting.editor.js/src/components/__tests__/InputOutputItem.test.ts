import {
  type Mock,
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import { nextTick, ref } from "vue";
import { mount } from "@vue/test-utils";
import Handlebars from "handlebars";

import { Collapser, useMultiSelection } from "@knime/components";
import { DataType } from "@knime/kds-components";

import { useInputOutputSelectionStore } from "@/store/io-selection";
import InputOutputItem, {
  COLUMN_INSERTION_EVENT,
  INPUT_OUTPUT_DRAG_EVENT_ID,
  type InputOutputModel,
} from "../InputOutputItem.vue";
import { createDragGhost, removeDragGhost } from "../utils/dragGhost";
import { insertionEventHelper } from "../utils/insertionEventHelper";

vi.mock("monaco-editor");
vi.mock("../utils/dragGhost", () => ({
  createDragGhost: vi.fn(),
  removeDragGhost: vi.fn(),
}));
vi.mock("@knime/components", async () => {
  const actual = await vi.importActual("@knime/components");
  const selectedIndexes = ref([0]);
  const multiSelection = {
    isSelected: vi.fn((idx) => selectedIndexes.value.includes(idx)),
    selectedIndexes,
    resetSelection: vi.fn(),
    handleSelectionClick: vi.fn(),
  };
  return {
    ...actual,
    useMultiSelection: vi.fn(() => multiSelection),
  };
});

describe("InputOutputItem", () => {
  const inputOutputItemMinimal: InputOutputModel = {
    name: "mocked item",
  };

  const inputOutputItemWithAlias: InputOutputModel = {
    name: "alias mock",
    codeAlias: "alias.mock",
  };

  const inputOutputItemWithRowsAndAlias: InputOutputModel = {
    name: "supermock",
    codeAlias: "super.mock",
    subItemCodeAliasTemplate: "my template {{ subItems.[0].name }}",
    subItems: [
      {
        name: "row 1",
        type: {
          id: "string-datatype",
          title: "String DataType Title",
          displayName: "String",
        },
        supported: true,
      },
      {
        name: "row 2",
        type: {
          displayName: "Double",
        },
        supported: false,
      },
      {
        name: "row 3",
        type: {
          displayName: "Row number",
        },
        supported: true,
      },
    ],
  };

  const dragEventMock = {
    dataTransfer: {
      setDragImage: vi.fn(),
      setData: vi.fn(),
    },
  };

  const multiSelection = useMultiSelection({} as any);

  const doMount = (
    inputOutputItem: InputOutputModel = inputOutputItemWithRowsAndAlias,
  ) => {
    return mount(InputOutputItem, {
      props: { inputOutputItem },
    });
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("with collapser", () => {
    it("renders collapser if item contains rows / columnInfo", () => {
      const wrapper = doMount();
      expect(wrapper.findComponent(Collapser).exists()).toBeTruthy();
      wrapper.unmount();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = doMount();
      expect(
        wrapper.find(".top-card").find(".code-alias").exists(),
      ).toBeTruthy();
      wrapper.unmount();
    });

    it("collapser title creates drag ghost on drag start", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart");
      expect(createDragGhost).toHaveBeenCalledWith({
        elements: [{ dragGhostContent: expect.any(HTMLElement) }],
        font: "monospace",
        numSelectedItems: 1,
      });
      expect(
        (createDragGhost as Mock).mock.calls[0][0].elements[0].dragGhostContent
          .innerText,
      ).toBe("super.mock");
      wrapper.unmount();
    });

    it("collapser title removes drag ghost on drag end", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalled();
      wrapper.unmount();
    });

    it("dragging collapser title adds code alias to drag event", () => {
      const wrapper = doMount();
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
      wrapper.unmount();
    });

    it("subitem creates drag ghost on drag start with data type component", async () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".sub-item-content")[0];
      const dragGhostMock = { drag: "my drag ghost" };
      (createDragGhost as any).mockReturnValueOnce(dragGhostMock);
      await subItem.trigger("dragstart", dragEventMock);

      const mockCall = (createDragGhost as Mock).mock.calls[0][0];
      expect(mockCall.numSelectedItems).toBe(1);
      const { dragGhostContent } = mockCall.elements[0];
      expect(dragGhostContent.children.length).toBe(2);
      expect(
        dragGhostContent.getElementsByClassName("sub-item-name")[0].textContent,
      ).toBe("row 1");
      expect(dragEventMock.dataTransfer.setDragImage).toHaveBeenCalledWith(
        dragGhostMock,
        0,
        0,
      );
      wrapper.unmount();
    });

    it("subitem creates drag ghost on drag start without data type component", async () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".sub-item-content")[2];
      const dragGhostMock = { drag: "my drag ghost" };
      (createDragGhost as any).mockReturnValueOnce(dragGhostMock);
      await subItem.trigger("dragstart", dragEventMock);

      const mockCall = (createDragGhost as Mock).mock.calls[0][0];
      expect(mockCall.numSelectedItems).toBe(1);
      const { dragGhostContent } = mockCall.elements[0];
      expect(dragGhostContent.children.length).toBe(1);
      expect(
        dragGhostContent.getElementsByClassName("sub-item-name")[0].textContent,
      ).toBe("row 3");
      expect(dragEventMock.dataTransfer.setDragImage).toHaveBeenCalledWith(
        dragGhostMock,
        0,
        0,
      );
      wrapper.unmount();
    });

    it("has the correct class if it's disabled", () => {
      const wrapper = doMount();
      const subItem1 = wrapper.findAll(".sub-item")[0];
      expect(subItem1.classes()).not.toContain("disabled");
      const subItem2 = wrapper.findAll(".sub-item")[1];
      expect(subItem2.classes()).toContain("disabled");
      wrapper.unmount();
    });

    it("subitem removes drag ghost on drag end", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[0];
      subItem.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalledWith();
      wrapper.unmount();
    });

    it("dragging subitem uses code alias template", () => {
      const handleBarsSpy = vi.spyOn(Handlebars, "compile");
      const wrapper = doMount();
      expect(handleBarsSpy).toHaveBeenCalledWith(
        inputOutputItemWithRowsAndAlias.subItemCodeAliasTemplate,
      );
      const template = (wrapper.vm as any).subItemCodeAliasTemplate;
      const subItem = wrapper.findAll(".interactive")[0];
      subItem.trigger("dragstart", dragEventMock);
      expect(dragEventMock.dataTransfer.setData).toHaveBeenNthCalledWith(
        1,
        "text",
        template({ subItems: [{ name: "row 1" }] }),
      );
      wrapper.unmount();
    });

    it("collapser initially expanded if 15 subitems", () => {
      const wrapper = doMount({
        ...inputOutputItemWithRowsAndAlias,
        subItems: Array(15).fill({ name: "row 1", type: "String" }),
      });
      const subItems = wrapper.findAll(".sub-item");
      expect(subItems.length).toBe(15);
      subItems.forEach((subItem) => {
        expect(subItem.isVisible()).toBeTruthy();
      });
      wrapper.unmount();
    });

    it("collapser initially not expanded if 16 subitems", () => {
      const wrapper = doMount({
        ...inputOutputItemWithRowsAndAlias,
        subItems: Array(16).fill({ name: "row 1", type: "String" }),
      });
      const subItems = wrapper.findAll(".sub-item");
      expect(subItems.length).toBe(16);
      subItems.forEach((subItem) => {
        expect(subItem.isVisible()).toBeFalsy();
      });
      wrapper.unmount();
    });
  });

  describe("no collapser", () => {
    it("does not render collapser if item contains no subitems", () => {
      const wrapper = doMount(inputOutputItemMinimal);
      expect(wrapper.findComponent(Collapser).exists()).toBeFalsy();
      wrapper.unmount();
    });

    it("does not render collapser if subItems is array of length 0", () => {
      const wrapper = doMount({ ...inputOutputItemMinimal, subItems: [] });
      expect(wrapper.findComponent(Collapser).exists()).toBeFalsy();
      wrapper.unmount();
    });

    it("renders codeAlias in title if it exists", () => {
      const wrapper = doMount({
        ...inputOutputItemMinimal,
        codeAlias: "myAlias",
      });
      expect(wrapper.find(".code-alias").exists()).toBeTruthy();
      wrapper.unmount();
    });

    it("title creates drag ghost on drag start", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragstart");
      expect(createDragGhost).toHaveBeenCalledWith({
        elements: [{ dragGhostContent: expect.any(HTMLElement) }],
        font: "monospace",
        numSelectedItems: 1,
      });
      expect(
        (createDragGhost as Mock).mock.calls[0][0].elements[0].dragGhostContent
          .innerText,
      ).toBe("super.mock");
      wrapper.unmount();
    });

    it("title removes drag ghost on drag end", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".top-card").find(".code-alias");
      codeAliasInTitle.trigger("dragend");
      expect(removeDragGhost).toHaveBeenCalled();
      wrapper.unmount();
    });

    it("dragging title adds code alias to drag event", () => {
      const wrapper = doMount();
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
      wrapper.unmount();
    });
  });

  describe("uses io-selection store", () => {
    beforeEach(() => {
      delete useInputOutputSelectionStore().selectedItem;
    });

    it("mousedown on header code alias (with subitems) sets selectedItem", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".code-alias");
      codeAliasInTitle.trigger("mousedown");
      expect(useInputOutputSelectionStore().selectedItem).toEqual(
        inputOutputItemWithRowsAndAlias,
      );
      wrapper.unmount();
    });

    it("mousedown on header code alias (without subitems) sets selectedItem", () => {
      const wrapper = doMount(inputOutputItemWithAlias);
      const codeAliasInTitle = wrapper.find(".code-alias");
      codeAliasInTitle.trigger("mousedown");
      expect(useInputOutputSelectionStore().selectedItem).toEqual(
        inputOutputItemWithAlias,
      );
      wrapper.unmount();
    });

    it("click on subitem sets selectedItem", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[0];
      subItem.trigger("click");
      expect(useInputOutputSelectionStore().selectedItem).toEqual(
        inputOutputItemWithRowsAndAlias,
      );
      wrapper.unmount();
    });

    it("store change resets selection", async () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[0];
      await subItem.trigger("click"); // item selected

      expect(useInputOutputSelectionStore().selectedItem).toEqual(
        inputOutputItemWithRowsAndAlias,
      );

      // Select another item
      useInputOutputSelectionStore().selectedItem = undefined;
      await nextTick();
      expect(multiSelection.resetSelection).toHaveBeenCalledOnce();
      wrapper.unmount();
    });

    it("sets selected item on sub-item dragstart", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[1];
      subItem.trigger("dragstart");
      expect(useInputOutputSelectionStore().selectedItem).toStrictEqual(
        inputOutputItemWithRowsAndAlias,
      );
    });
  });

  describe("multi-selection", () => {
    it("should reset multi-selection on header mousedown", () => {
      const wrapper = doMount();
      const codeAliasInTitle = wrapper.find(".code-alias");
      codeAliasInTitle.trigger("mousedown");
      expect(multiSelection.resetSelection).toHaveBeenCalledOnce();
      wrapper.unmount();
    });

    it("should update multi-selection on sub-item click", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[1];
      subItem.trigger("click");
      expect(multiSelection.handleSelectionClick).toHaveBeenCalledWith(
        1,
        expect.anything(),
      );
    });

    it("should not reset multi-selection on sub-item dragstart on selected", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".sub-item")[0];
      subItem.trigger("dragstart");
      expect(multiSelection.resetSelection).not.toHaveBeenCalledOnce();
    });

    it("should reset multi-selection on sub-item dragstart on unselected", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[1];
      subItem.trigger("dragstart");
      expect(multiSelection.resetSelection).toHaveBeenCalledOnce();
      expect(multiSelection.handleSelectionClick).toHaveBeenCalledWith(1);
    });
  });

  describe("double click behaviour", () => {
    it("fires an event on double-click", () => {
      const wrapper = doMount();
      const subItem = wrapper.findAll(".interactive")[1];

      const listener = vi.fn();
      insertionEventHelper
        .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
        .registerInsertionListener(listener);

      subItem.trigger("dblclick");

      expect(listener).toHaveBeenCalledWith(
        expect.objectContaining({
          textToInsert: "my template row 2",
        }),
      );
    });
  });

  describe("rendering", () => {
    it("renders sub items without data type icons", () => {
      const itemIndex = 2;
      const wrapper = doMount();
      const content = wrapper.findAll(".sub-item-content").at(itemIndex);

      expect(content?.find(".sub-item-name").text()).toStrictEqual(
        inputOutputItemWithRowsAndAlias?.subItems?.[itemIndex].name,
      );
      expect(content?.findComponent(DataType).exists()).toBeFalsy();
    });

    it("renders sub items with data type icons", () => {
      const itemIndex = 0;
      const wrapper = doMount();
      const content = wrapper.findAll(".sub-item-content").at(itemIndex);

      const subItem = inputOutputItemWithRowsAndAlias?.subItems?.[itemIndex];
      expect(content?.find(".sub-item-name").text()).toStrictEqual(
        subItem?.name,
      );
      expect(content?.findComponent(DataType).exists()).toBeTruthy();
      expect(content?.findComponent(DataType).props()).toStrictEqual({
        iconName: subItem?.type.id,
        iconTitle: subItem?.type.title,
        size: "small",
      });
    });

    it.each([
      ["table", "unknown-datatype", "Unknown data type"],
      ["flowVariable", "UNKNOWN", "Unknown variable type"],
    ] as const)(
      "renders sub items with fallback type icons for %s items",
      (portType, iconName, iconTitle) => {
        const itemIndex = 0;
        const wrapper = doMount({
          ...inputOutputItemWithRowsAndAlias,
          portType,
          subItems: [
            {
              name: "row 1",
              type: {
                displayName: "UNKNOWN",
              },
              supported: true,
            },
          ],
        });
        const content = wrapper.findAll(".sub-item-content").at(itemIndex);
        expect(content?.find(".sub-item-name").text()).toBe("row 1");
        expect(content?.findComponent(DataType).exists()).toBeTruthy();
        expect(content?.findComponent(DataType).props()).toStrictEqual({
          iconName,
          iconTitle,
          size: "small",
        });
      },
    );
  });
});
