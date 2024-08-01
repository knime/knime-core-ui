import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks } from "../constants";

import { defineAsyncComponent } from "vue";

const TextInput = defineAsyncComponent(() =>
  import("../uiComponents/TextInput.vue"),
);

export const textTester = isStringControl;

export const textRenderer = {
  renderer: TextInput,
  tester: rankWith(priorityRanks.default, textTester),
};
