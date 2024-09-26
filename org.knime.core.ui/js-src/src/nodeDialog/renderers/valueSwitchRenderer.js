import { rankWith, isOneOfControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

export const delay = (time) =>
  new Promise((resolve) => setTimeout(resolve, time));

const ValueSwitchControl = defineAsyncComponent(async () => {
  console.log("Starting to load ValueSwitchControl");
  await delay(1000);
  return import("../uiComponents/ValueSwitchControl.vue");
});

export const valueSwitchTester = (uischema, schema) => {
  const isOneOf = isOneOfControl(uischema, schema);
  return isOneOf && uischema.options?.format === inputFormats.valueSwitch;
};

export const valueSwitchRenderer = {
  name: "ValueSwitchControl",
  renderer: ValueSwitchControl,
  tester: rankWith(priorityRanks.default, valueSwitchTester),
};
