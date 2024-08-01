import { rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const SimpleButtonInput = defineAsyncComponent(() =>
  import("../uiComponents/SimpleButtonInput.vue"),
);

export const simpleButtonTester = (uischema, _schema) =>
  uischema.options?.format === inputFormats.simpleButton;

export const simpleButtonRenderer = {
  renderer: SimpleButtonInput,
  tester: rankWith(priorityRanks.default, simpleButtonTester),
};
