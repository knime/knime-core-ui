import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import {
  useInputOutputSelectionStore,
  type InputOutputSelectionStore,
} from "@/store";
import { beforeEach, describe, expect, it } from "vitest";

describe("Input/Output item selection store", () => {
  const selectionStore: InputOutputSelectionStore =
    useInputOutputSelectionStore();
  const item1: InputOutputModel = {
    name: "input item 1",
    multiSelection: true,
    subItems: [
      { name: "subitem1", type: "type1" },
      { name: "subitem2", type: "type2" },
      { name: "subitem3", type: "type3" },
    ],
  };
  const item2: InputOutputModel = {
    name: "input item 2",
    multiSelection: true,
    subItems: [
      { name: "subitem1", type: "type1" },
      { name: "subitem2", type: "type2" },
      { name: "subitem3", type: "type3" },
    ],
  };
  const item3: InputOutputModel = {
    name: "input item 2",
    multiSelection: false,
    subItems: [
      { name: "subitem1", type: "type1" },
      { name: "subitem2", type: "type2" },
      { name: "subitem3", type: "type3" },
    ],
  };

  beforeEach(() => {
    // reset store
    selectionStore.clearSelection();
  });

  describe("click on header", () => {
    it("click, no previous selection", () => {
      selectionStore.handleSelection(item1, false);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });

    it("shift+click, no previous selection ", () => {
      selectionStore.handleSelection(item1, true);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });

    it("click with same selection and subitems", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item1, true);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });

    it("shift+click with previous selection and subitems", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item1, true);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });
  });

  describe("multiselection with subitems", () => {
    it("click, no previous selection", () => {
      selectionStore.handleSelection(item1, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("shift+click, no previous selection ", () => {
      selectionStore.handleSelection(item1, true, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("click on previously selected element toggles element if nothing else is selected", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0]);
      selectionStore.handleSelection(item1, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([]));
    });

    it("click on previously selected element focuses element if others are selected", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1]);
      selectionStore.handleSelection(item1, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("click with new item selection", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item2, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item2);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("shift+click with new item selection ", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item2, true, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item2);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("shift+click on subitem of current selection toggles subitem", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1]);
      selectionStore.handleSelection(item1, true, 2);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0, 1, 2]));
      selectionStore.handleSelection(item1, true, 2);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0, 1]));
    });
  });

  describe("single selection with subitems", () => {
    it("shift+click on subitem selects single subitem", () => {
      selectionStore.selectedItem = item3;
      selectionStore.selectedIndices = new Set([0]);
      selectionStore.handleSelection(item3, true, 1);
      expect(selectionStore.selectedItem).toStrictEqual(item3);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([1]));
    });
  });
});
