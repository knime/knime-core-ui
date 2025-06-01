import { defineAsyncComponent } from "vue";
import { rankWith, uiTypeIs } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

const ConfigurationNodeNotSupported = defineAsyncComponent(
  () => import("../uiComponents/ConfigurationNodeNotSupported.vue"),
);

const configurationNodeNotSupportedTester = uiTypeIs(
  "ConfigurationNodeNotSupported",
);

export const configurationNodeNotSupportedRenderer = {
  name: "ConfigurationNodeNotSupported",
  renderer: ConfigurationNodeNotSupported,
  tester: rankWith(priorityRanks.default, configurationNodeNotSupportedTester),
};
