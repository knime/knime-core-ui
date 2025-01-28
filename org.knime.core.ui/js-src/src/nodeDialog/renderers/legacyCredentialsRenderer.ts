import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const LegacyCredentialsControl = defineAsyncComponent(
  () => import("../uiComponents/credentials/LegacyCredentialsControl.vue"),
);

export const legacyCredentialsRenderer = withLabel({
  name: "LegacyCredentialsControl",
  control: LegacyCredentialsControl,
  tester: rankWith(
    priorityRanks.default,
    hasFormat(inputFormats.legacyCredentials),
  ),
});
