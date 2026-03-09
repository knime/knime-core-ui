import { defineAsyncComponent } from "vue";
import { rankWith, uiTypeIs } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

/**
 * A pass-through wrapper renderer that applies one additional rule to its child elements.
 */
const MultiRuleWrapper = defineAsyncComponent(
  () => import("../layoutComponents/MultiRuleWrapper.vue"),
);

export const multiRuleWrapperRenderer = {
  name: "MultiRuleWrapper",
  layout: MultiRuleWrapper,
  tester: rankWith(priorityRanks.default, uiTypeIs("MultiRuleWrapper")),
};
