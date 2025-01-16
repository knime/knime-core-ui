import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { addLabel, priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const FileChooserControl = defineAsyncComponent(
  () => import("../uiComponents/fileChooser/withTabs/FileChooserControl.vue"),
);
export const fileChooserRenderer = {
  name: "FileChooserControl",
  control: addLabel(FileChooserControl),
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.fileChooser)),
};
