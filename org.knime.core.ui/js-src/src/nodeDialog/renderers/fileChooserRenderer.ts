import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const FileChooserControl = defineAsyncComponent(
  () => import("../uiComponents/fileChooser/withTabs/FileChooserControl.vue"),
);
export const fileChooserRenderer = withLabel({
  name: "FileChooserControl",
  control: FileChooserControl,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.fileChooser)),
});
