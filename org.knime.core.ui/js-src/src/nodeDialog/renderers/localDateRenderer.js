import { and, isDateTimeControl, rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";
import { defineAsyncComponent } from "vue";
import { DateControlRenderer } from "@jsonforms/vue-vanilla";

// const DateControl = defineAsyncComponent({
//   loader: () => import("../uiComponents/DateControl.vue"),
// });

export const hasLocalTimeFormat = (uischema, _schema) =>
  uischema.options?.format === inputFormats.localDate;

export const dateRenderer = {
  name: "DateControl",
  // TODO: swap this with DateControl from up there 
  renderer: DateControlRenderer,
  tester: rankWith(priorityRanks.default, and(isDateTimeControl, hasLocalTimeFormat)),
};
