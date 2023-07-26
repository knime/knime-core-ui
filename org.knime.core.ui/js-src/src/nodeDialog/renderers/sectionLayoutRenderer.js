import { rankWith, uiTypeIs } from "@jsonforms/core";
import SectionLayout from "../layoutComponents/SectionLayout.vue";
import { priorityRanks } from "../constants";

export const sectionLayoutTester = uiTypeIs("Section");

export const sectionLayoutRenderer = {
  renderer: SectionLayout,
  tester: rankWith(priorityRanks.default, sectionLayoutTester),
};
