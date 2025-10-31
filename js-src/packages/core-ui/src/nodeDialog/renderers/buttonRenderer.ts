import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const ButtonControl = defineAsyncComponent(
  () => import("../uiComponents/ButtonControl.vue"),
);

export const buttonRenderer = {
  name: "ButtonControl",
  control: ButtonControl,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.button)),
};
