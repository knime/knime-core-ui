import { rankWith, isDateTimeControl } from "@jsonforms/core";
import { priorityRanks } from "../constants";
import { defineAsyncComponent } from "vue";

const DateTimeInput = defineAsyncComponent(() =>
  import("../uiComponents/DateTimeInput.vue"),
);

export const dateTimeRenderer = {
  renderer: DateTimeInput,
  tester: rankWith(priorityRanks.default, isDateTimeControl),
};
