import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const RestCallButtonControl = defineAsyncComponent(
  () => import("../uiComponents/RestCallButtonControl.vue"),
);

export const restCallButtonRenderer = {
  name: "RestCallButtonControl",
  control: RestCallButtonControl,
  tester: rankWith(
    priorityRanks.default,
    hasFormat(inputFormats.restCallButton),
  ),
};
