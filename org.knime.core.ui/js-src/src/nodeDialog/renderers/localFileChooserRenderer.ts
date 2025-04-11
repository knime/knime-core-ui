import { defineAsyncComponent } from "vue";
import { and, isStringControl, rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const LocalFileChooserControl = defineAsyncComponent(
  () => import("../uiComponents/fileChooser/local/LocalFileChooserControl.vue"),
);

export const localFileChooserRenderer = withLabel()({
  name: "LocalFileChooserControl",
  control: LocalFileChooserControl,
  tester: rankWith(
    priorityRanks.default,
    and(isStringControl, hasFormat(inputFormats.localFileChooser)),
  ),
});
