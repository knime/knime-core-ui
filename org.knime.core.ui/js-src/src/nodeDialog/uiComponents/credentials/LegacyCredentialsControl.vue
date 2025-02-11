<script setup lang="ts">
import { computed } from "vue";

import type { VueControlPropsForLabelContent } from "@knime/jsonforms";

import { useFlowSettings } from "../../composables/components/useFlowVariables";
import { useProvideForFlowVariables } from "../flowVariables/components/useProvideForFlowVariables";

import CredentialsControlBase from "./CredentialsControlBase.vue";
import LegacyFlowVariableHandler from "./LegacyFlowVariableHandler.vue";
import type { Credentials } from "./types/Credentials";

export interface LegacyCredentials {
  credentials: Credentials;
  flowVarName?: string | null;
}

const props = defineProps<VueControlPropsForLabelContent<LegacyCredentials>>();

const { flowSettings, configPaths } = useFlowSettings({
  path: computed(() => props.control.path),
});

useProvideForFlowVariables({
  flowSettings,
  configPaths,
  dataPath: props.control.path,
});

const onChangeLegacyCredentialsControl = (credentials: Credentials) => {
  props.changeValue({ credentials, flowVarName: null });
};

const onLegacyFlowVariableSet = (
  flowVariableValue: Credentials | undefined,
  flowVariableName: string,
) => {
  onChangeLegacyCredentialsControl({
    ...(flowVariableValue ?? props.control.data.credentials),
    flowVariableName,
  });
};
</script>

<template>
  <CredentialsControlBase
    :control="control"
    :data="control.data.credentials"
    :flow-settings="flowSettings"
    :disabled="disabled"
    :label-for-id="labelForId"
    :is-valid
    @change="onChangeLegacyCredentialsControl"
  />
  <LegacyFlowVariableHandler
    :flow-variable-name="control.data.flowVarName"
    @flow-variable-set="onLegacyFlowVariableSet"
  />
</template>
