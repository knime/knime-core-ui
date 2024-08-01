import { rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";
import { defineAsyncComponent } from "vue";

const FileChooserInput = defineAsyncComponent(() =>
  import("../uiComponents/fileChooser/withTabs/FileChooserInput.vue"),
);

export const hasFileChooserFormat = (uischema, _schema) =>
  uischema.options?.format === inputFormats.fileChooser;

export const fileChooserRenderer = {
  renderer: FileChooserInput,
  tester: rankWith(priorityRanks.default, hasFileChooserFormat),
};
