import { defineAsyncComponent } from "vue";
import { rankWith } from "@jsonforms/core";

import { type VueControlRenderer, priorityRanks, withLabel } from "@knime/jsonforms";

import { hasFormat, inputFormats } from "../constants/inputFormats";

const CodeEditorControl = defineAsyncComponent(
  () => import("../uiComponents/CodeEditorControl.vue"),
);

export const codeEditorRenderer: VueControlRenderer = withLabel()({
  name: "CodeEditorControl",
  control: CodeEditorControl,
  tester: rankWith(priorityRanks.default, hasFormat(inputFormats.codeEditor)),
});
