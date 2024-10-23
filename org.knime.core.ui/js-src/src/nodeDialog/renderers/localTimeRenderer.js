import { and, isDateTimeControl, rankWith } from "@jsonforms/core";
import { priorityRanks, inputFormats } from "../constants";
import { defineAsyncComponent } from "vue";
import { TimeControlRenderer } from "@jsonforms/vue-vanilla";

// const TimeControl = defineAsyncComponent({
//   loader: () => import("../uiComponents/TimeControl.vue"),
// });

export const hasLocalTimeFormat = (uischema, _schema) =>
  uischema.options?.format === inputFormats.localTime;

export const timeRenderer = {
  name: "TimeControl",
  // TODO: swap this with TimeControl from up there 
  renderer: TimeControlRenderer,
  tester: rankWith(priorityRanks.default, and(isDateTimeControl, hasLocalTimeFormat)),
};
