import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { addLabel, priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const LegacyCredentialsControl = defineAsyncComponent(
  () => import("../uiComponents/credentials/LegacyCredentialsControl.vue"),
);

export const legacyCredentialsRenderer = addLabel({
  name: "LegacyCredentialsControl",
  control: LegacyCredentialsControl,
  tester: rankWith(
    priorityRanks.default,
    hasFormat(inputFormats.legacyCredentials),
  ),
});
