import { defineAsyncComponent } from "vue";
import { rankWith, uiTypeIs } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

/**
 * Current minimal solution for a group of widgets with a common label and description.
 */
const LabeledGroup = defineAsyncComponent(
  () => import("../layoutComponents/LabeledGroup.vue"),
);

export const labeledGroupRenderer = {
  name: "LabeledGroup",
  layout: LabeledGroup,
  tester: rankWith(priorityRanks.default, uiTypeIs("Group")),
};
