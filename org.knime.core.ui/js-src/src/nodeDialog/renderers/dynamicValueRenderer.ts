import { defineAsyncComponent } from "vue";
import { and, isControl, rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const DynamicValuesControl = defineAsyncComponent(
  () => import("../uiComponents/dynamicValue/DynamicValuesControl.vue"),
);

export const dynamicValueRenderer = {
  name: "DynamicValueControl",
  control: DynamicValuesControl,
  tester: rankWith(
    priorityRanks.default,
    and(isControl, hasFormat(inputFormats.dynamicValue)),
  ),
};
