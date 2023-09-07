import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks } from "../constants";
import FileChooserInput from "../uiComponents/fileChooser/FileChooserInput.vue";

export const textTester = isStringControl;

export const textRenderer = {
  renderer: FileChooserInput,
  tester: rankWith(priorityRanks.default, textTester),
};
