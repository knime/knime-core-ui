import { rankWith, isOneOfControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const RadioInput = defineAsyncComponent(() =>
  import("../uiComponents/RadioInput.vue"),
);

export const radioTester = (uischema, schema) => {
  const isOneOf = isOneOfControl(uischema, schema);
  return isOneOf && uischema.options?.format === inputFormats.radio;
};

export const radioRenderer = {
  renderer: RadioInput,
  tester: rankWith(priorityRanks.default, radioTester),
};
