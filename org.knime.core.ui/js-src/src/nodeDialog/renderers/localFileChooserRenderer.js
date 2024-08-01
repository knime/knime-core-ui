import { rankWith, isStringControl, and } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const LabeledLocalFileChooserInput = defineAsyncComponent(() =>
  import(
    "../uiComponents/fileChooser/withValueSwitch/LabeledLocalFileChooserInput.vue"
  ),
);

export const localFileChooserTester = and(
  isStringControl,
  (uischema, _schema) =>
    uischema.options?.format === inputFormats.localFileChooser,
);

export const localFileChooserRenderer = {
  renderer: LabeledLocalFileChooserInput,
  tester: rankWith(priorityRanks.default, localFileChooserTester),
};
