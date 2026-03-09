import { defineAsyncComponent } from "vue";
import { type Tester, rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const FileChooserControl = defineAsyncComponent(
  () => import("../uiComponents/fileChooser/withTabs/FileChooserControl.vue"),
);
export const fileChooserRenderer = withLabel()({
  name: "FileChooserControl",
  control: FileChooserControl,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.fileChooser)),
});

const FileChooserControlForMultiFile = defineAsyncComponent(
  () =>
    import(
      "../uiComponents/fileChooser/withTabs/FileChooserControlForMultiFile.vue"
    ),
);

export const fileChooserForMultiFileFormat = "fileChooserForMultiFile";
export const fileChooserForMultiFileTester: Tester = (uischema) =>
  uischema.options?.format === fileChooserForMultiFileFormat;

export const fileChooserForMultiFileRenderer = withLabel()({
  name: "FileChooserControlForMultiFile",
  control: FileChooserControlForMultiFile,
  tester: rankWith(priorityRanks.default, fileChooserForMultiFileTester),
});
