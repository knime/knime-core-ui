import { reactive } from "vue";

import type { InputOutputModel } from "@/components/InputOutputItem.vue";

/**
 * Stores the current selection of input/output items
 */
export interface InputOutputSelectionStore {
  selectedItem?: InputOutputModel;
}

const inputOutputSelectionStore: InputOutputSelectionStore =
  reactive<InputOutputSelectionStore>({});

/**
 * @returns the store that is used to store the current selection for drag/drop of input/output objects into the code editor
 */
export const useInputOutputSelectionStore = (): InputOutputSelectionStore =>
  inputOutputSelectionStore;
