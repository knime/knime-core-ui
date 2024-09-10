import { and, hasType, rankWith, schemaMatches } from "@jsonforms/core";
import ComboBoxControl from "../uiComponents/ComboBoxControl.vue";
import { inputFormats, priorityRanks } from "@/nodeDialog/constants";

const isArray = schemaMatches((s) => hasType(s, "array"));
const hasComboBoxFormat = (uischema, _schema) =>
  uischema.options?.format === inputFormats.comboBox;

export const comboBoxTester = and(isArray, hasComboBoxFormat);

export const comboBoxRenderer = {
  renderer: ComboBoxControl,
  tester: rankWith(priorityRanks.default, comboBoxTester),
};