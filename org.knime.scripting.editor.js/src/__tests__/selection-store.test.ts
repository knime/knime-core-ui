import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import {
  useInputOutputSelectionStore,
  type InputOutputSelectionStore,
} from "@/store";
import { beforeEach, describe, expect, it } from "vitest";

const itShiftCtrl = it.each([
  { shift: true, ctrl: false },
  { shift: false, ctrl: true },
  { shift: true, ctrl: true },
]);

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
      selectionStore.handleSelection(item1, false, false);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });

    itShiftCtrl(
      "(shift|ctrl)+click, no previous selection ",
      ({ shift, ctrl }) => {
        selectionStore.handleSelection(item1, shift, ctrl);
        expect(selectionStore.selectedItem).toStrictEqual(item1);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set());
      },
    );

    it("click with same selection and subitems", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item1, false, false);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set());
    });

    itShiftCtrl(
      "(shift|ctrl)+click with previous selection and subitems",
      ({ shift, ctrl }) => {
        selectionStore.selectedItem = item1;
        selectionStore.selectedIndices = new Set([0, 1, 2]);
        selectionStore.handleSelection(item1, shift, ctrl);
        expect(selectionStore.selectedItem).toStrictEqual(item1);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set());
      },
    );
  });

  describe("multiselection with subitems", () => {
    it("click, no previous selection", () => {
      selectionStore.handleSelection(item1, false, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    itShiftCtrl(
      "((shift|ctrl)+click, no previous selection ",
      ({ shift, ctrl }) => {
        selectionStore.handleSelection(item1, shift, ctrl, 0);
        expect(selectionStore.selectedItem).toStrictEqual(item1);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
      },
    );

    it("click on previously selected element toggles element if nothing else is selected", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0]);
      selectionStore.handleSelection(item1, false, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([]));
    });

    it("click on previously selected element focuses element if others are selected", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1]);
      selectionStore.handleSelection(item1, false, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item1);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    it("click with new item selection", () => {
      selectionStore.selectedItem = item1;
      selectionStore.selectedIndices = new Set([0, 1, 2]);
      selectionStore.handleSelection(item2, false, false, 0);
      expect(selectionStore.selectedItem).toStrictEqual(item2);
      expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
    });

    itShiftCtrl(
      "(shift|ctrl)+click with new item selection ",
      ({ shift, ctrl }) => {
        selectionStore.selectedItem = item1;
        selectionStore.selectedIndices = new Set([0, 1, 2]);
        selectionStore.handleSelection(item2, shift, ctrl, 0);
        expect(selectionStore.selectedItem).toStrictEqual(item2);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set([0]));
      },
    );

    itShiftCtrl(
      "(shift|ctrl)+click on subitem of current selection toggles subitem",
      ({ shift, ctrl }) => {
        selectionStore.selectedItem = item1;
        selectionStore.selectedIndices = new Set([0, 1]);
        selectionStore.handleSelection(item1, shift, ctrl, 2);
        expect(selectionStore.selectedItem).toStrictEqual(item1);
        expect(selectionStore.selectedIndices).toStrictEqual(
          new Set([0, 1, 2]),
        );
        selectionStore.handleSelection(item1, shift, ctrl, 2);
        expect(selectionStore.selectedItem).toStrictEqual(item1);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set([0, 1]));
      },
    );
  });

  describe("single selection with subitems", () => {
    itShiftCtrl(
      "(shift|ctrl)+click on subitem selects single subitem",
      ({ shift, ctrl }) => {
        selectionStore.selectedItem = item3;
        selectionStore.selectedIndices = new Set([0]);
        selectionStore.handleSelection(item3, shift, ctrl, 1);
        expect(selectionStore.selectedItem).toStrictEqual(item3);
        expect(selectionStore.selectedIndices).toStrictEqual(new Set([1]));
      },
    );
  });
});
