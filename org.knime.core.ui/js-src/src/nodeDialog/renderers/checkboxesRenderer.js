import { rankWith, isAnyOfControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const CheckboxesInput = defineAsyncComponent(() =>
  import("../uiComponents/CheckboxesInput.vue"),
);

export const checkboxesTester = (uischema, schema) => {
  const isAnyOf = isAnyOfControl(uischema, schema);
  return isAnyOf && uischema.options?.format === inputFormats.checkboxes;
};

export const checkboxesRenderer = {
  renderer: CheckboxesInput,
  tester: rankWith(priorityRanks.default, checkboxesTester),
};
