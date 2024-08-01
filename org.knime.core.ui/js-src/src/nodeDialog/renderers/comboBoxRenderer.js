import { and, hasType, rankWith, schemaMatches } from "@jsonforms/core";
import { inputFormats, priorityRanks } from "@/nodeDialog/constants";

import { defineAsyncComponent } from "vue";

const ComboBoxInput = defineAsyncComponent(() =>
  import("../uiComponents/ComboBoxInput.vue"),
);

const isArray = schemaMatches((s) => hasType(s, "array"));
const hasComboBoxFormat = (uischema, _schema) =>
  uischema.options?.format === inputFormats.comboBox;

export const comboBoxTester = and(isArray, hasComboBoxFormat);

export const comboBoxRenderer = {
  renderer: ComboBoxInput,
  tester: rankWith(priorityRanks.default, comboBoxTester),
};
