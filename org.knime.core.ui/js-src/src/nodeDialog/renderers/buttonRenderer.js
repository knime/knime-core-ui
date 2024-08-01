import { rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const ButtonInput = defineAsyncComponent(() =>
  import("../uiComponents/ButtonInput.vue"),
);

export const buttonTester = (uischema, _schema) =>
  uischema.options?.format === inputFormats.button;

export const buttonRenderer = {
  renderer: ButtonInput,
  tester: rankWith(priorityRanks.default, buttonTester),
};
