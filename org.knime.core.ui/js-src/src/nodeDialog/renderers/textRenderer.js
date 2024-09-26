import { rankWith, isStringControl } from "@jsonforms/core";
import { priorityRanks } from "../constants";

import { defineAsyncComponent } from "vue";

function delay(time) {
  return new Promise((resolve) => setTimeout(resolve, time));
}

const TextControl = defineAsyncComponent(() => {
  return import("../uiComponents/TextControl.vue");
});

export const textTester = isStringControl;

export const textRenderer = {
  name: "TextControl",
  renderer: TextControl,
  tester: rankWith(priorityRanks.default, textTester),
};
