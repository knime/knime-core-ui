import { isControl, rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";
import { defineAsyncComponent } from "vue";

const DropdownInput = defineAsyncComponent(() =>
  import("../uiComponents/DropdownInput.vue"),
);

export const dropdownTester = (uischema, _schema) =>
  isControl(uischema) && uischema.options?.format === inputFormats.dropDown;

export const dropdownRenderer = {
  renderer: DropdownInput,
  tester: rankWith(priorityRanks.default, dropdownTester),
};
