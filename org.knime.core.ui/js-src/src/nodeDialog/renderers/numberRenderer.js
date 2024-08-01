import { rankWith, isNumberControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const NumberInput = defineAsyncComponent(() =>
  import("../uiComponents/NumberInput.vue"),
);

export const numberTester = (uischema, schema) => {
  const isNumber = isNumberControl(uischema, schema);
  return isNumber && uischema.options?.format === inputFormats.number;
};

export const numberRenderer = {
  renderer: NumberInput,
  tester: rankWith(priorityRanks.default, numberTester),
};
