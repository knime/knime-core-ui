<script setup lang="ts">
import useDialogControl from "@/nodeDialog/composables/components/useDialogControl";
import { rendererProps } from "@jsonforms/vue";
import CredentialsControlBase from "./CredentialsControlBase.vue";
import type Credentials from "./types/Credentials";
import LegacyFlowVariableHandler from "./LegacyFlowVariableHandler.vue";

interface LegacyCredentials {
  credentials: Credentials;
  flowVarName: string | null | undefined;
}

const props = defineProps(rendererProps());
const {
  control,
  onChange: onChangeControl,
  disabled,
  flowSettings,
} = useDialogControl<LegacyCredentials>({
  props,
});

const onChange = (credentials: Credentials) => {
  onChangeControl({ credentials, flowVarName: null });
};

const onLegacyFlowVariableSet = (flowVariableValue?: Credentials) => {
  onChange(flowVariableValue ?? control.value.data.credentials);
};
</script>

<template>
  <CredentialsControlBase
    :control="control"
    :data="control.data.credentials"
    :flow-settings="flowSettings"
    :disabled="disabled"
    @change="onChange"
  />
  <LegacyFlowVariableHandler
    :flow-variable-name="control.data.flowVarName"
    @flow-variable-set="onLegacyFlowVariableSet"
  />
</template>
