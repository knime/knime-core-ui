import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const TextAreaInput = defineAsyncComponent(() =>
  import("../uiComponents/TextAreaInput.vue"),
);

export const textAreaTester = (uischema) =>
  isStringControl && uischema.options?.format === inputFormats.textArea;

export const textAreaRenderer = {
  renderer: TextAreaInput,
  tester: rankWith(priorityRanks.default, textAreaTester),
};
