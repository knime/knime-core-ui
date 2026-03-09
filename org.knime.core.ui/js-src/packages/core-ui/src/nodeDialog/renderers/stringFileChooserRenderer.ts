import { defineAsyncComponent } from "vue";
import { and, isStringControl, rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const StringFileChooserControl = defineAsyncComponent(
  () =>
    import(
      "../uiComponents/fileChooser/singleFileSystem/StringFileChooserControl.vue"
    ),
);

export const stringFileChooserRenderer = withLabel()({
  name: "StringFileChooserControl",
  control: StringFileChooserControl,
  tester: rankWith(
    priorityRanks.default,
    and(isStringControl, hasFormat(inputFormats.stringFileChooser)),
  ),
});
