import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const DBTableChooserControl = defineAsyncComponent(
  () => import("../uiComponents/dbTableChooser/DBTableChooserControl.vue"),
);
export const dbTableChooserRenderer = {
  name: "DBTableChooserControl",
  control: DBTableChooserControl,
  tester: rankWith(
    priorityRanks.default,
    hasFormat(inputFormats.dbTableChooser),
  ),
};
