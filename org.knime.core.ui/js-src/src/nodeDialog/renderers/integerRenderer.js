import { rankWith, isIntegerControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const IntegerInput = defineAsyncComponent(() =>
  import("../uiComponents/IntegerInput.vue"),
);

export const integerTester = (uischema, schema) => {
  const isInteger = isIntegerControl(uischema, schema);
  return isInteger && uischema.options?.format === inputFormats.integer;
};

export const integerRenderer = {
  renderer: IntegerInput,
  tester: rankWith(priorityRanks.default, integerTester),
};
