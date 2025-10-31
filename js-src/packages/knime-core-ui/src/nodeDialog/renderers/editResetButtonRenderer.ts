import { type Tester, rankWith } from "@jsonforms/core";

import { priorityRanks } from "@knime/jsonforms";

import EditResetButton from "../layoutComponents/arrayLayout/EditResetButton.vue";

export const editResetButtonFormat = "editResetButton";
export const editResetButtonTester: Tester = (uischema) =>
  uischema.options?.format === editResetButtonFormat;

export const editResetButtonRenderer = {
  name: "EditResetButton",
  renderer: EditResetButton,
  tester: rankWith(priorityRanks.default, editResetButtonTester),
};
