import { rankWith, isBooleanControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const CheckboxInput = defineAsyncComponent(() =>
  import("../uiComponents/CheckboxInput.vue"),
);

export const checkboxTester = (uischema, schema) => {
  const isBoolean = isBooleanControl(uischema, schema);
  return isBoolean && uischema.options?.format === inputFormats.checkbox;
};

export const checkboxRenderer = {
  renderer: CheckboxInput,
  tester: rankWith(priorityRanks.default, checkboxTester),
};
