import { reactive, ref } from "vue";
import type { InputOutputModel } from "./components/InputOutputItem.vue";

export interface Message {
  role: "reply" | "request";
  content: string;
}

export type PromptResponse = {
  suggestedCode: string;
  message: Message;
};

export type PromptResponseStore = {
  promptResponse?: PromptResponse;
};

const promptResponseStore: PromptResponseStore = reactive<PromptResponseStore>(
  {},
);

export const usePromptResponseStore = (): PromptResponseStore => {
  return promptResponseStore;
};

export const clearPromptResponseStore = (): void => {
  if (typeof promptResponseStore.promptResponse !== "undefined") {
    delete promptResponseStore.promptResponse;
  }
};

// Whether the disclaimer needs to be shown to the user.
// This is part of the store so it is only shown the first time the user
// opens the AI bar while the script editor is open.
export const showDisclaimer = ref<boolean>(true);

/**
 * Stores the current selection of input/output items
 */
export interface InputOutputSelectionStore {
  selectedItem?: InputOutputModel;
  selectedIndices?: number[];
  /**
   *
   * @param item The InputOutputModel that was clicked on
   * @param shiftKeyPressed Whether shift key was pressed while clicking
   * @param index The index of the subitem that was clicked on, undefined if collapser header is clicked
   * @returns
   */
  handleSelection: (
    item: InputOutputModel,
    shiftKeyPressed: boolean,
    index?: number,
  ) => void;
  clearSelection: () => void;
}

/**
 * Add element to array if it is not contained in array, otherwise remove element from array
 * @param selection
 * @param element
 */
const toggleSelection = (selection: number[], element: number) => {
  const index = selection.indexOf(element);
  if (index === -1) {
    selection.push(element);
  } else {
    selection.splice(index, 1);
  }
};

const inputOutputSelectionStore: InputOutputSelectionStore =
  reactive<InputOutputSelectionStore>({
    handleSelection(
      item: InputOutputModel,
      shiftKeyPressed: boolean,
      index?: number,
    ) {
      if (typeof index === "undefined") {
        // handle click on header
        this.selectedItem = item;
        this.selectedIndices = [];
        return;
      }

      if (
        typeof this.selectedItem === "undefined" ||
        this.selectedItem.name !== item.name
      ) {
        // handle click on item that was not selected yet
        this.selectedItem = item;
        this.selectedIndices = [index];
        return;
      }

      if (typeof this.selectedIndices === "undefined") {
        this.selectedIndices = [];
      }

      // update selection (no multiselection)
      if (!this.selectedItem.multiSelection) {
        this.selectedIndices = [index];
        return;
      }

      // multiselection
      if (shiftKeyPressed) {
        toggleSelection(this.selectedIndices, index);
      } else if (this.selectedIndices.includes(index)) {
        if (this.selectedIndices.length === 1) {
          this.selectedIndices = [];
        } else {
          this.selectedIndices = [index];
        }
      } else {
        this.selectedIndices = [index];
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
