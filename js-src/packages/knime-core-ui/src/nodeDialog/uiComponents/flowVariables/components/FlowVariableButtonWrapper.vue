<script setup lang="ts">
import type { FlowVariableButtonProps } from "../types/FlowVariableButtonProps";

import FlowVariableButton from "./FlowVariableButton.vue";
import {
  useProvideForFlowVariables,
  type useProvidedFlowVariablesProps,
} from "./useProvideForFlowVariables";

const props = defineProps<
  FlowVariableButtonProps & useProvidedFlowVariablesProps
>();

const emit = defineEmits<{
  controllingFlowVariableSet: [string, unknown, string];
}>();

useProvideForFlowVariables(props);
</script>

<template>
  <FlowVariableButton
    :hover="hover"
    @controlling-flow-variable-set="
      (path, value, flowVarName) =>
        emit('controllingFlowVariableSet', path, value, flowVarName)
    "
  />
</template>
