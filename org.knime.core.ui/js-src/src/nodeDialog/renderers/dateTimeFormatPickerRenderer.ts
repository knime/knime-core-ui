import { defineAsyncComponent } from "vue";
import { type UISchemaElement, isControl, rankWith } from "@jsonforms/core";

import { inputFormats, priorityRanks } from "../constants";

const DateTimeFormatPickerControl = defineAsyncComponent(
  () => import("../uiComponents/DateTimeFormatPickerControl.vue"),
);

export const dateTimeFormatPickerTester = (uischema: UISchemaElement) => {
  return (
    isControl(uischema) &&
    uischema.options?.format === inputFormats.dateTimeFormat
  );
};

export const dateFormatPickerRenderer = {
  name: "DateTimeFormatPickerControl",
  renderer: DateTimeFormatPickerControl,
  tester: rankWith(priorityRanks.default, dateTimeFormatPickerTester),
};
