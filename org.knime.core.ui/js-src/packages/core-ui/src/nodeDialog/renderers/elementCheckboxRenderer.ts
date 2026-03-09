import { type Tester, rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import ArrayLayoutItemCheckbox from "../layoutComponents/arrayLayout/ArrayLayoutItemCheckbox.vue";

export const elementCheckboxFormat = "elementCheckbox";
export const elementCheckboxTester: Tester = (uischema) =>
  uischema.options?.format === elementCheckboxFormat;

export const elementCheckboxRenderer = {
  name: "ElementCheckbox",
  control: ArrayLayoutItemCheckbox,
  tester: rankWith(priorityRanks.default, elementCheckboxTester),
};
