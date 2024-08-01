import { isControl, rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const DynamicValuesInput = defineAsyncComponent(() =>
  import("../uiComponents/dynamicValue/DynamicValuesInput.vue"),
);

export const dynamicValueTester = (uischema, _schema) =>
  isControl(uischema) && uischema.options?.format === inputFormats.dynamicValue;

export const dynamicValueRenderer = {
  renderer: DynamicValuesInput,
  tester: rankWith(priorityRanks.default, dynamicValueTester),
};
