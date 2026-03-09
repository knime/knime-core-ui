import { defineAsyncComponent } from "vue";
import { rankWith, uiTypeIs } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

/**
 * Current minimal solution for a group of widgets with a common label and description.
 */
const ControlWithSubParameters = defineAsyncComponent(
  () => import("../layoutComponents/ControlWithSubParameters.vue"),
);

export const controlWithSubParametersRenderer = {
  name: "ControlWithSubParameters",
  layout: ControlWithSubParameters,
  tester: rankWith(priorityRanks.default, uiTypeIs("ControlWithSubParameters")),
};
