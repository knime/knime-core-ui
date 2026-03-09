import { rankWith } from "@jsonforms/core";

import { type VueControlRenderer, priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";
import CustomWidget from "../uiComponents/CustomWidget.vue";

export const customWidgetRenderer: VueControlRenderer = {
  name: "CustomWidget",
  control: CustomWidget,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.custom)),
};
