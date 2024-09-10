import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";
import RichTextControl from "../uiComponents/richTextControl/RichTextControl.vue";

export const richTextTester = (uischema, _schema) => {
  const isString = isStringControl(uischema, _schema);
  return isString && uischema.options?.format === inputFormats.richTextInput;
};

export const richTextRenderer = {
  renderer: RichTextControl,
  tester: rankWith(priorityRanks.default, richTextTester),
};