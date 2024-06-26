import { isObjectArray, rankWith } from "@jsonforms/core";
import ArrayLayout from "@/nodeDialog/layoutComponents/arrayLayout/ArrayLayout.vue";
import { priorityRanks } from "@/nodeDialog/constants";

export const arrayLayoutTester = isObjectArray;
export const arrayLayoutRenderer = {
  renderer: ArrayLayout,
  tester: rankWith(priorityRanks.default, arrayLayoutTester),
};
