import { defineAsyncComponent } from "vue";
import { and, isControl, rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const DynamicInputControl = defineAsyncComponent(
  () => import("../uiComponents/DynamicInputControl.vue"),
);

export const dynamicInputRenderer = {
  name: "DynamicInputControl" as const,
  control: DynamicInputControl,
  tester: rankWith(
    priorityRanks.default,
    and(isControl, hasFormat(inputFormats.dynamicInput)),
  ),
};
