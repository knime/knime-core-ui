import { and, isControl, not, rankWith, schemaMatches } from "@jsonforms/core";
import { inputFormats, priorityRanks } from "../constants";

import { defineAsyncComponent } from "vue";

const TwinlistInput = defineAsyncComponent(() =>
  import("../uiComponents/twinlist/TwinlistInput.vue"),
);
const SimpleTwinlistInput = defineAsyncComponent(() =>
  import("../uiComponents/twinlist/SimpleTwinlistInput.vue"),
);

const isSelection = schemaMatches(
  (s) =>
    s.hasOwnProperty("properties") && s.properties.hasOwnProperty("selected"),
);

const isTwinlist = (uischema, _schema) =>
  isControl(uischema) && uischema.options?.format === inputFormats.twinList;

export const twinlistTester = and(isTwinlist, isSelection);

export const twinlistRenderer = {
  renderer: TwinlistInput,
  tester: rankWith(priorityRanks.default, twinlistTester),
};

export const simpleTwinlistTester = and(isTwinlist, not(isSelection));

export const simpleTwinlistRenderer = {
  renderer: SimpleTwinlistInput,
  tester: rankWith(priorityRanks.default, simpleTwinlistTester),
};
