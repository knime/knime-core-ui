import { rankWith, uiTypeIs } from "@jsonforms/core";
import { priorityRanks } from "../constants";

import { defineAsyncComponent } from "vue";
import { delay } from "./valueSwitchRenderer";
import SectionLayout from "../layoutComponents/SectionLayout.vue";
const SectionLayout2 = defineAsyncComponent(async () => {
  await delay(1000);
  return import("../layoutComponents/SectionLayout.vue");
});

export const sectionLayoutTester = uiTypeIs("Section");

export const sectionLayoutRenderer = {
  name: "SectionLayout",
  renderer: SectionLayout,
  tester: rankWith(priorityRanks.default, sectionLayoutTester),
};
