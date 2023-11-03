import type { InputOutputModel } from "@/components/InputOutputItem.vue";
import { reactive } from "vue";

/**
 * Stores the current selection of input/output items
 */
export interface InputOutputSelectionStore {
  selectedItem?: InputOutputModel;
  selectedIndices?: Set<number>;
  /**
   *
   * @param item The InputOutputModel that was clicked on
   * @param rangeSelectKeyPressed Whether to do a range selection
   * @param multiSelectKeyPressed Whether to do a multi selection
   * @param index The index of the subitem that was clicked on, undefined if collapser header is clicked
   * @returns
   */
  handleSelection: (
    item: InputOutputModel,
    rangeSelectKeyPressed: boolean,
    multiSelectKeyPressed: boolean,
    index?: number,
  ) => void;
  clearSelection: () => void;
}

/**
 * Add element to array if it is not contained in array, otherwise remove element from array
 * @param selection
 * @param element
 */
const toggleSelection = (selection: Set<number>, element: number) => {
  if (selection.has(element)) {
    selection.delete(element);
  } else {
    selection.add(element);
  }
};

const inputOutputSelectionStore: InputOutputSelectionStore =
  reactive<InputOutputSelectionStore>({
    handleSelection(
      item: InputOutputModel,
      shiftKeyPressed: boolean,
      ctrlKeyPressed: boolean,
      index?: number,
    ) {
      if (typeof index === "undefined") {
        // handle click on header
        this.selectedItem = item;
        this.selectedIndices = new Set();
        return;
      }

      if (
        typeof this.selectedItem === "undefined" ||
        this.selectedItem.name !== item.name
      ) {
        // handle click on item that was not selected yet
        this.selectedItem = item;
        this.selectedIndices = new Set([index]);
        return;
      }

      if (typeof this.selectedIndices === "undefined") {
        this.selectedIndices = new Set();
      }

      // update selection (no multiselection)
      if (!this.selectedItem.multiSelection) {
        this.selectedIndices = new Set([index]);
        return;
      }

      if (shiftKeyPressed || ctrlKeyPressed) {
        // Shift key pressed - toggle selection
        toggleSelection(this.selectedIndices, index);
      } else if (
        this.selectedIndices.has(index) &&
        this.selectedIndices.size === 1
      ) {
        // No no shift key pressed and clicked on the only selected item - deselect
        this.selectedIndices = new Set();
      } else {
        // No shift key pressed - select only the clicked item
        this.selectedIndices = new Set([index]);
      }
    },
    clearSelection() {
      delete this.selectedItem;
      delete this.selectedIndices;
    },
  });

/**
 * @returns the store that is used to store the current selection for drag/drop of input/output objects into the code editor
 */
export const useInputOutputSelectionStore = (): InputOutputSelectionStore =>
  inputOutputSelectionStore;
