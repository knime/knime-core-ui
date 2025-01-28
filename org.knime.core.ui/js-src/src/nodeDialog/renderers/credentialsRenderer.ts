import { defineAsyncComponent } from "vue";
import { type Tester, rankWith } from "@jsonforms/core";

import { priorityRanks, withLabel } from "@knime/jsonforms";

import { inputFormats } from "../constants/inputFormats";

const CredentialsControl = defineAsyncComponent(
  () => import("../uiComponents/credentials/CredentialsControl.vue"),
);

export const credentialsTester: Tester = (uischema) => {
  return uischema.options?.format === inputFormats.credentials;
};

export const credentialsRenderer = withLabel({
  name: "CredentialsControl",
  control: CredentialsControl,
  tester: rankWith(priorityRanks.default, credentialsTester),
});
