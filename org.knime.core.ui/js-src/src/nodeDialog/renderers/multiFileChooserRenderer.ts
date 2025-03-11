import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const MultiFileChooserControl = defineAsyncComponent(
  () =>
    import("../uiComponents/fileChooser/withTabs/MultiFileChooserControl.vue"),
);
export const multiFileChooserRenderer = withLabel()({
  name: "MultiFileChooserControl",
  control: MultiFileChooserControl,
  tester: rankWith(
    priorityRanks.default,
    hasFormat(inputFormats.multiFileChooser),
  ),
});
