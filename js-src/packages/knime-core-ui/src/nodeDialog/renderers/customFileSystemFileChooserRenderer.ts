import { defineAsyncComponent } from "vue";
import { and, isStringControl, rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const CustomFileSystemFileChooserControl = defineAsyncComponent(
  () =>
    import(
      "../uiComponents/fileChooser/singleFileSystem/CustomFileSystemFileChooserControl.vue"
    ),
);

export const customFileSystemFileChooserRenderer = withLabel()({
  name: "CustomFileSystemFileChooserControl",
  control: CustomFileSystemFileChooserControl,
  tester: rankWith(
    priorityRanks.default,
    and(isStringControl, hasFormat(inputFormats.customFileSystemFileChooser)),
  ),
});
