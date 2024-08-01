import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";

import { defineAsyncComponent } from "vue";

const RichTextInput = defineAsyncComponent(() =>
  import("../uiComponents/RichTextInput.vue"),
);

export const richTextInputTester = (uischema, _schema) => {
  const isString = isStringControl(uischema, _schema);
  return isString && uischema.options?.format === inputFormats.richTextInput;
};

export const richTextInputRenderer = {
  renderer: RichTextInput,
  tester: rankWith(priorityRanks.default, richTextInputTester),
};
