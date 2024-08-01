import { isControl, rankWith } from "@jsonforms/core";
import { inputFormats, priorityRanks } from "../constants";

import { defineAsyncComponent } from "vue";

const SortListInput = defineAsyncComponent(() =>
  import("../uiComponents/SortListInput.vue"),
);

export const sortListTester = (uischema, _schema) =>
  isControl(uischema) && uischema.options?.format === inputFormats.sortList;

export const sortListRenderer = {
  renderer: SortListInput,
  tester: rankWith(priorityRanks.default, sortListTester),
};
