import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { type VueControlRenderer, priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const DirtyTrackerRenderer = defineAsyncComponent(
  () => import("../uiComponents/DirtyTrackerRenderer.vue"),
);

export const dirtyTrackerRenderer: VueControlRenderer = {
  control: DirtyTrackerRenderer,
  name: "DirtyTrackerRenderer",
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.dirtyTracker)),
};
